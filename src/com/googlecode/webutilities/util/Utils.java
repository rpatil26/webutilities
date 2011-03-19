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

package com.googlecode.webutilities.util;

import java.io.File;

import static com.googlecode.webutilities.common.Constants.*;
/**
 * Common Utilities provider class
 *
 * @author rpatil
 * @version 1.0
 */
public final class Utils {

    /**
     * @param string       string representation of a int which is to be parsed and read from
     * @param defaultValue in case parsing fails or string is null, returns this default value
     * @return int parsed value or the default value in case parsing failed
     */
    public static int readInt(String string, int defaultValue) {
        int returnValue = defaultValue;
        try {
            returnValue = Integer.parseInt(string);
        } catch (Exception e) {
            returnValue = defaultValue;
        }
        return returnValue;
    }


    /**
     * @param string       string representation of a long which is to be parsed and read from
     * @param defaultValue in case parsing fails or string is null, returns this default value
     * @return long parsed value or the default value in case parsing failed
     */
    public static long readLong(String string, long defaultValue) {
        long returnValue = defaultValue;
        try {
            returnValue = Long.parseLong(string);
        } catch (Exception e) {
            returnValue = defaultValue;
        }
        return returnValue;
    }

    /**
     * @param string       string representation of a boolean (true or false) which is to be parsed and read from
     * @param defaultValue in case string is null or does not contain valid boolean, returns this default value
     * @return boolean parsed value or the default value
     */

    public static boolean readBoolean(String string, boolean defaultValue) {

        if (string == null || !string.toLowerCase().matches("true|false")) {
            return defaultValue;
        } else {
            return "true".equalsIgnoreCase(string);
        }

    }


    /**
     * @param requestURI the URL string
     * @return extension .css or .js etc.
     */
    public static String detectExtension(String requestURI) { //!TODO case sensitivity? http://server/context/path/a.CSS
        String requestURIExtension;
        if (requestURI.endsWith(EXT_JS)) {
            requestURIExtension = EXT_JS;
        } else if (requestURI.endsWith(EXT_JSON)) {
            requestURIExtension = EXT_JSON;
        } else if (requestURI.endsWith(EXT_CSS)) {
            requestURIExtension = EXT_CSS;
        } else {
            requestURIExtension = "";
        }
        return requestURIExtension;
    }

    /**
     *
     * @param extension - .js or .css etc.
     * @return - mime like text/javascript or text/css etc.
     */
    public static String selectMimeForExtension(String extension){
        if (EXT_JS.equals(extension)) {
            return MIME_JS;
        }else        if (EXT_CSS.equals(extension)) {
            return MIME_CSS;
        }else        if (EXT_JSON.equals(extension)) {
            return MIME_JSON;
        }
        return null;//"plain/text";
    }
    //!TODO might have problems, need to test or replace with something better
    public static String buildProperPath(String parentPath, String relativePathFromParent){
        if (relativePathFromParent == null) return null;

        if(relativePathFromParent.startsWith("./")){
            relativePathFromParent = relativePathFromParent.replaceFirst("(./)+","");
        }

        String path = "";

        if (relativePathFromParent.startsWith("/")) { //absolute
            path = relativePathFromParent;
        } else if(relativePathFromParent.startsWith("../")){
            while(relativePathFromParent.startsWith("../")){
                relativePathFromParent = relativePathFromParent.replaceFirst("../","");
                if(relativePathFromParent.startsWith("./")) {relativePathFromParent = relativePathFromParent.replaceFirst("./","");}
                parentPath = parentPath == null || parentPath.trim().equals("/") ? "/" : new File(parentPath).getParent();
            }
            path = parentPath + File.separator + relativePathFromParent;
        }else{
            path = parentPath + File.separator + relativePathFromParent;
        }

        return path.replaceAll("//","/").replaceAll("(\\./)+","");
    }


    private Utils() {
    } //non instantiable

}
