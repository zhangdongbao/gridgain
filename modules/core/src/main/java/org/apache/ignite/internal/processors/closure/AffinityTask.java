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

package org.apache.ignite.internal.processors.closure;

import java.util.Collection;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.jetbrains.annotations.Nullable;

/**
 * Affinity mapped task.
 */
public interface AffinityTask {
    /**
     * @return Partition.
     */
    public int partition();

    /**
     * @return Affinity cache name.
     */
    @Nullable public Collection<String> affinityCacheNames();

    /**
     * @return Affinity topology version.
     */
    @Nullable public AffinityTopologyVersion topologyVersion();
}