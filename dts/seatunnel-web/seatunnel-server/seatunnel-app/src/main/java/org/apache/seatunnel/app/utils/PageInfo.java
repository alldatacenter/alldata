/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.utils;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

/**
 * page info
 *
 * @param <T> model
 */
public class PageInfo<T> {

    /** totalList */
    @Schema(description = "CURRENT_PAGE_RECORDS")
    private List<T> totalList = Collections.emptyList();

    /** total */
    @Schema(description = "TOTAL_RECORDS")
    private Integer total = 0;

    /** total Page */
    @Schema(description = "TOTAL_PAGE")
    private Integer totalPage;

    /** page size */
    @Schema(description = "PAGE_SIZE")
    private Integer pageSize = 20;

    /** current page */
    @Schema(description = "CURRENT_PAGE")
    private Integer currentPage = 0;

    /** pageNo */
    @Schema(description = "PAGE_NO")
    private Integer pageNo;

    public PageInfo() {}

    public PageInfo(Integer currentPage, Integer pageSize) {
        if (currentPage == null) {
            currentPage = 1;
        }
        this.pageNo = (currentPage - 1) * pageSize;
        this.pageSize = pageSize;
        this.currentPage = currentPage;
    }

    public Integer getStart() {
        return pageNo;
    }

    public void setStart(Integer start) {
        this.pageNo = start;
    }

    public List<T> getTotalList() {
        return totalList;
    }

    public void setTotalList(List<T> totalList) {
        this.totalList = totalList;
    }

    public Integer getTotal() {
        if (total == null) {
            total = 0;
        }
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getTotalPage() {
        if (pageSize == null || pageSize == 0) {
            pageSize = 7;
        }
        this.totalPage =
                (this.total % this.pageSize) == 0
                        ? ((this.total / this.pageSize) == 0 ? 1 : (this.total / this.pageSize))
                        : (this.total / this.pageSize + 1);
        return this.totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    public Integer getPageSize() {
        if (pageSize == null || pageSize == 0) {
            pageSize = 7;
        }
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getCurrentPage() {
        if (currentPage == null || currentPage <= 0) {
            this.currentPage = 1;
        }
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }
}
