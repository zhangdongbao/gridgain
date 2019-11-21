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

package org.apache.ignite.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.util.typedef.F;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.util.GridCommandHandlerIndexingUtils.CacheEntityThreeFields.DOUBLE_NAME;
import static org.apache.ignite.util.GridCommandHandlerIndexingUtils.CacheEntityThreeFields.ID_NAME;
import static org.apache.ignite.util.GridCommandHandlerIndexingUtils.CacheEntityThreeFields.STR_NAME;

/**
 * Utility class for tests.
 */
public class GridCommandHandlerIndexingUtils {
    /** Test cache name. */
    public static final String CACHE_NAME = "persons-cache-vi";

    /** Cache name second. */
    public static final String CACHE_NAME_SECOND = CACHE_NAME + "-second";

    /** Test group name. */
    public static final String GROUP_NAME = "group1";

    /** Test group name. */
    public static final String GROUP_NAME_SECOND = GROUP_NAME + "_second";

    /** Three entries cache name common partition. */
    public static final String THREE_ENTRIES_CACHE_NAME_COMMON_PART = "three_entries";

    /** Private constructor */
    private GridCommandHandlerIndexingUtils() {
        throw new IllegalArgumentException("don't create");
    }

    /**
     * Create and fill cache. Key - integer, value - {@code Person}.
     * <br/>
     * <table class="doctable">
     * <th>Cache parameter</th>
     * <th>Value</th>
     * <tr>
     *     <td>Synchronization mode</td>
     *     <td>{@link CacheWriteSynchronizationMode#FULL_SYNC FULL_SYNC}</td>
     * </tr>
     * <tr>
     *     <td>Atomicity mode</td>
     *     <td>{@link CacheAtomicityMode#ATOMIC ATOMIC}</td>
     * </tr>
     * <tr>
     *     <td>Number of backup</td>
     *     <td>1</td>
     * </tr>
     * <tr>
     *     <td>Query entities</td>
     *     <td>{@link #personEntity()}</td>
     * </tr>
     * <tr>
     *     <td>Affinity</td>
     *     <td>{@link RendezvousAffinityFunction} with exclNeighbors = false, parts = 32</td>
     * </tr>
     * </table>
     *
     *
     * @param ignite Node.
     * @param cacheName Cache name.
     * @param grpName Group name.
     * @see Person
     * */
    public static void createAndFillCache(final Ignite ignite, final String cacheName, final String grpName) {
        assert nonNull(ignite);
        assert nonNull(cacheName);
        assert nonNull(grpName);

        ignite.createCache(new CacheConfiguration<Integer, Person>()
            .setName(cacheName)
            .setGroupName(grpName)
            .setWriteSynchronizationMode(FULL_SYNC)
            .setAtomicityMode(ATOMIC)
            .setBackups(1)
            .setQueryEntities(F.asList(personEntity()))
            .setAffinity(new RendezvousAffinityFunction(false, 32)));

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        try (IgniteDataStreamer<Integer, Person> streamer = ignite.dataStreamer(cacheName)) {
            for (int i = 0; i < 10_000; i++)
                streamer.addData(i, new Person(rand.nextInt(), valueOf(rand.nextLong())));
        }
    }

    /**
     * Creates and fills cache.
     *
     * @param ignite Ignite instance.
     * @param cacheName Cache name.
     * @param grpName Cache group.
     * @param entities Collection of {@link QueryEntity}.
     */
    public static void createAndFillThreeFieldsEntryCache(
        final Ignite ignite,
        final String cacheName,
        final String grpName,
        final Collection<QueryEntity> entities)
    {
        assert nonNull(ignite);
        assert nonNull(cacheName);

        ignite.createCache(new CacheConfiguration<Integer, CacheEntityThreeFields>()
            .setName(cacheName)
            .setGroupName(grpName)
            .setWriteSynchronizationMode(FULL_SYNC)
            .setAtomicityMode(ATOMIC)
            .setBackups(1)
            .setQueryEntities(entities)
            .setAffinity(new RendezvousAffinityFunction(false, 32)));

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        try (IgniteDataStreamer<Integer, CacheEntityThreeFields> streamer = ignite.dataStreamer(cacheName)) {
            for (int i = 0; i < 10_000; i++)
                streamer.addData(i, new CacheEntityThreeFields(rand.nextInt(), valueOf(rand.nextLong()), rand.nextDouble()));
        }
    }

