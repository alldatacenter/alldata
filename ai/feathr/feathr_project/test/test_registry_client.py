import json
import os
import datetime
import time
from uuid import UUID
from feathr.definition.anchor import FeatureAnchor
from feathr.definition.dtype import BOOLEAN, FLOAT, INT32, BooleanFeatureType, ValueType
from feathr.definition.feature import Feature
from feathr.definition.feature_derivations import DerivedFeature
from feathr.definition.source import INPUT_CONTEXT, HdfsSource, InputContext
from feathr.definition.transformation import ExpressionTransformation, WindowAggTransformation
from feathr.definition.typed_key import TypedKey

from feathr.registry._feathr_registry_client import _FeatureRegistry, dict_to_source, dict_to_anchor, dict_to_feature, dict_to_derived_feature, dict_to_project


def test_parse_source():
    s = r'''{
        "attributes": {
            "name": "nycTaxiBatchSource",
            "path": "wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
            "preprocessing": "    def add_new_dropoff_and_fare_amount_column(df: DataFrame):\n        df = df.withColumn(\"new_lpep_dropoff_datetime\", col(\"lpep_dropoff_datetime\"))\n        df = df.withColumn(\"new_fare_amount\", col(\"fare_amount\") + 1000000)\n        return df\n",
            "qualifiedName": "feathr_getting_started__nycTaxiBatchSource",
            "tags": {
                "for_test_purpose": "true"
            },
            "type": "wasbs"
        },
        "displayText": "nycTaxiBatchSource",
        "guid": "c4a0ae0f-09cc-43bf-94e9-21ff178fbda6",
        "labels": [],
        "lastModifiedTS": "1",
        "name": "nycTaxiBatchSource",
        "qualifiedName": "feathr_getting_started__nycTaxiBatchSource",
        "status": "Active",
        "typeName": "feathr_source_v1"
    }'''
    source = dict_to_source(json.loads(s))
    assert isinstance(source, HdfsSource)
    assert source.name == "nycTaxiBatchSource"
    assert source.path == "wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv"
    assert source._registry_id == UUID("c4a0ae0f-09cc-43bf-94e9-21ff178fbda6")
    assert source._qualified_name == "feathr_getting_started__nycTaxiBatchSource"


def test_parse_anchor():
    s = r'''{
        "attributes": {
            "features": [
                {
                    "guid": "2380fe5b-ce2a-401e-98bf-af8b98460f67",
                    "typeName": "feathr_anchor_feature_v1",
                    "uniqueAttributes": {
                        "qualifiedName": "feathr_getting_started__request_features__f_day_of_week"
                    }
                },
                {
                    "guid": "103baca1-377a-4ddf-8429-5da91026c269",
                    "typeName": "feathr_anchor_feature_v1",
                    "uniqueAttributes": {
                        "qualifiedName": "feathr_getting_started__request_features__f_trip_time_duration"
                    }
                }
            ],
            "name": "request_features",
            "qualifiedName": "feathr_getting_started__request_features",
            "source": {
                "guid": "a4cfbc03-c65d-4f32-be3d-1d11247c9cdd",
                "typeName": "feathr_source_v1",
                "uniqueAttributes": {
                    "qualifiedName": "feathr_getting_started__PASSTHROUGH"
                }
            },
            "tags": {
                "for_test_purpose": "true"
            }
        },
        "displayText": "request_features",
        "guid": "260325a5-27f9-40d1-8697-c727feb1dbdc",
        "labels": [],
        "lastModifiedTS": "1",
        "name": "request_features",
        "qualifiedName": "feathr_getting_started__request_features",
        "status": "Active",
        "typeName": "feathr_anchor_v1"
    }'''
    anchor = dict_to_anchor(json.loads(s))
    # Parsed anchor is empty, features and source are added later
    assert anchor.name == "request_features"
    assert anchor._registry_id == UUID("260325a5-27f9-40d1-8697-c727feb1dbdc")
    assert anchor._qualified_name == "feathr_getting_started__request_features"


