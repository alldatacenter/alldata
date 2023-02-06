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

const mockData: CP_API_EDITOR.Spec = {
  type: 'APIEditor',
  name: 'apiEditor',
  props: {
    showSave: true,
    asserts: {
      comparisonOperators: [
        {
          label: '大于',
          value: '\u003e',
        },
        {
          label: '大于等于',
          value: '\u003e=',
        },
        {
          label: '等于',
          value: '=',
        },
        {
          label: '小于等于',
          value: '\u003c=',
        },
        {
          label: '小于',
          value: '\u003c',
        },
        {
          label: '不等于',
          value: '!=',
        },
        {
          label: '包含',
          value: 'contains',
        },
        {
          label: '不包含',
          value: 'not_contains',
        },
        {
          label: '存在',
          value: 'exist',
        },
        {
          label: '不存在',
          value: 'not_exist',
        },
        {
          label: '为空',
          value: 'empty',
        },
        {
          label: '不为空',
          value: 'not_empty',
        },
        {
          label: '属于',
          value: 'belong',
        },
        {
          label: '不属于',
          value: 'not_belong',
        },
      ],
    },
    body: {
      form: {
        showTitle: false,
      },
    },
    commonTemp: {
      target: ['headers', 'body.form'],
      temp: [
        {
          key: 'key',
          render: {
            props: {
              placeholder: '参数名',
            },
            required: true,
            rules: [
              {
                msg: '参数名为英文、数字、中划线或下划线',
                pattern: '/^[a-zA-Z0-9_-]*$/',
              },
            ],
            uniqueValue: true,
          },
          title: '参数名',
          width: 100,
        },
        {
          flex: 2,
          key: 'value',
          render: {
            props: {
              options: [
                {
                  children: [
                    {
                      children: null,
                      isLeaf: true,
                      label: 'scene1',
                      // eslint-disable-next-line no-template-curly-in-string
                      value: '${{  params.input1 }}',
                      disabled: true,
                    },
                  ],
                  isLeaf: false,
                  label: '本场景入参',
                  value: '1',
                },
                {
                  children: [],
                  isLeaf: false,
                  label: '前置接口入参',
                  value: '12',
                },
                {
                  children: [],
                  isLeaf: false,
                  label: '全局变量入参',
                  value: '344',
                  disable: true,
                },
              ],
              placeholder: '可选择表达式',
            },
            required: true,
            type: 'inputSelect',
            valueConvertType: 'last',
          },
          title: '默认值',
        },
        {
          key: 'desc',
          render: {
            props: {
              placeholder: '描述',
            },
            required: false,
            type: 'textarea',
          },
          title: '描述',
          width: 300,
        },
      ],
    },
    headers: {
      showTitle: false,
    },
    methodList: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS', 'PATCH', 'COPY', 'HEAD'],
    params: {
      showTitle: false,
      temp: [
        {
          key: 'key',
          name: 'key',
          render: {
            props: {
              placeholder: '参数名',
            },
            required: true,
            rules: [
              {
                msg: '参数名为英文、数字、中划线或下划线',
                pattern: '/^[a-zA-Z0-9_-]*$/',
              },
            ],
          },
          title: '参数名',
          width: 100,
        },
        {
          flex: 2,
          key: 'value',
          name: 'value',
          render: {
            props: {
              options: [
                {
                  children: [
                    {
                      children: null,
                      isLeaf: true,
                      label: 'scene1',
                      // eslint-disable-next-line no-template-curly-in-string
                      value: '${{  params.input1 }}',
                    },
                  ],
                  isLeaf: false,
                  label: '本场景入参',
                  value: '',
                },
                {
                  children: [],
                  isLeaf: false,
                  label: '前置接口入参',
                  value: '',
                },
                {
                  children: [],
                  isLeaf: false,
                  label: '全局变量入参',
                  value: '',
                },
              ],
              placeholder: '可选择表达式',
            },
            required: true,
            type: 'inputSelect',
            valueConvertType: 'last',
          },
          title: '默认值',
        },
        {
          key: 'desc',
          name: 'desc',
          placeholder: '描述',
          render: {
            props: {
              placeholder: '描述',
            },
            required: false,
            type: 'textarea',
          },
          title: '描述',
          width: 300,
        },
      ],
    },

    apiExecute: {
      text: '执行',
      type: 'primary',
      disabled: true,
      allowSave: true,
      menu: [
        {
          text: '开发环境',
          key: 'dev',
          operations: {
            click: { key: 'execute', reload: true, meta: { env: 'dev' } },
          },
        },
        {
          text: '测试环境',
          key: 'test',
          operations: {
            click: { key: 'execute', reload: true, meta: { env: 'test' } },
          },
        },
      ],
    },
  },
  state: {
    // attemptTest: {
    //   status: 'Passed',
    //   data: {
    //     asserts: {
    //       success: false,
    //       result: [
    //         {
    //           arg: 'code',
    //           operator: '=',
    //           value: '200',
    //           success: false,
    //           actualValue: '401',
    //           errorInfo: '',
    //         },
    //       ],
    //     },
    //     response: {
    //       status: 400,
    //       headers: {
    //         Connection: ['keep-alive'],
    //         'Content-Length': [42],
    //         Date: ['Fri, 26 Feb 2021 13:41:41 GMT'],
    //       },
    //       body: {page: [1,2,3], data: {name: 'hello'}}
    //     },
    //     request: {
    //       method: "GET",
    //       url: "/docker-java-app/test?jet=${{  params.input1 }}",
    //       params: {
    //         pageSize: [10],
    //         pageNo: [1],
    //         orgId: [1]
    //       },
    //       headers:{
    //         pageSize: [10],
    //         pageNo: [1],
    //         orgId: [1]
    //       },
    //       body: {
    //         type: 'application/json',
    //         content: 'hello';
    //       }
    //     }
    //   }
    // },
    data: {
      apiSpec: {
        asserts: [
          {
            arg: 'code',
            operator: '=',
            value: '200',
          },
          {
            arg: 'success',
            operator: '=',
            value: 'true',
          },
          {
            arg: 'total',
            operator: '>=',
            value: '0',
          },
        ],
        body: {
          content: '{"name": "mobius"}',
          type: 'JSON(application/json)',
        },
        headers: null,
        id: '',
        method: 'PUT',
        name: 'test put',
        out_params: [
          {
            key: 'code',
            source: 'status',
            expression: 'status',
            matchIndex: '',
          },
          {
            key: 'success',
            source: 'body:json',
            expression: 'success',
            matchIndex: '',
          },
          {
            key: 'total',
            source: 'body:json',
            expression: 'data.total',
            matchIndex: '',
          },
        ],
        params: [
          {
            desc: '',
            key: 'jet',
            // eslint-disable-next-line no-template-curly-in-string
            value: '${{  params.input1 }}',
          },
        ],
        // eslint-disable-next-line no-template-curly-in-string
        url: '/docker-java-app/test?jet=${{  params.input1 }}',
      },
      apiSpecId: 10,
    },
    stepId: 20,
  },
  operations: {
    onChange: {
      key: 'onChange',
      reload: true,
    },
    close: {
      key: 'closeApiEdit',
      reload: false,
      command: { key: 'set', target: 'apiEditorDrawer', state: { visible: false } },
    },
  },
};

export default mockData;
