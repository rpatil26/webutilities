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

package com.googlecode.webutilities.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.googlecode.webutilities.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tag to get fingerprinted URL for static resources.
 *
 * @author rpatil
 * @version 1.0
 *
 */
public class URLTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;

	private String value;

    private String context;

    private String var;

    private int scope = PageContext.PAGE_SCOPE; //default scope

    private static final Logger LOGGER = LoggerFactory.getLogger(URLTag.class.getName());

    public URLTag() {
        super();
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public int doStartTag() throws JspException {
        return EVAL_BODY_BUFFERED;
    }

    public int doEndTag() throws JspException {

        if(value == null || Utils.isProtocolURL(value.toLowerCase().trim())){
            LOGGER.trace("Invalid url : {}", value);
            return gracefully();
        }

        if(context == null){
            HttpServletRequest httpServletRequest = (HttpServletRequest)pageContext.getRequest();
            context = httpServletRequest.getContextPath();
        }

        if ((!context.startsWith("/") || !value.startsWith("/")) && value.endsWith("/")) {
            LOGGER.warn("Invalid context|value.");
            throw new JspTagException("Invalid context|value");
        }

        //We got the url, now suffix the fingerprint to it, right before .
        String eTag = Utils.buildETagForResources(Utils.findResourcesToMerge(context, value),pageContext.getServletContext());

        value = Utils.addFingerPrint(eTag, value);

        value = context + "/" + value;

        value = value.replaceAll("/+","/");

        return gracefully();
    }

    private int gracefully() throws JspTagException{
        if(value != null){
            if (var != null)
                pageContext.setAttribute(var, value, scope);
            else {
                try {
                    pageContext.getOut().print(value);
                } catch (java.io.IOException ex) {
                    LOGGER.error("IO Error:", ex);
                    throw new JspTagException(ex.toString(), ex);
                }
            }
        }
        return EVAL_PAGE;
    }
}

