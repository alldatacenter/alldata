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
import urllib2, base64, json, ssl, time, random, sys
from optparse import OptionParser
from contextlib import closing
  
class SslContext:
  def build(self, url):
    if not url.startswith('https') or not hasattr(ssl, 'SSLContext'):
      return None
    return ssl.SSLContext(self._protocol()) if self._protocol() else ssl.create_default_context()

  def _protocol(self):
    if hasattr(ssl, 'PROTOCOL_TLS'): return ssl.PROTOCOL_TLS
    elif hasattr(ssl, 'PROTOCOL_TLSv1_2'): return ssl.PROTOCOL_TLSv1_2
    elif hasattr(ssl, 'PROTOCOL_TLSv1_1'): return ssl.PROTOCOL_TLSv1_1
    elif hasattr(ssl, 'PROTOCOL_TLSv1'): return ssl.PROTOCOL_TLSv1
    else: return None

class PermissiveSslContext:
  def build(self, url):
    context = SslContext().build(url)
    if hasattr(context, '_https_verify_certificates'):
      context._https_verify_certificates(False)
    return context

class Url:
  @classmethod
  def base(clazz, protocol, host, port):
      return clazz('%s://%s:%d' % (protocol, host, port))

  def __init__(self, url_str):
    self.base = url_str.rstrip('/')

  def __div__(self, suffix_url):
    suffix_str = str(suffix_url)
    if self._is_absolute(suffix_str): 
      return Url(suffix_str)
    else:
      return Url(self.base + (suffix_str if suffix_str.startswith('/') else '/' + suffix_str))
  
  def _is_absolute(self, suffix_str):
    return suffix_str.startswith(self.base)

  def query_params(self, a_dict):
    return Url(self.base + '?' + '&'.join('%s=%s' % (name, value) for name, value in a_dict.items()))

  def __str__(self):
    return self.base

class Header:
  @classmethod
  def csrf(clazz):
    return clazz('X-Requested-By', 'ambari')

  def __init__(self, key, value):
    self.key, self.value = key, value

  def add_to(self, request):
    request.add_header(self.key, self.value)

class BasicAuth:
  def __init__(self, user, password):
    self.header = Header(
      'Authorization', 
      'Basic %s' % base64.encodestring('%s:%s' % (user, password)).replace('\n', ''))

  def authenticate(self, request):
    self.header.add_to(request)

class ResponseTransformer:
  @staticmethod
  def identity():
    return lambda url, code, data: (code, data)

  def __call__(self, url, code, data):
    raise RuntimeError('Subclass responsibility')

class UnexpectedHttpCode(Exception): pass

class JsonTransformer(ResponseTransformer):
  def __call__(self, url, code, data):
    if 200 <= code <= 299:
      return code, self._parse(data)
    else:
      return UnexpectedHttpCode('Unexpected http code: %d url: %s response: %s' % (code, url, data))

  def _parse(self, a_str):
    if not a_str: 
      return {}
    try:
      return json.loads(a_str)
    except ValueError as e:
      raise ValueError('Error %s while parsing: %s' % (e, a_str))

class RestClient:
  def __init__(self, an_url, authenticator, headers=[], ssl_context=SslContext(), request_transformer=lambda r:r, response_transformer=ResponseTransformer.identity()):
    self.base_url = an_url
    self.authenticator = authenticator
    self.headers = headers
    self.ssl_context = ssl_context
    self.request_transformer = request_transformer
    self.response_transformer = response_transformer

  def get(self, suffix_str):
    return self._response(*self._request(suffix_str, 'GET'))

  def post(self, suffix_str, data):
    return self._response(*self._request(suffix_str, 'POST', data=data))
    
  def put(self, suffix_str, data):
    return self._response(*self._request(suffix_str, 'PUT', data=data))
    
  def delete(self, suffix_str):
    return self._response(*self._request(suffix_str, 'DELETE'))
    
  def _request(self, suffix_str, http_method, data=""):
    url = str(self.base_url / suffix_str)
    request = urllib2.Request(url, data=self.request_transformer(data))
    request.get_method = lambda: http_method
    self.authenticator.authenticate(request)
    map(lambda each: each.add_to(request), self.headers)
    return request, self.ssl_context.build(url)

  def _response(self, request, ssl_context):
    with closing(urllib2.urlopen(request, context=ssl_context)) as response:
      return self.response_transformer(request.get_full_url(), response.getcode(), response.read())

  def rebased(self, new_base_url):
    return RestClient(
      new_base_url,
      self.authenticator,
      self.headers,
      self.ssl_context,
      self.request_transformer,
      self.response_transformer)

