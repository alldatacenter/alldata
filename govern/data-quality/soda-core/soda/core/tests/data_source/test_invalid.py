import pytest
from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source

id_regex = r"ID[0-9]"


def test_default_invalid(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_variables({"zero": "0"})
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - invalid_count(id) = ${{zero}}
        - valid_count(id) = 9
    """
    )
    scan.execute_unchecked()

    scan.assert_log_warning("Counting invalid without valid or invalid specification does not make sense")
    scan.assert_all_checks_pass()


def test_column_configured_invalid_values(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - invalid_count(id) = 6
        - valid_count(id) = 3
      configurations for {table_name}:
        valid values for id:
         - ID1
         - ID2
         - ID3
    """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_valid_min_max(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - invalid_count(cst_size) = 3:
            valid min: 0
        - invalid_count(cst_size) = 4:
            valid max: 0
    """
    )
    scan.execute()
    scan.assert_all_checks_pass()


@pytest.mark.skipif(
    test_data_source == "sqlserver",
    reason="Full regex support is not supported by SQLServer",
)
def test_valid_format_email(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - invalid_count(email) = 2:
                valid format: email

        """
    )
    scan.execute()

    scan.assert_all_checks_pass()


@pytest.mark.skipif(
    test_data_source == "sqlserver",
    reason="Full regex support is not supported by SQLServer. 'Percentage' format is supported but with limited functionality.",
)
def test_column_configured_invalid_and_missing_values(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - missing_count(pct) = 3
            - invalid_count(pct) = 1
            - valid_count(pct) = 6
          configurations for {table_name}:
            missing values for pct: ['N/A', 'No value']
            valid format for pct: percentage
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_valid_length(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - invalid_count(cat) = 2
            - valid_count(cat) = 3
          configurations for {table_name}:
            valid min length for cat: 4
            valid max length for cat: 4
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - invalid_count(cat) = 2
            - valid_count(cat) = 3
          configurations for {table_name}:
            valid length for cat: 4
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_check_and_column_configured_invalid_values(data_source_fixture: DataSourceFixture):
    """
    In case both column *and* check configurations are specified, they both are applied.
    """
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    digit_regex = data_source_fixture.data_source.escape_regex(id_regex)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - valid_count(id) = 9
            - valid_count(id) = 2:
                valid values:
                 - ID1
                 - ID2
            - invalid_count(id) = 0
            - invalid_count(id) = 7:
                valid values:
                 - ID1
                 - ID2
          configurations for {table_name}:
            valid regex for id: {digit_regex}
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()


def test_non_existing_format(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    scan = data_source_fixture.create_test_scan()

    format = "non-existing"

    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - invalid_count(email) = 2:
                valid format: {format}
        """
    )
    scan.execute_unchecked()

    scan.assert_log(f"Format {format} is not supported")


@pytest.mark.parametrize(
    "check",
    [
        pytest.param(
            """invalid_count(id) = 2:
                    invalid values:
                        - ID1
                        - ID2
            """,
            id="invalid values",
        ),
        pytest.param(
            """invalid_count(id) = 0:
                    invalid format: uuid
            """,
            id="invalid format",
        ),
        pytest.param(
            """invalid_count(id) = 9:
                    invalid regex: {{digit_regex}}
            """,
            id="invalid regex",
        ),
    ],
)
def test_invalid_with_invalid_config(check: str, data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    digit_regex = data_source_fixture.data_source.escape_regex(id_regex)

    check = check.replace("{{digit_regex}}", digit_regex)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
                - {check}
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()


@pytest.mark.parametrize(
    "check",
    [
        pytest.param(
            """valid_count(id) = 7:
                    invalid values:
                        - ID1
                        - ID2
            """,
            id="invalid values",
        ),
        pytest.param(
            """valid_count(id) = 9:
                    invalid format: uuid
            """,
            id="invalid format",
        ),
        pytest.param(
            """valid_count(id) = 0:
                    invalid regex: {{digit_regex}}
            """,
            id="invalid regex",
        ),
    ],
)
def test_valid_with_invalid_config(check: str, data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    digit_regex = data_source_fixture.data_source.escape_regex(id_regex)
    check = check.replace("{{digit_regex}}", digit_regex)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - {check}
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()
