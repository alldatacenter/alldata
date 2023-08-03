import os
from datetime import datetime, timedelta
from pathlib import Path
from feathr import (BOOLEAN, FLOAT, INPUT_CONTEXT, INT32, STRING,
                    DerivedFeature, Feature, FeatureAnchor, HdfsSource,
                    TypedKey, ValueType, WindowAggTransformation)
from feathr import FeathrClient
from feathr.definition.sink import CosmosDbSink, ElasticSearchSink
from feathr.definition.source import HdfsSource

import pytest
from click.testing import CliRunner

from feathr import (BackfillTime, MaterializationSettings)
from feathr import FeathrClient
from feathr import FeatureQuery
from feathr import ObservationSettings
from feathr import RedisSink, HdfsSink, JdbcSink,AerospikeSink
from feathr import TypedKey
from feathr import ValueType
from feathr.utils.job_utils import get_result_df
from feathrcli.cli import init
from test_fixture import (basic_test_setup, get_online_test_table_name, composite_keys_test_setup)
from test_utils.constants import Constants

# make sure you have run the upload feature script before running these tests
# the feature configs are from feathr_project/data/feathr_user_workspace
def test_feathr_materialize_to_offline():
    """
    Test FeathrClient() HdfsSink.
    """
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    # os.chdir(test_workspace_dir)

    client: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))

    now = datetime.now()
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    offline_sink = HdfsSink(output_path=output_path)
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[offline_sink],
                                       feature_names=[
                                           "f_location_avg_fare", "f_location_max_fare"],
                                       backfill_time=backfill_time)
    client.materialize_features(settings)
    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    # download result and just assert the returned result is not empty
    # by default, it will write to a folder appended with date
    res_df = get_result_df(client, data_format="avro", res_url=output_path + "/df0/daily/2020/05/20")
    assert res_df.shape[0] > 0

def test_feathr_online_store_agg_features():
    """
    Test FeathrClient() get_online_features and batch_get can get data correctly.
    """

    online_test_table = get_online_test_table_name("nycTaxiCITableSparkE2E")
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    # os.chdir(test_workspace_dir)

    client: FeathrClient = composite_keys_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))
    redisSink = RedisSink(table_name=online_test_table)
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[redisSink],
                                       feature_names=[
                                           "f_location_avg_fare", "f_location_max_fare"],
                                       backfill_time=backfill_time)
    client.materialize_features(settings)
    # just assume the job is successful without validating the actual result in Redis. Might need to consolidate
    # this part with the test_feathr_online_store test case
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res = client.get_online_features(online_test_table, ["81", "254"], [
                                     'f_location_avg_fare', 'f_location_max_fare'])
    # just assume there are values. We don't hard code the values for now for testing
    # the correctness of the feature generation should be guaranteed by feathr runtime.
    # ID 239 and 265 are available in the `DOLocationID` column in this file:
    # https://s3.amazonaws.com/nyc-tlc/trip+data/green_tripdata_2020-04.csv
    # View more details on this dataset: https://www1.nyc.gov/site/tlc/about/tlc-trip-record-data.page
    assert res != None
    res = client.multi_get_online_features(online_test_table,
                                           [["81","254"], ["25","42"]],
                                           ['f_location_avg_fare', 'f_location_max_fare'])
    assert res['81#254'] != None
    assert res['25#42'] != None
    
    client._clean_test_data(online_test_table)

