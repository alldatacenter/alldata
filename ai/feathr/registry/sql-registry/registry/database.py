from abc import ABC, abstractmethod
from contextlib import contextmanager
import logging
import threading
import os
from typing import List, Dict

# Checks if the platform is Max (Darwin).
# If so, imports _scproxy that is necessary for pymssql to work on MacOS
import platform
if platform.system().lower().startswith('dar'):
    import _scproxy

import pymssql


providers = []

class DbConnection(ABC):
    @abstractmethod
    def query(self, sql: str, *args, **kwargs) -> List[Dict]:
        pass

# already has one in 'db_registry.py'; shall we remove it?
'''
def quote(id):
    if isinstance(id, str):
        return f"'{id}'"
    else:
        return ",".join([f"'{i}'" for i in id])
'''

def parse_conn_str(s: str) -> Dict:
    """
    TODO: Not a sound and safe implementation, but useful enough in this case
    as the connection string is provided by users themselves.
    """
    parts = dict([p.strip().split("=", 1)
                 for p in s.split(";") if len(p.strip()) > 0])
    server = parts["Server"].split(":")[1].split(",")[0]
    return {
        "host": server,
        "database": parts["Initial Catalog"],
        "user": parts["User ID"],
        "password": parts["Password"],
        # "charset": "utf-8",   ## For unknown reason this causes connection failure
    }


import sqlite3

class SQLiteConnection(DbConnection):
    @staticmethod
    def connect(autocommit = True):
        # This is just to implement the abstract method. It's usually not used.
        return SQLiteConnection()

    def __init__(self):
        self.make_connection()
        self.mutex = threading.Lock()


    def make_connection(self):
        # use ` check_same_thread=False` otherwise an error like 
        # sqlite3.ProgrammingError: SQLite objects created in a thread can only be used in that same thread. The object was created in thread id 140309046605632 and this is thread id 140308968896064.
        # will be thrown out
        # Use the mem just to make sure it can connect. The actual file path will be initialized in the db_registry.py file
        self.conn = sqlite3.connect("file::memory:?cache=shared", uri=True, check_same_thread=False)

    def query(self, sql: str, *args, **kwargs) -> List[Dict]:
        # this is just to implement the abstract method.
        pass

    @contextmanager
    def transaction(self):
        """
        Start a transaction so we can run multiple SQL in one batch.
        User should use `with` with the returned value, look into db_registry.py for more real usage.

        NOTE: `self.query` and `self.execute` will use a different MSSQL connection so any change made
        in this transaction will *not* be visible in these calls.

        The minimal implementation could look like this if the underlying engine doesn't support transaction.
        ```
        @contextmanager
        def transaction(self):
            try:
                c = self.create_or_get_connection(...)
                yield c
            finally:
                c.close(...)
        ```
        """
        conn = None
        cursor = None
        try:
            # As one MssqlConnection has only one connection, we need to create a new one to disable `autocommit`
            conn = SQLiteConnection.connect().conn
            cursor = conn.cursor()
            yield cursor
        except Exception as e:
            logging.warning(f"Exception: {e}")
            if conn:
                conn.rollback()
            raise e
        finally:
            if conn:
                conn.commit()


class MssqlConnection(DbConnection):
    @staticmethod
    def connect(autocommit = True):
        conn_str = os.environ["CONNECTION_STR"]
        if "Server=" not in conn_str:
            logging.debug("`CONNECTION_STR` is not in ADO connection string format")
            return None
        params = parse_conn_str(conn_str)
        if not autocommit:
            params["autocommit"] = False
        return MssqlConnection(params)

    def __init__(self, params):
        self.params = params
        self.make_connection()
        self.mutex = threading.Lock()

    def make_connection(self):
        self.conn = pymssql.connect(**self.params)

    def query(self, sql: str, *args, **kwargs) -> List[Dict]:
        """
        Make SQL query and return result
        """
        logging.debug(f"SQL: `{sql}`")
        # NOTE: Only one cursor is allowed at the same time
        retry = 0
        while True:
            try:
                with self.mutex:
                    c = self.conn.cursor(as_dict=True)
                    c.execute(sql, *args, **kwargs)
                    return c.fetchall()
            except pymssql.OperationalError:
                logging.warning("Database error, retrying...")
                # Reconnect
                self.make_connection()
                retry += 1
                if retry >= 3:
                    # Stop retrying
                    raise
                pass

    @contextmanager
    def transaction(self):
        """
        Start a transaction so we can run multiple SQL in one batch.
        User should use `with` with the returned value, look into db_registry.py for more real usage.

        NOTE: `self.query` and `self.execute` will use a different MSSQL connection so any change made
        in this transaction will *not* be visible in these calls.

        The minimal implementation could look like this if the underlying engine doesn't support transaction.
        ```
        @contextmanager
        def transaction(self):
            try:
                c = self.create_or_get_connection(...)
                yield c
            finally:
                c.close(...)
        ```
        """
        conn = None
        cursor = None
        try:
            # As one MssqlConnection has only one connection, we need to create a new one to disable `autocommit`
            conn = MssqlConnection.connect(autocommit=False).conn
            cursor = conn.cursor(as_dict=True)
            yield cursor
        except Exception as e:
            logging.warning(f"Exception: {e}")
            if conn:
                conn.rollback()
            raise e
        finally:
            if conn:
                conn.commit()


# This is ordered list. So append SQLite first
if os.environ.get("FEATHR_SANDBOX"):
    providers.append(SQLiteConnection)
providers.append(MssqlConnection)


def connect(*args, **kargs):
    for p in providers:
        ret = p.connect(*args, **kargs)
        if ret is not None:
            return ret
    raise RuntimeError("Cannot connect to database")
