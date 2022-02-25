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

import java.util.Objects;

@AtlasJSON
public class AtlasRelatedCategoryHeader {
    private String categoryGuid;
    private String parentCategoryGuid;
    private String relationGuid;
    private String displayText;
    private String description;

    public AtlasRelatedCategoryHeader() {
    }

    public String getCategoryGuid() {
        return categoryGuid;
    }

    public void setCategoryGuid(final String categoryGuid) {
        this.categoryGuid = categoryGuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
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
        if (!(o instanceof AtlasRelatedCategoryHeader)) return false;
        final AtlasRelatedCategoryHeader that = (AtlasRelatedCategoryHeader) o;
        return Objects.equals(categoryGuid, that.categoryGuid) &&
                       Objects.equals(parentCategoryGuid, that.parentCategoryGuid) &&
                       Objects.equals(relationGuid, that.relationGuid) &&
                       Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {

        return Objects.hash(categoryGuid, parentCategoryGuid, relationGuid, description);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AtlasRelatedCategoryId{");
        sb.append("categoryGuid='").append(categoryGuid).append('\'');
        sb.append(", parentCategoryGuid='").append(parentCategoryGuid).append('\'');
        sb.append(", relationGuid='").append(relationGuid).append('\'');
        sb.append(", displayText='").append(displayText).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }


    public String getRelationGuid() {
        return relationGuid;
    }

    public void setRelationGuid(final String relationGuid) {
        this.relationGuid = relationGuid;
    }

    public String getParentCategoryGuid() {
        return parentCategoryGuid;
    }

    public void setParentCategoryGuid(final String parentCategoryGuid) {
        this.parentCategoryGuid = parentCategoryGuid;
    }
}
