from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from helpers.fixtures import test_data_source


def test_formats(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    test_definitions = {
        "integer": {
            "passing_values": ["0", "1234567890", "-0", "- 1234567890", "+0", "+1"],
            "failing_values": ["", "a", " ", "1.5", "4,2"],
        },
        "positive integer": {
            "passing_values": ["0", "1234567890", "+0", "+1"],
            "failing_values": [
                "",
                "a",
                " ",
                "-0",
                "- 1234567890",
            ],
        },
        "negative integer": {
            "passing_values": [
                "0",
                "-0",
                "- 1234567890",
            ],
            "failing_values": ["", "a", " ", "1234567890", "+0", "+1"],
        },
        "percentage": {
            "passing_values": [
                "0%",
                " 0 % ",
                "- 0 %",
                "+ 0 %",
                "010%",
                "0.0 %",
                "0,0  %",
                ".0 %",
                ",0 %",
                "99.99%",
            ],
            "failing_values": ["", " ", "%", "a %", "0", "0.0"],
        },
        "date us": {
            "passing_values": ["1/1/2020", "01/06/2020", "12/31/1925", "11-13-1981", "9-05-2000"],
            "failing_values": [
                "",
                " ",
                "a",
                "1/1/2020 12:00:00",
                "1/1/2020 12:00",
                "13/11/1981",
                "31-12-1925",
            ],
        },
        "date eu": {
            "passing_values": ["1/1/2020", "01/06/2020", "31/12/1925", "13-11-1981", "9-05-2000"],
            "failing_values": [
                "",
                " ",
                "a",
                "1/1/2020 12:00:00",
                "1/1/2020 12:00",
                "11-13-1981",
                "12/31/1925",
            ],
        },
        "date inverse": {
            "passing_values": ["2020/1/1", "2020/01/06", "1925-12-31", "1981-11-13", "2000/9/05"],
            "failing_values": [
                "",
                " ",
                "a",
                "2020/1/1 12:00:00",
                "2020/1/1 12:00",
                "1981-13-11",
                "1925/31/12",
            ],
        },
        "date iso 8601": {
            "passing_values": [
                "2020-02-08",
                "2020-W06-5",
                "2020-039",
                "20200208",
                "2020W065",
                "2020W06",
                "2020039",
                "2020-02-08T09",
                "2020-02-08 09",
                "2020-02-08 09:30",
                "2020-02-08 09:30:26",
                "2020-02-08 09:30:26.123",
                "20200208T080910,123",
                "20200208T080910.123",
                "20200208T080910",
                "20200208T0809",
                "20200208T08",
                "2020-W06-5 09",
                "2020-039 09",
                "2020-02-08 09+07:00",
                "2020-02-08 09+07:00",
                "2020-02-08 09-0100",
                "2020-02-08 09Z",
                "2020-04-30",
                "2020-04-30T00:00:00.000",
            ],
            "failing_values": [
                "",
                " ",
                "a",
                "9999-01-01",
                "2000-13-01",
                "2000-01-32",
            ],
        },
    }

    if test_data_source == "sqlserver":
        test_definitions.pop("percentage")  # Partially supported.
        test_definitions.pop("date us")  # Partially supported.
        test_definitions.pop("date eu")  # Partially supported.
        test_definitions.pop("date inverse")  # Partially supported.
        test_definitions.pop("date iso 8601")  # Not supported.
    elif test_data_source == "dask":
        test_definitions.pop("date iso 8601")

    for format, values in test_definitions.items():
        assert_format_values(
            format,
            data_source_fixture,
            table_name,
            passing_values=values["passing_values"],
            failing_values=values["failing_values"],
        )


def assert_format_values(format, data_source_fixture: DataSourceFixture, table_name, passing_values, failing_values):
    data_source = data_source_fixture.data_source
    qualified_table_name = data_source.qualified_table_name(table_name)

    def set_up_expression(value: str, format: str) -> str:
        expression = data_source.get_default_format_expression(f"'{value}'", format)
        # Special handling for sqlserver and teradata - expression matching cannot be used in the SELECT statement, so wrap it in CASE ... THEN ... ELSE for this test.
        if test_data_source in ["sqlserver", "teradata"]:
            expression = f"CASE WHEN {expression} THEN 1 ELSE 0 END"

        return expression

    values = []
    expressions = []
    expected_values = []
    for passing_value in passing_values:
        expressions.append(set_up_expression(passing_value, format))
        values.append(passing_value)
        expected_values.append(True)
    for failing_value in failing_values:
        expressions.append(set_up_expression(failing_value, format))
        values.append(failing_value)
        expected_values.append(False)

    expressions_sql = ",\n  ".join(expressions)
    sql = f"SELECT \n  {expressions_sql} FROM {qualified_table_name}"
    row = data_source_fixture._fetch_all(sql)[0]
    if test_data_source == "dask":
        # Parse string boolean values to boolean.
        row = [value == "True" for value in row]

    failures_messages = []
    for index, expected_value in enumerate(expected_values):
        actual_value = row[index]
        if actual_value != expected_value:
            if expected_values[index]:
                failures_messages.append(f'"{values[index]}" not valid "{format}", expected valid')
            else:
                failures_messages.append(f'"{values[index]}" valid "{format}", expected not valid')

    if failures_messages:
        raise AssertionError("\n".join(failures_messages))
