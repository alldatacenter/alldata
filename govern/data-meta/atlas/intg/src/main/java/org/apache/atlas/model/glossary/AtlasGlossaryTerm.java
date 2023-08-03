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
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.atlas.model.annotation.AtlasJSON;
import org.apache.atlas.model.glossary.relations.AtlasGlossaryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.atlas.model.glossary.relations.AtlasTermCategorizationHeader;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@AtlasJSON
public class AtlasGlossaryTerm extends AtlasGlossaryBaseObject {
    // Core attributes
    private List<String> examples;
    private String       abbreviation;
    private String       usage;

    // Attributes derived from relationships
    private AtlasGlossaryHeader                anchor;
    private Set<AtlasRelatedObjectId>          assignedEntities;
    private Set<AtlasTermCategorizationHeader> categories;

    // Related Terms
    private Set<AtlasRelatedTermHeader> seeAlso;

    // Term Synonyms
    private Set<AtlasRelatedTermHeader> synonyms;

    // Term antonyms
    private Set<AtlasRelatedTermHeader> antonyms;

    // Term preference
    private Set<AtlasRelatedTermHeader> preferredTerms;
    private Set<AtlasRelatedTermHeader> preferredToTerms;

    // Term replacements
    private Set<AtlasRelatedTermHeader> replacementTerms;
    private Set<AtlasRelatedTermHeader> replacedBy;

    // Term translations
    private Set<AtlasRelatedTermHeader> translationTerms;
    private Set<AtlasRelatedTermHeader> translatedTerms;

    // Term classification
    private Set<AtlasRelatedTermHeader> isA;
    private Set<AtlasRelatedTermHeader> classifies;

    // Values for terms
    private Set<AtlasRelatedTermHeader> validValues;
    private Set<AtlasRelatedTermHeader> validValuesFor;

    private boolean hasTerms = false;

    public AtlasGlossaryTerm() {
    }

