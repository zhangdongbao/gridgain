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

package org.apache.ignite.internal.processors.security.compute.closure;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.internal.processors.security.AbstractRemoteSecurityContextCheckTest;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.testframework.GridTestUtils.IgniteRunnableX;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Testing operation security context when the service task is executed on remote nodes.
 * <p>
 * The initiator node broadcasts a task to 'run' nodes that starts service task. That service task is executed
 * on 'check' nodes and broadcasts a task to 'endpoint' nodes. On every step, it is performed verification that
 * operation security context is the initiator context.
 */
@RunWith(JUnit4.class)
public class ExecutorServiceRemoteSecurityContextCheckTest extends AbstractRemoteSecurityContextCheckTest {
    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGridAllowAll(SRV_INITIATOR);

        startClientAllowAll(CLNT_INITIATOR);

        startGridAllowAll(SRV_RUN);

        startClientAllowAll(CLNT_RUN);

        startGridAllowAll(SRV_CHECK);

        startClientAllowAll(CLNT_CHECK);

        startGridAllowAll(SRV_ENDPOINT);

        startClientAllowAll(CLNT_ENDPOINT);

        G.allGrids().get(0).cluster().active(true);
    }

    /** {@inheritDoc} */
    @Override protected void setupVerifier(Verifier verifier) {
        verifier
            .expect(SRV_RUN, 1)
            .expect(CLNT_RUN, 1)
            .expect(SRV_CHECK, 2)
            .expect(CLNT_CHECK, 2)
            .expect(SRV_ENDPOINT, 4)
            .expect(CLNT_ENDPOINT, 4);
    }

    /** */
    @Test
    public void test() {
        IgniteRunnableX operation = () -> {
            VERIFIER.register();

            Ignite loc = Ignition.localIgnite();

            for (UUID nodeId : nodesToCheck()) {
                ExecutorService svc = loc.executorService(loc.cluster().forNodeId(nodeId));

                svc.submit((Runnable) createRunner()).get();
            }
        };

        runAndCheck(grid(SRV_INITIATOR), operation);
        runAndCheck(grid(CLNT_INITIATOR), operation);
    }
}
