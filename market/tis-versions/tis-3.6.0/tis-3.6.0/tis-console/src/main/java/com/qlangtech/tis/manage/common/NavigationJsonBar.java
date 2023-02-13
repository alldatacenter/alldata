/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.common;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import com.qlangtech.tis.manage.module.screen.BuildNavData;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-4-22
 */
public class NavigationJsonBar extends BuildNavData {

    /**
     */
    private static final long serialVersionUID = 1L;

    private final HttpServletRequest request;

    public NavigationJsonBar(HttpServletRequest request) {
        super();
        this.request = request;
    }

    @Override
    protected String createBooksItemStart(String id, String text) {
        return "{groupname:\"" + text + "\",items:[";
    }

    @Override
    protected String createBooksItemEnd(boolean isLast) {
        return "]}" + (isLast ? StringUtils.EMPTY : ",");
    }

    @Override
    protected String createItemStart(String id, String text, String icon, Item item, boolean islast) {
        return "{href:\"" + item.getUrl() + "\",text:\"" + text + "\"}" + (islast ? StringUtils.EMPTY : ",");
    }

    @Override
    public String itemEnd() {
        // return "}";
        return StringUtils.EMPTY;
    }

    @Override
    protected String getNavtreehead() {
        return "{results:[";
    }

    @Override
    protected String getTail() {
        return "]}";
    }

    // @Override
    // public String getAppsFromDB() {
    // return StringUtils.EMPTY;
    // }
    public HttpServletRequest getRequest() {
        return request;
    }
}
