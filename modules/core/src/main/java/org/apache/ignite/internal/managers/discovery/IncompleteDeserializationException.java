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

package org.apache.ignite.internal.managers.discovery;

import org.jetbrains.annotations.NotNull;

/**
 * Exception which can be used to access a message which failed to be deserialized completely using Java serialization.
 * Throwed from deserialization methods it can be caught by a caller.
 * <p>
 * Should be {@link RuntimeException} because of limitations of Java serialization mechanisms.
 * <p>
 * Catching {@link ClassNotFoundException} inside deserialization methods cannot do the same trick because
 * Java deserialization remembers such exception internally and will rethrow it anyway upon returing to a user.
 */
public class IncompleteDeserializationException extends RuntimeException {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private final DiscoveryCustomMessage m;

    /**
     * @param m Message.
     */
    public IncompleteDeserializationException(@NotNull DiscoveryCustomMessage m) {
        super(null, null, false, false);

        this.m = m;
    }

    /**
     * @return Message.
     */
    @NotNull public DiscoveryCustomMessage message() {
        return m;
    }
}
