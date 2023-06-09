/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.sql.parser.meta;

import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DependencyNode {

    private String id;

    private String tabid;

    private String dbid;

    private String name;

    private String dbName;

    private String extractSql;

    private String type = NodeType.JOINER_SQL.getType();

    // 节点额外元数据信息
    private TabExtraMeta extraMeta;

    public static DependencyNode create(String id, String name, NodeType type) {
        DependencyNode dnode = new DependencyNode();
        dnode.setId(id);
        dnode.setName(name);
        dnode.setType(type.getType());
        return dnode;
    }


    public DependencyNode() {
    }

    public TabExtraMeta getExtraMeta() {
        return extraMeta;
    }

    public void setExtraMeta(TabExtraMeta extraMeta) {
        this.extraMeta = extraMeta;
    }

    private Position position;

    public Position getPosition() {
        return this.position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getTabid() {
        return this.tabid;
    }

    public void setTabid(String tabid) {
        this.tabid = tabid;
    }

    public String getDbid() {
        return this.dbid;
    }

    public void setDbid(String dbid) {
        this.dbid = dbid;
    }

    public String getDbName() {
        return this.dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getExtraSql() {
        return this.extractSql;
    }

    public void setExtraSql(String extractSql) {
        this.extractSql = extractSql;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NodeType parseNodeType() {
        return parseNodeType(true);
    }

    public NodeType parseNodeType(boolean validateTabId) {
        NodeType result = NodeType.parse(this.type);
        if (result == NodeType.DUMP) {
            if (validateTabId && StringUtils.isBlank(this.tabid)) {
                throw new IllegalStateException("tabid can not be null,id:" + this.id);
            }
        }
        return result;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EntityName parseEntityName() {
        if (StringUtils.isEmpty(this.dbName)) {
            return EntityName.parse(this.name);
        } else {
            return EntityName.create(this.dbName, this.name);
        }
    }

    @Override
    public String toString() {
        return dbName + "." + name;
    }
}
