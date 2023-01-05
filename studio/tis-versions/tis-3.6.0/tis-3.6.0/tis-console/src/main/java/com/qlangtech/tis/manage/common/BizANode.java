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

import java.util.ArrayList;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-1-4
 */
public class BizANode extends ANode {

    // private final ANode bizNode;
    private final List<ANode> children;

    BizANode(String id, String text, String realname, boolean leaf, Integer bizid) {
        super(id, text, realname, leaf, bizid);
        this.children = new ArrayList<ANode>();
    }

    // public BizANode(int bizid, String bizName) {
    // super();
    // bizNode = ANode.createBizNode(bizid, bizName);
    // this.children = new ArrayList<ANode>();
    // }
    public void addAppNode(int appid, String appName) {
        this.children.add(ANode.createAppNode(appid, appName));
    }

    // public ANode getBizNode() {
    // return bizNode;
    // }
    public List<ANode> getChildren() {
        return children;
    }
}
