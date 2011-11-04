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

package com.googlecode.webutilities.modules.ne;

import com.googlecode.webutilities.modules.infra.ModuleRequest;
import com.googlecode.webutilities.modules.infra.ModuleResponse;
import com.googlecode.webutilities.util.Utils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class HeaderModule implements IModule {

    public static enum Directive {
        Request,
        Response,
        Expires
    }

    public static enum Condition {
        onsuccess,
        always
    }

    public static enum Action {
        set,
        append,
        add,
        unset,
        echo
    }

    @Override
    public DirectivePair parseDirectives(String ruleString) {
        DirectivePair pair = null;
        int index = 0;

        String[] splits = ruleString.split("\\s+");

        assert splits.length >= 4;

        if (!splits[index++].equals(HeaderModule.class.getSimpleName())) return pair;

        Directive directive;
        Condition condition = Condition.always;
        Action action;

        String headerName;
        String headerValue = null;

        directive = Directive.valueOf(splits[index++]);

        assert directive != null;

        if (directive.equals(Directive.Response)) {
            if (splits[index].equals(Condition.onsuccess.name()) || splits[index].equals(Condition.always.name())) {
                condition = Condition.valueOf(splits[index++]);
            }
        }

        action = Action.valueOf(splits[index++]);

        assert action != null;

        headerName = splits[index++];

        if (index < splits.length) {
            headerValue = ruleString.substring(ruleString.indexOf(splits[index]));
        }

        pair = new HeaderDirectivePair(directive.equals(Directive.Response) ?
                new ResponseDirective(condition, action, headerName, headerValue)
                : new RequestDirective(action, headerName, headerValue), null);

        return pair;
    }


}

class HeaderDirectivePair extends DirectivePair {

    HeaderDirectivePair(PreChainDirective preChainDirective, PostChainDirective postChainDirective) {
        super(preChainDirective, postChainDirective);
    }

    @Override
    public ModuleResponse getResponse(HttpServletRequest request, HttpServletResponse response) {
        return new HeaderResponse(response);
    }

    @Override
    public ModuleRequest getRequest(HttpServletRequest request) {
        return new HeaderRequest(request);
    }
}

class RequestDirective implements PreChainDirective {

    HeaderModule.Action action;

    String headerName;

    String headerValue;

    public RequestDirective(HeaderModule.Action action, String headerName, String headerValue) {
        this.action = action;
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {

        HeaderRequest headerRequest = (HeaderRequest) request;

        switch (action) {
            case add:
                headerRequest.addHeader(headerName, headerValue);
                break;
            case set:
                headerRequest.setHeader(headerName, headerValue);
                break;
            case append:
                headerRequest.appendHeader(headerName, headerValue);
                break;
            case unset:
                headerRequest.unsetHeader(headerName);
                break;

        }
        return IDirective.OK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestDirective that = (RequestDirective) o;

        return action == that.action && !(headerName != null ? !headerName.equals(that.headerName) :
                that.headerName != null) && !(headerValue != null ? !headerValue.equals(that.headerValue) :
                that.headerValue != null);

    }

    @Override
    public int hashCode() {
        int result = action != null ? action.hashCode() : 0;
        result = 31 * result + (headerName != null ? headerName.hashCode() : 0);
        result = 31 * result + (headerValue != null ? headerValue.hashCode() : 0);
        return result;
    }
}

class ResponseDirective extends RequestDirective {

    HeaderModule.Condition condition;

