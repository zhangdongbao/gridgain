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

package org.apache.ignite.internal.processors.query;

import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.Nullable;

/**
 * Query type name key.
 */
public class QueryTypeNameKey {
    /** */
    private final String cacheName;

    /** */
    private final String typeName;

    /**
     * @param cacheName Cache name.
     * @param typeName Type name.
     */
    public QueryTypeNameKey(@Nullable String cacheName, String typeName) {
        assert !F.isEmpty(typeName) : typeName;

        this.cacheName = cacheName;
        this.typeName = typeName;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        QueryTypeNameKey other = (QueryTypeNameKey)o;

        return (cacheName != null ? cacheName.equals(other.cacheName) : other.cacheName == null) &&
            typeName.equals(other.typeName);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return 31 * (cacheName != null ? cacheName.hashCode() : 0) + typeName.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(QueryTypeNameKey.class, this);
    }
}
