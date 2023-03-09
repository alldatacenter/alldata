import importlib
import inspect
import json
import logging
import os
from pathlib import Path
import sys
from urllib.parse import urlparse
from uuid import UUID
from azure.identity import DefaultAzureCredential
from typing import Any, Dict, List, Optional, Tuple
from re import sub
from click import option
from jinja2 import Template

import requests
from feathr.constants import INPUT_CONTEXT, TYPEDEF_ANCHOR, TYPEDEF_ANCHOR_FEATURE, TYPEDEF_DERIVED_FEATURE, TYPEDEF_SOURCE
from feathr.definition.anchor import FeatureAnchor
from feathr.definition.dtype import FeatureType, str_to_value_type, value_type_to_str
from feathr.definition.feature import Feature, FeatureBase
from feathr.definition.feature_derivations import DerivedFeature
from feathr.definition.repo_definitions import RepoDefinitions
from feathr.definition.source import GenericSource, HdfsSource, InputContext, JdbcSource, SnowflakeSource, Source, SparkSqlSource
from feathr.definition.transformation import ExpressionTransformation, Transformation, WindowAggTransformation
from feathr.definition.typed_key import TypedKey
from feathr.registry.feature_registry import FeathrRegistry
from feathr.utils._file_utils import write_to_file

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


def _topological_sort(derived_feature_list: List[DerivedFeature]) -> List[DerivedFeature]:
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

    if len(set(ret)) != len(set(derived_feature_list)):
        raise ValueError("Cyclic dependency detected")
    return ret


class _FeatureRegistry(FeathrRegistry):
    def __init__(self, project_name: str, endpoint: str, project_tags: Dict[str, str] = None, credential=None, config_path=None):
        self.project_name = project_name
        self.project_tags = project_tags
        self.endpoint = endpoint
        # TODO: expand to more credential provider
        # If FEATHR_SANDBOX is set in the environment variable, don't do auth
        self.credential = DefaultAzureCredential(
            exclude_interactive_browser_credential=False) if credential is None and not os.environ.get("FEATHR_SANDBOX") else credential
        self.project_id = None

    def register_features(self, workspace_path: Optional[Path] = None, from_context: bool = True, anchor_list: List[FeatureAnchor]=[], derived_feature_list=[]):
        """Register Features for the specified workspace. 
        Args:
            workspace_path (str, optional): path to a workspace. Defaults to None, not used in this implementation.
            from_context: whether the feature is from context (i.e. end users has to callFeathrClient.build_features()) or the feature is from a pre-built config file. Currently Feathr only supports register features from context.
            anchor_list: The anchor list after feature build
            derived_feature_list: the derived feature list after feature build
        """
        if not from_context:
            raise RuntimeError(
                "Currently Feathr only supports registering features from context (i.e. you must call FeathrClient.build_features() before calling this function).")

        # Before starting, create the project
        self.project_id = self._create_project()

        for anchor in anchor_list:
            source = anchor.source
            # 1. Create Source on the registry
            # We always re-create INPUT_CONTEXT as lots of existing codes reuse the singleton in different projects
            if (source.name == INPUT_CONTEXT) or (not hasattr(source, "_registry_id")):
                source._registry_id = self._create_source(source)
            # 2. Create Anchor on the registry
            if not hasattr(anchor, "_registry_id"):
                anchor._registry_id = self._create_anchor(anchor)
            # 3. Create all features on the registry
            for feature in anchor.features:
                if not hasattr(feature, "_registry_id"):
                    feature._registry_id = self._create_anchor_feature(
                        anchor._registry_id, feature)
        # 4. Create all derived features on the registry
        for df in _topological_sort(derived_feature_list):
            if not hasattr(df, "_registry_id"):
                df._registry_id = self._create_derived_feature(df)
        url = '/'.join(self.endpoint.split('/')[:3])       
        logging.info(f"Check project lineage by this link: {url}/projects/{self.project_name}/lineage")

    def list_registered_features(self, project_name: str) -> List[str]:
        """List all the already registered features. If project_name is not provided or is None, it will return all
        the registered features; otherwise it will only return features under this project
        """
        resp = self._get(f"/projects/{project_name}/features")
        # In V1 API resp should be an array, will be changed in V2 API
        return [{
            "name": r["attributes"]["name"],
            "id": r["guid"],
            "qualifiedName": r["attributes"]["qualifiedName"],
        } for r in resp]
    
    def list_dependent_entities(self, qualified_name: str):
        """
        Returns list of dependent entities for provided entity
        """
        resp = self._get(f"/dependent/{qualified_name}")
        return [{
            "name": r["attributes"]["name"],
            "id": r["guid"],
            "qualifiedName": r["attributes"]["qualifiedName"],
        } for r in resp]
    
    def delete_entity(self, qualified_name: str):
        """
        Deletes entity if it has no dependent entities
        """
        self._delete(f"/entity/{qualified_name}")

    def get_features_from_registry(self, project_name: str) -> Tuple[List[FeatureAnchor], List[DerivedFeature]]:
        """
        [Sync Features from registry to local workspace, given a project_name, will write project's features from registry to to user's local workspace]

        Args:
            project_name (str): project name.
        """
        lineage = self._get(f"/projects/{project_name}")
        return dict_to_project(lineage)

    def _create_project(self) -> UUID:
        r = self._post(f"/projects", {"name": self.project_name})
        self.project_id = UUID(r["guid"])
        return self.project_id

    def _create_source(self, s: Source) -> UUID:
        r = self._post(
            f"/projects/{self.project_id}/datasources", source_to_def(s))
        id = UUID(r["guid"])
        s._registry_id = id
        s._qualified_name = f"{self.project_name}__{s.name}"
        return id

    def _create_anchor(self, s: FeatureAnchor) -> UUID:
        r = self._post(
            f"/projects/{self.project_id}/anchors", anchor_to_def(s))
        id = UUID(r["guid"])
        s._registry_id = id
        s._qualified_name = f"{self.project_name}__{s.name}"
        return id

    def _create_anchor_feature(self, anchor_name: str, s: Feature) -> UUID:
        r = self._post(
            f"/projects/{self.project_id}/anchors/{anchor_name}/features", feature_to_def(s))
        id = UUID(r["guid"])
        s._registry_id = id
        s._qualified_name = f"{self.project_name}__{anchor_name}__{s.name}"
        return id

    def _create_derived_feature(self, s: DerivedFeature) -> UUID:
        r = self._post(
            f"/projects/{self.project_id}/derivedfeatures", derived_feature_to_def(s))
        id = UUID(r["guid"])
        s._registry_id = id
        s._qualified_name = f"{self.project_name}__{s.name}"
        return id

    def _get(self, path: str) -> dict:
        logging.debug("PATH: ", path)
        return check(requests.get(f"{self.endpoint}{path}", headers=self._get_auth_header())).json()
    
    def _delete(self, path: str) -> dict:
        logging.debug("PATH: ", path)
        return check(requests.delete(f"{self.endpoint}{path}", headers=self._get_auth_header())).json()

    def _post(self, path: str, body: dict) -> dict:
        logging.debug("PATH: ", path)
        logging.debug("BODY: ", json.dumps(body, indent=2))
        return check(requests.post(f"{self.endpoint}{path}", headers=self._get_auth_header(), json=body)).json()

    def _get_auth_header(self) -> dict:
        # if the environment is sandbox, don't do auth
        # TODO: expand to more credential providers
        return {"Authorization": f'Bearer {self.credential.get_token("https://management.azure.com/.default").token}'} if not os.environ.get("FEATHR_SANDBOX") else None
    

