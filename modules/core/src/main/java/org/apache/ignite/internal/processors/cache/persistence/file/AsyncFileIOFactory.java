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

package org.apache.ignite.internal.processors.cache.persistence.file;

import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;

/**
 * File I/O factory which uses {@link AsynchronousFileChannel} based implementation of FileIO.
 */
public class AsyncFileIOFactory implements FileIOFactory {
    /** */
    private static final long serialVersionUID = 0L;

    /** Thread local channel future holder. */
    private transient volatile ThreadLocal<AsyncFileIO.ChannelOpFuture> holder = initHolder();

    /** {@inheritDoc} */
    @Override public FileIO create(File file, OpenOption... modes) throws IOException {
        if (holder == null) {
            synchronized (this) {
                if (holder == null)
                    holder = initHolder();
            }
        }

        return new AsyncFileIO(file, holder, modes);
    }

    /**
     * Initializes thread local channel future holder.
     */
    private ThreadLocal<AsyncFileIO.ChannelOpFuture> initHolder() {
        return new ThreadLocal<AsyncFileIO.ChannelOpFuture>() {
            @Override protected AsyncFileIO.ChannelOpFuture initialValue() {
                return new AsyncFileIO.ChannelOpFuture();
            }
        };
    }
}