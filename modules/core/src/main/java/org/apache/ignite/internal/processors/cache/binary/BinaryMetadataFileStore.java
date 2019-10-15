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
package org.apache.ignite.internal.processors.cache.binary;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.failure.FailureContext;
import org.apache.ignite.failure.FailureType;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteInterruptedCheckedException;
import org.apache.ignite.internal.binary.BinaryMetadata;
import org.apache.ignite.internal.binary.BinaryUtils;
import org.apache.ignite.internal.processors.cache.persistence.file.FileIO;
import org.apache.ignite.internal.processors.cache.persistence.file.FileIOFactory;
import org.apache.ignite.internal.util.future.GridFinishedFuture;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.util.worker.GridWorker;
import org.apache.ignite.thread.IgniteThread;
import org.jetbrains.annotations.Nullable;

/**
 * Class handles saving/restoring binary metadata to/from disk.
 *
 * Current implementation needs to be rewritten as it issues IO operations from discovery thread
 * which may lead to segmentation of nodes from cluster.
 */
class BinaryMetadataFileStore {
    /** Link to resolved binary metadata directory. Null for non persistent mode */
    private File workDir;

    /** */
    private final ConcurrentMap<Integer, BinaryMetadataHolder> metadataLocCache;

    /** */
    private final GridKernalContext ctx;

    /** */
    private FileIOFactory fileIOFactory;

    /** */
    private final IgniteLogger log;

    /** */
    private BinaryMetadataWriter writer;

    /**
     * @param metadataLocCache Metadata locale cache.
     * @param ctx Context.
     * @param log Logger.
     * @param binaryMetadataFileStoreDir Path to binary metadata store configured by user, should include binary_meta and consistentId
     */
    BinaryMetadataFileStore(
        final ConcurrentMap<Integer, BinaryMetadataHolder> metadataLocCache,
        final GridKernalContext ctx,
        final IgniteLogger log,
        @Nullable final File binaryMetadataFileStoreDir
    ) throws IgniteCheckedException {
        this.metadataLocCache = metadataLocCache;
        this.ctx = ctx;
        this.log = log;

        if (!CU.isPersistenceEnabled(ctx.config()))
            return;

        fileIOFactory = ctx.config().getDataStorageConfiguration().getFileIOFactory();

        if (binaryMetadataFileStoreDir != null)
            workDir = binaryMetadataFileStoreDir;
        else {
            final String subFolder = ctx.pdsFolderResolver().resolveFolders().folderName();

            workDir = new File(U.resolveWorkDirectory(
                ctx.config().getWorkDirectory(),
                "binary_meta",
                false
            ),
                subFolder);
        }

        U.ensureDirectory(workDir, "directory for serialized binary metadata", log);
        writer = new BinaryMetadataWriter();

        new IgniteThread(writer).start();
    }

    /**
     * @param binMeta Binary metadata to be written to disk.
     */
    void writeMetadata(BinaryMetadata binMeta) {
        if (!CU.isPersistenceEnabled(ctx.config()))
            return;

        try {
            File file = new File(workDir, binMeta.typeId() + ".bin");

            byte[] marshalled = U.marshal(ctx, binMeta);

            try (final FileIO out = fileIOFactory.create(file)) {
                int left = marshalled.length;
                while ((left -= out.writeFully(marshalled, 0, Math.min(marshalled.length, left))) > 0)
                    ;

                out.force();
            }
        }
        catch (Exception e) {
            final String msg = "Failed to save metadata for typeId: " + binMeta.typeId() +
                "; exception was thrown: " + e.getMessage();

            U.error(log, msg);

            ctx.failure().process(new FailureContext(FailureType.CRITICAL_ERROR, e));

            throw new IgniteException(msg, e);
        }
    }

