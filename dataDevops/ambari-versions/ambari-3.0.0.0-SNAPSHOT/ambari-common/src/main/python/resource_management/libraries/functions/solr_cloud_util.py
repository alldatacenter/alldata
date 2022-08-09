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
import json
import random
from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_jinja2 import Environment as JinjaEnvironment
from random import randrange
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.source import StaticFile
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.format import format

__all__ = ["upload_configuration_to_zk", "create_collection", "setup_kerberos", "set_cluster_prop",
           "setup_kerberos_plugin", "create_znode", "check_znode", "secure_solr_znode", "secure_znode"]

def __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts=None, jaas_file=None, separated_znode=False):
  sudo = AMBARI_SUDO_BINARY

  infra_solr_cli_opts= format(' INFRA_SOLR_CLI_OPTS="{java_opts}"') if java_opts is not None else ''
  solr_cli_prefix = format('{sudo} JAVA_HOME={java64_home}{infra_solr_cli_opts} /usr/lib/ambari-infra-solr-client/solrCloudCli.sh ' \
                           '--zookeeper-connect-string {zookeeper_quorum}')
  if separated_znode:
    solr_cli_prefix+=format(' --znode {solr_znode}')
  else:
    solr_cli_prefix+=format('{solr_znode}')

  if jaas_file:
    solr_cli_prefix+=format(' --jaas-file {jaas_file}')

  return solr_cli_prefix

def __append_flags_if_exists(command, flagsDict):
  for key, value in flagsDict.iteritems():
    if value is not None:
        command+= " %s %s" % (key, value)
  return command

def upload_configuration_to_zk(zookeeper_quorum, solr_znode, config_set, config_set_dir, tmp_dir,
                         java64_home, retry = 5, interval = 10, solrconfig_content = None, jaas_file=None, java_opts=None):
  """
  Upload configuration set to zookeeper with solrCloudCli.sh
  At first, it tries to download configuration set if exists into a temporary location, then upload that one to
  zookeeper. If the configuration set does not exist in zookeeper then upload it based on the config_set_dir parameter.
  """
  random_num = random.random()
  tmp_config_set_dir = format('{tmp_dir}/solr_config_{config_set}_{random_num}')
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file)
  Execute(format('{solr_cli_prefix} --download-config --config-dir {tmp_config_set_dir} --config-set {config_set} --retry {retry} --interval {interval}'),
          only_if=format("{solr_cli_prefix} --check-config --config-set {config_set} --retry {retry} --interval {interval}"))

  if solrconfig_content is not None:
      File(format("{tmp_config_set_dir}/solrconfig.xml"),
       content=solrconfig_content,
       only_if=format("test -d {tmp_config_set_dir}")
      )
      upload_tmp_config_cmd = format('{solr_cli_prefix} --upload-config --config-dir {tmp_config_set_dir} --config-set {config_set} --retry {retry} --interval {interval}')
      Execute(upload_tmp_config_cmd,
        only_if=format("test -d {tmp_config_set_dir}")
      )
  upload_config_cmd = format('{solr_cli_prefix} --upload-config --config-dir {config_set_dir} --config-set {config_set} --retry {retry} --interval {interval}')
  Execute(upload_config_cmd,
    not_if=format("test -d {tmp_config_set_dir}")
  )

  Directory(tmp_config_set_dir,
              action="delete",
              create_parents=True
            )

