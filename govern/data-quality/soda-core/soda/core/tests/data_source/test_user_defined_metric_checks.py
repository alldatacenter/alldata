import os
import tempfile

from helpers.common_test_tables import customers_test_table
from helpers.data_source_fixture import DataSourceFixture
from soda.execution.check_outcome import CheckOutcome


def test_user_defined_table_expression_metric_check(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    length_expr = "LEN" if data_source_fixture.data_source_name == "sqlserver" else "LENGTH"

    ones_expression = f"SUM({length_expr}(cst_size_txt))"

    scan.add_sodacl_yaml_str(
        f"""
      checks for {table_name}:
        - avg_surface between 1068 and 1069:
            avg_surface expression: AVG(cst_size * distance)
        - ones(cst_size_txt):
            name: The total length of cst_size_txt must be 14
            ones expression: {ones_expression}
            warn: when <= 14
            fail: when > 14
    """
    )
    scan.execute()

    avg_surface_check = scan._checks[0]
    avg_surface = avg_surface_check.check_value
    assert isinstance(avg_surface, float)
    assert 1068 < avg_surface < 1069
    assert avg_surface_check.outcome == CheckOutcome.PASS

    ones_check = scan._checks[1]
    assert ones_check.check_value == 14
    assert ones_check.check_cfg.name == "The total length of cst_size_txt must be 14"
    assert ones_check.outcome == CheckOutcome.WARN


def test_user_defined_table_expression_metric_check_with_variables(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    scan = data_source_fixture.create_test_scan()
    scan.add_variables({"dist": "distance"})
    scan.add_sodacl_yaml_str(
        f"""
          checks for {table_name}:
            - avg_surface between 1068 and 1069:
                avg_surface expression: AVG(cst_size * ${{dist}})
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()

    avg_surface = scan._checks[0].check_value
    assert isinstance(avg_surface, float)
    assert 1068 < avg_surface < 1069


def test_user_defined_data_source_query_metric_check(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    qualified_table_name = data_source_fixture.data_source.qualified_table_name(table_name)

    scan = data_source_fixture.create_test_scan()
    scan.add_sodacl_yaml_str(
        f"""
          checks:
            - avg_surface between 1068 and 1069:
                avg_surface query: |
                  SELECT AVG(cst_size * distance) as avg_surface
                  FROM {qualified_table_name}
        """
    )
    scan.execute()

    scan.assert_all_checks_pass()

    avg_surface = scan._checks[0].check_value
    assert isinstance(avg_surface, float)
    assert 1068 < avg_surface < 1069


def test_user_defined_data_source_query_metric_check_with_variable(data_source_fixture: DataSourceFixture):
    table_name = data_source_fixture.ensure_test_table(customers_test_table)

    qualified_table_name = data_source_fixture.data_source.qualified_table_name(table_name)

    scan = data_source_fixture.create_test_scan()
    scan.add_variables({"dist": "distance"})
    scan.add_sodacl_yaml_str(
        f"""
              checks:
                - avg_surface between 1068 and 1069:
                    avg_surface query: |
                      SELECT AVG(cst_size * ${{dist}}) as avg_surface
                      FROM {qualified_table_name}
            """
    )
    scan.execute()

    scan.assert_all_checks_pass()

    avg_surface = scan._checks[0].check_value
    assert isinstance(avg_surface, float)
    assert 1068 < avg_surface < 1069


def test_user_defined_data_source_query_metric_with_sql_file(data_source_fixture: DataSourceFixture):
    fd, path = tempfile.mkstemp()
    table_name = data_source_fixture.ensure_test_table(customers_test_table)
    qualified_table_name = data_source_fixture.data_source.qualified_table_name(table_name)

    scan = data_source_fixture.create_test_scan()
    try:
        with os.fdopen(fd, "w") as tmp:
            tmp.write(f"SELECT AVG(cst_size * distance) as avg_surface \n" f"FROM {qualified_table_name}")

        scan.add_sodacl_yaml_str(
            f"""
              checks:
                - avg_surface between 1068 and 1069:
                    avg_surface sql_file: "{path}"
                """
        )
        scan.execute()
        scan.assert_all_checks_pass()
        avg_surface = scan._checks[0].check_value
        assert isinstance(avg_surface, float)
        assert 1068 < avg_surface < 1069

    finally:
        os.remove(path)
