# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import os
from xml.etree import ElementTree as ET
from xml.parsers.expat import ExpatError

def write_properties_to_xml(xml_path, property_name='', property_value=''):
	if(os.path.isfile(xml_path)):
		try:
			xml = ET.parse(xml_path)
		except ExpatError:
			print("Error while parsing file:"+xml_path)
			return -1
		root = xml.getroot()
		for child in root.findall('property'):
			name = child.find("name").text.strip()
			if name == property_name:
				child.find("value").text = property_value
				break
		xml.write(xml_path)
		return 0
	else:
		return -1



if __name__ == '__main__':
	if(len(sys.argv) > 1):
		if(len(sys.argv) > 3):
			parameter_name = sys.argv[1] if len(sys.argv) > 1  else None
			parameter_value = sys.argv[2] if len(sys.argv) > 2  else None
			ranger_admin_site_xml_path = sys.argv[3] if len(sys.argv) > 3  else None
		else:
			if(len(sys.argv) > 2):
				parameter_name = sys.argv[1] if len(sys.argv) > 1  else None
				parameter_value = ""
				ranger_admin_site_xml_path = sys.argv[2] if len(sys.argv) > 2  else None
		write_properties_to_xml(ranger_admin_site_xml_path,parameter_name,parameter_value)
