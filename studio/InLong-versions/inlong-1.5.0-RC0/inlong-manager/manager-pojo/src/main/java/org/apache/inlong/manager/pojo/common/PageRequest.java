/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.pojo.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Pagination request
 */
@ApiModel(value = "Pagination request")
public class PageRequest {

    public static final Integer MAX_PAGE_SIZE = 100;

    @ApiModelProperty(value = "Current page number, default is 1")
    private int pageNum = 1;

    @ApiModelProperty(value = "Page size, default is 10")
    private int pageSize = 10;

    @ApiModelProperty(value = "Order field, support create_time and modify_time, default is create_time")
    private String orderField = "create_time";

    @ApiModelProperty(value = "Order type, only support asc and desc, default is desc")
    private String orderType = "desc";

    public String getOrderField() {
        return orderField;
    }

    public PageRequest setOrderField(String orderField) {
        this.orderField = orderField;
        return this;
    }

    public String getOrderType() {
        return orderType;
    }

    public PageRequest setOrderType(String orderType) {
        this.orderType = orderType;
        return this;
    }

    public int getPageNum() {
        return pageNum;
    }

    public PageRequest setPageNum(int pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    public int getPageSize() {
        return pageSize;
    }

    public PageRequest setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }
}
