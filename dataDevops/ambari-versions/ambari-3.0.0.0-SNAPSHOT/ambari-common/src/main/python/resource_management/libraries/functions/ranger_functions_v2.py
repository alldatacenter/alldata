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
"""

import re
import time
import sys
import urllib2
import base64
import httplib
# simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import ambari_simplejson as json
from StringIO import StringIO as BytesIO
from ambari_commons.inet_utils import openurl
from resource_management.core.logger import Logger
from ambari_commons.exceptions import TimeoutError
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.decorator import safe_retry
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.core.environment import Environment

class RangeradminV2:
  sInstance = None

  def __init__(self, url='http://localhost:6080', skip_if_rangeradmin_down = True):
    if url.endswith('/'):
      url = url.rstrip('/')
    self.base_url = url
    self.url_login = self.base_url + '/login.jsp'
    self.url_login_post = self.base_url + '/j_spring_security_check'
    self.url_repos = self.base_url + '/service/assets/assets'
    self.url_repos_pub = self.base_url + '/service/public/v2/api/service'
    self.url_policies = self.base_url + '/service/public/v2/api/policy'
    self.url_policies_get = self.base_url + '/service/public/v2/api/service/{servicename}/policy'
    self.url_groups = self.base_url + '/service/xusers/groups'
    self.url_users = self.base_url + '/service/xusers/users'
    self.url_sec_users = self.base_url + '/service/xusers/secure/users'
    self.skip_if_rangeradmin_down = skip_if_rangeradmin_down

    if self.skip_if_rangeradmin_down:
      Logger.info("RangeradminV2: Skip ranger admin if it's down !")

  @safe_retry(times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None)
  def get_repository_by_name_urllib2(self, name, component, status, usernamepassword):
    """
    :param name: name of the component, from which, function will search in list of repositories
    :param component:, component for which repository has to be checked
    :param status: active or inactive
    :param usernamepassword: user credentials using which repository needs to be searched.
    :return: Returns Ranger repository object if found otherwise None
    """
    try:
      search_repo_url = self.url_repos_pub + "?name=" + name + "&type=" + component + "&status=" + status
      request = urllib2.Request(search_repo_url)
      base_64_string = base64.encodestring(usernamepassword).replace('\n', '')
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", "Basic {0}".format(base_64_string))
      result = openurl(request, timeout=20)
      response_code = result.getcode()
      response = json.loads(result.read())
      if response_code == 200 and len(response) > 0:
        for repo in response:
          repo_dump = json.loads(json.JSONEncoder().encode(repo))
          if repo_dump['name'].lower() == name.lower():
            return repo_dump
        return None
      else:
        return None
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        raise Fail("Error getting {0} repository for component {1}. Http status code - {2}. \n {3}".format(name, component, e.code, e.read()))
      else:
        raise Fail("Error getting {0} repository for component {1}. Reason - {2}.".format(name, component, e.reason))
    except httplib.BadStatusLine:
      raise Fail("Ranger Admin service is not reachable, please restart the service and then try again")
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")

  def create_ranger_repository(self, component, repo_name, repo_properties,
                               ambari_ranger_admin, ambari_ranger_password,
                               admin_uname, admin_password, policy_user, is_security_enabled = False, is_stack_supports_ranger_kerberos = False,
                               component_user = None, component_user_principal = None, component_user_keytab = None):
    if not is_stack_supports_ranger_kerberos or not is_security_enabled:
      response_code = self.check_ranger_login_urllib2(self.base_url)
      repo_data = json.dumps(repo_properties)
      ambari_ranger_password = unicode(ambari_ranger_password)
      admin_password = unicode(admin_password)
      ambari_username_password_for_ranger = format('{ambari_ranger_admin}:{ambari_ranger_password}')


      if response_code is not None and response_code == 200:
        user_resp_code = self.create_ambari_admin_user(ambari_ranger_admin, ambari_ranger_password, format("{admin_uname}:{admin_password}"))
        if user_resp_code is not None and user_resp_code == 200:
          retryCount = 0
          while retryCount <= 5:
            repo = self.get_repository_by_name_urllib2(repo_name, component, 'true', ambari_username_password_for_ranger)
            if repo is not None:
              Logger.info('{0} Repository {1} exist'.format(component.title(), repo['name']))
              break
            else:
              response = self.create_repository_urllib2(repo_data, ambari_username_password_for_ranger)
              if response is not None:
                Logger.info('{0} Repository created in Ranger admin'.format(component.title()))
                break
              else:
                if retryCount < 5:
                  Logger.info("Retry Repository Creation is being called")
                  time.sleep(30) # delay for 30 seconds
                  retryCount += 1
                else:
                  Logger.error('{0} Repository creation failed in Ranger admin'.format(component.title()))
                  break
        else:
          Logger.error('Ambari admin user creation failed')
      elif not self.skip_if_rangeradmin_down:
        Logger.error("Connection failed to Ranger Admin !")
    elif is_stack_supports_ranger_kerberos and is_security_enabled:
      response = self.check_ranger_login_curl(component_user,component_user_keytab,component_user_principal,self.url_login,True)

      if response and response[0] == 200:
        retryCount = 0
        repo_data = json.dumps(repo_properties)
        while retryCount <= 5:
          response = self.get_repository_by_name_curl(component_user,component_user_keytab,component_user_principal,repo_name, component, 'true')
          if response is not None:
            Logger.info('{0} Repository {1} exist'.format(component.title(), (response['name'])))
            break
          else:
            response = self.create_repository_curl(component_user,component_user_keytab,component_user_principal,repo_name, repo_data,policy_user)
            if response and len(response) > 0:
              Logger.info('{0} Repository created in Ranger admin'.format(component.title()))
              break
            else:
              if retryCount < 5:
                time.sleep(30) # delay for 30 seconds
                retryCount += 1
              else:
                Logger.error('{0} Repository creation failed in Ranger admin'.format(component.title()))
                break
      else:
        Logger.error("Connection failed to Ranger Admin !")

  @safe_retry(times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None)
  def create_repository_urllib2(self, data, usernamepassword):
    """
    :param data: json object to create repository
    :param usernamepassword: user credentials using which repository needs to be searched.
    :return: Returns created Ranger repository object
    """
    try:
      search_repo_url = self.url_repos_pub
      base_64_string = base64.encodestring('{0}'.format(usernamepassword)).replace('\n', '')
      headers = {
        'Accept': 'application/json',
        "Content-Type": "application/json"
      }
      request = urllib2.Request(search_repo_url, data, headers)
      request.add_header("Authorization", "Basic {0}".format(base_64_string))
      result = openurl(request, timeout=20)
      response_code = result.getcode()
      response = json.loads(json.JSONEncoder().encode(result.read()))

      if response_code == 200:
        Logger.info('Repository created Successfully')
        return response
      else:
        raise Fail('Repository creation failed')
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        raise Fail("Error creating repository. Http status code - {0}. \n {1}".format(e.code, e.read()))
      else:
        raise Fail("Error creating repository. Reason - {0}.".format(e.reason))
    except httplib.BadStatusLine:
      raise Fail("Ranger Admin service is not reachable, please restart the service and then try again")
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")

  @safe_retry(times=75, sleep_time=8, backoff_factor=1, err_class=Fail, return_on_fail=None)
  def check_ranger_login_urllib2(self, url):
    """
    :param url: ranger admin host url
    :param usernamepassword: user credentials using which repository needs to be searched.
    :return: Returns login check response
    """
    try:
      response = openurl(url, timeout=20)
      response_code = response.getcode()
      return response_code
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        raise Fail("Connection failed to Ranger Admin. Http status code - {0}. \n {1}".format(e.code, e.read()))
      else:
        raise Fail("Connection failed to Ranger Admin. Reason - {0}.".format(e.reason))
    except httplib.BadStatusLine, e:
      raise Fail("Ranger Admin service is not reachable, please restart the service and then try again")
    except TimeoutError:
      raise Fail("Connection failed to Ranger Admin. Reason - timeout")

  @safe_retry(times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None)
  def create_ambari_admin_user(self, ambari_admin_username, ambari_admin_password, usernamepassword):
    """
    :param ambari_admin_username: username of user to be created
    :param ambari_admin_password: user password of user to be created
    :param usernamepassword: user credentials using which repository needs to be searched.
    :return: Returns user credentials if user exist otherwise rerutns credentials of  created user.
    """
    flag_ambari_admin_present = False
    match = re.match('[a-zA-Z0-9_\S]+$', ambari_admin_password)
    if match is None:
      raise Fail('Invalid password given for Ranger Admin user for Ambari')
    try:
      url =  self.url_users + '?name=' + str(ambari_admin_username)
      request = urllib2.Request(url)
      base_64_string = base64.encodestring(usernamepassword).replace('\n', '')
      request.add_header("Content-Type", "application/json")
      request.add_header("Accept", "application/json")
      request.add_header("Authorization", "Basic {0}".format(base_64_string))
      result = openurl(request, timeout=20)
      response_code = result.getcode()
      response = json.loads(result.read())
      if response_code == 200 and len(response['vXUsers']) >= 0:
        for vxuser in response['vXUsers']:
          if vxuser['name'] == ambari_admin_username:
            flag_ambari_admin_present = True
            break
          else:
            flag_ambari_admin_present = False

        if flag_ambari_admin_present:
          Logger.info(ambari_admin_username + ' user already exists.')
          return response_code
        else:
          Logger.info(ambari_admin_username + ' user is not present, creating user using given configurations')
          url = self.url_sec_users
          admin_user = dict()
          admin_user['status'] = 1
          admin_user['userRoleList'] = ['ROLE_SYS_ADMIN']
          admin_user['name'] = ambari_admin_username
          admin_user['password'] = ambari_admin_password
          admin_user['description'] = ambari_admin_username
          admin_user['firstName'] = ambari_admin_username
          data =  json.dumps(admin_user)
          base_64_string = base64.encodestring('{0}'.format(usernamepassword)).replace('\n', '')
          headers = {
            'Accept': 'application/json',
            "Content-Type": "application/json"
          }
          request = urllib2.Request(url, data, headers)
          request.add_header("Authorization", "Basic {0}".format(base_64_string))
          result = openurl(request, timeout=20)
          response_code = result.getcode()
          response = json.loads(json.JSONEncoder().encode(result.read()))
          if response_code == 200 and response is not None:
            Logger.info('Ambari admin user creation successful.')
            return response_code
          else:
            Logger.error('Ambari admin user creation failed.')
            return None
      else:
        return None
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        raise Fail("Error creating ambari admin user. Http status code - {0}. \n {1}".format(e.code, e.read()))
      else:
        raise Fail("Error creating ambari admin user. Reason - {0}.".format(e.reason))
    except httplib.BadStatusLine:
      raise Fail("Ranger Admin service is not reachable, please restart the service and then try again")
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")

  def call_curl_request(self,user,keytab,principal, url, flag_http_response, request_method='GET',request_body='',header=''):
    """
    :param user: service user for which call is to be made
    :param keytab: keytab of service user
    :param principal: principal of service user
    :param url: url with which call is to be made
    :param flag_http_response: flag to get only response-code or response string
    :param request_method: http method (GET / POST / PUT / DELETE)
    :param request_body: data to be send along with the request
    :param header: http header required for the call
    :return: Returns the response error_msg , time_millis
    """
    response = None
    error_msg = None
    time_millis = 0
    response, error_msg, time_millis = curl_krb_request(Environment.get_instance().tmp_dir, keytab, principal, url, 'ranger_admin_calls',
                                                         None, flag_http_response, "Ranger-Admin API calls", user,kinit_timer_ms=0,method = request_method,body=request_body,header=header)

    return response, error_msg, time_millis

  @safe_retry(times=75, sleep_time=8, backoff_factor=1, err_class=Fail, return_on_fail=None)
  def check_ranger_login_curl(self, component_user,component_user_keytab,component_user_principal,base_url,True):
    """
    :param url: ranger admin host url
    :param usernamepassword: user credentials using which repository needs to be searched.
    :return: Returns login check response
    """
    response = ''
    error_msg = ''
    time_millis = 0
    try:
      response,error_msg,time_millis = self.call_curl_request(component_user,component_user_keytab,component_user_principal,base_url,True)
    except Fail,fail:
      raise Fail(fail.args)

    return response, error_msg,time_millis

  @safe_retry(times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None)
  def get_repository_by_name_curl(self, component_user, component_user_keytab, component_user_principal, name, component, status, is_keyadmin = False):
    """
    :param component_user: service user for which call is to be made
    :param component_user_keytab: keytab of service user
    :param component_user_principal: principal of service user
    :param name: name of the component, te be searched
    :param component:, component for which repository has to be checked
    :param status: active or inactive
    :param usernamepassword: user credentials using which repository needs to be searched.
    :return: Returns Ranger repository object if found otherwise None
    """
    try:
      search_repo_url = self.url_repos_pub + "?serviceName=" + name + "&serviceType=" + component + "&isEnabled=" + status
      if is_keyadmin:
        search_repo_url = '{0}&suser=keyadmin'.format(search_repo_url)
      response,error_message,time_in_millis = self.call_curl_request(component_user,component_user_keytab,component_user_principal,search_repo_url,False,request_method='GET')
      response_stripped = response[1:len(response) - 1]
      if response_stripped and len(response_stripped) > 0:
        response_json = json.loads(response_stripped)
        if 'name' in response_json and response_json['name'].lower() == name.lower():
          return response_json
        else:
          return None
      else:
        return None
    except Exception, err:
      raise Fail('Error in call for getting Ranger service:\n {0}'.format(err))

  @safe_retry(times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None)
  def create_repository_curl(self, component_user, component_user_keytab, component_user_principal, name, data, policy_user, is_keyadmin = False):
    """
    :param component_user: service user for which call is to be made
    :param component_user_keytab: keytab of service user
    :param component_user_principal: principal of service user
    :param name: name of the repository to be created
    :param data: service definition of the repository
    :return:
    """
    try:
      search_repo_url = self.url_repos_pub
      if is_keyadmin:
        search_repo_url = '{0}?suser=keyadmin'.format(search_repo_url)
      header = 'Content-Type: application/json'
      method = 'POST'

      response,error_message,time_in_millis = self.call_curl_request(component_user,component_user_keytab,component_user_principal,search_repo_url,False,method,data,header)
      if response and len(response) > 0:
        response_json = json.loads(response)
        if 'name' in response_json and response_json['name'].lower() == name.lower():
          Logger.info('Repository created Successfully')
          return response_json
        elif 'exists' in response.lower():
          Logger.info('Repository {name} already exists'.format(name=name))
          return response_json
        else:
          Logger.info('Repository creation failed')
          return None
      else:
        Logger.info('Repository creation failed')
        return None
    except Exception, err:
      raise Fail('Error in call for creating Ranger service:\n {0}'.format(err))

  @safe_retry(times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None)
  def update_repository_urllib2(self, component, repo_name, repo_properties, admin_user, admin_password, force_rename = False):
    """
    param component: name of service supported by Ranger Admin
    param repo_name: name of service name that needs to be updated
    param repo_properties: configs that needs to be updated for given service name
    param admin_user: user having role admin in Ranger Admin
    param admin_password: password of the admin user used
    param force_rename: flag to forcefully rename service name if required during updation
    """
    try:
      update_repo_url = self.url_repos_pub + "/name/" + repo_name
      if force_rename:
        update_repo_url = update_repo_url + "?forceRename=true"
      repo_update_data = json.dumps(repo_properties)
      usernamepassword = admin_user + ":" + admin_password
      base_64_string = base64.encodestring("{0}".format(usernamepassword)).replace("\n", "")
      headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      }
      request = urllib2.Request(update_repo_url, repo_update_data, headers)
      request.add_header("Authorization", "Basic {0}".format(base_64_string))
      request.get_method = lambda: 'PUT'
      result = openurl(request, timeout=20)
      response_code = result.getcode()
      response = json.loads(json.JSONEncoder().encode(result.read()))

      if response_code == 200:
        Logger.info("Service name {0} updated successfully on Ranger Admin for service {1}".format(repo_name, component))
        return response
      else:
        raise Fail("Service name {0} updation failed on Ranger Admin for service {1}".format(repo_name, component))
    except urllib2.URLError, e:
      if isinstance(e, urllib2.HTTPError):
        raise Fail("Error updating service name {0} on Ranger Admin for service {1}. Http status code - {2} \n {3}".format(repo_name, component, e.code, e.read()))
      else:
        raise Fail("Error updating service name {0} on Ranger Admin for service {1}. Reason - {2}".format(repo_name, component, e.reason))
    except httplib.BadStatusLine:
      raise Fail("Ranger Admin is not reachable for updating service name {0} for service {1}".format(repo_name, component))
    except TimeoutError:
      raise Fail("Connection to Ranger Admin failed. Reason - timeout")

  @safe_retry(times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=None)
  def update_repository_curl(self, component, repo_name, repo_properties, component_user, component_user_principal, component_user_keytab, force_rename = False):
    """
    param component: name of service supported by Ranger Admin
    param repo_name: name of service name that needs to be updated
    param repo_properties: configs that needs to be updated for given service name
    param component_user: service process user
    param component_user_principal: principal of service user
    param component_user_keytab: keytab of service user
    param force_rename: flag to forcefully rename service name if required during updation
    """
    try:
      update_repo_url = self.url_repos_pub + "/name/" + repo_name
      if force_rename:
        update_repo_url = update_repo_url + "?forceRename=true"
      repo_update_data = json.dumps(repo_properties)
      header = "Content-Type: application/json"
      method = "PUT"

      response, error_message, time_in_millis = self.call_curl_request(component_user, component_user_keytab, component_user_principal, update_repo_url, False, method, repo_update_data, header)
      if response and len(response) > 0:
        response_json = json.loads(response)
        if "name" in response_json:
          Logger.info("Service name {0} updated successfully on Ranger Admin for service {1}".format(repo_name, component))
          return response_json
        else:
          Logger.info("Service name {0} updation failed on Ranger Admin for service {1}".format(repo_name, component))
          return None
      else:
        Logger.info("Service name {0} updation failed on Ranger Admin for service {1}".format(repo_name, component))
        return None
    except Exception, err:
      raise Fail('Error updating service name {0} on Ranger Admin for service {1}.\n Reason - {2}'.format(repo_name, component, err))
