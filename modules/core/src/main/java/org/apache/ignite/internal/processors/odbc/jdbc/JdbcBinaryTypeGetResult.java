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

import java.io.IOException;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.internal.binary.BinaryMetadata;
import org.apache.ignite.internal.binary.BinaryReaderExImpl;
import org.apache.ignite.internal.binary.BinaryWriterExImpl;
import org.apache.ignite.internal.processors.odbc.ClientListenerProtocolVersion;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * JDBC get binary type schema result.
 */
public class JdbcBinaryTypeGetResult extends JdbcResult {
    /** ID of initial request. */
    private long reqId;

    /** Binary type schema. */
    private BinaryMetadata meta;

    /** Default constructor for deserialization purpose. */
    JdbcBinaryTypeGetResult() {
        super(BINARY_TYPE_GET);
    }

    /**
     * @param reqId ID of initial request.
     * @param meta Schema of binary type.
     */
    public JdbcBinaryTypeGetResult(long reqId, BinaryMetadata meta) {
        super(BINARY_TYPE_GET);

        this.reqId = reqId;
        this.meta = meta;
    }

    /**
     * Returns schema of binary type.
     *
     * @return Schema of binary type.
     */
    public BinaryMetadata meta() {
        return meta;
    }

    /**
     * Returns ID of initial request.
     *
     * @return ID of initial request.
     */
    public long reqId() {
        return reqId;
    }

    /** {@inheritDoc} */
    @Override public void writeBinary(BinaryWriterExImpl writer, ClientListenerProtocolVersion ver) throws BinaryObjectException {
        super.writeBinary(writer, ver);

        writer.writeLong(reqId);
        try {
            meta.writeTo(writer);
        }
        catch (IOException e) {
            throw new BinaryObjectException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void readBinary(BinaryReaderExImpl reader, ClientListenerProtocolVersion ver) throws BinaryObjectException {
        super.readBinary(reader, ver);

        reqId = reader.readLong();
        meta = new BinaryMetadata();
        try {
            meta.readFrom(reader);
        }
        catch (IOException e) {
            throw new BinaryObjectException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(JdbcBinaryTypeGetResult.class, this);
    }
}