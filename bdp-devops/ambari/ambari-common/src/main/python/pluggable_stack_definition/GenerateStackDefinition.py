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

import sys
import getopt
import json
import os
import shutil
import xml.etree.ElementTree as ET
from xml.dom import minidom
import re
from os.path import join
import random
import string

def generate_random_string(size=7, chars=string.ascii_uppercase + string.digits):
  return ''.join(random.choice(chars) for _ in range(size))

class _named_dict(dict):
  """
  Allow to get dict items using attribute notation, eg dict.attr == dict['attr']
  """

  def __init__(self, _dict):

    def repl_list(_list):
      for i, e in enumerate(_list):
        if isinstance(e, list):
          _list[i] = repl_list(e)
        if isinstance(e, dict):
          _list[i] = _named_dict(e)
      return _list

    dict.__init__(self, _dict)
    for key, value in self.iteritems():
      if isinstance(value, dict):
        self[key] = _named_dict(value)
      if isinstance(value, list):
        self[key] = repl_list(value)

  def __getattr__(self, item):
    if item in self:
      return self[item]
    else:
      dict.__getattr__(self, item)

def copy_tree(src, dest, exclude=None, post_copy=None):
  """
  Copy files form src to dest.

  :param src: source folder
  :param dest: destination folder
  :param exclude: list for excluding, eg [".xml"] will exclude all xml files
  :param post_copy: callable that accepts source and target paths and will be called after copying
  """
  if not os.path.exists(src):
    return
  for item in os.listdir(src):
    if exclude:
      skip = False
      for ex in exclude:
        if item.endswith(ex):
          skip = True
          break
      if skip:
        continue

    _src = os.path.join(src, item)
    _dest = os.path.join(dest, item)

    if os.path.isdir(_src):
      if not os.path.exists(_dest):
        os.makedirs(_dest)
      copy_tree(_src, _dest, exclude, post_copy)
    else:
      _dest_dirname = os.path.dirname(_dest)
      if not os.path.exists(_dest_dirname):
        os.makedirs(_dest_dirname)
      shutil.copy(_src, _dest)

    if post_copy:
      post_copy(_src, _dest)

def process_replacements(file_path, config_data, stack_version_changes):
  file_data = open(file_path, 'r').read().decode('utf-8')

  # save user-defined text before replacements
  preserved_map = {}
  if "preservedText" in config_data:
    for preserved in config_data.preservedText:
      rnd = generate_random_string()
      file_data = file_data.replace(preserved, rnd)
      preserved_map[rnd] = preserved

  # replace user defined values
  if 'textReplacements' in config_data:
    for _from, _to in config_data['textReplacements']:
      file_data = file_data.replace(_from, _to)
  # replace stack version changes
  # it can be dangerous to replace versions in xml files, because it can be a part of version of some package or service
  # eg 2.1.2.1 with stack version change 2.1->3.0 will result in 3.0.3.0
  if not file_path.endswith(".xml"):
    for _from, _to in stack_version_changes.iteritems():
      file_data = file_data.replace(_from, _to)
      file_data = process_version_replace(file_data, _from, _to)
  # preform common replacements
  if 'performCommonReplacements' in config_data and config_data.performCommonReplacements:
    for from_version, to_version in stack_version_changes.iteritems():
      file_data = file_data.replace('HDP-'+from_version, config_data.stackName+"-"+to_version)
      file_data = file_data.replace('HDP '+from_version, config_data.stackName+" "+to_version)
    file_data = file_data.replace('hdp', config_data.stackName.lower())
    file_data = file_data.replace('HDP', config_data.stackName)
  if preserved_map:
    for _from, _to in preserved_map.iteritems():
      file_data = file_data.replace(_from, _to)
  with open(file_path, "w") as target:
    target.write(file_data.encode('utf-8'))
  return file_path

def process_version_replace(text, base_version, version):
  dash_base_version = base_version.replace('.', '-')
  dash_version = version.replace('.', '-')
  underscore_base_version = base_version.replace('.', '_')
  underscore_version = version.replace('.', '_')
  if dash_base_version in text:
    text = text.replace(dash_base_version, dash_version)
  if underscore_base_version in text:
    text = text.replace(underscore_base_version, underscore_version)
  return text


