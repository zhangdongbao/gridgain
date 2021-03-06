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

package org.apache.ignite.transactions.spring;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * Spring transaction test.
 */
public class GridSpringTransactionManagerSelfTest extends GridSpringTransactionManagerAbstractTest {
    /** */
    private GridSpringTransactionService service;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        CacheConfiguration cache = new CacheConfiguration(DEFAULT_CACHE_NAME);

        cache.setName(CACHE_NAME);
        cache.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        cfg.setCacheConfiguration(cache);

        return cfg;
    }

    @Override public IgniteCache<Integer, String> cache() {
        return grid().cache(CACHE_NAME);
    }

    @Override public GridSpringTransactionService service() {
        return service;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrid();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        ApplicationContext appCtx = new GenericXmlApplicationContext("config/spring-transactions.xml");
        service = (GridSpringTransactionService)appCtx.getBean("gridSpringTransactionService");
    }
}
