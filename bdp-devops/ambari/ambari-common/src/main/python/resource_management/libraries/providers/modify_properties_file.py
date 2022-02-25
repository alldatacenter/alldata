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

from resource_management.core.resources import File
from resource_management.core.providers import Provider
from resource_management.libraries.functions.format import format
from resource_management.core.environment import Environment
from resource_management.core.source import InlineTemplate
from resource_management.core.logger import Logger
from resource_management.core import sudo


class ModifyPropertiesFileProvider(Provider):
  def action_create(self):
    filename = self.resource.filename
    comment_symbols = self.resource.comment_symbols
    delimiter = self.resource.key_value_delimiter
    properties = self.resource.properties
    unsaved_values = properties.keys()
    new_content_lines = []
    final_content_lines = ""
    
    if sudo.path_isfile(filename):
      file_content = sudo.read_file(filename, encoding=self.resource.encoding)
      new_content_lines += file_content.split('\n')

      Logger.info(format("Modifying existing properties file: {filename}"))
      
      for line_num in range(len(new_content_lines)):
        line = new_content_lines[line_num]
        
        if line.lstrip() and not line.lstrip()[0] in comment_symbols and delimiter in line:
          in_var_name = line.split(delimiter)[0].strip()
          in_var_value = line.split(delimiter)[1].strip()
          
          if in_var_name in properties:
            value = InlineTemplate(unicode(properties[in_var_name])).get_content()
            new_content_lines[line_num] = u"{0}{1}{2}".format(unicode(in_var_name), delimiter, value)
            unsaved_values.remove(in_var_name)
    else:
      Logger.info(format("Creating new properties file as {filename} doesn't exist"))
       
    for property_name in unsaved_values:
      value = InlineTemplate(unicode(properties[property_name])).get_content()
      line = u"{0}{1}{2}".format(unicode(property_name), delimiter, value)
      new_content_lines.append(line)

    final_content_lines = u"\n".join(new_content_lines)
    if not final_content_lines.endswith("\n"):
      final_content_lines = final_content_lines + "\n"

    File (filename,
          content = final_content_lines,
          owner = self.resource.owner,
          group = self.resource.group,
          mode = self.resource.mode,
          encoding = self.resource.encoding,
    )