    public AtlasGlossaryTerm(final AtlasGlossaryTerm other) {
        super(other);
        this.examples = other.examples;
        this.abbreviation = other.abbreviation;
        this.usage = other.usage;
        this.anchor = other.anchor;
        this.assignedEntities = other.assignedEntities;
        this.categories = other.categories;
        this.seeAlso = other.seeAlso;
        this.synonyms = other.synonyms;
        this.antonyms = other.antonyms;
        this.preferredTerms = other.preferredTerms;
        this.preferredToTerms = other.preferredToTerms;
        this.replacementTerms = other.replacementTerms;
        this.replacedBy = other.replacedBy;
        this.translationTerms = other.translationTerms;
        this.translatedTerms = other.translatedTerms;
        this.isA = other.isA;
        this.classifies = other.classifies;
        this.validValues = other.validValues;
        this.validValuesFor = other.validValuesFor;
        this.hasTerms = other.hasTerms;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(final List<String> examples) {
        this.examples = examples;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(final String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(final String usage) {
        this.usage = usage;
    }

    public AtlasGlossaryHeader getAnchor() {
        return anchor;
    }

    public void setAnchor(final AtlasGlossaryHeader anchor) {
        this.anchor = anchor;
    }

    public Set<AtlasTermCategorizationHeader> getCategories() {
        return categories;
    }

    public void setCategories(final Set<AtlasTermCategorizationHeader> categories) {
        this.categories = categories;
    }

    public void addCategory(final AtlasTermCategorizationHeader category) {
        Set<AtlasTermCategorizationHeader> categories = this.categories;
        if (categories == null) {
            categories = new HashSet<>();
        }
        categories.add(category);
        setCategories(categories);
    }

    public Set<AtlasRelatedObjectId> getAssignedEntities() {
        return assignedEntities;
    }

    public void setAssignedEntities(final Set<AtlasRelatedObjectId> assignedEntities) {
        this.assignedEntities = assignedEntities;
    }

    public void addAssignedEntity(final AtlasRelatedObjectId atlasObjectId) {
        Set<AtlasRelatedObjectId> assignedEntities = this.assignedEntities;
        if (assignedEntities == null) {
            assignedEntities = new HashSet<>();
        }
        assignedEntities.add(atlasObjectId);
        setAssignedEntities(assignedEntities);
    }

    public Set<AtlasRelatedTermHeader> getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(final Set<AtlasRelatedTermHeader> seeAlso) {
        this.seeAlso = seeAlso;

        if (CollectionUtils.isNotEmpty(seeAlso)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(final Set<AtlasRelatedTermHeader> synonyms) {
        this.synonyms = synonyms;

        if (CollectionUtils.isNotEmpty(synonyms)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getAntonyms() {
        return antonyms;
    }

    public void setAntonyms(final Set<AtlasRelatedTermHeader> antonyms) {
        this.antonyms = antonyms;

        if (CollectionUtils.isNotEmpty(antonyms)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getPreferredTerms() {
        return preferredTerms;
    }

    public void setPreferredTerms(final Set<AtlasRelatedTermHeader> preferredTerms) {
        this.preferredTerms = preferredTerms;

        if (CollectionUtils.isNotEmpty(preferredTerms)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getPreferredToTerms() {
        return preferredToTerms;
    }

    public void setPreferredToTerms(final Set<AtlasRelatedTermHeader> preferredToTerms) {
        this.preferredToTerms = preferredToTerms;

        if (CollectionUtils.isNotEmpty(preferredToTerms)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getReplacementTerms() {
        return replacementTerms;
    }

    public void setReplacementTerms(final Set<AtlasRelatedTermHeader> replacementTerms) {
        this.replacementTerms = replacementTerms;

        if (CollectionUtils.isNotEmpty(replacementTerms)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(final Set<AtlasRelatedTermHeader> replacedBy) {
        this.replacedBy = replacedBy;

        if (CollectionUtils.isNotEmpty(replacedBy)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getTranslationTerms() {
        return translationTerms;
    }

    public void setTranslationTerms(final Set<AtlasRelatedTermHeader> translationTerms) {
        this.translationTerms = translationTerms;

        if (CollectionUtils.isNotEmpty(translationTerms)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getTranslatedTerms() {
        return translatedTerms;
    }

    public void setTranslatedTerms(final Set<AtlasRelatedTermHeader> translatedTerms) {
        this.translatedTerms = translatedTerms;

        if (CollectionUtils.isNotEmpty(translatedTerms)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getIsA() {
        return isA;
    }

    public void setIsA(final Set<AtlasRelatedTermHeader> isA) {
        this.isA = isA;

        if (CollectionUtils.isNotEmpty(isA)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getClassifies() {
        return classifies;
    }

    public void setClassifies(final Set<AtlasRelatedTermHeader> classifies) {
        this.classifies = classifies;

        if (CollectionUtils.isNotEmpty(classifies)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getValidValues() {
        return validValues;
    }

    public void setValidValues(final Set<AtlasRelatedTermHeader> validValues) {
        this.validValues = validValues;

        if (CollectionUtils.isNotEmpty(validValues)) {
            hasTerms = true;
        }
    }

    public Set<AtlasRelatedTermHeader> getValidValuesFor() {
        return validValuesFor;
    }

    public void setValidValuesFor(final Set<AtlasRelatedTermHeader> validValuesFor) {
        this.validValuesFor = validValuesFor;

        if (CollectionUtils.isNotEmpty(validValuesFor)) {
            hasTerms = true;
        }
    }

    public AtlasGlossaryTermHeader getGlossaryTermHeader() {
        return new AtlasGlossaryTermHeader(this.getGuid(), this.getQualifiedName());
    }

    @JsonIgnore
    public String toAuditString() {
        AtlasGlossaryTerm t = new AtlasGlossaryTerm();
        t.setGuid(this.getGuid());
        t.setName(this.getName());
        t.setQualifiedName(this.getQualifiedName());

        return AtlasType.toJson(t);
    }

    @JsonIgnore
    public boolean hasTerms() {
        return hasTerms;
    }

    @JsonIgnore
    @Override
    public void setAttribute(String attrName, String attrVal) {
        Objects.requireNonNull(attrName, "AtlasGlossaryTerm attribute name");
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
            case "abbreviation":
                setAbbreviation(attrVal);
                break;
            case "usage":
                setUsage(attrVal);
                break;
            default:
                throw new IllegalArgumentException("Invalid attribute '" + attrName + "' for object AtlasGlossaryTerm");
        }
    }

    @JsonIgnore
    public Map<Relation, Set<AtlasRelatedTermHeader>> getRelatedTerms() {
        Map<Relation, Set<AtlasRelatedTermHeader>> ret = new HashMap<>();

        if (CollectionUtils.isNotEmpty(seeAlso)) {
            ret.put(Relation.SEE_ALSO, seeAlso);
        }

        if (CollectionUtils.isNotEmpty(synonyms)) {
            ret.put(Relation.SYNONYMS, synonyms);
        }

        if (CollectionUtils.isNotEmpty(antonyms)) {
            ret.put(Relation.ANTONYMS, antonyms);
        }

        if (CollectionUtils.isNotEmpty(preferredTerms)) {
            ret.put(Relation.PREFERRED_TERMS, preferredTerms);
        }

        if (CollectionUtils.isNotEmpty(preferredToTerms)) {
            ret.put(Relation.PREFERRED_TO_TERMS, preferredToTerms);
        }

        if (CollectionUtils.isNotEmpty(replacementTerms)) {
            ret.put(Relation.REPLACEMENT_TERMS, replacementTerms);
        }

        if (CollectionUtils.isNotEmpty(replacedBy)) {
            ret.put(Relation.REPLACED_BY, replacedBy);
        }

        if (CollectionUtils.isNotEmpty(translationTerms)) {
            ret.put(Relation.TRANSLATION_TERMS, translationTerms);
        }

        if (CollectionUtils.isNotEmpty(translatedTerms)) {
            ret.put(Relation.TRANSLATED_TERMS, translatedTerms);
        }

        if (CollectionUtils.isNotEmpty(isA)) {
            ret.put(Relation.ISA, isA);
        }

        if (CollectionUtils.isNotEmpty(classifies)) {
            ret.put(Relation.CLASSIFIES, classifies);
        }

        if (CollectionUtils.isNotEmpty(validValues)) {
            ret.put(Relation.VALID_VALUES, validValues);
        }

        if (CollectionUtils.isNotEmpty(validValuesFor)) {
            ret.put(Relation.VALID_VALUES_FOR, validValuesFor);
        }

        return ret;
    }

    @Override
    protected StringBuilder toString(final StringBuilder sb) {
        sb.append("examples=").append(examples);
        sb.append(", abbreviation='").append(abbreviation).append('\'');
        sb.append(", usage='").append(usage).append('\'');
        sb.append(", anchor=").append(anchor);
        sb.append(", assignedEntities=").append(assignedEntities);
        sb.append(", categories=").append(categories);
        sb.append(", seeAlso=").append(seeAlso);
        sb.append(", synonyms=").append(synonyms);
        sb.append(", antonyms=").append(antonyms);
        sb.append(", preferredTerms=").append(preferredTerms);
        sb.append(", preferredToTerms=").append(preferredToTerms);
        sb.append(", replacementTerms=").append(replacementTerms);
        sb.append(", replacedBy=").append(replacedBy);
        sb.append(", translationTerms=").append(translationTerms);
        sb.append(", translatedTerms=").append(translatedTerms);
        sb.append(", isA=").append(isA);
        sb.append(", classifies=").append(classifies);
        sb.append(", validValues=").append(validValues);
        sb.append(", validValuesFor=").append(validValuesFor);

        return sb;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AtlasGlossaryTerm)) return false;
        if (!super.equals(o)) return false;
        final AtlasGlossaryTerm that = (AtlasGlossaryTerm) o;
        return Objects.equals(examples, that.examples) &&
                       Objects.equals(abbreviation, that.abbreviation) &&
                       Objects.equals(usage, that.usage) &&
                       Objects.equals(anchor, that.anchor) &&
                       Objects.equals(assignedEntities, that.assignedEntities) &&
                       Objects.equals(categories, that.categories) &&
                       Objects.equals(seeAlso, that.seeAlso) &&
                       Objects.equals(synonyms, that.synonyms) &&
                       Objects.equals(antonyms, that.antonyms) &&
                       Objects.equals(preferredTerms, that.preferredTerms) &&
                       Objects.equals(preferredToTerms, that.preferredToTerms) &&
                       Objects.equals(replacementTerms, that.replacementTerms) &&
                       Objects.equals(replacedBy, that.replacedBy) &&
                       Objects.equals(translationTerms, that.translationTerms) &&
                       Objects.equals(translatedTerms, that.translatedTerms) &&
                       Objects.equals(isA, that.isA) &&
                       Objects.equals(classifies, that.classifies) &&
                       Objects.equals(validValues, that.validValues) &&
                       Objects.equals(validValuesFor, that.validValuesFor);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), examples, abbreviation, usage, anchor, assignedEntities, categories,
                            seeAlso, synonyms, antonyms, preferredTerms, preferredToTerms, replacementTerms, replacedBy,
                            translationTerms, translatedTerms, isA, classifies, validValues, validValuesFor);
    }

    public enum Relation {
        SEE_ALSO("AtlasGlossaryRelatedTerm", "seeAlso"),
        SYNONYMS("AtlasGlossarySynonym", "synonyms"),
        ANTONYMS("AtlasGlossaryAntonym", "antonyms"),
        PREFERRED_TO_TERMS("AtlasGlossaryPreferredTerm", "preferredToTerms", true),
        PREFERRED_TERMS("AtlasGlossaryPreferredTerm", "preferredTerms"),
        REPLACEMENT_TERMS("AtlasGlossaryReplacementTerm", "replacementTerms", true),
        REPLACED_BY("AtlasGlossaryReplacementTerm", "replacedBy"),
        TRANSLATION_TERMS("AtlasGlossaryTranslation", "translationTerms", true),
        TRANSLATED_TERMS("AtlasGlossaryTranslation", "translatedTerms"),
        ISA("AtlasGlossaryIsARelationship", "isA", true),
        CLASSIFIES("AtlasGlossaryIsARelationship", "classifies"),
        VALID_VALUES("AtlasGlossaryValidValue", "validValues", true),
        VALID_VALUES_FOR("AtlasGlossaryValidValue", "validValuesFor"),
        ;

        private String  name;
        private String  attrName;
        private boolean isEnd2Attr;

        Relation(final String name, final String attrName) {
            this(name, attrName, false);
        }

        Relation(final String name, final String attrName, final boolean isEnd2Attr) {
            this.name = name;
            this.attrName = attrName;
            this.isEnd2Attr = isEnd2Attr;
        }

        public String getName() {
            return name;
        }

        @JsonValue
        public String getAttrName() {
            return attrName;
        }

        public boolean isEnd2Attr() {
            return isEnd2Attr;
        }
    }
}
