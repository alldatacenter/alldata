"""
Queue storage module that uses SQLAlchemy to access queue information and frames in a database.


"""
import threading
import logging
import os
import os.path
import shelve
from collections import deque
from datetime import datetime, timedelta

# try:
#     from configparser import ConfigParser
# except ImportError:
#     from ConfigParser import ConfigParser

from sqlalchemy import engine_from_config, MetaData
from sqlalchemy.orm import scoped_session, sessionmaker
from sqlalchemy.sql import select, func, distinct

from coilmq.store import QueueStore
from coilmq.config import config
from coilmq.exception import ConfigError
from coilmq.util.concurrency import synchronized
from coilmq.store.sa import meta, model

__authors__ = ['"Hans Lellelid" <hans@xmpl.org>']
__copyright__ = "Copyright 2009 Hans Lellelid"
__license__ = """Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""


def make_sa():
    """
    Factory to creates a SQLAlchemy queue store, pulling config values from the CoilMQ configuration.
    """
    configuration = dict(config.items('coilmq'))
    engine = engine_from_config(configuration, 'qstore.sqlalchemy.')
    init_model(engine)
    store = SAQueue()
    return store


def init_model(engine, create=True, drop=False):
    """
    Initializes the shared SQLAlchemy state in the L{coilmq.store.sa.model} module.

    @param engine: The SQLAlchemy engine instance.
    @type engine: C{sqlalchemy.Engine}

    @param create: Whether to create the tables (if they do not exist).
    @type create: C{bool}

    @param drop: Whether to drop the tables (if they exist).
    @type drop: C{bool}
    """
    meta.engine = engine
    meta.metadata = MetaData(bind=meta.engine)
    meta.Session = scoped_session(sessionmaker(bind=meta.engine))
    model.setup_tables(create=create, drop=drop)


class SAQueue(QueueStore):
    """
    A QueueStore implementation that stores messages in a database and uses SQLAlchemy to interface
    with the database.

    Note that this implementation does not actually use the ORM capabilities of SQLAlchemy, but simply
    uses SQLAlchemy for the DB abstraction for SQL building and DDL (table creation).

    This L{coilmq.store.sa.model.setup_tables} function is used to actually define (& create) the 
    database tables.  This class also depends on the L{init_model} method have been called to 
    define the L{coilmq.store.sa.model.Session} class-like callable (and the engine & metadata).

    Finally, this class does not explicitly use shared data (db connections); a new Session is created
    in each method.  The actual implementation is handled using SQLAlchemy scoped sessions, which provide
    thread-local Session class-like callables. As a result of deferring that to the SA layer, we don't 
    need to use synchronization locks to guard calls to the methods in this store implementation.
    """

    def enqueue(self, destination, frame):
        """
        Store message (frame) for specified destinationination.

        @param destination: The destinationination queue name for this message (frame).
        @type destination: C{str}

        @param frame: The message (frame) to send to specified destinationination.
        @type frame: C{stompclient.frame.Frame}
        """
        session = meta.Session()
        message_id = frame.headers.get('message-id')
        if not message_id:
            raise ValueError("Cannot queue a frame without message-id set.")
        ins = model.frames_table.insert().values(
            message_id=message_id, destination=destination, frame=frame)
        session.execute(ins)
        session.commit()

    def dequeue(self, destination):
        """
        Removes and returns an item from the queue (or C{None} if no items in queue).

        @param destination: The queue name (destinationination).
        @type destination: C{str}

        @return: The first frame in the specified queue, or C{None} if there are none.
        @rtype: C{stompclient.frame.Frame} 
        """
        session = meta.Session()

        try:

            selstmt = select(
                [model.frames_table.c.message_id, model.frames_table.c.frame])
            selstmt = selstmt.where(
                model.frames_table.c.destination == destination)
            selstmt = selstmt.order_by(
                model.frames_table.c.queued, model.frames_table.c.sequence)

            result = session.execute(selstmt)

            first = result.fetchone()
            if not first:
                return None

            delstmt = model.frames_table.delete().where(model.frames_table.c.message_id ==
                                                        first[model.frames_table.c.message_id])
            session.execute(delstmt)

            frame = first[model.frames_table.c.frame]

        except:
            session.rollback()
            raise
        else:
            session.commit()
            return frame

    def has_frames(self, destination):
        """
        Whether specified queue has any frames.

        @param destination: The queue name (destinationination).
        @type destination: C{str}

        @return: Whether there are any frames in the specified queue.
        @rtype: C{bool}
        """
        session = meta.Session()
        sel = select([model.frames_table.c.message_id]).where(
            model.frames_table.c.destination == destination)
        result = session.execute(sel)

        first = result.fetchone()
        return first is not None

    def size(self, destination):
        """
        Size of the queue for specified destination.

        @param destination: The queue destination (e.g. /queue/foo)
        @type destination: C{str}

        @return: The number of frames in specified queue.
        @rtype: C{int}
        """
        session = meta.Session()
        sel = select([func.count(model.frames_table.c.message_id)]).where(
            model.frames_table.c.destination == destination)
        result = session.execute(sel)
        first = result.fetchone()
        if not first:
            return 0
        else:
            return int(first[0])

    def destinations(self):
        """
        Provides a list of destinations (queue "addresses") available.

        @return: A list of the detinations available.
        @rtype: C{set}
        """
        session = meta.Session()
        sel = select([distinct(model.frames_table.c.destination)])
        result = session.execute(sel)
        return set([r[0] for r in result.fetchall()])

    def close(self):
        """
        Closes the databases, freeing any resources (and flushing any unsaved changes to disk).
        """
        meta.Session.remove()
