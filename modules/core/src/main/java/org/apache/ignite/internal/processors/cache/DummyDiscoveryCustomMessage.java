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

import java.util.HashMap;
import org.apache.ignite.internal.managers.discovery.DiscoCache;
import org.apache.ignite.internal.managers.discovery.DiscoveryCustomMessage;
import org.apache.ignite.internal.managers.discovery.GridDiscoveryManager;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.lang.IgniteUuid;
import org.jetbrains.annotations.Nullable;

/**
 * Dummy message is uses as lightweight storage discovery custom event parameters.
 */
public class DummyDiscoveryCustomMessage implements DiscoveryCustomMessage {

    /** */
    private final IgniteUuid id;

    public DummyDiscoveryCustomMessage(IgniteUuid id) {
        this.id = id;
    }

    HashMap<Object, Object> map = new HashMap<>();

    public Object getParameter(Object key) {
        return map.get(key);
    }

    public Object putParameter(Object key, Object val) {
        return map.put(key, val);
    }

    @Override public IgniteUuid id() {
        return id;
    }

    @Override public @Nullable DiscoveryCustomMessage ackMessage() {
        return null;
    }

    @Override public boolean isMutable() {
        return false;
    }

    @Override public boolean stopProcess() {
        return false;
    }

    @Override public DiscoCache createDiscoCache(GridDiscoveryManager mgr, AffinityTopologyVersion topVer,
        DiscoCache discoCache) {
        return null;
    }
}
