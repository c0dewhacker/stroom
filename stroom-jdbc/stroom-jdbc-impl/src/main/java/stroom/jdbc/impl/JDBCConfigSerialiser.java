/*
 * Copyright 2019 Crown Copyright
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

package stroom.jdbc.impl;

import stroom.docstore.api.DocumentSerialiser2;
import stroom.docstore.api.Serialiser2;
import stroom.docstore.api.Serialiser2Factory;
import stroom.jdbc.shared.JDBCConfigDoc;

import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;

public class JDBCConfigSerialiser implements DocumentSerialiser2<JDBCConfigDoc> {

    private final Serialiser2<JDBCConfigDoc> delegate;

    @Inject
    public JDBCConfigSerialiser(final Serialiser2Factory serialiser2Factory) {
        this.delegate = serialiser2Factory.createSerialiser(JDBCConfigDoc.class);
    }

    @Override
    public JDBCConfigDoc read(final Map<String, byte[]> data) throws IOException {
        return delegate.read(data);
    }

    @Override
    public Map<String, byte[]> write(final JDBCConfigDoc document) throws IOException {
        return delegate.write(document);
    }
}