    public ResponseDirective(HeaderModule.Condition condition, HeaderModule.Action action, String headerName, String headerValue) {
        super(action, headerName, headerValue);
        this.condition = condition;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {

        HeaderResponse headerResponse = (HeaderResponse) response;
        long dateValue = -99;
        if (HeaderModule.Directive.Expires.name().equals(headerName.trim())) {
            dateValue = deriveExpiresFromString(request, headerValue, context);
        }

        if (condition.equals(HeaderModule.Condition.always) ||
                (condition.equals(HeaderModule.Condition.onsuccess) &&
                        headerResponse.getStatus() >= 200 && headerResponse.getStatus() < 300)) {

            switch (action) {
                case add:
                    if (dateValue != -99) {
                        headerResponse.addDateHeader(headerName, dateValue);
                    } else
                        headerResponse.addHeader(headerName, headerValue);
                    break;
                case set:
                    if (dateValue != -99) {
                        headerResponse.setDateHeader(headerName, dateValue);
                    } else
                        headerResponse.forceHeader(headerName, headerValue);
                    break;
                case append:
                    headerResponse.appendHeader(headerName, headerValue);
                    break;
                case unset:
                    headerResponse.unsetHeader(headerName);
                    break;
                case echo:
                    Enumeration requestHeaders = request.getHeaderNames();
                    while (requestHeaders.hasMoreElements()) {
                        String name = (String) requestHeaders.nextElement();
                        if (name.matches(headerName)) {
                            headerResponse.setHeader(name, request.getHeader(name));
                        }
                    }
                    break;
            }
        }
        return OK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ResponseDirective that = (ResponseDirective) o;

        return condition == that.condition;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        return result;
    }


    /**
     * @param request HttpServletRequest
     * @return string url
     */
    private String getURL(HttpServletRequest request) {
        return Utils.removeFingerPrint(request.getRequestURI());
    }

    public long deriveExpiresFromString(ModuleRequest request, String string, ServletContext context) {
        if (string == null) return -1;
        string = string.trim();

        //String PATTEN = "(access|modified|now)\\s+plus(\\s+[1-9][0-9]*\\s+(year|month|week|day|hour|minute|second)s?)+";

        if (string.length() <= 0) {
            return new Date().getTime();
        }
        int quoteStart = string.indexOf("");
        int quoteEnd = string.lastIndexOf("");
        if (quoteStart == 0 && quoteEnd == string.length()) {
            string = string.substring(1, quoteEnd - 1);
        }

        List<String> resourcesToMerge = Utils.findResourcesToMerge(request.getContextPath(), getURL(request));

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(new Date().getTime());

        if (string.indexOf("modified") == 0 || string.indexOf("modification") == 0) {
            cal.setTimeInMillis(Utils.getLastModifiedFor(resourcesToMerge, context));
        }

        String[] tokens = string.substring(string.indexOf("plus") + 4).split("\\s+");
        int index = 0;
        while (index < tokens.length) {
            try {
                int value = Utils.readInt(tokens[index++], -1);
                if (value == -1) continue;
                if (tokens[index].indexOf("year") == 0) {
                    cal.add(Calendar.YEAR, value);
                } else if (tokens[index].indexOf("month") == 0) {
                    cal.add(Calendar.MONTH, value);
                } else if (tokens[index].indexOf("week") == 0) {
                    cal.add(Calendar.WEEK_OF_YEAR, value);
                } else if (tokens[index].indexOf("day") == 0) {
                    cal.add(Calendar.DATE, value);
                } else if (tokens[index].indexOf("hour") == 0) {
                    cal.add(Calendar.HOUR, value);
                } else if (tokens[index].indexOf("minute") == 0) {
                    cal.add(Calendar.MINUTE, value);
                } else if (tokens[index].indexOf("second") == 0) {
                    cal.add(Calendar.SECOND, value);
                }

            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }

        }
        return cal.getTimeInMillis();

    }

}

class HeaderRequest extends ModuleRequest {

    private Map<String, Object> headers = new HashMap<String, Object>();

    public HeaderRequest(HttpServletRequest request) {
        super(request);
        Enumeration existingHeaders = request.getHeaderNames();
        while (existingHeaders.hasMoreElements()) {
            String name = (String) existingHeaders.nextElement();
            Enumeration multiple = request.getHeaders(name);
            if (multiple != null) {
                while (multiple.hasMoreElements()) {
                    this.addHeader(name.toLowerCase(), multiple.nextElement());
                }
            } else {
                headers.put(name, request.getHeader(name));
            }
        }
    }

