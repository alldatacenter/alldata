// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

export default {
  HTTP_METHOD_LIST: ['GET', 'POST', 'PUT', 'DELEE', 'OPTINS', 'PATCH', 'COPY', 'HEAD'],
  MAX_URL_LENGTH: 1024,
  MAX_BODY_LENGTH: 10000,
  DEFAULT_REQUEST_PARAMS: {
    method: 'GET',
    url: '',
    body: '',
    query: {},
    header: {},
  },
};