@pytest.mark.skip(reason="Add back when complex types are supported in python API")
def test_feathr_online_store_non_agg_features():
    """
    Test FeathrClient() online_get_features and batch_get can get data correctly.
    """
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    client = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    online_test_table = get_online_test_table_name('nycTaxiCITableNonAggFeature')
    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))
    redisSink = RedisSink(table_name=online_test_table)
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[redisSink],
                                       feature_names=["f_gen_trip_distance", "f_gen_is_long_trip_distance", "f1", "f2", "f3", "f4", "f5", "f6"],
                                       backfill_time=backfill_time)

    client.materialize_features(settings, allow_materialize_non_agg_feature=True)
    # just assume the job is successful without validating the actual result in Redis. Might need to consolidate
    # this part with the test_feathr_online_store test case
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res = client.get_online_features(online_test_table, '111', ['f_gen_trip_distance', 'f_gen_is_long_trip_distance',
                                                                   'f1', 'f2', 'f3', 'f4', 'f5', 'f6'])
    # just assume there are values. We don't hard code the values for now for testing
    # the correctness of the feature generation should be guaranteed by feathr runtime.
    # ID 239 and 265 are available in the `DOLocationID` column in this file:
    # https://s3.amazonaws.com/nyc-tlc/trip+data/green_tripdata_2020-04.csv
    # View more detail on this dataset: https://www1.nyc.gov/site/tlc/about/tlc-trip-record-data.page

    assert len(res) == 8
    assert res[0] != None
    assert res[1] != None
    # assert constant features
    _validate_constant_feature(res)
    res = client.multi_get_online_features(online_test_table,
                                           ['239', '265'],
                                           ['f_gen_trip_distance', 'f_gen_is_long_trip_distance', 'f1', 'f2', 'f3', 'f4', 'f5', 'f6'])
    _validate_constant_feature(res['239'])
    assert res['239'][0] != None
    assert res['239'][1] != None
    _validate_constant_feature(res['265'])
    assert res['265'][0] != None
    assert res['265'][1] != None
    
    client._clean_test_data(online_test_table)


def _validate_constant_feature(feature):
    assert feature[2] == [10.0, 20.0, 30.0]
    assert feature[3] == ['a', 'b', 'c']
    assert feature[4] == ([1, 2, 3], ['10', '20', '30'])
    assert feature[5] == ([1, 2, 3], [True, False, True])
    assert feature[6] == ([1, 2, 3], [1.0, 2.0, 3.0])
    assert feature[7] == ([1, 2, 3], [1, 2, 3])


def test_dbfs_path():
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    client = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    if client.spark_runtime.casefold() == "databricks":
        # expect this raise an error since the result path is not in dbfs: format
        with pytest.raises(RuntimeError):
            client.feathr_spark_launcher.download_result(result_path="wasb://res_url", local_folder="/tmp")


def test_feathr_get_offline_features():
    """
    Test get_offline_features() can get data correctly.
    """
    runner = CliRunner()
    with runner.isolated_filesystem():
        runner.invoke(init, [])
        client = basic_test_setup(
            "./feathr_user_workspace/feathr_config.yaml")

        location_id = TypedKey(key_column="DOLocationID",
                               key_column_type=ValueType.INT32,
                               description="location id in NYC",
                               full_name="nyc_taxi.location_id")

        feature_query = FeatureQuery(
            feature_list=["f_location_avg_fare", "f_trip_time_rounded"], key=location_id)
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

        # download result and just assert the returned result is not empty
        res_df = get_result_df(client)
        assert res_df.shape[0] > 0

def test_feathr_get_offline_features_to_sql():
    """
    Test get_offline_features() can save data to SQL.
    """
    # runner.invoke(init, [])
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    client: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

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
    # Set DB user and password before submitting job
    # os.environ[f"sql1_USER"] = "some_user@feathrtestsql4"
    # os.environ[f"sql1_PASSWORD"] = "some_password"
    output_path = JdbcSink(name="sql1",
                            url="jdbc:sqlserver://feathrazureci.database.windows.net:1433;database=feathrci;encrypt=true;",
                            dbtable=f'feathr_ci_materialization_{str(now)[:19].replace(" ", "_").replace(":", "_").replace("-", "_")}',
                            auth="USERPASS")

    client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

@pytest.mark.skip(reason="Marked as skipped as we need to setup token and enable SQL AAD login for this test")
def test_feathr_get_offline_features_to_sql_with_token():
    """
    Test get_offline_features() can save data to SQL.
    """
    # runner.invoke(init, [])
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    client: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

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

    # Set DB token before submitting job
    # os.environ[f"SQL1_TOKEN"] = "some_token"
    os.environ["SQL1_TOKEN"] = client.credential.get_token("https://management.azure.com/.default").token
    output_path = JdbcSink(name="sql1",
                            url="jdbc:sqlserver://feathrazureci.database.windows.net:1433;database=feathrci;encrypt=true;",
                            dbtable=f'feathr_ci_sql_token_{str(now)[:19].replace(" ", "_").replace(":", "_").replace("-", "_")}',
                            auth="TOKEN")

    client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

