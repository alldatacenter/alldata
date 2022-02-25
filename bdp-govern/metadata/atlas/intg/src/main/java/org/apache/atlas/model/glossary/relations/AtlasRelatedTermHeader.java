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
package org.apache.atlas.model.glossary.relations;

import org.apache.atlas.model.annotation.AtlasJSON;
import org.apache.atlas.model.glossary.enums.AtlasTermRelationshipStatus;

import java.util.Objects;

@AtlasJSON
public class AtlasRelatedTermHeader {
    private String termGuid;
    private String relationGuid;
    private String displayText;
    private String description;
    private String expression;
    private String steward;
    private String source;

    private AtlasTermRelationshipStatus status;

    public AtlasRelatedTermHeader() {
    }

    public String getTermGuid() {
        return termGuid;
    }

    public void setTermGuid(final String termGuid) {
        this.termGuid = termGuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(final String expression) {
        this.expression = expression;
    }

    public String getSteward() {
        return steward;
    }

    public void setSteward(final String steward) {
        this.steward = steward;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public AtlasTermRelationshipStatus getStatus() {
        return status;
    }

    public void setStatus(final AtlasTermRelationshipStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AtlasRelatedTermHeader)) return false;
        final AtlasRelatedTermHeader that = (AtlasRelatedTermHeader) o;
        return Objects.equals(termGuid, that.termGuid) &&
                       Objects.equals(relationGuid, that.relationGuid) &&
                       Objects.equals(description, that.description) &&
                       Objects.equals(expression, that.expression) &&
                       Objects.equals(steward, that.steward) &&
                       Objects.equals(source, that.source) &&
                       status == that.status;
    }

    @Override
    public int hashCode() {

        return Objects.hash(termGuid, relationGuid, description, expression, steward, source, status);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("AtlasRelatedTermId{");
        sb.append("termGuid='").append(termGuid).append('\'');
        sb.append(", relationGuid='").append(relationGuid).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", displayText='").append(displayText).append('\'');
        sb.append(", expression='").append(expression).append('\'');
        sb.append(", steward='").append(steward).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(final String displayText) {
        this.displayText = displayText;
    }

    public String getRelationGuid() {
        return relationGuid;
    }

    public void setRelationGuid(final String relationGuid) {
        this.relationGuid = relationGuid;
    }
}