def check(r):
    if not r.ok:
        raise RuntimeError(
            f"Failed to call registry API, status is {r.status_code}, error is {r.text}")
    return r


def source_to_def(v: Source) -> dict:
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
            "type": urlparse(v.path).scheme,
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
    elif isinstance(v, GenericSource):
        ret = v.to_dict()
        ret["name"] = v.name
    elif isinstance(v, SparkSqlSource):
        ret = v.to_dict()
        ret["name"] = v.name
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


def dict_to_source(v: dict) -> Source:
    id = UUID(v["guid"])
    type = v["attributes"]["type"]
    source = None
    if type == INPUT_CONTEXT:
        source = InputContext()
    elif type == "sparksql":
        source = SparkSqlSource(name=v["attributes"]["name"],
                            sql=v["attributes"].get("sql"),
                            table=v["attributes"].get("table"),
                            preprocessing=_correct_function_indentation(
                                v["attributes"].get("preprocessing")),
                            event_timestamp_column=v["attributes"].get(
                                "eventTimestampColumn"),
                            timestamp_format=v["attributes"].get(
                                "timestampFormat"),
                            registry_tags=v["attributes"].get("tags", {}))
    elif type == "jdbc":
        source = JdbcSource(name=v["attributes"]["name"],
                            url=v["attributes"].get("url"),
                            dbtable=v["attributes"].get("dbtable"),
                            query=v["attributes"].get("query"),
                            auth=v["attributes"].get("auth"),
                            preprocessing=_correct_function_indentation(
                                v["attributes"].get("preprocessing")),
                            event_timestamp_column=v["attributes"].get(
                                "eventTimestampColumn"),
                            timestamp_format=v["attributes"].get(
                                "timestampFormat"),
                            registry_tags=v["attributes"].get("tags", {}))
    elif type == "SNOWFLAKE":
        snowflake_path = v["attributes"]["path"]
        snowflake_parameters = SnowflakeSource.parse_snowflake_path(snowflake_path)
        source = SnowflakeSource(name=v["attributes"]["name"],
                                dbtable=snowflake_parameters.get("dbtable", None),
                                query=snowflake_parameters.get("query", None),
                                database=snowflake_parameters["sfDatabase"],
                                schema=snowflake_parameters["sfSchema"],
                                preprocessing=_correct_function_indentation(
                                    v["attributes"].get("preprocessing")),
                                event_timestamp_column=v["attributes"].get(
                                    "eventTimestampColumn"),
                                timestamp_format=v["attributes"].get(
                                    "timestampFormat"),
                                registry_tags=v["attributes"].get("tags", {}))
    elif type == "generic":
        options = v["attributes"].copy()
        # These are not options
        del options["type"]
        del options["name"]
        del options["format"]
        if "qualifiedName" in options:
            del options["qualifiedName"]
        if "mode" in options:
            del options["mode"]
        if "preprocessing" in options:
            del options["preprocessing"]
        if "eventTimestampColumn" in options:
            del options["eventTimestampColumn"]
        if "timestampFormat" in options:
            del options["timestampFormat"]
        if "tags" in options:
            del options["tags"]
        source = GenericSource(
            name=v["attributes"]["name"],
            format=v["attributes"]["format"],
            mode=v["attributes"].get("mode", ""),
            options=options,
            preprocessing=_correct_function_indentation(
                v["attributes"].get("preprocessing")),
            event_timestamp_column=v["attributes"].get(
                "eventTimestampColumn"),
            timestamp_format=v["attributes"].get(
                "timestampFormat"),
            registry_tags=v["attributes"].get("tags", {}))
    elif v["attributes"].get("path"):
        source = HdfsSource(name=v["attributes"]["name"],
                            path=v["attributes"]["path"],
                            preprocessing=_correct_function_indentation(
                                v["attributes"].get("preprocessing")),
                            event_timestamp_column=v["attributes"].get(
                                "eventTimestampColumn"),
                            timestamp_format=v["attributes"].get(
                                "timestampFormat"),
                            registry_tags=v["attributes"].get("tags", {}))
    else:
        raise ValueError(f"Invalid source format {type}")
    source._registry_id = id
    source._qualified_name = v["attributes"]["qualifiedName"]
    return source


