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

const mockData: CP_INFO_PREVIEW.Spec = {
  type: 'InfoPreview',
  data: {
    info: {
      title: '创建用户',
      desc: '这里是描述信息',
      apiInfo: { method: 'POST', path: '/api/xxxx' },
      header: [{ name: 'Accept', desc: '接受类型', required: '是', defaultValue: 'JSON' }],
      urlParams: [{ name: 'a', type: 'string', desc: 'a', required: '是', defaultValue: '123' }],
      body: [
        {
          name: 'info',
          type: 'Object',
          desc: 'info',
          required: '是',
          defaultValue: '-',
          children: [
            {
              name: 'menu',
              type: 'Array',
              desc: 'menu',
              required: '否',
              defaultValue: '-',
              children: [
                { name: 'menuName', type: 'string', desc: 'menu1', required: '否', defaultValue: '-' },
                { name: 'menuType', type: 'string', desc: 'menu1', required: '否', defaultValue: '-' },
              ],
            },
          ],
        },
      ],
      example: {
        info: {
          menu1: [{ menuName: 'xxx', menuType: 'xxx' }],
          menu2: [{ menuName: 'xxx', menuType: 'xxx' }],
          menu3: [{ menuName: 'xxx', menuType: 'xxx' }],
          menu4: [{ menuName: 'xxx', menuType: 'xxx' }],
        },
      },
    },
  },
  props: {
    render: [
      { type: 'Title', dataIndex: 'title' },
      { type: 'Desc', dataIndex: 'desc' },
      { type: 'BlockTitle', props: { title: '请求信息' } },
      { type: 'API', dataIndex: 'apiInfo' },
      {
        type: 'Table',
        dataIndex: 'header',
        props: {
          title: '请求头',
          columns: [
            { title: '名称', dataIndex: 'name' },
            { title: '描述', dataIndex: 'desc' },
            { title: '必需', dataIndex: 'required' },
            { title: '默认值', dataIndex: 'defaultValue' },
          ],
        },
      },
      {
        type: 'Table',
        dataIndex: 'urlParams',
        props: {
          title: 'URL参数',
          columns: [
            { title: '名称', dataIndex: 'name' },
            { title: '类型', dataIndex: 'type' },
            { title: '描述', dataIndex: 'desc' },
            { title: '必需', dataIndex: 'required' },
            { title: '默认值', dataIndex: 'defaultValue' },
          ],
        },
      },
      {
        type: 'Table',
        dataIndex: 'body',
        props: {
          title: '请求体',
          columns: [
            { title: '名称', dataIndex: 'name' },
            { title: '类型', dataIndex: 'type' },
            { title: '描述', dataIndex: 'desc' },
            { title: '必需', dataIndex: 'required' },
            { title: '默认值', dataIndex: 'defaultValue' },
          ],
        },
      },
      {
        type: 'FileEditor',
        dataIndex: 'example',
        props: {
          title: '展示样例',
        },
      },
      { type: 'BlockTitle', props: { title: '响应信息' } },
      { type: 'Title', props: { title: '响应头: 无', level: 2 } },
    ],
  },
};

export default mockData;
