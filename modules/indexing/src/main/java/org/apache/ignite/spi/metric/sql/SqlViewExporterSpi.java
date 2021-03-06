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

package org.apache.ignite.spi.metric.sql;

import java.util.function.Predicate;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.metric.MetricRegistry;
import org.apache.ignite.internal.processors.query.QueryUtils;
import org.apache.ignite.internal.processors.query.h2.IgniteH2Indexing;
import org.apache.ignite.internal.processors.query.h2.SchemaManager;
import org.apache.ignite.spi.IgniteSpiAdapter;
import org.apache.ignite.spi.IgniteSpiContext;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.metric.MetricExporterSpi;
import org.apache.ignite.spi.metric.ReadOnlyMetricRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * This SPI implementation exports metrics as SQL views.
 */
public class SqlViewExporterSpi extends IgniteSpiAdapter implements MetricExporterSpi {
    /** System view name. */
    public static final String SYS_VIEW_NAME = "METRICS";

    /** Metric filter. */
    private @Nullable Predicate<MetricRegistry> filter;

    /** Metric Registry. */
    private ReadOnlyMetricRegistry mreg;

    /** {@inheritDoc} */
    @Override protected void onContextInitialized0(IgniteSpiContext spiCtx) throws IgniteSpiException {
        GridKernalContext ctx = ((IgniteEx)ignite()).context();

        SchemaManager mgr = ((IgniteH2Indexing)ctx.query().getIndexing()).schemaManager();

        try {
            mgr.createSystemView(QueryUtils.SCHEMA_MONITORING, new MetricSetLocalSystemView(ctx, mreg, filter));
        }
        catch (IgniteCheckedException e) {
            throw new IgniteSpiException(e);
        }

        if (log.isDebugEnabled())
            log.debug(SYS_VIEW_NAME + " SQL view for metrics created.");
    }

    /** {@inheritDoc} */
    @Override public void spiStart(@Nullable String igniteInstanceName) throws IgniteSpiException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void spiStop() throws IgniteSpiException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void setMetricRegistry(ReadOnlyMetricRegistry mreg) {
        this.mreg = mreg;
    }

    /** {@inheritDoc} */
    @Override public void setExportFilter(Predicate<MetricRegistry> filter) {
        this.filter = filter;
    }

}
