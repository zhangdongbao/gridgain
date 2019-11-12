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

package org.apache.ignite.internal.visor.cache.index;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ignite.IgniteException;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorMultiNodeTask;
import org.apache.ignite.internal.visor.VisorTaskArgument;
import org.jetbrains.annotations.Nullable;

public class IndexListTask extends VisorMultiNodeTask<IndexListTaskArg, IndexListTaskResult, IndexListJobResult> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorJob<IndexListTaskArg, IndexListJobResult> job(IndexListTaskArg arg) {
        //TODO: implement
        return new IndexListJob(arg, debug);
    }

    /** {@inheritDoc} */
    @Override protected IndexListTaskResult reduce0(List<ComputeJobResult> results) throws IgniteException {
        //TODO: implement
        return null;
    }

    @Override protected Collection<UUID> jobNodes(VisorTaskArgument<IndexListTaskArg> arg) {
        //TODO: parse .* for nodes here
        //TODO: support regex here for nodes
        return arg.getNodes();
    }

    private static class IndexListJob extends VisorJob<IndexListTaskArg, IndexListJobResult> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with specified argument.
         *
         * @param arg Job argument.
         * @param debug Flag indicating whether debug information should be printed into node log.
         */
        protected IndexListJob(
            @Nullable IndexListTaskArg arg, boolean debug) {
            super(arg, debug);
        }

        @Override protected IndexListJobResult run(@Nullable IndexListTaskArg arg) throws IgniteException {
            //TODO: doing actual work here, but this can go to separate closure;
            System.out.println("It's alive: " + ignite.localNode().id());
            return null;
        }
    }
}
