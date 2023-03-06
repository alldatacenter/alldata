from __future__ import annotations

# Initialize telemetry in test mode. This is done before importing anything datasource/scan/scanner related which initializes telemetry in standard mode so that we avoid unnecessary setup and re-setup which easily causes errors.
from soda.telemetry.soda_telemetry import SodaTelemetry

soda_telemetry = SodaTelemetry.get_instance(test_mode=True)

import logging
import os
from typing import Any

import pytest
from dotenv import load_dotenv
from helpers.data_source_fixture import DataSourceFixture
from helpers.mock_file_system import MockFileSystem
from soda.common.file_system import FileSystemSingleton
from soda.common.logs import configure_logging

logger = logging.getLogger(__name__)

# Load local env file so that test data sources can be set up.
project_root_dir = __file__[: -len("soda/core/tests/helpers/fixtures.py")]
load_dotenv(f"{project_root_dir}/.env", override=True)

# In global scope because it is used in pytest annotations, it would not work as a fixture.
test_data_source = os.getenv("test_data_source", "postgres")


def pytest_sessionstart(session: Any) -> None:
    configure_logging()


def pytest_runtest_logstart(nodeid: str, location: tuple[str, int | None, str]) -> None:
    """
    Prints the test function name and the location in a format that PyCharm recognizes and turns into a link in the console
    """
    logging.debug(f'### "soda/core/tests/{location[0]}:{(location[1]+1)}" {location[2]}')


@pytest.fixture(scope="session")
def data_source_fixture():
    data_source_fixture = DataSourceFixture._create()
    data_source_fixture._test_session_starts()
    yield data_source_fixture
    data_source_fixture._test_session_ends()


@pytest.fixture
def mock_file_system():
    """
    Fixture that swaps the file_system() with an in-memory, mock version
    Usage:

    def test_mytest_with_mock_file_system(mock_file_system):
        mock_file_system.files = {
          '~/.soda/configuration.yml': '...content of the file...',
          '/project/mytable.soql.yml': '...content of the file...'
        }

        # Now file system will use the given the in memory string files above instead of using the real file system
        file_system().exists('~/.soda/configuration.yml') -> True
    """
    original_file_system = FileSystemSingleton.INSTANCE
    FileSystemSingleton.INSTANCE = MockFileSystem()
    yield FileSystemSingleton.INSTANCE
    FileSystemSingleton.INSTANCE = original_file_system


def format_query_one_line(query: str) -> str:
    """@TODO: implement"""
    return query
