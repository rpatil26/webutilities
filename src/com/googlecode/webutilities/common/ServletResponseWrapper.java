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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Common Simple Servlet Response Wrapper using ServletResponseOutputStream
 * @see ServletResponseOutputStream
 *
 * @author rpatil
 * @version 1.0
 */

public class ServletResponseWrapper extends HttpServletResponseWrapper {
	
	private ServletResponseOutputStream stream;
	private Map<String,Object> headers = new HashMap<String,Object>();
	private Set<Cookie> cookies = new HashSet<Cookie>();
	private String contentType;
	private int status;
	boolean getWriterCalled = false;
	boolean getStreamCalled = false;

    @Override
	public void addCookie(Cookie cookie) {
		super.addCookie(cookie);
		cookies.add(cookie);
	}
    @Override
	public void setStatus(int sc, String sm) {
		super.setStatus(sc, sm);
		this.status = sc;
	}
	@Override
	public void setStatus(int sc) {
		super.setStatus(sc);
		this.status = sc;
	}
	@Override
	public void sendError(int sc) throws IOException {
		super.sendError(sc);
		this.status = sc;
	}
	@Override
	public void sendError(int sc, String msg) throws IOException {
		super.sendError(sc, msg);
		this.status = sc;
	}
	@Override
	public void addDateHeader(String name, long date) {
		super.addDateHeader(name, date);
		headers.put(name,date);
	}
	@Override
	public void addHeader(String name, String value) {
		super.addHeader(name, value);
		headers.put(name, value);
	}
	@Override
	public void addIntHeader(String name, int value) {
		super.addIntHeader(name, value);
		headers.put(name, value);
	}
	@Override
	public void setDateHeader(String name, long date) {
		super.setDateHeader(name, date);
		headers.put(name, date);
	}
	@Override
	public void setHeader(String name, String value) {
		super.setHeader(name, value);
		headers.put(name, value);
	}
	@Override
	public void setIntHeader(String name, int value) {
		super.setIntHeader(name, value);
		headers.put(name, value);
	}
	@Override
	public String getContentType() {
		return contentType;
	}
	@Override
	public void setContentType(String type) {
		super.setContentType(type);
		this.contentType = type;
	}
	@Override
	public boolean containsHeader(String name) {
		return headers.containsKey(name);
	}
	public Map<String, Object> getHeaders() {
		return headers;
	}
	public Set<Cookie> getCookies() {
		return cookies;
	}
	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if(getWriterCalled){
			throw new IllegalStateException("getWriter already called.");
		}
		getStreamCalled = true;
		return this.stream;
	}
	
	@Override
	public PrintWriter getWriter() throws IOException {
		if(getStreamCalled){
			throw new IllegalStateException("getStream already called.");
		}
		getWriterCalled = true;
		return new PrintWriter(stream);
	}
	
	public int getStatus() {
		return status;
	}
	
	public String getContents() {
		return new String(stream.getByteArrayOutputStream().toByteArray());
	}
	
	public byte[] getBytes() {
		return stream.getByteArrayOutputStream().toByteArray();
	}
	
    public ServletResponseWrapper(HttpServletResponse response) {
		super(response);
		stream = new ServletResponseOutputStream();
	}
	
}