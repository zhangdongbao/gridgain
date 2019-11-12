package org.apache.ignite.internal.commandline.cache;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.client.GridClient;
import org.apache.ignite.internal.client.GridClientConfiguration;
import org.apache.ignite.internal.commandline.Command;
import org.apache.ignite.internal.commandline.CommandArgIterator;
import org.apache.ignite.internal.commandline.TaskExecutor;
import org.apache.ignite.internal.commandline.argument.CommandArg;
import org.apache.ignite.internal.commandline.argument.CommandArgUtils;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.cache.VisorListIndexesResult;
import org.apache.ignite.internal.visor.cache.index.IndexListTaskArg;

public class IndexesList implements Command<IndexesList.CdmArgs> {

    /** Exception message. */
    public static final String CACHES_GROUPS_OR_INDEXES_WERE_SPECIFIED_MESSAGE = "Arguments " + IndexListComand.GRP_NAME
        + ", " + IndexListComand.CACHE_NAME
        + ", " + IndexListComand.IDX_NAME + " are mutually exclusive.";

    /** Command parsed arguments. */
    private CdmArgs args;

    /** Logger. */
    private Logger logger;

    @Override public Object execute(GridClientConfiguration clientCfg, Logger logger) throws Exception {
        this.logger = logger;


        //String nodeId = args.nodeId();


        Map<ClusterNode, VisorListIndexesResult> res = null;

        try (GridClient client = Command.startClient(clientCfg)) {
            //TODO: list for index names
            IndexListTaskArg taskArg = new IndexListTaskArg(args.nodeIds, args.groups, args.caches, args.indexes);

            TaskExecutor.executeTaskByNameOnNode(client, "org.apache.ignite.internal.visor.cache.index.IndexListTask", taskArg, null, clientCfg);

            //TODO: list for cache groups
            //TODO: list for cache names
            System.out.println("client started");
        }

        return res;
    }

    @Override public CdmArgs arg() {
        return args;
    }

    @Override public void printUsage(Logger logger) {

    }

    @Override public String name() {
        return null;
    }

    @Override public void parseArguments(CommandArgIterator argIterator) {
        Set<String> nodeIds = null;
        Set<String> groups = null;
        Set<String> caches = null;
        Set<String> indexes = null;

        while (argIterator.hasNextSubArg()) {
            String nextArg = argIterator.nextArg("");

            IndexListComand arg = CommandArgUtils.of(nextArg, IndexListComand.class);

            switch (arg) {
                case NODE_ID:
                    if (nodeIds != null)
                        throw new IllegalArgumentException(arg.argName() + " arg specified twice.");

                    nodeIds = argIterator.nextStringSet(arg.argName());
                    break;

                case CACHE_NAME:
                    if (caches != null || groups != null || indexes != null)
                        throw new IllegalArgumentException(CACHES_GROUPS_OR_INDEXES_WERE_SPECIFIED_MESSAGE);

                    caches = argIterator.nextStringSet(arg.argName());

                    break;

                case GRP_NAME:
                    if (caches != null || groups != null || indexes != null)
                        throw new IllegalArgumentException(CACHES_GROUPS_OR_INDEXES_WERE_SPECIFIED_MESSAGE);

                    groups = argIterator.nextStringSet(arg.argName());

                    break;

                case IDX_NAME:
                    if (caches != null || groups != null || indexes != null)
                        throw new IllegalArgumentException(CACHES_GROUPS_OR_INDEXES_WERE_SPECIFIED_MESSAGE);

                    indexes = argIterator.nextStringSet(arg.argName());

                    break;
            }

            groups = argIterator.parseStringSet(nextArg);
        }

        args = new CdmArgs(nodeIds, groups, caches, indexes);
    }

    private enum IndexListComand implements CommandArg {
        /** */
        NODE_ID("--node-id"),

        /** */
        GRP_NAME("--cache-group-name"),

        /** */
        CACHE_NAME("--cache-name"),

        /** */
        IDX_NAME("--index-name");

        /** Option name. */
        private final String name;

        /** */
        IndexListComand(String name) {
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
    public static class CdmArgs {
        /** Groups. */
        private Set<String> groups;

        /** List of caches names. */
        private Set<String> caches;

        /** List of indexes names. */
        private Set<String> indexes;

        /** Node ids. */
        private Set<String> nodeIds;

        /**
         *
         */
        public CdmArgs(Set<String> groups, Set<String> caches, Set<String> indexes, Set<String> nodeIds) {
            this.groups = groups;
            this.indexes = indexes;
            this.caches = caches;
            this.nodeIds = nodeIds;
        }

        /**
         * @return Node id.
         */
        public Set<String> nodeId() {
            return nodeIds;
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
            return S.toString(CdmArgs.class, this);
        }
    }
}
