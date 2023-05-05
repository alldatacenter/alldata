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

import React from 'react';
import { Button } from 'antd';
import { Form } from 'dop/pages/form-editor/index';

const CP_FORM_TEST = () => {
  const formRef = React.useRef(null as any);
  const [isRequired, setIsRequired] = React.useState(true);

  const formConfig = React.useMemo(
    () => [
      // {
      //   label: '任务名称',
      //   component: 'input',
      //   required: true,
      //   key: 'alias',
      //   componentProps: {
      //     placeholder: '请输入任务名称',
      //   },
      // },
      // {
      //   label: '版本',
      //   component: 'select',
      //   required: true,
      //   key: 'version',
      //   dataSource: {
      //     type: 'static',
      //     static: [
      //       {
      //         name: '1.0',
      //         value: '1.0',
      //       },
      //     ],
      //   },
      // },
      // {
      //   label: '执行条件',
      //   component: 'input',
      //   key: 'if',
      //   componentProps: {
      //     placeholder: '请输入执行条件',
      //   },
      // },
      // {
      //   key: 'params',
      //   component: 'formGroup',
      //   group: 'params',
      //   componentProps: {
      //     title: '任务参数',
      //     showDivider: true,
      //     indentation: true,
      //   },
      // },
      // {
      //   label: 'uri',
      //   component: 'input',
      //   key: 'params.uri',
      //   group: 'params',
      //   componentProps: {
      //     placeholder: '请输入数据',
      //   },
      //   defaultValue: '((gittar.repo))',
      //   labelTip: '仓库完整地址。可使用占位符 ((gittar.repo))',
      // },
      // {
      //   label: 'branch',
      //   component: 'input',
      //   key: 'params.branch',
      //   group: 'params',
      //   componentProps: {
      //     placeholder: '请输入',
      //   },
      //   labelTip: '要检出的远程引用,支持 分支,标签和 commit-sha。可使用占位符 ((gittar.branch))',
      //   defaultValue: '((gittar.branch))',
      // },
      // {
      //   label: 'username',
      //   component: 'input',
      //   key: 'params.username',
      //   group: 'params',
      //   componentProps: {
      //     placeholder: '请输入',
      //   },
      //   labelTip: '用户名。可使用占位符 ((gittar.username))',
      //   defaultValue: '((gittar.username))',
      // },
      // {
      //   label: 'password',
      //   component: 'input',
      //   key: 'params.password',
      //   group: 'params',
      //   componentProps: {
      //     placeholder: '请输入',
      //   },
      //   labelTip: '密码。可使用占位符 ((gittar.password))',
      //   defaultValue: '((gittar.password))',
      // },
      // {
      //   label: 'depth',
      //   component: 'input',
      //   key: 'params.depth',
      //   group: 'params',
      //   componentProps: {
      //     placeholder: '请输入',
      //   },
      //   labelTip: 'git clone --depth 参数，浅克隆。 因此如果指定了该参数，将不能得到完整的克隆。',
      //   defaultValue: '1',
      // },
      // {
      //   key: 'resource',
      //   component: 'formGroup',
      //   group: 'resource',
      //   componentProps: {
      //     title: '运行资源',
      //     showDivider: true,
      //     indentation: true,
      //   },
      // },
      // {
      //   label: 'cpu',
      //   component: 'input',
      //   key: 'resource.cpu',
      //   group: 'resource',
      //   componentProps: {
      //     placeholder: '请输入',
      //     addonAfter: '核',
      //   },
      //   defaultValue: '0.3',
      // },
      // {
      //   label: 'mem',
      //   component: 'input',
      //   key: 'resource.mem',
      //   group: 'resource',
      //   componentProps: {
      //     placeholder: '请输入',
      //     addonAfter: 'MB',
      //   },
      //   defaultValue: '512',
      // },
      {
        label: 'a',
        component: 'arrayObj',
        required: true,
        key: 'a',
        componentProps: {
          objItems: [
            {
              key: 'a',
              required: true,
            },
            {
              key: 'b',
              component: 'object',
              options: 'name:string;age:number',
            },
          ],
        },
      },
    ],
    [],
  );

  const changeRequire = () => {
    setIsRequired(!isRequired);
  };

  const onFinish = (data: any) => {
    // eslint-disable-next-line no-console
    console.log('------', data);
  };

  return (
    <>
      <Form fields={formConfig} formRef={formRef} onFinish={onFinish}>
        <Form.Submit Button={Button} type="primary" />
      </Form>
      <Button onClick={changeRequire}>切换</Button>
    </>
  );
};

export default CP_FORM_TEST;
