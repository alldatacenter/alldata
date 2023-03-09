from feathr import ObservationSettings


def test_observation_setting_with_timestamp():
    observation_settings = ObservationSettings(
        observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss")
    config = observation_settings.to_feature_config()
    expected_config = """
            settings: {
                joinTimeSettings: {
                    timestampColumn: {
                        def: "lpep_dropoff_datetime"
                        format: "yyyy-MM-dd HH:mm:ss"
                    }
                }
            }
            
            observationPath: "wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv"
        """
    assert ''.join(config.split()) == ''.join(expected_config.split())


def test_observation_setting_without_timestamp():

    observation_settings = ObservationSettings(
        observation_path='snowflake://snowflake_account/?dbtable=CALL_CENTER&sfDatabase=SNOWFLAKE_SAMPLE_DATA&sfSchema=TPCDS_SF10TCL')
    config = observation_settings.to_feature_config()
    expected_config = """            
            observationPath:"snowflake://snowflake_account/?dbtable=CALL_CENTER&sfDatabase=SNOWFLAKE_SAMPLE_DATA&sfSchema=TPCDS_SF10TCL"
        """
    assert ''.join(config.split()) == ''.join(expected_config.split())