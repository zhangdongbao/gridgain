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

package org.apache.ignite.agent.action.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.agent.dto.action.JobResponse;
import org.apache.ignite.agent.dto.action.Request;
import org.apache.ignite.agent.dto.action.TaskResponse;
import org.apache.ignite.agent.dto.action.query.NextPageQueryArgument;
import org.apache.ignite.agent.dto.action.query.QueryArgument;
import org.apache.ignite.agent.dto.action.query.RunningQueriesArgument;
import org.apache.ignite.agent.dto.action.query.ScanQueryArgument;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.testframework.GridTestUtils;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.apache.ignite.agent.StompDestinationsUtils.buildActionJobResponseDest;
import static org.apache.ignite.agent.StompDestinationsUtils.buildActionRequestTopic;
import static org.apache.ignite.agent.StompDestinationsUtils.buildActionTaskResponseDest;
import static org.apache.ignite.agent.dto.action.Status.COMPLETED;
import static org.apache.ignite.agent.dto.action.Status.FAILED;
import static org.apache.ignite.agent.dto.action.Status.RUNNING;

/**
 * Query actions controller test.
 */
public class QueryActionsControllerTest extends AbstractActionControllerTest {
    /** {@inheritDoc} */
    @Before
    @Override public void startup() throws Exception {
        startup0(3);
    }

    /**
     * Should execute query.
     */
    @Test
    public void shouldExecuteQuery() {
        Request req = new Request()
            .setAction("QueryActions.executeSqlQuery")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument(
                new QueryArgument()
                    .setQueryId("qry")
                    .setQueryText(getCreateQuery() + getInsertQuery(1, 2) + getSelectQuery())
                    .setPageSize(10)
            );

        executeAction(req, (res) -> {
            JobResponse r = F.first(res);

            if (r != null && r.getStatus() == COMPLETED) {
                DocumentContext ctx = parse(r.getResult());

                int id = ctx.read("$[2].rows[0][0]");

                int val = ctx.read("$[2].rows[0][1]");

                return id == 1 && val == 2;
            }

            return false;
        });
    }

    /**
     * Should execute query with parameters.
     */
    @Test
    public void shouldExecuteQueryWithParameters() {
        Request req = new Request()
            .setAction("QueryActions.executeSqlQuery")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument(
                new QueryArgument()
                    .setQueryId("qry")
                    .setQueryText(getCreateQuery() + getInsertQuery(1, 2) + getInsertQuery(2, 3) + getSelectQueryWithParameter())
                    .setPageSize(10)
                    .setParameters(new Object[]{1})
            );

        executeAction(req, (res) -> {
            JobResponse r = F.first(res);

            if (r != null && r.getStatus() == COMPLETED) {
                DocumentContext ctx = parse(r.getResult());

                JSONArray arr = ctx.read("$[3].rows[*]");

                int id = ctx.read("$[3].rows[0][0]");

                int val = ctx.read("$[3].rows[0][1]");

                return arr.size() == 1 && id == 1 && val == 2;
            }

            return false;
        });
    }

    /**
     * Should get next page.
     */
    @Test
    public void shouldGetNextPage() {
        final AtomicReference<String> cursorId = new AtomicReference<>();
        Request req = new Request()
            .setAction("QueryActions.executeSqlQuery")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument(
                new QueryArgument()
                    .setQueryId("qry")
                    .setQueryText(getCreateQuery() + getInsertQuery(1, 2) + getInsertQuery(2, 3) + getSelectQuery())
                    .setPageSize(1)
            );

        executeAction(req, (res) -> {
            JobResponse r = F.first(res);

            if (r != null && r.getStatus() == COMPLETED) {
                DocumentContext ctx = parse(r.getResult());

                JSONArray arr = ctx.read("$[3].rows[*]");

                boolean hasMore = ctx.read("$[3].hasMore");

                cursorId.set(ctx.read("$[3].cursorId"));

                return hasMore && arr.size() == 1;
            }

            return false;
        });

        Request nextPageReq = new Request()
            .setAction("QueryActions.nextPage")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument(
                new NextPageQueryArgument().setQueryId("qry").setCursorId(cursorId.get()).setPageSize(1)
            );

        executeAction(nextPageReq, (res) -> {
            JobResponse r = F.first(res);

            if (r != null && r.getStatus() == COMPLETED) {
                DocumentContext ctx = parse(r.getResult());

                JSONArray arr = ctx.read("$.rows[*]");

                boolean hasMore = ctx.read("$.hasMore");

                int id = ctx.read("$.rows[0][0]");

                int val = ctx.read("$.rows[0][1]");

                return arr.size() == 1 && !hasMore && id == 2 && val == 3;
            }

            return false;
        });
    }

