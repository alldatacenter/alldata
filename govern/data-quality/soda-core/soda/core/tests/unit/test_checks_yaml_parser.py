import logging
from textwrap import dedent

from soda.common.log import LogLevel
from soda.scan import Scan

logger = logging.getLogger(__name__)


def test_checks_parsing():
    scan = Scan()
    scan.add_sodacl_yaml_str(
        sodacl_yaml_str=dedent(
            """
      checks for CUSTOMERS:
        - row_count > 0
        - row_count between .0005 and 10.45
        - row_count between (0 and 10.
        - row_count between (0 and 10.0)
        - invalid_count(cst_size) >= .5
        - change avg last 7 for missing(cst_size) between -100 and +1)
      checks for "Quoted table name with weird $%()[]\\" chars":
        - row_count between 100 and 500
        - custom_column_metric("Weird column with [$%\\(\\)[] special chars") = 0
    """
        )
    )

    scan.assert_no_error_nor_warning_logs()

    data_source_scan_cfg = scan._sodacl_cfg.data_source_scan_cfgs[None]
    customers_table_cfg = data_source_scan_cfg.tables_cfgs["CUSTOMERS"]
    customers_partition_cfg = customers_table_cfg.partition_cfgs[0]
    assert len(customers_partition_cfg.check_cfgs) == 4
    assert len(customers_partition_cfg.column_checks_cfgs["cst_size"].check_cfgs) == 2

    weird_name_table_cfg = data_source_scan_cfg.tables_cfgs['"Quoted table name with weird $%()[]\\" chars"']
    weird_name_partition_cfg = weird_name_table_cfg.partition_cfgs[0]
    assert len(weird_name_partition_cfg.check_cfgs) == 1
    assert (
        len(weird_name_partition_cfg.column_checks_cfgs['"Weird column with [$%\\(\\)[] special chars"'].check_cfgs)
        == 1
    )


def test_configurations_parsing():
    scan = Scan()
    scan.add_sodacl_yaml_str(
        dedent(
            """
      checks for CUSTOMERS:
        - row_count > 0:
            filter: |
              "country" NOT IN ('UK', 'US')

      configurations for CUSTOMERS:
        missing values for id: [N/A, No value]
        valid format for id: uuid
        valid max length for cst_size: 6
        valid min for cst_size: -500.10
    """
        ).strip()
    )
    scan.assert_no_error_nor_warning_logs()

    data_source_scan_cfg = scan._sodacl_cfg.data_source_scan_cfgs[None]
    customers_table_cfg = data_source_scan_cfg.tables_cfgs["CUSTOMERS"]

    assert customers_table_cfg.column_configurations_cfgs["id"].missing_values == ["N/A", "No value"]
    assert customers_table_cfg.column_configurations_cfgs["id"].valid_format == "uuid"
    assert customers_table_cfg.column_configurations_cfgs["cst_size"].valid_max_length == 6
    assert customers_table_cfg.column_configurations_cfgs["cst_size"].valid_min == -500.10


def test_wrong_yaml():
    scan = Scan()
    scan.add_sodacl_yaml_str(
        dedent(
            """
      checks for CUSTOMERS: ...
      "invalid" yaml
    """
        ).strip()
    )

    log = next(log for log in scan._logs.logs if "yaml syntax error" in log.message.lower())
    assert log.level == LogLevel.ERROR
    assert '"invalid" yaml' in str(log.exception).lower()
    assert log.location.line == 2
    assert log.location.col == 1


def test_invalid_check():
    scan = Scan()
    scan.add_sodacl_yaml_str(
        dedent(
            """
      checks for CUSTOMERS:
          - this is not a good check
    """
        ).strip()
    )
    log = next(log for log in scan._logs.logs if "this is not a good check" in log.message.lower())
    assert log.level == LogLevel.ERROR
    assert "invalid check" in log.message.lower()
