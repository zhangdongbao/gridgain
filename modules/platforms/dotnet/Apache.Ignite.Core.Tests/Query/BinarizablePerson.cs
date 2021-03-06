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

namespace Apache.Ignite.Core.Tests.Query
{
    using Apache.Ignite.Core.Binary;

    /// <summary>
    /// Test person.
    /// </summary>
    internal class BinarizablePerson : IBinarizable
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="BinarizablePerson"/> class.
        /// </summary>
        /// <param name="name">The name.</param>
        /// <param name="age">The age.</param>
        public BinarizablePerson(string name, int age)
        {
            Name = name;
            Age = age;
        }

        /// <summary>
        /// Gets or sets the name.
        /// </summary>
        public string Name { get; set; }

        /// <summary>
        /// Gets or sets the address.
        /// </summary>
        public string Address { get; set; }

        /// <summary>
        /// Gets or sets the age.
        /// </summary>
        public int Age { get; set; }

        /** <ineritdoc /> */
        public void WriteBinary(IBinaryWriter writer)
        {
            writer.WriteString("name", Name);
            writer.WriteString("address", Address);
            writer.WriteInt("age", Age);
        }

        /** <ineritdoc /> */
        public void ReadBinary(IBinaryReader reader)
        {
            Name = reader.ReadString("name");
            Address = reader.ReadString("address");
            Age = reader.ReadInt("age");
        }
    }
}
