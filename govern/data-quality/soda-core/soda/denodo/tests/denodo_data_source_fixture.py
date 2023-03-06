from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture

logger = logging.getLogger(__name__)


class DenodoDataSourceFixture(DataSourceFixture):
    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source denodo": {
                "type": "denodo",
                "host": "localhost",
                "username": os.getenv("DENODO_USERNAME", "sodasql"),
                "password": os.getenv("DENODO_PASSWORD"),
                "database": os.getenv("DENODO_DATABASE", "sodasql"),
            }
        }

    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)
