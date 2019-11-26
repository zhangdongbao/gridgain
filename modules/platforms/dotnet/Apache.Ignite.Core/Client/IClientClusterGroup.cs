﻿/*
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

namespace Apache.Ignite.Core.Client
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics.CodeAnalysis;
    using Apache.Ignite.Core.Cluster;

    public interface IClientClusterGroup
    {
        /// <summary>
        /// Creates projection for nodes containing given name and value
        /// specified in user attributes.
        /// </summary>
        /// <param name="name">Name of the attribute.</param>
        /// <param name="val">Optional attribute value to match.</param>
        /// <returns>Grid projection for nodes containing specified attribute.</returns>
        IClientClusterGroup ForAttribute(string name, string val);

        /// <summary>
        /// Creates grid projection for nodes supporting .Net, i.e. for nodes started with Apache.Ignite.exe.
        /// </summary>
        /// <returns>Grid projection for nodes supporting .Net.</returns>
        IClientClusterGroup ForDotNet();

        /// <summary>
        /// Creates a cluster group of nodes started in server mode (<see cref="IgniteConfiguration.ClientMode"/>).
        /// </summary>
        /// <returns>Cluster group of nodes started in server mode.</returns>
        IClientClusterGroup ForServers();

        /// <summary>
        /// Creates a grid projection which includes all nodes that pass the given predicate filter.
        /// </summary>
        /// <param name="p">Predicate filter for nodes to include into this projection.</param>
        /// <returns>Grid projection for nodes that passed the predicate filter.</returns>
        IClientClusterGroup ForPredicate(Func<IClusterNode, bool> p);

        /// <summary>
        /// Gets read-only collections of nodes in this projection.
        /// </summary>
        /// <returns>All nodes in this projection.</returns>
        [SuppressMessage("Microsoft.Design", "CA1024:UsePropertiesWhereAppropriate", Justification = "Semantics.")]
        ICollection<IClusterNode> GetNodes();

        /// <summary>
        /// Gets a node for given ID from this grid projection.
        /// </summary>
        /// <param name="id">Node ID.</param>
        /// <returns>Node with given ID from this projection or null if such node does not 
        /// exist in this projection.</returns>
        IClusterNode GetNode(Guid id);

        /// <summary>
        /// Gets first node from the list of nodes in this projection.
        /// </summary>
        /// <returns>Node.</returns>
        [SuppressMessage("Microsoft.Design", "CA1024:UsePropertiesWhereAppropriate", Justification = "Semantics.")]
        IClusterNode GetNode();
    }
}