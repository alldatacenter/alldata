#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import unittest

from unittest.mock                      import patch
from apache_ranger.exceptions           import RangerServiceException
from apache_ranger.model.ranger_service import RangerService

try:
    from apache_ranger.client.ranger_client import API, HttpMethod, HTTPStatus, RangerClient
except ModuleNotFoundError: # requests not installed
    exit() # skipping unit tests


class MockResponse:
    def __init__(self, status_code, response=None, content=None):
        self.status_code = status_code
        self.response    = response
        self.content     = content
        return

    def json(self):
        return self.response

    def text(self):
        return str(self.content)


class TestRangerClient(unittest.TestCase):
    URL  = "url"
    AUTH = ("user", "password")

    @patch('apache_ranger.client.ranger_client.Session')
    def test_get_service_unavailable(self, mock_session):
        mock_session.return_value.get.return_value = MockResponse(HTTPStatus.SERVICE_UNAVAILABLE)
        result                                     = RangerClient(TestRangerClient.URL, TestRangerClient.AUTH).find_services()

        self.assertTrue(result is None)


    @patch('apache_ranger.client.ranger_client.Session')
    def test_get_success(self, mock_session):
        response                                   = [ RangerService() ]
        mock_session.return_value.get.return_value = MockResponse(HTTPStatus.OK, response=response, content='Success')
        result                                     = RangerClient(TestRangerClient.URL, TestRangerClient.AUTH).find_services()

        self.assertEqual(response, result)


    @patch('apache_ranger.client.ranger_client.Session')
    @patch('apache_ranger.client.ranger_client.Response')
    def test_get_unexpected_status_code(self, mock_response, mock_session):
        content                                    = 'Internal Server Error'
        mock_response.text                         = content
        mock_response.content                      = content
        mock_response.status_code                  = HTTPStatus.INTERNAL_SERVER_ERROR
        mock_session.return_value.get.return_value = mock_response

        try:
            RangerClient(TestRangerClient.URL, TestRangerClient.AUTH).find_services()
        except RangerServiceException as e:
            self.assertTrue(HTTPStatus.INTERNAL_SERVER_ERROR, e.statusCode)


    @patch('apache_ranger.client.ranger_client.RangerClient.FIND_SERVICES')
    def test_unexpected_http_method(self, mock_api):
        mock_api.method.return_value = "PATCH"
        mock_api.url                 = TestRangerClient.URL
        mock_api.path                = RangerClient.URI_SERVICE

        try:
            RangerClient(TestRangerClient.URL, TestRangerClient.AUTH).find_services()
        except RangerServiceException as e:
            self.assertTrue('Unsupported HTTP Method' in repr(e))


    def test_url_missing_format(self):
        params = {'arg1': 1, 'arg2': 2}

        try:
            API("{arg1}test{arg2}path{arg3}", HttpMethod.GET, HTTPStatus.OK).format_path(params)

            self.fail("Supposed to fail")
        except KeyError as e:
            self.assertTrue('KeyError' in repr(e))


    def test_url_invalid_format(self):
        params = {'1', '2'}

        try:
            API("{}test{}path{}", HttpMethod.GET, HTTPStatus.OK).format_path(params)

            self.fail("Supposed to fail")
        except TypeError as e:
            self.assertTrue('TypeError' in repr(e))


if __name__ == '__main__':
    unittest.main()
