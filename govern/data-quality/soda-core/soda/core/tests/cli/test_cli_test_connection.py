from textwrap import dedent

import pytest
from cli.run_cli import run_cli
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source
from helpers.mock_file_system import MockFileSystem


@pytest.mark.skipif(
    test_data_source in ["spark_df", "dask"],
    reason="Spark DF and dask only supports programmatic scans.",
)
def test_connection_test(data_source_fixture: DataSourceFixture, mock_file_system: MockFileSystem):
    user_home_dir = mock_file_system.user_home_dir()
    mock_file_system.files = {
        f"{user_home_dir}/configuration.yml": dedent(
            str(data_source_fixture.create_test_configuration_yaml_str())
        ).strip(),
    }

    result = run_cli(
        [
            "test-connection",
            "-d",
            data_source_fixture.data_source_name,
            "-c",
            "configuration.yml",
        ]
    )

    assert result.exit_code == 0


@pytest.mark.skipif(
    test_data_source == "spark_df",
    reason="Spark DF only supports programmatic scans.",
)
def test_connection_test_fail(data_source_fixture: DataSourceFixture, mock_file_system: MockFileSystem):
    user_home_dir = mock_file_system.user_home_dir()
    mock_file_system.files = {
        f"{user_home_dir}/configuration.yml": dedent(
            str(data_source_fixture.create_test_configuration_yaml_str())
        ).strip(),
    }

    result = run_cli(
        [
            "test-connection",
            "-d",
            "non-existing",
            "-c",
            "configuration.yml",
        ]
    )

    assert result.exit_code == 1