class ServiceComponent:
  def __init__(self, client, a_dict):
    self.client = client
    self.name = a_dict['ServiceComponentInfo']['component_name']
    self.component = a_dict

  def host_names(self):
    return [each['HostRoles']['host_name'] for each in self.component['host_components']]

  def __str__(self):
    return self.name

class Service:
  def __init__(self, client, a_dict):
    self.client = client
    self.service = a_dict
    self.href = self.service['href']
    self.name = self.service['ServiceInfo']['service_name']

  def delete(self):
    try:
      self.client.delete(self.href)
    except urllib2.HTTPError as e:
      if e.code != 404:
        raise e

  def start(self):
    _, data = self.client.put(self.href, {'ServiceInfo': {'state' : 'STARTED'}})
    return AsyncResult.of(self.client, data)

  def components(self):
    return [ServiceComponent(self.client, self.client.get(each['href'])[1]) for each in self.service['components']]

  def component(self, component_name):
    matches = [each for each in self.components() if each.name == 'HDFS_CLIENT']
    return matches[0] if matches else None

  def __str__(self):
    return self.name

class Cluster:
  def __init__(self, cluster_name, host, port=8080, protocol='http', user='admin', password='admin', api_version='v1'):
    self.cluster_name = cluster_name
    self.base_url = Url.base(protocol, host, port) / 'api' / api_version
    self.client = RestClient(
      self.base_url / 'clusters' / cluster_name,
      BasicAuth(user, password),
      headers=[Header.csrf()],
      ssl_context=PermissiveSslContext(),
      request_transformer=json.dumps,
      response_transformer=JsonTransformer())

  def version(self):
    _, data = self.client.get('')
    return data['Clusters']['version']

  def installed_stack(self):
    stack_name, stack_ver = cluster.version().split('-')
    return Stack(stack_name, stack_ver, self.client.rebased(self.base_url / 'stacks'))

  def add_service(self, service_name):
    self.client.post(Url('services') / service_name, {'ServiceInfo' : {'service_name' : service_name}})

  def add_service_component(self, service_name, component_name):
    self.client.post(Url('services') / service_name / 'components' / component_name, {})

  def add_host_component(self, service_name, component_name, host_name):
    self.client.post(
      Url('hosts').query_params({'Hosts/host_name': host_name}),
      {'host_components': [{'HostRoles': {'component_name': component_name}}]})
    _, data = self.client.put(Url('services') / service_name, {'ServiceInfo': {'state' : 'INSTALLED'}})
    return AsyncResult.of(self.client, data)

  def service(self, service_name):
    _, data = self.client.get(Url('services') / service_name)
    return Service(self.client, data)

  def services(self):
    _, data = self.client.get(Url('services'))
    return [Service(self.client, self.client.get(each['href'])[1]) for each in data['items']]

  def has_service(self, service_name):
    return service_name in [each.name for each in self.services()]

  def add_config(self, config_type, tag, properties):
    self.client.post(Url('configurations'), {
      'type': config_type,
      'tag': tag,
      'properties' : properties
    })
    self.client.put('', {
      'Clusters' : { 
        'desired_configs': {'type': config_type, 'tag' : tag }
      }
    })
  
  def config(self, config_type):
    code, data = self.client.get(Url('configurations').query_params({'type': config_type}))
    return Configs(self.client, [Config(self.client, each) for each in data['items']])

  def start_all(self):
    _, data = self.client.put('services', {
      'RequestInfo' : {
        'context' : '_PARSE_.START.ALL_SERVICES',
        'operation_level' : { 'level' : 'CLUSTER', 'cluster_name' : self.cluster_name }
      },
      'Body' : { 'ServiceInfo' : {'state' : 'STARTED'} }
    })
    return AsyncResult.of(self.client, data)

  def stop_all(self):
    _, data = self.client.put('services', {
      'RequestInfo' : {
        'context' : '_PARSE_.STOP.ALL_SERVICES',
        'operation_level' : { 'level' : 'CLUSTER', 'cluster_name' : self.cluster_name }
      },
      'Body' : { 'ServiceInfo' : {'state' : 'INSTALLED'} }
    })
    return AsyncResult.of(self.client, data)

  def __str__(self):
    return 'Cluster: %s (%s)' % (self.cluster_name, self.client.base_url)