def process_metainfo(file_path, config_data, stack_version_changes, common_services = []):
  tree = ET.parse(file_path)
  root = tree.getroot()

  if root.find('versions') is not None or root.find('services') is None:
    # process stack metainfo.xml
    extends_tag = root.find('extends')
    if extends_tag is not None:
      version = extends_tag.text
      if version in stack_version_changes:
        extends_tag.text = stack_version_changes[version]
        tree.write(file_path)

    current_version = file_path.split(os.sep)[-2]
    modify_active_tag = False
    active_tag_value = None
    for stack in config_data.versions:
      if stack.version == current_version and 'active' in stack:
        modify_active_tag = True
        active_tag_value = stack.active
        break

    if modify_active_tag:
      versions_tag = root.find('versions')
      if versions_tag is None:
        versions_tag = ET.SubElement(root, 'versions')
      active_tag = versions_tag.find('active')
      if active_tag is None:
        active_tag = ET.SubElement(versions_tag, 'active')
      active_tag.text = active_tag_value
      tree.write(file_path)
  else:
    # Process service metainfo.xml
    services_tag = root.find('services')
    if services_tag is not None:
      for service_tag in services_tag.findall('service'):
        name = service_tag.find('name').text
        ####################################################################################################
        # Add common service to be copied.
        ####################################################################################################
        extends_tag = service_tag.find('extends')
        if extends_tag is not None:
          common_services.append(extends_tag.text)
        service_version_tag = service_tag.find('version')
        # file_path <resource_dir>/stacks/<stack_name>/<stack_version>/services/<service_name>/metainfo.xml
        split_path = file_path.split(os.path.sep)
        split_path_len = len(split_path)
        path_stack_version = split_path[split_path_len - 4]
        for stack in config_data.versions:
          if stack.version == path_stack_version:
            for service in stack.services:
              if service.name == name:
                ######################################################################################################
                # Update service version
                ######################################################################################################
                if 'version' in service:
                  ####################################################################################################
                  # If explicit service version is provided in the config, override the service version
                  ####################################################################################################
                  if service_version_tag is None:
                    service_version_tag = ET.SubElement(service_tag, 'version')
                  service_version_tag.text = service.version
                else:
                  ####################################################################################################
                  # Default: Update service version by replacing the stack version in the service version string
                  # Example (ex: HDFS 2.7.1.2.3 -> HDFS 2.7.1.3.1)
                  ####################################################################################################
                  if service_version_tag is not None:
                    service_version_split = service_version_tag.text.split(".")
                    if len(stack.baseVersion) < len(service_version_tag.text):
                      version_suffix = service_version_tag.text[-len(stack.baseVersion):]
                      if version_suffix == stack.baseVersion:
                        version_prefix = service_version_tag.text[0:-len(stack.baseVersion)]
                        service_version_tag.text = version_prefix + stack.version
                ######################################################################################################
                # Update service version
                ######################################################################################################
                osSpecifics_tag = service_tag.find('osSpecifics')
                if 'packages' in service:
                  if osSpecifics_tag is not None:
                    service_tag.remove(osSpecifics_tag)
                  osSpecifics_tag = ET.SubElement(service_tag, 'osSpecifics')
                  for item in service['packages']:
                    osSpecific_tag = ET.SubElement(osSpecifics_tag, 'osSpecific')
                    family = item['family']
                    osFamily_tag = ET.SubElement(osSpecific_tag, 'osFamily')
                    osFamily_tag.text = family
                    packages_tag = ET.SubElement(osSpecific_tag, 'packages')
                    for package in item['packages']:
                      package_tag = ET.SubElement(packages_tag, 'package')
                      name_tag = ET.SubElement(package_tag, 'name')
                      if isinstance(package, basestring):
                        name_tag.text = package
                      else:
                        name_tag.text = package['name']
                      if 'skipUpgrade' in package:
                        skipUpgrade_tag = ET.SubElement(package_tag, 'skipUpgrade')
                        skipUpgrade_tag.text = package['skipUpgrade']
                else:
                  ####################################################################################################
                  # Default: Update package version by replacing stack version in the package name
                  # Example (ex: falcon_2_2_* -> falcon_3_0_*, falcon-2-3-* -> falcon-3-1-*)
                  ####################################################################################################
                  for packages_tag in service_tag.getiterator('packages'):
                    for package_tag in packages_tag.getiterator('package'):
                      name_tag = package_tag.find('name')
                      for base_version in stack_version_changes:
                        version = stack_version_changes[base_version]
                        name_tag.text = process_version_replace(name_tag.text, base_version, version)
    tree.write(file_path)
  return file_path

