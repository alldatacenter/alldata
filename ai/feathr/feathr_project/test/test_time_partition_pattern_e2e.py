import os
from datetime import datetime, timedelta
from pathlib import Path
from feathr import FeathrClient
from feathr import (BackfillTime, MaterializationSettings)
from feathr import FeathrClient

from feathr import HdfsSink
from feathr.utils.job_utils import get_result_df, copy_cloud_dir, cloud_dir_exists
from test_fixture import (basic_test_setup, time_partition_pattern_feature_gen_test_setup, time_partition_pattern_feature_join_test_setup)
from test_utils.constants import Constants
'''
def setup_module():
    """
    Prepare data sources for 'timePartitionPattern' test cases
    """
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
   
    # Create data sources to support testing with 'timePartitionPattern' cases below
    client_producer: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    if client_producer.spark_runtime == 'databricks':
        output_path = 'dbfs:/timePartitionPattern_test'
        output_pf_path = 'dbfs:/timePartitionPattern_postfix_test/df0/daily/2020/05/01/postfixPath'
        output_hourly_path = 'dbfs:/timePartitionPattern_hourly_test/df0/daily/2020/05/01/00'
    else:
        output_path = 'abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/timePartitionPattern_test'
        output_pf_path = 'abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/timePartitionPattern_postfix_test/df0/daily/2020/05/01/postfixPath'
        output_hourly_path = 'abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/timePartitionPattern_hourly_test/df0/daily/2020/05/01/00'
     
    source_url = output_path + "/df0/daily/2020/05/01" 
    if not cloud_dir_exists(client_producer, source_url):
        backfill_time = BackfillTime(start=datetime(
            2020, 5, 1), end=datetime(2020, 5, 1), step=timedelta(days=1))
        offline_sink = HdfsSink(output_path=output_path)
        settings = MaterializationSettings("nycTaxiTable",
                sinks=[offline_sink],
                feature_names=[
                    "f_location_avg_fare", "f_location_max_fare"],
                backfill_time=backfill_time)
    
        client_producer.materialize_features(settings)
        # assuming the job can successfully run; otherwise it will throw exception
        client_producer.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)
    
        # Check if data sources prepared well
        res_df = get_result_df(client_producer, data_format="avro", res_url=source_url)
        assert res_df.shape[0] > 0
    
    # Copy created data sources to another folder to support 'postfix_path' test  
    if not cloud_dir_exists(client_producer, output_pf_path):
        copy_cloud_dir(client_producer, source_url, output_pf_path)
        res_df_pf = get_result_df(client_producer, data_format="avro", res_url=output_pf_path)
        assert res_df_pf.shape[0] > 0
    
    # Copy created data sources to another folder to support 'hourly' test
    if not cloud_dir_exists(client_producer, output_hourly_path):
        copy_cloud_dir(client_producer, source_url, output_hourly_path)
        res_df_hourly = get_result_df(client_producer, data_format="avro", res_url=output_hourly_path)
        assert res_df_hourly.shape[0] > 0
'''
def test_feathr_materialize_with_time_partition_pattern():
    """
    Test FeathrClient() using HdfsSource with 'timePartitionPattern'.
    """
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    
    client_dummy = FeathrClient(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    if client_dummy.spark_runtime == 'databricks':
        source_path = 'dbfs:/timePartitionPattern_test/df0/daily/'
    else:
        source_path = 'abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/timePartitionPattern_test/df0/daily/'
      
    client: FeathrClient = time_partition_pattern_feature_gen_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"), source_path,local_workspace_dir="test_materialize_tpp")
    
    backfill_time_tpp = BackfillTime(start=datetime(
        2020, 5, 2), end=datetime(2020, 5, 2), step=timedelta(days=1))
    now = datetime.now()
    if client.spark_runtime == 'databricks':
        output_path_tpp = ''.join(['dbfs:/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    else:
        output_path_tpp = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    offline_sink_tpp = HdfsSink(output_path=output_path_tpp)
    settings_tpp = MaterializationSettings("nycTaxiTable",
                                       sinks=[offline_sink_tpp],
                                       feature_names=[
                                           "f_loc_avg_output", "f_loc_max_output"],
                                       backfill_time=backfill_time_tpp)
    client.materialize_features(settings_tpp)
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)
    
    res_df = get_result_df(client, data_format="avro", res_url=output_path_tpp + "/df0/daily/2020/05/02")
    assert res_df.shape[0] > 0
     
def test_feathr_materialize_with_time_partition_pattern_postfix_path():
    """
    Test FeathrClient() using HdfsSource with 'timePartitionPattern' and 'postfixPath'.
    """  
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
   
    client_dummy = FeathrClient(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    if client_dummy.spark_runtime == 'databricks':
        source_path = 'dbfs:/timePartitionPattern_postfix_test/df0/daily/'
    else:
        source_path = 'abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/timePartitionPattern_postfix_test/df0/daily/'
    
    client: FeathrClient = time_partition_pattern_feature_gen_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"), source_path,local_workspace_dir="test_materialize_tpp_postfix", postfix_path='postfixPath')

    backfill_time_pf = BackfillTime(start=datetime(
        2020, 5, 2), end=datetime(2020, 5, 2), step=timedelta(days=1))
    now = datetime.now()
    if client.spark_runtime == 'databricks':
        output_path_pf = ''.join(['dbfs:/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    else:
        output_path_pf = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    offline_sink_pf = HdfsSink(output_path=output_path_pf)
    settings_pf = MaterializationSettings("nycTaxiTable",
                                       sinks=[offline_sink_pf],
                                       feature_names=[
                                           "f_loc_avg_output", "f_loc_max_output"],
                                       backfill_time=backfill_time_pf)
    client.materialize_features(settings_pf)
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res_df = get_result_df(client, data_format="avro", res_url=output_path_pf + "/df0/daily/2020/05/02")
    assert res_df.shape[0] > 0
    
def test_feathr_materialize_with_time_partition_pattern_hourly():
    """
    Test FeathrClient() using HdfsSource with hourly 'timePartitionPattern'.
    """   
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
   
    client_dummy = FeathrClient(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    if client_dummy.spark_runtime == 'databricks':
        source_path = 'dbfs:/timePartitionPattern_hourly_test/df0/daily/'
    else:
        source_path = 'abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/timePartitionPattern_hourly_test/df0/daily/'
    
    client: FeathrClient = time_partition_pattern_feature_gen_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"), source_path,local_workspace_dir="test_materialize_hourly", resolution='HOURLY')

    backfill_time_tpp = BackfillTime(start=datetime(
        2020, 5, 2), end=datetime(2020, 5, 2), step=timedelta(days=1))
    now = datetime.now()
    if client.spark_runtime == 'databricks':
        output_path_tpp = ''.join(['dbfs:/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    else:
        output_path_tpp = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    offline_sink_tpp = HdfsSink(output_path=output_path_tpp)
    settings_tpp = MaterializationSettings("nycTaxiTable",
                                       sinks=[offline_sink_tpp],
                                       feature_names=[
                                           "f_loc_avg_output", "f_loc_max_output"],
                                       backfill_time=backfill_time_tpp,
                                       resolution = 'HOURLY')
    client.materialize_features(settings_tpp)
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res_df = get_result_df(client, data_format="avro", res_url=output_path_tpp + "/df0/daily/2020/05/02/00")
    assert res_df.shape[0] > 0

def test_feathr_get_offline_with_time_partition_pattern_postfix_path():
    """
    Test FeathrClient() using HdfsSource with 'timePartitionPattern' and 'postfixPath'.
    """  
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
   
    client_dummy = FeathrClient(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    if client_dummy.spark_runtime == 'databricks':
        source_path = 'dbfs:/timePartitionPattern_postfix_test/df0/daily/'
    else:
        source_path = 'abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/timePartitionPattern_postfix_test/df0/daily/'
    
    [client, feature_query, settings] = time_partition_pattern_feature_join_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"), source_path, local_workspace_dir="test_offline_tpp",postfix_path='postfixPath')

    now = datetime.now()
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), ".avro"])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/output','_', str(now.minute), '_', str(now.second), ".avro"])
                
    client.get_offline_features(observation_settings=settings,
                            feature_query=feature_query,
                            output_path=output_path)
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res_df = get_result_df(client, data_format="avro", res_url = output_path)
    assert res_df.shape[0] > 0
    
