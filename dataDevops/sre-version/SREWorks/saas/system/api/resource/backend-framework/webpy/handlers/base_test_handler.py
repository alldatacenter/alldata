#!/usr/bin/env python
# encoding: utf-8
""" """
from teslafaas.container.webpy.common.BaseHandler import BaseHandler
from teslafaas.container.webpy.common.BaseHandler import PomHandler
from teslafaas.container.webpy.common.decorators import fcached,ActionError,app_route
import time
import web

from ..models.mysql_test_model import MysqlTestModel

@app_route(r'/base/(.*)')
class TestBaseHandler(PomHandler):

    def test(self, params):
        # 自定义request_id，跟其他系统级联
        self.set_ret_request_id(4444)
        # 记录日志和获得当前请求用户邮箱前缀和工号
        self.logger.info("record some body %s %s request" % (self.user_name, self.user_empid))
        return params

    def echo(self, params):
        self.logger.info("params=%s, body=%s" % (params, web.data()))
        return "echo"

    def test_argument(self, params):
        ## 请求中不带 aaa 会自动抛出标准的 400 错误
        self.get_argument('aaa')

    def test_error(self, params):
        # 返回给前端自定义错误code的信息
        raise ActionError("yee", code=333)

    def test_mysql(self, params):
        return MysqlTestModel().get_db_time()

    def test_tesla_sdk(self, params):
        ## Tesla 通道服务实例，更多SDK使用参考https://yuque.antfin-inc.com/bdsre/userguid
        channel = self.factory.get_channel_wrapper()
        resp = channel.run_command('echo 123', ip=[self.TEST_IP, ])
        # 检查结果是否执行成功
        if not resp.is_success():
            print '--- channel error: ', resp.message, resp.code, resp.data
            data = resp.message
        else:
            data = resp.data

    def test_set_dynamic_code_and_dynamic_message(self, params):
        self.set_dynamic_message("dynamic message")
        self.set_dynamic_code('200 dynamic code')
        return "success"

    def test_set_dynamic_code_and_dynamic_message2(self, params):
        return "success"

    def test_specified_factory(self, params):
        # 自定义工厂会被自动初始化
        self.factory.hello_factory.say()



class DemoHandler(TestBaseHandler):
    pass
