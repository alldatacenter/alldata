from abc import ABC, abstractmethod
from enum import Enum
from typing import Optional, Union, List, Dict
from uuid import UUID
import json
import re


def to_snake(d, level: int = 0):
    """
    Convert `string`, `list[string]`, or all keys in a `dict` into snake case
    The maximum length of input string or list is 100, or it will be truncated before being processed, for dict, the exception will be thrown if it has more than 100 keys.
    the maximum nested level is 10, otherwise the exception will be thrown
    """
    if level >= 10:
        raise ValueError("Too many nested levels")
    if isinstance(d, str):
        d = d[:100]
        return re.sub(r'(?<!^)(?=[A-Z])', '_', d).lower()
    if isinstance(d, list):
        d = d[:100]
        return [to_snake(i, level + 1) if isinstance(i, (dict, list)) else i for i in d]
    if len(d) > 100:
        raise ValueError("Dict has too many keys")
    return {to_snake(a, level + 1): to_snake(b, level + 1) if isinstance(b, (dict, list)) else b for a, b in d.items()}


def _to_type(value, type):
    """
    Convert `value` into `type`,
    or `list[type]` if `value` is a list
    NOTE: This is **not** a generic implementation, only for objects in this module
    """
    if isinstance(value, type):
        return value
    if isinstance(value, list):
        return list([_to_type(v, type) for v in value])
    if isinstance(value, dict):
        if hasattr(type, "new"):
            try:
                # The convention is to use `new` method to create the object from a dict
                return type.new(**to_snake(value))
            except TypeError:
                pass
        return type(**to_snake(value))
    if issubclass(type, Enum):
        try:
            n = int(value)
            return type(n)
        except ValueError:
            pass
        if hasattr(type, "new"):
            try:
                # As well as Enum types, some of them have alias that cannot be handled by default Enum constructor
                return type.new(value)
            except KeyError:
                pass
        return type[value]
    return type(value)


def _to_uuid(value):
    return _to_type(value, UUID)


class ValueType(Enum):
    UNSPECIFIED = 0
    BOOLEAN = 1
    INT = 2
    LONG = 3
    FLOAT = 4
    DOUBLE = 5
    STRING = 6
    BYTES = 7


class VectorType(Enum):
    TENSOR = 0


class TensorCategory(Enum):
    DENSE = 0
    SPARSE = 1


class EntityType(Enum):
    Project = 1
    Source = 2
    Anchor = 3
    AnchorFeature = 4
    DerivedFeature = 5

    @staticmethod
    def new(v):
        return {
            "feathr_workspace_v1": EntityType.Project,
            "feathr_source_v1": EntityType.Source,
            "feathr_anchor_v1": EntityType.Anchor,
            "feathr_anchor_feature_v1": EntityType.AnchorFeature,
            "feathr_derived_feature_v1": EntityType.DerivedFeature,
        }[v]

    def __str__(self):
        return {
            EntityType.Project: "feathr_workspace_v1",
            EntityType.Source: "feathr_source_v1",
            EntityType.Anchor: "feathr_anchor_v1",
            EntityType.AnchorFeature: "feathr_anchor_feature_v1",
            EntityType.DerivedFeature: "feathr_derived_feature_v1",
        }[self]


class RelationshipType(Enum):
    Contains = 1
    BelongsTo = 2
    Consumes = 4
    Produces = 8

    @staticmethod
    def new(r):
        return {
            "CONTAINS": RelationshipType.Contains,
            "CONTAIN": RelationshipType.Contains,
            "BELONGSTO": RelationshipType.BelongsTo,
            "CONSUMES": RelationshipType.Consumes,
            "PRODUCES": RelationshipType.Produces,
        }[r]

class ToDict(ABC):
    """
    This ABC is used to convert object to dict, then JSON.
    """
    @abstractmethod
    def to_dict(self) -> Dict:
        pass

    def to_json(self, indent=None) -> str:
        return json.dumps(self.to_dict(), indent=indent)