def anchor_to_def(v: FeatureAnchor) -> dict:
    source_id = v.source._registry_id
    ret = {
        "name": v.name,
        "sourceId": str(source_id),
    }
    if v.registry_tags:
        ret["tags"] = v.registry_tags
    return ret


def dict_to_anchor(v: dict) -> FeatureAnchor:
    ret = FeatureAnchor(name=v["attributes"]["name"],
                        source=None,
                        features=[],
                        registry_tags=v["attributes"].get("tags", {}),
                        __no_validate=True)
    ret._source_id = UUID(v["attributes"]["source"]["guid"])
    ret._features = [UUID(f["guid"]) for f in v["attributes"]["features"]]
    ret._qualified_name = v["attributes"]["qualifiedName"]
    ret._registry_id = UUID(v["guid"])
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


def dict_to_transformation(v: dict) -> Transformation:
    if v is None:
        return None
    v = to_camel(v)
    if 'transformExpr' in v:
        # it's ExpressionTransformation
        return ExpressionTransformation(v['transformExpr'])
    elif 'defExpr' in v:
        return WindowAggTransformation(agg_expr=v['defExpr'],
                                       agg_func=v.get('aggFunc'),
                                       window=v.get('window'),
                                       group_by=v.get('groupBy'),
                                       filter=v.get('filter'),
                                       limit=v.get('limit'))
    raise ValueError(f"Invalid transformation format {v}")


def feature_type_to_def(v: FeatureType) -> dict:
    return {
        "type": v.type,
        "tensorCategory": v.tensor_category,
        "dimensionType": [value_type_to_str(t) for t in v.dimension_type],
        "valType": value_type_to_str(v.val_type),
    }


def dict_to_feature_type(v: dict) -> FeatureType:
    return FeatureType(val_type=str_to_value_type(v["valType"]),
                       dimension_type=[str_to_value_type(
                           s) for s in v["dimensionType"]],
                       tensor_category=v["tensorCategory"],
                       type=v["type"])


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


def dict_to_typed_key(v: dict) -> TypedKey:
    v = to_camel(v)
    return TypedKey(key_column=v["keyColumn"],
                    key_column_type=str_to_value_type(v["keyColumnType"]),
                    full_name=v.get("fullName"),
                    description=v.get("description"),
                    key_column_alias=v.get("keyColumnAlias"))


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