def test_parse_feature():
    s = r'''{
        "attributes": {
            "key": [
                {
                    "description": "A dummy typed key for passthrough/request feature.",
                    "fullName": "feathr.dummy_typedkey",
                    "keyColumn": "NOT_NEEDED",
                    "keyColumnAlias": "NOT_NEEDED",
                    "keyColumnType": "UNSPECIFIED"
                }
            ],
            "name": "f_is_long_trip_distance",
            "qualifiedName": "feathr_getting_started__request_features__f_is_long_trip_distance",
            "tags": {},
            "transformation": {
                "transformExpr": "cast_float(trip_distance)>30"
            },
            "type": {
                "dimensionType": [],
                "tensorCategory": "DENSE",
                "type": "TENSOR",
                "valType": "BOOLEAN"
            }
        },
        "displayText": "f_is_long_trip_distance",
        "guid": "dc24b1d5-206d-40db-b10a-606dd16a0297",
        "labels": [],
        "lastModifiedTS": "1",
        "name": "f_is_long_trip_distance",
        "qualifiedName": "feathr_getting_started__request_features__f_is_long_trip_distance",
        "status": "Active",
        "typeName": "feathr_anchor_feature_v1"
    }'''
    f: Feature = dict_to_feature(json.loads(s))
    assert f.feature_type == BOOLEAN
    assert f.name == "f_is_long_trip_distance"
    assert len(f.key) == 1
    assert f.key[0].key_column_type == ValueType.UNSPECIFIED
    assert f.key[0].key_column == "NOT_NEEDED"
    assert isinstance(f.transform, ExpressionTransformation)
    assert f.transform.expr == "cast_float(trip_distance)>30"
    # Hidden properties
    assert f._qualified_name == "feathr_getting_started__request_features__f_is_long_trip_distance"
    assert f._registry_id == UUID("dc24b1d5-206d-40db-b10a-606dd16a0297")


def test_parse_derived_feature():
    s = r'''{
        "attributes": {
            "inputAnchorFeatures": [],
            "inputDerivedFeatures": [
                {
                    "guid": "c626c41c-d6c2-4b16-a267-6cdeea497c52",
                    "typeName": "feathr_anchor_feature_v1",
                    "uniqueAttributes": {
                        "qualifiedName": "feathr_getting_started__f_trip_time_rounded"
                    }
                }
            ],
            "key": [
                {
                    "description": "A dummy typed key for passthrough/request feature.",
                    "fullName": "feathr.dummy_typedkey",
                    "keyColumn": "NOT_NEEDED",
                    "keyColumnAlias": "NOT_NEEDED",
                    "keyColumnType": "UNSPECIFIED"
                }
            ],
            "name": "f_trip_time_rounded_plus",
            "qualifiedName": "feathr_getting_started__f_trip_time_rounded_plus",
            "tags": {},
            "transformation": {
                "transformExpr": "f_trip_time_rounded + 100"
            },
            "type": {
                "dimensionType": [],
                "tensorCategory": "DENSE",
                "type": "TENSOR",
                "valType": "INT"
            }
        },
        "displayText": "f_trip_time_rounded_plus",
        "guid": "479c6306-5fdb-4e06-9008-c18f68db52a4",
        "labels": [],
        "lastModifiedTS": "1",
        "name": "f_trip_time_rounded_plus",
        "qualifiedName": "feathr_getting_started__f_trip_time_rounded_plus",
        "status": "Active",
        "typeName": "feathr_derived_feature_v1"
    }'''
    df = dict_to_derived_feature(json.loads(s))
    assert df.name == "f_trip_time_rounded_plus"
    assert df.feature_type == INT32
    assert len(df.key) == 1
    assert df.key[0].key_column_type == ValueType.UNSPECIFIED
    # Hidden properties
    assert df._qualified_name == "feathr_getting_started__f_trip_time_rounded_plus"
    assert df._registry_id == UUID("479c6306-5fdb-4e06-9008-c18f68db52a4")

