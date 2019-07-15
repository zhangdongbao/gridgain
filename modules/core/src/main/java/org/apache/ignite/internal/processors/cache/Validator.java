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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataPageEvictionMode;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.DeploymentMode;
import org.apache.ignite.configuration.DiskPageCompression;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.configuration.TransactionConfiguration;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.IgniteFeatures;
import org.apache.ignite.internal.IgniteNodeAttributes;
import org.apache.ignite.internal.binary.BinaryMarshaller;
import org.apache.ignite.internal.processors.cache.persistence.DataRegion;
import org.apache.ignite.internal.processors.datastructures.DataStructuresProcessor;
import org.apache.ignite.internal.processors.query.QuerySchemaPatch;
import org.apache.ignite.internal.processors.query.QueryUtils;
import org.apache.ignite.internal.processors.security.OperationSecurityContext;
import org.apache.ignite.internal.processors.security.SecurityContext;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteProductVersion;
import org.apache.ignite.marshaller.Marshaller;
import org.apache.ignite.marshaller.jdk.JdkMarshaller;
import org.apache.ignite.plugin.security.SecurityException;
import org.apache.ignite.spi.IgniteNodeValidationResult;
import org.apache.ignite.spi.discovery.DiscoveryDataBag;
import org.apache.ignite.spi.encryption.EncryptionSpi;
import org.apache.ignite.spi.indexing.IndexingSpi;
import org.apache.ignite.spi.indexing.noop.NoopIndexingSpi;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.IgniteSystemProperties.IGNITE_SKIP_CONFIGURATION_CONSISTENCY_CHECK;
import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT;
import static org.apache.ignite.cache.CacheMode.LOCAL;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.cache.CacheRebalanceMode.NONE;
import static org.apache.ignite.cache.CacheRebalanceMode.SYNC;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_ASYNC;
import static org.apache.ignite.configuration.DeploymentMode.ISOLATED;
import static org.apache.ignite.configuration.DeploymentMode.PRIVATE;
import static org.apache.ignite.internal.IgniteNodeAttributes.ATTR_CONSISTENCY_CHECK_SKIPPED;
import static org.apache.ignite.internal.IgniteNodeAttributes.ATTR_TX_CONFIG;
import static org.apache.ignite.internal.processors.cache.GridCacheUtils.isDefaultDataRegionPersistent;
import static org.apache.ignite.internal.processors.security.SecurityUtils.nodeSecurityContext;

public class Validator {
    /** Template of message of conflicts during configuration merge*/
    private static final String MERGE_OF_CONFIG_CONFLICTS_MESSAGE =
        "Conflicts during configuration merge for cache '%s' : \n%s";

    /** Template of message of node join was fail because it requires to merge of config */
    private static final String MERGE_OF_CONFIG_REQUIRED_MESSAGE = "Failed to join node to the active cluster " +
        "(the config of the cache '%s' has to be merged which is impossible on active grid). " +
        "Deactivate grid and retry node join or clean the joining node.";

    /** Template of message of failed node join because encryption settings are different for the same cache. */
    private static final String ENCRYPT_MISMATCH_MESSAGE = "Failed to join node to the cluster " +
        "(encryption settings are different for cache '%s' : local=%s, remote=%s.)";

    /** Supports non default precision and scale for DECIMAL and VARCHAR types. */
    private static final IgniteProductVersion PRECISION_SCALE_SINCE_VER = IgniteProductVersion.fromString("2.7.0");

