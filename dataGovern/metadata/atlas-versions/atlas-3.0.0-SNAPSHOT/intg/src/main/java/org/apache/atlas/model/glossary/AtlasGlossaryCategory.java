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
package org.apache.atlas.model.glossary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.atlas.model.annotation.AtlasJSON;
import org.apache.atlas.model.glossary.relations.AtlasGlossaryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedCategoryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@AtlasJSON
public class AtlasGlossaryCategory extends AtlasGlossaryBaseObject {
    // Inherited attributes from relations
    private AtlasGlossaryHeader anchor;

    // Category hierarchy links
    private AtlasRelatedCategoryHeader      parentCategory;
    private Set<AtlasRelatedCategoryHeader> childrenCategories;

    // Terms associated with this category
    private Set<AtlasRelatedTermHeader> terms;

    public AtlasGlossaryCategory() {
    }

    public AtlasGlossaryCategory(final AtlasGlossaryCategory other) {
        super(other);
        this.anchor = other.anchor;
        this.parentCategory = other.parentCategory;
        this.childrenCategories = other.childrenCategories;
        this.terms = other.terms;
    }

    public AtlasGlossaryHeader getAnchor() {
        return anchor;
    }

    public void setAnchor(final AtlasGlossaryHeader anchor) {
        this.anchor = anchor;
    }

    public AtlasRelatedCategoryHeader getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(final AtlasRelatedCategoryHeader parentCategory) {
        this.parentCategory = parentCategory;
    }

    public Set<AtlasRelatedCategoryHeader> getChildrenCategories() {
        return childrenCategories;
    }

    public void setChildrenCategories(final Set<AtlasRelatedCategoryHeader> childrenCategories) {
        this.childrenCategories = childrenCategories;
    }

    public Set<AtlasRelatedTermHeader> getTerms() {
        return terms;
    }

    public void setTerms(final Set<AtlasRelatedTermHeader> terms) {
        this.terms = terms;
    }

    @JsonIgnore
    public void addChild(AtlasRelatedCategoryHeader child) {
        Set<AtlasRelatedCategoryHeader> children = this.childrenCategories ;
        if (children == null) {
            children = new HashSet<>();
        }
        children.add(child);
        setChildrenCategories(children);
    }

    @JsonIgnore
    public void removeChild(AtlasRelatedCategoryHeader child) {
        if (CollectionUtils.isNotEmpty(childrenCategories)) {
            childrenCategories.remove(child);
        }
    }

    @JsonIgnore
    public void addTerm(AtlasRelatedTermHeader term) {
        Set<AtlasRelatedTermHeader> terms = this.terms;
        if (terms == null) {
            terms = new HashSet<>();
        }
        terms.add(term);
        setTerms(terms);
    }

    @JsonIgnore
    public void removeTerm(AtlasRelatedTermHeader term) {
        if (CollectionUtils.isNotEmpty(terms)) {
            terms.remove(term);
        }
    }

    @JsonIgnore
    @Override
    public void setAttribute(String attrName, String attrVal) {
        Objects.requireNonNull(attrName, "AtlasGlossary attribute name");
        switch(attrName) {
            case "name":
                setName(attrVal);
                break;
            case "shortDescription":
                setShortDescription(attrVal);
                break;
            case "longDescription":
                setLongDescription(attrVal);
                break;
            default:
                throw new IllegalArgumentException("Invalid attribute '" + attrName + "' for object AtlasGlossaryCategory");
        }
    }

    @Override
    protected StringBuilder toString(final StringBuilder sb) {
        sb.append(", anchor=").append(anchor);
        sb.append(", parentCategory=").append(parentCategory);
        sb.append(", childrenCategories=").append(childrenCategories);
        sb.append(", terms=").append(terms);

        return sb;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AtlasGlossaryCategory)) return false;
        if (!super.equals(o)) return false;
        final AtlasGlossaryCategory category = (AtlasGlossaryCategory) o;
        return Objects.equals(anchor, category.anchor) &&
                       Objects.equals(parentCategory, category.parentCategory) &&
                       Objects.equals(childrenCategories, category.childrenCategories) &&
                       Objects.equals(terms, category.terms);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), anchor, parentCategory, childrenCategories, terms);
    }
}
