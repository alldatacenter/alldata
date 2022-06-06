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

import time
import os
import resource_management
from resource_management.core.resources import File
from resource_management.core.providers import Provider
from resource_management.core.source import InlineTemplate
from resource_management.libraries.functions.format import format
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger
from resource_management.libraries.functions.is_empty import is_empty

class XmlConfigProvider(Provider):
  def action_create(self):
    filename = self.resource.filename
    xml_config_provider_config_dir = self.resource.conf_dir
    configuration_attrs = {} if is_empty(self.resource.configuration_attributes) else self.resource.configuration_attributes

    # |e - for html-like escaping of <,>,',"
    config_content = InlineTemplate('''  <configuration  xmlns:xi="http://www.w3.org/2001/XInclude">
    {% for key, value in configurations_dict|dictsort %}
    <property>
      <name>{{ key|e }}</name>
      <value>{{ resource_management.core.source.InlineTemplate(unicode(value)).get_content() |e }}</value>
      {%- if not configuration_attrs is none -%}
      {%- for attrib_name, attrib_occurances in  configuration_attrs.items() -%}
      {%- for property_name, attrib_value in  attrib_occurances.items() -%}
      {% if property_name == key and attrib_name %}
      <{{attrib_name|e}}>{{attrib_value|e}}</{{attrib_name|e}}>
      {%- endif -%}
      {%- endfor -%}
      {%- endfor -%}
      {%- endif %}
    </property>
    {% endfor %}
    {%- if not xml_include_file is none %}
    <xi:include href="{{xml_include_file|e}}"/>
    {% endif %}
  </configuration>''', extra_imports=[time, resource_management, resource_management.core, resource_management.core.source],
                                    configurations_dict=self.resource.configurations,
                                    configuration_attrs=configuration_attrs, xml_include_file=self.resource.xml_include_file)

    xml_config_dest_file_path = os.path.join(xml_config_provider_config_dir, filename)
    Logger.info("Generating config: {0}".format(xml_config_dest_file_path))

    File (xml_config_dest_file_path,
      content = config_content,
      owner = self.resource.owner,
      group = self.resource.group,
      mode = self.resource.mode,
      encoding = self.resource.encoding
    )
