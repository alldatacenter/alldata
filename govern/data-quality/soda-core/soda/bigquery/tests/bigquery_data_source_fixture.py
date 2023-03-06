from __future__ import annotations

import logging
import os

from google.cloud import bigquery
from helpers.data_source_fixture import DataSourceFixture
from helpers.test_table import TestTable

logger = logging.getLogger(__name__)


class BigQueryDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source bigquery": {
                "type": "bigquery",
                "dataset": schema_name or os.getenv("BIGQUERY_DATASET"),
                "account_info_json_path": os.getenv("BIGQUERY_ACCOUNT_INFO_JSON_PATH"),
                "account_info_json": os.getenv("BIGQUERY_ACCOUNT_INFO_JSON"),
            }
        }

    def _get_dataset_id(self):
        return f"{self.schema_data_source.project_id}.{self.schema_name}"

    def _create_schema_if_not_exists(self):
        dataset_id = self._get_dataset_id()
        dataset = bigquery.Dataset(dataset_id)
        dataset.location = "EU"
        logging.debug(f"CREATE SCHEMA: Creating BigQuery dataset '{dataset_id}'")
        try:
            self.schema_data_source.client.create_dataset(dataset, timeout=30)
            logging.debug(f"Create BigQuery dataset '{dataset_id}' OK")
        except Exception as e:
            logging.error(f"Creating BigQuery dataset '{dataset_id}' FAILED: {e}", e)

    def _drop_schema_if_exists(self):
        dataset_id = self._get_dataset_id()
        logging.debug(f"DROP SCHEMA: Deleting BigQuery dataset '{dataset_id}'")
        try:
            self.schema_data_source.client.delete_dataset(dataset_id, delete_contents=True, not_found_ok=True)
            logging.debug(f"Delete BigQuery dataset '{dataset_id}' OK")
        except Exception as e:
            logging.error(f"Deleting BigQuery dataset '{dataset_id}' FAILED: {e}", e)

    def _drop_test_table_sql(self, table_name):
        qualified_table_name = self.data_source.qualified_table_name(table_name)
        return f"DROP TABLE {qualified_table_name};"

    def _create_view_from_table_sql(self, test_table: TestTable):
        return f"CREATE VIEW {self.dataset}.{test_table.unique_view_name} AS SELECT * FROM {self.dataset}.{test_table.unique_table_name}"
