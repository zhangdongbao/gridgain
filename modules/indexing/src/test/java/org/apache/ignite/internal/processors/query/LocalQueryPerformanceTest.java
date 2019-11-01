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

package org.apache.ignite.internal.processors.query;

import java.util.Collections;
import java.util.List;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.cache.index.AbstractIndexingCommonTest;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.testframework.GridTestUtils;
import org.junit.Test;

/**
 * Tests for local query execution in lazy mode.
 */
public class LocalQueryPerformanceTest extends AbstractIndexingCommonTest {
    /** Keys count. */
    private static final int KEY_CNT = 100_000;

    /** Iterations. */
    private static final int ITERS = 1000;

    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        return super.getConfiguration(igniteInstanceName)
            .setDataStorageConfiguration(new DataStorageConfiguration()
                .setDefaultDataRegionConfiguration(new DataRegionConfiguration()
                    .setPersistenceEnabled(true)))
            .setClientMode(igniteInstanceName.startsWith("cli"));
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        cleanPersistenceDir();

        startGrids(3);
        startGrid("cli");

        grid(0).cluster().active(true);

        grid(0).createCache(new CacheConfiguration<Long, Value>()
            .setName("test")
            .setSqlSchema("TEST")
            .setQueryEntities(Collections.singleton(new QueryEntity(Long.class, Value.class)
                .setTableName("TEST")))
            .setBackups(1));

        try (IgniteDataStreamer streamer = grid(0).dataStreamer("test")) {
            for (long i = 0; i < KEY_CNT; ++i)
                streamer.addData(i, new Value(i));
        }
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /**
     */
    @Test
    public void test() throws Exception {
        log.info("+++ BEGIN");

        final String sql = "select * from \n" +
            "test a\n" +
            "inner join table(id LONG=(10,20,30,40,50,60)) FixedTable\n" +
            "where a.id = FixedTable.id";

        IgniteCallable<List<List<?>>> call = new IgniteCallable<List<List<?>>>() {
            @IgniteInstanceResource
            Ignite ign;

            @Override
            public List<List<?>> call() throws Exception {
                return sql((IgniteEx)ign, sql, false).getAll();
            }
        };

        IgniteCallable<List<List<?>>> callLocal = new IgniteCallable<List<List<?>>>() {
            @IgniteInstanceResource
            Ignite ign;

            @Override
            public List<List<?>> call() throws Exception {
                return sql((IgniteEx)ign, sql, true).getAll();
            }
        };

        log.info("+++ Local: " + benchmark(
            () -> {
                List<List<?>> res = grid("cli").compute().affinityCall("test", 40L, callLocal);
                assert !res.isEmpty();
            }
        ) + "ms");

        log.info("+++ non-Local: " + benchmark(
            () -> {
                List<List<?>> res = grid("cli").compute().affinityCall("test", 40L, call);
                assert !res.isEmpty();
            }
        ) + "ms");
    }

    /**
     */
    double benchmark(GridTestUtils.RunnableX r) throws Exception {
        long t0 = U.currentTimeMillis();

        for (int i = 0; i < ITERS; ++i)
            r.runx();

        return (double)(U.currentTimeMillis() - t0) / ITERS;
    }

    /**
     * @param ign Node.
     * @param sql SQL query.
     * @param args Query parameters.
     * @return Results cursor.
     */
    private static FieldsQueryCursor<List<?>> sql(IgniteEx ign, String sql, boolean local, Object ... args) {
        return ign.context().query().querySqlFields(new SqlFieldsQuery(sql)
            .setSchema("TEST")
            .setLocal(local)
            .setArgs(args), false);
    }

    /**
     * This class represents organization object.
     */
    public static class Value {
        /** Organization ID (indexed). */
        @QuerySqlField(index = true)
        private Long id;

        /** Organization name (indexed). */
        @QuerySqlField(index = true)
        private String name;

        /**
         */
        public Value() {
            // No-op.
        }

        /**
         */
        public Value(long id ){
            this.id = id;
            this.name = "name " + id;
        }
    }
}
