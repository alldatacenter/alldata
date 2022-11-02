#!/usr/bin/env python
# encoding: utf-8
""" """
from teslafaas.container.webpy.bcc_factory import BCCFactoryBase

class HelloFactory(BCCFactoryBase):

    @classmethod
    def register(cls):
        """
        继承与工厂基类并且实现了注册接口，会在启动时自动注入到全局单例
        返回一个字符串用来代表名字，后续在handler中可以通过 self.factory.hello.test 来访问
        :return:
        """
        return 'hello_factory'

    def say(self):
        return 'HelloFactory'
