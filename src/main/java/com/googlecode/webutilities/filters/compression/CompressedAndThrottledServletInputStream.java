/*
 * Copyright 2010-2014 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.webutilities.filters.compression;

import com.googlecode.webutilities.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

final class CompressedAndThrottledServletInputStream extends ServletInputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressedAndThrottledServletInputStream.class.getName());

    private static final long SLEEP_DURATION_MS = 50;

    private final InputStream compressedStream;

    private boolean closed;

    private long readRatePerSecond;

    private final long startTime;

    private long maxBytesToRead;

    private long bytesRead;

    private long currentReadRate;

    CompressedAndThrottledServletInputStream(InputStream inputStream, EncodedStreamsFactory encodedStreamsFactory,
                                             long allowedBytesPerSecond, long maxBytesToRead) throws IOException {
        this.readRatePerSecond = allowedBytesPerSecond > 0 ? allowedBytesPerSecond : Constants.DEFAULT_DECOMPRESS_BYTES_PER_SECOND;
        this.compressedStream = encodedStreamsFactory.getCompressedStream(inputStream).getCompressedInputStream();
        this.startTime = System.currentTimeMillis();
        this.bytesRead = 0;
        this.maxBytesToRead = maxBytesToRead;
    }

    public int read() throws IOException {
        assertOpen();
        assertReadRate();
        int count = compressedStream.read();
        assertReadSize(count);
        if (count > 0) {
            bytesRead += count;
        }
        return count;
    }

    private void assertReadSize(int count) throws IOException {
        if ((bytesRead + count) > maxBytesToRead) { //we crossed the allowed size
            this.close();
            throw new IOException("The request size is larger than allowed limit of " + maxBytesToRead + " bytes");
        }
    }

    private void assertReadRate() throws IOException {
        this.calculateReadRate();
        if (this.currentReadRate > this.readRatePerSecond) {
            try {
                Thread.sleep(SLEEP_DURATION_MS);
            } catch (InterruptedException e) {
                this.close();
                throw new IOException("Thread aborted", e);
            }
        }
    }

    public void calculateReadRate() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        if (elapsed == 0) {
            currentReadRate = bytesRead;
        } else {
            currentReadRate = bytesRead / elapsed;
        }
    }

    public int read(byte[] b) throws IOException {
        assertOpen();
        assertReadRate();
        int count = compressedStream.read(b);
        assertReadSize(count);
        if (count > 0) {
            bytesRead += count;
        }
        return count;
    }

    public int read(byte[] b, int offset, int length) throws IOException {
        assertOpen();
        assertReadRate();
        int count = compressedStream.read(b, offset, length);
        assertReadSize(count);
        if (count > 0) {
            bytesRead += count;
        }
        return count;
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
            LOGGER.debug("Finished reading {} bytes @ average read rate: {} bytes/sec", bytesRead, currentReadRate);
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

    @Override
    public boolean isFinished() {
        return bytesRead >= maxBytesToRead;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new RuntimeException("Not yet implemented");
    }
}
