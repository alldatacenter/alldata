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
#* Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import inspect
import os

class ConvertToXml:
  "Template class, allow to output fields in xml format"
  def getField(self):
    return [name for name, obj in inspect.getmembers(self)
            if not name.startswith("__") and not inspect.isroutine(obj)]

  def attributesToXml(self):
    result = ""
    listOfAttr = self.getField()
    for attrName in listOfAttr:
      result += "<" + attrName + ">"
      result += str(getattr(self, attrName))
      result += "</" + attrName + ">\n"
    return result
