# coding: utf-8
"""
Handler Base
"""
import copy
import datetime
import json
import logging
import os
import re
import subprocess
import time
import traceback

import web
from web.webapi import HTTPError

import teslafaas.common.return_code as CODE
from BaseModel import CrudModel, BaseModel
from teslafaas.container.webpy.common.decorators import exception_wrapper, login_required
from teslafaas.common.exceptions import ArgError, TeslaBaseException, FeArgError
from teslafaas.common.jsonify import jsondumps as jds
from teslafaas.common.jsonify import jsonloads as jls
from teslafaas.common.util_db_func import PAGE_SIZE
from teslafaas.common.util_func import toint
from teslafaas.container.webpy.common.logging_requestid import tlocal
from teslafaas.common.trace_id import get_upstream_trace, set_request_id


class RestHandler(object):
    def __init__(self):
        set_request_id()
        self.request_id = get_upstream_trace()
        self.cors_wl_process()
        # Add a copy of context value in case of new coroutine
        self.server = web.ctx.server
        self.tesla_context = web.ctx.tesla
        # 兼容老code
        self.tesla_ctx = web.ctx.tesla
        self.config = self.tesla_ctx.config
        self.factory = web.ctx.factory
        self.db = self.tesla_ctx.db
        self.dbs = self.tesla_ctx.dbs
        self.dm = self.tesla_ctx.dm
        self.r = self.factory.get_redis_conn_wrapper()
        if 'env' not in web.ctx.tesla or \
                web.ctx.tesla['env'] == 'test':
            self.test_env = True
        else:
            self.test_env = False
        self.params = self.input()
        self.body = web.data()
        self.message = ""
        self.user_name = ''
        self.user_id = 0
        self.logger = logging.getLogger(__name__)
        self.user_name = web.ctx.env.get("HTTP_X_AUTH_USER") if web.ctx.env.get(
            "HTTP_X_AUTH_USER") and "" != web.ctx.env.get("HTTP_X_AUTH_USER") else "unkown"
        self.from_empid = web.ctx.env.get("HTTP_X_FROM_EMPID") if web.ctx.env.get(
            "HTTP_X_FROM_EMPID") and "" != web.ctx.env.get("HTTP_X_FROM_EMPID") else "unknown"
        self.from_auth_user = web.ctx.env.get("HTTP_X_FROM_AUTH_USER") if web.ctx.env.get(
            "HTTP_X_FROM_AUTH_USER") and "" != web.ctx.env.get("HTTP_X_FROM_AUTH_USER") else "unknown"

        self.logger.debug(
            'get HTTP_X_AUTH_USER is : %s ,user_name is: %s' %
            (web.ctx.env.get("HTTP_X_AUTH_USER"), self.user_name))
        self.logger.debug(
            'get HTTP_X_FROM_EMPID is : %s ,from_empid is: %s' %
            (web.ctx.env.get("HTTP_X_FROM_EMPID", ""), self.from_empid))
        self.logger.debug(
            'get HTTP_X_FROM_AUTH_USER is : %s ,from_user_name is: %s' %
            (web.ctx.env.get("HTTP_X_FROM_AUTH_USER", ""), self.from_auth_user))

        self.path = None

        self.result = {
            'code': 200,
            'message': 'OK',
            'data': {}
        }
        web.header('Content-Type', 'application/json')
        self.logger.info("%s %s" % (web.ctx.method, web.ctx.fullpath))

    def cors_wl_process(self):
        wl = web.ctx.tesla['http_opts']['cors_wl']
        # origin header exist only in cross site request
        origin = web.ctx.env.get('HTTP_ORIGIN', '')
        # if origin:
        #     web.header('Access-Control-Allow-Origin', origin)
        #     web.header('Access-Control-Allow-Credentials', 'true')
        #     web.header(
        #         "Access-Control-Allow-Methods",
        #         "GET,POST,OPTIONS,PUT,DELETE")
        #     web.header(
        #         "Access-Control-Allow-Headers",
        #         "Content-Type,Accept,X-Auth-User,X-User-Info,Product-Name,X-File,X-Product,X-Cluster, X-Upload-Path")

    def input(self, **kwargs):
        d = web.input(**kwargs)
        for item in d:
            _anti_sql_injection(d[item])
        return d

    def data(self):
        d = web.data()
        # for item in d:
        #     _anti_sql_injection(d[item])
        return d

    @property
    def json_body(self):
        if not self.body:
            return {}
        try:
            return self.jsonloads(self.body)
        except:
            raise Exception('invalid JSON HTTP body')

    def OPTIONS(self):
        return

    def jsondumps(self, obj):
        return jds(obj)

    def jsonloads(self, s):
        return jls(s)


