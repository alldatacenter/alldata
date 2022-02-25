'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import types
from ambari_commons import OSCheck


class OsFamilyImpl(object):
  """
  Base class for os dependent factory. Usage::

      class BaseFoo(object): pass
      @OsFamilyImpl(os_family="windows")
      class OsFooW(BaseFoo):pass
      print BaseFoo()# OsFooW
      @OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
      class OsFooD(BaseFoo):pass
      print BaseFoo()# OsFooD

  """

  DEFAULT = "default"
  """
  constant for default implementation
  """

  def __init__(self, base_cls=None, os_family=None):
    self.base_cls = base_cls
    self.os_const = os_family


  def __call__(self, cls):
    if self.base_cls:
      base_cls = self.base_cls
    else:
      base_cls = cls.__bases__[0]

    if not hasattr(base_cls, "_impls"):
      base_cls._impls = {}

    base_cls._impls[self.os_const] = cls

    def new(cls, *args, **kwargs):
      if OSCheck.get_os_family() in cls._impls:
        os_impl_cls = cls._impls[OSCheck.get_os_family()]
      else:
        os_impl_cls = cls._impls[OsFamilyImpl.DEFAULT]
      return object.__new__(os_impl_cls)

    base_cls.__new__ = types.MethodType(new, base_cls)

    return cls

class OsFamilyFuncImpl(object):
  """
  Base class for os dependent function. Usage::

      @OSFamilyFuncImpl(os_family="windows")
      def os_foo(...):pass

  """
  _func_impls = {}

  def _createFunctionInstance(self, func):
    self._func_impls[func.__module__ + "." + func.__name__ + "." + self.os_const] = func

    def thunk(*args, **kwargs):
      fn_id_base = func.__module__ + "." + func.__name__
      fn_id = fn_id_base + "." + OSCheck.get_os_family()
      if fn_id not in self._func_impls:
        fn_id = fn_id_base + "." + OsFamilyImpl.DEFAULT

      fn = self._func_impls[fn_id]
      return fn(*args, **kwargs)
    return thunk

  def __init__(self, os_family):
    self.os_const = os_family

  def __call__(self, func):
    return self._createFunctionInstance(func)
