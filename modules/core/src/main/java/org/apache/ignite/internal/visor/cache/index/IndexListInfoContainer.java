package org.apache.ignite.internal.visor.cache.index;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Collections;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Container for index info.
 */
public class IndexListInfoContainer extends IgniteDataTransferObject {
    /** Requeired for serialization */
    private static final long serialVersionUID = 0L;

    /** Empty group name. */
    public static final String EMPTY_GROUP_NAME = "no_group";

    /** Group name. */
    private String grpName;

    /** Cache name. */
    private String cacheName;

    /** Index name. */
    private String idxName;

    /** Columns names. */
    @GridToStringInclude
    private Collection<String> colsNames;

    /** Table name. */
    private String tblName;

    /**
     * Empty constructor required for Serializable.
     */
    public IndexListInfoContainer() {
        // No-op.
    }

    /** */
    public IndexListInfoContainer(
        GridCacheContext ctx,
        String idxName,
        Collection<String> colsNames,
        String tblName)
    {
        cacheName = ctx.name();

        final String cfgGrpName = ctx.config().getGroupName();
        grpName = cfgGrpName == null ? EMPTY_GROUP_NAME : cfgGrpName;

        this.idxName = idxName;
        this.colsNames = colsNames;
        this.tblName = tblName;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, cacheName);
        U.writeString(out, grpName);
        U.writeString(out, idxName);
        U.writeCollection(out, colsNames);
        U.writeString(out, tblName);
    }

    /** {@inheritDoc} */
    @Override
    protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        cacheName = U.readString(in);
        grpName = U.readString(in);
        idxName = U.readString(in);
        colsNames = U.readCollection(in);
        tblName = U.readString(in);
    }

    /**
     * @param tblName New table name.
     */
    public void tableName(String tblName) {
        this.tblName = tblName;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(IndexListInfoContainer.class, this);
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (!(o instanceof IndexListInfoContainer))
            return false;

        IndexListInfoContainer other = (IndexListInfoContainer)o;

        return cacheName.equals(other.cacheName) && idxName.equals(other.idxName);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return cacheName.hashCode();
    }

    /**
     * @return Cache name.
     */
    public String cacheName() {
        return cacheName;
    }

    /**
     * @return Group name.
     */
    public String groupName() {
        return grpName;
    }

    /**
     * @return Index name.
     */
    public String indexName() {
        return idxName;
    }

    /**
     * @return Columns names.
     */
    public Collection<String> columnsNames() {
        return Collections.unmodifiableCollection(colsNames);
    }

    /**
     * @return Table name.
     */
    public String tableName() {
        return tblName;
    }
}
