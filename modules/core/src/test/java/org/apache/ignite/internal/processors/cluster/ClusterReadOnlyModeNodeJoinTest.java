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

package org.apache.ignite.internal.processors.cluster;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

import static org.apache.ignite.internal.processors.cache.ClusterReadOnlyModeTestUtils.assertCachesReadOnlyMode;
import static org.apache.ignite.internal.processors.cache.ClusterReadOnlyModeTestUtils.assertDataStreamerReadOnlyMode;
import static org.apache.ignite.internal.processors.cache.ClusterReadOnlyModeTestUtils.cacheConfigurations;
import static org.apache.ignite.internal.processors.cache.ClusterReadOnlyModeTestUtils.cacheNames;

/**
 * Checks that new joining node accept enabled read-only mode.
 */
public class ClusterReadOnlyModeNodeJoinTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        return super.getConfiguration(igniteInstanceName).setCacheConfiguration(cacheConfigurations());
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /** */
    @Test
    public void testJoinNodeToReadOnlyCluster() throws Exception {
        IgniteEx grid = startGrid(0);

        assertTrue(grid.cluster().active());
        assertFalse(grid.cluster().readOnly());

        grid.cluster().readOnly(true);

        assertCachesReadOnlyMode(true, cacheNames());
        assertDataStreamerReadOnlyMode(true, cacheNames());

        startGrid(1);

        awaitPartitionMapExchange();

        for (int i = 0; i < 2; i++)
            assertTrue(grid(i).configuration().getIgniteInstanceName(), grid(i).cluster().readOnly());

        assertCachesReadOnlyMode(true, cacheNames());
        assertDataStreamerReadOnlyMode(true, cacheNames());
    }
}