    @Nullable static IgniteNodeValidationResult validateNode(
        ClusterNode node,
        DiscoveryDataBag.JoiningNodeDiscoveryData discoData,
        Marshaller marsh,
        GridKernalContext ctx,
        Function<String, DynamicCacheDescriptor> cacheDescriptorProvider
    ) {
        if (discoData.hasJoiningNodeData() && discoData.joiningNodeData() instanceof CacheJoinNodeDiscoveryData) {
            CacheJoinNodeDiscoveryData nodeData = (CacheJoinNodeDiscoveryData)discoData.joiningNodeData();

            boolean isGridActive = ctx.state().clusterState().active();

            StringBuilder errorMsg = new StringBuilder();

            if (!node.isClient()) {
                Validator.validateRmtRegions(node, ctx).forEach(error -> {
                    if (errorMsg.length() > 0) {
                        errorMsg.append("\n");
                    }

                    errorMsg.append(error);
                });
            }

            SecurityContext secCtx = null;

            if (ctx.security().enabled()) {
                try {
                    secCtx = nodeSecurityContext(marsh, U.resolveClassLoader(ctx.config()), node);
                }
                catch (SecurityException se) {
                    errorMsg.append(se.getMessage());
                }
            }

            for (CacheJoinNodeDiscoveryData.CacheInfo cacheInfo : nodeData.caches().values()) {
                if (secCtx != null && cacheInfo.cacheType() == CacheType.USER) {
                    try (OperationSecurityContext s = ctx.security().withContext(secCtx)) {
                        GridCacheProcessor.authorizeCacheCreate(cacheInfo.cacheData().config(), ctx);
                    }
                    catch (SecurityException ex) {
                        if (errorMsg.length() > 0)
                            errorMsg.append("\n");

                        errorMsg.append(ex.getMessage());
                    }
                }

                DynamicCacheDescriptor locDesc = cacheDescriptorProvider.apply(cacheInfo.cacheData().config().getName());

                if (locDesc == null)
                    continue;

                QuerySchemaPatch schemaPatch = locDesc.makeSchemaPatch(cacheInfo.cacheData().queryEntities());

                if (schemaPatch.hasConflicts() || (isGridActive && !schemaPatch.isEmpty())) {
                    if (errorMsg.length() > 0)
                        errorMsg.append("\n");

                    if (schemaPatch.hasConflicts())
                        errorMsg.append(String.format(MERGE_OF_CONFIG_CONFLICTS_MESSAGE,
                            locDesc.cacheName(), schemaPatch.getConflictsMessage()));
                    else
                        errorMsg.append(String.format(MERGE_OF_CONFIG_REQUIRED_MESSAGE, locDesc.cacheName()));
                }

                // This check must be done on join, otherwise group encryption key will be
                // written to metastore regardless of validation check and could trigger WAL write failures.
                boolean locEnc = locDesc.cacheConfiguration().isEncryptionEnabled();
                boolean rmtEnc = cacheInfo.cacheData().config().isEncryptionEnabled();

                if (locEnc != rmtEnc) {
                    if (errorMsg.length() > 0)
                        errorMsg.append("\n");

                    // Message will be printed on remote node, so need to swap local and remote.
                    errorMsg.append(String.format(ENCRYPT_MISMATCH_MESSAGE, locDesc.cacheName(), rmtEnc, locEnc));
                }
            }

            if (errorMsg.length() > 0) {
                String msg = errorMsg.toString();

                return new IgniteNodeValidationResult(node.id(), msg);
            }
        }
        return null;
    }

