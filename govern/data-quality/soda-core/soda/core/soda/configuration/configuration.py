from __future__ import annotations

from soda.cloud.dbt_config import DbtCloudConfig
from soda.cloud.soda_cloud import SodaCloud
from soda.common.file_system import file_system
from soda.execution.telemetry import Telemetry
from soda.sampler.sampler import Sampler
from soda.sampler.soda_cloud_sampler import SodaCloudSampler
from soda.scan import Scan


class Configuration:
    def __init__(self, scan: Scan):
        self.scan = scan
        self.data_source_properties_by_name: dict[str, dict] = {}
        self.telemetry: Telemetry | None = Telemetry()
        self.soda_cloud: SodaCloud | None = None
        self.file_system = file_system()
        self.sampler: Sampler = SodaCloudSampler()
        self.dbt_cloud: DbtCloudConfig | None = None
        self.exclude_columns: dict[str, list] = {}
        self.samples_limit: int | None = None

    def add_spark_session(self, data_source_name: str, spark_session):
        self.data_source_properties_by_name[data_source_name] = {
            "type": "spark_df",
            "connection": "spark_df_data_source",
            "spark_session": spark_session,
        }

    def add_dask_context(self, data_source_name: str, dask_context):
        self.data_source_properties_by_name[data_source_name] = {
            "type": "dask",
            "connection": "dask_data_source",
            "context": dask_context,
        }

    def add_duckdb_connection(self, data_source_name: str, duckdb_connection):
        self.data_source_properties_by_name[data_source_name] = {
            "type": "duckdb",
            "connection": "duckdb_connection_data_source",
            "duckdb_connection": duckdb_connection,
        }
