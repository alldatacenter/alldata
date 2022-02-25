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
"""
import os
import time
import threading
import collections
import traceback
from functools import wraps
from ambari_agent.ExitHelper import ExitHelper

AGENT_AUTO_RESTART_EXIT_CODE = 77

class BlockingDictionary():
  """
  A dictionary like class.
  Which allow putting an item. And retrieving it in blocking way (the caller is blocked until item is available).
  """
  def __init__(self, dictionary=None):
    self.dict = {} if dictionary is None else dictionary
    self.cv = threading.Condition()
    self.put_event = threading.Event()
    self.dict_lock = threading.RLock()

  def put(self, key, value):
    """
    Thread-safe put to dictionary.
    """
    with self.dict_lock:
      self.dict[key] = value
    self.put_event.set()

  def blocking_pop(self, key, timeout=None):
    """
    Block until a key in dictionary is available and than pop it.
    If timeout exceeded None is returned.
    """
    with self.dict_lock:
      if key in self.dict:
        return self.dict.pop(key)

    while True:
      self.put_event.wait(timeout)
      if not self.put_event.is_set():
        raise BlockingDictionary.DictionaryPopTimeout()

      self.put_event.clear()
      with self.dict_lock:
        if key in self.dict:
          return self.dict.pop(key)

  def __repr__(self):
    return self.dict.__repr__()

  def __str__(self):
    return self.dict.__str__()

  class DictionaryPopTimeout(Exception):
    pass

class Utils(object):
  @staticmethod
  def are_dicts_equal(d1, d2, keys_to_skip=[]):
    """
    Check if two dictionaries are equal. Comparing the nested dictionaries is done as well.
    """
    return Utils.are_dicts_equal_one_way(d1, d2, keys_to_skip) and Utils.are_dicts_equal_one_way(d2, d1, keys_to_skip)
  @staticmethod
  def are_dicts_equal_one_way(d1, d2, keys_to_skip=[]):
    """
    Check if d1 has all the same keys and their values as d2
    including nested dictionaries
    """
    for k in d1.keys():
      if k in keys_to_skip:
        #print "skipping " + str(k)
        continue
      if not d2.has_key(k):
        #print "don't have key="+str(k)
        return False
      else:
        if type(d1[k]) is dict:
          are_equal = Utils.are_dicts_equal_one_way(d1[k], d2[k], keys_to_skip)
          if not are_equal:
            return False
        else:
          if d1[k] != d2[k]:
            #print "not equal at "+str(k)
            return False
    return True

  @staticmethod
  def update_nested(d, u):
    """
    Update the dictionary 'd' and its sub-dictionaries with values of dictionary 'u' and its sub-dictionaries.
    """
    for k, v in u.iteritems():
      if isinstance(d, collections.Mapping):
        if isinstance(v, collections.Mapping):
          r = Utils.update_nested(d.get(k, {}), v)
          d[k] = r
        else:
          d[k] = u[k]
      else:
        d = {k: u[k]}
    return d

  @staticmethod
  def make_immutable(value):
    if isinstance(value, ImmutableDictionary):
      return value
    if isinstance(value, dict):
      return ImmutableDictionary(value)
    if isinstance(value, (list, tuple)):
      return tuple([Utils.make_immutable(x) for x in value])

    return value

  @staticmethod
  def get_mutable_copy(param):
    if isinstance(param, dict):
      mutable_dict = {}

      for k, v in param.iteritems():
        mutable_dict[k] = Utils.get_mutable_copy(v)

      return mutable_dict
    elif isinstance(param, (list, tuple)):
      return [Utils.get_mutable_copy(x) for x in param]

    return param

  @staticmethod
  def read_agent_version(config):
    data_dir = config.get('agent', 'prefix')
    ver_file = os.path.join(data_dir, 'version')
    with open(ver_file, "r") as f:
      version = f.read().strip()
    return version

  @staticmethod
  def restartAgent(stop_event, graceful_stop_timeout=30):
    ExitHelper().exitcode = AGENT_AUTO_RESTART_EXIT_CODE
    stop_event.set()

    t = threading.Timer( graceful_stop_timeout, ExitHelper().exit, [AGENT_AUTO_RESTART_EXIT_CODE])
    t.start()

  @staticmethod
  def get_traceback_as_text(ex):
    return ''.join(traceback.format_exception(etype=type(ex), value=ex, tb=ex.__traceback__))

class ImmutableDictionary(dict):
  def __init__(self, dictionary):
    """
    Recursively turn dict to ImmutableDictionary
    """
    if not isinstance(dictionary, ImmutableDictionary):
      for k, v in dictionary.iteritems():
        dictionary[k] = Utils.make_immutable(v)

    super(ImmutableDictionary, self).__init__(dictionary)

  def __getattr__(self, name):
    """
    Access to self['attribute'] as self.attribute
    """
    if name in self:
      return self[name]

    try:
      return self[name]
    except KeyError:
      raise AttributeError("'%s' object has no attribute '%s'" % (self.__class__.__name__, name))

def raise_immutable_error(*args, **kwargs):
  """
  PLEASE MAKE SURE YOU NEVER UPDATE CACHE on agent side. The cache should contain exactly the data received from server.
  Modifications on agent-side will lead to unnecessary cache sync every agent registration. Which is a big concern on perf clusters!
  Also immutability can lead to multithreading issues.
  """
  raise TypeError("The dictionary is immutable cannot change it")

ImmutableDictionary.__setitem__ = raise_immutable_error
ImmutableDictionary.__delitem__ = raise_immutable_error
ImmutableDictionary.clear = raise_immutable_error
ImmutableDictionary.pop = raise_immutable_error
ImmutableDictionary.update = raise_immutable_error


def lazy_property(undecorated):
  """
  Only run the function decorated once. Next time return cached value.
  """
  name = '_' + undecorated.__name__

  @property
  @wraps(undecorated)
  def decorated(self):
    try:
      return getattr(self, name)
    except AttributeError:
      v = undecorated(self)
      setattr(self, name, v)
      return v

  return decorated

def synchronized(lock):
    def wrap(f):
        def newFunction(*args, **kw):
            lock.acquire()
            try:
                return f(*args, **kw)
            finally:
                lock.release()
        return newFunction
    return wrap

def execute_with_retries(tries, try_sleep, retry_exception_class, func, *args, **kwargs):
  for i in range(tries):
    try:
      func(*args, **kwargs)
      break
    except retry_exception_class:
      if i==tries-1:
        raise
      time.sleep(try_sleep)