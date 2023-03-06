from textwrap import dedent

from soda.scan import Scan


def test_invalid_postgres_configs():
    scan = Scan()
    scan.set_data_source_name("postgresds")
    scan.add_configuration_yaml_str(
        dedent(
            """
                data_source postgresds:
                  type: postgres
                  connection:
                    heist: localhost
            """
        ).strip()
    )
    scan.add_sodacl_yaml_str(
        dedent(
            """
                checks for CUSTOMERS:
                  - row_count > 0
            """
        ).strip()
    )
    scan.execute()

    scan.assert_has_error('Could not connect to data source "postgresds"')
