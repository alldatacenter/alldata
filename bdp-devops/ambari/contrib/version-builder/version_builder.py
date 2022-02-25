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

import optparse
import os
import subprocess
import sys
import xml.etree.ElementTree as ET

class VersionBuilder:
  """
  Used to build a version definition file
  """
  def __init__(self, filename):
    self._check_xmllint()

    self.filename = filename

    if os.path.exists(filename):
      tree = ET.ElementTree()
      tree.parse(filename)
      root = tree.getroot()
    else:
      attribs = {}
      attribs['xmlns:xsi'] = "http://www.w3.org/2001/XMLSchema-instance"
      attribs['xsi:noNamespaceSchemaLocation'] = "version_definition.xsd"
      root = ET.Element("repository-version", attribs)

      ET.SubElement(root, "release")
      ET.SubElement(root, "manifest")
      ET.SubElement(root, "available-services")
      ET.SubElement(root, "repository-info")

    self.root_element = root


  def persist(self):
    """
    Saves the XML file
    """
    p = subprocess.Popen(['xmllint', '--format', '--output', self.filename, '-'], stdout=subprocess.PIPE, stdin=subprocess.PIPE)
    (stdout, stderr) = p.communicate(input=ET.tostring(self.root_element))

  def finalize(self, xsd_file):
    """
    Validates the XML file against the XSD
    """
    args = ['xmllint', '--noout', '--load-trace', '--schema', xsd_file, self.filename]

    p = subprocess.Popen(args, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE)
    (stdout, stderr) = p.communicate()

    if p.returncode != 0:
      raise Exception(stderr)

    if len(stdout) > 0:
      print(stdout.decode("UTF-8"))

    if len(stderr) > 0:
      print(stderr.decode("UTF-8"))

  def set_release(self, type=None, stack=None, version=None, build=None, notes=None, display=None,
    compatible=None):
    """
    Create elements of the 'release' parent
    """
    release_element = self.root_element.find("./release")

    if release_element is None:
      raise Exception("Element 'release' is not found")

    if type:
      update_simple(release_element, "type", type)

    if stack:
      update_simple(release_element, "stack-id", stack)

    if version:
      update_simple(release_element, "version", version)

    if build:
      update_simple(release_element, "build", build)

    if compatible:
      update_simple(release_element, "compatible-with", compatible)

    if notes:
      update_simple(release_element, "release-notes", notes)

    if display:
      update_simple(release_element, "display", display)

  def set_os(self, os_family, package_version=None):
    repo_parent = self.root_element.find("./repository-info")
    if repo_parent is None:
      raise Exception("'repository-info' element is not found")

    os_element = self.findByAttributeValue(repo_parent, "./os", "family", os_family)
    if os_element is None:
      os_element = ET.SubElement(repo_parent, 'os')
      os_element.set('family', os_family)

    if package_version:
      pv_element = os_element.find("package-version")
      if pv_element is None:
        pv_element = ET.SubElement(os_element, "package-version")
      pv_element.text = package_version


  def add_manifest(self, id, service_name, version, version_id=None, release_version=None):
    """
    Add a manifest service.  A manifest lists all services in a repo, whether they are to be
    upgraded or not.
    """
    manifest_element = self.root_element.find("./manifest")

    if manifest_element is None:
      raise Exception("Element 'manifest' is not found")

    service_element = self.findByAttributeValue(manifest_element, "./service", "id", id)

    if service_element is None:
      service_element = ET.SubElement(manifest_element, "service")
      service_element.set('id', id)

    service_element.set('name', service_name)
    service_element.set('version', version)
    if version_id:
      service_element.set('version-id', version_id)

    if release_version:
      service_element.set('release-version', release_version)

  def add_available(self, manifest_id, available_components=None):
    """
    Adds services available to upgrade for patches
    """
    manifest_element = self.root_element.find("./manifest")
    if manifest_element is None:
      raise Exception("'manifest' element is not found")

    service_element = self.findByAttributeValue(manifest_element, "./service", "id", manifest_id)
    if service_element is None:
      raise Exception("Cannot add an available service for {0}; it's not on the manifest".format(manifest_id))

    available_element = self.root_element.find("./available-services")
    if available_element is None:
      raise Exception("'available-services' is not found")

    service_element = self.findByAttributeValue(available_element, "./service", "idref", manifest_id)

    if service_element is not None:
      available_element.remove(service_element)

    service_element = ET.SubElement(available_element, "service")
    service_element.set('idref', manifest_id)

    if available_components:
      components = available_components.split(',')
      for component in components:
        e = ET.SubElement(service_element, 'component')
        e.text = component

  def add_repo(self, os_family, repo_id, repo_name, base_url, unique, tags):
    """
    Adds a repository
    """
    repo_parent = self.root_element.find("./repository-info")
    if repo_parent is None:
      raise Exception("'repository-info' element is not found")

    os_element = self.findByAttributeValue(repo_parent, "./os", "family", os_family)
    if os_element is None:
      os_element = ET.SubElement(repo_parent, 'os')
      os_element.set('family', os_family)

    if self.useNewSyntax():
      repo_element = os_element.find("./repo/[reponame='{0}']".format(repo_name))
    else:
      repo_element = self.findByValue(os_element, "./repo/reponame", repo_name)

    if repo_element is not None:
      os_element.remove(repo_element)

    repo_element = ET.SubElement(os_element, 'repo')
    e = ET.SubElement(repo_element, 'baseurl')
    e.text = base_url

    e = ET.SubElement(repo_element, 'repoid')
    e.text = repo_id

    e = ET.SubElement(repo_element, 'reponame')
    e.text = repo_name

    if unique is not None:
      e = ET.SubElement(repo_element, 'unique')
      e.text = unique

    if tags is not None:
      e = ET.SubElement(repo_element, 'tags')
      tag_names = tags.split(',')
      for tag in tag_names:
        t = ET.SubElement(e, 'tag')
        t.text = tag


  def _check_xmllint(self):
    """
    Verifies utility xmllint is available
    """
    try:
      p = subprocess.Popen(['xmllint', '--version'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=False)
      (stdout, stderr) = p.communicate()

      if p.returncode != 0:
        raise Exception("xmllint command does not appear to be available")

    except:
      raise Exception("xmllint command does not appear to be available")

  def findByAttributeValue(self, root, element, attribute, value):
    if self.useNewSyntax():
      return root.find("./{0}[@{1}='{2}']".format(element, attribute, value))
    else:
      for node in root.findall("{0}".format(element)):
        if node.attrib[attribute] == value:
          return node
      return None;
  
  def findByValue(self, root, element, value):
    for node in root.findall("{0}".format(element)):
      if node.text == value:
        return node
    return None

  def useNewSyntax(self):
     #Python2.7 and newer shipps with ElementTree that supports a different syntax for XPath queries
     major=sys.version_info[0]
     minor=sys.version_info[1]
     if major > 3 :
       return True
     elif major == 2:
       return (minor > 6)
     else:
       return False;

def update_simple(parent, name, value):
  """
  Helper method to either update or create the element
  """
  element = parent.find('./' + name) 

  if element is None:
    element = ET.SubElement(parent, name)
    element.text = value
  else:
    element.text = value

def process_release(vb, options):
  """
  Create elements of the 'release' parent
  """
  if options.release_type:
    vb.set_release(type=options.release_type)

  if options.release_stack:
    vb.set_release(stack=options.release_stack)

  if options.release_version:
    vb.set_release(version=options.release_version)

  if options.release_build:
    vb.set_release(build=options.release_build)

  if options.release_compatible:
    vb.set_release(compatible=options.release_compatible)

  if options.release_notes:
    vb.set_release(notes=options.release_notes)

  if options.release_display:
    vb.set_release(display=options.release_display)

  if options.release_package_version:
    vb.set_release(package_version=options.release_package_version)

def process_manifest(vb, options):
  """
  Creates the manifest element
  """
  if not options.manifest:
    return

  vb.add_manifest(options.manifest_id, options.manifest_service, options.manifest_version, options.manifest_version_id,
    options.manifest_release_version)

def process_available(vb, options):
  """
  Processes available service elements
  """
  if not options.available:
    return

  vb.add_available(options.manifest_id, options.available_components)


def process_os(vb, options):
  if not options.os:
    return

  vb.set_os(options.os_family, options.os_package_version)

def process_repo(vb, options):
  """
  Processes repository options.  This method doesn't update or create individual elements, it
  creates the entire repo structure
  """
  if not options.repo:
    return

  vb.add_repo(options.repo_os, options.repo_id, options.repo_name, options.repo_url,
    options.unique, options.repo_tags)

def validate_manifest(parser, options):
  """
  Validates manifest options from the command line
  """
  if not options.manifest:
    return

  template = "When specifying --manifest, {0} is also required"

  if not options.manifest_id:
    parser.error(template.format("--manifest-id"))
  
  if not options.manifest_service:
    parser.error(template.format("--manifest-service"))

  if not options.manifest_version:
    parser.error(template.format("--manifest-version"))

def validate_available(parser, options):
  """
  Validates available service options from the command line
  """
  if not options.available:
    return

  if not options.manifest_id:
    parser.error("When specifying --available, --manifest-id is also required")

def validate_os(parser, options):
  if not options.os:
    return

  if not options.os_family:
    parser.error("When specifying --os, --os-family is also required")

def validate_repo(parser, options):
  """
  Validates repo options from the command line
  """
  if not options.repo:
    return

  template = "When specifying --repo, {0} is also required"

  if not options.repo_os:
    parser.error(template.format("--repo-os"))

  if not options.repo_url:
    parser.error(template.format("--repo-url"))

  if not options.repo_id:
    parser.error(template.format("--repo-id"))

  if not options.repo_name:
    parser.error(template.format("--repo-name"))


def main(argv):
  parser = optparse.OptionParser(
    epilog="OS utility 'xmllint' is required for this tool to function.  It handles pretty-printing and XSD validation.")
  
  parser.add_option('--file', dest='filename',
    help="The output XML file")

  parser.add_option('--finalize', action='store_true', dest='finalize',
    help="Finalize and validate the XML file")
  parser.add_option('--xsd', dest='xsd_file',
    help="The XSD location when finalizing")

  parser.add_option('--release-type', type='choice', choices=['STANDARD', 'PATCH', 'MAINT'], dest='release_type' ,
    help="Indicate the release type: i.e. STANDARD, PATCH, MAINT")
  parser.add_option('--release-stack', dest='release_stack',
    help="The stack id: e.g. HDP-2.4")
  parser.add_option('--release-version', dest='release_version',
    help="The release version without build number: e.g. 2.4.0.1")
  parser.add_option('--release-build', dest='release_build',
    help="The release build number: e.g. 1234")
  parser.add_option('--release-compatible', dest='release_compatible',
    help="Regular Expression string to identify version compatibility for patches: e.g. 2.4.1.[0-9]")
  parser.add_option('--release-notes', dest='release_notes',
    help="A http link to the documentation notes")
  parser.add_option('--release-display', dest='release_display',
    help="The display name for this release")
  parser.add_option('--release-package-version', dest='release_package_version',
    help="Identifier to use when installing packages, generally a part of the package name")

  parser.add_option('--manifest', action='store_true', dest='manifest',
    help="Add a manifest service with other options: --manifest-id, --manifest-service, --manifest-version, --manifest-version-id, --manifest-release-version")
  parser.add_option('--manifest-id', dest='manifest_id',
    help="Unique ID for a service in a manifest.  Required when specifying --manifest and --available")
  parser.add_option('--manifest-service', dest='manifest_service')
  parser.add_option('--manifest-version', dest='manifest_version')
  parser.add_option('--manifest-version-id', dest='manifest_version_id')
  parser.add_option('--manifest-release-version', dest='manifest_release_version')

  parser.add_option('--available', action='store_true', dest='available',
    help="Add an available service with other options: --manifest-id, --available-components --service-release-version")
  parser.add_option('--available-components', dest='available_components',
    help="A CSV of service components that are intended to be upgraded via patch. \
      Omitting this implies the entire service should be upgraded")

  parser.add_option('--os', action='store_true', dest='os', help="Add OS data with options --os-family, --os-package-version")
  parser.add_option('--os-family', dest='os_family', help="The operating system: i.e redhat7, debian7, ubuntu12, ubuntu14, suse11, suse12")
  parser.add_option('--os-package-version', dest='os_package_version',
    help="The package version to use for the OS")

  parser.add_option('--repo', action='store_true', dest='repo',
    help="Add repository data with options: --repo-os, --repo-url, --repo-id, --repo-name, --repo-unique")
  parser.add_option('--repo-os', dest='repo_os',
    help="The operating system type: i.e. redhat6, redhat7, debian7, debian9, ubuntu12, ubuntu14, ubuntu16, suse11, suse12")
  parser.add_option('--repo-url', dest='repo_url',
    help="The base url for the repository data")
  parser.add_option('--repo-unique', dest='unique', type='choice', choices=['true', 'false'],
                    help="Indicates base url should be unique")
  parser.add_option('--repo-id', dest='repo_id', help="The ID of the repo")
  parser.add_option('--repo-name', dest='repo_name', help="The name of the repo")
  parser.add_option('--repo-tags', dest='repo_tags', help="The CSV tags for the repo")

  (options, args) = parser.parse_args()

  # validate_filename
  if not options.filename:
    parser.error("--file option is required")

  # validate_finalize
  if options.finalize and not options.xsd_file:
    parser.error("Must supply XSD (--xsd) when finalizing")

  validate_manifest(parser, options)
  validate_available(parser, options)
  validate_os(parser, options)
  validate_repo(parser, options)

  vb = VersionBuilder(options.filename)

  process_release(vb, options)
  process_manifest(vb, options)
  process_available(vb, options)
  process_os(vb, options)
  process_repo(vb, options)

  # save file
  vb.persist()

  if options.finalize:
    vb.finalize(options.xsd_file)

if __name__ == "__main__":
  main(sys.argv)
