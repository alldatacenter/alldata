from textwrap import dedent

from soda.scan import Scan


def test_invalid_data_source_header():
    scan = Scan()
    scan.add_configuration_yaml_str(
        dedent(
            """
                data_source:
                  type: postgres
            """
        ).strip()
    )

    scan.assert_has_error("Invalid configuration header")


def test_missing_type():
    scan = Scan()
    scan.add_configuration_yaml_str(
        dedent(
            """
                data_source postgres:
                  connection:
                    host: h
            """
        ).strip()
    )

    scan.assert_has_error("type is required")
