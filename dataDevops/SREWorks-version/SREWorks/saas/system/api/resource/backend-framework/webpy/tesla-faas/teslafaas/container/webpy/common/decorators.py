# coding: utf-8
"""
Base Decorators
"""
import web
import time
import hashlib
import logging
import msgpack
import inspect
from undecorated import undecorated

from functools import wraps


from teslafaas.common.decorators import exception_wrapper
from teslafaas.common.exceptions import ArgError,ActionError
from teslafaas.common.jsonify import *
from teslafaas.common.return_code import *
from . import urls as app_route_urls
from teslafaas.common.trace_id import get_upstream_trace

logger = logging.getLogger(__name__)


# urls mapping used by webpy
def app_route(route):
    """
        decorator to associate http handler class with uri
    """

    def f(cls):
        app_route_urls.append(route)
        app_route_urls.append(cls)
        return cls

    return f


def cached(keys, ttl, *args, **kwargs):
    def _deco(func):
        def __deco(*args, **kwargs):
            if web.ctx.method == 'GET':
                r = web.ctx.factory.get_redis_conn_wrapper()
                url_path = web.ctx.path
                if keys is None:
                    hashed_key = hashlib.md5(web.ctx.fullpath).hexdigest()
                else:
                    if len(set(keys).difference(set(args[1].keys()))) == 0:
                        hash_str_list = [web.ctx.path]
                        for key in keys:
                            hash_str_list.append(args[1][key])
                        hashed_key = hashlib.md5(''.join(hash_str_list)).hexdigest()
                    else:
                        return func(*args, **kwargs)

                # check if hashed_key cached
                try:
                    data = json.loads(r.hget(url_path, hashed_key))
                except Exception as e:
                    logger.info(e.message)
                    data = None

                try:
                    if data is None:
                        data = func(*args, **kwargs)
                        r.hset(url_path, hashed_key, json.dumps(data))
                        r.expire(url_path, ttl)
                except Exception as e:
                    logger.error(e.message)
                    return func(*args, **kwargs)

                return data

        return __deco

    return _deco


def clear_redis_cache(fn):
    fn = undecorated(fn)
    filename = fn_name_to_key(fn)
    func_name = "*_faas_function-" + filename
    r = web.ctx.factory.get_redis_conn_wrapper()
    if r is None:
        logger.info("There is no redis, skip clear redis cache")
        return True
    try:
        for key in r.scan_iter(match=func_name):
            r.expire(key, 0)
            logger.info("clear redis key %s succeed" % key)
    except Exception as e:
        logger.error("clear redis key %s failed, errmsg: %s" % (func_name, e.message))


def fcached(ttl, *args, **kwargs):
    def _deco(func):
        def __deco(*args, **kwargs):
            api_product = web.ctx.env.get("HTTP_X_BIZ_APP", "")
            r = web.ctx.factory.get_redis_conn_wrapper()
            if r is None:
                return func(*args, **kwargs)

            func_name = api_product + "_faas_function-" + fn_name_to_key(func)
            hashed_key = hashlib.sha1(msgpack.packb((func_name, args[1:], kwargs), default=str)).hexdigest()


            try:
                data = r.hget(func_name, hashed_key)
            except Exception as e:
                logger.error(str(e))
                return func(*args, **kwargs)

            # check if hashed_key cached
            try:
                if data is None:
                    data = func(*args, **kwargs)
                    r.hset(func_name, hashed_key, msgpack.packb(data, default=custom_encode))
                    r.expire(func_name, ttl)
                else:
                    data = msgpack.unpackb(data, use_list=True)
            except Exception as e:
                logger.error(str(e))
                return func(*args, **kwargs)

            return data

        return __deco

    return _deco

def redislock(key,expire=10,*args, **kwargs):
    '''
    :param key: value is **kwargs[key], if not exist, use key
    :param expire:
    :param args:
    :param kwargs:
    :return:
    '''
    def _deco(func):
        def __deco(*args, **kwargs):
            func_name = fn_name_to_key(func)
            r = web.ctx.factory.get_redis_conn_wrapper()
            lock_key = 'LOCK4_{func_name}_{key}'.format(key=kwargs.get(key,key),func_name=func_name)
            logger.info('redis lock for [%s]' % lock_key)
            data = False
            for i in range(1,3):
                if r.setnx(lock_key, 1) == 1:
                    r.expire(lock_key, 10)
                    try:
                        data = func(*args, **kwargs)
                        return data
                    except:
                        raise
                    finally:
                        r.delete(lock_key)
                time.sleep(1)
            else:
                raise ActionError('can not aquire lock, NOOP')
        return __deco
    return _deco

import threading
cache = dict()
tlocal = threading.local()

def mcache():
    """Memoizing cache decorator.

    Arguments to the cached function must be hashable.

    """
    def decorating_function(func,
                tuple=tuple, sorted=sorted, len=len, KeyError=KeyError):

        @wraps(func)
        def wrapper(*args, **kwargs):
            func_name = fn_name_to_key(func)
            hashed_key = hashlib.sha1(msgpack.packb((func_name, args[1:], kwargs), default=str)).hexdigest()

            if not hasattr(tlocal, hashed_key):
                result = func(*args, **kwargs)
                logging.debug('func: %s missing cache.' % func)
                setattr(tlocal, hashed_key, result)
            else:
                return getattr(tlocal, hashed_key)
            return result

        return wrapper

    return decorating_function


def fn_name_to_key(fn):
    filename = inspect.getsourcefile(fn)
    if not filename:
        return fn.__name__
    else:
        return filename + "." + fn.__name__

def custom_encode(obj):
    if isinstance(obj, Decimal):
        return float(obj)
    if (isinstance(obj, datetime)):
        return obj.isoformat(' ')
    return obj


def login_required(func, *args, **kwargs):
    def __decorator(*args, **kwargs):
        env = web.ctx.env
        auth_user = env.get("HTTP_X_AUTH_USER")
        path_info = env.get("PATH_INFO")
        if not auth_user:
            # ret = dict(code=ERR_API_AUTHFAILED,
            #            data=[],
            #            message="Login required for: '%s'!\n"
            #                    "Add x-auth-user header." % path_info)
            # return jsondumps(ret)
            pass
        ret = func(*args, **kwargs)
        return ret

    return __decorator


def rest_log(func, *args, **kwargs):
    def __decorator(*args, **kwargs):
        env = web.ctx.env
        request_id = get_upstream_trace()
        start_time = int(round(time.time() * 1000))
        try:
            ret = func(*args, **kwargs)
            return ret
        finally:
            end_time = int(round(time.time() * 1000))
            cost_time = end_time - start_time
            logger.info("[API REQUEST] [%s] [%s] [%d]ms" % (request_id, web.ctx.fullpath, cost_time))
    return __decorator
