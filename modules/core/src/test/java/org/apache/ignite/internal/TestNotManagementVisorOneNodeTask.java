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

package org.apache.ignite.internal;

import java.util.List;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorOneNodeTask;
import org.apache.ignite.internal.visor.VisorTaskArgument;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class TestNotManagementVisorOneNodeTask extends VisorOneNodeTask<VisorTaskArgument, Object> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorNotManagementOneNodeJob job(VisorTaskArgument arg) {
        return new VisorNotManagementOneNodeJob(arg, debug);
    }

    /** {@inheritDoc} */
    @Nullable @Override protected Object reduce0(List<ComputeJobResult> results) {
        return null;
    }

    /**
     * Not management one node visor job.
     */
    private static class VisorNotManagementOneNodeJob extends VisorJob<VisorTaskArgument, Object> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Argument.
         * @param debug Debug flag.
         */
        protected VisorNotManagementOneNodeJob(VisorTaskArgument arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected Object run(VisorTaskArgument arg) {
            return null;
        }
    }
}