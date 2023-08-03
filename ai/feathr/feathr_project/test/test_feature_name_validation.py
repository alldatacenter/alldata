import string
import os
import pytest
from pathlib import Path

from feathr import FeatureBase
from feathr import (TypedKey, ValueType, FeatureQuery, ObservationSettings)
from feathr import FeathrClient

@pytest.mark.parametrize('bad_feature_name',
                         [None,
                          ''])
def test_feature_name_fails_on_empty_name(bad_feature_name: str):
    with pytest.raises(Exception, match="empty feature name"):
        FeatureBase.validate_feature_name(bad_feature_name)


def test_feature_name_fails_on_leading_number():
    with pytest.raises(Exception, match="cannot start with a number"):
        FeatureBase.validate_feature_name("4featurename")


def test_feature_name_fails_on_punctuation_chars():
    for char in set(string.punctuation) - set('_'):
        with pytest.raises(Exception,  match="only letters, numbers, and underscores are allowed"):
            FeatureBase.validate_feature_name(f"feature_{char}_name")


@pytest.mark.parametrize('feature_name',
                         ["feature_name",
                          "features4lyfe",
                          "f_4_feature_",
                          "_leading_underscores_are_ok",
                          "CapitalizedFeature"
                          ''])
def test_feature_name_validates_ok(feature_name: str):
    assert FeatureBase.validate_feature_name(feature_name)
    
def test_feature_name_conflicts_with_public_dataset_columns():
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    
    client = client = FeathrClient(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    
    location_id = TypedKey(key_column="DOLocationID",
                            key_column_type=ValueType.INT32,
                            description="location id in NYC",
                            full_name="nyc_taxi.location_id")
    
    feature_query = FeatureQuery(
        feature_list=["trip_distance","fare_amount"], key=location_id)
    settings = ObservationSettings(
        observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04_with_index.csv",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss")
    output_path = "wasbs://fake_path"
    with pytest.raises(RuntimeError) as e:
        client.get_offline_features(observation_settings=settings,
                feature_query=feature_query,
                output_path=output_path
        )
    assert str(e.value) == "Feature names exist conflicts with dataset column names: trip_distance,fare_amount"
        
    settings = ObservationSettings(
        observation_path="wasbs://public@fake_file",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss")
    output_path = "wasbs://fakepath"
    with pytest.raises(RuntimeError) as e:
        client.get_offline_features(observation_settings=settings,
                feature_query=feature_query,
                output_path=output_path,
                dataset_column_names=set(('trip_distance','fare_amount'))
        )
    assert str(e.value) == "Feature names exist conflicts with dataset column names: trip_distance,fare_amount"
    
def test_feature_name_conflicts_with_private_dataset_columns():
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    
    client = client = FeathrClient(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    
    if client.spark_runtime == 'databricks':
        source_path = 'dbfs:/timePartitionPattern_test/df0/daily/2020/05/01/'
    else:
        source_path = 'abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/timePartitionPattern_test/df0/daily/2020/05/01/'
      
    location_id = TypedKey(key_column="DOLocationID",
                            key_column_type=ValueType.INT32,
                            description="location id in NYC",
                            full_name="nyc_taxi.location_id")
    
    feature_query = FeatureQuery(
        feature_list=["f_location_avg_fare","f_location_max_fare"], key=location_id)
    
    settings = ObservationSettings(
        observation_path=source_path,
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss",
        file_format="avro",
        is_file_path=False)
    output_path = "wasbs://fake_path"
    with pytest.raises(RuntimeError) as e:
        client.get_offline_features(observation_settings=settings,
                feature_query=feature_query,
                output_path=output_path
        )
    assert str(e.value) == "Feature names exist conflicts with dataset column names: f_location_avg_fare,f_location_max_fare"
        
