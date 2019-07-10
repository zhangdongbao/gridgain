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

package org.apache.ignite.ml.inference.storage.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@link ModelStorageProvider} based on local {@link ConcurrentHashMap}.
 */
public class LocalModelStorageProvider implements ModelStorageProvider {
    /** Storage of the files and directories. */
    private final ConcurrentMap<String, FileOrDirectory> storage = new ConcurrentHashMap<>();

    /** Storage of the locks. */
    private final Map<String, WeakReference<Lock>> locks = new HashMap<>();

    /** Reference queue with reference to be cleaned up. */
    private final ReferenceQueue<Lock> refQueue = new ReferenceQueue<>();

    /** {@inheritDoc} */
    @Override public FileOrDirectory get(String key) {
        return storage.get(key);
    }

    /** {@inheritDoc} */
    @Override public void put(String key, FileOrDirectory file) {
        storage.put(key, file);
    }

    /** {@inheritDoc} */
    @Override public void remove(String key) {
        storage.remove(key);
    }

    /** {@inheritDoc} */
    @Override public synchronized Lock lock(String key) {
        WeakReferenceWithCleanUp ref;
        while ((ref = (WeakReferenceWithCleanUp)refQueue.poll()) != null)
            ref.cleanUp();

        Lock lock = new ReentrantLock();
        return locks.computeIfAbsent(key, k -> new WeakReferenceWithCleanUp(key, lock)).get();
    }

    /**
     * Weak reference with clean up. Allows to clean up key associated with weak reference content.
     */
    private class WeakReferenceWithCleanUp extends WeakReference<Lock> {
        /** Key to be cleaned up. */
        private final String key;

        /**
         * Constructs a new instance of weak reference with clean up.
         *
         * @param key Key to be cleaned up.
         * @param referent Reference containing a lock.
         */
        public WeakReferenceWithCleanUp(String key, Lock referent) {
            super(referent, refQueue);
            this.key = key;
        }

        /**
         * Cleans up.
         */
        public void cleanUp() {
            locks.remove(key);
        }
    }
}
