from abc import ABC, abstractmethod
from contextlib import contextmanager
import logging
import threading
import os
import pymssql

# TODO: refactor common utils in registry folder
# This is a copy of database.py from sql-registry folder 
# And add a `update` function to execute insert and set commands

providers = []


class DbConnection(ABC):
    @abstractmethod
    def query(self, sql: str, *args, **kwargs) -> list[dict]:
        pass


def quote(id):
    if isinstance(id, str):
        return f"'{id}'"
    else:
        return ",".join([f"'{i}'" for i in id])


def parse_conn_str(s: str) -> dict:
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


class MssqlConnection(DbConnection):
    @staticmethod
    def connect(autocommit=True):
        conn_str = os.environ["RBAC_CONNECTION_STR"]
        if "Server=" not in conn_str:
            logging.debug(
                "`RBAC_CONNECTION_STR` is not in ADO connection string format")
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

    def query(self, sql: str, *args, **kwargs) -> list[dict]:
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

    def update(self, sql: str, *args, **kwargs):
        retry = 0
        while True:
            try:
                with self.mutex:
                    c = self.conn.cursor(as_dict=True)
                    c.execute(sql, *args, **kwargs)
                    self.conn.commit()
                    return True
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


providers.append(MssqlConnection)


def connect(*args, **kargs):
    for p in providers:
        ret = p.connect(*args, **kargs)
        if ret is not None:
            return ret
    raise RuntimeError("Cannot connect to database")