def process_upgrade_xml(file_path, target_version, config_data, stack_version_changes):
  # change versions in xml
  tree = ET.parse(file_path)
  root = tree.getroot()

  for target_tag in root.findall('target'):
    version = '.'.join([el for el in target_tag.text.split('.') if el != '*'])
    if version in stack_version_changes:
      target_tag.text = target_tag.text.replace(version, stack_version_changes[version])
      tree.write(file_path)

  for target_tag in root.findall('target-stack'):
    base_stack_name, base_stack_version = target_tag.text.split('-')
    new_target_stack_text = target_tag.text.replace(base_stack_name, config_data.stackName)
    if base_stack_version in stack_version_changes:
      new_target_stack_text = new_target_stack_text.replace(base_stack_version,
                                                            stack_version_changes[base_stack_version])
    target_tag.text = new_target_stack_text
    tree.write(file_path)

  # rename upgrade files
  new_file_path = file_path
  if target_version in stack_version_changes:
    new_file_path = os.path.join(os.path.dirname(file_path),
                            'upgrade-{0}.xml'.format(stack_version_changes[target_version]))
    os.rename(file_path, new_file_path)
  return new_file_path

def process_stack_advisor(file_path, config_data, stack_version_changes):
  CLASS_NAME_REGEXP = r'([A-Za-z]+)(\d+)StackAdvisor'

  stack_advisor_content = open(file_path, 'r').read()

  for stack_name, stack_version in re.findall(CLASS_NAME_REGEXP, stack_advisor_content):
    what = stack_name + stack_version + 'StackAdvisor'
    stack_version_dotted = '.'.join(list(stack_version))
    if stack_version_dotted in stack_version_changes:
      to = config_data.stackName + stack_version_changes[stack_version_dotted].replace('.', '') + 'StackAdvisor'
    else:
      to = config_data.stackName + stack_version + 'StackAdvisor'
    stack_advisor_content = stack_advisor_content.replace(what, to)

  with open(file_path, 'w') as f:
    f.write(stack_advisor_content)
  return file_path

def process_repoinfo_xml(file_path, config_data, stack_version_changes, stack):
  if 'repoinfo' in stack:
    #########################################################################################
    # Update repo info from explicitly defined repo info from config
    # Assumption: All elements in repo info are configured
    #########################################################################################
    root = ET.Element("reposinfo")
    if 'latest' in stack.repoinfo:
      latest_tag = ET.SubElement(root, 'latest')
      latest_tag.text = stack.repoinfo.latest
    if 'os' in stack.repoinfo:
      for family, repos in stack.repoinfo.os.iteritems():
        os_tag = ET.SubElement(root, 'os')
        os_tag.set('family', family)
        for repo in repos:
          repo_tag = ET.SubElement(os_tag, 'repo')
          baseurl_tag = ET.SubElement(repo_tag, 'baseurl')
          baseurl_tag.text = repo.baseurl
          repoid_tag = ET.SubElement(repo_tag, 'repoid')
          repoid_tag.text = repo.repoid
          reponame_tag= ET.SubElement(repo_tag, 'reponame')
          reponame_tag.text = repo.reponame
    open(file_path,"w").write(minidom.parseString(ET.tostring(root, 'utf-8')).toprettyxml(indent="  "))
  else:
    #########################################################################################
    # Update repo info with defaults if repo info is not defined in config
    #########################################################################################
    tree = ET.parse(file_path)
    root = tree.getroot()
    remove_list = list()
    if 'family' in stack:
      for os_tag in root.getiterator("os"):
        os_family = os_tag.get('family')
        if os_family not in stack.family:
          remove_list.append(os_tag)
    for os_tag in remove_list:
      root.remove(os_tag)

    # Update all base urls
    for baseurl_tag in root.getiterator('baseurl'):
      baseurl_tag.text = 'http://SET_REPO_URL'
    # Update latest url
    for latest_tag in root.getiterator('latest'):
      latest_tag.text = 'http://SET_LATEST_REPO_URL_INFO'
    # Update repo ids
    for repoid_tag in root.getiterator('repoid'):
      repoid_tag.text = repoid_tag.text.replace(config_data.baseStackName, config_data.stackName)
      for baseVersion in stack_version_changes:
        repoid_tag.text = repoid_tag.text.replace(baseVersion, stack_version_changes[baseVersion])
    # Update repo name
    for reponame_tag in root.getiterator('reponame'):
      reponame_tag.text = reponame_tag.text.replace(config_data.baseStackName, config_data.stackName)
    tree.write(file_path)
  return file_path

