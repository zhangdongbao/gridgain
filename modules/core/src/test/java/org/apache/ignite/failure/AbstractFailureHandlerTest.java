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

package org.apache.ignite.failure;

import org.apache.ignite.Ignite;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 * Abstract failure handler test.
 */
public class AbstractFailureHandlerTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected FailureHandler getFailureHandler(String igniteInstanceName) {
        return new DummyFailureHandler();
    }

    /**
     * Gets dummy failure handler for ignite instance.
     *
     * @param ignite Ignite.
     */
    protected static DummyFailureHandler dummyFailureHandler(Ignite ignite) {
        return (DummyFailureHandler)ignite.configuration().getFailureHandler();
    }

    /**
     *
     */
    protected static class DummyFailureHandler extends AbstractFailureHandler {
        /** Failure. */
        private volatile boolean failure;

        /** Failure context. */
        private volatile FailureContext ctx;

        /** {@inheritDoc} */
        @Override protected boolean handle(Ignite ignite, FailureContext failureCtx) {
            failure = true;

            ctx = failureCtx;

            return true;
        }

        /**
         * @return Failure.
         */
        public boolean failure() {
            return failure;
        }

        /**
         * @return Failure context.
         */
        public FailureContext failureContext() {
            return ctx;
        }
    }
}
