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
 * @date 2012-4-10
 */
public class NavigationBar extends BuildNavData {

    /**
     */
    private static final long serialVersionUID = 1L;

    private final HttpServletRequest request;

    public NavigationBar(HttpServletRequest request) {
        super();
        this.request = request;
    }

    @Override
    protected String createBooksItemEnd(boolean isLast) {
        return "</ul></li>";
    }

    @Override
    protected String createBooksItemStart(String id, String text) {
        return "<li><h4>" + text + "</h4><ul class=\"\">";
    }

    @Override
    public String itemEnd() {
        return "</li>";
    }

    @Override
    protected String getNavtreehead() {
        return "<div id=\"CustomerCenterMenu\"><ul class=\"menuBody\">";
    }

    @Override
    protected String getTail() {
        return "</ul></div>";
    }

    @Override
    protected String createItemStart(String id, String text, String icon, Item item, boolean isLast) {
        final boolean selected = StringUtils.equals(this.request.getRequestURI(), item.getUrl());
        return "<li><a target='_top' href=\"" + item.getUrl() + "\" " + (selected ? "class=\"se\"" : StringUtils.EMPTY) + " >" + text + "</a></li>";
    }

    // @Override
    // public String getAppsFromDB() {
    // return StringUtils.EMPTY;
    // }
    public HttpServletRequest getRequest() {
        return request;
    }
    // public void setRequest(HttpServletRequest request) {
    // this.request = request;
    // }
    // @Override
    // public String rowStart() {
    //
    // return super.rowStart();
    // }
}