class BaseHandler(object):
    def __init__(self, *args, **kwargs):
        set_request_id()
        self.request_id = get_upstream_trace()
        self.cors_wl_process()
        # Add a copy of context value in case of new coroutine
        self.server = web.ctx.server
        self.tesla_context = web.ctx.tesla
        # 兼容老code
        self.tesla_ctx = web.ctx.tesla
        self.config = self.tesla_ctx.config
        self.factory = web.ctx.factory
        self.db = self.tesla_ctx.db
        self.dbs = self.tesla_ctx.dbs
        self.dm = self.tesla_ctx.dm
        self.r = self.factory.get_redis_conn_wrapper()

        oplog_tn = self.tesla_ctx.get("db_opts", {}).get("oplog_tn")

        self.oplog_tn = oplog_tn if oplog_tn else "bcc_op_logs"
        self.op_log = OpLogMethods(self.oplog_tn)
        self.cluster_source = web.ctx.tesla['clusterinfo_source']

        if 'env' not in web.ctx.tesla or web.ctx.tesla['env'] == 'test':
            self.test_env = True
        else:
            self.test_env = False

        self.params_error = self.get_params()
        self.message = ""
        self.code = ""
        self.dynamic_code = ''
        self.dynamic_message = ''
        self.logger = logging.getLogger(__name__)
        self.logger.info("%s %s" % (web.ctx.method, web.ctx.fullpath))

        self.from_empid = web.ctx.env.get("HTTP_X_FROM_EMPID") if web.ctx.env.get(
            "HTTP_X_FROM_EMPID") and "" != web.ctx.env.get("HTTP_X_FROM_EMPID") else "unknown"
        self.from_auth_user = web.ctx.env.get("HTTP_X_FROM_AUTH_USER") if web.ctx.env.get(
            "HTTP_X_FROM_AUTH_USER") and "" != web.ctx.env.get("HTTP_X_FROM_AUTH_USER") else "unknown"

        self.logger.debug(
            'get HTTP_X_FROM_EMPID is : %s ,from_empid is: %s' %
            (web.ctx.env.get("HTTP_X_FROM_EMPID", ""), self.from_empid))
        self.logger.debug(
            'get HTTP_X_FROM_AUTH_USER is : %s ,from_user_name is: %s' %
            (web.ctx.env.get("HTTP_X_FROM_AUTH_USER", ""), self.from_auth_user))


        self.locale = web.ctx.env.get("HTTP_X_LOCALE", "zh_CN")
        u = web.ctx.env.get("HTTP_X_AUTH_USER")
        self.user_name = u if u else "unknown"
        empid = web.ctx.env.get("HTTP_X_EMPID")
        self.user_empid = str(empid) if empid else '0'
        self.user_id = self.user_empid
        self.logger.debug('get HTTP_X_AUTH_USER is [%s], HTTP_X_EMPID is [%s]' % (self.user_name, self.user_empid))
        self.path = None
        web.header('Content-Type', 'application/json')

    @property
    def json_body(self):
        if not self.body:
            return {}
        try:
            return self.jsonloads(self.body)
        except:
            raise Exception('invalid JSON HTTP body')

    def GET(self, path=None):
        """
            Params:
                path: urls like /test/a?t=1, then path is 'a'
        """
        self.path = path
        return self.request('GET', path)

    def POST(self, path=None):
        self.path = path
        return self.request('POST', path)

    def DELETE(self, path=None):
        self.path = path
        return self.request('DELETE', path)


    def waf_anti_sql_inject(self):
        # url_white_list = []  # 前缀匹配
        # keywords = ['\'', '"', ';', '`']
        # patterns = ['', ]  # re pattens
        # cur_url = web.ctx.get('homepath', '/') + web.ctx.get('path', '')
        # for wl in url_white_list:
        #     if cur_url.startswith(wl):
        #         return
        # for k, v in self.params.iteritems():
        #     for kw in keywords:
        #         if kw not in v:
        #             continue
        #         raise ArgError(u"参数错误: 参数(%s)包含SQL注入敏感字符(%s), "
        #                        u"完整参数值: %s" % (k, kw, v))
        pass

    def request(self, method, path):
        start_time = int(round(time.time() * 1000))
        ret = {
            'code': CODE.OK,
            'message': method,
            'data': [],
            'dynamicCode': '',
            'dynamicMessage': ''
        }
        result = self.handle(ret, path)
        # steaming response
        if result:
            return result

        # maybe set by user
        ret['requestId'] = self.request_id
        # json response
        web.header('Content-Type', 'application/json')
        end_time = int(round(time.time() * 1000))
        cost_time = end_time - start_time
        self.logger.info("[API REQUEST] [%s] [%s] [%s] cost %s milliseconds" % (self.request_id, web.ctx.fullpath, method, cost_time))
        web.header("X-FaaS-Cost", cost_time)
        return self.jsondumps(ret)

    def set_ret_msg(self, msg):
        self.message = msg

    def set_ret_code(self, code):
        try:
            self.code = int(code)
        except:
            self.code = code

    def set_dynamic_code(self, dynamic_code):
        self.dynamic_code = dynamic_code

    def set_dynamic_message(self, dynamic_message):
        self.dynamic_message = dynamic_message

    def set_ret_request_id(self, request_id):
        self.request_id = request_id

    @exception_wrapper
    def handle(self, ret, path):
        # get handler for operation
        try:
            handler = getattr(self, path)
        except Exception:
            raise ArgError("no url path: '%s' found" % path, CODE.NOT_FOUND)
        # exec handler and get data
        if self.params_error:
            raise self.params_error

        try:
            if hasattr(self, '__enter__'):
                getattr(self, '__enter__')()

            # if inspect.getargspec(handler).args:
            #     data = handler(self.params)
            # else:
            #     data = handler()
            data = handler(self.params)

            if data and hasattr(data, 'next'):
                return self.stream_response_handler(data)
            ret['data'] = data
            if self.code:
                ret['code'] = self.code
            ret['message'] = self.message
            ret['dynamicCode'] = self.dynamic_code
            ret['dynamicMessage'] = self.dynamic_message

        except Exception, e:
            trace_msg = traceback.format_exc()
            self.logger.error("handle request to %s error: %s" % (self.get_full_path(), trace_msg))

            if isinstance(e, TeslaBaseException):
                ret['code'] = e.get_ret_code()
                ret['message'] = e.get_ret_message()
                ret['data'] = e.get_ret_data()
            else:
                error_mesg = "Server exception: %s" % e
                ret['message'] = error_mesg
                ret['code'] = 500
                ret['data'] = trace_msg

    def get_argument(self, name, default=None):
        if name not in self.params and default is None:
            raise ArgError("param %s is required" % name)

        return self.params.get(name, default)

    def OPTIONS(self):
        return

    # aiyu
    def stream_response_handler(self, generator, mode=None, buf_size=1024):
        """
            the dhandler will be a stream response handler in chunked encoding sent to client
            if it use 'yield' to reutrn data

            e.g.

            with open('rb', '/tmp/foo') as f:
                yield(f.read(1024))
        """

        def h_custom():
            try:
                is_first = True
                while True:
                    if is_first:
                        web.header('Content-type', 'application/octet-stream')
                        is_first = False
                    content = generator.read(buf_size)
                    yield content
                    if is_first is False and content is None:
                        break
            except Exception as e:
                error = str(e)
                trace_msg = traceback.format_exc()
                self.logger.error("handle request to %s error: %s" % (self.get_full_path(), trace_msg))
                web.header('Content-type', 'text/html')
                web.header('Content-Length', len(error))
                yield error

        def h():
            try:
                _inited = False
                for _d in generator:
                    if not _inited:  # set header util the first part of data generated
                        web.header('Content-type', 'application/octet-stream')
                        # gevent wsgi handler  will add the header as chunked encoding if not provided Content-Length
                        # web.header('Transfer-Encoding','chunked')
                        _inited = True
                    yield _d

            except Exception as e:
                error = str(e)
                trace_msg = traceback.format_exc()
                self.logger.error("handle request to %s error: %s" % (self.get_full_path(), trace_msg))
                web.header('Content-type', 'text/html')
                web.header('Content-Length', len(error))
                yield error

        if mode and mode == 'custom':
            return h_custom()
        else:
            return h()

    def text_response_handler(self, content):
        """
        text/ html
        """

        def h():
            web.header('Content-type', 'text/html')
            web.header('Content-Length', len(content))
            yield content
        return h()


    def set_header(self, key, value):
        web.header(key, value)

    def get_client_ip(self):
        return web.ctx.ip

    def get_full_path(self):
        return web.ctx.fullpath

    # use function name or web path as parameters
    def purge_cache(self, path=None):
        r = self.factory.get_redis_conn_wrapper()
        ret = False
        if path is not None:
            self.logger.info("purge cache for %s" % path)
            if r.exists(path):
                r.expire(path, 0)
                ret = True
            else:
                self.logger.warning("%s is not cached" % path)
                return ret
        return ret

    def cors_wl_process(self):
        wl = web.ctx.tesla['http_opts']['cors_wl']
        # origin header exist only in cross site request
        origin = web.ctx.env.get('HTTP_ORIGIN', '')
        # if origin:
        #     web.header('Access-Control-Allow-Origin', origin)
        #     web.header('Access-Control-Allow-Credentials', 'true')
        #     web.header(
        #         "Access-Control-Allow-Methods",
        #         "GET,POST,OPTIONS,PUT,DELETE")
        #     web.header(
        #         "Access-Control-Allow-Headers",
        #         "Content-Type,Accept,X-Auth-User,X-User-Info,Product-Name,X-File,X-Product,X-Cluster,"
        #         "X-Upload-Path")

    def get_user_id(self):
        return 0

    def data(self):
        d = web.data()
        # for item in d:
        #     _anti_sql_injection(d[item])
        return d

    def redirect(self, url):
        raise web.redirect(url)

    def redirect_to_another(self, url):
        headers = {
            'Content-Type': 'text/html',
            'Location': url
        }
        raise HTTPError('302 Found', headers)

    def get_params(self):
        # support json params in body
        content_type = web.ctx.environ.get('CONTENT_TYPE')
        if content_type and content_type.startswith('application/json'): 
            try:
                self.uri_params = self.input()
                self.params = self.input()
                self.body = web.data()

                if self.body and isinstance(json.loads(self.body), dict):
                    self.params.update(json.loads(self.body))
            except:
                logging.error("get param failed, error=%s", traceback.format_exc())
                return ArgError("CONTENT_TYPE is json, but format error, content_type=%s, body=%s" % (content_type, self.body))
        else:
            # read the whole request include header and body
            # so we need avoid to call when dealing with large stream requests
            if hasattr(self, '_stream_request_body'):
                self.params = web.input(_method='get')
                self.uri_params = self.params
                self.body = None
            else:
                self.params = self.input()
                self.uri_params = self.params
                self.body = web.data()

        # set it global
        web.ctx.params = self.params

        return None

    def input(self, **kwargs):
        d = web.input(**kwargs)
        for item in d:
            _anti_sql_injection(d[item])
        return d

    def environ(self):
        env = os.environ.get("TESLA")
        if env:
            return self.jsonloads(env)
        return {}

    def jsondumps(self, obj):
        return jds(obj)

    def jsonloads(self, s):
        return jls(s)

    def getClustersByProduct(self, product):
        """
        :param product:
        :return: example
         [
            {
                "cluster": "AYODPS-SPARROW1",
                "type": ["control","computing"]
            }
        ]
        """
        data = []
        if ".json" in self.cluster_source:
            if "odps" == product:
                product = "apsara"

            jsondata = _get_cluster_data(product)
            for region, region_data in jsondata['regions'].iteritems():
                for cluster in region_data['clusters']:
                    if "apsara" == product and "ODPS" not in cluster.upper():
                        continue

                    clusterInfo = {}
                    clusterInfo["cluster"] = cluster
                    # clusterdata[cluster]['type']
                    clusterInfo["type"] = ["control", "computing"]
                    data.append(clusterInfo)

        else:
            pass
        return data

    def getClustersByProductAndType(self, product, type):
        """
        :param product:
        :param type:
        :return: example
        [
            {
                "cluster": "AYODPS-SPARROW1",
                "type": ["control","computing"]
            }
        ]
        """
        data = []
        if ".json" in self.cluster_source:
            if "odps" == product:
                product = "apsara"

            jsondata = _get_cluster_data(product)
            for region, region_data in jsondata['regions'].iteritems():
                for cluster in region_data['clusters']:
                    if "apsara" == product and "ODPS" not in cluster.upper():
                        continue

                    clusterInfo = {}
                    clusterInfo["cluster"] = cluster
                    # clusterdata[cluster]['type']
                    clusterInfo["type"] = ["control", "computing"]
                    data.append(clusterInfo)
        else:
            pass
        return data

    def getMachineInfoByClusterRole(self, product, cluster, role):
        """
        :param product:
        :param cluster:
        :param role:
        :return: example
        {"ag": "10.101.219.18"}
        """
        data = {}
        if ".json" in self.cluster_source:
            jsondata = _get_cluster_data(self.cluster_source)
            data[role] = jsondata[product][cluster][role] if product in jsondata and cluster in jsondata[
                product] and role in jsondata[product][cluster] else ""
        else:
            pass
        return data

    def cluster2ag(self, cloudproduct, cluster):
        aghost = self.get_server_by_role(
            cloudproduct, "admingateway", cluster)[0]
        return aghost

    def get_all_cluster_ag(self, cloudproduct):
        clusters = [tmp["cluster"]
                    for tmp in self.getClustersByProduct(cloudproduct)]
        return [
            self.get_server_by_role(
                cloudproduct,
                "admingateway",
                cluster)[0] for cluster in clusters]

    def ifclusterright(self, params):
        if "cluster" not in params:
            return False

        product = params["cloudproduct"]
        in_cluster = params["cluster"]

        if "odps" == product:
            product = "apsara"

        jsondata = _get_cluster_data(product)
        for region, region_data in jsondata['regions'].iteritems():
            for cluster in region_data['clusters']:
                if in_cluster == cluster:
                    return region_data['clusters'][cluster]['ag']

        return False

    def dealparams(self, params, checklist):
        for word in checklist:
            if word not in params or not params[word]:
                return json.dumps(
                    {"code": 400, "msg": "lack of %s" % word, "data": []})

        if "cluster" in checklist:
            clusteraghost = self.ifclusterright(params)
            if not clusteraghost:
                return json.dumps(
                    {"code": 400, "msg": "no such cluster", "data": []})
            params["clusteraghost"] = clusteraghost
        return ""

    @staticmethod
    def _get_odps_cluster_from_db(product):
        _res = BaseModel().db.query(
            "select cluster from bcc_cluster_hostmanage where product=$product limit 1;",
            vars={"product": product}).list()
        c_result = _res[0] if _res else {"cluster": ""}
        return c_result['cluster'] if "cluster" in c_result else ""

    @staticmethod
    def _get_cluster_hostmanage_raw_from_db(product, cluster):
        _res = BaseModel().db.query(
            "select * from bcc_cluster_hostmanage where conf_type = 'server_conf' and product=$product and (cluster = $cluster or cluster=$default_cluster);",
            vars={
                "product": product,
                "cluster": cluster,
                "default_cluster": 'Default_' +
                cluster}).list()
        if (len(cluster) < 2):
            _res = BaseModel().db.query(
                "select * from bcc_cluster_hostmanage where conf_type = 'server_conf' and product=$product ;",
                vars={"product": product}).list()
        return _res

    @staticmethod
    def get_server_by_role(product, role, cluster):
        if cluster == 'AYADS-ZM' and role == 'admingateway':
            return ['10.110.76.6']
        if cluster == 'AY-ADS-BEIJING' and role == 'admingateway':
            return ['10.155.40.200']
        if role == "bcc_controller":
            # TODO will be return a list ?
            return json.dumps(['bcc_controller'])
        _res = BaseHandler._get_cluster_hostmanage_raw_from_db(
            product, cluster)
        if len(_res) < 1:
            return []
        confList = []
        ipList = []
        for item in _res:
            addr = item['host']
            if len(addr) < 6:
                continue
            if addr in ipList:
                continue
            if item['role'] is None and item['role_apsara'] is None and role != "all":
                continue
            if item['role'] is None:
                item['role'] = ""
            if item['role_apsara'] is None:
                item['role_apsara'] = ""
            if role not in [
                x.strip(' ') for x in (
                    item['role'] +
                    "," +
                    item['role_apsara']).split(",")] and role != "all":
                continue
            # if role == "all" and len(item['role_apsara']) < 1:
            #    continue
            ipList.append(addr)
            confList.append({"addr": addr, "role": item['role']})
        return ipList
        # return json.dumps(ipList)

    @staticmethod
    def get_role_by_server(product, server, cluster):
        _res = BaseModel().db.query(
            "select * from bcc_cluster_hostmanage where conf_type = 'server_conf' and product='%s' and host = '%s' and (cluster = '%s' or cluster='%s');" %
            (product, server, cluster, 'Default_' + cluster)).list()
        if (len(cluster) < 2):
            _res = BaseModel().db.query(
                "select * from bcc_cluster_hostmanage where conf_type = 'server_conf' and product='%s' and host = '%s';" %
                (product, server)).list()
        _roles = []
        for i in _res:
            if i['role']:
                _roles += i['role'].split(',')
            if i['role_apsara']:
                _roles += i['role_apsara'].split(',')
        return list(set(_roles))
        # return json.dumps(list(set(_roles)))

    @staticmethod
    def get_iphostname_list(product, cluster):
        _res = BaseHandler._get_cluster_hostmanage_raw_from_db(
            product, cluster)
        if len(_res) < 1:
            return {}

        ipHostNameList = {}
        for item in _res:
            addr = item['host']
            if len(addr) < 6:
                continue
            if addr in ipHostNameList:
                continue
            if 'host_name' not in item or not item['host_name']:
                continue

            ipHostNameList[addr] = item['host_name']
        return ipHostNameList

    @staticmethod
    def get_role_def(product, cluster=''):
        _res = BaseHandler._get_cluster_hostmanage_raw_from_db(
            product, cluster)
        if len(_res) < 1:
            return {"ips": [], "iproles": {}, "roleips": {}}
        roleConf = {}
        ipList = []
        ipRoleList = {}
        roleIpList = {}
        for item in _res:
            addr = item['host']
            if addr in ipList:
                continue

            ipList.append(addr)
            roleList = item["role_apsara"].split(",")
            roleIpList[addr] = roleList
            for role in roleList:
                if role not in ipRoleList:
                    ipRoleList[role] = []
                ipRoleList[role].append(addr)
        roleConf = {
            "ips": ipList,
            "iproles": ipRoleList,
            "roleips": roleIpList}
        return roleConf

    @staticmethod
    def get_local_ip():
        _ret = subprocess.check_output(['ip', 'a'])
        _re = re.findall('\sinet\s(\S+?)\/\d+', _ret)
        if (len(_re) > 0):
            for _ip in _re:
                if (not str(_ip).startswith('127.')) and (not str(_ip).startswith('169.')) and (
                        not str(_ip).startswith('0.')) and (len(re.findall('\d', str(_ip))) > 0):
                    return str(_ip)
        else:
            return '127.0.0.1'


