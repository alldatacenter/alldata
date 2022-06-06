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

package org.apache.atlas.v1.model.discovery;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.v1.model.instance.Referenceable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class DSLSearchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String              requestId;
    private String              queryType;
    private String              query;
    private String              dataType;
    private int                 count = 0;
    private List<Referenceable> results;


    public DSLSearchResult() {
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Referenceable> getResults() {
        return results;
    }

    public void setResults(List<Referenceable> results) {
        this.results = results;
    }

    public void addResult(Referenceable entity) {
        if (this.results == null) {
            this.results = new ArrayList<>();
        }

        this.results.add(entity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DSLSearchResult obj = (DSLSearchResult) o;

        return Objects.equals(requestId, obj.requestId) &&
               Objects.equals(queryType, obj.queryType) &&
               Objects.equals(query, obj.query) &&
               Objects.equals(dataType, obj.dataType) &&
               Objects.equals(count, obj.count) &&
               Objects.equals(results, obj.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, queryType, query, dataType, count, results);
    }


    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("FullTextSearchResult{")
                .append("requestId=").append(requestId)
                .append(", queryType=").append(queryType)
                .append(", query=").append(query)
                .append(", dataType=").append(dataType)
                .append(", count=").append(count)
                .append(", results=").append(results)
                .append("}");

        return sb;
    }
}
