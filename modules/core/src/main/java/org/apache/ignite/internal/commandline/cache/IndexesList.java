package org.apache.ignite.internal.commandline.cache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.client.GridClient;
import org.apache.ignite.internal.client.GridClientConfiguration;
import org.apache.ignite.internal.commandline.Command;
import org.apache.ignite.internal.commandline.CommandArgIterator;
import org.apache.ignite.internal.commandline.argument.CommandArg;
import org.apache.ignite.internal.commandline.argument.CommandArgUtils;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.cache.VisorListIndexesResult;

public class IndexesList implements Command<IndexesList.Arguments> {

    /** Exception message. */
    public static final String CACHES_GROUPS_OR_INDEXES_WERE_SPECIFIED_MESSAGE = "Arguments " + IndexexListComand.GRP_NAME
        + ", " + IndexexListComand.CACHE_NAME
        + ", " + IndexexListComand.IDX_NAME + " cannot use together.";

    /** Command parsed arguments. */
    private Arguments args;

    /** Logger. */
    private Logger logger;

    @Override public Object execute(GridClientConfiguration clientCfg, Logger logger) throws Exception {
        this.logger = logger;

        String nodeId = args.nodeId();


        Map<ClusterNode, VisorListIndexesResult> res = null;

        try (GridClient client = Command.startClient(clientCfg)) {

        }

        return res;
    }

    @Override public Arguments arg() {
        return args;
    }

    @Override public void printUsage(Logger logger) {

    }

    @Override public String name() {
        return null;
    }

    @Override public void parseArguments(CommandArgIterator argIterator) {
        String nodeId = null;
        Set<String> groups = null;
        Set<String> caches = null;
        Set<String> indexes = null;

        while (argIterator.hasNextSubArg()) {
            String nextArg = argIterator.nextArg("");

            IndexexListComand arg = CommandArgUtils.of(nextArg, IndexexListComand.class);

            switch (arg) {
                case NODE_ID:
                    nodeId = argIterator.nextArg("");
                    break;

                case CACHE_NAME:
                    if (caches != null || groups != null || indexes != null)
                        throw new IllegalArgumentException(CACHES_GROUPS_OR_INDEXES_WERE_SPECIFIED_MESSAGE);

                    caches = new HashSet<>();

                    caches.addAll(argIterator.nextStringSet(""));

                    break;

                case GRP_NAME:
                    if (caches != null || groups != null || indexes != null)
                        throw new IllegalArgumentException(CACHES_GROUPS_OR_INDEXES_WERE_SPECIFIED_MESSAGE);

                    groups = new HashSet<>();

                    groups.addAll(argIterator.nextStringSet(""));

                    break;

                case IDX_NAME:
                    if (caches != null || groups != null || indexes != null)
                        throw new IllegalArgumentException(CACHES_GROUPS_OR_INDEXES_WERE_SPECIFIED_MESSAGE);

                    indexes = new HashSet<>();

                    indexes.addAll(argIterator.nextStringSet(""));

                    break;

            }

            groups = argIterator.parseStringSet(nextArg);
        }

        args = new Arguments(nodeId, groups, caches, indexes);
    }

    private enum IndexexListComand implements CommandArg {
        /**
         *
         */
        NODE_ID("--node-id"),

        /**
         *
         */
        GRP_NAME("--cache-group-name"),

        /**
         *
         */
        CACHE_NAME("--cache-name"),

        /**
         *
         */
        IDX_NAME("--index-name");

        /** Option name. */
        private final String name;

        /**
         *
         */
        IndexexListComand(String name) {
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
        /** Groups. */
        private Set<String> groups;

        /** List of caches names. */
        private Set<String> caches;

        /** List of indexes names. */
        private Set<String> indexes;

        /** Node id. */
        private String nodeId;

        /**
         *
         */
        public Arguments(String nodeId, Set<String> groups, Set<String> caches, Set<String> indexes) {
            this.groups = groups;
            this.indexes = indexes;
            this.caches = caches;
            this.nodeId = nodeId;
        }

        /**
         * @return Node id.
         */
        public String nodeId() {
            return nodeId;
        }

        /**
         * @return Cache group to scan for, null means scanning all groups.
         */
        public Set<String> groups() {
            return groups;
        }

        /**
         * @return List of caches names.
         */
        public Set<String> caches() {
            return caches;
        }

        /**
         * @return List of indexes names.
         */
        public Set<String> indexes() {
            return indexes;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(Arguments.class, this);
        }
    }
}