    @Override
    public long getDateHeader(String name) {
        return (Long) headers.get(name.toLowerCase());
    }

    @Override
    public String getHeader(String name) {
        Object obj = headers.get(name.toLowerCase());
        if (obj instanceof ArrayList) {
            return (String) ((ArrayList) obj).get(0);
        }
        return (String) obj;
    }

    @Override
    public Enumeration getHeaders(String name) {
        Object list = headers.get(name.toLowerCase());
        if (list instanceof ArrayList) {
            return Collections.enumeration((ArrayList) list);
        }
        ArrayList<Object> aList = new ArrayList<Object>();
        aList.add(list);
        return Collections.enumeration(aList);
    }

    @Override
    public Enumeration getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public int getIntHeader(String name) {
        return (Integer) headers.get(name.toLowerCase());
    }

    public void setHeader(String name, Object value) {
        headers.put(name, value);
    }

    public void appendHeader(String name, Object value) {
        Object existing = headers.get(name.toLowerCase());
        if (existing != null) {
            value = existing + "," + value;
        }
        headers.put(name, value);
    }

    public void addHeader(String name, Object value) {
        Object header = headers.get(name.toLowerCase());
        if (header instanceof ArrayList) {
            ArrayList<Object> list = (ArrayList<Object>) header;
            list.add(value);
            headers.put(name, list);
        } else {
            ArrayList<Object> list = new ArrayList<Object>();
            if (header != null) list.add(header);
            list.add(value);
            headers.put(name, list);
        }

    }

    public void unsetHeader(String name) {
        headers.remove(name);
    }

}

class HeaderResponse extends ModuleResponse {

    private Set<String> toBeUnsetHeaders = new HashSet<String>();

    private Map<String, Object> toBeAppendedHeaders = new HashMap<String, Object>();

    HeaderResponse(HttpServletResponse response) {
        super(response);
    }

    public void forceHeader(String name, String value) {
        super.setHeader(name.toLowerCase(), value);
        toBeUnsetHeaders.add(name.toLowerCase());
    }

    @Override
    public void setHeader(String name, String value) {
        if (!toBeUnsetHeaders.contains(name.toLowerCase())) {
            if (toBeAppendedHeaders.containsKey(name.toLowerCase())) {
                value += "," + toBeAppendedHeaders.get(name.toLowerCase());
            }
            super.setHeader(name.toLowerCase(), value);
        }
    }

    @Override
    public void addDateHeader(String name, long date) {
        if (!toBeUnsetHeaders.contains(name.toLowerCase())) {
            super.addDateHeader(name.toLowerCase(), date);
        }
    }

    @Override
    public void setDateHeader(String name, long date) {
        if (!toBeUnsetHeaders.contains(name.toLowerCase())) {
            super.setDateHeader(name.toLowerCase(), date);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (!toBeUnsetHeaders.contains(name.toLowerCase())) {
            if (toBeAppendedHeaders.containsKey(name.toLowerCase())) {
                value += "," + toBeAppendedHeaders.get(name.toLowerCase());
            }
            super.addHeader(name.toLowerCase(), value);
        }
    }

    @Override
    public void setIntHeader(String name, int value) {
        if (!toBeUnsetHeaders.contains(name.toLowerCase())) {
            super.setIntHeader(name.toLowerCase(), value);
        }
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (!toBeUnsetHeaders.contains(name.toLowerCase())) {
            super.addIntHeader(name.toLowerCase(), value);
        }
    }

    public void appendHeader(String name, Object value) {
        Object existingVal = toBeAppendedHeaders.get(name.toLowerCase());
        if (existingVal != null) {
            value = value.toString() + "," + existingVal;
        }
        toBeAppendedHeaders.put(name, value);
    }

    public void unsetHeader(String name) {
        toBeUnsetHeaders.add(name.toLowerCase());
    }
}