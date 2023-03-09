import os
from pathlib import Path
import pytest
from feathr import MaterializationSettings, RedisSink, AvroJsonSchema, KafkaConfig, KafKaSource
from test_fixture import kafka_test_setup
from test_utils.constants import Constants

def test_feathr_kafka_configs():
    schema = AvroJsonSchema(schemaStr="""
    {
        "type": "record",
        "schema_name": "DriverTrips",
        "fields": [
            {"name": "driver_id", "type": "long"},
        ]
    }
    """)
    stream_source = KafKaSource(name="kafkaStreamingSource",
                              kafkaConfig=KafkaConfig(brokers=["feathrazureci.servicebus.windows.net:9093"],
                                                      topics=["feathrcieventhub"],
                                                      schema=schema))
    config_list = stream_source.to_feature_config().split()
    config = ''.join([conf.replace('\\"', '').strip('\\n') for conf in config_list])
    expected_config = """
    kafkaStreamingSource: {
    type: KAFKA
    config: {
        brokers: ["feathrazureci.servicebus.windows.net:9093"]
        topics: [feathrcieventhub]
        
        schema: {
            type = "avro"
            avroJson:"{
                type: record,
                schema_name: DriverTrips,
                fields: [{
                    name: driver_id, 
                    type: long
                    },]
            }"
        }  
    }
    }
    """
    assert config == "".join(expected_config.split())
    
@pytest.mark.skipif(os.environ.get('SPARK_CONFIG__SPARK_CLUSTER') != "azure_synapse",
                    reason="skip for databricks, as it cannot stop streaming job automatically for now.")
def test_feathr_kafa_streaming_features():
    """
    Test FeathrClient() materialize_features can ingest streaming feature correctly
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client = kafka_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    redisSink = RedisSink(table_name="kafkaSampleDemoFeature", streaming=True, streamingTimeoutMs=10000)
    settings = MaterializationSettings(name="kafkaSampleDemo",
                                   sinks=[redisSink],
                                   feature_names=['f_modified_streaming_count']
                                   )
    client.materialize_features(settings, allow_materialize_non_agg_feature=True)
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)
