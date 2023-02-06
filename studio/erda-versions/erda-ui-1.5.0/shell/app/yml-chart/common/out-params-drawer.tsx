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
import { Drawer, Input, Select, Button } from 'antd';
import { Form } from 'dop/pages/form-editor/index';
import i18n from 'i18n';
import { uniq, map, compact, flatten, get, isEmpty } from 'lodash';
import { getSnippetNodeDetail } from 'project/services/auto-test-case';
import routeInfoStore from 'core/stores/route';

import './out-params-drawer.scss';

export interface IOutParamsDrawerProps {
  visible: boolean;
  nodeData: PIPELINE.IPipelineOutParams[];
  editing: boolean;
  closeDrawer: () => void;
  onSubmit?: (arg: PIPELINE.IPipelineOutParams[]) => void;
  ymlObj: IPipelineYmlStructure;
}

const getOutputs = (data: AUTO_TEST.ISnippetDetailRes) => {
  let outputs = [] as string[];
  map(data, (item) => {
    if (!isEmpty(item.outputs)) {
      outputs = outputs.concat(item.outputs);
    }
  });
  return outputs;
};

const noop = () => {};
const OutParamsDrawer = (props: IOutParamsDrawerProps) => {
  const { visible, closeDrawer, onSubmit: submit = noop, nodeData, editing, ymlObj } = props;
  const projectId = routeInfoStore.getState((s) => s.params.projectId);
  const formRef = React.useRef(null as any);
  const [outputList, setOutputList] = React.useState([] as string[]);
  const [formValue, setFormValue] = React.useState({} as Obj);

  React.useEffect(() => {
    if (visible) {
      const stages = map(flatten(get(ymlObj, 'stages')));
      const snippetConfigs = [] as any[];

      map(stages, (item: any) => {
        const { snippet_config, alias, type } = item;
        if (type === 'snippet' && !isEmpty(snippet_config)) {
          snippetConfigs.push({
            alias,
            ...snippet_config,
            labels: {
              ...(get(snippet_config, 'labels') || {}),
              projectID: projectId,
            },
          });
        } else {
          snippetConfigs.push({
            alias,
            source: 'action',
            name: type,
            labels: {
              actionJson: JSON.stringify(item),
              actionVersion: item.version,
              projectID: projectId,
            },
          });
        }
      });
      getSnippetNodeDetail({ snippetConfigs }).then((res: any) => {
        setOutputList(getOutputs(res.data));
      });
    }
  }, [projectId, visible, ymlObj]);

  React.useEffect(() => {
    setFormValue({
      outputs: get(nodeData, 'data'),
    });
  }, [nodeData]);

  const fields = React.useMemo(
    () => [
      {
        label: i18n.t('dop:outputs configuration'),
        component: 'arrayObj',
        key: 'outputs',
        required: false,
        disabled: !editing,
        wrapperProps: {
          // extra: '修改角色key或删除角色后，导出数据会删除对应的角色',
        },
        componentProps: {
          defaultItem: { name: '', ref: '' },
          itemRender: (_data: Obj, updateItem: Function) => {
            return (
              <div className="out-params-content w-full">
                <Input
                  key="name"
                  className={`flex-1 content-item mr-2 ${!_data.name ? 'empty-error' : ''}`}
                  disabled={!editing}
                  value={_data.name}
                  onChange={(e: any) => updateItem({ name: e.target.value })}
                  placeholder={i18n.t('please enter {name}', { name: i18n.t('dop:parameter name') })}
                />
                <Select
                  key="ref"
                  className={`flex-1 content-item ${!_data.ref ? 'empty-error' : ''}`}
                  disabled={!editing}
                  value={_data.ref}
                  onChange={(val: any) => updateItem({ ref: val })}
                  placeholder={i18n.t('please choose {name}', { name: i18n.t('dop:parameter value') })}
                >
                  {map(outputList, (item) => (
                    <Select.Option key={item}>{item}</Select.Option>
                  ))}
                </Select>
              </div>
            );
          },
        },
        rules: [
          {
            validator: (val = []) => {
              let tip = '';
              const nameArr = map(val, 'name');
              const refArr = map(val, 'ref');
              const reg = /^[a-zA-Z0-9_]*$/;

              if (compact(nameArr).length !== val.length || compact(refArr).length !== val.length) {
                tip = i18n.t('{name} can not empty');
              } else if (uniq(nameArr).length !== val.length) {
                tip = i18n.t('the same {key} exists', { key: 'key' });
              } else {
                map(nameArr, (nameItem: string) => {
                  if (!tip) {
                    tip =
                      nameItem.length > 50
                        ? `${i18n.t('dop:parameter name')} ${i18n.t('length is {min}~{max}', { min: 1, max: 50 })}`
                        : !reg.test(nameItem)
                        ? `${i18n.t('dop:parameter name')} ${i18n.t('includes letters, numbers and underscores')}`
                        : '';
                  }
                });
              }
              // if (!tip) {
              //   const keyReg = /^[a-zA-Z]+$/;
              //   valueArr.forEach((item) => {
              //     if (!keyReg.test(item)) {
              //       tip = i18n.t('key only can be letters');
              //     }
              //   });
              // }
              return [!tip, tip];
            },
          },
        ],
      },
    ],
    [editing, outputList],
  );

  React.useEffect(() => {
    if (formRef && formRef.current) {
      formRef.current.setFields(fields);
    }
  }, [fields]);

  const onSubmit = () => {
    if (formRef && formRef.current) {
      formRef.current.onSubmit((val: any) => {
        submit(get(val, 'outputs'));
        closeDrawer();
      });
    }
  };

  const drawerProps = editing
    ? {
        title: i18n.t('dop:outputs configuration'),
        maskClosable: false,
      }
    : {
        title: i18n.t('dop:outputs form'),
        maskClosable: true,
      };
  return (
    <Drawer
      visible={visible}
      destroyOnClose
      onClose={closeDrawer}
      width={600}
      {...drawerProps}
      className="pipeline-out-params-drawer"
    >
      <Form fields={fields} value={formValue} formRef={formRef} />
      {editing ? (
        <div className="pipeline-out-params-drawer-footer">
          <Button onClick={closeDrawer} className="mr-2">
            {i18n.t('cancel')}
          </Button>
          <Button onClick={onSubmit} type="primary">
            {i18n.t('ok')}
          </Button>
        </div>
      ) : null}
    </Drawer>
  );
};

export default OutParamsDrawer;
