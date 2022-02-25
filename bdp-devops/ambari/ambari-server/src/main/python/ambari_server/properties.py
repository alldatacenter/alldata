#!/usr/bin/env python

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

import os
import re
import time

#Apache License Header
ASF_LICENSE_HEADER = '''
# Copyright 2011 The Apache Software Foundation
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
'''

# A Python replacement for java.util.Properties
# Based on http://code.activestate.com/recipes
# /496795-a-python-replacement-for-javautilproperties/
class Properties(object):
  def __init__(self, props=None):
    self._props = {}
    self._origprops = {}
    self._keymap = {}

    self.othercharre = re.compile(r'(?<!\\)(\s*\=)|(?<!\\)(\s*\:)')
    self.othercharre2 = re.compile(r'(\s*\=)|(\s*\:)')
    self.bspacere = re.compile(r'\\(?!\s$)')

  def __parse(self, lines):
    lineno = 0
    i = iter(lines)
    for line in i:
      lineno += 1
      line = line.strip()
      if not line:
        continue
      if line[0] == '#':
        continue
      escaped = False
      sepidx = -1
      flag = 0
      m = self.othercharre.search(line)
      if m:
        first, last = m.span()
        start, end = 0, first
        flag = 1
        wspacere = re.compile(r'(?<![\\\=\:])(\s)')
      else:
        if self.othercharre2.search(line):
          wspacere = re.compile(r'(?<![\\])(\s)')
        start, end = 0, len(line)
      m2 = wspacere.search(line, start, end)
      if m2:
        first, last = m2.span()
        sepidx = first
      elif m:
        first, last = m.span()
        sepidx = last - 1
      while line[-1] == '\\':
        nextline = i.next()
        nextline = nextline.strip()
        lineno += 1
        line = line[:-1] + nextline
      if sepidx != -1:
        key, value = line[:sepidx], line[sepidx + 1:]
      else:
        key, value = line, ''
      self.process_pair(key, value)

  def process_pair(self, key, value):
    """
    Adds or overrides the property with the given key.
    """
    oldkey = key
    oldvalue = value
    keyparts = self.bspacere.split(key)
    strippable = False
    lastpart = keyparts[-1]
    if lastpart.find('\\ ') != -1:
      keyparts[-1] = lastpart.replace('\\', '')
    elif lastpart and lastpart[-1] == ' ':
      strippable = True
    key = ''.join(keyparts)
    if strippable:
      key = key.strip()
      oldkey = oldkey.strip()
    oldvalue = self.unescape(oldvalue)
    value = self.unescape(value)
    self._props[key] = None if value is None else value.strip()
    if self._keymap.has_key(key):
      oldkey = self._keymap.get(key)
      self._origprops[oldkey] = None if oldvalue is None else oldvalue.strip()
    else:
      self._origprops[oldkey] = None if oldvalue is None else oldvalue.strip()
      self._keymap[key] = oldkey

  def unescape(self, value):
    newvalue = value
    if not value is None:
      newvalue = value.replace('\:', ':')
      newvalue = newvalue.replace('\=', '=')
    return newvalue

  def removeOldProp(self, key):
    if self._origprops.has_key(key):
      del self._origprops[key]
    pass

  def removeProp(self, key):
    if self._props.has_key(key):
      del self._props[key]
    pass

  def load(self, stream):
    if type(stream) is not file:
      raise TypeError, 'Argument should be a file object!'
    if stream.mode != 'r':
      raise ValueError, 'Stream should be opened in read-only mode!'
    try:
      self.fileName = os.path.abspath(stream.name)
      lines = stream.readlines()
      self.__parse(lines)
    except IOError:
      raise

  def get_property(self, key):
    return self._props.get(key, '')

  def propertyNames(self):
    return self._props.keys()

  def getPropertyDict(self):
    return self._props

  def __getitem__(self, name):
    return self.get_property(name)

  def __getattr__(self, name):
    try:
      return self.__dict__[name]
    except KeyError:
      if hasattr(self._props, name):
        return getattr(self._props, name)

  def sort_props(self):
    tmp_props = {}
    for key in sorted(self._props.iterkeys()):
      tmp_props[key] = self._props[key]
    self._props = tmp_props
    pass

  def sort_origprops(self):
    tmp_props = self._origprops.copy()
    self._origprops.clear()
    for key in sorted(tmp_props.iterkeys()):
      self._origprops[key] = tmp_props[key]
    pass

  def store(self, out, header=""):
    """ Write the properties list to the stream 'out' along
    with the optional 'header'
    This function will attempt to close the file handler once it's done.
    """
    if out.mode[0] != 'w':
      raise ValueError, 'Steam should be opened in write mode!'
    try:
      out.write(''.join(('#', ASF_LICENSE_HEADER, '\n')))
      out.write(''.join(('#', header, '\n')))
      # Write timestamp
      tstamp = time.strftime('%a %b %d %H:%M:%S %Z %Y', time.localtime())
      out.write(''.join(('#', tstamp, '\n')))
      # Write properties from the pristine dictionary
      for prop, val in self._origprops.items():
        if val is not None:
          out.write(''.join((prop, '=', val, '\n')))
    except IOError:
      raise
    finally:
      if out:
        out.close()

  def store_ordered(self, out, header=""):
    """ Write the properties list to the stream 'out' along
    with the optional 'header' """
    if out.mode[0] != 'w':
      raise ValueError, 'Steam should be opened in write mode!'
    try:
      out.write(''.join(('#', ASF_LICENSE_HEADER, '\n')))
      out.write(''.join(('#', header, '\n')))
      # Write timestamp
      tstamp = time.strftime('%a %b %d %H:%M:%S %Z %Y', time.localtime())
      out.write(''.join(('#', tstamp, '\n')))
      # Write properties from the pristine dictionary
      for key in sorted(self._origprops.iterkeys()):
        val = self._origprops[key]
        if val is not None:
          out.write(''.join((key, '=', val, '\n')))
      out.close()
    except IOError:
      raise
