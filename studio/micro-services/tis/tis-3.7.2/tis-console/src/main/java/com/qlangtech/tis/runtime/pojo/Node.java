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
package com.qlangtech.tis.runtime.pojo;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-5-9
 */
public class Node {

    private final String name;

    private final Integer pId;

    private final Integer id;

    public Node(String name, Integer pId, Integer id) {
        super();
        this.name = name;
        this.pId = pId;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Integer getpId() {
        return pId;
    }

    public Integer getId() {
        return id;
    }
}
