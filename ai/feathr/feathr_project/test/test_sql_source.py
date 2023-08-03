import os
from datetime import datetime
from pathlib import Path

import pytest

from feathr import BOOLEAN, FLOAT, INT32
from feathr import FeathrClient
from feathr import FeatureQuery
from feathr import JdbcSource
from feathr import ObservationSettings
from feathr import TypedKey
from feathr import ValueType, Feature, FeatureAnchor, DerivedFeature, WindowAggTransformation, INPUT_CONTEXT
from feathr.utils.job_utils import get_result_df
from test_fixture import get_online_test_table_name
from test_utils.constants import Constants

def basic_test_setup(config_path: str):
    """
    Basically this is same as the one in `text_fixture.py` with the same name.
    The difference is the `batch_source` is configured to read from JDBC instead of HDFS
    """

    client = FeathrClient(config_path=config_path)

    # Using database under @windoze account, so this e2e test still doesn't work in CI
    batch_source = JdbcSource(name="nycTaxiBatchJdbcSource",
                              url="jdbc:sqlserver://feathrtestsql4.database.windows.net:1433;database=testsql;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;",
                              dbtable="green_tripdata_2020_04",
                              auth="USERPASS",
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
    
    # This feature is read from Jdbc data source
    agg_features = [Feature(name="f_location_avg_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="AVG",
                                                              window="90d")),
                    ]

    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                               source=batch_source,
                               features=agg_features)

    client.build_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=[
        f_trip_time_distance, f_trip_time_rounded])

    return client

@pytest.mark.skip(reason="Requires database with test data imported, which doesn't exist in the current CI env")
def test_feathr_get_offline_features():
    """
    Test FeathrClient() get_offline_features works with Jdbc data source.
    Currently it doesn't work in CI env hence being marked as `skip`,
    because it requires a database with `green_tripdata_2020-04.csv` imported into a table named `green_tripdata_2020_04`.
    To run this test, you'll also need following environment variables set correctly:
    - nycTaxiBatchJdbcSource_USER: The user name to login to database server
    - nycTaxiBatchJdbcSource_PASSWORD: The password to login to database server
    These 2 variables will be passed to the Spark job in `--system-properties` parameter so Spark can access the database
    """

    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    # os.chdir(test_workspace_dir)

    client = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    location_id = TypedKey(key_column="DOLocationID",
                            key_column_type=ValueType.INT32,
                            description="location id in NYC",
                            full_name="nyc_taxi.location_id")

    feature_query = FeatureQuery(
        feature_list=["f_location_avg_fare"], key=location_id)
    settings = ObservationSettings(
        observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss")

    now = datetime.now()
    output_path = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), ".avro"])
    
    client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    # download result and just assert the returned result is not empty
    res_df = get_result_df(client)
    assert res_df.shape[0] > 0

    
if __name__ == "__main__":
    test_feathr_get_offline_features()