def process_py_files(file_path, config_data, stack_version_changes):
  new_file_path = process_replacements(file_path, config_data, stack_version_changes)
  if  config_data.baseStackName.lower() in file_path:
    new_file_path = file_path.replace(config_data.baseStackName.lower(), config_data.stackName.lower())
    os.rename(file_path, new_file_path)
  return new_file_path

def process_xml_files(file_path, config_data, stack_version_changes):
  return process_replacements(file_path, config_data, stack_version_changes)

def process_other_files(file_path, config_data, stack_version_changes):
  return process_replacements(file_path, config_data, stack_version_changes)

def process_config_xml(file_path, config_data):
  tree = ET.parse(file_path)
  root = tree.getroot()
  #############################################################################################
  # <resource_dir>/common-services/<service_name>/<service_version>/configuration/<config>.xml
  #############################################################################################
  COMMON_SERVICES_CONFIG_PATH_REGEX = r'common-services/([A-Za-z_-]+)/([0-9\.]+)/configuration/([A-Za-z0-9_-]+).xml'
  #############################################################################################
  # <resource_dir>/stacks/<stack_name>/<stack_version>/services/<service_name>/configuration/<config>.xml
  #############################################################################################
  STACK_SERVICE_CONFIG_PATH_REGEX = r'stacks/([A-Za-z_-]+)/([0-9\.]+)/services/([A-Za-z_-]+)/configuration/([A-Za-z0-9_-]+).xml'
  #############################################################################################
  # <resource_dir>/stacks/<stack_name>/<stack_version>/configuration/<config>.xml
  #############################################################################################
  STACK_CONFIG_PATH_REGEX = r'stacks/([A-Za-z_-]+)/([0-9\.]+)/configuration/([A-Za-z0-9_-]+).xml'

  #########################################################################################
  # Override stack config properties
  #########################################################################################
  match = re.search(COMMON_SERVICES_CONFIG_PATH_REGEX, file_path)
  if match:
    #############################################################################################
    # Config file path in common services
    #############################################################################################
    path_service_name = match.group(1)
    path_service_version = match.group(2)
    path_config_name = match.group(3)
    if 'common-services' in config_data:
      for service in config_data['common-services']:
        if service.name == path_service_name:
          for serviceVersion in service.versions:
            if serviceVersion.version == path_service_version:
              if 'configurations' in serviceVersion:
                for conf in serviceVersion['configurations']:
                  if conf.name == path_config_name:
                    for property_tag in root.findall('property'):
                      property_name = property_tag.find('name').text
                      if property_name in conf.properties:
                        value_tag = property_tag.find('value')
                        value_tag.text = conf.properties[property_name]
  else:
    match = re.search(STACK_SERVICE_CONFIG_PATH_REGEX, file_path)
    if match:
      #############################################################################################
      # Config file path for a service in stack
      #############################################################################################
      path_stack_name = match.group(1)
      path_stack_version = match.group(2)
      path_service_name = match.group(3)
      path_config_name = match.group(4)
      for stack in config_data.versions:
        if stack.version == path_stack_version:
          for service in stack.services:
            if service.name == path_service_name:
              if 'configurations' in service:
                for conf in service['configurations']:
                  if conf.name == path_config_name:
                    for property_tag in root.findall('property'):
                      property_name = property_tag.find('name').text
                      if property_name in conf.properties:
                        value_tag = property_tag.find('value')
                        value_tag.text = conf.properties[property_name]
    else:
      match = re.search(STACK_CONFIG_PATH_REGEX, file_path)
      if match:
        #############################################################################################
        # Config file path for global stack configs
        #############################################################################################
        path_stack_name = match.group(1)
        path_stack_version = match.group(2)
        path_config_name = match.group(3)
        for stack in config_data.versions:
          if stack.version == path_stack_version:
            if 'configurations' in stack:
              for conf in stack['configurations']:
                if conf.name == path_config_name:
                  for property_tag in root.findall('property'):
                    property_name = property_tag.find('name').text
                    if property_name in conf.properties:
                      value_tag = property_tag.find('value')
                      value_tag.text = conf.properties[property_name]

  tree.write(file_path)
  return file_path

