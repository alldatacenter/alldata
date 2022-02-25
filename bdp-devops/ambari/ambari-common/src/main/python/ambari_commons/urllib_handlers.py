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

import logging
import string

from urllib2 import BaseHandler
from urlparse import urlparse
from urlparse import urlunparse
from urlparse import ParseResult

logger = logging.getLogger()

REFRESH_HEADER = "refresh"
REFRESH_HEADER_URL_KEY = "url"

class RefreshHeaderProcessor(BaseHandler):
  """
  Examines responses from urllib2 and determines if there is a refresh header
  that points to a different location. If there is, then build a new URL
  swapping out the original requests's host for the redirected host and
  re-execute the query. If at any point, the parsing fails, then return the
  original response.
  """
  def __init__(self):
    """
    Initialization
    :return:
    """
    pass

  def http_response(self, request, response):
    """
    Inspect the http response from urllib2 and see if there is a refresh
    response header. If there is, then attempt to follow it and re-execute
    the query using the new host.
    :param request:
    :param response:
    :return:
    """
    # extract the original response code and headers
    response_code = response.code

    # unless we got back a 200 don't do any further processing
    if response_code != 200:
      return response

    # attempt to parse and follow the refresh header if it exists
    try:
      response_headers = response.info()
      refresh_header = None

      for response_header_key in response_headers.keys():
        if response_header_key.lower() == REFRESH_HEADER.lower():
          refresh_header = response_headers.getheader(response_header_key)
          break

      if refresh_header is None:
        return response

      # at this point the header should resemble
      # Refresh: 3; url=http://c6403.ambari.apache.org:8088/
      semicolon_index = string.find(refresh_header, ';')

      # slice the redirect URL out of
      # 3; url=http://c6403.ambari.apache.org:8088/jmx"
      if semicolon_index >= 0:
        redirect_url_key_value_pair = refresh_header[semicolon_index+1:]
      else:
        redirect_url_key_value_pair = refresh_header

      equals_index = string.find(redirect_url_key_value_pair, '=')
      key = redirect_url_key_value_pair[:equals_index]
      redirect_url = redirect_url_key_value_pair[equals_index+1:]

      if key.strip().lower() != REFRESH_HEADER_URL_KEY:
        logger.warning("Unable to parse refresh header {0}".format(refresh_header))
        return response

      # extract out just host:port
      # c6403.ambari.apache.org:8088
      redirect_netloc = urlparse(redirect_url).netloc

      # deconstruct the original request URL into parts
      original_url_parts = urlparse(request.get_full_url())

      # build a brand new URL by swapping out the original request URL's
      # netloc with the redirect's netloc
      redirect_url = urlunparse(ParseResult(original_url_parts.scheme,
        redirect_netloc, original_url_parts.path, original_url_parts.params,
        original_url_parts.query, original_url_parts.fragment))

      # follow the new new and return the response
      return self.parent.open(redirect_url)
    except Exception,exception:
      logger.error("Unable to follow refresh header {0}. {1}".format(
        refresh_header, str(exception)))

    # return the original response
    return response
