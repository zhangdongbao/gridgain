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

package org.apache.ignite.spi;

/**
 * Strategy to calculate next timeout and check if total timeout reached.
 */
public interface TimeoutStrategy {
    /**
     * Get next timeout based on previously timeout calculated by strategy.
     *
     * @return Gets next timeout.
     * @throws IgniteSpiOperationTimeoutException in case of total timeout already breached.
     */
    public long nextTimeout(long currTimeout) throws IgniteSpiOperationTimeoutException;

    /**
     * Get next timeout.
     *
     * @return Get next timeout.
     * @throws IgniteSpiOperationTimeoutException In case of total timeout already breached.
     */
    public default long nextTimeout() throws IgniteSpiOperationTimeoutException {
        return nextTimeout(0);
    }

    /**
     * Check if total timeout will be reached in now() + timeInFut.
     *
     * If timeInFut is 0, will check that timeout already reached.
     *
     * @param timeInFut Some millis in future.
     * @return {@code True} if total timeout will be reached.
     */
    public boolean checkTimeout(long timeInFut);

    /**
     * Check if total timeout will be reached by now.
     *
     * @return {@code True} if total timeout already reached.
     */
    public default boolean checkTimeout() {
        return checkTimeout(0);
    }
}