class FeatureType(ToDict):
    def __init__(self,
                 type: Union[str, VectorType],
                 tensor_category: Union[str, TensorCategory],
                 dimension_type: List[Union[str, ValueType]],
                 val_type: Union[str, ValueType]):
        self.type = _to_type(type, VectorType)
        self.tensor_category = _to_type(tensor_category, TensorCategory)
        self.dimension_type = _to_type(dimension_type, ValueType)
        self.val_type = _to_type(val_type, ValueType)

    def __eq__(self, o: object) -> bool:
        return self.type == o.type \
            and self.tensor_category == o.tensor_category \
            and self.dimension_type == o.dimension_type \
            and self.val_type == o.val_type

    def to_dict(self) -> Dict:
        return {
            "type": self.type.name,
            "tensorCategory": self.tensor_category.name,
            "dimensionType": [t.name for t in self.dimension_type],
            "valType": self.val_type.name,
        }


class TypedKey(ToDict):
    def __init__(self,
                 key_column: str,
                 key_column_type: ValueType,
                 full_name: Optional[str] = None,
                 description: Optional[str] = None,
                 key_column_alias: Optional[str] = None):
        self.key_column = key_column
        self.key_column_type = _to_type(key_column_type, ValueType)
        self.full_name = full_name
        self.description = description
        self.key_column_alias = key_column_alias

    def __eq__(self, o: object) -> bool:
        if not isinstance(o, TypedKey):
            return False
        return self.key_column == o.key_column \
            and self.key_column_type == o.key_column_type \
            and self.key_column_alias == o.key_column_alias
    
    def to_dict(self) -> Dict:
        ret = {
            "key_column": self.key_column,
            "key_column_type": self.key_column_type.name,
        }
        if self.full_name is not None:
            ret["full_name"] = self.full_name
        if self.description is not None:
            ret["description"] = self.full_name
        if self.key_column_alias is not None:
            ret["key_column_alias"] = self.key_column_alias
        return ret


class Transformation(ToDict):
    @staticmethod
    def new(**kwargs):
        if "transform_expr" in kwargs:
            return ExpressionTransformation(**kwargs)
        elif "def_expr" in kwargs:
            return WindowAggregationTransformation(**kwargs)
        elif "name" in kwargs:
            return UdfTransformation(**kwargs)
        else:
            raise ValueError(kwargs)


class ExpressionTransformation(Transformation):
    def __init__(self, transform_expr: str):
        self.transform_expr = transform_expr

    def __eq__(self, o: object) -> bool:
        if not isinstance(o, ExpressionTransformation):
            return False
        return self.transform_expr == o.transform_expr

    def to_dict(self) -> Dict:
        return {
            "transform_expr": self.transform_expr
        }


class WindowAggregationTransformation(Transformation):
    def __init__(self,
                 def_expr: str,
                 agg_func: Optional[str] = None,
                 window: Optional[str] = None,
                 group_by: Optional[str] = None,
                 filter: Optional[str] = None,
                 limit: Optional[int] = None):
        self.def_expr = def_expr
        self.agg_func = agg_func
        self.window = window
        self.group_by = group_by
        self.filter = filter
        self.limit = limit

    def __eq__(self, o: object) -> bool:
        if not isinstance(o, WindowAggregationTransformation):
            return False
        return self.def_expr == o.def_expr \
            and self.agg_func == o.agg_func \
            and self.window == o.window \
            and self.group_by == o.group_by \
            and self.filter == o.filter \
            and self.limit == o.limit

    def to_dict(self) -> Dict:
        ret = {
            "def_expr": self.def_expr,
        }
        if self.agg_func is not None:
            ret["agg_func"] = self.agg_func
        if self.window is not None:
            ret["window"] = self.window
        if self.group_by is not None:
            ret["group_by"] = self.group_by
        if self.filter is not None:
            ret["filter"] = self.filter
        if self.limit is not None:
            ret["limit"] = self.limit
        return ret