def _get_cluster_data(project):
    root_dir = os.path.split(os.path.realpath(__file__))[0] + "/../"
    data = eval(
        open(
            root_dir +
            "/conf/product/" +
            project +
            "/domain.json",
            "r").read())

    return data


def _anti_sql_injection(value):
    return


#     keyword = (
#         "insert", "update", "select",
#         "'", "(", ")", "/", "*", ";"
#     )
#     value = value.lower()
#     for item in keyword:
#         if value.find(item) >= 0:
#             raise Exception, "invalid parameter %s detected." % item


class HttpMethods(object):
    """
    Supported HTTP methods, used by NoMethod(cls=)

    ['GET', 'HEAD', 'POST', 'PUT', 'DELETE']
    """

    def __init__(self, *args):
        for arg in args:
            setattr(self, arg, 0)


read_only_handler = HttpMethods('GET', 'HEAD')
write_only_handler = HttpMethods('POST', 'HEAD')


class ArgValidationError(TeslaBaseException):
    """ Used by CrudHandler for raise validation arg error """
    pass


class PomHandler(BaseHandler):
    def __init__(self):
        super(PomHandler, self).__init__()
        self.error_code = ""
        self.error_message = ""
        self.request_id = ""
        self.success = ""

    def set_error_msg(self, msg):
        self.error_message = msg

    def set_error_code(self, code):
        self.error_code = code

    def set_request_id(self, request_id):
        self.request_id = request_id

    def set_success(self, success):
        self.success = success

    def request(self, method, path):
        start_time = int(round(time.time() * 1000))

        ret = {
            'Success': "",
            'RequestId': "",
            'ErrorCode': "",
            'ErrorMessage': "",
            'DynamicCode': '',
            'DynamicMessage': ''
        }
        result = self.handle(ret, path)
        # steaming response
        if result:
            return result

        # maybe set by user
        # ret['requestId'] = self.request_id
        # json response
        web.header('Content-Type', 'application/json')
        end_time = int(round(time.time() * 1000))
        cost_time = end_time - start_time
        web.header("X-FaaS-Cost", cost_time)
        self.logger.info("[API REQUEST] [%s] [%s] cost %s milliseconds" % (web.ctx.fullpath, method, cost_time))
        return self.jsondumps(ret)

    @exception_wrapper
    def handle(self, ret, path):
        # get handler for operation
        try:
            handler = getattr(self, path)
        except Exception:
            raise ArgError("no url path: '%s' found" % path, CODE.NOT_FOUND)
        # exec handler and get data
        if self.params_error:
            raise self.params_error

        try:
            if hasattr(self, '__enter__'):
                getattr(self, '__enter__')()

            # if inspect.getargspec(handler).args:
            #     data = handler(self.params)
            # else:
            #     data = handler()
            data = handler(self.params)

            if data and hasattr(data, 'next'):
                return self.stream_response_handler(data)

            if data and isinstance(data, dict):
                ret.update(data)
            if self.error_code is not None:
                ret['ErrorCode'] = self.error_code
            if self.error_message is not None:
                ret['ErrorMessage'] = self.error_message
            if self.success is not None:
                ret["Success"] = self.success
            if self.request_id is not None:
                ret["RequestId"] = self.request_id

        except Exception, e:
            trace_msg = traceback.format_exc()
            self.logger.error("handle request to %s error: %s" % (self.get_full_path(), trace_msg))

            ret["ErrorCode"] = 'InternalError'
            ret["ErrorMessage"] = 'A server error occurred while processing your request'
            ret["Success"] = False
        ret['DynamicCode'] = self.dynamic_code
        ret['DynamicMessage'] = self.dynamic_message


