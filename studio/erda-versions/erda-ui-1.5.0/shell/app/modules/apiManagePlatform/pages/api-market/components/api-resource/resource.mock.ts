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

export const mockData: CP_API_RESOURCE.Spec = {
  type: 'ApiResource',
  data: {
    apiData: {
      apiMethod: 'get',
      apiName: '/api/get/1',
      operationId: 'test1',
      description: '描述',
      parameters: [
        {
          name: 'newQueryParameter',
          required: true,
          in: 'query',
          schema: {
            example: 'Example',
            type: 'string',
          },
        },
        {
          name: 'newHeader',
          required: true,
          in: 'header',
          schema: {
            example: 'Example',
            type: 'string',
          },
        },
        {
          name: 'newHeader1',
          required: true,
          in: 'header',
          schema: {
            example: 'Example',
            type: 'string',
          },
        },
      ],
      responses: {
        200: {
          description: '',
          content: {
            'application/json': {
              schema: {
                type: 'object',
                description: 'get-response-desc',
                required: ['myString'],
                properties: {
                  myString: {
                    type: 'string',
                    description: 'description of myString',
                    example: 'stringExample',
                    default: 'defaultString',
                    maxLength: 15,
                    minLength: 8,
                  },
                },
              },
            },
          },
        },
      },
    },
    operations: {
      submit: {
        key: 'submit',
        reload: true,
      },
    },
  },
};
