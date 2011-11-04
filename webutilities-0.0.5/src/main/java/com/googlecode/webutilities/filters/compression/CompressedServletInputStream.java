/*
 *
 *  Copyright 2011 Rajendra Patil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.googlecode.webutilities.filters.compression;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

final class CompressedServletInputStream extends ServletInputStream {

    private final InputStream compressedStream;

    private boolean closed;

    CompressedServletInputStream(InputStream inputStream, EncodedStreamsFactory encodedStreamsFactory) throws IOException {
        this.compressedStream = encodedStreamsFactory.getCompressedStream(inputStream).getCompressedInputStream();
    }

    public int read() throws IOException {
        assertOpen();
        return compressedStream.read();
    }

    public int read(byte[] b) throws IOException {
        assertOpen();
        return compressedStream.read(b);
    }

    public int read(byte[] b, int offset, int length) throws IOException {
        assertOpen();
        return compressedStream.read(b, offset, length);
    }

    public long skip(long n) throws IOException {
        assertOpen();
        return compressedStream.skip(n);
    }

    public int available() throws IOException {
        assertOpen();
        return compressedStream.available();
    }

    public void close() throws IOException {
        if (!closed) {
            compressedStream.close();
            closed = true;
        }
    }

    public synchronized void mark(int limit) {
        assertOpen();
        compressedStream.mark(limit);
    }

    public synchronized void reset() throws IOException {
        assertOpen();
        compressedStream.reset();
    }

    public boolean markSupported() {
        assertOpen();
        return compressedStream.markSupported();
    }

    private void assertOpen() {
        if (closed) {
            throw new IllegalStateException("Stream has been already closed.");
        }
    }

}