    /**
     * @param c Ignite configuration.
     * @param cc Configuration to validate.
     * @param cacheType Cache type.
     * @param cfgStore Cache store.
     * @param ctx
     * @param log
     * @throws IgniteCheckedException If failed.
     */
    static void validate(IgniteConfiguration c,
        CacheConfiguration cc,
        CacheType cacheType,
        @Nullable CacheStore cfgStore, GridKernalContext ctx, IgniteLogger log,
        BiFunction<Boolean, String, IgniteCheckedException> assertParameter
    ) throws IgniteCheckedException {
        apply(assertParameter,cc.getName() != null && !cc.getName().isEmpty(), "name is null or empty");

        if (cc.getCacheMode() == REPLICATED) {
            if (cc.getNearConfiguration() != null &&
                ctx.discovery().cacheAffinityNode(ctx.discovery().localNode(), cc.getName())) {
                U.warn(log, "Near cache cannot be used with REPLICATED cache, " +
                    "will be ignored [cacheName=" + U.maskName(cc.getName()) + ']');

                cc.setNearConfiguration(null);
            }
        }

        if (storesLocallyOnClient(c, cc, ctx))
            throw new IgniteCheckedException("DataRegion for client caches must be explicitly configured " +
                "on client node startup. Use DataStorageConfiguration to configure DataRegion.");

        if (cc.getCacheMode() == LOCAL && !cc.getAffinity().getClass().equals(GridCacheProcessor.LocalAffinityFunction.class))
            U.warn(log, "AffinityFunction configuration parameter will be ignored for local cache [cacheName=" +
                U.maskName(cc.getName()) + ']');

        if (cc.getAffinity().partitions() > CacheConfiguration.MAX_PARTITIONS_COUNT)
            throw new IgniteCheckedException("Cannot have more than " + CacheConfiguration.MAX_PARTITIONS_COUNT +
                " partitions [cacheName=" + cc.getName() + ", partitions=" + cc.getAffinity().partitions() + ']');

        if (cc.getRebalanceMode() != CacheRebalanceMode.NONE) {
            apply(assertParameter,cc.getRebalanceBatchSize() > 0, "rebalanceBatchSize > 0");
            apply(assertParameter,cc.getRebalanceTimeout() >= 0, "rebalanceTimeout >= 0");
            apply(assertParameter,cc.getRebalanceThrottle() >= 0, "rebalanceThrottle >= 0");
            apply(assertParameter,cc.getRebalanceBatchesPrefetchCount() > 0, "rebalanceBatchesPrefetchCount > 0");
        }

        if (cc.getCacheMode() == PARTITIONED || cc.getCacheMode() == REPLICATED) {
            if (cc.getAtomicityMode() == ATOMIC && cc.getWriteSynchronizationMode() == FULL_ASYNC)
                U.warn(log, "Cache write synchronization mode is set to FULL_ASYNC. All single-key 'put' and " +
                    "'remove' operations will return 'null', all 'putx' and 'removex' operations will return" +
                    " 'true' [cacheName=" + U.maskName(cc.getName()) + ']');
        }

        DeploymentMode depMode = c.getDeploymentMode();

        if (c.isPeerClassLoadingEnabled() && (depMode == PRIVATE || depMode == ISOLATED) &&
            !CU.isSystemCache(cc.getName()) && !(c.getMarshaller() instanceof BinaryMarshaller))
            throw new IgniteCheckedException("Cache can be started in PRIVATE or ISOLATED deployment mode only when" +
                " BinaryMarshaller is used [depMode=" + ctx.config().getDeploymentMode() + ", marshaller=" +
                c.getMarshaller().getClass().getName() + ']');

        if (cc.getAffinity().partitions() > CacheConfiguration.MAX_PARTITIONS_COUNT)
            throw new IgniteCheckedException("Affinity function must return at most " +
                CacheConfiguration.MAX_PARTITIONS_COUNT + " partitions [actual=" + cc.getAffinity().partitions() +
                ", affFunction=" + cc.getAffinity() + ", cacheName=" + cc.getName() + ']');

        if (cc.getAtomicityMode() == TRANSACTIONAL_SNAPSHOT) {
            apply(assertParameter,cc.getCacheMode() != LOCAL,
                "LOCAL cache mode cannot be used with TRANSACTIONAL_SNAPSHOT atomicity mode");

            apply(assertParameter,cc.getNearConfiguration() == null,
                "near cache cannot be used with TRANSACTIONAL_SNAPSHOT atomicity mode");

            apply(assertParameter,!cc.isReadThrough(),
                "readThrough cannot be used with TRANSACTIONAL_SNAPSHOT atomicity mode");

            apply(assertParameter,!cc.isWriteThrough(),
                "writeThrough cannot be used with TRANSACTIONAL_SNAPSHOT atomicity mode");

            apply(assertParameter,!cc.isWriteBehindEnabled(),
                "writeBehindEnabled cannot be used with TRANSACTIONAL_SNAPSHOT atomicity mode");

            apply(assertParameter,cc.getRebalanceMode() != NONE,
                "Rebalance mode NONE cannot be used with TRANSACTIONAL_SNAPSHOT atomicity mode");

            ExpiryPolicy expPlc = null;

            if (cc.getExpiryPolicyFactory() instanceof FactoryBuilder.SingletonFactory)
                expPlc = (ExpiryPolicy)cc.getExpiryPolicyFactory().create();

            if (!(expPlc instanceof EternalExpiryPolicy)) {
                apply(assertParameter,cc.getExpiryPolicyFactory() == null,
                    "expiry policy cannot be used with TRANSACTIONAL_SNAPSHOT atomicity mode");
            }

            apply(assertParameter,cc.getInterceptor() == null,
                "interceptor cannot be used with TRANSACTIONAL_SNAPSHOT atomicity mode");

            // Disable in-memory evictions for mvcc cache. TODO IGNITE-10738
            String memPlcName = cc.getDataRegionName();
            DataRegion dataRegion = ctx.cache().context().database().dataRegion(memPlcName);

            if (dataRegion != null && !dataRegion.config().isPersistenceEnabled() &&
                dataRegion.config().getPageEvictionMode() != DataPageEvictionMode.DISABLED) {
                throw new IgniteCheckedException("Data pages evictions cannot be used with TRANSACTIONAL_SNAPSHOT " +
                    "cache atomicity mode for in-memory regions. Please, either disable evictions or enable " +
                    "persistence for data regions with TRANSACTIONAL_SNAPSHOT caches. [cacheName=" + cc.getName() +
                    ", dataRegionName=" + memPlcName + ", pageEvictionMode=" +
                    dataRegion.config().getPageEvictionMode() + ']');
            }

            IndexingSpi idxSpi = ctx.config().getIndexingSpi();

            apply(assertParameter,idxSpi == null || idxSpi instanceof NoopIndexingSpi,
                "Custom IndexingSpi cannot be used with TRANSACTIONAL_SNAPSHOT atomicity mode");
        }

        if (cc.isWriteBehindEnabled() && ctx.discovery().cacheAffinityNode(ctx.discovery().localNode(), cc.getName())) {
            if (cfgStore == null)
                throw new IgniteCheckedException("Cannot enable write-behind (writer or store is not provided) " +
                    "for cache: " + U.maskName(cc.getName()));

            apply(assertParameter,cc.getWriteBehindBatchSize() > 0, "writeBehindBatchSize > 0");
            apply(assertParameter,cc.getWriteBehindFlushSize() >= 0, "writeBehindFlushSize >= 0");
            apply(assertParameter,cc.getWriteBehindFlushFrequency() >= 0, "writeBehindFlushFrequency >= 0");
            apply(assertParameter,cc.getWriteBehindFlushThreadCount() > 0, "writeBehindFlushThreadCount > 0");

            if (cc.getWriteBehindFlushSize() == 0 && cc.getWriteBehindFlushFrequency() == 0)
                throw new IgniteCheckedException("Cannot set both 'writeBehindFlushFrequency' and " +
                    "'writeBehindFlushSize' parameters to 0 for cache: " + U.maskName(cc.getName()));
        }

        if (cc.isReadThrough() && cfgStore == null
            && ctx.discovery().cacheAffinityNode(ctx.discovery().localNode(), cc.getName()))
            throw new IgniteCheckedException("Cannot enable read-through (loader or store is not provided) " +
                "for cache: " + U.maskName(cc.getName()));

        if (cc.isWriteThrough() && cfgStore == null
            && ctx.discovery().cacheAffinityNode(ctx.discovery().localNode(), cc.getName()))
            throw new IgniteCheckedException("Cannot enable write-through (writer or store is not provided) " +
                "for cache: " + U.maskName(cc.getName()));

        long delay = cc.getRebalanceDelay();

        if (delay != 0) {
            if (cc.getCacheMode() != PARTITIONED)
                U.warn(log, "Rebalance delay is supported only for partitioned caches (will ignore): " + (cc.getName()));
            else if (cc.getRebalanceMode() == SYNC) {
                if (delay < 0) {
                    U.warn(log, "Ignoring SYNC rebalance mode with manual rebalance start (node will not wait for " +
                        "rebalancing to be finished): " + U.maskName(cc.getName()));
                }
                else {
                    U.warn(log, "Using SYNC rebalance mode with rebalance delay (node will wait until rebalancing is " +
                        "initiated for " + delay + "ms) for cache: " + U.maskName(cc.getName()));
                }
            }
        }

        ctx.coordinators().validateCacheConfiguration(cc);

        if (cc.getAtomicityMode() == ATOMIC)
            apply(assertParameter,cc.getTransactionManagerLookupClassName() == null,
                "transaction manager can not be used with ATOMIC cache");

        if ((cc.getEvictionPolicyFactory() != null || cc.getEvictionPolicy() != null) && !cc.isOnheapCacheEnabled())
            throw new IgniteCheckedException("Onheap cache must be enabled if eviction policy is configured [cacheName="
                + U.maskName(cc.getName()) + "]");

        if (cacheType != CacheType.DATA_STRUCTURES && DataStructuresProcessor.isDataStructureCache(cc.getName()))
            throw new IgniteCheckedException("Using cache names reserved for datastructures is not allowed for " +
                "other cache types [cacheName=" + cc.getName() + ", cacheType=" + cacheType + "]");

        if (cacheType != CacheType.DATA_STRUCTURES && DataStructuresProcessor.isReservedGroup(cc.getGroupName()))
            throw new IgniteCheckedException("Using cache group names reserved for datastructures is not allowed for " +
                "other cache types [cacheName=" + cc.getName() + ", groupName=" + cc.getGroupName() +
                ", cacheType=" + cacheType + "]");

        // Make sure we do not use sql schema for system views.
        if (ctx.query().moduleEnabled()) {
            String schema = QueryUtils.normalizeSchemaName(cc.getName(), cc.getSqlSchema());

            if (F.eq(schema, QueryUtils.SCHEMA_SYS)) {
                if (cc.getSqlSchema() == null) {
                    // Conflict on cache name.
                    throw new IgniteCheckedException("SQL schema name derived from cache name is reserved (" +
                        "please set explicit SQL schema name through CacheConfiguration.setSqlSchema() or choose " +
                        "another cache name) [cacheName=" + cc.getName() + ", schemaName=" + cc.getSqlSchema() + "]");
                }
                else {
                    // Conflict on schema name.
                    throw new IgniteCheckedException("SQL schema name is reserved (please choose another one) [" +
                        "cacheName=" + cc.getName() + ", schemaName=" + cc.getSqlSchema() + ']');
                }
            }
        }

        if (cc.isEncryptionEnabled() && !ctx.clientNode()) {
            StringBuilder cacheSpec = new StringBuilder("[cacheName=").append(cc.getName())
                .append(", groupName=").append(cc.getGroupName())
                .append(", cacheType=").append(cacheType)
                .append(']');

            if (!CU.isPersistentCache(cc, c.getDataStorageConfiguration())) {
                throw new IgniteCheckedException("Using encryption is not allowed" +
                    " for not persistent cache " + cacheSpec.toString());
            }

            EncryptionSpi encSpi = c.getEncryptionSpi();

            if (encSpi == null) {
                throw new IgniteCheckedException("EncryptionSpi should be configured to use encrypted cache " +
                    cacheSpec.toString());
            }

            if (cc.getDiskPageCompression() != DiskPageCompression.DISABLED)
                throw new IgniteCheckedException("Encryption cannot be used with disk page compression " +
                    cacheSpec.toString());
        }

        Collection<QueryEntity> ents = cc.getQueryEntities();

        if (ctx.discovery().discoCache() != null) {
            boolean nonDfltPrecScaleExists = ents.stream().anyMatch(
                e -> !F.isEmpty(e.getFieldsPrecision()) || !F.isEmpty(e.getFieldsScale()));

            if (nonDfltPrecScaleExists) {
                ClusterNode oldestNode = ctx.discovery().discoCache().oldestServerNode();

                if (PRECISION_SCALE_SINCE_VER.compareTo(oldestNode.version()) > 0) {
                    throw new IgniteCheckedException("Non default precision and scale is supported since version 2.7. " +
                        "The node with oldest version [node=" + oldestNode + ']');
                }
            }
        }
    }
    
