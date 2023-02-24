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


class AtlasTermAssignmentStatus(enum.Enum):
    DISCOVERED = 0
    PROPOSED = 1
    IMPORTED = 2
    VALIDATED = 3
    DEPRECATED = 4
    OBSOLETE = 5
    OTHER = 6


class AtlasTermRelationshipStatus(enum.Enum):
    DRAFT = 0
    ACTIVE = 1
    DEPRECATED = 2
    OBSOLETE = 3
    OTHER = 99


class TypeCategory(enum.Enum):
    PRIMITIVE = 0
    OBJECT_ID_TYPE = 1
    ENUM = 2
    STRUCT = 3
    CLASSIFICATION = 4
    ENTITY = 5
    ARRAY = 6
    MAP = 7
    RELATIONSHIP = 8
    BUSINESS_METADATA = 9


class Cardinality(enum.Enum):
    SINGLE = 0
    LIST = 1
    SET = 2


class Condition(enum.Enum):
    AND = 0
    OR = 1


class EntityOperation(enum.Enum):
    CREATE = 0
    UPDATE = 1
    PARTIAL_UPDATE = 2
    DELETE = 3
    PURGE = 4


class EntityStatus(enum.Enum):
    ACTIVE = 0
    DELETED = 1
    PURGED = 2


class IndexType(enum.Enum):
    DEFAULT = 0
    STRING = 1


class LineageDirection(enum.Enum):
    INPUT = 0
    OUTPUT = 1
    BOTH = 2


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


class PropagateTags(enum.Enum):
    NONE = 0
    ONE_TO_TWO = 1
    TWO_TO_ONE = 2
    BOTH = 3


class QueryType(enum.Enum):
    DSL = 0
    FULL_TEXT = 1
    GREMLIN = 2
    BASIC = 3
    ATTRIBUTE = 4
    RELATIONSHIP = 5


class RelationshipCategory(enum.Enum):
    ASSOCIATION = 0
    AGGREGATION = 1
    COMPOSITION = 2


class RelationshipStatus(enum.Enum):
    ACTIVE = 0
    DELETED = 1


class SavedSearchType(enum.Enum):
    BASIC = 0
    ADVANCED = 1


class SortOrder(enum.Enum):
    ASCENDING = 0
    DESCENDING = 1


class SortType(enum.Enum):
    NONE = 0
    ASC = 1
    DESC = 2
