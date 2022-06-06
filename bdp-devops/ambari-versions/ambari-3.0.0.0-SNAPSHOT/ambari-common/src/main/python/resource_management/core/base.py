#!/usr/bin/env python
"""
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

Ambari Agent

"""

__all__ = ["Resource", "ResourceArgument", "ForcedListArgument",
           "BooleanArgument"]

from resource_management.core.exceptions import Fail, InvalidArgument
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger
from resource_management.core.utils import PasswordString

class ResourceArgument(object):
  def __init__(self, default=None, required=False):
    self.required = False # Prevents the initial validate from failing
    if hasattr(default, '__call__'):
      self.default = default
    else:
      self.default = self.validate(default)
    self.required = required

  def validate(self, value):
    if self.required and value is None:
      raise InvalidArgument("Required argument %s missing" % self.name)
    return value

class ForcedListArgument(ResourceArgument):
  def validate(self, value):
    value = super(ForcedListArgument, self).validate(value)
    if not isinstance(value, (tuple, list)):
      value = [value]
    return value


class BooleanArgument(ResourceArgument):
  def validate(self, value):
    value = super(BooleanArgument, self).validate(value)
    if not value in (True, False):
      raise InvalidArgument(
        "Expected a boolean for %s received %r" % (self.name, value))
    return value

class IntegerArgument(ResourceArgument):
  def validate(self, value):
    if value is None:
      return value

    value = super(IntegerArgument, self).validate(value)
    if not isinstance( value, int ):
      raise InvalidArgument(
        "Expected an integer for %s received %r" % (self.name, value))
    return value


class PasswordArgument(ResourceArgument):
  def log_str(self, key, value):
    # Hide the passwords from text representations
    return repr(PasswordString(value))


class Accessor(object):
  def __init__(self, name):
    self.name = name

  def __get__(self, obj, cls):
    try:
      return obj.arguments[self.name]
    except KeyError:
      val = obj._arguments[self.name].default
      if hasattr(val, '__call__'):
        val = val(obj)
      return val

  def __set__(self, obj, value):
    obj.arguments[self.name] = obj._arguments[self.name].validate(value)


class ResourceMetaclass(type):
  # def __new__(cls, name, bases, attrs):
  #     super_new = super(ResourceMetaclass, cls).__new__
  #     return super_new(cls, name, bases, attrs)

  def __init__(mcs, _name, bases, attrs):
    mcs._arguments = getattr(bases[0], '_arguments', {}).copy()
    for key, value in list(attrs.items()):
      if isinstance(value, ResourceArgument):
        value.name = key
        mcs._arguments[key] = value
        setattr(mcs, key, Accessor(key))
  
  
class Resource(object):
  __metaclass__ = ResourceMetaclass

  action = ForcedListArgument(default="nothing")
  ignore_failures = BooleanArgument(default=False)
  not_if = ResourceArgument() # pass command e.g. not_if = ('ls','/root/jdk')
  only_if = ResourceArgument() # pass command
  initial_wait = ResourceArgument() # in seconds

  actions = ["nothing"]
  
  def __new__(cls, name, env=None, provider=None, **kwargs):
    if isinstance(name, list):
      names_list = name[:]
      while len(names_list) != 1:
        cls(names_list.pop(0), env, provider, **kwargs)
        
      name = names_list[0]
    
    env = env or Environment.get_instance()
    provider = provider or getattr(cls, 'provider', None)
    
    r_type = cls.__name__
    if r_type not in env.resources:
      env.resources[r_type] = {}

    obj = super(Resource, cls).__new__(cls)
    env.resources[r_type][name] = obj
    env.resource_list.append(obj)
    return obj

  def __init__(self, name, env=None, provider=None, **kwargs):
    if isinstance(name, list):
      name = name[-1]
    
    if hasattr(self, 'name'):
      return

    self.env = env or Environment.get_instance()
    self.name = name
     
    self.provider = provider or getattr(self, 'provider', None)

    self.arguments = {}
    for key, value in kwargs.items():
      try:
        arg = self._arguments[key]
      except KeyError:
        raise Fail("%s received unsupported argument %s" % (self, key))
      else:
        try:
          self.arguments[key] = arg.validate(value)
        except InvalidArgument, exc:
          raise InvalidArgument("%s %s" % (self, exc))
    
    if not self.env.test_mode:
      self.env.run()

  def validate(self):
    pass

  def __repr__(self):
    return unicode(self)

  def __unicode__(self):
    return u"%s[%s]" % (self.__class__.__name__, Logger._get_resource_name_repr(self.name))

  def __getstate__(self):
    return dict(
      name=self.name,
      provider=self.provider,
      arguments=self.arguments,
      env=self.env,
    )

  def __setstate__(self, state):
    self.name = state['name']
    self.provider = state['provider']
    self.arguments = state['arguments']
    self.env = state['env']

    self.validate()