    private static void apply(BiFunction<Boolean, String, IgniteCheckedException> assertParameter, Boolean x, String y) throws IgniteCheckedException {
        IgniteCheckedException apply = assertParameter.apply(x, y);
        
        if (apply != null)
            throw apply;
    }


    /**
     * @param c Ignite Configuration.
     * @param cc Cache Configuration.
     * @param ctx
     * @return {@code true} if cache is starting on client node and this node is affinity node for the cache.
     */
    private static boolean storesLocallyOnClient(IgniteConfiguration c, CacheConfiguration cc, GridKernalContext ctx) {
        if (c.isClientMode() && c.getDataStorageConfiguration() == null) {
            if (cc.getCacheMode() == LOCAL)
                return true;

            return ctx.discovery().cacheAffinityNode(ctx.discovery().localNode(), cc.getName());

        }
        else
            return false;
    }




    /**
     * @throws IgniteCheckedException if check failed.
     * @param ctx
     * @param log
     */
    static void checkConsistency(GridKernalContext ctx, IgniteLogger log) throws IgniteCheckedException {
        Collection<ClusterNode> rmtNodes = ctx.discovery().remoteNodes();

        boolean changeablePoolSize =
            IgniteFeatures.allNodesSupports(ctx, rmtNodes, IgniteFeatures.DIFFERENT_REBALANCE_POOL_SIZE);

        for (ClusterNode n : rmtNodes) {
            if (Boolean.TRUE.equals(n.attribute(ATTR_CONSISTENCY_CHECK_SKIPPED)))
                continue;

            if(!changeablePoolSize)
                checkRebalanceConfiguration(n, ctx);

            checkTransactionConfiguration(n, ctx, log);

            checkMemoryConfiguration(n, ctx);

            DeploymentMode locDepMode = ctx.config().getDeploymentMode();
            DeploymentMode rmtDepMode = n.attribute(IgniteNodeAttributes.ATTR_DEPLOYMENT_MODE);

            CU.checkAttributeMismatch(log, null, n.id(), "deploymentMode", "Deployment mode",
                locDepMode, rmtDepMode, true);
        }
    }

