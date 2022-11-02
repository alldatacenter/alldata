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
import org.apache.atlas.model.glossary.relations.AtlasRelatedCategoryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@AtlasJSON
public class AtlasGlossary extends AtlasGlossaryBaseObject {
    private String language;
    private String usage;

    private Set<AtlasRelatedTermHeader>     terms;
    private Set<AtlasRelatedCategoryHeader> categories;

    public AtlasGlossary() {
    }

    public AtlasGlossary(final AtlasGlossary other) {
        super(other);
        super.setQualifiedName(other.getQualifiedName());
        super.setGuid(other.getGuid());
        super.setName(other.name);
        super.setShortDescription(other.shortDescription);
        super.setLongDescription(other.longDescription);
        this.language = other.language;
        this.usage = other.usage;
        this.terms = other.terms;
        this.categories = other.categories;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(final String usage) {
        this.usage = usage;
    }

    public Set<AtlasRelatedCategoryHeader> getCategories() {
        return categories;
    }

    public void setCategories(final Set<AtlasRelatedCategoryHeader> categories) {
        this.categories = categories;
    }

    public Set<AtlasRelatedTermHeader> getTerms() {
        return terms;
    }

    public void setTerms(final Set<AtlasRelatedTermHeader> terms) {
        this.terms = terms;
    }

    @JsonIgnore
    @Override
    public void setAttribute(String attrName, String attrVal) {
        Objects.requireNonNull(attrName, "AtlasGlossary attribute name");
        switch (attrName) {
            case "name":
                setName(attrVal);
                break;
            case "shortDescription":
                setShortDescription(attrVal);
                break;
            case "longDescription":
                setLongDescription(attrVal);
                break;
            case "language":
                setLanguage(attrVal);
                break;
            case "usage":
                setUsage(attrVal);
                break;
            default:
                throw new IllegalArgumentException("Invalid attribute '" + attrName + "' for object AtlasGlossary");
        }
    }

    @JsonIgnore
    public void addTerm(AtlasRelatedTermHeader relatedTermId) {
        Set<AtlasRelatedTermHeader> terms = this.terms;
        if (terms == null) {
            terms = new HashSet<>();
        }
        terms.add(relatedTermId);
        setTerms(terms);
    }

    @JsonIgnore
    public void addCategory(AtlasRelatedCategoryHeader relatedCategoryId) {
        Set<AtlasRelatedCategoryHeader> categories = this.categories;
        if (categories == null) {
            categories = new HashSet<>();
        }
        categories.add(relatedCategoryId);
        setCategories(categories);
    }

    @JsonIgnore
    public void removeTerm(AtlasRelatedTermHeader relatedTermId) {
        if (CollectionUtils.isNotEmpty(terms)) {
            terms.remove(relatedTermId);
        }
    }

    @JsonIgnore
    public void removeCategory(AtlasRelatedCategoryHeader relatedCategoryId) {
        if (CollectionUtils.isNotEmpty(categories)) {
            categories.remove(relatedCategoryId);
        }
    }

    @Override
    protected StringBuilder toString(final StringBuilder sb) {
        sb.append(", language='").append(language).append('\'');
        sb.append(", usage='").append(usage).append('\'');
        sb.append(", terms=").append(terms);
        sb.append(", categories=").append(categories);

        return sb;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AtlasGlossary)) return false;
        if (!super.equals(o)) return false;
        final AtlasGlossary glossary = (AtlasGlossary) o;
        return Objects.equals(language, glossary.language) &&
                       Objects.equals(usage, glossary.usage) &&
                       Objects.equals(terms, glossary.terms) &&
                       Objects.equals(categories, glossary.categories);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), language, usage, terms, categories);
    }

    @AtlasJSON
    public static class AtlasGlossaryExtInfo extends AtlasGlossary {
        private Map<String, AtlasGlossaryTerm>     termInfo;
        private Map<String, AtlasGlossaryCategory> categoryInfo;

        public AtlasGlossaryExtInfo() {
        }

        public AtlasGlossaryExtInfo(AtlasGlossary glossary) {
            super(glossary);
        }

        public Map<String, AtlasGlossaryTerm> getTermInfo() {
            return termInfo;
        }

        public void addTermInfo(final AtlasGlossaryTerm term) {
            if (termInfo == null) {
                termInfo = new HashMap<>();
            }
            termInfo.put(term.getGuid(), term);
        }

        public void setTermInfo(final Map<String, AtlasGlossaryTerm> termInfo) {
            this.termInfo = termInfo;
        }

        public Map<String, AtlasGlossaryCategory> getCategoryInfo() {
            return categoryInfo;
        }

        public void addCategoryInfo(final AtlasGlossaryCategory category) {
            if (categoryInfo == null) {
                categoryInfo = new HashMap<>();
            }
            categoryInfo.put(category.getGuid(), category);
        }

        public void setCategoryInfo(final Map<String, AtlasGlossaryCategory> categoryInfo) {
            this.categoryInfo = categoryInfo;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof AtlasGlossaryExtInfo)) return false;
            if (!super.equals(o)) return false;
            final AtlasGlossaryExtInfo that = (AtlasGlossaryExtInfo) o;
            return Objects.equals(termInfo, that.termInfo) &&
                           Objects.equals(categoryInfo, that.categoryInfo);
        }

        @Override
        public int hashCode() {

            return Objects.hash(super.hashCode(), termInfo, categoryInfo);
        }

        @Override
        public StringBuilder toString(StringBuilder sb) {
            sb.append(", termInfo=").append(termInfo);
            sb.append(", categoryInfo=").append(categoryInfo);

            return sb;
        }


    }
}
