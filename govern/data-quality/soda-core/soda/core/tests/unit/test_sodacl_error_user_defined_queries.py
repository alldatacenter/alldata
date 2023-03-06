from textwrap import dedent

from soda.scan import Scan


def test_user_defined_metric_query_unsupported_configuration():
    scan = Scan()
    scan.add_sodacl_yaml_str(
        dedent(
            """
      checks for CUSTOMERS:
        - avg_surface between 1068 and 1069:
            typo config: AVG(cst_size * distance)
    """
        )
    )
    scan.assert_has_error("Skipping unsupported check configuration: typo config")


def test_user_defined_metric_query_metric_name_typo():
    scan = Scan()
    scan.add_sodacl_yaml_str(
        dedent(
            """
      checks for CUSTOMERS:
        - avg_surface between 1068 and 1069:
            avg_surfas expression: AVG(cst_size * distance)
    """
        )
    )
    scan.assert_has_error(
        'In configuration "avg_surfas expression" the metric name must match exactly the metric name in the check "avg_surface"'
    )


def test_typo_in_metric_name():
    scan = Scan()
    scan.add_sodacl_yaml_str(
        dedent(
            """
      checks for CUSTOMERS:
        - inval id_percent(pct) < 5 %
    """
        )
    )
    scan.assert_has_error('Invalid check "inval id_percent(pct) < 5 %"')

    assert len(scan.get_error_logs()) == 1