def create_collection(zookeeper_quorum, solr_znode, collection, config_set, java64_home,
                      shards = 1, replication_factor = 1, max_shards = 1, retry = 5, interval = 10, implicitRouting = False,
                      router_name = None, router_field = None, jaas_file = None, key_store_location = None,
                      key_store_password = None, key_store_type = None, trust_store_location = None,
                      trust_store_password = None, trust_store_type = None, java_opts=None):
  """
  Create Solr collection based on a configuration set in zookeeper.
  If this method called again the with higher shard number (or max_shard number), then it will indicate
  the cli tool to add new shards to the Solr collection. This can be useful after added a new Solr Cloud
  instance to the cluster.

  If you would like to add shards later to a collection, then use implicit routing, e.g.:
  router_name = "implicit", router_field = "_router_field_"
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file)

  if max_shards == 1: # if max shards is not specified use this strategy
    max_shards = int(replication_factor) * int(shards)

  create_collection_cmd = format('{solr_cli_prefix} --create-collection --collection {collection} --config-set {config_set} '\
                                 '--shards {shards} --replication {replication_factor} --max-shards {max_shards} --retry {retry} '\
                                 '--interval {interval}')

  create_collection_cmd = create_collection_cmd + ' --implicit-routing' if implicitRouting else create_collection_cmd
  appendableDict = {}
  appendableDict["--router-name"] = router_name
  appendableDict["--router-field"] = router_field
  appendableDict["--key-store-location"] = key_store_location
  appendableDict["--key-store-password"] = None if key_store_password is None else '{key_store_password_param!p}'
  appendableDict["--key-store-type"] = key_store_type
  appendableDict["--trust-store-location"] = trust_store_location
  appendableDict["--trust-store-password"] = None if trust_store_password is None else '{trust_store_password_param!p}'
  appendableDict["--trust-store-type"] = trust_store_type
  create_collection_cmd = __append_flags_if_exists(create_collection_cmd, appendableDict)
  create_collection_cmd = format(create_collection_cmd, key_store_password_param=key_store_password, trust_store_password_param=trust_store_password)

  Execute(create_collection_cmd)

def setup_kerberos(zookeeper_quorum, solr_znode, copy_from_znode, java64_home, secure=False, jaas_file=None, java_opts=None):
  """
  Copy all unsecured (or secured) Znode content to a secured (or unsecured) Znode,
  and restrict the world permissions there.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True)
  setup_kerberos_cmd = format('{solr_cli_prefix} --setup-kerberos --copy-from-znode {copy_from_znode}')
  if secure and jaas_file is not None:
    setup_kerberos_cmd+=format(' --secure --jaas-file {jaas_file}')
  Execute(setup_kerberos_cmd)

def check_znode(zookeeper_quorum, solr_znode, java64_home, retry = 5, interval = 10, java_opts=None, jaas_file=None):
  """
  Check znode exists or not, throws exception if does not accessible.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True)
  check_znode_cmd = format('{solr_cli_prefix} --check-znode --retry {retry} --interval {interval}')
  Execute(check_znode_cmd)

def create_znode(zookeeper_quorum, solr_znode, java64_home, retry = 5 , interval = 10, java_opts=None, jaas_file=None):
  """
  Create znode if does not exists, throws exception if zookeeper is not accessible.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True)
  create_znode_cmd = format('{solr_cli_prefix} --create-znode --retry {retry} --interval {interval}')
  Execute(create_znode_cmd)

def setup_kerberos_plugin(zookeeper_quorum, solr_znode, java64_home, secure=False, security_json_location = None, jaas_file = None, java_opts=None):
  """
  Set Kerberos plugin on the Solr znode in security.json, if secure is False, then clear the security.json
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True)
  setup_kerberos_plugin_cmd = format('{solr_cli_prefix} --setup-kerberos-plugin')
  if secure and jaas_file is not None and security_json_location is not None:
    setup_kerberos_plugin_cmd+=format(' --jaas-file {jaas_file} --secure --security-json-location {security_json_location}')
  Execute(setup_kerberos_plugin_cmd)

def set_cluster_prop(zookeeper_quorum, solr_znode, prop_name, prop_value, java64_home, jaas_file = None, java_opts=None):
  """
  Set a cluster property on the Solr znode in clusterprops.json
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file)
  set_cluster_prop_cmd = format('{solr_cli_prefix} --cluster-prop --property-name {prop_name} --property-value {prop_value}')
  Execute(set_cluster_prop_cmd)

def set_autoscaling_props(zookeeper_quorum, solr_znode, autoscaling_json_file_location, java64_home, jaas_file = None, java_opts=None):
  """
  Set a cluster property on the Solr znode in autoscaling.json
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True)
  set_cluster_prop_cmd = format('{solr_cli_prefix} --set-autoscaling --autoscaling-json-location {autoscaling_json_file_location}')
  Execute(set_cluster_prop_cmd)

def secure_znode(config, zookeeper_quorum, solr_znode, jaas_file, java64_home, sasl_users=[], retry = 5 , interval = 10, java_opts=None):
  """
  Secure znode, set a list of sasl users acl to 'cdrwa', and set acl to 'r' only for the world.
  Add infra-solr user by default if its available.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True)
  if "infra-solr-env" in config['configurations']:
    sasl_users.append(__get_name_from_principal(config['configurations']['infra-solr-env']['infra_solr_kerberos_principal']))
  sasl_users_str = ",".join(str(__get_name_from_principal(x)) for x in sasl_users)
  secure_znode_cmd = format('{solr_cli_prefix} --secure-znode --sasl-users {sasl_users_str} --retry {retry} --interval {interval}')
  Execute(secure_znode_cmd)


