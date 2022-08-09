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
# Unless required by applicabwle law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import enum

from apache_atlas.model.enums import QueryType
from apache_atlas.model.instance import AtlasEntityHeader
from apache_atlas.model.misc import AtlasBase
from apache_atlas.model.misc import AtlasBaseModelObject
from apache_atlas.utils import non_null
from apache_atlas.utils import type_coerce
from apache_atlas.utils import type_coerce_dict
from apache_atlas.utils import type_coerce_dict_list
from apache_atlas.utils import type_coerce_list


class AtlasAggregationEntry(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.name = attrs.get('name')
        self.name = attrs.get('count')


class AtlasQuickSearchResult(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.searchResults = attrs.get('searchResults')
        self.aggregationMetrics = attrs.get('aggregationMetrics')

    def type_coerce_attrs(self):
        super(AtlasQuickSearchResult, self).type_coerce_attrs()

        self.searchResults = type_coerce(self.searchResults, AtlasSearchResult)
        self.aggregationMetrics = type_coerce_dict_list(self.aggregationMetrics, AtlasAggregationEntry)


class AtlasSearchResult(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.queryType = non_null(attrs.get('queryType'), QueryType.BASIC.name)
        self.searchParameters = attrs.get('searchParameters')
        self.queryText = attrs.get('queryText')
        self.type = attrs.get('type')
        self.classification = attrs.get('classification')
        self.entities = attrs.get('entities')
        self.attributes = attrs.get('attributes')
        self.fullTextResult = attrs.get('fullTextResult')
        self.referredEntities = attrs.get('referredEntities')
        self.approximateCount = non_null(attrs.get('approximateCount'), -1)

    def type_coerce_attrs(self):
        super(AtlasSearchResult, self).type_coerce_attrs()

        self.entities = type_coerce_list(self.entities, AtlasEntityHeader)
        self.attributes = type_coerce(self.attributes, AttributeSearchResult)
        self.referredEntities = type_coerce_dict(self.referredEntities, AtlasEntityHeader)


class AttributeSearchResult(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.name = attrs.get('name')
        self.values = attrs.get('values')


class AtlasFullTextResult(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.entity = attrs.get('entity')
        self.score = attrs.get('score')

    def type_coerce_attrs(self):
        super(AtlasFullTextResult, self).type_coerce_attrs()

        self.entity = type_coerce(self.criterion, AtlasEntityHeader)


class AtlasSuggestionsResult(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.suggestions = attrs.get('suggestions')
        self.prefixString = attrs.get('prefixString')
        self.fieldName = attrs.get('fieldName')


class QuickSearchParameters(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.query = attrs.get('query')
        self.typeName = attrs.get('typeName')
        self.entityFilters = attrs.get('entityFilters')
        self.includeSubTypes = attrs.get('includeSubTypes')
        self.excludeDeletedEntities = attrs.get('excludeDeletedEntities')
        self.offset = attrs.get('offset')
        self.limit = attrs.get('limit')
        self.attributes = attrs.get('attributes')

    def type_coerce_attrs(self):
        super(QuickSearchParameters, self).type_coerce_attrs()

        self.entityFilters = type_coerce(self.entityFilters, FilterCriteria)


class SearchParameters(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.query = attrs.get('query')
        self.typeName = attrs.get('typeName')
        self.classification = attrs.get('classification')
        self.termName = attrs.get('termName')
        self.sortBy = attrs.get('sortBy')
        self.excludeDeletedEntities = attrs.get('excludeDeletedEntities')
        self.includeClassificationAttributes = attrs.get('includeClassificationAttributes')
        self.includeSubTypes = non_null(attrs.get('includeSubTypes'), True)
        self.includeSubClassifications = non_null(attrs.get('includeSubClassifications'), True)
        self.limit = attrs.get('limit')
        self.offset = attrs.get('offset')
        self.entityFilters = attrs.get('entityFilters')
        self.tagFilters = attrs.get('tagFilters')
        self.attributes = attrs.get('attributes')
        self.sortOrder = attrs.get('sortOrder')

    def type_coerce_attrs(self):
        super(SearchParameters, self).type_coerce_attrs()

        self.entityFilters = type_coerce(self.entityFilters, FilterCriteria)
        self.tagFilters = type_coerce(self.tagFilters, FilterCriteria)


class FilterCriteria(AtlasBase):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBase.__init__(self, attrs)

        self.attributeName = attrs.get('attributeName')
        self.operator = attrs.get('operator')
        self.attributeValue = attrs.get('attributeValue')
        self.condition = attrs.get('condition')
        self.criterion = attrs.get('criterion')

    def type_coerce_attrs(self):
        super(FilterCriteria, self).type_coerce_attrs()

        self.criterion = type_coerce(self.criterion, FilterCriteria)


class AtlasUserSavedSearch(AtlasBaseModelObject):
    def __init__(self, attrs=None):
        attrs = attrs or {}

        AtlasBaseModelObject.__init__(self, attrs)

        self.ownerName = attrs.get('ownerName')
        self.name = attrs.get('name')
        self.searchType = attrs.get('searchType')
        self.searchParameters = attrs.get('searchParameters')
        self.uiParameters = attrs.get('uiParameters')

    def type_coerce_attrs(self):
        super(AtlasUserSavedSearch, self).type_coerce_attrs()

        self.searchParameters = type_coerce(self.searchParameters, SearchParameters)


class Operator(enum.Enum):
    LT = ("<", "lt")
    GT = ('>', 'gt')
    LTE = ('<=', 'lte')
    GTE = ('>=', 'gte')
    EQ = ('=', 'eq')
    NEQ = ('!=', 'neq')
    IN = ('in', 'IN')
    LIKE = ('like', 'LIKE')
    STARTS_WITH = ('startsWith', 'STARTSWITH', 'begins_with', 'BEGINS_WITH')
    ENDS_WITH = ('endsWith', 'ENDSWITH', 'ends_with', 'ENDS_WITH')
    CONTAINS = ('contains', 'CONTAINS')
    NOT_CONTAINS = ('not_contains', 'NOT_CONTAINS')
    CONTAINS_ANY = ('containsAny', 'CONTAINSANY', 'contains_any', 'CONTAINS_ANY')
    CONTAINS_ALL = ('containsAll', 'CONTAINSALL', 'contains_all', 'CONTAINS_ALL')
    IS_NULL = ('isNull', 'ISNULL', 'is_null', 'IS_NULL')
    NOT_NULL = ('notNull', 'NOTNULL', 'not_null', 'NOT_NULL')


class SortOrder(enum.Enum):
    sort_order = enum.Enum('sort_order', 'ASCENDING DESCENDING', module=__name__)
