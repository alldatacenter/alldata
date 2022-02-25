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
import time
import sys
import urllib2
import socket
import re
from ambari_commons import OSCheck
from functools import wraps

from exceptions import FatalException, NonFatalException, TimeoutError

if OSCheck.is_windows_family():
  from ambari_commons.os_windows import os_run_os_command
else:
  # MacOS not supported
  from ambari_commons.os_linux import os_run_os_command
  pass

from logging_utils import *
from os_check import OSCheck


def openurl(url, timeout=socket._GLOBAL_DEFAULT_TIMEOUT, *args, **kwargs):
  """

  :param url: url to open
  :param timeout: open timeout, raise TimeoutError on timeout
  :rtype urllib2.Request
  """
  try:
    return urllib2.urlopen(url, timeout=timeout, *args, **kwargs)
  except urllib2.URLError as e:
    # Python 2.6 timeout handling
    if hasattr(e, "reason") and isinstance(e.reason, socket.timeout):
      raise TimeoutError(e.reason)
    else:
      raise e  # re-throw exception
  except socket.timeout as e:  # Python 2.7 timeout handling
    raise TimeoutError(e)


def download_file(link, destination, chunk_size=16 * 1024, progress_func = None):
  print_info_msg("Downloading {0} to {1}".format(link, destination))
  if os.path.exists(destination):
      print_warning_msg("File {0} already exists, assuming it was downloaded before".format(destination))
      return

  force_download_file(link, destination, chunk_size, progress_func = progress_func)


def download_file_anyway(link, destination, chunk_size=16 * 1024, progress_func = None):
  print_info_msg("Trying to download {0} to {1} with python lib [urllib2].".format(link, destination))
  if os.path.exists(destination):
    print_warning_msg("File {0} already exists, assuming it was downloaded before".format(destination))
    return
  try:
    force_download_file(link, destination, chunk_size, progress_func = progress_func)
  except:
    print_error_msg("Download {0} with python lib [urllib2] failed with error: {1}".format(link, str(sys.exc_info())))

  if not os.path.exists(destination):
    print "Trying to download {0} to {1} with [curl] command.".format(link, destination)
    #print_info_msg("Trying to download {0} to {1} with [curl] command.".format(link, destination))
    curl_command = "curl --fail -k -o %s %s" % (destination, link)
    retcode, out, err = os_run_os_command(curl_command)
    if retcode != 0:
      print_error_msg("Download file {0} with [curl] command failed with error: {1}".format(link, out + err))


  if not os.path.exists(destination):
    print_error_msg("Unable to download file {0}!".format(link))
    print "ERROR: unable to donwload file %s!" % (link)


def download_progress(file_name, downloaded_size, blockSize, totalSize):
  percent = int(downloaded_size * 100 / totalSize)
  status = "\r" + file_name

  if totalSize < blockSize:
    status += "... %d%%" % (100)
  else:
    status += "... %d%% (%.1f MB of %.1f MB)" % (
      percent, downloaded_size / 1024 / 1024.0, totalSize / 1024 / 1024.0)
  sys.stdout.write(status)
  sys.stdout.flush()

def wait_for_port_opened(hostname, port, tries_count, try_sleep):
  sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  sock.settimeout(2)

  for i in range(tries_count):
    if sock.connect_ex((hostname, port)) == 0:
      return True
    time.sleep(try_sleep)

  return False


def find_range_components(meta):
  file_size = 0
  seek_pos = 0
  hdr_range = meta.getheaders("Content-Range")
  if len(hdr_range) > 0:
    range_comp1 = hdr_range[0].split('/')
    if len(range_comp1) > 1:
      range_comp2 = range_comp1[0].split(' ') #split away the "bytes" prefix
      if len(range_comp2) == 0:
        raise FatalException(12, 'Malformed Content-Range response header: "{0}".'.format(hdr_range))
      range_comp3 = range_comp2[1].split('-')
      seek_pos = int(range_comp3[0])
      if range_comp1[1] != '*': #'*' == unknown length
        file_size = int(range_comp1[1])

  if file_size == 0:
    #Try the old-fashioned way
    hdrLen = meta.getheaders("Content-Length")
    if len(hdrLen) == 0:
      raise FatalException(12, "Response header doesn't contain Content-Length. Chunked Transfer-Encoding is not supported for now.")
    file_size = int(hdrLen[0])

  return (file_size, seek_pos)


