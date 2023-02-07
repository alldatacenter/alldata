import json
import os


def get_task_id():
    return os.environ.get('CRAWLAB_TASK_ID')


def get_is_dedup():
    return os.environ.get('CRAWLAB_IS_DEDUP')


def get_dedup_field():
    return os.environ.get('CRAWLAB_DEDUP_FIELD')


def get_dedup_method():
    return os.environ.get('CRAWLAB_DEDUP_METHOD')


def get_collection():
    return os.environ.get('CRAWLAB_COLLECTION')


def get_data_source_type():
    ds = get_data_source()
    return ds.get('type') or 'mongo'


def get_data_source():
    if os.environ.get('CRAWLAB_DATA_SOURCE') is None:
        return {}
    try:
        ds = json.loads(os.environ.get('CRAWLAB_DATA_SOURCE'))
        return ds
    except:
        return {}