class CrudHandler(RestHandler):
    """
    A common CRUD operation handler.     -----   Adonis

    Depending on CrudModel or other compatible Model interface(i.e.
    add_records/update_records/del_records)
    Extract common code of Topology and TailoredDeploy handler.
    """

    # ArgType = enum('number', 'number_list', 'list', 'bool', 'string',
    #                'pks', 'pk', 'datetime', 'json')
    class ArgType(object):
        number = 0
        number_list = 1
        list = 2
        string_list = 2
        bool = 3
        string = 4
        pks = 5  # TODO: add pk and pks validation
        pk = 6
        datetime = 7
        json = 8
        datetime_tuple = 9  # used for datetime between/<=/>= sql select
        datetime_list = 10

    # Controller args for HTTP GET request
    ARGS_CONF = {
        'pk': {
            'arg_type': ArgType.string,
            'mandatory': False,
        },
        'get_fields': {
            'arg_type': ArgType.string_list,
            'mandatory': False,
            # 'arg_range': tb_fields
        },
        'order_fields': {
            'arg_type': ArgType.string_list,
            'mandatory': False,
            # 'arg_range': tb_fields
        },
        'desc': {
            'arg_type': ArgType.bool,
            'mandatory': False,
        },
        'pagination_info': {
            'arg_type': ArgType.bool,
            'mandatory': False,
        },
        'pagination_on': {
            'arg_type': ArgType.bool,
            'mandatory': False,
        },
        'page_size': {
            'arg_type': ArgType.number,
            'mandatory': False,
            'arg_condition': lambda x: x > 0
        },
        'page_index': {
            'arg_type': ArgType.number,
            'mandatory': False,
            'arg_condition': lambda x: x > 0
        },
    }

    VALID_ACTIONS = ['add', 'create', 'update', 'delete', ]

    def __init__(self, model_class=None):
        # BaseHandler.__init__(self)
        super(CrudHandler, self).__init__()
        self.tesla_context = web.ctx.tesla
        self.tesla_ctx = web.ctx.tesla
        self.config = self.tesla_ctx.config
        self.params = self.input()
        self.user_id = 0
        # subclass must override following attributes.
        self.model = model_class() if model_class is not None else None
        # Give Handler a chance to override Model settings,
        # set fields visible to user
        self.table_name = ''
        self.get_fields = []  # fields returned to user.
        self.edit_fields = []  # fields can be edited
        self.mandatory_fields = []  # fields mandatory when create new record
        self.fields = []
        self.args_conf = copy.deepcopy(CrudHandler.ARGS_CONF)
        self.result = {'code': 200, 'message': 'OK', 'data': {}}

    def validate_arg(self, arg_name, params, ret,
                     default_value=None,
                     arg_type=ArgType.string,
                     arg_range=None,
                     arg_condition=None):
        """
        :param ret: ret value to be returned as json.
        :param arg_name: values to be validated.
        :param arg_type: one of self.ArgType,
        :param arg_range: tuple/list/dict/object that support in operation.
        :param arg_condition: condition function to be applied to arg, i.e.
            lambda x: x < 10 and x > 3
        :return: True/False, typed value of arg.
        """

        def check_arg():
            if arg_range and arg_value not in arg_range:
                ret["code"] = 403
                ret["message"] = "Param '%s' invalid, " \
                                 "valid values: %s." % (arg_name, arg_range)
                return False
            if arg_condition and not arg_condition(arg_value):
                ret["code"] = 403
                ret["message"] = "Param '%s' invalid. Condition: %s, %s" \
                                 % (arg_name,
                                    arg_condition.func_name,
                                    arg_condition.func_doc)
                return False
            return True

        if arg_name not in params:
            if default_value is None:
                ret["code"] = 403
                ret["message"] = "Param '%s' is missing. " % arg_name
                return False, ''
            else:
                return True, default_value
        arg_value = str(params[arg_name])
        if arg_type == self.ArgType.string:
            arg_value = arg_value.strip()
            if not arg_value and not default_value:
                ret["code"] = 400
                ret["message"] = "Param '%s' invalid, empty !" % arg_name
                return False, "Param '%s' is empty string" % arg_name
            elif not arg_value:
                arg_value = default_value
            if not check_arg():
                return False, ''
        elif arg_type == self.ArgType.number:
            if not arg_value.isdigit():
                ret["code"] = 403
                ret["message"] = "Param '%s' invalid, not a digit" % arg_name
                return False, ''
            arg_value = long(arg_value)
            if not check_arg():
                return False, ''
        elif arg_type == self.ArgType.bool:
            if arg_value not in ('0', '1'):
                ret["code"] = 403
                ret["message"] = "Param '%s' invalid, " \
                                 "valid value: '0' or '1' " % arg_name
                return False, ''
            arg_value = bool(toint(arg_value))
        elif arg_type == self.ArgType.datetime:
            code, dt_value = self.validate_datetime(arg_value)
            if not code:
                ret["code"] = 403
                ret["message"] = "Param '%s' invalid, valid values:'%s'" \
                                 % (arg_name, dt_value)
                return False, ''
            if not check_arg():
                return False, ''
            arg_value = dt_value
        elif arg_type == self.ArgType.datetime_tuple:
            times = arg_value.split(',')
            if len(times) != 2:
                ret["code"] = 403
                ret["message"] = u"Param '%s' invalid, length of arg value" \
                                 u"is not 2" % arg_name
                return False, ''
            if times[0]:
                code1, dt_value1 = self.validate_datetime(times[0])
            else:  # support null value
                code1, dt_value1 = True, None
            if times[1]:
                code2, dt_value2 = self.validate_datetime(times[1])
            else:
                code2, dt_value2 = True, None
            if not code1 or not code2:
                ret["code"] = 403
                ret["message"] = u"参数'%s'不可用, 用逗号隔开两个如下格式的时间:" \
                                 u"%s %s" % (arg_name, dt_value1, dt_value2)
                return False, ''
            arg_value = (dt_value1, dt_value2)
        elif arg_type == self.ArgType.datetime_list:
            times = arg_value.split(',')
            arg_value = []
            for x in times:
                if not x:
                    continue
                is_valid, dt_value = self.validate_datetime(x)
                if not is_valid:
                    ret["code"] = 403
                    ret["message"] = u"参数'%s'不可用, '%s'时间格式不正确" \
                                     % (arg_name, x)
                    return False, ''
                arg_value.append(dt_value)
        elif arg_type == self.ArgType.json:
            try:
                dt_value = self.jsonloads(arg_value)
            except ValueError:
                ret["code"] = 403
                ret["message"] = "Param '%s' is not a valid json string" \
                                 % arg_name
                return False, ''
            arg_value = dt_value
        elif arg_type == self.ArgType.list:
            arg_value_list = arg_value.strip().split(',')
            arg_value_list = map(lambda x: x.strip(), arg_value_list)
            arg_value_list = filter(lambda x: x, arg_value_list)
            if not arg_value_list:
                ret["code"] = 403
                ret["message"] = "Param '%s' invalid, empty list" % arg_name
                return False, ''
            for arg_value in arg_value_list:
                if not check_arg():
                    return False, ''
            arg_value = arg_value_list
        elif arg_type == self.ArgType.number_list:
            arg_value_list = arg_value.split(',')
            arg_value_list = map(lambda x: x.strip(), arg_value_list)
            arg_value_list = filter(lambda x: x, arg_value_list)
            for arg_value_item in arg_value_list:
                if not arg_value_item.isdigit():
                    ret["code"] = 403
                    ret["message"] = "Param '%s' invalid, %s is not a digit" \
                                     % (arg_name, arg_value_item)
                    return False, ''
                arg_value = int(arg_value_item)
                if not check_arg():
                    return False, ''
            arg_value = map(lambda x: int(x), arg_value_list)
        else:
            return False, 'Arg type not supported, Handler.validate_arg()'
        return True, arg_value

    @staticmethod
    def _is_required(arg_conf):
        # mandatory is a bad name, use 'required' instead.
        required = arg_conf['required'] if 'required' in arg_conf else \
            arg_conf.get('mandatory', True)
        return required

    def validate_args(self, args_conf, params, ret):
        """
        Deprecated!!! Use validate_args_with_exception() instead.
        Validate multiple args at once

        :param args_conf:  Dict containing args conf
        {
            arg1: {
                    arg_type: ArgType.int,
                    required: True/False,
                    mandatory: True/False,  # Deprecated! use 'required' field
                    default_value: None,    # optional
                    arg_range: None,        # optional
                    arg_condition: None     # optional
            },
        }
        :return:  True/False and Dict with valid arg and arg value
        transformed to corresponding type as key / value
        """
        res = {}
        optional_confs = ('default_value', 'arg_range', 'arg_condition')
        for arg_name, conf_item in args_conf.iteritems():
            required = self._is_required(conf_item)
            if not required and (arg_name not in params or
                                 not params[arg_name].strip()
                                 ):
                continue
            optional_conf_args = {key: conf_item.get(key, None)
                                  for key in optional_confs}
            code, info = self.validate_arg(
                arg_name, params, ret, arg_type=conf_item['arg_type'],
                **optional_conf_args)
            if not code:
                return False, res
            res[arg_name] = info
        return True, res

    def validate_args_with_exception(self, args_conf, params):
        """ Raise exception when validation failed """
        api_res = {}
        code, res = self.validate_args(args_conf, params, api_res)
        if not code:
            err_msg = api_res.get('message', '')
            raise FeArgError(mesg=err_msg)
        return res

    @staticmethod
    def validate_datetime(dt_str,
                          dt_formats=['%Y-%m-%d %H:%M',
                                      '%Y-%m-%d %H:%M:%S',
                                      '%Y-%m-%d %H',
                                      '%Y-%m-%d', ]):
        for dt_format in dt_formats:
            try:
                dt_value = datetime.datetime.strptime(dt_str, dt_format)
            except ValueError as e:
                continue
            else:  # Succeed
                return True, dt_value
        now_tm = datetime.datetime.now()
        valid_tm_str = '/'.join(now_tm.strftime(dt_format)
                                for dt_format in dt_formats)
        return False, valid_tm_str

    def validate_mandatory_args(self, ret, *mandatory_args):
        for arg in mandatory_args:
            if arg not in self.params or not self.params[arg].strip():
                ret['code'] = 403
                ret['message'] = "Param '%s' is required!" % arg
                return False
        return True

    def get_page_args(self):
        page_index = toint(self.params.get('page', 1)) or 1
        page_size = toint(self.params.get('limit', PAGE_SIZE)) or PAGE_SIZE
        # offset = (page_index-1)*page_size
        return page_size, page_index

    @exception_wrapper
    def GET(self):
        ret = {'code': 200, 'message': 'OK', 'data': []}
        get_all = False
        gf_range = []  # range of args_conf.get_fields
        if self.get_fields:
            gf_range = self.get_fields
        else:
            if not self.table_name:
                self.table_name = self.model.get_default_table()
            if self.table_name:
                get_all = True
                gf_range = self.model.get_tb_fields(self.table_name)
        if gf_range:
            self.args_conf['get_fields']['arg_range'] = gf_range
            self.args_conf['order_fields']['arg_range'] = gf_range
        code, ac_kwargs = self.validate_args(self.args_conf, self.params, ret)
        if not code:
            return self.jsondumps(ret)
        record_kwargs = {key: self.params[key] for key in gf_range
                         if key in self.params}
        record_kwargs.update(ac_kwargs)
        if get_all:
            gf_range = []
        if 'get_fields' not in record_kwargs:
            record_kwargs['get_fields'] = gf_range
        records = self.model.get_records(self.table_name, **record_kwargs)
        ret['data'] = records
        return self.jsondumps(ret)

    @exception_wrapper
    @login_required
    def POST(self):
        self.logger.debug("Post body: %s" % self.params)
        ret = dict(code=200, message="ok", data=[])
        if not self.table_name:
            self.table_name = self.model.get_default_table()
        if 'action' not in self.params:
            ret = dict(code=403, message="Param 'action' is required!",
                       data=[])
            return self.jsondumps(ret)

        action = self.params.get("action")
        if self.edit_fields:
            valid_args = self.edit_fields
        else:
            valid_args = self.model.get_tb_fields(
                self.table_name, field_type=self.model.FieldType.editable)
        if action in ['create', 'add']:
            # Argument checking and filtering.
            if self.mandatory_fields:
                mandatory_args = self.mandatory_fields
            else:
                mandatory_args = self.model.get_tb_fields(
                    self.table_name,
                    field_type=self.model.FieldType.mandatory)
            if not self.validate_mandatory_args(ret, *mandatory_args):
                return self.jsondumps(ret)
            kw_args = {arg: self.params[arg]
                       for arg in valid_args if arg in self.params}
            new_id = self.model.add_record(self.user_id, self.table_name,
                                           **kw_args)
            ret['data'] = new_id
            if not new_id:
                ret['code'] = 500
                ret['message'] = 'DB operation return 0, added 0 records!'
        elif action == "update":
            code, info = self.validate_arg('id', self.params, ret,
                                           arg_type=self.ArgType.number)
            if not code:
                return self.jsondumps(ret)
            record_id = info
            kw_args = {arg: self.params[arg]
                       for arg in valid_args if arg in self.params}
            ret['data'] = self.model.update_record(
                self.user_id, self.table_name, record_id, **kw_args)
        elif action == "delete":
            if 'id' not in self.params:
                ret['code'] = 406
                ret['message'] = "Param 'id' is required!"
            else:
                ids = self.params['id'].split(',')
                ids = map(lambda x: toint(x), ids)
                if all(ids):
                    ret['data'] = self.model.del_records(
                        self.table_name, pks=ids)
                else:
                    ret['code'] = 406
                    ret['message'] = "Value of param 'id' is invalid"
        else:
            ret['code'] = 406
            ret['message'] = "Value of param 'action' is invalid."
        return self.jsondumps(ret)