    /**
     * Restores metadata on startup of {@link CacheObjectBinaryProcessorImpl} but before starting discovery.
     */
    void restoreMetadata() {
        if (!CU.isPersistenceEnabled(ctx.config()))
            return;

        for (File file : workDir.listFiles()) {
            try (FileInputStream in = new FileInputStream(file)) {
                BinaryMetadata meta = U.unmarshal(ctx.config().getMarshaller(), in, U.resolveClassLoader(ctx.config()));

                metadataLocCache.put(meta.typeId(), new BinaryMetadataHolder(meta, 0, 0));
            }
            catch (Exception e) {
                U.warn(log, "Failed to restore metadata from file: " + file.getName() +
                    "; exception was thrown: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if binary metadata for the same typeId is already presented on disk.
     * If so merges it with new metadata and stores the result.
     * Otherwise just writes new metadata.
     *
     * @param binMeta new binary metadata to write to disk.
     */
    void mergeAndWriteMetadata(BinaryMetadata binMeta) {
        BinaryMetadata existingMeta = readMetadata(binMeta.typeId());

        if (existingMeta != null) {
            BinaryMetadata mergedMeta = BinaryUtils.mergeMetadata(existingMeta, binMeta);

            writeMetadata(mergedMeta);
        } else
            writeMetadata(binMeta);
    }

    /**
     * Reads binary metadata for given typeId.
     *
     * @param typeId typeId of BinaryMetadata to be read.
     */
    private BinaryMetadata readMetadata(int typeId) {
        File file = new File(workDir, Integer.toString(typeId) + ".bin");

        if (!file.exists())
            return null;

        try (FileInputStream in = new FileInputStream(file)) {
            return U.unmarshal(ctx.config().getMarshaller(), in, U.resolveClassLoader(ctx.config()));
        }
        catch (Exception e) {
            U.warn(log, "Failed to restore metadata from file: " + file.getName() +
                "; exception was thrown: " + e.getMessage());
        }

        return null;
    }

    /**
     *
     */
    IgniteInternalFuture<Void> writeMetadataAsync(BinaryMetadata meta, int typeVer) {
        if (!CU.isPersistenceEnabled(ctx.config()))
            return null;

        writer.submit(new WriteOpTask(meta, typeVer));

        return new GridFinishedFuture<>();
    }

    private final class WriteOpTask {
        private final BinaryMetadata meta;

        private final int typeVer;

        private WriteOpTask(BinaryMetadata meta, int ver) {
            this.meta = meta;
            typeVer = ver;
        }
    }

    public void waitForWriteOp(int typeId, int typeVer) throws IgniteCheckedException {
        if (typeVer < 0)
            return;

        GridFutureAdapter fut = writeOpFutures.get(new WriteOpSync(typeId, typeVer));

        if (fut != null)
            fut.get();
    }

    private final ConcurrentHashMap<WriteOpSync, GridFutureAdapter> writeOpFutures = new ConcurrentHashMap<>();

    private class BinaryMetadataWriter extends GridWorker {

        /** */
        private final BlockingQueue<WriteOpTask> queue = new LinkedBlockingQueue();

        protected BinaryMetadataWriter() {
            super(ctx.igniteInstanceName(), "binary-metadata-writer", BinaryMetadataFileStore.this.log, ctx.workersRegistry());
        }

        public void submit(WriteOpTask task) {
            //TODO graceful shutdown
            writeOpFutures.put(new WriteOpSync(task.meta.typeId(), task.typeVer), new GridFutureAdapter());

            queue.add(task);
        }

        @Override protected void body() throws InterruptedException, IgniteInterruptedCheckedException {
           while (!isCancelled()) {
               try {
                   body0();
               }
               catch (InterruptedException e) {
                   if (!isCancelled) {
                       ctx.failure().process(new FailureContext(FailureType.SYSTEM_WORKER_TERMINATION, e));

                       throw e;
                   }
               }
           }
        }

        private void body0() throws InterruptedException {
            WriteOpTask task;

            blockingSectionBegin();

            try {
                task = queue.take();
            }
            finally {
                blockingSectionEnd();
            }

            writeMetadata(task.meta);

            GridFutureAdapter fut = writeOpFutures.remove(new WriteOpSync(task.meta.typeId(), task.typeVer));

            if (fut != null)
                fut.onDone();
        }
    }

    private final class WriteOpSync {
        /** */
        private final int typeId;

        /** */
        private final int typeVer;

        private WriteOpSync(int typeId, int typeVer) {
            this.typeId = typeId;
            this.typeVer = typeVer;
        }
    }
}