def secure_solr_znode(zookeeper_quorum, solr_znode, jaas_file, java64_home, sasl_users_str='', java_opts=None):
  """
  Secure solr znode - setup acls to 'cdrwa' for solr user, set 'r' only for the world, skipping /znode/configs and znode/collections (set those to 'cr' for the world)
  sasl_users_str: comma separated sasl users
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file, True)
  secure_solr_znode_cmd = format('{solr_cli_prefix} --secure-solr-znode --sasl-users {sasl_users_str}')
  Execute(secure_solr_znode_cmd)

def remove_admin_handlers(zookeeper_quorum, solr_znode, java64_home, collection, jaas_file, retry = 5, interval = 10, java_opts=None):
  """
  Remove "solr.admin.AdminHandlers" request handler from collection config. Required for migrating to Solr 6 from Solr 5.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file)
  remove_admin_handlers_cmd = format('{solr_cli_prefix} --remove-admin-handlers --collection {collection} --retry {retry} --interval {interval}')
  Execute(remove_admin_handlers_cmd)

def copy_solr_znode(zookeeper_quorum, solr_znode, java64_home, jaas_file, src_znode, target_znode, retry = 5, interval = 10, java_opts=None):
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file)
  copy_znode_cmd = format("{solr_cli_prefix} --transfer-znode --copy-src {src_znode} --copy-dest {target_znode} --retry {retry} --interval {interval}")
  Execute(copy_znode_cmd)

def copy_solr_znode_to_locat(zookeeper_quorum, solr_znode, java64_home, jaas_file, src_znode, target, retry = 5, interval = 10, java_opts=None):
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file)
  copy_znode_cmd = format("{solr_cli_prefix} --transfer-znode --transfer-mode copyToLocal --copy-src {src_znode} --copy-dest {target} --retry {retry} --interval {interval}")
  Execute(copy_znode_cmd)

def copy_solr_znode_from_local(zookeeper_quorum, solr_znode, java64_home, jaas_file, src, target_znode, retry = 5, interval = 10, java_opts=None):
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, java_opts, jaas_file)
  copy_znode_cmd = format("{solr_cli_prefix} --transfer-znode --transfer-mode copyFromLocal --copy-src {src} --copy-dest {target_znode} --retry {retry} --interval {interval}")
  Execute(copy_znode_cmd)

def default_config(config, name, default_value):
  subdicts = filter(None, name.split('/'))
  if not config:
    return default_value
  for x in subdicts:
    if x in config:
      config = config[x]
    else:
      return default_value
  return config

def setup_solr_client(config, custom_log4j = True, custom_log_location = None, log4jcontent = None):
    solr_client_dir = '/usr/lib/ambari-infra-solr-client'
    solr_client_log_dir = default_config(config, '/configurations/infra-solr-client-log4j/infra_solr_client_log_dir', '/var/log/ambari-infra-solr-client') if custom_log_location is None else custom_log_location
    solr_client_log = format("{solr_client_log_dir}/solr-client.log")
    solr_client_log_maxfilesize =  default_config(config, 'configurations/infra-solr-client-log4j/infra_client_log_maxfilesize', 80)
    solr_client_log_maxbackupindex =  default_config(config, 'configurations/infra-solr-client-log4j/infra_client_log_maxbackupindex', 60)

    Directory(solr_client_log_dir,
                mode=0755,
                cd_access='a',
                create_parents=True
                )
    Directory(solr_client_dir,
                mode=0755,
                cd_access='a',
                create_parents=True,
                recursive_ownership=True
                )
    solrCliFilename = format("{solr_client_dir}/solrCloudCli.sh")
    File(solrCliFilename,
         mode=0755,
         content=StaticFile(solrCliFilename)
         )
    if custom_log4j:
      # use custom log4j content only, when infra is not installed on the cluster
      solr_client_log4j_content = config['configurations']['infra-solr-client-log4j']['content'] if log4jcontent is None else log4jcontent
      context = {
        'solr_client_log': solr_client_log,
        'solr_client_log_maxfilesize': solr_client_log_maxfilesize,
        'solr_client_log_maxbackupindex': solr_client_log_maxbackupindex
      }
      template = JinjaEnvironment(
        line_statement_prefix='%',
        variable_start_string="{{",
        variable_end_string="}}")\
        .from_string(solr_client_log4j_content)

      File(format("{solr_client_dir}/log4j.properties"),
             content=template.render(context),
             mode=0644
             )
    else:
        File(format("{solr_client_dir}/log4j.properties"),
             mode=0644
             )

    File(solr_client_log,
         mode=0664,
         content=''
         )

def __get_name_from_principal(principal):
  if not principal:  # return if empty
    return principal
  slash_split = principal.split('/')
  if len(slash_split) == 2:
    return slash_split[0]
  else:
    at_split = principal.split('@')
    return at_split[0]