class UdfTransformation(Transformation):
    def __init__(self, name: str):
        self.name = name

    def __eq__(self, o: object) -> bool:
        if not isinstance(o, UdfTransformation):
            return False
        return self.name == o.name

    def to_dict(self) -> Dict:
        return {
            "name": self.name
        }


class EntityRef(ToDict):
    def __init__(self,
                 id: UUID,
                 type: Union[str, EntityType],
                 qualified_name: Optional[str] = None,
                 uniq_attr: Dict = {}):
        self.id = id
        self.type = _to_type(type, EntityType)
        if qualified_name is not None:
            self.uniq_attr = {"qualifiedName": qualified_name}
        else:
            self.uniq_attr = uniq_attr

    @property
    def entity_type(self) -> EntityType:
        return self.type

    @property
    def qualified_name(self) -> EntityType:
        return self.uniq_attr['qualifiedName']

    def get_ref(self):
        return self

    def to_dict(self) -> Dict:
        return {
            "guid": str(self.id),
            "typeName": str(self.type),
            "uniqueAttributes": self.uniq_attr,
        }


class Attributes(ToDict):
    @staticmethod
    def new(entity_type: Union[str, EntityType], **kwargs):
        return {
            EntityType.Project: ProjectAttributes,
            EntityType.Source: SourceAttributes,
            EntityType.Anchor: AnchorAttributes,
            EntityType.AnchorFeature: AnchorFeatureAttributes,
            EntityType.DerivedFeature: DerivedFeatureAttributes,
        }[_to_type(entity_type, EntityType)](**kwargs)


class Entity(ToDict):
    def __init__(self,
                 entity_id: Union[str, UUID],
                 qualified_name: str,
                 entity_type: Union[str, EntityType],
                 attributes: Union[Dict, Attributes],
                 **kwargs):
        self.id = _to_uuid(entity_id)
        self.qualified_name = qualified_name
        self.entity_type = _to_type(entity_type, EntityType)
        if isinstance(attributes, Attributes):
            self.attributes = attributes
        else:
            self.attributes = Attributes.new(
                entity_type, **to_snake(attributes))

    def get_ref(self) -> EntityRef:
        return EntityRef(self.id,
                         self.attributes.entity_type,
                         self.qualified_name)

    def to_dict(self) -> Dict:
        return {
            "guid": str(self.id),
            "lastModifiedTS": "1",
            "status": "ACTIVE",
            "displayText": self.attributes.name,
            "typeName": str(self.attributes.entity_type),
            "attributes": self.attributes.to_dict(),
        }

    def to_min_repr(self) -> Dict:
        return {
            'qualifiedName':self.qualified_name,
            'guid':str(self.id),
            'typeName':str(self.attributes.entity_type),
        }


class ProjectAttributes(Attributes):
    def __init__(self,
                 name: str,
                 children: List[Union[Dict, Entity]] = [],
                 tags: Dict = {},
                 **kwargs):
        self.name = name
        self.tags = tags
        self._children = []
        if len(children) > 0:
            self.children = children

    @property
    def entity_type(self) -> EntityType:
        return EntityType.Project

    @property
    def children(self):
        return self._children

    @children.setter
    def children(self, v: List[Union[Dict, Entity]]):
        for f in v:
            if isinstance(f, Entity):
                self._children.append(f)
            elif isinstance(f, dict):
                self._children.append(_to_type(f, Entity))
            else:
                raise TypeError(f)

    @property
    def sources(self):
        return [
            e for e in self.children if e.entity_type == EntityType.Source]

    @property
    def anchors(self):
        return [
            e for e in self.children if e.entity_type == EntityType.Anchor]

    @property
    def anchor_features(self):
        return [
            e for e in self.children if e.entity_type == EntityType.AnchorFeature]

    @property
    def derived_features(self):
        return [
            e for e in self.children if e.entity_type == EntityType.DerivedFeature]

    def to_dict(self) -> Dict:
        return {
            "qualifiedName": self.name,
            "name": self.name,
            "sources": list([e.get_ref().to_dict() for e in self.sources]),
            "anchors": list([e.get_ref().to_dict() for e in self.anchors]),
            "anchor_features": list([e.get_ref().to_dict() for e in self.anchor_features]),
            "derived_features": list([e.get_ref().to_dict() for e in self.derived_features]),
            "tags": self.tags,
        }


