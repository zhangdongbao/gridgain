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

package org.apache.ignite.cache;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
import org.apache.ignite.internal.processors.cache.CacheGroupContext;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionDemandMessage;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class RebalanceCancellationTest extends GridCommonAbstractTest {
    /** Start cluster nodes. */
    public static final int NODES_CNT = 3;

    /** Count of backup partitions. */
    public static final int BACKUPS = 2;
    /***/
    public static final String MEM_REGION = "mem-region";
    /***/
    public static final String MEM_REGOIN_CACHE = DEFAULT_CACHE_NAME + "_mem";
    /***/
    public static final String DYNAMIC_CACHE_NAME = DEFAULT_CACHE_NAME + "_dynamic";
    /***/
    public static final String FITERED_NODE_SUFFIX = "_fitered";

    /** Persistence enabled. */
    public boolean persistenceEnabled;

    /** Add additional non-persistence data region. */
    public boolean addtiotionalMemRegion;

    /** Filter node. */
    public boolean filterNode;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName)
            .setConsistentId(igniteInstanceName)
            .setCommunicationSpi(new TestRecordingCommunicationSpi())
            .setDataStorageConfiguration(new DataStorageConfiguration()
                .setDefaultDataRegionConfiguration(new DataRegionConfiguration()
                    .setPersistenceEnabled(persistenceEnabled)))
            .setCacheConfiguration(
                new CacheConfiguration(DEFAULT_CACHE_NAME)
                    .setAffinity(new RendezvousAffinityFunction(false, 15))
                    .setBackups(BACKUPS));

        if (addtiotionalMemRegion) {
            cfg.setCacheConfiguration(cfg.getCacheConfiguration()[0],
                new CacheConfiguration(MEM_REGOIN_CACHE)
                    .setDataRegionName(MEM_REGION)
                    .setBackups(BACKUPS))
                .getDataStorageConfiguration()
                .setDataRegionConfigurations(new DataRegionConfiguration()
                    .setName(MEM_REGION));
        }

        if (filterNode) {
            for (CacheConfiguration ccfg : cfg.getCacheConfiguration()) {
                ccfg.setNodeFilter(new IgnitePredicate<ClusterNode>() {
                    @Override public boolean apply(ClusterNode node) {
                        return !node.consistentId().toString().contains(FITERED_NODE_SUFFIX);
                    }
                });
            }
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
        cleanPersistenceDir();
    }

    //--

    /** */
    @Test
    public void testRebalanceNoneBltNodeLeftOnOnlyPersistenceCluster() throws Exception {
        testRebalanceNoneBltNode(true, false, false);
    }

    /** */
    @Test
    public void testRebalanceNoneBltNodeLeftOnOnlyInMemoryCluster() throws Exception {
        testRebalanceNoneBltNode(false, false, false);
    }

    /** */
    @Test
    public void testRebalanceNoneBltNodeLeftOnMixedCluster() throws Exception {
        testRebalanceNoneBltNode(true, true, false);
    }

    //--

    /** */
    @Test
    public void testRebalanceNoneBltNodeFailedOnOnlyPersistenceCluster() throws Exception {
        testRebalanceNoneBltNode(true, false, true);
    }

    /** */
    @Test
    public void testRebalanceNoneBltNodeFailedOnOnlyInMemoryCluster() throws Exception {
        testRebalanceNoneBltNode(false, false, true);
    }

    /** */
    @Test
    public void testRebalanceNoneBltNodeFailedOnMixedCluster() throws Exception {
        testRebalanceNoneBltNode(true, true, true);
    }

    //--

    /** */
    @Test
    public void testRebalanceFilteredNodeOnOnlyPersistenceCluster() throws Exception {
        testRebalanceNoneBltNode(true, false, true);
    }

    /** */
    @Test
    public void testRebalanceFilteredNodeOnOnlyInMemoryCluster() throws Exception {
        testRebalanceNoneBltNode(false, false, true);
    }

    /** */
    @Test
    public void testRebalanceFilteredNodeOnMixedCluster() throws Exception {
        testRebalanceNoneBltNode(true, true, true);
    }

    //--

    /** */
    @Test
    public void testRebalanceDynamicCacheOnOnlyPersistenceCluster() throws Exception {
        testRebalanceDynamicCache(true, false);
    }

    /** */
    @Test
    public void testRebalanceDynamicCacheOnOnlyInMemoryCluster() throws Exception {
        testRebalanceDynamicCache(false, false);
    }

    /** */
    @Test
    public void testRebalanceDynamicCacheOnMixedCluster() throws Exception {
        testRebalanceDynamicCache(true, true);
    }

    /** */
    public void testRebalanceDynamicCache(boolean persistence, boolean addtiotionalRegion) throws Exception {
        persistenceEnabled = persistence;
        addtiotionalMemRegion = addtiotionalRegion;

        IgniteEx ignite0 = startGrids(NODES_CNT);

        ignite0.cluster().active(true);

        grid(1).close();

        for (String cache : ignite0.cacheNames())
            loadData(ignite0, cache);

        awaitPartitionMapExchange();

        TestRecordingCommunicationSpi commSpi1 = startNodeWithBlockingRebalance(getTestIgniteInstanceName(1));

        commSpi1.waitForBlocked();

        IgniteInternalFuture<Boolean>[] futs = getAllRebalanceFutures(ignite0);

        int previousCaches = ignite0.cacheNames().size();

        for (int i = 0; i < 3; i++) {
            ignite0.createCache(DYNAMIC_CACHE_NAME);

            assertEquals(previousCaches + 1, ignite0.cacheNames().size());

            ignite0.destroyCache(DYNAMIC_CACHE_NAME);

            assertEquals(previousCaches, ignite0.cacheNames().size());
        }

        for (IgniteInternalFuture<Boolean> fut : futs)
            assertFalse(futInfoString(fut), fut.isDone());

        commSpi1.stopBlock();

        awaitPartitionMapExchange();

        for (IgniteInternalFuture<Boolean> fut : futs)
            assertTrue(futInfoString(fut), fut.isDone() && fut.get());
    }

    /** */
    public void testRebalanceNoneBltNode(boolean persistence, boolean addtiotionalRegion,
        boolean fail) throws Exception {
        persistenceEnabled = persistence;
        addtiotionalMemRegion = addtiotionalRegion;

        IgniteEx ignite0 = startGrids(NODES_CNT);

        ignite0.cluster().active(true);

        ignite0.cluster().baselineAutoAdjustEnabled(false);

        IgniteEx newNode = startGrid(NODES_CNT);

        grid(1).close();

        for (String cache : ignite0.cacheNames())
            loadData(ignite0, cache);

        awaitPartitionMapExchange();

        TestRecordingCommunicationSpi commSpi1 = startNodeWithBlockingRebalance(getTestIgniteInstanceName(1));

        commSpi1.waitForBlocked();

        IgniteInternalFuture<Boolean>[] futs = getAllRebalanceFutures(ignite0);

        for (int i = 0; i < 3; i++) {
            if (fail) {
                ignite0.configuration().getDiscoverySpi().failNode(newNode.localNode().id(), "Fail node by test.");

                newNode.close();
            }
            else
                newNode.close();

            checkTopology(NODES_CNT);

            newNode = startGrid(NODES_CNT);

            checkTopology(NODES_CNT + 1);
        }

        for (IgniteInternalFuture<Boolean> fut : futs) {
            CacheGroupContext grp = U.field(fut, "grp");

            if (CU.isPersistentCache(grp.config(), ignite0.configuration().getDataStorageConfiguration()))
                assertFalse(futInfoString(fut), fut.isDone());
        }

        commSpi1.stopBlock();

        awaitPartitionMapExchange();

        for (IgniteInternalFuture<Boolean> fut : futs) {
            CacheGroupContext grp = U.field(fut, "grp");

            if (CU.isPersistentCache(grp.config(), ignite0.configuration().getDataStorageConfiguration()))
                assertTrue(futInfoString(fut), fut.isDone() && fut.get());
        }
    }

    /** */
    public void testRebalanceFilteredNode(boolean persistence, boolean addtiotionalRegion) throws Exception {
        persistenceEnabled = persistence;
        addtiotionalMemRegion = addtiotionalRegion;
        filterNode = true;

        IgniteEx ignite0 = startGrids(NODES_CNT);
        IgniteEx filteredNode = startGrid(getTestIgniteInstanceName(NODES_CNT) + FITERED_NODE_SUFFIX);

        ignite0.cluster().active(true);

        grid(1).close();

        for (String cache : ignite0.cacheNames())
            loadData(ignite0, cache);

        awaitPartitionMapExchange();

        TestRecordingCommunicationSpi commSpi1 = startNodeWithBlockingRebalance(getTestIgniteInstanceName(1));

        commSpi1.waitForBlocked();

        IgniteInternalFuture<Boolean>[] futs = getAllRebalanceFutures(ignite0);

        for (int k = 0; k < 3; k++) {
            filteredNode.close();

            checkTopology(NODES_CNT);

            filteredNode = startGrid(getTestIgniteInstanceName(NODES_CNT) + FITERED_NODE_SUFFIX);

            checkTopology(NODES_CNT + 1);
        }

        for (IgniteInternalFuture<Boolean> fut : futs)
            assertFalse(futInfoString(fut), fut.isDone());

        commSpi1.stopBlock();

        awaitPartitionMapExchange();

        for (IgniteInternalFuture<Boolean> fut : futs)
            assertTrue(futInfoString(fut), fut.isDone() && fut.get());
    }

    /**
     * @param ignite Ignite.
     * @return Array of rebelance futures.
     */
    @NotNull private IgniteInternalFuture<Boolean>[] getAllRebalanceFutures(IgniteEx ignite) {
        IgniteInternalFuture<Boolean>[] futs = new IgniteInternalFuture[ignite.cacheNames().size()];

        int i = 0;

        for (String cache : ignite.cacheNames()) {
            futs[i] = grid(1).context().cache()
                .cacheGroup(CU.cacheId(cache)).preloader().rebalanceFuture();

            assertFalse(futInfoString(futs[i]), futs[i].isDone());

            i++;
        }
        return futs;
    }

    /**
     * @param rebalanceFuture Rebalance future.
     * @return Information string about passed future.
     */
    @NotNull private String futInfoString(IgniteInternalFuture<Boolean> rebalanceFuture) {
        return "Fut: " + rebalanceFuture
            + " is done: " + rebalanceFuture.isDone()
            + " result: " + (rebalanceFuture.isDone() ? rebalanceFuture.result() : "NoN");
    }

    /**
     * @param ignite Ignite.
     * @param cacheName Cache name.
     */
    private void loadData(Ignite ignite, String cacheName) {
        try (IgniteDataStreamer streamer = ignite.dataStreamer(cacheName)) {
            streamer.allowOverwrite(true);

            for (int i = 0; i < 100; i++)
                streamer.addData(i, System.nanoTime());
        }
    }

    /**
     * @param name Node instance name.
     * @return Test communication SPI.
     * @throws Exception If failed.
     */
    @NotNull private TestRecordingCommunicationSpi startNodeWithBlockingRebalance(String name) throws Exception {
        IgniteConfiguration cfg = optimize(getConfiguration(name));

        TestRecordingCommunicationSpi communicationSpi = (TestRecordingCommunicationSpi)cfg.getCommunicationSpi();

        communicationSpi.blockMessages((node, msg) -> {
            if (msg instanceof GridDhtPartitionDemandMessage) {
                GridDhtPartitionDemandMessage demandMessage = (GridDhtPartitionDemandMessage)msg;

                long rebalanceId = U.field(demandMessage, "rebalanceId");

                if (CU.cacheId(DEFAULT_CACHE_NAME) != demandMessage.groupId()
                    && CU.cacheId(MEM_REGOIN_CACHE) != demandMessage.groupId())
                    return false;

                info("Message was caught: " + msg.getClass().getSimpleName()
                    + " rebalanceId = " + rebalanceId
                    + " to: " + node.consistentId()
                    + " by cache id: " + demandMessage.groupId());

                return true;
            }

            return false;
        });

        startGrid(cfg);

        return communicationSpi;
    }

}
