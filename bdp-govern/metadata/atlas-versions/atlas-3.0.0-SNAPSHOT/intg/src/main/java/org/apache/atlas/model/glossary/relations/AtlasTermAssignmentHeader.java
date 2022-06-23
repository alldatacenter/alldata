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
import org.apache.atlas.model.glossary.enums.AtlasTermAssignmentStatus;

import java.util.Objects;

@AtlasJSON
public class AtlasTermAssignmentHeader {
    private String termGuid;
    private String relationGuid;
    private String description;
    private String displayText;
    private String expression;
    private String createdBy;
    private String steward;
    private String source;
    private int    confidence;

    private AtlasTermAssignmentStatus status;

    public AtlasTermAssignmentHeader() {
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
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

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(final int confidence) {
        this.confidence = confidence;
    }

    public AtlasTermAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(final AtlasTermAssignmentStatus status) {
        this.status = status;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(final String displayText) {
        this.displayText = displayText;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AtlasTermAssignmentHeader)) return false;
        final AtlasTermAssignmentHeader that = (AtlasTermAssignmentHeader) o;
        return confidence == that.confidence &&
                       Objects.equals(termGuid, that.termGuid) &&
                       Objects.equals(relationGuid, that.relationGuid) &&
                       Objects.equals(description, that.description) &&
                       Objects.equals(expression, that.expression) &&
                       Objects.equals(createdBy, that.createdBy) &&
                       Objects.equals(steward, that.steward) &&
                       Objects.equals(source, that.source) &&
                       status == that.status;
    }

    @Override
    public int hashCode() {

        return Objects.hash(termGuid, relationGuid, description, expression, createdBy, steward, source, confidence, status);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AtlasTermAssignmentId{");
        sb.append("termGuid='").append(termGuid).append('\'');
        sb.append(", relationGuid='").append(relationGuid).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", displayText='").append(displayText).append('\'');
        sb.append(", expression='").append(expression).append('\'');
        sb.append(", createdBy='").append(createdBy).append('\'');
        sb.append(", steward='").append(steward).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", confidence=").append(confidence);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }


    public String getRelationGuid() {
        return relationGuid;
    }

    public void setRelationGuid(final String relationGuid) {
        this.relationGuid = relationGuid;
    }
}
