from textwrap import dedent

from cli.run_cli import run_cli
from helpers.data_source_fixture import DataSourceFixture
from helpers.mock_file_system import MockFileSystem


def test_warn(data_source_fixture: DataSourceFixture, mock_file_system: MockFileSystem):
    user_home_dir = mock_file_system.user_home_dir()

    mock_file_system.files = {
        f"{user_home_dir}/configuration.yml": data_source_fixture.create_test_configuration_yaml_str(),
        f"{user_home_dir}/checks.yml": dedent(
            f"""

            """
        ).strip(),
    }

    result = run_cli(
        [
            "scan",
            "-d",
            data_source_fixture.data_source_name,
            "-c",
            "configuration.yml",
            "-v",
            "scan_execution_date=2020-06-25 00:00:00",
            "checks.yml",
        ]
    )
    assert result.exit_code == 0
