#!/usr/bin/python

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

import urllib2
import json
import sys
import base64

try:
  host = sys.argv[1]
  port = sys.argv[2]
  cluster = sys.argv[3]
  protocol = sys.argv[4]
  login = sys.argv[5]
  password = base64.b64decode(sys.argv[6])
  name = sys.argv[7]
  alerts_url = 'api/v1/clusters/{0}/alerts?fields=Alert/label,Alert/service_name,Alert/name,Alert/text,Alert/state&Alert/name={1}'.format(cluster, name)
  url = '{0}://{1}:{2}/{3}'.format(protocol, host, port, alerts_url)
  admin_auth = base64.encodestring('%s:%s' % (login, password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')
  response = urllib2.urlopen(request)
  response_body = response.read()
  alert = json.loads(response_body)['items'][0]
  state = alert['Alert']['state']
  text = alert['Alert']['text']
except Exception as exc:
  text = 'Unable to retrieve alert info: %s' % exc
  state = 'UNKNOWN'
finally:
  print text
  exit_code = {
    'OK': 0,
    'WARNING': 1,
    'CRITICAL': 2,
    'UNKNOWN': 3,
  }.get(state, 3)
  sys.exit(exit_code)