class SourceAttributes(Attributes):
    def __init__(self,
                 qualified_name: str,
                 name: str,
                 type: str,
                 path: str,
                 preprocessing: Optional[str] = None,
                 event_timestamp_column: Optional[str] = None,
                 timestamp_format: Optional[str] = None,
                 tags: Dict = {}):
        self.qualified_name = qualified_name
        self.name = name
        self.type = type
        self.path = path
        self.preprocessing = preprocessing
        self.event_timestamp_column = event_timestamp_column
        self.timestamp_format = timestamp_format
        self.tags = tags

    @property
    def entity_type(self) -> EntityType:
        return EntityType.Source

    def to_dict(self) -> Dict:
        ret = {
            "qualifiedName": self.qualified_name,
            "name": self.name,
            "type": self.type,
            "path": self.path,
            "tags": self.tags,
        }
        if self.preprocessing is not None:
            ret["preprocessing"] = self.preprocessing
        if self.event_timestamp_column is not None:
            ret["eventTimestampColumn"] = self.event_timestamp_column
            ret["event_timestamp_column"] = self.event_timestamp_column
        if self.timestamp_format is not None:
            ret["timestampFormat"] = self.timestamp_format
            ret["timestamp_format"] = self.timestamp_format

        return ret


class AnchorAttributes(Attributes):
    def __init__(self,
                 qualified_name: str,
                 name: str,
                 # source: Optional[Union[Dict, EntityRef, Entity]] = None,
                 # features: list[Union[Dict, EntityRef, Entity]] = [],
                 tags: Dict = {},
                 **kwargs):
        self.qualified_name = qualified_name
        self.name = name
        self._source = None
        self._features = []
        # if source is not None:
        #     self._source = _to_type(source, Entity).get_ref()
        # if features:
        #     self.features = features
        self.tags = tags
        if 'source' in kwargs:
            self._source = kwargs['source']

    @property
    def entity_type(self) -> EntityType:
        return EntityType.Anchor

    @property
    def source(self) -> EntityRef:
        return self._source

    @source.setter
    def source(self, s):
        if isinstance(s, Entity):
            self._source = s.get_ref()
        elif isinstance(s, EntityRef):
            self._source = s
        elif isinstance(s, dict):
            self._source = _to_type(s, Entity).get_ref()
        else:
            raise TypeError(s)

    @property
    def features(self):
        return self._features

    @features.setter
    def features(self, features):
        self._features = []
        for f in features:
            if isinstance(f, Entity):
                self._features.append(f.get_ref())
            elif isinstance(f, EntityRef):
                self._features.append(f)
            elif isinstance(f, dict):
                self._features.append(_to_type(f, Entity).get_ref())
            else:
                raise TypeError(f)

    def to_dict(self) -> Dict:
        ret = {
            "qualifiedName": self.qualified_name,
            "name": self.name,
            "features": list([e.get_ref().to_dict() for e in self.features]),
            "tags": self.tags,
        }
        if self.source is not None and isinstance(self.source, EntityRef):
            source_ref = self.source.get_ref()
            if source_ref is not None:
                ret["source"] = source_ref.to_dict() 
        return ret