class OperationFailed(Exception): pass

class AsyncResult:
  @staticmethod
  def of(client, data):
    return AsyncResult(client, data) if data else NoResult()

  def __init__(self, client, a_dict):
    self.client = client
    self.status = a_dict['Requests']['status']
    self.id = a_dict['Requests']['id']
    self.href = a_dict['href']

  def request_status(self):
    _, data = self.client.get(self.href)
    return data['Requests']['request_status']

  def is_finished(self):
    return self.request_status() in ['FAILED', 'TIMEDOUT', 'ABORTED', 'COMPLETED', 'SKIPPED_FAILED']

  def await(self):
    while not self.is_finished():
      time.sleep(1)
    status = self.request_status()
    if status != 'COMPLETED':
      raise OperationFailed("%s failed with status: %s" % (self.id, status))
    return status

  def __str__(self):
    return "Request status: %s id: %d" % (self.status, self.id)

class NoResult:
  def request_status(): return 'UNKNOWN'
  def is_finished(self): return True
  def await(self): pass

class Config:
  def __init__(self, client, a_dict):
    self.client = client
    self.config = a_dict
  
  def version(self):
    return int(self.config['version'])

  def href(self):
    return self.config['href']

  def properties(self):
    code, data = self.client.get(self.href())
    return data['items'][0]['properties']

  def __str__(self):
    return json.dumps(self.config)

class Configs:
  def __init__(self, client, config_list):
    self.client = client
    self.configs = sorted(config_list, key=lambda config: config.version())

  def latest(self):
    return self.configs[-1]


class Stack:
  def __init__(self, stack_name, stack_version, client):
    self.name = stack_name
    self.version = stack_version
    self.client = client

  def has_service(self, service_name):
    try:
      _, data = self.client.get(Url(self.name) / 'versions' / self.version / 'services' / service_name)
      return True
    except urllib2.HTTPError as e:
      if e.code == 404:
        return False
      else:
        raise e

class CannotLoad(Exception): pass

class FsStorage:
  def save(self, key, value):
    with open("saved-" + key, 'wt') as f:
      f.write(repr(value))

  def load(self, key):
    try:
      with open("saved-" + key, 'rt') as f:
        return eval(f.read())
    except IOError as e:
      raise CannotLoad(key + ' not found')