def __remove_host_from_principal(principal, realm):
  if not realm:
    raise Exception("Realm parameter is missing.")
  if not principal:
    raise Exception("Principal parameter is missing.")
  username=__get_name_from_principal(principal)
  at_split = principal.split('@')
  if len(at_split) == 2:
    realm = at_split[1]
  return format('{username}@{realm}')

def __get_random_solr_host(actual_host, solr_hosts = []):
  """
  Get a random solr host, use the actual one, if there is an installed infra solr there (helps blueprint installs)
  If there is only one solr host on the cluster, use that.
  """
  if not solr_hosts:
    raise Exception("Solr hosts parameter is empty.")
  if len(solr_hosts) == 1:
    return solr_hosts[0]
  if actual_host in solr_hosts:
    return actual_host
  else:
    random_index = randrange(0, len(solr_hosts))
    return solr_hosts[random_index]

def add_solr_roles(config, roles = [], new_service_principals = [], tries = 30, try_sleep = 10):
  """
  Set user-role mappings based on roles and principal users for secured cluster. Use solr REST API to check is there any authoirzation enabled,
  if it is then update the user-roles mapping for Solr (this will upgrade the solr_znode/security.json file).
  In case of custom security.json is used for infra-solr, this step will be skipped.
  """
  solr_hosts = default_config(config, "/clusterHostInfo/infra_solr_hosts", [])
  security_enabled = config['configurations']['cluster-env']['security_enabled']
  solr_ssl_enabled = default_config(config, 'configurations/infra-solr-env/infra_solr_ssl_enabled', False)
  solr_port = default_config(config, 'configurations/infra-solr-env/infra_solr_port', '8886')
  kinit_path_local = get_kinit_path(default_config(config, '/configurations/kerberos-env/executable_search_paths', None))
  infra_solr_custom_security_json_content = None
  infra_solr_security_manually_managed = False
  if 'infra-solr-security-json' in config['configurations']:
    infra_solr_custom_security_json_content = config['configurations']['infra-solr-security-json']['content']
    infra_solr_security_manually_managed = config['configurations']['infra-solr-security-json']['infra_solr_security_manually_managed']

  Logger.info(format("Adding {roles} roles to {new_service_principals} if infra-solr is installed."))
  if infra_solr_security_manually_managed:
    Logger.info("security.json file is manually managed, skip adding roles...")
  elif infra_solr_custom_security_json_content and str(infra_solr_custom_security_json_content).strip():
    Logger.info("Custom security.json is not empty for infra-solr, skip adding roles...")
  elif security_enabled \
    and "infra-solr-env" in config['configurations'] \
    and solr_hosts is not None \
    and len(solr_hosts) > 0:
    solr_protocol = "https" if solr_ssl_enabled else "http"
    hostname = config['agentLevelParams']['hostname'].lower()
    solr_host = __get_random_solr_host(hostname, solr_hosts)
    solr_url = format("{solr_protocol}://{solr_host}:{solr_port}/solr/admin/authorization")
    solr_user = config['configurations']['infra-solr-env']['infra_solr_user']
    solr_user_keytab = config['configurations']['infra-solr-env']['infra_solr_kerberos_keytab']
    solr_user_principal = config['configurations']['infra-solr-env']['infra_solr_kerberos_principal'].replace('_HOST', hostname)
    solr_user_kinit_cmd = format("{kinit_path_local} -kt {solr_user_keytab} {solr_user_principal};")
    solr_authorization_enabled_cmd=format("{solr_user_kinit_cmd} curl -k -s --negotiate -u : {solr_protocol}://{solr_host}:{solr_port}/solr/admin/authorization | grep authorization.enabled")

    if len(new_service_principals) > 0:
      new_service_users = []

      kerberos_realm = config['configurations']['kerberos-env']['realm']
      for new_service_user in new_service_principals:
        new_service_users.append(__remove_host_from_principal(new_service_user, kerberos_realm))
      user_role_map = {}

      for new_service_user in new_service_users:
        user_role_map[new_service_user] = roles

      Logger.info(format("New service users after removing fully qualified names: {new_service_users}"))

      set_user_role_map = {}
      set_user_role_map['set-user-role'] = user_role_map
      set_user_role_json = json.dumps(set_user_role_map)

      add_solr_role_cmd = format("{solr_user_kinit_cmd} curl -H 'Content-type:application/json' -d '{set_user_role_json}' -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k {solr_url} | grep 200")

      Logger.info(format("Check authorization enabled command: {solr_authorization_enabled_cmd} \nSet user-role settings command: {add_solr_role_cmd}"))
      Execute(solr_authorization_enabled_cmd + " && "+ add_solr_role_cmd,
              tries=tries,
              try_sleep=try_sleep,
              user=solr_user,
              logoutput=True)
