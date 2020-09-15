/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.pipeline.xsltfunctions;

import stroom.pipeline.refdata.LookupIdentifier;
import stroom.pipeline.refdata.ReferenceData;
import stroom.pipeline.refdata.ReferenceDataResult;
import stroom.pipeline.refdata.store.RefDataValueProxyConsumerFactory;
import stroom.pipeline.state.MetaHolder;
import stroom.util.date.DateUtil;
import stroom.util.shared.Severity;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.EmptyAtomicSequence;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;

import javax.inject.Inject;

class BitmapLookup extends AbstractLookup {
    @Inject
    BitmapLookup(final ReferenceData referenceData,
                 final MetaHolder metaHolder,
                 final RefDataValueProxyConsumerFactory.Factory consumerFactoryFactory) {
        super(referenceData, metaHolder, consumerFactoryFactory);
    }

    @Override
    protected Sequence doLookup(final XPathContext context,
                                final boolean ignoreWarnings,
                                final boolean trace,
                                final LookupIdentifier lookupIdentifier) throws XPathException {
        SequenceMaker sequenceMaker = null;

        String key = lookupIdentifier.getKey();
        int val;
        try {
            if (key.startsWith("0x")) {
                // Hex input
                val = Integer.valueOf(key.substring(2), 16);
            } else {
                // Decimal input
                val = Integer.parseInt(key);
            }
        } catch (final NumberFormatException e) {
            throw new NumberFormatException("unable to parse number '" + key + "'");
        }

        // Convert the (decimal/hex) input value into a bitmap then into an array of the bit positions
        // that are set to 1.
        final int[] bits = Bitmap.getBits(val);
        StringBuilder failedBits = null;

        if (bits.length > 0) {
            // Now treat each bit position as a key and perform a lookup for each.
            for (final int bit : bits) {
                final String k = String.valueOf(bit);
                final LookupIdentifier bitIdentifier = lookupIdentifier.cloneWithNewKey(k);
                final ReferenceDataResult result = getReferenceData(bitIdentifier);

                boolean wasFound = false;

                try {
                    // Rather than doing individual lookups for each key (bit position) we could pass all the keys
                    // (bit positions) to the store and get it to open a cursor on the first key then scan over the
                    // keys concatenating the values of the matched keys.  Debatable if this is much quicker given
                    // the bitmap could be quite large so there would be a lot of keys to skip over.
                    // In fact this would only work if the data was stored in a store that was keyed by integer rather
                    // than string as the ordering would be wrong for a string keyed store.
                    if (result.getRefDataValueProxy().isPresent()) {
                        if (sequenceMaker == null) {
                            sequenceMaker = new SequenceMaker(context, getRefDataValueProxyConsumerFactoryFactory());
                            sequenceMaker.open();
                        }
                        wasFound = sequenceMaker.consume(result.getRefDataValueProxy().get());
                    }

                    if (trace && wasFound) {
                        outputInfo(Severity.INFO, "Lookup success ", lookupIdentifier, trace, result, context);
                    }

                    if (!wasFound && !ignoreWarnings) {
                        if (trace) {
                            outputInfo(Severity.WARNING, "Lookup failed ", lookupIdentifier, trace, result, context);
                        }

                        if (failedBits == null) {
                            failedBits = new StringBuilder();
                        }
                        failedBits.append(k);
                        failedBits.append(",");
                    }

                } catch (XPathException e) {
                    outputInfo(Severity.ERROR, "Lookup errored: " + e.getMessage(), lookupIdentifier, trace, result, context);
                }
            }

            if (failedBits != null) {
                failedBits.setLength(failedBits.length() - 1);
                failedBits.insert(0, "{");
                failedBits.append("}");

                // Create the message.
                final StringBuilder sb = new StringBuilder();
                sb.append("Lookup failed ");
                sb.append("(map = ");
                sb.append(lookupIdentifier.getPrimaryMapName());
                sb.append(", key = ");
                sb.append(failedBits.toString());
                sb.append(", eventTime = ");
                sb.append(DateUtil.createNormalDateTimeString(lookupIdentifier.getEventTime()));
                sb.append(")");
                outputWarning(context, sb, null);
            }

            if (sequenceMaker != null) {
                sequenceMaker.close();
                return sequenceMaker.toSequence();
            }
        }

        return EmptyAtomicSequence.getInstance();
    }
}
