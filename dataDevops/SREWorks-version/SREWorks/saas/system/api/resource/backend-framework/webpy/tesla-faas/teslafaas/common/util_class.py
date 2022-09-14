# coding: utf-8
import logging
from collections import deque


class DBRecord(dict):
    """
    Class for creating instance of DBModel.
    """

    def __init__(self, **kw):
        super(DBRecord, self).__init__(**kw)

    def __getattr__(self, key):
        try:
            return self[key]
        except KeyError:
            raise AttributeError(
                r"'DBRecord' object has no attribute '%s'" % key)

    def __setattr__(self, key, value):
        self[key] = value


class Queue(deque):
    """
    Subclass a dequeue to implement a one direction queue.
    """

    def put(self, element):
        self.append(element)

    def length(self):
        return len(self)

    def is_empty(self):
        return self.length() == 0

    def get(self):
        if self.length() == 0:
            return None
        return self.popleft()


class SubList(list):
    """ List that support subtract operation """

    def __init__(self, *args):
        super(SubList, self).__init__(args)

    def __sub__(self, other):
        return self.__class__(*[item for item in self if item not in other])


class StrFormatDict(dict):
    """
    Used for partially formation %()s template string
    """

    def __missing__(self, key):
        return '%%(%s)s' % key


class StreamLogger(object):
    """
    Fake file-like stream object that redirects writes to a logger instance.
    """

    def __init__(self, logger, log_level=logging.INFO):
        self.logger = logger
        self.log_level = log_level

    def write(self, buf):
        # 1.
        # for line in buf.rstrip().splitlines():
        #     self.logger.log(self.log_level, line.rstrip())
        # 2.
        if buf != '\n':
            self.logger.log(self.log_level, buf)


def Enum(*sequential, **named):
    """Self defined Enum type, supports converting values back to names.

    :param sequential: automatically attach number to each entry in sequential.
    :param named: use specified value for each key.
    Attention: key in named will overwrite the same key in sequential.
    """
    enums = dict(zip(sequential, range(len(sequential))), **named)
    reverse = dict((value, key) for key, value in enums.iteritems())
    enums['reverse_mapping'] = reverse
    return type('Enum', (), enums)

