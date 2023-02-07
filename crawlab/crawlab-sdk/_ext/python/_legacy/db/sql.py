import psycopg2
import pymysql

from crawlab.constants import DataSourceType
from crawlab.utils.config import get_data_source, get_data_source_type, get_collection

conn_cache = {}


def get_conn():
    ds_type = get_data_source_type()
    if conn_cache.get(ds_type) is None:
        if ds_type == DataSourceType.MYSQL:
            # MySQL
            conn_cache[ds_type] = connect_mysql()
        elif ds_type == DataSourceType.POSTGRES:
            # Postgres
            conn_cache[ds_type] = connect_postgres()
    return conn_cache.get(ds_type)


def connect_mysql():
    ds = get_data_source()
    return pymysql.connect(
        host=ds.get('host') or 'localhost',
        port=int(ds.get('port')) if ds.get('port') is not None else 3306,
        user=ds.get('username') or None,
        password=ds.get('password') or None,
        database=ds.get('database') or 'test',
        charset='utf8'
    )


def connect_postgres():
    ds = get_data_source()
    return psycopg2.connect(
        host=ds.get('host') or 'localhost',
        port=ds.get('port') or '5432',
        user=ds.get('username') or None,
        password=ds.get('password') or None,
        database=ds.get('database') or 'test',
    )


def _insert_item(item: dict):
    conn = get_conn()
    columns = item.keys()
    table_name = get_collection()
    if table_name is None:
        raise Exception('table_name is empty')
    sql_str = f'INSERT INTO {table_name}({",".join(columns)}) VALUES ({",".join(["%s" for _ in columns])});'
    cursor = conn.cursor()
    cursor.execute(sql_str, list(item.values()))
    conn.commit()
    cursor.close()


def insert_item_mysql(item: dict):
    _insert_item(item)


def insert_item_postgres(item: dict):
    _insert_item(item)


def insert_item_sqlserver(item: dict):
    raise NotImplementedError('sqlserver is not implemented')


def insert_item(item: dict):
    ds_type = get_data_source_type()
    if ds_type == DataSourceType.MYSQL:
        insert_item_mysql(item)
    elif ds_type == DataSourceType.POSTGRES:
        insert_item_postgres(item)
    elif ds_type == DataSourceType.SQLSERVER:
        insert_item_sqlserver(item)
    else:
        raise NotImplementedError(f'{ds_type} is not implemented')


def _update_item(item: dict, dedup_field: str):
    conn = get_conn()
    table_name = get_collection()
    if table_name is None:
        raise Exception('table_name is empty')
    update_str = ','.join([f'{k}=\'{v}\'' for k, v in item.items()])
    sql_str = f'UPDATE {table_name} SET {update_str} WHERE {dedup_field} = \'{item[dedup_field]}\';'
    cursor = conn.cursor()
    cursor.execute(sql_str)
    conn.commit()
    cursor.close()


def update_item_mysql(item: dict, dedup_field: str):
    _update_item(item, dedup_field)


def update_item_postgres(item: dict, dedup_field: str):
    _update_item(item, dedup_field)


def update_item_sqlserver(item: dict, dedup_field: str):
    raise NotImplementedError('sqlserver is not implemented')


def update_item(item: dict, dedup_field: str):
    ds_type = get_data_source_type()
    if ds_type == DataSourceType.MYSQL:
        update_item_mysql(item, dedup_field)
    elif ds_type == DataSourceType.POSTGRES:
        update_item_postgres(item, dedup_field)
    elif ds_type == DataSourceType.SQLSERVER:
        update_item_sqlserver(item, dedup_field)
    else:
        raise NotImplementedError(f'{ds_type} is not implemented')


def _get_item(key: str, value: str) -> dict:
    conn = get_conn()
    table_name = get_collection()
    if table_name is None:
        raise Exception('table_name is empty')
    sql_str = f'SELECT * FROM {table_name} WHERE {key} = \'{value}\'';
    cursor = conn.cursor()
    cursor.execute(sql_str)
    conn.commit()
    res = cursor.fetchone()
    cursor.close()
    return res


def get_item_mysql(key: str, value: str) -> dict:
    return _get_item(key, value)


def get_item_postgres(key: str, value: str) -> dict:
    return _get_item(key, value)


def get_item_sqlserver(key: str, value: str) -> dict:
    raise NotImplementedError('sqlserver is not implemented')


def get_item(item: dict, dedup_field: str) -> dict:
    ds_type = get_data_source_type()
    key = dedup_field
    value = item[dedup_field]
    if ds_type == DataSourceType.MYSQL:
        return get_item_mysql(key, value)
    elif ds_type == DataSourceType.POSTGRES:
        return get_item_postgres(key, value)
    elif ds_type == DataSourceType.SQLSERVER:
        return get_item_sqlserver(key, value)
    else:
        raise NotImplementedError(f'{ds_type} is not implemented')
