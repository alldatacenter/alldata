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
import zipfile

def _zip_dir(zip, root):
  for dirname, dirnames, filenames in os.walk(root):
    for filename in filenames:
      if len(dirname) > len(root):
        rel_path = os.path.relpath(dirname, root)
        arch_name = rel_path + os.sep + filename
      else:
        arch_name = filename
      zip.write(os.path.join(dirname, filename), arch_name)


def archive_dir(output_filename, input_dir):
  zipf = zipfile.ZipFile(output_filename, 'w')
  try:
    _zip_dir(zipf, input_dir)
  finally:
    zipf.close()
