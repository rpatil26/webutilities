/*
 * Copyright 2010-2011 Rajendra Patil
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.googlecode.webutilities.test.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

public final class TestUtils {

    //HTTP dates are in one of these format
    //@see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html

    public static final String DATE_PATTERN_RFC_1123 = "EEE, dd MMM yyyy HH:mm:ss z";

    public static final String DATE_PATTERN_RFC_1036 = "EEEEEEEEE, dd-MMM-yy HH:mm:ss z";

    public static final String DATE_PATTERN_ANSI_C = "EEE MMM d HH:mm:ss yyyy";

    public static final String DATE_PATTERN_HTTP_HEADER ="EEE, dd MMM yyyy HH:mm:ss zzz";

    //HTTP locale - US
    public final static Locale DEFAULT_LOCALE_US = Locale.US;

    //HTTP timeZone - GMT
    public final static TimeZone DEFAULT_ZONE_GMT = TimeZone.getTimeZone("GMT");

    private static final Logger logger = Logger.getLogger(TestUtils.class.getName());

    private TestUtils() {
    }

    public static String readContents(InputStream inputStream) throws Exception {

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        inputStream.close();
        return stringBuilder.toString();

    }

    /**
     *
     * @param headerDateString - from request header
     * @return Date object after reading from header string
     */
    public static Date readDateFromHeader(String headerDateString){

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_HTTP_HEADER, DEFAULT_LOCALE_US);
        try{
            return simpleDateFormat.parse(headerDateString);
        }catch (Exception e){
            logger.warning("Date parsing using HTTP header pattern failed.");
        }

        //try another rfc1123
        simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_RFC_1123,DEFAULT_LOCALE_US);
        try{
            return simpleDateFormat.parse(headerDateString);
        }catch (Exception e){
            logger.warning("Date parsing using RFC_1123 pattern failed.");
        }

        //try another rfc1036
        simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_RFC_1036,DEFAULT_LOCALE_US);
        try{
            return simpleDateFormat.parse(headerDateString);
        }catch (Exception e){
            logger.warning("Date parsing using RFC_1036 pattern failed.");
        }

        //try another ansi
        simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_ANSI_C,DEFAULT_LOCALE_US);
        try{
            return simpleDateFormat.parse(headerDateString);
        }catch (Exception e){
            logger.warning("Date is not even ANSI C pattern.");
        }

        return null;
    }

    public static boolean contentEquals(InputStream streamLeft, InputStream streamRight) throws IOException {
        if (!(streamLeft instanceof BufferedInputStream)) {
            streamLeft = new BufferedInputStream(streamLeft);
        }
        if (!(streamRight instanceof BufferedInputStream)) {
            streamRight = new BufferedInputStream(streamRight);
        }
        int ch = streamLeft.read();
        while (-1 != ch) {
            int ch2 = streamRight.read();
            if (ch != ch2) {
                return false;
            }
            ch = streamLeft.read();
        }

        int ch2 = streamRight.read();
        return (ch2 == -1);
    }
}
