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
import { FormModal, MemberSelector } from 'common';
import { Modal, Tabs, Radio, Button, message } from 'antd';
import { map, isEmpty, cloneDeep, intersection, get } from 'lodash';
import { changePerm2Yml } from 'user/stores/_perm-state';
import { createIssue } from 'project/services/issue';
import IterationSelect from 'project/common/components/issue/iteration-select';
import i18n from 'i18n';
import './perm-export.scss';

const { TabPane } = Tabs;
const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;
interface IProps {
  roleMap: Obj;
  projectId: string;
  activeScope: string;
  data: any;
  isEdit: boolean;
  onSubmit: (arg: any) => void;
}

const checkValidRole = (data: Obj, role: Obj) => {
  const roleKeys = map(role, 'value');
  const newData = cloneDeep(data);

  const check = (_d: Obj) => {
    // eslint-disable-next-line no-param-reassign
    if (_d.role) _d.role = intersection(_d.role, roleKeys);
    map(_d, (item) => {
      typeof item === 'object' && check(item);
    });
  };
  check(newData);
  return newData;
};

const PermExport = (props: IProps) => {
  const { data, isEdit, onSubmit, roleMap, activeScope, projectId } = props;
  const [visible, setVisible] = React.useState(false);
  const [type, setType] = React.useState('json');
  const [value, setValue] = React.useState({});
  const [tabKey, setTabKey] = React.useState(activeScope);
  const [taskVisible, setTaskVisible] = React.useState(false);
  const [invalidJson, setInvalidJson] = React.useState([] as string[]);

  React.useEffect(() => {
    setTabKey(activeScope);
  }, [activeScope]);

  React.useEffect(() => {
    if (visible || taskVisible) {
      const valueObj = {};
      map(data, (item, key) => {
        const reItem = checkValidRole(item, roleMap[key]);
        valueObj[key] = {
          _valueStr: JSON.stringify(reItem, null, 2),
          ...reItem,
        };
      });
      setValue(valueObj);
    }
  }, [data, roleMap, taskVisible, visible]);

  React.useEffect(() => {
    const inValidArr = [] as string[];
    map(value, (item: any) => {
      const { _valueStr } = item;
      if (!isValidJsonStr(_valueStr)) inValidArr.push(item.name);
    });
    setInvalidJson(inValidArr);
  }, [value]);

  const onOk = () => {
    if (isEmpty(invalidJson)) {
      const submitData = {};
      map(value, (item, key) => {
        const { _valueStr } = item;
        submitData[key] = JSON.parse(_valueStr);
      });
      onSubmit(submitData);
      onCancel();
    }
  };

  const onCancel = () => {
    setType('json');
    setVisible(false);
    setValue({});
  };

  const changeValueStr = (val: string, key: string) => {
    setValue({
      ...value,
      [key]: {
        ...value[key],
        _valueStr: val,
      },
    });
  };

  const isValidJsonStr = (_jsonStr: string) => {
    try {
      JSON.parse(_jsonStr);
      return true;
    } catch (e) {
      return false;
    }
  };

  const CancelBtn = (
    <Button key="cancel" onClick={onCancel}>
      {i18n.t('cancel')}
    </Button>
  );
  const OkBtn = (
    <Button key="ok" onClick={onOk} type="primary">
      {i18n.t('ok')}
    </Button>
  );
  const modalProps =
    isEdit && type === 'json'
      ? {
          maskClosable: false,
          footer: [CancelBtn, OkBtn],
        }
      : {
          footer: [CancelBtn],
        };

  const taskFieldsList = [
    {
      label: i18n.t('title'),
      name: 'title',
      initialValue: `${get(data, `${tabKey}.name`)}权限修改`,
      itemProps: {
        maxLength: 255,
      },
    },
    {
      label: i18n.t('dop:iteration'),
      name: 'iterationID',
      getComp: () => (projectId ? <IterationSelect /> : '请在具体项目下添加任务'),
    },
    {
      label: i18n.t('dop:assignee'),
      name: 'assignee',
      getComp: () =>
        projectId ? (
          <MemberSelector allowClear={false} scopeType="project" scopeId={projectId} />
        ) : (
          '请在具体项目下添加任务'
        ),
    },
    {
      label: i18n.t('description'),
      name: 'content',
      type: 'textArea',
      required: false,
      itemProps: {
        placeholder: i18n.t('please enter'),
      },
    },
  ];

  const addTask = (v: Obj) => {
    const curScope = activeScope;
    const curPerm = value[activeScope];
    const curScopeName = curPerm.name;
    const { _valueStr, ...rest } = curPerm; // 当前权限
    const curRole = roleMap[curScope]; // 当前角色
    const _roleMapStr = JSON.stringify(curRole, null, 2);
    const _ymlStr = changePerm2Yml(rest, curScope, curRole);
    const content = `${v.content || ''}
### ${curScopeName}角色数据
￥
const ${curScope}RoleMap = ${_roleMapStr}
￥
### ${curScopeName}权限数据
￥
const ${curScope}Perm = ${_valueStr}
￥
### ${curScopeName} yml数据
￥
${_ymlStr}
￥
`;

    const { iterationID } = v;
    const newTask: any = {
      ...v,
      iterationID: +iterationID,
      type: 'TASK',
      projectID: +projectId,
      content: content.replace(/￥/g, '```'),
    };

    createIssue(newTask).then(() => {
      message.success(i18n.t('added successfully'));
      closeTask();
    });
  };

  const closeTask = () => {
    setTaskVisible(false);
  };

  return (
    <div className="dice-perm-export flex justify-between items-center">
      <Button className="mr-2" size="small" onClick={() => setVisible(true)}>
        {i18n.t('export')}
      </Button>
      <Button value="task" className="mr-2" size="small" onClick={() => setTaskVisible(true)}>
        {i18n.t('add {name}', { name: i18n.t('task') })}
      </Button>
      <Modal
        title={i18n.t('dop:permission configuration')}
        visible={visible}
        onOk={onOk}
        onCancel={onCancel}
        width={800}
        {...modalProps}
      >
        <Tabs
          activeKey={tabKey}
          onChange={(curKey: string) => setTabKey(curKey)}
          tabBarExtraContent={
            <RadioGroup value={type} onChange={(e: any) => setType(e.target.value)}>
              <RadioButton value="json">json</RadioButton>
              <RadioButton value="yml">yml</RadioButton>
            </RadioGroup>
          }
          renderTabBar={(p: any, DefaultTabBar) => <DefaultTabBar {...p} onKeyDown={(e: any) => e} />}
        >
          {map(value, (item: any, key: string) => {
            const { _valueStr, ...rest } = item;
            const _roleStr = JSON.stringify(roleMap[key], null, 2);
            return (
              <TabPane tab={item.name} key={key}>
                {type === 'json' ? (
                  <div className="flex justify-between items-center dice-perm-export-data">
                    <div className="flex-1 mr-2 flex flex-col justify-center">
                      <span>{item.name}权限数据</span>
                      <textarea
                        readOnly={!isEdit}
                        className="dice-perm-export-pre"
                        value={_valueStr}
                        onChange={(e) => changeValueStr(e.target.value, key)}
                      />
                    </div>
                    <div className="flex-1 ml-2 flex flex-col justify-center">
                      <span>{item.name}角色数据</span>
                      <textarea readOnly className="dice-perm-export-pre" value={_roleStr} />
                    </div>
                  </div>
                ) : (
                  <textarea readOnly className="dice-perm-export-pre" value={changePerm2Yml(rest, key, roleMap[key])} />
                )}
              </TabPane>
            );
          })}
        </Tabs>
        <div className="text-danger">
          {type === 'json' && !isEmpty(invalidJson) ? `${invalidJson.join('/')} 为无效json` : ''}
        </div>
      </Modal>
      <FormModal
        title={i18n.t('task')}
        fieldsList={taskFieldsList}
        visible={taskVisible}
        onOk={addTask}
        onCancel={closeTask}
      />
    </div>
  );
};

export default PermExport;
