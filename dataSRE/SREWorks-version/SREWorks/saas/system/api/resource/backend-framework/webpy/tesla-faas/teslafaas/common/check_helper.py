# -*- coding: utf-8 -*-

import six
import json


class CheckHelper(object):
    """
    检查辅助工具类
    """
    @staticmethod
    def ensure_string(name, value, allow_empty=True):
        """
        检验给定的 value 是否为 str/unicode 类型
        :param name: 名称
        :param value: 值
        :param allow_empty: 是否允许为空
        :return: 如果没有错误，返回原值
        """
        if value is None and allow_empty:
            return None
        if not value and not allow_empty:
            raise ValueError("{} should be non-empty str or unicode".format(name))
        if not isinstance(value, (six.binary_type, six.text_type)):
            raise ValueError("{} should be str or unicode".format(name))
        return value

    @staticmethod
    def ensure_integer(name, value):
        """
        检验给定的 value 是否为 int 类型
        :param name: 名称
        :param value: 值
        :return: 如果没有错误，返回原值
        """
        if value is None:
            return None
        if not isinstance(value, int):
            raise ValueError("{} should be int".format(name))
        return value

    @staticmethod
    def ensure_boolean(name, value):
        """
        检验给定的 value 是否为 bool 类型
        :param name: 名称
        :param value: 值
        :return: 如果没有错误，返回原值
        """
        if value is None:
            return None
        if not isinstance(value, bool):
            raise ValueError("{} should be bool".format(name))
        return value

    @staticmethod
    def ensure_list(name, value, allow_empty=True):
        """
        检验给定的 value 是否为 list 类型
        :param name: 名称
        :param value: 值
        :param allow_empty: 是否允许为空
        :return: 如果没有错误，返回原值
        """
        if value is None and allow_empty:
            return None
        if not value and not allow_empty:
            raise ValueError("{} should be non-empty list".format(name))
        if not isinstance(value, list):
            raise ValueError("{} should be list".format(name))
        return value

    @staticmethod
    def ensure_dict(name, value, allow_empty=True):
        """
        检验给定的 value 是否为 dict 类型
        :param name: 名称
        :param value: 值
        :param allow_empty: 是否允许为空
        :return: 如果没有错误，返回原值
        """
        if value is None and allow_empty:
            return None
        if not value and not allow_empty:
            raise ValueError("{} should be non-empty dict".format(name))
        if not isinstance(value, dict):
            raise ValueError("{} should be dict".format(name))
        return value

    @staticmethod
    def ensure_file(name, value, allow_empty=True):
        """
        检验给定的 value 是否为 file 类型
        :param name: 名称
        :param value: 值
        :param allow_empty: 是否允许为空
        :return: 如果没有错误，返回原值
        """
        if value is None and allow_empty:
            return None
        if not value and not allow_empty:
            raise ValueError("{} should be non-empty file".format(name))
        return value

    @staticmethod
    def ensure_dict_or_list(name, value, allow_empty=True, to_string=True, ensure_ascii=False):
        """
        检验给定的 value 是否为 dict 或 list 类型
        :param name: 名称
        :param value: 值
        :param allow_empty: 是否允许为空
        :param to_string: 是否要转成字符串
        :param ensure_ascii: 是否默认ascii
        :return: 如果没有错误，返回原值
        """
        if value is None and allow_empty:
            return None
        if not value and not allow_empty:
            raise ValueError("{} should be non-empty dict or list".format(name))
        if not isinstance(value, (dict, list)):
            raise ValueError("{} should be dict or list".format(name))
        if not to_string:
            return value
        return json.dumps(value, ensure_ascii=ensure_ascii)

    @staticmethod
    def clean_dict(value):
        """
        清理 dict 中值为 None 的项
        """
        return {k: v for k, v in six.iteritems(value) if v is not None}

    @staticmethod
    def dump_clean_dict(value, ensure_ascii=False):
        """
        清理 dict 中值为 None 的项，并进行 json.dumps 并返回
        :param value:
        :param ensure_ascii: 是否默认ascii
        """
        return json.dumps(CheckHelper.clean_dict(value), ensure_ascii=ensure_ascii)
