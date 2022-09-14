#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'adonis'

#pylint: disable=no-member

import math
import re

import MySQLdb
from web.db import sqlquote

from teslafaas.common.util_func import *

PAGE_INDEX = 1
PAGE_SIZE = 20


def escape_sql_value(v):
    if isinstance(v, long):
        # sqlquote(10) will be '10L', additional 'L' causing error.
        return str(v)
    elif isinstance(v, bool):
        # True will be 't', causing error.
        return str(v)
    elif isinstance(v, basestring):
        escaped_v = MySQLdb.escape_string(safe_str(v))
        return "'%s'" % escaped_v
    else:
        return sqlquote(v)


def get_sql_query_item(k, v):
    k = safe_str(k)
    # Note: '= NULL' is different with 'IS NULL', do not use '= NULL'
    # MySQL: NULL cannot be equal to anything, including itself
    # (ie: NULL = NULL is always false).
    if v is None:
        sql_item = k + ' IS NULL'
    elif isinstance(v, (tuple,)) and len(v) == 2:  # Between/<=/>=
        v_ = [escape_sql_value(v_value) for v_value in v]
        if v[0] is not None and v[1] is not None:
            sql_item = k + ' BETWEEN ' + v_[0] + ' AND ' + v_[1]
        elif v[0] is None and v[1] is not None:
            sql_item = k + ' <= ' + v_[1]
        elif v[0] is not None and v[1] is None:
            sql_item = k + ' >= ' + v_[0]
        else:
            print "WARNING: get a empty 2-item tuple as SQL query value" \
                  "(%s=%s), will use '%s IS NOT NULL' instead!" \
                  % (k, v, k)
            sql_item = k + ' IS NOT NULL'  # ???
    elif isinstance(v, (tuple, list, set)):
        if not v:
            # 'id IN ()' will cause sql error, use 1=0 instead
            print "WARNING: get a empty %s as sql query arg value " \
                  "(%s=%s), will use '1=0' as the sql query condition" \
                  % (type(v), k, v)
            return '1=0'
        v_ = [escape_sql_value(v_value) for v_value in v]
        v_str = map(lambda x: str(x), v_)
        sql_item_value = "(%s)" % ', '.join(v_str)
        sql_item = k + ' IN ' + sql_item_value
    else:
        sql_item = k + ' = ' + escape_sql_value(v)
    return sql_item


def get_sql_where(and_rel=True, negative=False, **kwargs):
    """
    Produce SQL where clauses statement from kwargs.
    Notes: can not put NON-ASC char in list/tuple

    web.db.where() doesn't support "WHERE a IN (1, 2, 3) BETWEEN/>=/<=",
    add supporting for this
    :param and_rel: True, relation between kwargs are AND, else OR.
    :param negative: True: add NOT to negate query condition.
    :param kwargs: two item tuple for SQL 'between/<=/>=' select
                    (1, 2)/(None, 2)/(1, None) for 'BETWEEN/<=2/>=1'
                   list/non-2-item-tuple for SQL 'IN' select
                   others types will be SQL '=' select
    :return: string , "arg1=1 AND arg2 IN (1,2) AND args BETWEEN 1 AND 2"
    """
    clauses = [get_sql_query_item(k, v) for k, v in kwargs.iteritems()]
    if not clauses:
        return None
    join_word = ' AND ' if and_rel else ' OR '
    clauses = map(lambda x: safe_str(x), clauses)
    where_str = join_word.join(clauses)
    if negative:
        where_str = " NOT (%s)" % where_str
    return where_str


def get_page_sql(sql, pk='id', page_index=PAGE_INDEX, page_size=PAGE_SIZE):
    """
    Produce SQL statement for pagination.

    Page index should begin from 1, add ORDER BY statement if necessary.
    """
    # LIMIT is only allowed to appear after the ORDER BY
    sql_order = '' if re.search('order by', sql, re.IGNORECASE) \
        else ' ORDER BY %s ' % pk
    if page_index <= 0:
        page_index = PAGE_INDEX
    if page_size <= 0:
        page_size = PAGE_SIZE
    sql_page = " LIMIT %d OFFSET %d" % (page_size, (page_index - 1) * page_size)
    return sql + sql_order + sql_page


def get_page_sql_desc(sql,
                      pk='id',
                      page_index=PAGE_INDEX,
                      page_size=PAGE_SIZE):
    """
    Produce SQL statement for pagination.

    Page index should begin from 1, add ORDER BY statement if necessary.
    """
    # LIMIT is only allowed to appear after the ORDER BY
    sql_order = '' if re.search('order by', sql, re.IGNORECASE) \
        else ' ORDER BY %s desc' % pk
    if page_index <= 0:
        page_index = PAGE_INDEX
    if page_size <= 0:
        page_size = PAGE_SIZE
    sql_page = " LIMIT %d OFFSET %d" % (page_size, (page_index - 1) * page_size)
    return sql + sql_order + sql_page


def get_total_info(db, sql, limit=PAGE_SIZE):
    """ Get total number of records and pages. """
    # cnt_sql = sql.replace('SELECT *', 'SELECT COUNT(*) AS cnt')
    # Add consideration for non upper case SQL statement.
    cnt_sql = re.sub(
        'SELECT .* FROM', 'SELECT COUNT(*) AS cnt FROM',
        sql, flags=re.IGNORECASE)
    cnt_res = int(db.query(cnt_sql)[0].cnt)
    total_pages = int(math.ceil(cnt_res / float(limit)))
    return cnt_res, total_pages