class Conversion:
  def __init__(self, cluster, storage):
    self.cluster = cluster
    self.storage = storage

  def check_prerequisites(self):
    print 'Checking %s' % self.cluster
    ver = self.cluster.version()
    print 'Found stack %s' % ver
    if not ver.startswith('HDP-3.'):
      print 'Only HDP-3.x stacks are supported.'
      return False
    if not self.cluster.installed_stack().has_service('ONEFS'):
      print 'ONEFS management pack is not installed.'
      return False
    sys.stdout.write('Please, confirm you have made backup of the Ambari db [y/n] (n)? ')
    if raw_input() != 'y':
      return False
    return True

  def perform(self):
    hdfs_client_hosts = self.find_hdfs_client_hosts()
    self.stop_all_services()
    self.read_configs()
    self.delete_hdfs()
    self.add_onefs()
    self.configure_onefs()
    self.install_onefs_clients(hdfs_client_hosts)
    self.start_all_services()

  def find_hdfs_client_hosts(self):
    if self.cluster.has_service('HDFS'):
      print 'Collecting hosts with HDFS_CLIENT'
      hdfs_client_hosts = self.cluster.service('HDFS').component('HDFS_CLIENT').host_names()
      self.storage.save('hdfs_client_hosts', hdfs_client_hosts)
    else:
      print 'Using previously saved HDFS client hosts'
      hdfs_client_hosts = self.storage.load('hdfs_client_hosts')
    print 'Found hosts %s' % hdfs_client_hosts
    return hdfs_client_hosts

  def stop_all_services(self):
    print 'Stopping all services..' 
    self.cluster.stop_all().await()

  def read_configs(self):
    if self.cluster.has_service('HDFS'):
      print 'Downloading core-site..'
      self.core_site = self.cluster.config('core-site').latest().properties()
      print 'Downloading hdfs-site..'
      self.hdfs_site = self.cluster.config('hdfs-site').latest().properties()
      print 'Downloading hadoop-env..'
      self.hadoop_env = self.cluster.config('hadoop-env').latest().properties()
      self.storage.save('core-site', self.core_site)
      self.storage.save('hdfs-site', self.hdfs_site)
      self.storage.save('hadoop-env', self.hadoop_env)
    else:
      print 'Using previously saved HDFS configs'
      self.core_site = self.storage.load('core-site')
      self.hdfs_site = self.storage.load('hdfs-site')
      self.hadoop_env = self.storage.load('hadoop-env')

  def delete_hdfs(self):
    print 'Deleting HDFS..'
    if self.cluster.has_service('HDFS'):
      self.cluster.service('HDFS').delete()
    else:
      print 'Already deleted.'
  
  def add_onefs(self):
    print 'Adding ONEFS..'
    if self.cluster.has_service('ONEFS'):
      print 'Already added.'
    else:
      self.cluster.add_service('ONEFS')
    try:
      self.cluster.add_service_component('ONEFS', 'ONEFS_CLIENT')
    except urllib2.HTTPError as e:
      if e.code != 409:
        raise e

  def configure_onefs(self):
    print 'Adding ONEFS config..'
    self.cluster.add_config('onefs', random_tag('onefs'), { "onefs_host" : self.smart_connect_zone(self.core_site) })
    print 'Adding core-site'
    self.cluster.add_config('core-site', random_tag('new-core-site'), self.core_site)
    print 'Adding hdfs-site'
    self.cluster.add_config('hdfs-site', random_tag('new-hdfs-site'), self.hdfs_site)
    print 'Adding hadoop-env-site'
    self.cluster.add_config('hadoop-env', random_tag('new-hadoop-env'), self.hadoop_env)

  def smart_connect_zone(self, core_site):
    def_fs = core_site['fs.defaultFS']
    if '://' in def_fs:
      def_fs = def_fs.split('://')[1]
    if ':' in def_fs:
      def_fs = def_fs.split(':')[0]
    return def_fs

  def install_onefs_clients(self, hdfs_client_hosts):
    print 'Adding ONEFS_CLIENT to hosts: %s' % (hdfs_client_hosts)
    results = [self.add_onefs_client(each) for each in hdfs_client_hosts]
    for each in results:
      each.await() 

  def add_onefs_client(self, hostname):
    try:
      return self.cluster.add_host_component('ONEFS', 'ONEFS_CLIENT', hostname)
    except urllib2.HTTPError as e:
      if e.code == 409:
        print 'Already added to host %s' % hostname
        return NoResult() 
      else:
        raise e


  def start_all_services(self):
    print 'Starting all services..'
    self.cluster.start_all().await()

def random_tag(tag_name): return "%s-%s" % (tag_name, time.time())

class CommandLine:
  def __init__(self):
    self.parser = OptionParser()
    self.parser.add_option("-o", '--host', dest='host', help='Ambari server host', default='localhost')
    self.parser.add_option("-p", '--port', dest='port', help='Ambari server port', default='8080')
    self.parser.add_option("-c", '--cluster', dest='cluster_name', help='Cluster name')
    self.parser.add_option("-u", '--user', dest='admin_user', help='Admin user name', default='admin')
    self.parser.add_option("-k", '--password', dest='admin_pass', help='Admin user name', default='admin')
    self.parser.add_option("-t", '--protocol', dest='protocol', help='HTTP protocol', default='http')

  def parse_options(self):
    options, args = self.parser.parse_args()
    if not options.cluster_name:
      self.parser.error('Missing cluster name.')
    if not options.protocol or options.protocol.lower() not in ['http', 'https']:
      self.parser.error('Invalid protocol. Use http or https.')
    if not options.port or not options.port.isdigit():
      self.parser.error('Port should be an integer')
    return options

if __name__ == '__main__':
  options = CommandLine().parse_options()
  cluster = Cluster(
    options.cluster_name,
    options.host,
    port=int(options.port),
    protocol=options.protocol.lower(),
    user=options.admin_user,
    password=options.admin_pass)
  print 'This script will replace the HDFS service to ONEFS'
  print 'The following prerequisites are required:'
  print '  * ONEFS management package must be installed'
  print '  * Ambari must be upgraded to >=v2.7.1'
  print '  * Stack must be upgraded to >=HDP-3.0'
  print '  * Is highly recommended to backup ambari database before you proceed.'
  conversion = Conversion(cluster, FsStorage())
  if not conversion.check_prerequisites():
    sys.exit()
  else:
    conversion.perform()
