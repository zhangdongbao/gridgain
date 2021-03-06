/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.odbc.jdbc;

import java.util.Collection;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.internal.binary.BinaryReaderExImpl;
import org.apache.ignite.internal.binary.BinaryWriterExImpl;
import org.apache.ignite.internal.processors.odbc.ClientListenerProtocolVersion;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * JDBC tables metadata result.
 */
public class JdbcMetaSchemasResult extends JdbcResult {
    /** Found schemas. */
    private Collection<String> schemas;

    /**
     * Default constructor is used for deserialization.
     */
    JdbcMetaSchemasResult() {
        super(META_SCHEMAS);
    }

    /**
     * @param schemas Found schemas.
     */
    JdbcMetaSchemasResult(Collection<String> schemas) {
        super(META_SCHEMAS);
        this.schemas = schemas;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriterExImpl writer,
        ClientListenerProtocolVersion ver) throws BinaryObjectException {
        super.writeBinary(writer, ver);

        JdbcUtils.writeStringCollection(writer, schemas);
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReaderExImpl reader,
        ClientListenerProtocolVersion ver) throws BinaryObjectException {
        super.readBinary(reader, ver);

        schemas = JdbcUtils.readStringList(reader);
    }

    /**
     * @return Found schemas.
     */
    public Collection<String> schemas() {
        return schemas;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(JdbcMetaSchemasResult.class, this);
    }
}