    /**
     * Should cancel query before getting next page.
     */
    @Test
    public void shouldCancelQueryAndCleanup() {
        final AtomicReference<String> cursorId = new AtomicReference<>();
        Request req = new Request()
            .setAction("QueryActions.executeSqlQuery")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument(
                new QueryArgument()
                    .setQueryId("qry")
                    .setQueryText(getCreateQuery() + getInsertQuery(1, 2) + getInsertQuery(2, 3) + getSelectQuery())
                    .setPageSize(1)
            );

        executeAction(req, (res) -> {
            JobResponse r = F.first(res);

            if (r != null && r.getStatus() == COMPLETED) {
                DocumentContext ctx = parse(r.getResult());

                cursorId.set(ctx.read("$[3].cursorId"));

                return true;
            }

            return false;
        });

        Request cancelReq = new Request()
            .setAction("QueryActions.cancel")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument("qry");

        executeAction(cancelReq, (res) -> {
            JobResponse r = F.first(res);

            return r != null && r.getStatus() == COMPLETED;
        });

        Request nextPageReq = new Request()
            .setAction("QueryActions.nextPage")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument(
                new NextPageQueryArgument().setQueryId("qry").setCursorId(cursorId.get()).setPageSize(1)
            );

        executeAction(nextPageReq, (res) -> {
            JobResponse r = F.first(res);

            return r != null && r.getStatus() == FAILED;
        });
    }

