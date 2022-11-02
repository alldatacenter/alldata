#!/usr/bin/env/python
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
from apache_atlas.model.misc import AtlasBase, AtlasBaseModelObject
from apache_atlas.utils import type_coerce, type_coerce_dict, type_coerce_list


class AtlasGlossaryBaseObject(AtlasBaseModelObject):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBaseModelObject.__init__(self, attrs)

        self.qualifiedName = attrs.get('qualifiedName')
        self.name = attrs.get('name')
        self.shortDescription = attrs.get('shortDescription')
        self.longDescription = attrs.get('longDescription')
        self.additionalAttributes = attrs.get('additionalAttributes')
        self.classifications = attrs.get('classifications')

    def type_coerce_attrs(self):
        # This is to avoid the circular dependencies that instance.py and glossary.py has.
        import apache_atlas.model.instance as instance

        super(AtlasGlossaryBaseObject, self).type_coerce_attrs()
        self.classifications = type_coerce_list(self.classifications, instance.AtlasClassification)


class AtlasGlossary(AtlasGlossaryBaseObject):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasGlossaryBaseObject.__init__(self, attrs)

        self.language = attrs.get('language')
        self.usage = attrs.get('usage')
        self.terms = attrs.get('terms')
        self.categories = attrs.get('categories')

    def type_coerce_attrs(self):
        super(AtlasGlossary, self).type_coerce_attrs()

        self.terms = type_coerce_list(self.classifications, AtlasRelatedTermHeader)
        self.categories = type_coerce_list(self.categories, AtlasRelatedCategoryHeader)


class AtlasGlossaryExtInfo(AtlasGlossary):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasGlossary.__init__(self, attrs)

        self.termInfo = attrs.get('termInfo')
        self.categoryInfo = attrs.get('categoryInfo')

    def type_coerce_attrs(self):
        super(AtlasGlossaryExtInfo, self).type_coerce_attrs()

        self.termInfo = type_coerce_dict(self.termInfo, AtlasGlossaryTerm)
        self.categoryInfo = type_coerce_dict(self.categoryInfo, AtlasGlossaryCategory)


class AtlasGlossaryCategory(AtlasGlossaryBaseObject):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasGlossaryBaseObject.__init__(self, attrs)

        # Inherited attributes from relations
        self.anchor = attrs.get('anchor')

        # Category hierarchy links
        self.parentCategory = attrs.get('parentCategory')
        self.childrenCategories = attrs.get('childrenCategories')

        # Terms associated with this category
        self.terms = attrs.get('terms')

    def type_coerce_attrs(self):
        super(AtlasGlossaryCategory, self).type_coerce_attrs()

        self.anchor = type_coerce(self.anchor, AtlasGlossaryHeader)
        self.parentCategory = type_coerce(self.parentCategory, AtlasRelatedCategoryHeader)
        self.childrenCategories = type_coerce_list(self.childrenCategories, AtlasRelatedCategoryHeader)
        self.terms = type_coerce_list(self.terms, AtlasRelatedTermHeader)


class AtlasGlossaryTerm(AtlasGlossaryBaseObject):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasGlossaryBaseObject.__init__(self, attrs)

        # Core attributes
        self.examples = attrs.get('examples')
        self.abbreviation = attrs.get('abbreviation')
        self.usage = attrs.get('usage')

        # Attributes derived from relationships
        self.anchor = attrs.get('anchor')
        self.assignedEntities = attrs.get('assignedEntities')
        self.categories = attrs.get('categories')

        # Related Terms
        self.seeAlso = attrs.get('seeAlso')

        # Term Synonyms
        self.synonyms = attrs.get('synonyms')

        # Term antonyms
        self.antonyms = attrs.get('antonyms')

        # Term preference
        self.preferredTerms = attrs.get('preferredTerms')
        self.preferredToTerms = attrs.get('preferredToTerms')

        # Term replacements
        self.replacementTerms = attrs.get('replacementTerms')
        self.replacedBy = attrs.get('replacedBy')

        # Term translations
        self.translationTerms = attrs.get('translationTerms')
        self.translatedTerms = attrs.get('translatedTerms')

        # Term classification
        self.isA = attrs.get('isA')
        self.classifies = attrs.get('classifies')

        # Values for terms
        self.validValues = attrs.get('validValues')
        self.validValuesFor = attrs.get('validValuesFor')

    def type_coerce_attrs(self):
        super(AtlasGlossaryTerm, self).type_coerce_attrs()

        # This is to avoid the circular dependencies that instance.py and glossary.py has.
        import apache_atlas.model.instance as instance

        self.anchor = type_coerce(self.anchor, AtlasGlossaryHeader)
        self.assignedEntities = type_coerce_list(self.assignedEntities, instance.AtlasRelatedObjectId)
        self.categories = type_coerce_list(self.categories, AtlasTermCategorizationHeader)
        self.seeAlso = type_coerce_list(self.seeAlso, AtlasRelatedTermHeader)
        self.synonyms = type_coerce_list(self.synonyms, AtlasRelatedTermHeader)
        self.antonyms = type_coerce_list(self.antonyms, AtlasRelatedTermHeader)
        self.preferredTerms = type_coerce_list(self.preferredTerms, AtlasRelatedTermHeader)
        self.preferredToTerms = type_coerce_list(self.preferredToTerms, AtlasRelatedTermHeader)
        self.replacementTerms = type_coerce_list(self.replacementTerms, AtlasRelatedTermHeader)
        self.replacedBy = type_coerce_list(self.replacedBy, AtlasRelatedTermHeader)
        self.translationTerms = type_coerce_list(self.translationTerms, AtlasRelatedTermHeader)
        self.isA = type_coerce_list(self.isA, AtlasRelatedTermHeader)
        self.classifies = type_coerce_list(self.classifies, AtlasRelatedTermHeader)
        self.validValues = type_coerce_list(self.validValues, AtlasRelatedTermHeader)
        self.validValuesFor = type_coerce_list(self.validValuesFor, AtlasRelatedTermHeader)


class AtlasGlossaryHeader(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.glossaryGuid = attrs.get('glossaryGuid')
        self.relationGuid = attrs.get('relationGuid')
        self.displayText = attrs.get('displayText')


class AtlasRelatedCategoryHeader(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.categoryGuid = attrs.get('categoryGuid')
        self.parentCategoryGuid = attrs.get('parentCategoryGuid')
        self.relationGuid = attrs.get('relationGuid')
        self.displayText = attrs.get('displayText')
        self.description = attrs.get('description')


class AtlasRelatedTermHeader(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.termGuid = attrs.get('termGuid')
        self.relationGuid = attrs.get('relationGuid')
        self.displayText = attrs.get('displayText')
        self.description = attrs.get('description')
        self.expression = attrs.get('expression')
        self.steward = attrs.get('steward')
        self.source = attrs.get('source')
        self.status = attrs.get('status')


class AtlasTermAssignmentHeader(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.termGuid = attrs.get('termGuid')
        self.relationGuid = attrs.get('relationGuid')
        self.description = attrs.get('description')
        self.displayText = attrs.get('displayText')
        self.expression = attrs.get('expression')
        self.createdBy = attrs.get('createdBy')
        self.steward = attrs.get('steward')
        self.source = attrs.get('source')
        self.confidence = attrs.get('confidence')


class AtlasTermCategorizationHeader(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.categoryGuid = attrs.get('categoryGuid')
        self.relationGuid = attrs.get('relationGuid')
        self.description = attrs.get('description')
        self.displayText = attrs.get('displayText')
        self.status = attrs.get('status')
