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

package com.googlecode.webutilities.common.cache;

import com.googlecode.webutilities.common.WebUtilitiesResponseWrapper;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class CachedResponse implements Serializable {

    static final long serialVersionUID = 1L;

    private Map<String, Serializable> headers = new HashMap<>();

    private Set<Cookie> cookies = new HashSet<>();

    private int status = 0;

    private byte[] data;

    private String encoding;

    private String contentType;

    private Locale locale;

    private long time;

    public CachedResponse(long time, WebUtilitiesResponseWrapper response) {
        this.time = time;
        this.fromResponse(response);
    }

    public void fromResponse(WebUtilitiesResponseWrapper response) {
        this.cookies = response.getCookies();
        this.headers = response.getHeaders();
        this.status = response.getStatus();
        this.data = response.getBytes();
        this.encoding = response.getCharacterEncoding();
        this.contentType = response.getContentType();
        this.locale = response.getLocale();
    }

    public long getTime() {
        return time;
    }

    public void toResponse(HttpServletResponse response) {

        this.cookies.forEach(response::addCookie);
        for (String headerName : this.headers.keySet()) {
            Object value = this.headers.get(headerName);
            if (value instanceof Long) {
                response.setDateHeader(headerName, ((Long) value));
            } else if (value instanceof Integer) {
                response.setIntHeader(headerName, ((Integer) value));
            } else {
                response.setHeader(headerName, value.toString());
            }
        }

        response.setCharacterEncoding(this.encoding);
        response.setContentType(this.contentType);
        response.setLocale(this.locale);

        if (this.status > 0)
            response.setStatus(this.status);

        try {
            response.getOutputStream().write(this.data);
            response.getOutputStream().close();
        } catch (IOException ex) {
            try {
                response.getWriter().write(new String(this.data));
                response.getWriter().close();
            } catch (Exception ex1) {
                //LOGGER.error(ex1.getMessage(), ex1);
            }
            //   ex.printStackTrace();
        }
    }
}
