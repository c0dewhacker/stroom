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

import stroom.pipeline.shared.SourceLocation;
import stroom.pipeline.state.LocationHolder;
import stroom.pipeline.state.LocationHolder.FunctionType;
import stroom.util.NullSafe;
import stroom.util.shared.DataRange;
import stroom.util.shared.Location;

import javax.inject.Inject;

class LineTo extends AbstractLocationFunction {
    @Inject
    LineTo(final LocationHolder locationHolder) {
        super(locationHolder);
    }

    @Override
    String getValue(final SourceLocation sourceLocation) {
        return NullSafe.get(
                sourceLocation,
                SourceLocation::getFirstHighlight,
                DataRange::getLocationTo,
                Location::getLineNo,
                String::valueOf);
    }

    @Override
    FunctionType getType() {
        return FunctionType.LINE_TO;
    }
}
