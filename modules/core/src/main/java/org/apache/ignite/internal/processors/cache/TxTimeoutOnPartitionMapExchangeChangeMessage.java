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

package org.apache.ignite.internal.processors.cache;

import java.util.UUID;
import org.apache.ignite.internal.managers.discovery.DiscoCache;
import org.apache.ignite.internal.managers.discovery.DiscoveryCustomMessage;
import org.apache.ignite.internal.managers.discovery.GridDiscoveryManager;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.lang.IgniteUuid;
import org.jetbrains.annotations.Nullable;

/**
 * Discovery message for changing transaction timeout on partition map exchange.
 */
public class TxTimeoutOnPartitionMapExchangeChangeMessage implements DiscoveryCustomMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private final IgniteUuid id = IgniteUuid.randomUuid();

    /** Request ID. */
    private final UUID reqId;

    /** Transaction timeout on partition map exchange in milliseconds. */
    private final long timeout;

    /** Init flag. */
    private final boolean isInit;

    /**
     * Constructor for response.
     *
     * @param req Request message.
     */
    public TxTimeoutOnPartitionMapExchangeChangeMessage(TxTimeoutOnPartitionMapExchangeChangeMessage req) {
        this.reqId = req.reqId;
        this.timeout = req.timeout;
        this.isInit = false;
    }

    /**
     * Constructor.
     *
     * @param reqId Request ID.
     * @param timeout Transaction timeout on partition map exchange in milliseconds.
     */
    public TxTimeoutOnPartitionMapExchangeChangeMessage(UUID reqId, long timeout) {
        this.reqId = reqId;
        this.timeout = timeout;
        this.isInit = true;
    }

    /** {@inheritDoc} */
    @Override public IgniteUuid id() {
        return id;
    }

    /** {@inheritDoc} */
    @Nullable @Override public DiscoveryCustomMessage ackMessage() {
        return isInit() ? new TxTimeoutOnPartitionMapExchangeChangeMessage(this) : null;
    }

    /** {@inheritDoc} */
    @Override public boolean isMutable() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean stopProcess() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public DiscoCache createDiscoCache(GridDiscoveryManager mgr, AffinityTopologyVersion topVer,
        DiscoCache discoCache) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets request ID.
     *
     * @return Request ID.
     */
    public UUID getRequestId() {
        return reqId;
    }

    /**
     * Gets transaction timeout on partition map exchange in milliseconds.
     *
     * @return Transaction timeout on partition map exchange in milliseconds.
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Gets init flag.
     *
     * @return Init flag.
     */
    public boolean isInit() {
        return isInit;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(TxTimeoutOnPartitionMapExchangeChangeMessage.class, this);
    }
}
