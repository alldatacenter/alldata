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

package org.apache.atlas.model.audit;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.SortOrder;
import org.apache.atlas.model.discovery.SearchParameters.FilterCriteria;

import java.io.Serializable;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditSearchParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    AuditSearchParameters() {}

    private FilterCriteria auditFilters;
    private int            limit;
    private int            offset;
    private String         sortBy;
    private SortOrder      sortOrder;

    /**
     * Entity attribute filters for the type (if type name is specified)
     * @return
     */
    public FilterCriteria getAuditFilters() {
        return auditFilters;
    }

    /**
     * Filter the entities on this criteria
     * @param auditFilters
     */
    public void setAuditFilters(FilterCriteria auditFilters) {
        this.auditFilters = auditFilters;
    }

    /**
     * @return Max number of results to be returned
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Restrict the results to the specified limit
     * @param limit max number of results
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @return Offset(pagination) of the results
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @param offset
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * @return Attribute on which to sort the results
     */
    public String getSortBy() { return sortBy; }

    /**
     * Sort the results based on sortBy attribute
     * @param sortBy Attribute on which to sort the results
     */
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    /**
     * @return Sorting order of the results
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
     * Sorting order to sort the results
     * @param sortOrder ASCENDING vs DESCENDING
     */
    public void setSortOrder(SortOrder sortOrder) { this.sortOrder = sortOrder; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditSearchParameters that = (AuditSearchParameters) o;
        return Objects.equals(auditFilters, that.auditFilters) &&
                limit == that.limit &&
                offset == that.offset &&
                Objects.equals(sortBy, that.sortBy) &&
                Objects.equals(sortOrder, that.sortOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auditFilters, limit, offset, sortBy, sortOrder);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AuditSearchParameters{");
        sb.append("auditFilters=").append(auditFilters);
        sb.append(", limit=").append(limit);
        sb.append(", offset=").append(offset);
        sb.append(", sortBy='").append(sortBy).append('\'');
        sb.append(", sortOrder=").append(sortOrder);
        sb.append('}');
        return sb.toString();
    }
}