    /**
     * @param rmt Remote node to check.
     * @param ctx
     * @throws IgniteCheckedException If check failed.
     */
    private static void checkRebalanceConfiguration(ClusterNode rmt, GridKernalContext ctx) throws IgniteCheckedException {
        ClusterNode locNode = ctx.discovery().localNode();

        if (ctx.config().isClientMode() || locNode.isDaemon() || rmt.isClient() || rmt.isDaemon())
            return;

        Integer rebalanceThreadPoolSize = rmt.attribute(IgniteNodeAttributes.ATTR_REBALANCE_POOL_SIZE);

        if (rebalanceThreadPoolSize != null && rebalanceThreadPoolSize != ctx.config().getRebalanceThreadPoolSize()) {
            throw new IgniteCheckedException("Rebalance configuration mismatch (fix configuration or set -D" +
                IGNITE_SKIP_CONFIGURATION_CONSISTENCY_CHECK + "=true system property)." +
                " Different values of such parameter may lead to rebalance process instability and hanging. " +
                " [rmtNodeId=" + rmt.id() +
                ", locRebalanceThreadPoolSize = " + ctx.config().getRebalanceThreadPoolSize() +
                ", rmtRebalanceThreadPoolSize = " + rebalanceThreadPoolSize + "]");
        }
    }


