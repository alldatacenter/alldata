# coding: utf-8
import codecs
import errno
import os
import shutil
import string
import sys
import threading
import time
import types
import datetime
from itertools import chain
from random import seed, choice, sample, shuffle

import web
from web.utils import safestr

from jsonify import jsonloads

ERROR_FLAG = "- ERROR -"


def is_id(pk):
    """
    Return True if pk is valid DB id type.
    Used for record or pk type judgement.
    """
    return isinstance(pk, (basestring, int, long, float))


def is_container(entry):
    """
    Return True if entry is valid container type.
    """
    return isinstance(entry, (list, set, tuple, dict))


def is_ascii(s):
    """ Check if s is all consist of ASCii chars. """
    return all(ord(c) < 128 for c in s)


def toint(o):
    if o:
        if isinstance(o, bool):
            return int(o)
        s = str(o)
        if s.isdigit():
            return int(s)
    return 0


def unicode_to_str(*args):
    """
    Change unicode to string.

    Return a list.
    """

    def convert(arg_):
        return arg_.encode("utf-8") \
            if isinstance(arg_, types.UnicodeType) else arg_

    return map(convert, args)


def str_to_unicode(*args):
    """
    Change string to unicode.
    """

    def convert(arg_):
        return arg_.decode("utf-8") \
            if isinstance(arg_, types.StringType) else arg_

    return map(convert, args)


def asynchronize(func, *args, **kwargs):
    """
    Run a long-running function in a separate background thread, to make it
    run asynchronously.
    """
    thread_name = kwargs.get('thread_name', None)
    t = threading.Thread(target=func, name=thread_name, args=args,
                         kwargs=kwargs)
    t.setDaemon(True)  # Do not block current thread.
    t.start()
    return t.ident


def get_tesla_conf():
    if 'tesla' in web.ctx:
        return web.ctx.tesla
    else:
        return jsonloads(os.environ['TESLA'])['OPTIONS']


def is_test_env():
    tesla_conf = get_tesla_conf()
    if 'ENV' not in tesla_conf or 'env' not in tesla_conf["ENV"] \
            or tesla_conf['ENV']['env'] == 'test':
        test_env = True
    else:
        test_env = False
    return test_env


def save_file(dest, file_content=''):
    """
    Save file content to dest, and make dest dir if necessary.

    :param dest: absolute path with file name.
    :param file_content: file content to be saved to dest file.
    """
    dest_dir = os.path.dirname(dest)
    if not os.path.exists(dest_dir):
        try:
            os.makedirs(dest_dir)
        except OSError as os_error:
            if os_error.errno != errno.EEXIST:
                raise
    with codecs.open(dest, mode='w', encoding='utf-8') as f_obj:
        f_obj.write(file_content)


def copy_file(src, dest, file_content=''):
    """
    Copy file src to dest, and make dirs for dest if necessary.

    :param src: absolute path with file name.
    :param dest: absolute path with file name.
    :param file_content: if not empty, copy file_content to dest.
    :return: None
    """
    dest_dir = os.path.dirname(dest)
    if not os.path.exists(dest_dir):
        try:
            os.makedirs(dest_dir)
        except OSError as os_error:
            if os_error.errno != errno.EEXIST:
                raise
    if src:
        shutil.copyfile(src, dest)
    else:
        if not file_content:
            file_content = ''  # FileStore may return None for empty file.
        with codecs.open(dest, mode='w', encoding='utf-8') as f_obj:
            f_obj.write(file_content)


def safe_str(obj, encoding='utf-8'):
    r"""
    Copy web.util, for usage without webpy installed

    Converts any given object to utf-8 encoded string.

        >>> safestr('hello')
        'hello'
        >>> safestr(u'\u1234')
        '\xe1\x88\xb4'
        >>> safestr(2)
        '2'
    """
    if isinstance(obj, unicode):
        return obj.encode(encoding)
    elif isinstance(obj, str):
        return obj
    else:
        return str(obj)


def safe_unicode(obj, encoding='utf-8'):
    r"""
    Converts any given object to utf-8 decoded unicode.
    """
    if isinstance(obj, unicode):
        return obj
    elif isinstance(obj, str):
        return obj.decode(encoding=encoding)
    else:
        return unicode(obj)


def is_integer(s):
    """ Check if s is a valid integer. """
    try:
        int(s)
    except ValueError:
        return False
    else:
        return True


def get_now_str(time_fomat="%Y-%m-%d %H:%M:%S"):
    return time.strftime(time_fomat, time.localtime(time.time()))


get_cur_time_ms = lambda: int(round(time.time() * 1000))


def print_error(error_mesg):
    print >> sys.stderr, get_now_str(), ERROR_FLAG, error_mesg


def create_dir(file_dir):
    if not os.path.exists(file_dir):
        try:
            os.makedirs(file_dir)
        except OSError as os_error:
            if os_error.errno != errno.EEXIST:
                raise


def get_api_hash(key1, key2):
    import time
    import hashlib
    key = "%(key_1)s%(local_time)s%(key_2)s" % {
        'key_1': key1,
        'local_time': time.strftime('%Y%m%d', time.localtime(time.time())),
        'key_2': key2}
    m = hashlib.md5()
    m.update(key)
    return m.hexdigest()


def unixtime_to_datetime(utime, fmt='%Y-%m-%d %H:%M:%S'):
    return datetime.datetime.fromtimestamp(int(utime)).strftime(fmt)


def datetime_to_unixtime(dtime, fmt='%Y-%m-%d %H:%M:%S'):
    return int(time.mktime(datetime.datetime.strptime(dtime, fmt).timetuple()))