class AnchorFeatureAttributes(Attributes):
    def __init__(self,
                 qualified_name: str,
                 name: str,
                 type: Union[Dict, FeatureType],
                 transformation: Union[Dict, Transformation],
                 key: List[Union[Dict, TypedKey]],
                 tags: Dict = {}):
        self.qualified_name = qualified_name
        self.name = name
        self.type = _to_type(type, FeatureType)
        self.transformation = _to_type(transformation, Transformation)
        self.key = _to_type(key, TypedKey)
        self.tags = tags

    @property
    def entity_type(self) -> EntityType:
        return EntityType.AnchorFeature

    def to_dict(self) -> Dict:
        return {
            "qualifiedName": self.qualified_name,
            "name": self.name,
            "type": self.type.to_dict(),
            "transformation": self.transformation.to_dict(),
            "key": list([k.to_dict() for k in self.key]),
            "tags": self.tags,
        }


class DerivedFeatureAttributes(Attributes):
    def __init__(self,
                 qualified_name: str,
                 name: str,
                 type: Union[Dict, FeatureType],
                 transformation: Union[Dict, Transformation],
                 key: List[Union[Dict, TypedKey]],
                 input_anchor_features: List[Union[Dict, EntityRef, Entity]] = [],
                 input_derived_features: List[Union[Dict, EntityRef, Entity]] = [],
                 tags: Dict = {},
                 **kwargs):
        self.qualified_name = qualified_name
        self.name = name
        self.type = _to_type(type, FeatureType)
        self.transformation = _to_type(transformation, Transformation)
        self.key = _to_type(key, TypedKey)
        self._input_anchor_features = []
        self._input_derived_features = []
        self.tags = tags

    @property
    def entity_type(self) -> EntityType:
        return EntityType.DerivedFeature

    @property
    def input_features(self):
        return self._input_anchor_features + self._input_derived_features

    @input_features.setter
    def input_features(self, input_features_list: Union[Dict, Entity, EntityRef]):
        self._input_anchor_features = []
        self._input_derived_features = []
        for feature in input_features_list:
            entity = None
            if isinstance(feature, EntityRef):
                entity = feature
            elif isinstance(feature, Entity):
                entity = feature.get_ref()
            elif isinstance(feature, dict):
                try:
                    entity = _to_type(feature, Entity).get_ref()
                except:
                    entity = _to_type(feature, EntityRef)
            else:
                raise TypeError(feature)

            if entity.entity_type == EntityType.AnchorFeature:
                self._input_anchor_features.append(entity)
            elif entity.entity_type == EntityType.DerivedFeature:
                self._input_derived_features.append(entity)
            else:
                pass

    @property
    def input_anchor_features(self):
        return self._input_anchor_features

    @property
    def input_derived_features(self):
        return self._input_derived_features

    def to_dict(self) -> Dict:
        return {
            "qualifiedName": self.qualified_name,
            "name": self.name,
            "type": self.type.to_dict(),
            "transformation": self.transformation.to_dict(),
            "key": list([k.to_dict() for k in self.key]),
            "input_anchor_features": [e.to_dict() for e in self.input_anchor_features],
            "input_derived_features": [e.to_dict() for e in self.input_derived_features],
            "tags": self.tags,
        }


class Edge(ToDict):
    def __init__(self,
                 edge_id: Union[str, UUID],
                 from_id: Union[str, UUID],
                 to_id: Union[str, UUID],
                 conn_type: Union[str, RelationshipType]):
        self.id = _to_uuid(edge_id)
        self.from_id = _to_uuid(from_id)
        self.to_id = _to_uuid(to_id)
        self.conn_type = _to_type(conn_type, RelationshipType)

    def __eq__(self, o: object) -> bool:
        # Edge ID is kinda useless
        return self.from_id == o.from_id and self.to_id == o.to_id and self.conn_type == o.conn_type

    def __hash__(self) -> int:
        return hash((self.from_id, self.to_id, self.conn_type))

    def to_dict(self) -> Dict:
        return {
            "relationshipId": str(self.id),
            "fromEntityId": str(self.from_id),
            "toEntityId": str(self.to_id),
            "relationshipType": self.conn_type.name,
            "relationshipTypeValue": self.conn_type.value,
        }


