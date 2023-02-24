/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

import org.apache.atlas.model.SearchFilter.SortType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Paginated-list, for returning search results.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class PList<T> implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private List<T>  list       = null;
    private long     startIndex = 0;
    private int      pageSize   = 0;
    private long     totalCount = 0;
    private String   sortBy     = null;
    private SortType sortType   = null;

    public PList() {
    }

    public PList(List<T> list) {
        this(list, 0, list.size(), list.size(), SortType.NONE, null);
    }

    public PList(List<T> list, long startIndex, int pageSize, long totalCount, SortType sortType, String sortBy) {
        setList(list);
        setStartIndex(startIndex);
        setPageSize(pageSize);
        setTotalCount(totalCount);
        setSortType(sortType);
        setSortBy(sortBy);
    }

    public void setList(List<T> list) { this.list = list; }

    public List<T> getList() { return this.list; }

    public long getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(long startIndex) {
        this.startIndex = startIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public SortType getSortType() {
        return sortType;
    }

    public void setSortType(SortType sortType) {
        this.sortType = sortType;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }


    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("PList{");
        sb.append("listSize=").append((list == null ? 0 : list.size()));
        sb.append(", startIndex=").append(startIndex);
        sb.append(", pageSize=").append(pageSize);
        sb.append(", totalCount=").append(totalCount);
        sb.append(", sortType=").append(sortType);
        sb.append(", sortBy=").append(sortBy);
        sb.append('}');

        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