    /**
     * @param rmt Remote node to check.
     * @param ctx
     * @throws IgniteCheckedException If check failed.
     */
    private static void checkTransactionConfiguration(ClusterNode rmt, GridKernalContext ctx, IgniteLogger log) throws IgniteCheckedException {
        TransactionConfiguration rmtTxCfg = rmt.attribute(ATTR_TX_CONFIG);

        if (rmtTxCfg != null) {
            TransactionConfiguration locTxCfg = ctx.config().getTransactionConfiguration();

            checkDeadlockDetectionConfig(rmt, rmtTxCfg, locTxCfg, log);

            checkSerializableEnabledConfig(rmt, rmtTxCfg, locTxCfg);
        }
    }


    /** */
    private static void checkDeadlockDetectionConfig(ClusterNode rmt, TransactionConfiguration rmtTxCfg,
        TransactionConfiguration locTxCfg, IgniteLogger log) {
        boolean locDeadlockDetectionEnabled = locTxCfg.getDeadlockTimeout() > 0;
        boolean rmtDeadlockDetectionEnabled = rmtTxCfg.getDeadlockTimeout() > 0;

        if (locDeadlockDetectionEnabled != rmtDeadlockDetectionEnabled) {
            U.warn(log, "Deadlock detection is enabled on one node and disabled on another. " +
                "Disabled detection on one node can lead to undetected deadlocks. [rmtNodeId=" + rmt.id() +
                ", locDeadlockTimeout=" + locTxCfg.getDeadlockTimeout() +
                ", rmtDeadlockTimeout=" + rmtTxCfg.getDeadlockTimeout());
        }
    }

    /** */
    private static void checkSerializableEnabledConfig(ClusterNode rmt, TransactionConfiguration rmtTxCfg,
        TransactionConfiguration locTxCfg) throws IgniteCheckedException {
        if (locTxCfg.isTxSerializableEnabled() != rmtTxCfg.isTxSerializableEnabled())
            throw new IgniteCheckedException("Serializable transactions enabled mismatch " +
                "(fix txSerializableEnabled property or set -D" + IGNITE_SKIP_CONFIGURATION_CONSISTENCY_CHECK + "=true " +
                "system property) [rmtNodeId=" + rmt.id() +
                ", locTxSerializableEnabled=" + locTxCfg.isTxSerializableEnabled() +
                ", rmtTxSerializableEnabled=" + rmtTxCfg.isTxSerializableEnabled() + ']');
    }