class EntitiesAndRelations(ToDict):
    def __init__(self, entities: List[Entity], edges: List[Edge]):
        self.entities = dict([(e.id, e) for e in entities])
        self.edges = set(edges)

    def to_dict(self) -> Dict:
        return {
            "guidEntityMap": dict([(str(id), self.entities[id].to_dict()) for id in self.entities]),
            "relations": list([e.to_dict() for e in self.edges]),
        }


class ProjectDef:
    def __init__(self, name: str, qualified_name: str = "", tags: Dict = {}):
        self.name = name
        self.qualified_name = qualified_name
        self.tags = tags
    
    def to_attr(self) -> ProjectAttributes:
        return ProjectAttributes(name=self.name, tags=self.tags)


class SourceDef:
    def __init__(self,
                 name: str,
                 path: str,
                 type: str,
                 qualified_name: str = "",
                 preprocessing: Optional[str] = None,
                 event_timestamp_column: Optional[str] = None,
                 timestamp_format: Optional[str] = None,
                 tags: Dict = {}):
        self.qualified_name = qualified_name
        self.name = name
        self.path = path
        self.type = type
        self.preprocessing = preprocessing
        self.event_timestamp_column = event_timestamp_column
        self.timestamp_format = timestamp_format
        self.tags = tags

    def to_attr(self) -> SourceAttributes:
        return SourceAttributes(qualified_name=self.qualified_name,
                                name=self.name,
                                type=self.type,
                                path=self.path,
                                preprocessing=self.preprocessing,
                                event_timestamp_column=self.event_timestamp_column,
                                timestamp_format=self.timestamp_format,
                                tags=self.tags)

class AnchorDef:
    def __init__(self,
                 name: str,
                 source_id: Union[str, UUID],
                 qualified_name: str = "",
                 tags: Dict = {}):
        self.qualified_name = qualified_name
        self.name = name
        self.source_id = _to_uuid(source_id)
        self.tags = tags

    def to_attr(self, source: EntityRef) -> AnchorAttributes:
        attr = AnchorAttributes(qualified_name=self.qualified_name,
                                name=self.name,
                                tags=self.tags)
        attr.source = source
        return attr

class AnchorFeatureDef:
    def __init__(self,
                 name: str,
                 feature_type: Union[Dict, FeatureType],
                 transformation: Union[Dict, Transformation],
                 key: List[Union[Dict, TypedKey]],
                 qualified_name: str = "",
                 tags: Dict = {}):
        self.qualified_name = qualified_name
        self.name = name
        self.feature_type = _to_type(feature_type, FeatureType)
        self.transformation = _to_type(transformation, Transformation)
        self.key = _to_type(key, TypedKey)
        self.tags = tags

    def to_attr(self) -> AnchorFeatureAttributes:
        return AnchorFeatureAttributes(qualified_name=self.qualified_name,
                                name=self.name,
                                type=self.feature_type,
                                transformation=self.transformation,
                                key=self.key,
                                tags=self.tags)


class DerivedFeatureDef:
    def __init__(self,
                 name: str,
                 feature_type: Union[Dict, FeatureType],
                 transformation: Union[Dict, Transformation],
                 key: List[Union[Dict, TypedKey]],
                 input_anchor_features: List[Union[str, UUID]],
                 input_derived_features: List[Union[str, UUID]],
                 qualified_name: str = "",
                 tags: Dict = {}):
        self.qualified_name = qualified_name
        self.name = name
        self.feature_type = _to_type(feature_type, FeatureType)
        self.transformation = _to_type(transformation, Transformation)
        self.key = _to_type(key, TypedKey)
        self.input_anchor_features = _to_uuid(input_anchor_features)
        self.input_derived_features = _to_uuid(input_derived_features)
        self.tags = tags

    def to_attr(self, input_features: List[EntityRef]) -> DerivedFeatureAttributes:
        attr = DerivedFeatureAttributes(qualified_name=self.qualified_name,
                                name=self.name,
                                type=self.feature_type,
                                transformation=self.transformation,
                                key=self.key,
                                tags=self.tags)
        attr.input_features = input_features
        return attr