    /**
     * Should cancel long running query.
     */
    @Test
    public void shouldCancelLongQuery() {
        StringBuilder qryText = new StringBuilder(getCreateQuery());

        for (int i = 0; i <= 1_000; i++)
            qryText.append(getInsertQuery(i, i + 1));

        qryText.append(getSelectQuery());

        Request req = new Request()
            .setAction("QueryActions.executeSqlQuery")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument(
                new QueryArgument()
                    .setQueryId("qry")
                    .setQueryText(qryText.toString())
                    .setPageSize(1_000)
            );

        template.convertAndSend(buildActionRequestTopic(cluster.id()), req);

        assertWithPoll(
            () -> {
                TaskResponse res = interceptor.getPayload(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

                return res != null && res.getJobCount() == 1;
            }
        );

        Request cancelReq = new Request()
            .setAction("QueryActions.cancel")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument("qry");

        executeAction(cancelReq, (res) -> {
            JobResponse r = F.first(res);

            return r != null && r.getStatus() == COMPLETED;
        });

        assertWithPoll(
            () -> {
                JobResponse res = interceptor.getPayload(buildActionJobResponseDest(cluster.id(), req.getId()), JobResponse.class);
                return res != null && res.getStatus() == FAILED;
            }
        );
    }

    /**
     * Should execute scan query.
     */
    @Test
    public void shouldExecuteScanQuery() {
        IgniteCache<Object, Object> cache = cluster.ignite().createCache("test_cache");
        cache.put("key_1", "value_1");

        Request req = new Request()
            .setAction("QueryActions.executeScanQuery")
            .setNodeIds(singleton(cluster.localNode().id()))
            .setId(UUID.randomUUID())
            .setArgument(
                new ScanQueryArgument()
                    .setCacheName("test_cache")
                    .setQueryId("qry")
                    .setPageSize(1)
            );

        executeAction(req, (res) -> {
            JobResponse r = F.first(res);

            if (r != null && r.getStatus() == COMPLETED) {
                DocumentContext ctx = parse(r.getResult());

                JSONArray arr = ctx.read("$[0].rows[*]");

                boolean hasMore = ctx.read("$[0].hasMore");

                String id = ctx.read("$[0].rows[0][1]");

                String val = ctx.read("$[0].rows[0][3]");

                return arr.size() == 1 && !hasMore && "key_1".equals(id) && "value_1".equals(val);
            }

            return false;
        });
    }

    /**
     *
     */
    @Test
    public void shouldReturnRunningQueriesFromAllNodes() {
        cluster.ignite().createCache(
            new CacheConfiguration<>("TestCache")
                .setIndexedTypes(Integer.class, String.class)
                .setSqlFunctionClasses(GridTestUtils.SqlTestFunctions.class)
        );

        GridTestUtils.SqlTestFunctions.sleepMs = 5_000;

        for (UUID nid : allNodeIds) {
            Request req = new Request()
                .setAction("QueryActions.executeSqlQuery")
                .setNodeIds(singleton(nid))
                .setId(UUID.randomUUID())
                .setArgument(
                    new QueryArgument()
                        .setQueryId(UUID.randomUUID().toString())
                        .setQueryText("SELECT count(*), sleep() FROM \"TestCache\".STRING")
                        .setPageSize(1)
                        .setCacheName("TestCache")
                );

            executeAction(req, (res) -> {
                TaskResponse taskRes = interceptor.getPayload(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

                return taskRes != null && taskRes.getStatus() == RUNNING;
            });
        }

        Request req = new Request()
            .setAction("QueryActions.runningQueries")
            .setId(UUID.randomUUID())
            .setArgument(
                new RunningQueriesArgument()
                    .setDuration(1)
            );

        executeAction(req, (res) -> {
            TaskResponse taskRes = interceptor.getPayload(buildActionTaskResponseDest(cluster.id(), req.getId()), TaskResponse.class);

            if (taskRes != null && taskRes.getStatus() == COMPLETED && taskRes.getJobCount() == 3) {
                List<Map<String, String>> res_1 = (List<Map<String, String>>) res.get(0).getResult();
                List<Map<String, String>> res_2 = (List<Map<String, String>>) res.get(1).getResult();
                List<Map<String, String>> res_3 = (List<Map<String, String>>) res.get(2).getResult();

                return !res_1.isEmpty() && !res_2.isEmpty() && !res_3.isEmpty();
            }

            return false;
        });
    }

    /**
     * @return Create table query string.
     */
    private String getCreateQuery() {
        return getCreateQuery("mc_agent_test_table");
    }

    /**
     * @return Create table query string with special table name.
     */
    private String getCreateQuery(String name) {
        return "CREATE TABLE " + name + " (id int, value int, PRIMARY KEY (id));";
    }

    /**
     * @param id  Id.
     * @param val Value.
     * @return Insert query string.
     */
    private String getInsertQuery(int id, int val) {
        return getInsertQuery("mc_agent_test_table", id, val);
    }

    /**
     * @param name Table name.
     * @param id  ID.
     * @param val Value.
     * @return Insert query string.
     */
    private String getInsertQuery(String name, int id, int val) {
        return String.format("INSERT INTO " + name + " VALUES(%s, %s);", id, val);
    }

    /**
     * @return Select query string.
     */
    private String getSelectQuery() {
        return getSelectQuery("mc_agent_test_table");
    }

    /**
     * @param name Name.
     * @return Select query string.
     */
    private String getSelectQuery(String name) {
        return "SELECT * FROM " + name + " ORDER BY ID;";
    }

    /**
     * @return Select query string.
     */
    private String getSelectQueryWithParameter() {
        return "SELECT * FROM mc_agent_test_table WHERE id = ?;";
    }

    /**
     * @param obj Object.
     */
    private DocumentContext parse(Object obj) {
        try {
            return JsonPath.parse(mapper.writeValueAsString(obj));
        }
        catch (JsonProcessingException e) {
            throw new IgniteException(e);
        }
    }
}