    /**
     * @param rmt Remote node to check.
     * @param ctx
     * @throws IgniteCheckedException If check failed.
     */
    private static void checkMemoryConfiguration(ClusterNode rmt, GridKernalContext ctx) throws IgniteCheckedException {
        ClusterNode locNode = ctx.discovery().localNode();

        if (ctx.config().isClientMode() || locNode.isDaemon() || rmt.isClient() || rmt.isDaemon())
            return;

        DataStorageConfiguration dsCfg = null;

        Object dsCfgBytes = rmt.attribute(IgniteNodeAttributes.ATTR_DATA_STORAGE_CONFIG);

        if (dsCfgBytes instanceof byte[])
            dsCfg = new JdkMarshaller().unmarshal((byte[])dsCfgBytes, U.resolveClassLoader(ctx.config()));

        if (dsCfg == null) {
            // Try to use legacy memory configuration.
            MemoryConfiguration memCfg = rmt.attribute(IgniteNodeAttributes.ATTR_MEMORY_CONFIG);

            if (memCfg != null) {
                dsCfg = new DataStorageConfiguration();

                // All properties that are used in validation should be converted here.
                dsCfg.setPageSize(memCfg.getPageSize());
            }
        }

        if (dsCfg != null) {
            DataStorageConfiguration locDsCfg = ctx.config().getDataStorageConfiguration();

            if (dsCfg.getPageSize() != locDsCfg.getPageSize()) {
                throw new IgniteCheckedException("Memory configuration mismatch (fix configuration or set -D" +
                    IGNITE_SKIP_CONFIGURATION_CONSISTENCY_CHECK + "=true system property) [rmtNodeId=" + rmt.id() +
                    ", locPageSize = " + locDsCfg.getPageSize() + ", rmtPageSize = " + dsCfg.getPageSize() + "]");
            }
        }
    }


    /**
     * @param node Joining node.
     * @param ctx
     * @param map
     * @return Validation result or {@code null} in case of success.
     */
    @Nullable static IgniteNodeValidationResult validateHashIdResolvers(ClusterNode node, GridKernalContext ctx,
        Map<String, DynamicCacheDescriptor> map) {
        if (!node.isClient()) {
            for (DynamicCacheDescriptor desc : map.values()) {
                CacheConfiguration cfg = desc.cacheConfiguration();

                if (cfg.getAffinity() instanceof RendezvousAffinityFunction) {
                    RendezvousAffinityFunction aff = (RendezvousAffinityFunction)cfg.getAffinity();

                    Object nodeHashObj = aff.resolveNodeHash(node);

                    for (ClusterNode topNode : ctx.discovery().aliveServerNodes()) {
                        Object topNodeHashObj = aff.resolveNodeHash(topNode);

                        if (nodeHashObj.hashCode() == topNodeHashObj.hashCode()) {
                            String errMsg = "Failed to add node to topology because it has the same hash code for " +
                                "partitioned affinity as one of existing nodes [cacheName=" +
                                cfg.getName() + ", existingNodeId=" + topNode.id() + ']';

                            String sndMsg = "Failed to add node to topology because it has the same hash code for " +
                                "partitioned affinity as one of existing nodes [cacheName=" +
                                cfg.getName() + ", existingNodeId=" + topNode.id() + ']';

                            return new IgniteNodeValidationResult(topNode.id(), errMsg, sndMsg);
                        }
                    }
                }
            }
        }

        return null;
    }

    /** Invalid region configuration message. */
    private static final String INVALID_REGION_CONFIGURATION_MESSAGE = "Failed to join node " +
        "(Incompatible data region configuration [region=%s, locNodeId=%s, isPersistenceEnabled=%s, rmtNodeId=%s, isPersistenceEnabled=%s])";



