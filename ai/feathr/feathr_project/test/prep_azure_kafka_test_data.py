import io
import json
from datetime import datetime
import avro.schema
import numpy as np
import pandas as pd
import pytz
from avro.io import BinaryEncoder, DatumWriter
from confluent_kafka import Producer
from feathr.utils._env_config_reader import EnvConfigReader
"""
Produce some sample data for streaming feature using Kafka"""
KAFKA_BROKER = "feathrazureci.servicebus.windows.net:9093"
KAFKA_TOPIC = "feathrcieventhub"

GENERATION_SIZE = 10

def generate_entities():
    return range(GENERATION_SIZE)


def generate_trips(entities):
    df = pd.DataFrame(columns=["driver_id", "trips_today", "datetime", "created"])
    df['driver_id'] = entities
    df['trips_today'] = range(GENERATION_SIZE)
    df['datetime'] = pd.to_datetime(
        np.random.randint(
            datetime(2021, 10, 10).timestamp(),
            datetime(2022, 10, 30).timestamp(),
            size=GENERATION_SIZE),
        unit="s"
    )
    df['created'] = pd.to_datetime(datetime.now())
    return df


def send_avro_record_to_kafka(topic, record):
    value_schema = avro.schema.parse(avro_schema_json)
    writer = DatumWriter(value_schema)
    bytes_writer = io.BytesIO()
    encoder = BinaryEncoder(bytes_writer)
    writer.write(record, encoder)
    env_config = EnvConfigReader(config_path=None)
    sasl = env_config.get_from_env_or_akv('KAFKA_SASL_JAAS_CONFIG')
    conf = {
        'bootstrap.servers': KAFKA_BROKER,
        'security.protocol': 'SASL_SSL',
        'ssl.ca.location': '/usr/local/etc/openssl@1.1/cert.pem',
        'sasl.mechanism': 'PLAIN',
        'sasl.username': '$ConnectionString',
        'sasl.password': '{};EntityPath={}'.format(sasl, topic),
        'client.id': 'python-example-producer'
    }

    producer = Producer({
        **conf
    })
    producer.produce(topic=topic, value=bytes_writer.getvalue())
    producer.flush()

entities = generate_entities()
trips_df = generate_trips(entities)

avro_schema_json = json.dumps({
    "type": "record",
    "name": "DriverTrips",
    "fields": [
        {"name": "driver_id", "type": "long"},
        {"name": "trips_today", "type": "int"},
        {
            "name": "datetime",
            "type": {"type": "long", "logicalType": "timestamp-micros"}
        }
    ]
})

while True:
# This while loop is used to keep the process runinng and producing data stream;
# If no need please remove it
    for record in trips_df.drop(columns=['created']).to_dict('record'):
        record["datetime"] = (
            record["datetime"].to_pydatetime().replace(tzinfo=pytz.utc)
        )
        send_avro_record_to_kafka(topic=KAFKA_TOPIC, record=record)
