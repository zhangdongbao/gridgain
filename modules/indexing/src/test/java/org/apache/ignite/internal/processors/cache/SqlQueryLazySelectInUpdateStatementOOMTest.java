package org.apache.ignite.internal.processors.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.junit.Test;

/**
 * Update SQL statements generate synthetic SELECT first, which may cause to OOM. This test checks that OOM will not
 * happen in case of lazy DELETE query, because lazy flag will be inherited in SELECT statement from parent query
 */
public class SqlQueryLazySelectInUpdateStatementOOMTest {

    @Test
    public void testDeleteQueryDoesNotCauseOOM() {
        long mem = Runtime.getRuntime().maxMemory();

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("foo");
        cfg.setPeerClassLoadingEnabled(true);
        DataStorageConfiguration memCfg = new DataStorageConfiguration().setDefaultDataRegionConfiguration(
            new DataRegionConfiguration().setMaxSize(mem * 10));
        cfg.setDataStorageConfiguration(memCfg);
        cfg.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(
            new TcpDiscoveryVmIpFinder().setAddresses(Collections.singletonList("127.0.0.1:47500..47509")))
        );

        try (Ignite ignite = Ignition.start(cfg)) {
            CacheConfiguration<Long, Entry> configuration = new CacheConfiguration<>("test_cache");
            configuration.setIndexedTypes(Long.class, Entry.class);

            IgniteCache<Long, Entry> cache = ignite.getOrCreateCache(configuration);

            List<String> val = new ArrayList<>();
            for (int i = 0; i < 500; i++)
                val.add("12345678");

            System.out.println("populating cache");
            for (int i = 0; i < mem / 4000; i++)
                cache.put(new Long(i), new Entry(val));
            System.out.println("cache populated");

            System.out.println("launching delete query");
            SqlFieldsQuery qry = new SqlFieldsQuery("delete from Entry");
            qry.setLazy(true);
            cache.query(qry).getAll();
            System.out.println("delete performed");
        }

    }

    private static class Entry {
        final List<String> list;

        Entry(List<String> list) {
            this.list = list;
        }
    }
}
