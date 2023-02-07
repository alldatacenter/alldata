import json

from kafka import KafkaProducer

from crawlab.utils.config import get_data_source


def get_producer():
    ds = get_data_source()
    if ds.get('username') is not None and len(ds.get('username')) > 0:
        return KafkaProducer(
            sasl_mechanism="PLAIN",
            security_protocol='SASL_PLAINTEXT',
            sasl_plain_username=ds.get('username'),
            sasl_plain_password=ds.get('password'),
            bootstrap_servers=f'{ds.get("host")}:{ds.get("port")}'
        )
    else:
        return KafkaProducer(
            bootstrap_servers=f'{ds.get("host")}:{ds.get("port")}'
        )


def send_msg(item):
    ds = get_data_source()
    producer = get_producer()
    producer.send(ds.get('database'), json.dumps(item).encode('utf-8'))