def test_parse_project():
    filename = os.path.join(os.path.dirname(__file__), "test_registry_lineage.json")
    f=open(filename, "r")
    (anchors, derived_features) = dict_to_project(json.load(f))
    assert len(anchors)==2
    request_features = [a for a in anchors if a.name=='request_features'][0]
    assert isinstance(request_features.source, InputContext)
    assert len(request_features.features)==4
    aggregationFeatures = [a for a in anchors if a.name=='aggregationFeatures'][0]
    assert len(aggregationFeatures.features)==2
    assert isinstance(aggregationFeatures.source, HdfsSource)
    assert aggregationFeatures.source.name=="nycTaxiBatchSource"
    assert aggregationFeatures.source.path=="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv"
    assert len(derived_features)==3

def test_registry_client_list_features():
    c = _FeatureRegistry(project_name="p", endpoint="https://feathr-sql-registry.azurewebsites.net/api/v1")
    f = [e["qualifiedName"] for e in c.list_registered_features("feathr_ci_registry_getting_started")]
    print(f)
    assert len(f)==9
    for i in f:
        assert i.startswith("feathr_ci_registry_getting_started__")
        
def test_registry_client_load():
    c = _FeatureRegistry(project_name="p", endpoint="https://feathr-sql-registry.azurewebsites.net/api/v1")
    (anchors, derived_features) = c.get_features_from_registry("feathr_ci_registry_getting_started")
    assert len(anchors)==2
    request_features = [a for a in anchors if a.name=='request_features'][0]
    assert isinstance(request_features.source, InputContext)
    assert len(request_features.features)==4
    aggregationFeatures = [a for a in anchors if a.name=='aggregationFeatures'][0]
    assert len(aggregationFeatures.features)==3
    assert isinstance(aggregationFeatures.source, HdfsSource)
    assert aggregationFeatures.source.name=="nycTaxiBatchSource"
    assert aggregationFeatures.source.path=="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04_with_index.csv"
    assert len(derived_features)==2

def test_create():
    project_name = f"feathr_registry_client_test_{int(time.time())}"
    c = _FeatureRegistry(project_name="p", endpoint="https://feathr-sql-registry.azurewebsites.net/api/v1")
    
    batch_source = HdfsSource(name="nycTaxiBatchSource",
                            path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                            event_timestamp_column="lpep_dropoff_datetime",
                            timestamp_format="yyyy-MM-dd HH:mm:ss")

    f_trip_distance = Feature(name="f_trip_distance",
                              feature_type=FLOAT, transform="trip_distance")
    f_trip_time_duration = Feature(name="f_trip_time_duration",
                                   feature_type=INT32,
                                   transform="(to_unix_timestamp(lpep_dropoff_datetime) - to_unix_timestamp(lpep_pickup_datetime))/60")

    features = [
        f_trip_distance,
        f_trip_time_duration,
        Feature(name="f_is_long_trip_distance",
                feature_type=BOOLEAN,
                transform="cast_float(trip_distance)>30"),
        Feature(name="f_day_of_week",
                feature_type=INT32,
                transform="dayofweek(lpep_dropoff_datetime)"),
    ]


    request_anchor = FeatureAnchor(name="request_features",
                                   source=INPUT_CONTEXT,
                                   features=features)

    f_trip_time_distance = DerivedFeature(name="f_trip_time_distance",
                                          feature_type=FLOAT,
                                          input_features=[
                                              f_trip_distance, f_trip_time_duration],
                                          transform="f_trip_distance * f_trip_time_duration")

    f_trip_time_rounded = DerivedFeature(name="f_trip_time_rounded",
                                         feature_type=INT32,
                                         input_features=[f_trip_time_duration],
                                         transform="f_trip_time_duration % 10")

    location_id = TypedKey(key_column="DOLocationID",
                           key_column_type=ValueType.INT32,
                           description="location id in NYC",
                           full_name="nyc_taxi.location_id")
    agg_features = [Feature(name="f_location_avg_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="AVG",
                                                              window="90d")),
                    Feature(name="f_location_max_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="MAX",
                                                              window="90d"))
                    ]

    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                               source=batch_source,
                               features=agg_features)
    
    c.register_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=[f_trip_time_distance, f_trip_time_rounded])

    

if __name__ == "__main__":
    test_parse_source()
    test_parse_anchor()
    test_parse_feature()
    test_parse_derived_feature()
    test_parse_project()
    test_registry_client_list_features()
    test_registry_client_load()
    test_create()
