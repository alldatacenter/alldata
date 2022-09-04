#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from flask.views import View
from common.sreworks_result_factory import SREWorksResultFactory
from common.var_type import VarType
from common.check_helper import CheckHelper


class BaseView(View):
    def __init__(self, exec_func: str):
        self.result_factory = SREWorksResultFactory()
        self.func = exec_func.lower()

    def dispatch_request(self):
        func = getattr(self, self.func, None)
        assert func is not None, f"Unimplemented function {self.func!r}"
        return func()

    @staticmethod
    def _check_param(key: str, value, var_type: VarType, allow_empty=True, positive=False):
        """
        校验参数合法性
        :param key: 参数名称
        :param value: 参数值
        :param allow_empty: 是否允许为空, 对数值类型和布尔类型无效
        :param positive: 正数, 只对数值类型有效
        :return: 返回合法的参数值
        """
        if var_type == VarType.STR:
            try:
                value = CheckHelper.ensure_string(key, value, allow_empty)
            except ValueError as ex:
                raise ex
        elif var_type == VarType.BOOL:
            try:
                value = CheckHelper.ensure_boolean(key, value)
            except ValueError as ex:
                raise ex
        elif var_type == VarType.INT:
            try:
                value = CheckHelper.ensure_integer(key, value, positive)
            except ValueError as ex:
                raise ex
        elif var_type == VarType.LIST:
            try:
                value = CheckHelper.ensure_list(key, value, allow_empty)
            except ValueError as ex:
                raise ex
        elif var_type == VarType.DICT:
            try:
                value = CheckHelper.ensure_dict(key, value, allow_empty)
            except ValueError as ex:
                raise ex
        else:
            raise ValueError("{} unsupported param type".format(key))

        return value
