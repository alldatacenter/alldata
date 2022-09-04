import logging
import web.db
from web.db import SQLQuery, SQLParam, DB

# patch db module print to logger
class StreamToLogger(object):
    def write(self, buf):
        ss = ''
        for line in buf.rstrip().splitlines(True):
            ss += line
        if ss:
            logging.debug(ss)


def patch_all(multiInsert=True):
    # patch
    setattr(web.db, 'debug', StreamToLogger())
    if hasattr(web.db, 'config'):
        web.db.config['debug_sql'] = True
    if multiInsert:
        patch_multiInsert()


def patch_multiInsert():
    DB.multiple_insert = multiple_insert


def multiple_insert(self, tablename, values, seqname=None, _test=False):
    """
    Inserts multiple rows into `tablename`. The `values` must be a list of
    dictioanries,
    one for each row to be inserted, each with the same set of keys.
    Returns the list of ids of the inserted rows.
    Set `seqname` to the ID if it's not the default, or to `False`
    if there isn't one.

        >>> db = DB(None, {})
        >>> db.supports_multiple_insert = True
        >>> values = [{"name": "foo", "email": "foo@example.com"}, {"name":
        "bar", "email": "bar@example.com"}]
        >>> db.multiple_insert('person', values=values, _test=True)
        <sql: "INSERT INTO person (name, email) VALUES ('foo',
        'foo@example.com'), ('bar', 'bar@example.com')">
    """
    if not values:
        return []

    if not self.supports_multiple_insert:
        out = [self.insert(tablename, seqname=seqname, _test=_test, **v) for v
               in values]
        if seqname is False:
            return None
        else:
            return out

    keys = values[0].keys()
    # @@ make sure all keys are valid

    # make sure all rows have same keys.
    for v in values:
        if v.keys() != keys:
            raise ValueError, 'Bad data'

    sql_query = SQLQuery(
        'INSERT INTO %s (%s) VALUES ' % (tablename, ', '.join(keys)))

    for i, row in enumerate(values):
        if i != 0:
            sql_query.append(", ")
        SQLQuery.join([SQLParam(row[k]) for k in keys], sep=", ",
                      target=sql_query, prefix="(", suffix=")")

    # modifier: wangxing
    # add this to fix the quote problem
    # if you want to know this in detail, please check db.py: 113 and db.py:
    #  236
    SQLQuery(sql_query.items)

    if _test: return sql_query

    db_cursor = self._db_cursor()
    if seqname is not False:
        sql_query = self._process_insert_query(sql_query, tablename, seqname)

    if isinstance(sql_query, tuple):
        # for some databases, a separate query has to be made to find
        # the id of the inserted row.
        q1, q2 = sql_query
        self._db_execute(db_cursor, q1)
        self._db_execute(db_cursor, q2)
    else:
        self._db_execute(db_cursor, sql_query)

    try:
        out = db_cursor.fetchone()[0]
        out = range(out - len(values) + 1, out + 1)
    except Exception:
        out = None

    if not self.ctx.transactions:
        self.ctx.commit()
    return out
