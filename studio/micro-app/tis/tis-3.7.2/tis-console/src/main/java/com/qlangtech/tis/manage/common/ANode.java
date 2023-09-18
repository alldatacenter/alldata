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

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-12-11
 */
public class ANode {

    // id1,
    // text 'A leaf Node'
    // leaf: true,
    private final String id;

    private final String text;

    private final boolean leaf;

    private final Integer bizid;

    private final String realname;

    public static ANode createBizNode(int bizid, String bizName) {
        return new ANode("b" + bizid, bizName, null, false, bizid);
    }

    public static BizANode createExtBizNode(int bizid, String bizName) {
        return new BizANode("b" + bizid, bizName, null, false, bizid);
    }

    public static ANode createAppNode(int appid, String appName) {
        return new ANode(String.valueOf(appid), trimName(appName), (appName), true, null);
    }

    private static String trimName(String value) {
        if (StringUtils.startsWith(value, "search4")) {
            return "s" + StringUtils.substring(value, 6);
        }
        return value;
    }

    protected ANode(String id, String text, String realname, boolean leaf, Integer bizid) {
        super();
        this.id = id;
        this.text = text;
        this.leaf = leaf;
        this.bizid = bizid;
        this.realname = realname;
    }

    public Integer getBizid() {
        return bizid;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public boolean isLeaf() {
        return leaf;
    }
}
