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

package org.apache.ignite.internal.processors.cache.distributed.dht;

import java.nio.ByteBuffer;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.GridDirectTransient;
import org.apache.ignite.internal.processors.cache.GridCacheIdMessage;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteUuid;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;

/**
 *
 */
public class GridDhtTxQueryEnlistResponse extends GridCacheIdMessage {
    /** */
    private static final long serialVersionUID = -1510546400896574705L;

    /** Future ID. */
    private IgniteUuid futId;

    /** */
    private int batchId;

    /** Error. */
    @GridDirectTransient
    private Throwable err;

    /** Serialized error. */
    private byte[] errBytes;

    /**
     *
     */
    public GridDhtTxQueryEnlistResponse() {
    }

    /**
     * @param cacheId Cache id.
     * @param futId Future id.
     * @param batchId Batch id.
     * @param err Error.
     */
    public GridDhtTxQueryEnlistResponse(int cacheId, IgniteUuid futId, int batchId,
        Throwable err) {
        this.cacheId = cacheId;
        this.futId = futId;
        this.batchId = batchId;
        this.err = err;
    }

    /**
     * @return Future id.
     */
    public IgniteUuid futureId() {
        return futId;
    }

    /**
     * @return Batch id.
     */
    public int batchId() {
        return batchId;
    }

    /**
     * @return Error.
     */
    @Override public Throwable error() {
        return err;
    }

    /** {@inheritDoc} */
    @Override public void prepareMarshal(GridCacheSharedContext ctx) throws IgniteCheckedException {
        super.prepareMarshal(ctx);

        if (err != null && errBytes == null)
            errBytes = U.marshal(ctx.marshaller(), err);
    }

    /** {@inheritDoc} */
    @Override public void finishUnmarshal(GridCacheSharedContext ctx, ClassLoader ldr) throws IgniteCheckedException {
        super.finishUnmarshal(ctx, ldr);

        if (errBytes != null)
            err = U.unmarshal(ctx, errBytes, U.resolveClassLoader(ldr, ctx.gridConfig()));
    }

    /** {@inheritDoc} */
    @Override public boolean addDeploymentInfo() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public short directType() {
        return 144;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 7;
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!super.writeTo(buf, writer))
            return false;

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 4:
                if (!writer.writeInt("batchId", batchId))
                    return false;

                writer.incrementState();

            case 5:
                if (!writer.writeByteArray("errBytes", errBytes))
                    return false;

                writer.incrementState();

            case 6:
                if (!writer.writeIgniteUuid("futId", futId))
                    return false;

                writer.incrementState();

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        reader.setBuffer(buf);

        if (!reader.beforeMessageRead())
            return false;

        if (!super.readFrom(buf, reader))
            return false;

        switch (reader.state()) {
            case 4:
                batchId = reader.readInt("batchId");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 5:
                errBytes = reader.readByteArray("errBytes");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 6:
                futId = reader.readIgniteUuid("futId");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(GridDhtTxQueryEnlistResponse.class);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDhtTxQueryEnlistResponse.class, this);
    }
}
