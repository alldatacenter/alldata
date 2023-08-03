#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# -*-coding: utf-8-*-

import json
import sys
import urllib2

URL_BASE = "http://%s:%s/metrics/server"

def usage():
  print """Usage: python get-metrics.py [serverListFile] [jettyPort] [metricName]
Description: Extract server ip from line in the serverListFile, which contains
    lines of server node and the format is $IP-$PORT. The ip and port are server's
    rpc ip and port respectively, the port is useless here so we only extract the
    ip and assemble the url with jetty http port to get metrics and extract the
    value of the metric specified by metricName."""

def httpGet(url):
  try:
    content = urllib2.urlopen(url).read()
  except Exception, e:
    print >> sys.stderr, e
    return None
  return content

def parseMetrics(jsonStr):
  ret = dict()
  jsonObj = json.loads(jsonStr)
  metrics = jsonObj.get("metrics", None)
  if metrics is None:
    print >> sys.stderr, "metrics is invalid, jsonStr is %s" % jsonStr
    return ret
  for item in metrics:
    name = item.get("name", None)
    if name is None:
      print >> sys.stderr, "%s is invalid, jsonStr is %s" % (str(item), jsonStr)
      continue
    value = item.get("value", None)
    if value is None:
      print >> sys.stderr, "%s is invliad, jsonStr is %s" % (str(item), jsonStr)
      continue
    ret[name] = value
  return ret

def getAllMetrics(url):
  content = httpGet(url)
  if content is None:
    return dict()
  return parseMetrics(content)

def getMetrics(url, name):
  metrics = getAllMetrics(url)
  val = metrics.get(name, None)
  if val is None:
    print >> sys.stderr, "Fail to get %s using %s, metrics is %s" % (name, url, metrics)
  return val

def assembleUrl(line, port):
  try:
    ip, _ = line.strip().split("-")
    return URL_BASE % (ip, port)
  except Exception, e:
    print >> sys.stderr, "fail to assemble %s" % line
    print >> sys.stderr, e

def main():
  args = sys.argv
  if len(args) != 4:
    usage()
    sys.exit(1)
  with open(args[1], 'rb') as f:
    for line in f:
      url = assembleUrl(line, args[2])
      m = getMetrics(url, args[3])
      print "Node %s , Metrcis[%s] %s" % (line.strip(), args[3], m)

if __name__ == '__main__':
  main()