class OpLogMethods():
    """
    Operation log manage model
    """
    OL_EDIT_FIELDS = ['log_type', 'user_name', 'action', 'target',
                      'status', 'comment', 'validflag', 'cloudproduct']

    class LogType(object):
        debug = 0
        info = 1
        warning = 2
        danger = 3
        critical = 4

    class LogStatus(object):
        ok = 0
        failed = 1
        waiting = 2  # 等待操作结果, 后续扩展
        unknown = 3  # 未决状态， 后续扩展

    def __init__(self, tb_name):
        self.tb_name = tb_name
        self.crudModel = CrudModel()
        self.TB_STRUCTURE = {
            self.tb_name: {
                self.crudModel.FieldType.all: self.OL_EDIT_FIELDS + [
                    'id',
                    'create_time',
                ],
                self.crudModel.FieldType.editable: self.OL_EDIT_FIELDS,
                self.crudModel.FieldType.mandatory: [
                    'user_name',
                    'action',
                    'target'],
            },
        }

        self.crudModel.register_tb_structures(self.TB_STRUCTURE)

    def add_log(self, user_name, action,
                target=None,
                cloudproduct='odps',
                log_type=LogType.info,
                status=LogStatus.ok,
                comment=''):
        if target is None:
            target = {}
        target = jds(target)
        return self.crudModel.add_record(user_name, self.tb_name,
                                         user_name=user_name,
                                         action=action,
                                         target=target,
                                         status=status,
                                         log_type=log_type,
                                         comment=comment,
                                         cloudproduct=cloudproduct)