    /**
     * Create query entity.
     */
    private static QueryEntity personEntity() {
        QueryEntity entity = new QueryEntity();

        entity.setKeyType(Integer.class.getName());
        entity.setValueType(Person.class.getName());

        String orgIdField = "orgId";
        String nameField = "name";

        entity.addQueryField(orgIdField, Integer.class.getName(), null);
        entity.addQueryField(nameField, String.class.getName(), null);

        entity.setIndexes(asList(new QueryIndex(nameField), new QueryIndex(orgIdField)));

        return entity;
    }

    /** */
    private static QueryEntity complexIndexEntry() {
        QueryEntity entity = prepareQueryEntiry();

        entity.setIndexes(asList(new QueryIndex(ID_NAME),
                                 new QueryIndex(STR_NAME),
                                 new QueryIndex(asList(STR_NAME, DOUBLE_NAME), QueryIndexType.SORTED)));

        return entity;
    }

    /** */
    private static QueryEntity simpleIndexEntry() {
        QueryEntity entity = prepareQueryEntiry();

        entity.setIndexes(asList(new QueryIndex(ID_NAME),
                                 new QueryIndex(STR_NAME),
                                 new QueryIndex(DOUBLE_NAME)));

        return entity;
    }

    /** */
    private static QueryEntity prepareQueryEntiry() {
        QueryEntity entity = new QueryEntity();

        entity.setKeyType(Integer.class.getName());
        entity.setValueType(CacheEntityThreeFields.class.getName());

        entity.addQueryField(ID_NAME, Integer.class.getName(), null);
        entity.addQueryField(STR_NAME, String.class.getName(), null);
        entity.addQueryField(DOUBLE_NAME, Double.class.getName(), null);

        return entity;
    }

    /**
     * Simple class for tests.
     */
    static class Person implements Serializable {
        /** Id organization. */
        int orgId;

        /** Name organization. */
        String name;

        /**
         * Constructor.
         *
         * @param orgId Organization ID.
         * @param name Name.
         */
        public Person(int orgId, String name) {
            this.orgId = orgId;
            this.name = name;
        }
    }

    /**
     * Simple class for tests. Used for complex indexes.
     */
    static class CacheEntityThreeFields implements Serializable {
        /** */
        public static final String ID_NAME = "id";
        /** */
        public static final String STR_NAME = "strField";
        /** */
        public static final String DOUBLE_NAME = "boubleField";

        /** Id. */
        int id;

        /** String field. */
        String strField;

        /** Double field. */
        double doubleField;

        /** */
        public CacheEntityThreeFields(int id, String strField, double doubleField) {
            this.id = id;
            this.strField = strField;
            this.doubleField = doubleField;
        }
    }

    /**
     * Creates several caches with different indexes. Fills them with random values.
     *
     * @param ignite Ignite instance.
     */
    public static void createAndFillSeveralCaches(final Ignite ignite) {
        createAndFillCache(ignite, CACHE_NAME, GROUP_NAME);

        createAndFillThreeFieldsEntryCache(ignite, "test_" + THREE_ENTRIES_CACHE_NAME_COMMON_PART + "_complex_index",
            GROUP_NAME, asList(complexIndexEntry()));

        createAndFillCache(ignite, CACHE_NAME_SECOND, GROUP_NAME_SECOND);

        createAndFillThreeFieldsEntryCache(ignite, THREE_ENTRIES_CACHE_NAME_COMMON_PART + "_simple_indexes",
            null, asList(simpleIndexEntry()));

        createAndFillThreeFieldsEntryCache(ignite, THREE_ENTRIES_CACHE_NAME_COMMON_PART + "_no_indexes",
            null, Collections.emptyList());
    }
}
