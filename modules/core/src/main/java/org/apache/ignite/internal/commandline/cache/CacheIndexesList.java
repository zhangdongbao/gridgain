package org.apache.ignite.internal.commandline.cache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.apache.ignite.internal.client.GridClient;
import org.apache.ignite.internal.client.GridClientConfiguration;
import org.apache.ignite.internal.commandline.Command;
import org.apache.ignite.internal.commandline.CommandArgIterator;
import org.apache.ignite.internal.commandline.TaskExecutor;
import org.apache.ignite.internal.commandline.argument.CommandArg;
import org.apache.ignite.internal.commandline.argument.CommandArgUtils;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.cache.index.IndexListInfoContainer;
import org.apache.ignite.internal.visor.cache.index.IndexListTaskArg;

import static org.apache.ignite.internal.commandline.CommandLogger.optional;
import static org.apache.ignite.internal.commandline.cache.CacheCommands.usageCache;
import static org.apache.ignite.internal.commandline.cache.CacheSubcommands.INDEX_LIST;

/**
 * Cache subcommand that allows to show indexes.
 */
public class CacheIndexesList implements Command<CacheIndexesList.Arguments> {
    /** {@inheritDoc} */
    @Override public void printUsage(Logger logger) {
        String desc = "List all indexes that match specified filters.";

        Map<String, String> map = U.newLinkedHashMap(16);

        map.put(IndexListComandArgs.NODE_ID.argName() + "nodeId",
            "Specify node for job execution. If not specified explicitly, node will be chosen by grid");
        map.put(IndexListComandArgs.GRP_NAME.argName() + "regExp",
            "Regular expression allowing filtering by cache group name");
        map.put(IndexListComandArgs.CACHE_NAME.argName() + "regExp",
            "Regular expression allowing filtering by cache name");
        map.put(IndexListComandArgs.IDX_NAME.argName() + "regExp",
            "Regular expression allowing filtering by index name");

        usageCache(
            logger,
            INDEX_LIST,
            desc,
            map,
            optional(IndexListComandArgs.NODE_ID),
            optional(IndexListComandArgs.GRP_NAME),
            optional(IndexListComandArgs.CACHE_NAME),
            optional(IndexListComandArgs.IDX_NAME)
        );
    }

    /** Command parsed arguments. */
    private Arguments args;

    /** {@inheritDoc} */
    @Override public Object execute(GridClientConfiguration clientCfg, Logger logger) throws Exception {
        Set<IndexListInfoContainer> taskRes;

        IndexListTaskArg taskArg = new IndexListTaskArg(args.groupsRegEx, args.cachesRegEx, args.indexesRegEx);

        try (GridClient client = Command.startClient(clientCfg)) {
            taskRes = TaskExecutor.executeTaskByNameOnNode(client, "org.apache.ignite.internal.visor.cache.index.IndexListTask", taskArg, args.nodeId, clientCfg);
        }

        printIndexes(taskRes, logger);

        return taskRes;
    }

    /** {@inheritDoc} */
    @Override public Arguments arg() {
        return args;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return INDEX_LIST.text().toUpperCase();
    }

    /** {@inheritDoc} */
    @Override public void parseArguments(CommandArgIterator argIterator) {
        UUID nodeId = null;
        String groupsRegEx = null;
        String cachesRegEx = null;
        String indexesRegEx = null;

        while (argIterator.hasNextSubArg()) {
            String nextArg = argIterator.nextArg("");

            IndexListComandArgs arg = CommandArgUtils.of(nextArg, IndexListComandArgs.class);

            switch (arg) {
                case NODE_ID:
                    if (nodeId != null)
                        throw new IllegalArgumentException(arg.argName() + " arg specified twice.");

                    nodeId = UUID.fromString(nextArg);

                    break;

                case GRP_NAME:
                    groupsRegEx = argIterator.nextArg("Failed to read group name regex");

                    break;

                case CACHE_NAME:
                    cachesRegEx = argIterator.nextArg("Failed to read cache name regex");

                    break;

                case IDX_NAME:
                    indexesRegEx = argIterator.nextArg("Failed to read index name regex");

                    break;

                default:
                    throw new IllegalArgumentException("Unknown argument: " + arg.argName());
            }
        }

        args = new Arguments(nodeId, groupsRegEx, cachesRegEx, indexesRegEx);
    }

    /** */
    private enum IndexListComandArgs implements CommandArg {
        /** */
        NODE_ID("--node-id"),

        /** */
        GRP_NAME("--group-name"),

        /** */
        CACHE_NAME("--cache-name"),

        /** */
        IDX_NAME("--index-name");

        /** Option name. */
        private final String name;

        /** */
        IndexListComandArgs(String name) {
            this.name = name;
        }

        /** {@inheritDoc} */
        @Override public String argName() {
            return name;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return name;
        }
    }

    /**
     * Container for command arguments.
     */
    public static class Arguments {
        /** Node id. */
        private UUID nodeId;

        /** Cache groups regex. */
        private String groupsRegEx;

        /** Cache names regex. */
        private String cachesRegEx;

        /** Indexes names regex. */
        private String indexesRegEx;

        /** */
        public Arguments(UUID nodeId, String groupsRegEx, String cachesRegEx, String indexesRegEx) {
            this.nodeId = nodeId;
            this.groupsRegEx = groupsRegEx;
            this.indexesRegEx = indexesRegEx;
            this.cachesRegEx = cachesRegEx;
        }

        /**
         * @return Node id.
         */
        public UUID nodeId() {
            return nodeId;
        }

        /**
         * @return Cache group to scan for, null means scanning all groups.
         */
        public String groups() {
            return groupsRegEx;
        }

        /**
         * @return List of caches names.
         */
        public String cachesRegEx() {
            return cachesRegEx;
        }

        /**
         * @return List of indexes names.
         */
        public String indexesRegEx() {
            return indexesRegEx;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(Arguments.class, this);
        }

    }

    /**
     * Prints indexes info.
     *
     * @param res Set of indexes info to print.
     * @param logger Logger to use.
     */
    private void printIndexes(Set<IndexListInfoContainer> res, Logger logger) {
        List<IndexListInfoContainer> sorted = new ArrayList<>(res);

        sorted.sort(Comparator.comparing(IndexListInfoContainer::groupName)
                              .thenComparing(IndexListInfoContainer::cacheName)
                              .thenComparing(IndexListInfoContainer::indexName));

        String prevGrpName = "";

        for (IndexListInfoContainer container : sorted) {
            if (!prevGrpName.equals(container.groupName())) {
                prevGrpName = container.groupName();

                logger.info("");
            }

            String containerStr = container.toString();
            String noClsString = containerStr.substring(IndexListInfoContainer.class.getSimpleName().length() + 2,
                                                        containerStr.length() - 1);

            logger.info(noClsString);
        }

        logger.info("");
    }
}
