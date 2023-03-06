from soda.scan import Scan


def example_python_api():
    # The scan_name is only required when uploading the scan results to Soda Cloud.  The scan_name is used as
    # an identifier to link the scan result information between subsequent scans.
    scan = Scan()
    # Configuring the environment
    #############################

    # Specify the datasource on which the check files are going to be executed.
    # If there is exactly 1 datasource in all the environment config files, that one will be
    # set as the datasource automatically.  But that requires that you invoke the add_environment_* methods
    # Before the add_sodacl_yaml_* methods
    scan.set_data_source_name("events")

    # If this scan is connected to Soda Cloud the scan_name is used to correlate subsequent scans
    # from the same schedule and will potentially be shows to Soda Cloud users to attach their scan file to a schedule.
    scan.set_scan_definition_name("Default events schedule 6am UTC")

    # See docs/soda_environment_yaml.md for details on how to configure data_sources in the environment YAML

    # Typically there will be just 1 environment file containing the connection details of all the data_sources
    # The file can refer to environment variables for the credentials
    # file_path's starting with ~ will be resolved to the user home directory
    scan.add_configuration_yaml_file(file_path="~/.soda/my_local_soda_environment.yml")

    # Environment YAML can also be specified as the content of an environment variable
    scan.add_configuration_yaml_from_env_var(env_var_name="SODA_ENV")

    # Environment YAML can also be loaded from all variables starting with a prefix
    scan.add_configuration_yaml_from_env_vars(prefix="SODA_")

    # Environment YAML can also be included in the Python code as a string:
    scan.add_configuration_yaml_str(
        """
        data_source events:
          type: snowflake
          host: ${SNOWFLAKE_HOST}
          username: ${SNOWFLAKE_USERNAME}
          password: ${SNOWFLAKE_PASSWORD}
          database: events
          schema: public

        soda_cloud:
          api_key_id: "..."
          api_key_secret: "..."
          host: sodadata.io

    """
    )

    # Adding SodaCL files
    #####################

    # Any number of SodaCL files can be loaded
    scan.add_sodacl_yaml_file("./my_programmatic_test_scan/sodacl_file_one.yml")
    scan.add_sodacl_yaml_file("./my_programmatic_test_scan/sodacl_file_two.yml")
    # add_sodacl_yaml_files takes dir as well as file paths
    scan.add_sodacl_yaml_files("./my_scan_dir")
    scan.add_sodacl_yaml_files("./my_scan_dir/sodacl_file_three.yml")

    # Spark
    ###########
    from pyspark.sql import SparkSession, types

    spark_session = None
    # Or alternatively create a testing spark session with:
    spark_session = SparkSession.builder.master("local").appName("test").getOrCreate()

    # Construct the dataframes, example users df can be used to test things out.
    users_df = spark_session.createDataFrame(
        data=[{"id": "1", "name": "John Doe"}],
        schema=types.StructType(
            [types.StructField("id", types.StringType()), types.StructField("name", types.StringType())]
        ),
    )
    users_df.createOrReplaceTempView("users")

    df1 = None
    df2 = None
    # If the spark_session is connected to a Hive catalog, all the table names will be known already in the spark_session
    # If the dataframes referenced in the SodaCL files are not registered in a connected catalog,
    # users can link the dataframes manually to the name referenced in the SodaCL files
    df1.createOrReplaceTempView("df1")
    df2.createOrReplaceTempView("df2")
    scan.add_spark_session(spark_session=spark_session, data_source_name="the_spark_data_source")

    # Variables
    ###########
    scan.add_variables({"date": "2022-01-01"})

    # Execute the scan
    ##################
    # Set logs to verbose mode, equivalent to CLI -V option
    scan.set_verbose(True)

    # Execute the scan
    scan.execute()

    # Inspect the scan result
    #########################

    # Typical checking
    scan.assert_no_error_logs()
    scan.assert_no_checks_fail()

    # More advanced scan execution log introspection methods
    scan.has_error_logs()
    scan.get_error_logs_text()

    # Inspect the scan logs.
    scan.get_logs_text()

    # More advanced check results details methods
    scan.get_checks_fail()
    scan.has_check_fails()
    scan.get_checks_fail_text()
    scan.assert_no_checks_warn_or_fail()
    scan.get_checks_warn_or_fail()
    scan.has_checks_warn_or_fail()
    scan.get_checks_warn_or_fail_text()
    scan.get_all_checks_text()

    # Optionally users can inspect the details of the check results:
    # scan.checks: List[Check]
