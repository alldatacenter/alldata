import os
from datetime import datetime
from pathlib import Path
from unittest import result

from click.testing import CliRunner
from feathr import (BOOLEAN, FLOAT, INT32, FeatureQuery, ObservationSettings,
                    SparkExecutionConfiguration, TypedKey, ValueType)
from feathr.client import FeathrClient
from feathr.utils.job_utils import get_result_df

from test_fixture import basic_test_setup
from feathr.constants import OUTPUT_FORMAT

# test parquet file read/write without an extension name
def test_feathr_get_secrets_from_key_vault():
    """
    Test if the program can read the key vault secrets as expected
    """
    # TODO: need to test get_environment_variable() as well
    os.environ['SECRETS__AZURE_KEY_VAULT__NAME'] = 'feathrazuretest3-kv'

    # the config below doesn't have `ONLINE_STORE__REDIS__HOST` for testing purpose
    yaml_config = """
    project_config:
        project_name: 'project_feathr_integration_test'
    offline_store:
        s3:
            s3_enabled: true
            s3_endpoint: 's3.amazonaws.com'
        snowflake:
            url: "dqllago-ol19457.snowflakecomputing.com"
            user: "feathrintegration"
            role: "ACCOUNTADMIN"
    spark_config:
        spark_cluster: 'databricks'
        spark_result_output_parts: '1'
        azure_synapse:
            dev_url: 'https://feathrazuretest3synapse.dev.azuresynapse.net'
            pool_name: 'spark3'
            workspace_dir: 'abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/feathr_test_workspace'
            executor_size: 'Small'
            executor_num: 1

        databricks:
            workspace_instance_url: 'https://adb-2474129336842816.16.azuredatabricks.net/'
            workspace_token_value: ''
            config_template: '{"run_name":"FEATHR_FILL_IN","new_cluster":{"spark_version":"9.1.x-scala2.12","num_workers":1,"spark_conf":{"FEATHR_FILL_IN":"FEATHR_FILL_IN"},"instance_pool_id":"0403-214809-inlet434-pool-l9dj3kwz"},"libraries":[{"jar":"FEATHR_FILL_IN"}],"spark_jar_task":{"main_class_name":"FEATHR_FILL_IN","parameters":["FEATHR_FILL_IN"]}}'
            work_dir: 'dbfs:/feathr_getting_started'
            feathr_runtime_location: ''
    online_store:
        redis:
            port: 6380
            ssl_enabled: True
    feature_registry:
        purview:
            type_system_initialization: false
            purview_name: 'feathrazuretest3-purview1'
            delimiter: '__'
    """

    with open("/tmp/feathr_config.yaml", "w") as text_file:
        text_file.write(yaml_config)

    client = FeathrClient(config_path="/tmp/feathr_config.yaml")
    # `redis_host` should be there since it's not available in the environment variable, and not in the config file, we expect we get it from azure key_vault
    assert client.redis_host is not None

