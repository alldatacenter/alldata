from textwrap import dedent

from _pytest.python_api import raises
from soda.scan import Scan


def test_parse_environment_yaml(monkeypatch):
    # This is an example ~/.soda/local.yml file that engineers can put on their own
    # system locally inside their home folder to pass credentials and configurations
    # to Soda CLI and Soda Core library usage

    scan = Scan()
    scan.add_configuration_yaml_str(
        dedent(
            """
        data_source local_postgres_soda:
          type: postgres
          host: localhost
          username: simon
          database: soda
          schema: public

        soda_cloud:
          api_key_id: s09d8fs09d8f09sd
          scheme: http
    """
        )
    )
    scan.assert_no_error_nor_warning_logs()

    lpsp_properties: dict = scan._configuration.data_source_properties_by_name["local_postgres_soda"]
    assert lpsp_properties["type"] == "postgres"
    assert lpsp_properties["database"] == "soda"
    assert lpsp_properties["schema"] == "public"
    assert lpsp_properties["host"] == "localhost"
    assert lpsp_properties["username"] == "simon"
    assert lpsp_properties.get("password") is None

    assert scan._configuration.soda_cloud.api_key_id == "s09d8fs09d8f09sd"
    assert scan._configuration.soda_cloud.scheme == "http"


def test_parse_configuration_yaml_env_var_resolving(monkeypatch):
    # The whole file will be resolved at once with Jinja templating
    # env_var is a function that is made available to retrieve environment variables.

    monkeypatch.setenv("E", "x")

    scan = Scan()
    scan.add_configuration_yaml_str(
        dedent(
            """
        data_source local_postgres_soda:
          type: postgres
          host: ${ E }
          username: ${ env_var('E') }
          password: ${ env_var('E') }
          database: ${ env_var('E') }
          schema: ${ env_var('E') }

        soda_cloud:
          api_key_id: ${ env_var('E') }
          api_key_secret: ${ env_var('E') }
    """
        )
    )
    scan.assert_no_error_nor_warning_logs()

    lpsp_properties: dict = scan._configuration.data_source_properties_by_name["local_postgres_soda"]
    assert lpsp_properties["type"] == "postgres"
    assert lpsp_properties["database"] == "x"
    assert lpsp_properties["schema"] == "x"
    assert lpsp_properties["host"] == "x"
    assert lpsp_properties["username"] == "x"
    assert lpsp_properties["password"] == "x"

    assert scan._configuration.soda_cloud.api_key_id == "x"
    assert scan._configuration.soda_cloud.scheme == "https"


def test_no_data_source_type():
    scan = Scan()
    scan.add_configuration_yaml_str(
        dedent(
            """
        data_source local_postgres_soda:
          host: localhost
    """
        )
    )

    assert scan.has_error_logs()
    error_log_text = scan.get_error_logs_text()
    assert "type is required" in error_log_text
    with raises(AssertionError) as excinfo:
        scan.assert_no_error_logs()
    exception_messsage = str(excinfo.value)
    assert "type is required" in exception_messsage
