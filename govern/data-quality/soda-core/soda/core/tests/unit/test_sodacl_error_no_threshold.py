from textwrap import dedent

from soda.scan import Scan


def test_error_no_threshold():
    scan = Scan()
    scan.add_sodacl_yaml_str(
        dedent(
            """
                checks for CUSTOMERS:
                  - count
            """
        ).strip()
    )

    scan.assert_has_error("No threshold specified")