@pytest.mark.skipif(os.environ.get('SPARK_CONFIG__SPARK_CLUSTER') == "databricks",
                    reason="Due to package conflicts, the CosmosDB test doesn't work on databricks clusters, refer to https://github.com/feathr-ai/feathr/blob/main/docs/how-to-guides/jdbc-cosmos-notes.md#using-cosmosdb-as-the-online-store for more details")
def test_feathr_materialize_to_cosmosdb():
    """
    Test FeathrClient() CosmosDbSink.
    """
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    # os.chdir(test_workspace_dir)

    client: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))

    now = datetime.now()
    container = ''.join(['feathrazure_cijob_materialize_','_', str(now.minute), '_', str(now.second), ""])
    sink = CosmosDbSink(name='cosmos1', endpoint='https://feathrazuretest3-cosmosdb.documents.azure.com:443/', database='feathr', container=container)
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[sink],
                                       feature_names=[
                                           "f_location_avg_fare", "f_location_max_fare"],
                                       backfill_time=backfill_time)
    client.materialize_features(settings)
    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

@pytest.mark.skip(reason="Marked as skipped as we need to setup resources for this test")
def test_feathr_materialize_to_es():
    """
    Test FeathrClient() CosmosDbSink.
    """
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    # os.chdir(test_workspace_dir)

    client: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))

    now = datetime.now()
    index = ''.join(['feathrazure_cijob_materialize_','_', str(now.minute), '_', str(now.second), ""])
    sink = ElasticSearchSink(name='es1', host='somenode:9200', index=index, ssl=True, auth=True)
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[sink],
                                       feature_names=[
                                           "f_location_avg_fare", "f_location_max_fare"],
                                       backfill_time=backfill_time)
    client.materialize_features(settings)
    # Set user and password before submitting job
    # os.environ[f"es1_USER"] = "some_user"
    # os.environ[f"es1_PASSWORD"] = "some_password"
    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

@pytest.mark.skip(reason="Marked as skipped as we need to setup resources for this test")
def test_feathr_materialize_to_aerospike():
    """
    Test FeathrClient() CosmosDbSink.
    """
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    # os.chdir(test_workspace_dir)
    now = datetime.now()
    # set workspace folder by time; make sure we don't have write conflict if there are many CI tests running
    os.environ['SPARK_CONFIG__DATABRICKS__WORK_DIR'] = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), '_', str(now.microsecond)])
    os.environ['SPARK_CONFIG__AZURE_SYNAPSE__WORKSPACE_DIR'] = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/feathr_github_ci','_', str(now.minute), '_', str(now.second) ,'_', str(now.microsecond)])

    client = FeathrClient(config_path="feathr_config.yaml")
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
    agg_features = [Feature(name="avgfare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="AVG",
                                                              window="90d",
                                                              )),
                    Feature(name="maxfare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="MAX",
                                                              window="90d"))
                    ]

    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                               source=batch_source,
                               features=agg_features)

    client.build_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=[
        f_trip_time_distance, f_trip_time_rounded])


    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))

    now = datetime.now()
    os.environ[f"aerospike_USER"] = "feathruser"
    os.environ[f"aerospike_PASSWORD"] = "feathr"
    as_sink = AerospikeSink(name="aerospike",seedhost="20.57.186.153", port=3000, namespace="test", setname="test")
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[as_sink],
                                       feature_names=[
                                           "avgfare", "maxfare"],
                                       backfill_time=backfill_time)
    client.materialize_features(settings)
    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

if __name__ == "__main__":
    test_feathr_materialize_to_aerospike()
    test_feathr_get_offline_features_to_sql()
    test_feathr_materialize_to_cosmosdb()