    /**
     * @param rmtNode Joining node.
     * @param ctx
     * @return List of validation errors.
     */
    static List<String> validateRmtRegions(ClusterNode rmtNode, GridKernalContext ctx) {
        List<String> errorMessages = new ArrayList<>();

        DataStorageConfiguration rmtStorageCfg = extractDataStorage(rmtNode, ctx);
        Map<String, DataRegionConfiguration> rmtRegionCfgs = dataRegionCfgs(rmtStorageCfg);

        DataStorageConfiguration locStorageCfg = ctx.config().getDataStorageConfiguration();

        if (isDefaultDataRegionPersistent(locStorageCfg) != isDefaultDataRegionPersistent(rmtStorageCfg)) {
            errorMessages.add(String.format(
                INVALID_REGION_CONFIGURATION_MESSAGE,
                "DEFAULT",
                ctx.localNodeId(),
                isDefaultDataRegionPersistent(locStorageCfg),
                rmtNode.id(),
                isDefaultDataRegionPersistent(rmtStorageCfg)
            ));
        }

        for (ClusterNode clusterNode : ctx.discovery().aliveServerNodes()) {
            Map<String, DataRegionConfiguration> nodeRegionCfg = dataRegionCfgs(extractDataStorage(clusterNode, ctx));

            for (Map.Entry<String, DataRegionConfiguration> nodeRegionCfgEntry : nodeRegionCfg.entrySet()) {
                String regionName = nodeRegionCfgEntry.getKey();

                DataRegionConfiguration rmtRegionCfg = rmtRegionCfgs.get(regionName);

                if (rmtRegionCfg != null && rmtRegionCfg.isPersistenceEnabled() != nodeRegionCfgEntry.getValue().isPersistenceEnabled())
                    errorMessages.add(String.format(
                        INVALID_REGION_CONFIGURATION_MESSAGE,
                        regionName,
                        ctx.localNodeId(),
                        nodeRegionCfgEntry.getValue().isPersistenceEnabled(),
                        rmtNode.id(),
                        rmtRegionCfg.isPersistenceEnabled()
                    ));
            }
        }

        return errorMessages;
    }


    /**
     * @param rmtNode Remote node to check.
     * @param ctx
     * @return Data storage configuration
     */
    private static DataStorageConfiguration extractDataStorage(ClusterNode rmtNode, GridKernalContext ctx) {
        return GridCacheUtils.extractDataStorage(
            rmtNode,
            ctx.marshallerContext().jdkMarshaller(),
            U.resolveClassLoader(ctx.config())
        );
    }

    /**
     * @param dataStorageCfg User-defined data regions.
     */
    private static Map<String, DataRegionConfiguration> dataRegionCfgs(DataStorageConfiguration dataStorageCfg) {
        if(dataStorageCfg != null) {
            return Optional.ofNullable(dataStorageCfg.getDataRegionConfigurations())
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .collect(Collectors.toMap(DataRegionConfiguration::getName, e -> e));
        }

        return Collections.emptyMap();
    }


    /**
     * Checks that preload-order-dependant caches has SYNC or ASYNC preloading mode.
     *
     * @param cfgs Caches.
     * @return Maximum detected preload order.
     * @throws IgniteCheckedException If validation failed.
     */
    private static int validatePreloadOrder(CacheConfiguration[] cfgs) throws IgniteCheckedException {
        int maxOrder = 0;

        for (CacheConfiguration cfg : cfgs) {
            int rebalanceOrder = cfg.getRebalanceOrder();

            if (rebalanceOrder > 0) {
                if (cfg.getCacheMode() == LOCAL)
                    throw new IgniteCheckedException("Rebalance order set for local cache (fix configuration and restart the " +
                        "node): " + U.maskName(cfg.getName()));

                if (cfg.getRebalanceMode() == CacheRebalanceMode.NONE)
                    throw new IgniteCheckedException("Only caches with SYNC or ASYNC rebalance mode can be set as rebalance " +
                        "dependency for other caches [cacheName=" + U.maskName(cfg.getName()) +
                        ", rebalanceMode=" + cfg.getRebalanceMode() + ", rebalanceOrder=" + cfg.getRebalanceOrder() + ']');

                maxOrder = Math.max(maxOrder, rebalanceOrder);
            }
            else if (rebalanceOrder < 0)
                throw new IgniteCheckedException("Rebalance order cannot be negative for cache (fix configuration and restart " +
                    "the node) [cacheName=" + cfg.getName() + ", rebalanceOrder=" + rebalanceOrder + ']');
        }

        return maxOrder;
    }

}
