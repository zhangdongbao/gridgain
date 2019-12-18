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

package org.apache.ignite.internal.pagemem.wal.record;

import java.util.Base64;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.internal.processors.cache.CacheObjectValueContext;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Interface for Data Entry for automatic unwrapping key and value from Data Entry
 */
public interface UnwrappedDataEntry {
    /**
     * Unwraps key value from cache key object into primitive boxed type or source class. If client classes were used in
     * key, call of this method requires classes to be available in classpath.
     *
     * @return Key which was placed into cache. Or null if failed to convert.
     */
    Object unwrappedKey();

    /**
     * Unwraps value value from cache value object into primitive boxed type or source class. If client classes were
     * used in key, call of this method requires classes to be available in classpath.
     *
     * @return Value which was placed into cache. Or null for delete operation or for failure.
     */
    Object unwrappedValue();

    /**
     * Returns a string representation of the entry.
     *
     * @param entry          Object to get a string presentation for.
     * @param superToString  String representation of parent.
     * @param cacheObjValCtx Cache object value context. Context is used for unwrapping objects.
     * @param <T>            Composite type: extends DataEntry implements UnwrappedDataEntry
     * @return String presentation of the given object.
     */
    public static <T extends DataEntry & UnwrappedDataEntry> String toString(
        T entry,
        String superToString,
        final CacheObjectValueContext cacheObjValCtx
    ) {
        final Object key = entry.unwrappedKey();

        String keyStr;
        if (key instanceof String) {
            keyStr = (String)key;
        }
        else if (key instanceof BinaryObject) {
            keyStr = key.toString();
        }
        else {
            keyStr = (key != null) ? toStringRecursive(key.getClass(), key) : null;
        }

        if (keyStr == null || keyStr.isEmpty()) {
            try {
                keyStr = Base64.getEncoder().encodeToString(entry.key().valueBytes(cacheObjValCtx));
            }
            catch (IgniteCheckedException e) {
                cacheObjValCtx.kernalContext().log(UnwrapDataEntry.class)
                    .error("Unable to convert key [" + entry.key() + "]", e);
            }
        }

        final Object value = entry.unwrappedValue();

        String valueStr;
        if (value instanceof String) {
            valueStr = (String)value;
        }
        else
        if (value instanceof BinaryObject) {
            valueStr = value.toString();
        }
        else {
            valueStr = (value != null) ? toStringRecursive(value.getClass(), value) : null;
        }

        if (valueStr == null || valueStr.isEmpty()) {
            try {
                valueStr = Base64.getEncoder().encodeToString(entry.value().valueBytes(cacheObjValCtx));
            }
            catch (IgniteCheckedException e) {
                cacheObjValCtx.kernalContext().log(UnwrapDataEntry.class)
                    .error("Unable to convert value [" + entry.value() + "]", e);
            }
        }

        return entry.getClass().getSimpleName() + "[k = " + keyStr + ", v = ["
            + valueStr
            + "], super = ["
            + superToString + "]]";
    }

    /**
     * Produces auto-generated output of string presentation for given object (given the whole hierarchy).
     *
     * @param cls Declaration class of the object.
     * @param obj Object to get a string presentation for.
     * @return String presentation of the given object.
     */
    public static String toStringRecursive(Class cls, Object obj) {
        String result = null;
        if (cls != Object.class) {
            result = S.toString(cls, obj, toStringRecursive(cls.getSuperclass(), obj));
        }
        return result;
    }
}
