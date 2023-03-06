from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture

mock_schema = [
    {"type": "number", "name": "priority"},
    {"type": "singleSelect", "allowedValues": ["sales", "marketing"], "name": "department"},
    {"type": "multiSelect", "allowedValues": ["generated", "user-created"], "name": "tags"},
    {"type": "text", "name": "sales_owner"},
    {"type": "datetime", "name": "arrival_date"},
    {"type": "datetime", "name": "arrival_datetime"},
]
mock_variables = {"DEPT": "sales"}


def test_check_attributes_valid(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.mock_check_attributes_schema(mock_schema)
    scan.add_variables(mock_variables)
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - row_count > 0:
            attributes:
                priority: 1
                department: ${{DEPT}}
                ${{DEPT}}_owner: John Doe
                tags: ["user-created"]
                arrival_date: "2022-12-12"
                arrival_datetime: "2022-12-12T12:00:00"
    """
    )
    scan.execute()
    scan.assert_all_checks_pass()

    scan_result = scan.build_scan_results()
    assert scan_result["checks"][0]["resourceAttributes"] == [
        {"name": "priority", "value": "1"},
        {"name": "department", "value": "sales"},
        {"name": "sales_owner", "value": "John Doe"},
        {"name": "tags", "value": ["user-created"]},
        {"name": "arrival_date", "value": "2022-12-12"},
        {"name": "arrival_datetime", "value": "2022-12-12T12:00:00"},
    ]


def test_check_attributes_invalid(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.mock_check_attributes_schema(mock_schema)
    scan.add_variables(mock_variables)
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - row_count > 0:
            attributes:
                priority: "high"
                something-invalid: some-value
                tags: ["unknown"]
                arrival_date: 2022/01/01
                arrival_datetime: 2022/01/01T01:01:01

    """
    )
    scan.execute_unchecked()

    scan_result = scan.build_scan_results()
    assert scan_result["checks"] == []

    scan.assert_has_error(
        "Soda Cloud does not recognize 'tags': '['unknown']' attribute value. Valid attribute value(s): ['generated', 'user-created']"
    )
    scan.assert_has_error(
        "Soda Cloud does not recognize 'DoubleQuotedScalarString' type of attribute 'priority'. It expects the following type(s): ['int', 'float']"
    )
    scan.assert_has_error("Soda Cloud does not recognize 'something-invalid' attribute name.")
    scan.assert_has_error(
        "Soda Cloud expects an ISO formatted date or datetime value for the 'arrival_date' attribute."
    )
    scan.assert_has_error(
        "Soda Cloud expects an ISO formatted date or datetime value for the 'arrival_datetime' attribute."
    )


def test_foreach_attributes(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.mock_check_attributes_schema(mock_schema)
    scan.add_variables(mock_variables)
    scan.add_sodacl_yaml_str(
        f"""
      for each dataset D:
        datasets:
              - {table_name}
        checks:
        - row_count > 0:
            attributes:
                priority: 1.333
                tags: ["generated"]
                department: ${{DEPT}}
                ${{DEPT}}_owner: John Doe

    """
    )
    scan.execute()
    scan.assert_all_checks_pass()

    scan_result = scan.build_scan_results()
    assert scan_result["checks"][0]["resourceAttributes"] == [
        {"name": "priority", "value": "1.333"},
        {"name": "tags", "value": ["generated"]},
        {"name": "department", "value": "sales"},
        {"name": "sales_owner", "value": "John Doe"},
    ]


def test_check_attributes_skip_invalid(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.mock_check_attributes_schema(mock_schema)
    scan.add_variables(mock_variables)
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - row_count > 0:
            name: count
            attributes:
                priority: 1
        - missing_count(id) = 0:
            attributes:
                does-not-exist: 1
    """
    )
    scan.execute_unchecked()

    scan_result = scan.build_scan_results()
    assert len(scan_result["checks"]) == 0


def test_all_supported_check_types(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.mock_check_attributes_schema(mock_schema)
    scan.add_variables(mock_variables)
    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - row_count > 0:
            name: count
            attributes:
                priority: 1
        - schema:
            fail:
                when forbidden column present: [xxx]
            attributes:
                priority: 1
        - failed rows:
            fail condition: cat = 'xxx'
            attributes:
                priority: 1
        - values in (cst_size) must exist in {table_name} (cst_size):
            attributes:
                priority: 1
        - freshness(ts) < 10000d:
            attributes:
                priority: 1
    """
    )
    scan.execute()
    scan.assert_all_checks_pass()
    scan.assert_no_error_nor_warning_logs()
