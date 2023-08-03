import inspect
from re import sub
from typing import List
from urllib.parse import urlparse
from feathr.constants import INPUT_CONTEXT
from feathr.definition.anchor import FeatureAnchor
from feathr.definition.dtype import FeatureType, str_to_value_type, value_type_to_str
from feathr.definition.feature import Feature
from feathr.definition.feature_derivations import DerivedFeature
from feathr.definition.source import HdfsSource, JdbcSource, Source, SnowflakeSource
from pyapacheatlas.core import AtlasProcess,AtlasEntity

from feathr.definition.transformation import ExpressionTransformation, Transformation, WindowAggTransformation
from feathr.definition.typed_key import TypedKey
def to_camel(s):
    if not s:
        return s
    if isinstance(s, str):
        if "_" in s:
            s = sub(r"(_)+", " ", s).title().replace(" ", "")
            return ''.join([s[0].lower(), s[1:]])
        return s
    elif isinstance(s, list):
        return [to_camel(i) for i in s]
    elif isinstance(s, dict):
        return dict([(to_camel(k), s[k]) for k in s])
                                  
def source_to_def(v: Source) -> dict:
    # Note that after this method, attributes are Camel cased (eventTimestampColumn). 
    # If the old logic works with snake case (event_timestamp_column), make sure you handle them manually. 
    ret = {}
    if v.name == INPUT_CONTEXT:
        return {
            "name": INPUT_CONTEXT,
            "type": INPUT_CONTEXT,
            "path": INPUT_CONTEXT,
        }
    elif isinstance(v, HdfsSource):
        ret = {
            "name": v.name,
            "type": "hdfs",
            "path": v.path,
        }
    elif isinstance(v, SnowflakeSource):
        ret = {
            "name": v.name,
            "type": "SNOWFLAKE",
            "path": v.path,
        }
    elif isinstance(v, JdbcSource):
        ret = {
            "name": v.name,
            "type": "jdbc",
            "url": v.url,
        }
        if hasattr(v, "dbtable") and v.dbtable:
            ret["dbtable"] = v.dbtable
        if hasattr(v, "query") and v.query:
            ret["query"] = v.query
        if hasattr(v, "auth") and v.auth:
            ret["auth"] = v.auth
    else:
        raise ValueError(f"Unsupported source type {v.__class__}")
    if hasattr(v, "preprocessing") and v.preprocessing:
        ret["preprocessing"] = inspect.getsource(v.preprocessing)
    if v.event_timestamp_column:
        ret["eventTimestampColumn"] = v.event_timestamp_column
        ret["event_timestamp_column"] = v.event_timestamp_column
    if v.timestamp_format:
        ret["timestampFormat"] = v.timestamp_format
        ret["timestamp_format"] = v.timestamp_format
    if v.registry_tags:
        ret["tags"] = v.registry_tags
    return ret
    
def anchor_to_def(v: FeatureAnchor) -> dict:
    # Note that after this method, attributes are Camel cased (eventTimestampColumn). 
    # If the old logic works with snake case (event_timestamp_column), make sure you handle them manually. 
    source_id = v.source._registry_id
    ret = {
        "name": v.name,
        "sourceId": str(source_id),
    }
    if v.registry_tags:
        ret["tags"] = v.registry_tags
    return ret

def transformation_to_def(v: Transformation) -> dict:
    if isinstance(v, ExpressionTransformation):
        return {
            "transformExpr": v.expr
        }
    elif isinstance(v, WindowAggTransformation):
        ret = {
            "defExpr": v.def_expr,
        }
        if v.agg_func:
            ret["aggFunc"] = v.agg_func
        if v.window:
            ret["window"] = v.window
        if v.group_by:
            ret["groupBy"] = v.group_by
        if v.filter:
            ret["filter"] = v.filter
        if v.limit:
            ret["limit"] = v.limit
        return ret
    raise ValueError("Unsupported Transformation type")

def feature_type_to_def(v: FeatureType) -> dict:
    # Note that after this method, attributes are Camel cased (eventTimestampColumn). 
    # If the old logic works with snake case (event_timestamp_column), make sure you handle them manually. 
    return {
        "type": v.type,
        "tensorCategory": v.tensor_category,
        "dimensionType": [value_type_to_str(t) for t in v.dimension_type],
        "valType": value_type_to_str(v.val_type),
    }

def typed_key_to_def(v: TypedKey) -> dict:
    ret = {
        "keyColumn": v.key_column,
        "keyColumnType": value_type_to_str(v.key_column_type)
    }
    if v.full_name:
        ret["fullName"] = v.full_name
    if v.description:
        ret["description"] = v.description
    if v.key_column_alias:
        ret["keyColumnAlias"] = v.key_column_alias
    return ret

def feature_to_def(v: Feature) -> dict:
    ret = {
        "name": v.name,
        "featureType": feature_type_to_def(v.feature_type),
        "key": [typed_key_to_def(k) for k in v.key],
    }
    if v.transform:
        ret["transformation"] = transformation_to_def(
            v.transform)
    if v.registry_tags:
        ret["tags"] = v.registry_tags
    return ret

def derived_feature_to_def(v: DerivedFeature) -> dict:
    # Note that after this method, attributes are Camel cased (eventTimestampColumn). 
    # If the old logic works with snake case (event_timestamp_column), make sure you handle them manually. 
    ret = {
        "name": v.name,
        "featureType": feature_type_to_def(v.feature_type),
        "key": [typed_key_to_def(k) for k in v.key],
        "inputAnchorFeatures": [str(f._registry_id) for f in v.input_features if not isinstance(f, DerivedFeature)],
        "inputDerivedFeatures": [str(f._registry_id) for f in v.input_features if isinstance(f, DerivedFeature)],
    }
    if v.transform:
        ret["transformation"] = transformation_to_def(v.transform)
    return ret

def topological_sort(derived_feature_list: List[DerivedFeature]) -> List[DerivedFeature]:
    """
    In the current registry implementation, we need to make sure all upstream are registered before registering one derived feature
    Thus we need to sort derived features by the partial order of dependencies, upstream to downstream.
    """
    ret = []
    # We don't want to destroy the input list
    input = derived_feature_list.copy()
    
    # Each round add the most downstream features into `ret`, so `ret` is in reversed order
    while input:
        # Process all remaining features
        current = input.copy()
        
        # In Python you should not alter content while iterating
        current_copy = current.copy()
        
        # Go over all remaining features to see if some feature depends on others
        for f in current_copy:
            for i in f.input_features:
                if i in current:
                    # Someone depends on feature `i`, so `i` is **not** the most downstream
                    current.remove(i)
        
        # Now `current` contains only the most downstream features in this round
        ret.extend(current)
        
        # Remove one level of dependency from input
        for f in current:
            input.remove(f)
    
    # The ret was in a reversed order when it's generated
    ret.reverse()
    
    if len(set(ret)) != len (set(derived_feature_list)):
        raise ValueError("Cyclic dependency detected")
    return ret