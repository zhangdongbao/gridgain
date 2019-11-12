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

package org.apache.ignite.internal.visor.cache.index;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;
import org.apache.ignite.internal.util.typedef.internal.U;

//TODO: consider moving this to indexing module
/**
 * Argument object for {@code IndexListTask}
 */
public class IndexListTaskArg extends IgniteDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /**
     * Empty constructor required for Serializable.
     */
    public IndexListTaskArg() {
    }

    /** */
    Set<String> nodeIds;

    /** */
    Set<String> groups;

    /** */
    Set<String> caches;

    /** */
    Set<String> indexes;

    /**
     * @param nodeIds Node ids.
     * @param groups Groups.
     * @param caches Caches.
     * @param indexes Indexes.
     */
    public IndexListTaskArg(Set<String> nodeIds, Set<String> groups, Set<String> caches, Set<String> indexes) {
        this.nodeIds = nodeIds;
        this.groups  = groups;
        this.caches  = caches;
        this.indexes = indexes;
    }

    //TODO: now this shit duplicates IndexingList.CmdArgs. This can be, but not necessery. Should it be like this?
    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeCollection(out, nodeIds);
        U.writeCollection(out, groups);
        U.writeCollection(out, caches);
        U.writeCollection(out, indexes);
    }

    /** {@inheritDoc} */
    @Override
    protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        nodeIds = U.readSet(in);
        groups  = U.readSet(in);
        caches  = U.readSet(in);
        indexes = U.readSet(in);
    }
}