class GeneratorHelper(object):
  def __init__(self, config_data, resources_folder, output_folder):
    self.config_data = config_data
    self.resources_folder = resources_folder
    self.output_folder = output_folder

    stack_version_changes = {}

    for stack in config_data.versions:
      if stack.version != stack.baseVersion:
        stack_version_changes[stack.baseVersion] = stack.version

    self.stack_version_changes = stack_version_changes
    self.common_services = []

  def copy_stacks(self):
    original_folder = os.path.join(self.resources_folder, 'stacks', self.config_data.baseStackName)
    partial_target_folder = os.path.join(self.resources_folder, 'stacks', self.config_data.stackName)
    target_folder = os.path.join(self.output_folder, 'stacks', self.config_data.stackName)

    for stack in self.config_data.versions:
      original_stack = os.path.join(original_folder, stack.baseVersion)
      target_stack = os.path.join(target_folder, stack.version)
      partial_target_stack = os.path.join(partial_target_folder, stack.version)

      desired_services = [service.name for service in stack.services]
      desired_services.append('stack_advisor.py')  # stack_advisor.py placed in stacks folder
      base_stack_services = os.listdir(os.path.join(original_stack, 'services'))
      ignored_files = [service for service in base_stack_services if service not in desired_services]
      ignored_files.append('.pyc')

      def post_copy(src, target):
        if target.endswith('.xml'):
          ####################################################################
          # Add special case handling for specific xml files
          ###################################################################
          # process metainfo.xml
          if target.endswith('metainfo.xml'):
            target = process_metainfo(target, self.config_data, self.stack_version_changes, self.common_services)
          # process repoinfo.xml
          if target.endswith('repoinfo.xml'):
            target = process_repoinfo_xml(target, self.config_data, self.stack_version_changes, stack)
          if os.path.basename(os.path.dirname(target)) == 'configuration':
            # process configuration xml
            target = process_config_xml(target, self.config_data)
          # process upgrade-x.x.xml
          _upgrade_re = re.compile('upgrade-(.*)\.xml')
          result = re.search(_upgrade_re, target)
          if result:
            target_version = result.group(1)
            target = process_upgrade_xml(target, target_version, self.config_data, self.stack_version_changes)
          ####################################################################
          # Generic processing for xml files
          ###################################################################
          process_xml_files(target, self.config_data, self.stack_version_changes)
          return
        if target.endswith('.py'):
          ####################################################################
          # Add special case handling for specific py files
          ###################################################################
          # process stack_advisor.py
          if target.endswith('stack_advisor.py'):
            target = process_stack_advisor(target, self.config_data, self.stack_version_changes)
          ####################################################################
          # Generic processing for py files
          ###################################################################
          target = process_py_files(target, self.config_data, self.stack_version_changes)
          return
        ####################################################################
        # Generic processing for all other types of files.
        ####################################################################
        if target.endswith(".j2") or target.endswith(".sh"):
          process_other_files(target, self.config_data, self.stack_version_changes)

      copy_tree(original_stack, target_stack, ignored_files, post_copy=post_copy)
      # After generating target stack from base stack, overlay target stack partial definition defined under
      # <resourceDir>/stacks/<targetStackName>/<targetStackVersion>
      copy_tree(partial_target_stack, target_stack, ignored_files, post_copy=None)

    # copy default stack advisor
    shutil.copy(os.path.join(self.resources_folder, 'stacks', 'stack_advisor.py'), os.path.join(target_folder, '../stack_advisor.py'))

  def copy_common_services(self, common_services = []):
    ignored_files = ['.pyc']
    if not common_services:
      common_services = self.common_services
    for original_folder in common_services:
      source_folder = os.path.join(self.resources_folder, original_folder)
      target_folder = os.path.join(self.output_folder, original_folder)
      parent_services = []
      def post_copy(src, target):
        if target.endswith('.xml'):
          # process metainfo.xml
          if target.endswith('metainfo.xml'):
            process_metainfo(target, self.config_data, self.stack_version_changes, parent_services)
          if os.path.basename(os.path.dirname(target)) == 'configuration':
            # process configuration xml
            target = process_config_xml(target, self.config_data)
          # process generic xml
          process_xml_files(target, self.config_data, self.stack_version_changes)
          return
        # process python files
        if target.endswith('.py'):
          process_py_files(target, self.config_data, self.stack_version_changes)
          return
        ####################################################################
        # Generic processing for all other types of files.
        ####################################################################
        if target.endswith(".j2") or target.endswith(".sh"):
          process_other_files(target, self.config_data, self.stack_version_changes)

      copy_tree(source_folder, target_folder, ignored_files, post_copy=post_copy)
      if parent_services:
        self.copy_common_services(parent_services)
    pass

  def copy_remaining_common_services(self, common_services = []):
    ignored_files = ['.pyc']
    source_common_services_path = os.path.join(self.resources_folder, "common-services")
    dest_common_services_path = os.path.join(self.output_folder, "common-services")

    source_common_services_list = os.listdir(source_common_services_path)
    dest_common_services_list = os.listdir(dest_common_services_path)

    for service_name in source_common_services_list:
      if service_name not in dest_common_services_list:
        source = os.path.join(source_common_services_path, service_name)
        dest = os.path.join(dest_common_services_path, service_name)
        copy_tree(source, dest, ignored_files, post_copy=None)

  def copy_resource_management(self):
    source_folder = join(os.path.abspath(join(self.resources_folder, "..", "..", "..", "..")),
                         'ambari-common', 'src', 'main', 'python', 'resource_management')
    target_folder = join(self.output_folder, 'python', 'resource_management')
    ignored_files = ['.pyc']

    def post_copy(src, target):
      # process python files
      if target.endswith('.py'):
        # process script.py
        process_py_files(target, self.config_data, self.stack_version_changes)
        return

    copy_tree(source_folder, target_folder, ignored_files, post_copy=post_copy)

  def copy_ambari_properties(self):
    source_ambari_properties = join(os.path.abspath(join(self.resources_folder, "..", "..", "..", "..")),
                         'ambari-server', 'conf', 'unix', "ambari.properties")
    target_ambari_properties = join(self.output_folder, 'conf', 'unix', 'ambari.properties')
    target_dirname = os.path.dirname(target_ambari_properties)

    if not os.path.exists(target_dirname):
      os.makedirs(target_dirname)
    propertyMap = {}
    if "ambariProperties" in self.config_data:
      propertyMap = self.config_data.ambariProperties

    with open(source_ambari_properties, 'r') as in_file:
      with open(target_ambari_properties, 'w') as out_file:
        replaced_properties = []
        for line in in_file:
          property = line.split('=')[0]
          if property in propertyMap:
            out_file.write('='.join([property, propertyMap[property]]))
            out_file.write(os.linesep)
            replaced_properties.append(property)
          else:
            out_file.write(line)

        if len(propertyMap) - len(replaced_properties) > 0:
          out_file.write(os.linesep)  #make sure we don't break last entry from original properties

        for key in propertyMap:
          if key not in replaced_properties:
            out_file.write('='.join([key, propertyMap[key]]))
            out_file.write(os.linesep)
            
  def copy_custom_actions(self):
    original_folder = os.path.join(self.resources_folder, 'custom_actions')
    target_folder = os.path.join(self.output_folder, 'custom_actions')
    ignored_files = ['.pyc']

    def post_copy(src, target):
      # process python files
      if target.endswith('.py'):
        # process script.py
        process_py_files(target, self.config_data, self.stack_version_changes)
        return

    copy_tree(original_folder, target_folder, ignored_files, post_copy=post_copy)

def main(argv):
  HELP_STRING = 'GenerateStackDefinition.py -c <config> -r <resources_folder> -o <output_folder>'
  config = ''
  resources_folder = ''
  output_folder = ''
  try:
    opts, args = getopt.getopt(argv, "hc:o:r:", ["config=", "out=", "resources="])
  except getopt.GetoptError:
    print HELP_STRING
    sys.exit(2)
  for opt, arg in opts:
    if opt == '-h':
      print HELP_STRING
      sys.exit()
    elif opt in ("-c", "--config"):
      config = arg
    elif opt in ("-r", "--resources"):
      resources_folder = arg
    elif opt in ("-o", "--out"):
      output_folder = arg
  if not config or not resources_folder or not output_folder:
    print HELP_STRING
    sys.exit(2)

  config_data = _named_dict(json.load(open(config, "r")))
  gen_helper = GeneratorHelper(config_data, resources_folder, output_folder)
  gen_helper.copy_stacks()
  gen_helper.copy_resource_management()
  gen_helper.copy_common_services()
  gen_helper.copy_remaining_common_services()
  gen_helper.copy_ambari_properties()
  gen_helper.copy_custom_actions()


if __name__ == "__main__":
  main(sys.argv[1:])
