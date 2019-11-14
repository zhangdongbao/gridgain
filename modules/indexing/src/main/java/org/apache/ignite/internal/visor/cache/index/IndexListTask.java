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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.cache.CacheGroupContext;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.query.GridQueryProcessor;
import org.apache.ignite.internal.processors.query.GridQueryTypeDescriptor;
import org.apache.ignite.internal.processors.query.h2.IgniteH2Indexing;
import org.apache.ignite.internal.processors.query.h2.database.H2TreeIndexBase;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2Table;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorOneNodeTask;
import org.h2.index.Index;
import org.jetbrains.annotations.Nullable;

public class IndexListTask extends VisorOneNodeTask<IndexListTaskArg, Map<String, Set<String>>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected IndexListJob job(IndexListTaskArg arg) {
        return new IndexListJob(arg, debug);
    }

    /** */
    private static class IndexListJob extends VisorJob<IndexListTaskArg, Map<String, Set<String>>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with specified argument.
         *
         * @param arg Job argument.
         * @param debug Flag indicating whether debug information should be printed into node log.
         */
        protected IndexListJob(@Nullable IndexListTaskArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected Map<String, Set<String>> run(@Nullable IndexListTaskArg arg) throws IgniteException {
            Set<String> idxNames = arg.indexes();
            Set<String> grpNames = arg.groups();
            Set<String> cacheNames = arg.caches();

            Map<String, Set<String>> idxMap = new HashMap<>();

            //TODO: doing actual work here, but this can go to separate closure;
            Collection<CacheGroupContext> cacheGroups = ignite.context().cache().cacheGroups();

            GridQueryProcessor qry = ignite.context().query();

            IgniteH2Indexing indexing = (IgniteH2Indexing)qry.getIndexing();

            for (CacheGroupContext grpCtx: cacheGroups) {
                //TODO: support regex
                if (grpNames != null && !grpNames.contains(grpCtx.name()))
                    continue;

                for (GridCacheContext ctx : grpCtx.caches()) {
                    final String cacheName = ctx.name();

                    //TODO: support regex
                    if (cacheNames != null && !cacheNames.contains(cacheName))
                        continue;

                    Collection<GridQueryTypeDescriptor> types = qry.types(cacheName);

                    // TODO: why isEmpty??
                    if (!F.isEmpty(types)) {
                        for (GridQueryTypeDescriptor type : types) {
                            GridH2Table gridH2Tbl = indexing.schemaManager().dataTable(cacheName, type.tableName());

                            if (gridH2Tbl == null)
                                continue;

                            ArrayList<Index> indexes = gridH2Tbl.getIndexes();

                            for (Index idx : indexes) {
                                //TODO: support regex
                                if (idxNames != null && !idxNames.contains(idx.getName()))
                                    continue;

                                if (idx instanceof H2TreeIndexBase) {
                                    //TODO: does this shit actually work?
                                    Set<String> cacheIndexes = idxMap.get(cacheName);

                                    if (cacheIndexes == null) {
                                        cacheIndexes = new HashSet<>();
                                        idxMap.put(cacheName, cacheIndexes);
                                    }
                                    else
                                        cacheIndexes.add(idx.getName());
                                }
                            }
                        }
                    }
                }
            }

            return idxMap;
        }
    }
}
