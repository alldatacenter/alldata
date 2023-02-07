from crawlab.constants import DedupMethod
from legacy.db import index_item
from legacy.db.kafka import send_msg
from legacy.db import get_col
from legacy.db.sql import insert_item, get_item, update_item
from crawlab.utils.config import get_task_id, get_is_dedup, get_dedup_field, get_dedup_method


def save_item_mongo(item):
    col = get_col()

    # 赋值task_id
    item['task_id'] = get_task_id()

    # 是否开启去重
    is_dedup = get_is_dedup()

    if is_dedup == '1':
        # 去重
        dedup_field = get_dedup_field()
        dedup_method = get_dedup_method()
        if dedup_method == DedupMethod.OVERWRITE:
            # 覆盖
            if col.find_one({dedup_field: item[dedup_field]}):
                col.replace_one({dedup_field: item[dedup_field]}, item)
            else:
                col.save(item)
        elif dedup_method == DedupMethod.IGNORE:
            # 忽略
            col.save(item)
        else:
            # 其他
            col.save(item)
    else:
        # 不去重
        col.save(item)


def save_item_sql(item):
    # 是否开启去重
    is_dedup = get_is_dedup()

    # 赋值task_id
    item['task_id'] = get_task_id()

    if is_dedup == '1':
        # 去重
        dedup_field = get_dedup_field()
        dedup_method = get_dedup_method()
        if dedup_method == DedupMethod.OVERWRITE:
            # 覆盖
            if get_item(item, dedup_field):
                update_item(item, dedup_field)
            else:
                insert_item(item)
        elif dedup_method == DedupMethod.IGNORE:
            # 忽略
            insert_item(item)
        else:
            # 其他
            insert_item(item)
    else:
        # 不去重
        insert_item(item)


def save_item_kafka(item):
    item['task_id'] = get_task_id()
    send_msg(item)


def save_item_es(item):
    item['task_id'] = get_task_id()
    index_item(item)
