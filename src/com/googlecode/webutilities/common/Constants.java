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

package com.googlecode.webutilities.common;


/**
 * Common Class to hold the public static constant so that to share across the project
 *
 * @author rpatil
 * @version 1.0
 */
public final class Constants {


    public static final String TYPE_JS = "js";

    public static final String TYPE_CSS = "css";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String EXT_JS = ".js";

    public static final String EXT_JSON = ".json";

    public static final String EXT_CSS = ".css";

    public static final String MIME_JS = "text/javascript";

    public static final String MIME_JSON = "application/json";

    public static final String MIME_CSS = "text/css";

    public static final String HEADER_EXPIRES = "Expires";

    public static final String HEADER_LAST_MODIFIED = "Last-Modified";

    public static final String PARAM_EXPIRE_CACHE = "_expirecache_";

    public static final String PARAM_RESET_CACHE = "_resetcache_";

    public static final String PARAM_SKIP_CACHE = "_skipcache_";

    public static final String PARAM_DEBUG = "_dbg_";

    public static final long DEFAULT_EXPIRES_MINUTES = 7 * 24 * 60; //7 days

    public static final String DEFAULT_CACHE_CONTROL = "public";//

    public static final int DEFAULT_COMPRESSION_SIZE_THRESHOLD = 128 * 1024; //128KB

    public static final String HTTP_VARY_HEADER = "Vary";

    public static final String HTTP_ACCEPT_ENCODING_HEADER = "Accept-Encoding";

    public static final String HTTP_CONTENT_ENCODING_HEADER = "Content-Encoding";

    public static final String HTTP_CACHE_CONTROL_HEADER = "Cache-Control";

    public static final String HTTP_CONTENT_LENGTH_HEADER = "Content-Length";

    public static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";

    public static final String HTTP_ETAG_HEADER = "ETag";

    public static final String CONTENT_ENCODING_GZIP = "gzip";

    public static final String CONTENT_ENCODING_COMPRESS = "compress";

    public static final String CONTENT_ENCODING_DEFLATE = "deflate";

    public static final String CONTENT_ENCODING_IDENTITY = "identity";

    public static final String HTTP_USER_AGENT_HEADER = "User-Agent";

    private Constants() {
    } //non instantiable


}
