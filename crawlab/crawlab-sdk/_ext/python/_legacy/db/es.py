import json

import elasticsearch as elasticsearch

from crawlab.utils.config import get_data_source


def get_client() -> elasticsearch.Elasticsearch:
    ds = get_data_source()
    return elasticsearch.Elasticsearch(hosts=[{'host': ds.get('host'), 'port': ds.get('port')}])


def index_item(item):
    ds = get_data_source()
    client = get_client()
    client.index(
        index=ds.get('database'),
        body=item,
    )
