import os
from datetime import datetime, timedelta
from pathlib import Path

from feathr import (BackfillTime, MaterializationSettings)
from feathr import FeatureQuery
from feathr import ObservationSettings
from feathr import RedisSink
from feathr import TypedKey
from feathr import ValueType
from feathr.utils.job_utils import get_result_df
from test_fixture import (snowflake_test_setup, get_online_test_table_name)
from test_utils.constants import Constants


def test_feathr_online_store_agg_features():
    """
    Test FeathrClient() get_online_features and batch_get can get feature data correctly.
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client = snowflake_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    online_test_table = get_online_test_table_name("snowflakeSampleDemoFeature")

    backfill_time = BackfillTime(start=datetime(2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))
    redisSink = RedisSink(table_name= online_test_table)
    settings = MaterializationSettings(name="snowflakeSampleDemoFeature",
                                   sinks=[redisSink],
                                   feature_names=['f_snowflake_call_center_division_name',
                                                  'f_snowflake_call_center_zipcode'],
                                   backfill_time=backfill_time)
    client.materialize_features(settings, allow_materialize_non_agg_feature=True)
    # just assume the job is successful without validating the actual result in Redis. Might need to consolidate
    # this part with the test_feathr_online_store test case
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res = client.get_online_features(online_test_table, '1',
                                     ['f_snowflake_call_center_division_name', 'f_snowflake_call_center_zipcode'])

    assert len(res) == 2
    assert res[0] != None
    assert res[1] != None
    res = client.multi_get_online_features(online_test_table,
                                    ['1', '2'],
                                    ['f_snowflake_call_center_division_name', 'f_snowflake_call_center_zipcode'])
    assert res['1'][0] != None
    assert res['1'][1] != None
    assert res['2'][0] != None
    assert res['2'][1] != None

    client._clean_test_data(online_test_table)

def test_feathr_get_offline_features():
    """
    Test get_offline_features() can get feature data from Snowflake source correctly.
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"


    client = snowflake_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    call_sk_id = TypedKey(key_column="CC_CALL_CENTER_SK",
                          key_column_type=ValueType.INT32,
                          description="call center sk",
                          full_name="snowflake.CC_CALL_CENTER_SK")

    feature_query = FeatureQuery(
        feature_list=['f_snowflake_call_center_division_name', 'f_snowflake_call_center_zipcode'],
        key=call_sk_id)

    observation_path = client.get_snowflake_path(database="SNOWFLAKE_SAMPLE_DATA",schema="TPCDS_SF10TCL",dbtable="CALL_CENTER")
    settings = ObservationSettings(
        observation_path=observation_path)

    now = datetime.now()
     # set output folder based on different runtime
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob_snowflake','_', str(now.minute), '_', str(now.second), ".avro"])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/snowflake_output','_', str(now.minute), '_', str(now.second), ".avro"])

    client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res = get_result_df(client)
    # just assume there are results.
    assert res.shape[0] > 1

def test_client_get_snowflake_observation_path():
    """
    Test get_snowflake_path() returns correct snowflake observation path
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"


    client = snowflake_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    snowflake_path_actual = client.get_snowflake_path(database="DATABASE", schema="SCHEMA", dbtable="TABLE")
    snowflake_path_expected = "snowflake://snowflake_account/?sfDatabase=DATABASE&sfSchema=SCHEMA&dbtable=TABLE"
    assert snowflake_path_actual == snowflake_path_expected
