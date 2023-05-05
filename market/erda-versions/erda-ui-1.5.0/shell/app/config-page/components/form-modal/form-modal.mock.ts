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

const mockData: CP_FORM_MODAL.Spec = {
  type: 'FormModal',
  operations: {
    submit: {
      key: 'submit',
      reload: true,
    },
  },
  props: {
    title: '添加',
    fields: [
      {
        key: 'name',
        label: '名称',
        component: 'input',
        required: true,
        componentProps: {
          maxLength: 200,
        },
      },
      {
        key: 'desc',
        label: '描述',
        component: 'textarea',
        required: false,
        componentProps: {
          maxLength: 1000,
        },
      },
      {
        key: 'type',
        label: '添加类型',
        component: 'input',
        required: false,
        visible: false, // 隐藏项
      },
    ],
  },
  state: {
    visible: false,
    formData: undefined,
  },
};

export default mockData;
