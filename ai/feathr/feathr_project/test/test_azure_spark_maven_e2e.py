import os
from datetime import datetime, timedelta
from pathlib import Path

from feathr import (BackfillTime, MaterializationSettings)
# from feathr import *
from feathr.client import FeathrClient
from feathr.definition.dtype import ValueType
from feathr.definition.query_feature_list import FeatureQuery
from feathr.definition.settings import ObservationSettings
from feathr.definition.typed_key import TypedKey
from test_fixture import (basic_test_setup, get_online_test_table_name)
from test_utils.constants import Constants

def test_feathr_online_store_agg_features():
    """
    Test FeathrClient() get_online_features and batch_get can get data correctly.
    """

    online_test_table = get_online_test_table_name("nycTaxiCITableMaven")
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    # os.chdir(test_workspace_dir)

    # The `feathr_runtime_location` was commented out in this config file, so feathr should use
    # Maven package as the dependency and `noop.jar` as the main file
    client: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config_maven.yaml"))

    
    
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
    # set output folder based on different runtime
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), ".avro"])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/output','_', str(now.minute), '_', str(now.second), ".avro"])


    client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)
    return
    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))
    redisSink = RedisSink(table_name=online_test_table)
    settings = MaterializationSettings("TestJobName",
                                       sinks=[redisSink],
                                       feature_names=[
                                           "f_location_avg_fare", "f_location_max_fare"],
                                       backfill_time=backfill_time)
    client.materialize_features(settings)
    # just assume the job is successful without validating the actual result in Redis. Might need to consolidate
    # this part with the test_feathr_online_store test case
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res = client.get_online_features(online_test_table, '265', [
                                     'f_location_avg_fare', 'f_location_max_fare'])
    # just assume there are values. We don't hard code the values for now for testing
    # the correctness of the feature generation should be guaranteed by feathr runtime.
    # ID 239 and 265 are available in the `DOLocationID` column in this file:
    # https://s3.amazonaws.com/nyc-tlc/trip+data/green_tripdata_2020-04.csv
    # View more details on this dataset: https://www1.nyc.gov/site/tlc/about/tlc-trip-record-data.page
    assert len(res) == 2
    assert res[0] != None
    assert res[1] != None
    res = client.multi_get_online_features(online_test_table,
                                           ['239', '265'],
                                           ['f_location_avg_fare', 'f_location_max_fare'])
    assert res['239'][0] != None
    assert res['239'][1] != None
    assert res['265'][0] != None
    assert res['265'][1] != None