def force_download_file(link, destination, chunk_size = 16 * 1024, progress_func = None):
  request = urllib2.Request(link)

  if os.path.exists(destination) and not os.path.isfile(destination):
    #Directory specified as target? Must be a mistake. Bail out, don't assume anything.
    err = 'Download target {0} is a directory.'.format(destination)
    raise FatalException(1, err)

  (dest_path, file_name) = os.path.split(destination)

  temp_dest = destination + ".tmpdownload"
  partial_size = 0

  if os.path.exists(temp_dest):
    #Support for resuming downloads, in case the process is killed while downloading a file
    #  set resume range
    # See http://stackoverflow.com/questions/6963283/python-urllib2-resume-download-doesnt-work-when-network-reconnects
    partial_size = os.stat(temp_dest).st_size
    if partial_size > chunk_size:
      #Re-download the last chunk, to minimize the possibilities of file corruption
      resume_pos = partial_size - chunk_size
      request.add_header("Range", "bytes=%s-" % resume_pos)
  else:
    #Make sure the full dir structure is in place, otherwise file open will fail
    if not os.path.exists(dest_path):
      os.makedirs(dest_path)

  response = urllib2.urlopen(request)
  (file_size, seek_pos) = find_range_components(response.info())

  print_info_msg("Downloading to: %s Bytes: %s" % (destination, file_size))

  if partial_size < file_size:
    if seek_pos == 0:
      #New file, create it
      open_mode = 'wb'
    else:
      #Resuming download of an existing file
      open_mode = 'rb+' #rb+ doesn't create the file, using wb to create it
    f = open(temp_dest, open_mode)

    try:
      #Resume the download from where it left off
      if seek_pos > 0:
        f.seek(seek_pos)

      file_size_dl = seek_pos
      while True:
        buffer = response.read(chunk_size)
        if not buffer:
            break

        file_size_dl += len(buffer)
        f.write(buffer)
        if progress_func is not None:
          progress_func(file_name, file_size_dl, chunk_size, file_size)
    finally:
      f.close()

    sys.stdout.write('\n')
    sys.stdout.flush()

  print_info_msg("Finished downloading {0} to {1}".format(link, destination))

  downloaded_size = os.stat(temp_dest).st_size
  if downloaded_size != file_size:
    err = 'Size of downloaded file {0} is {1} bytes, it is probably damaged or incomplete'.format(destination, downloaded_size)
    raise FatalException(1, err)

  # when download is complete -> mv temp_dest destination
  if os.path.exists(destination):
    #Windows behavior: rename fails if the destination file exists
    os.unlink(destination)
  os.rename(temp_dest, destination)

def resolve_address(address):
  """
  Resolves address to proper one in special cases, for example 0.0.0.0 to 127.0.0.1 on windows os.

  :param address: address to resolve
  :return: resulting address
  """
  if OSCheck.is_windows_family():
    if address == '0.0.0.0':
      return '127.0.0.1'
  return address

def ensure_ssl_using_protocol(protocol="PROTOCOL_TLSv1_2", ca_certs=None):
  """
  Patching ssl module to use configured protocol and ca certs

  :param protocol: one of ("PROTOCOL_SSLv2", "PROTOCOL_SSLv3", "PROTOCOL_SSLv23", "PROTOCOL_TLSv1", "PROTOCOL_TLSv1_1", "PROTOCOL_TLSv1_2")
  :param ca_certs: path to ca_certs file
  :return:
  """
  from functools import wraps
  import ssl

  if hasattr(ssl, "_create_default_https_context"):
    if not hasattr(ssl._create_default_https_context, "_ambari_patched"):
      @wraps(ssl._create_default_https_context)
      def _create_default_https_context_patched():
        context = ssl.SSLContext(protocol = getattr(ssl, protocol))
        if ca_certs:
          context.load_verify_locations(ca_certs)
          context.verify_mode = ssl.CERT_REQUIRED
          context.check_hostname = False
        return context
      _create_default_https_context_patched._ambari_patched = True
      ssl._create_default_https_context = _create_default_https_context_patched

"""
See RFC3986, Appendix B
Tested on the following cases:
  "192.168.54.1"
  "192.168.54.2:7661
  "hdfs://192.168.54.3/foo/bar"
  "ftp://192.168.54.4:7842/foo/bar"

  Returns None if only a port is passed in
"""
def get_host_from_url(uri):
  if uri is None:
    return None

  # if not a string, return None
  if not isinstance(uri, basestring):
    return None

    # RFC3986, Appendix B
  parts = re.findall('^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?', uri)

  # index of parts
  # scheme    = 1
  # authority = 3
  # path      = 4
  # query     = 6
  # fragment  = 8

  host_and_port = uri
  if 0 == len(parts[0][1]):
    host_and_port = parts[0][4]
  elif 0 == len(parts[0][2]):
    host_and_port = parts[0][1]
  elif parts[0][2].startswith("//"):
    host_and_port = parts[0][3]

  if -1 == host_and_port.find(':'):
    if host_and_port.isdigit():
      return None

    return host_and_port
  else:
    return host_and_port.split(':')[0]

