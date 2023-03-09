import os
from pathlib import Path

from feathr import MonitoringSettings
from feathr import MonitoringSqlSink
from test_fixture import (basic_test_setup, get_online_test_table_name)
from test_utils.constants import Constants


def test_feature_monitoring():
    monitor_sink_table = get_online_test_table_name("nycTaxiCITableMonitoring")
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"

    client = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    monitor_sink = MonitoringSqlSink(table_name=monitor_sink_table)
    settings = MonitoringSettings("monitoringSetting",
                                  sinks=[monitor_sink],
                                  feature_names=[
                                      "f_location_avg_fare", "f_location_max_fare"])
    client.monitor_features(settings)
    # just assume the job is successful without validating the actual result in Redis. Might need to consolidate
    # this part with the test_feathr_online_store test case
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)
