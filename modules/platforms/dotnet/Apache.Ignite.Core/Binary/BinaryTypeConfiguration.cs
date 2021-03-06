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

namespace Apache.Ignite.Core.Binary
{
    using System;
    using Apache.Ignite.Core.Impl.Binary;
    using Apache.Ignite.Core.Impl.Common;

    /// <summary>
    /// Binary type configuration.
    /// </summary>
    public class BinaryTypeConfiguration
    {
        /// <summary>
        /// Constructor.
        /// </summary>
        public BinaryTypeConfiguration()
        {
            // No-op.
        }

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="typeName">Type name.</param>
        public BinaryTypeConfiguration(string typeName)
        {
            TypeName = typeName;
        }

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="type">Type.</param> 
        public BinaryTypeConfiguration(Type type)
        {
            IgniteArgumentCheck.NotNull(type, "type");

            TypeName = type.AssemblyQualifiedName;
            IsEnum = BinaryUtils.IsIgniteEnum(type);
        }

        /// <summary>
        /// Copying constructor.
        /// </summary>
        /// <param name="cfg">Configuration to copy.</param>
        public BinaryTypeConfiguration(BinaryTypeConfiguration cfg)
        {
            IgniteArgumentCheck.NotNull(cfg, "cfg");

            AffinityKeyFieldName = cfg.AffinityKeyFieldName;
            IdMapper = cfg.IdMapper;
            NameMapper = cfg.NameMapper;
            Serializer = cfg.Serializer;
            TypeName = cfg.TypeName;
            KeepDeserialized = cfg.KeepDeserialized;
            IsEnum = cfg.IsEnum;
        }

        /// <summary>
        /// Fully qualified type name. 
        /// </summary>
        public string TypeName { get; set; }

        /// <summary>
        /// Name mapper for the given type. 
        /// </summary>
        public IBinaryNameMapper NameMapper { get; set; }

        /// <summary>
        /// ID mapper for the given type. When it is necessary to resolve class (field) ID, then 
        /// this property will be checked first. 
        /// Otherwise, ID will be hash code of the class (field) simple name in lower case. 
        /// </summary>
        public IBinaryIdMapper IdMapper { get; set; }

        /// <summary>
        /// Serializer for the given type. If not provided and class implements <see cref="IBinarizable" />
        /// then its custom logic will be used. If not provided and class doesn't implement <see cref="IBinarizable" />
        /// then all fields of the class except of those with [NotSerialized] attribute will be serialized
        /// with help of reflection.
        /// </summary>
        public IBinarySerializer Serializer { get; set; }

        /// <summary>
        /// Affinity key field name.
        /// </summary>
        public string AffinityKeyFieldName { get; set; }

        /// <summary>
        /// Keep deserialized flag. If set to non-null value, overrides default value set in 
        /// <see cref="BinaryTypeConfiguration"/>.
        /// </summary>
        public bool? KeepDeserialized { get; set; }

        /// <summary>
        /// Gets or sets a value indicating whether this instance describes an enum type.
        /// </summary>
        public bool IsEnum { get; set; }

        /// <summary>
        /// Returns a string that represents the current object.
        /// </summary>
        /// <returns>
        /// A string that represents the current object.
        /// </returns>
        public override string ToString()
        {
            return
                string.Format(
                    "{0} [TypeName={1}, NameMapper={2}, IdMapper={3}, Serializer={4}, AffinityKeyFieldName={5}, " +
                    "KeepDeserialized={6}, IsEnum={7}]",
                    typeof (BinaryTypeConfiguration).Name, TypeName, NameMapper, IdMapper, Serializer,
                    AffinityKeyFieldName, KeepDeserialized, IsEnum);
        }
    }
}
