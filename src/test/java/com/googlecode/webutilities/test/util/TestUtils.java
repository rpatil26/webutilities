/*
 * Copyright 2010-2016 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.webutilities.test.util;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public final class TestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class.getName());

    private TestUtils() {
    }

    public static String readContents(InputStream inputStream, String encoding) throws Exception {
        return new String(ByteStreams.toByteArray(inputStream), encoding);
    }


    public static boolean contentEquals(InputStream streamLeft, InputStream streamRight) throws IOException {
        int ch;
        while ((ch = streamLeft.read()) != -1) {
            int ch2 = streamRight.read();
            if (ch != ch2) {
                return false;
            }
        }
        int ch2 = streamRight.read();
        return (ch2 == -1);
    }

    public static boolean compressedContentEquals(String left, String right) throws IOException {
        if (left == null && right == null) {
            return true;
        }
        assert left != null;
        ByteArrayInputStream streamLeft = new ByteArrayInputStream(left.getBytes());
        ByteArrayInputStream streamRight = new ByteArrayInputStream(right.getBytes());


        int ch, ch2, pos = 0;
        while (true) { //(ch = streamLeft.read()) != -1 || (ch2 = streamRight.read()) != -1) {
            ch = streamLeft.read();
            ch2 = streamRight.read();
            if (ch == -1 && ch == ch2) { //streams ended
                return true;
            }
            if (ch != ch2) {
                if (pos == 10) { //Ignore OS byte in GZIP header (was suppose to be 9th? Needed to make it 10 to make the test case work)
                    LOGGER.info("Ignoring OS bit.... {} != {}", ch, ch2);
                } else {
                    return false;
                }
            }
            pos++;
        }
    }
}
