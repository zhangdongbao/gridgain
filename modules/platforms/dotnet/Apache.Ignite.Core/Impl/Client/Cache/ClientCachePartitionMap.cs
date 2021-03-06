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

namespace Apache.Ignite.Core.Impl.Client.Cache
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;

    /// <summary>
    /// Partition map for a cache.
    /// </summary>
    internal class ClientCachePartitionMap
    {
        /** Cache id. */
        private readonly int _cacheId;

        /** Array of node id per partition. */
        private readonly IList<Guid> _partitionNodeIds;

        /** Key configuration. */
        private readonly IDictionary<int, int> _keyConfiguration;

        public ClientCachePartitionMap(int cacheId, IList<Guid> partitionNodeIds,
            IDictionary<int, int> keyConfiguration)
        {
            Debug.Assert(partitionNodeIds != null && partitionNodeIds.Count > 0);

            _cacheId = cacheId;
            _keyConfiguration = keyConfiguration;
            _partitionNodeIds = partitionNodeIds;
        }

        public int CacheId
        {
            get { return _cacheId; }
        }

        /// <summary>
        /// Key configuration: map from key type id to affinity key field id.
        /// </summary>
        public IDictionary<int, int> KeyConfiguration
        {
            get { return _keyConfiguration; }
        }

        public IList<Guid> PartitionNodeIds
        {
            get { return _partitionNodeIds; }
        }
    }
}