def dict_to_feature(v: dict) -> Feature:
    ret = Feature(name=v["attributes"]["name"],
                  feature_type=dict_to_feature_type(v["attributes"]["type"]),
                  key=[dict_to_typed_key(k) for k in v["attributes"]["key"]],
                  transform=dict_to_transformation(
                      v["attributes"].get("transformation")),
                  registry_tags=v["attributes"].get("tags", {}))
    ret._qualified_name = v["attributes"]["qualifiedName"]
    ret._registry_id = UUID(v["guid"])
    return ret


def _get_type_name(v: Any) -> str:
    if isinstance(v, Source):
        return TYPEDEF_SOURCE
    elif isinstance(v, FeatureAnchor):
        return TYPEDEF_ANCHOR
    elif isinstance(v, DerivedFeature):
        return TYPEDEF_DERIVED_FEATURE
    elif isinstance(v, Feature):
        return TYPEDEF_ANCHOR_FEATURE
    raise TypeError("Invalid type")


def derived_feature_to_def(v: DerivedFeature) -> dict:
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


def dict_to_derived_feature(v: dict) -> DerivedFeature:
    v["attributes"] = to_camel(v["attributes"])
    ret = DerivedFeature(name=v["attributes"]["name"],
                         feature_type=dict_to_feature_type(
                             v["attributes"]["type"]),
                         input_features=[],
                         key=[dict_to_typed_key(k)
                              for k in v["attributes"]["key"]],
                         transform=dict_to_transformation(
                             v["attributes"]["transformation"]),
                         registry_tags=v["attributes"].get("tags", {}),
                         __no_validate=True)
    ret._input_anchor_features = [
        UUID(f["guid"]) for f in v["attributes"]["inputAnchorFeatures"]]
    ret._input_derived_features = [
        UUID(f["guid"]) for f in v["attributes"]["inputDerivedFeatures"]]
    ret._qualified_name = v["attributes"]["qualifiedName"]
    ret._registry_id = UUID(v["guid"])
    return ret


class Relation:
    def __init__(self, v: dict):
        self.f = UUID(v["fromEntityId"])
        self.t = UUID(v["toEntityId"])
        self.type = v["relationshipType"]


def dict_to_project(v: dict) -> Tuple[List[FeatureAnchor], List[DerivedFeature]]:
    entities = v["guidEntityMap"]
    # Step 1, Extract each entity
    sources = dict([(UUID(k), dict_to_source(entities[k]))
                   for k in entities if entities[k]["typeName"] == "feathr_source_v1"])
    anchors = dict([(UUID(k), dict_to_anchor(entities[k]))
                   for k in entities if entities[k]["typeName"] == "feathr_anchor_v1"])
    features = dict([(UUID(k), dict_to_feature(entities[k]))
                    for k in entities if entities[k]["typeName"] == "feathr_anchor_feature_v1"])
    derived_features = dict([(UUID(k), dict_to_derived_feature(entities[k]))
                            for k in entities if entities[k]["typeName"] == "feathr_derived_feature_v1"])
    # Step 2, Setup connections between extracted entities
    # Step 2-1, Set up anchors
    for k in anchors:
        anchor = anchors[k]
        anchor.source = sources[anchor._source_id]
        anchor.features = [features[id] for id in anchor._features]
    # Step 2-1, Set up derived features
    for k in derived_features:
        df = derived_features[k]
        input_anchor_features = [features[id]
                                 for id in df._input_anchor_features]
        input_derived_features = [derived_features[id]
                                  for id in df._input_derived_features]
        df.input_features = input_anchor_features + input_derived_features
    return (list(anchors.values()), list(derived_features.values()))


def _correct_function_indentation(user_func: str) -> str:
    """
    The function read from registry might have the wrong indentation. We need to correct those indentations.
    More specifically, we are using the inspect module to copy the function body for UDF for further submission. In that case, there will be situations like this:

    def feathr_udf1(df)
        return df

            def feathr_udf2(df)
                return df

    For example, in Feathr test cases, there are similar patterns for `feathr_udf2`, since it's defined in another function body (the registry_test_setup method).
    This is not an ideal way of dealing with that, but we'll keep it here until we figure out a better way.
    """
    if user_func is None:
        return None
    # if user_func is a string, turn it into a list of strings so that it can be used below
    temp_udf_source_code = user_func.split('\n')
    # assuming the first line is the function name
    leading_space_num = len(
        temp_udf_source_code[0]) - len(temp_udf_source_code[0].lstrip())
    # strip the lines to make sure the function has the correct indentation
    udf_source_code_striped = [line[leading_space_num:]
                               for line in temp_udf_source_code]
    # append '\n' back since it was deleted due to the previous split
    udf_source_code = [line+'\n' for line in udf_source_code_striped]
    return " ".join(udf_source_code)
