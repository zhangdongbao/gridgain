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

package org.apache.ignite.internal.util;

import org.apache.ignite.internal.util.typedef.T2;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concurrent queue that wraps collection of {@code Pair<K, V[]>}
 * The only garantee {@link #poll} provided is sequentially emtify values per key array.
 * i.e. input like: <br>
 * p1 = new Pair<1, [1, 3, 5, 7]> <br>
 * p2 = new Pair<2, [2, 3]> <br>
 * p3 = new Pair<3, [200, 100]> <br>
 * and further sequence of {@code poll} calls may produce output like: <br>
 * [3, 200], [3, 100], [1, 1], [1, 3], [1, 5], [1, 7], [2, 2], [2, 3]
 *
 * @param <K> The type of key in input pair collection.
 * @param <V> The type of value array.
 */
public class GridConcurrentMultiPairQueue<K, V> {
    /** */
    public static final GridConcurrentMultiPairQueue EMPTY =
        new GridConcurrentMultiPairQueue<>(Collections.emptyMap());

    /** Inner holder. */
    private final V[][] vals;

    /** Storage for every array length. */
    private final long[] lenSeq;

    /** Current absolute position. */
    private final AtomicLong pos = new AtomicLong();

    /** Precalculated max position. */
    private final int maxPos;

    /** Keys array. */
    private final K[] keysArr;

    /** */
    public GridConcurrentMultiPairQueue(Map<K, ? extends Collection<V>> items) {
        int pairCnt = (int)items.entrySet().stream().map(Map.Entry::getValue).filter(k -> k.size() > 0).count();

        vals = (V[][])new Object[pairCnt][];

        keysArr = (K[])new Object[pairCnt];

        lenSeq = new long[pairCnt];

        int keyPos = 0;

        int size = -1;

        for (Map.Entry<K, ? extends Collection<V>> p : items.entrySet()) {
            if (p.getValue().size() == 0)
                continue;

            keysArr[keyPos] = p.getKey();

            lenSeq[keyPos] = size += p.getValue().size();

            vals[keyPos++] = (V[])p.getValue().toArray();
        }

        maxPos = size + 1;
    }

    /** */
    public GridConcurrentMultiPairQueue(Collection<T2<K, V[]>> items) {
        int pairCnt = (int)items.stream().map(Map.Entry::getValue).filter(k -> k.length > 0).count();

        vals = (V[][])new Object[pairCnt][];

        keysArr = (K[])new Object[pairCnt];

        lenSeq = new long[pairCnt];

        int keyPos = 0;

        int size = -1;

        for (Map.Entry<K, V[]> p : items) {
            if (p.getValue().length == 0)
                continue;

            keysArr[keyPos] = p.getKey();

            lenSeq[keyPos] = size += p.getValue().length;

            vals[keyPos++] = p.getValue();
        }

        maxPos = size + 1;
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public @Nullable T2<K, V> poll() {
        long absPos = pos.getAndIncrement();

        if (absPos >= maxPos)
            return null;

        int segment = Arrays.binarySearch(lenSeq, absPos);

        segment = segment < 0 ? -segment - 1 : segment;

        int relPos = segment == 0 ? (int)absPos : (int)(absPos - lenSeq[segment - 1] - 1);

        K key = keysArr[segment];

        return new T2<>(key, vals[segment][relPos]);
    }

    /**
     * @return {@code true} if empty.
     */
    public boolean isEmpty() {
        return pos.get() >= maxPos;
    }

    /**
     * @return Constant initialisation size.
     */
    public int initialSize() {
        return maxPos;
    }
}
