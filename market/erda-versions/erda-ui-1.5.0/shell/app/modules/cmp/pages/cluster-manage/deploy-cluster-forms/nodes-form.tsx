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
import i18n from 'i18n';
import { IFormProps, regRulesMap, FormUnitContainer } from '../form-utils';
import { EditableTable } from './edit-table';
import { RenderPureForm, ReadonlyForm } from 'common';
import { Switch, Select, Radio, Button } from 'antd';
import { map, uniqBy, get } from 'lodash';
import clusterDashboardStore from 'app/modules/dcos/stores/dashboard';

const { Option } = Select;

const addColums = [
  {
    title: 'ip',
    dataIndex: 'ip',
    editable: true,
    rules: [{ ...regRulesMap.ip }],
  },
  {
    title: 'type',
    dataIndex: 'type',
    type: 'select',
    className: 'node-type',
    options: ['app', 'lb', 'master'],
    editable: true,
  },
  {
    title: 'tag',
    dataIndex: 'tag',
    editable: true,
  },
];

let uid = 0;
const setUid = (list: any[]) => {
  return map(list, (item) => {
    const re = { ...item };
    if (re.uid === undefined) {
      re.uid = uid;
      uid += 1;
    }
    return re;
  });
};

// 节点列表
export const NodesForm = ({ form, isReadonly, data, curRef }: IFormProps) => {
  const formPrefix = 'config';
  const nodeLabels = clusterDashboardStore.useStore((s) => s.nodeLabels);

  const [mutilNode, setMutilNode] = React.useState([] as any);
  const [tagList, setTagList] = React.useState([]);
  const [nodeType, setNodeType] = React.useState('app');

  const [helperVis, setHelperVis] = React.useState(false);

  const [dataList, setDataList] = React.useState(form.getFieldValue(['config', 'nodes']) || []);

  React.useEffect(() => {
    const initNodes = get(data, 'config.nodes') || [];
    setDataList(setUid([...initNodes]));
  }, [data]);

  React.useEffect(() => {
    form.setFieldsValue({ 'config.nodes': dataList });
  }, [dataList, form]);

  const onNodeChange = (val: string[]) => {
    const validVal = val.filter((item) => regRulesMap.ip.pattern.test(item));
    setMutilNode(validVal);
  };
  const onTagChange = (val: any) => {
    setTagList(val);
  };
  const clearSet = () => {
    setMutilNode([]);
    setTagList([]);
  };
  const onSet = () => {
    const setValues = [] as any;
    if (mutilNode && mutilNode.length) {
      mutilNode.forEach((n: string) => {
        if (n) {
          setValues.push({ ip: n, type: nodeType, tag: tagList.join(',') });
        }
      });
    }
    handleNode('add', setValues);
  };

  const handleNode = (operations: string, nodes: any) => {
    const curNodeValue = [...dataList];
    let newNodes = setUid([...curNodeValue]);
    if (operations === 'add') {
      const addNodes = setUid(Array.isArray(nodes) ? nodes : [nodes]);
      newNodes = uniqBy(addNodes.concat(newNodes), 'ip');
    } else if (operations === 'del') {
      newNodes = newNodes.filter((item: any) => item.uid !== nodes.uid);
    } else if (operations === 'edit') {
      const editIdx = newNodes.findIndex((i: any) => nodes.uid === i.uid);
      const item = newNodes[editIdx];
      newNodes.splice(editIdx, 1, { ...item, ...nodes });
      newNodes = uniqBy(newNodes, 'ip');
    }
    setDataList(newNodes);
  };

  const fieldsList = [
    {
      getComp: () => {
        return (
          <div className="set-helper">
            <div className="set-helper-title">
              <span>{i18n.t('batch setting')}</span>
              <Switch checked={helperVis} onClick={() => setHelperVis(!helperVis)} />
            </div>
            <div className={`set-helper-form ${helperVis ? 'block' : 'hidden'}`}>
              <Select
                mode="tags"
                tokenSeparators={[';', ',', ' ']}
                dropdownStyle={{ display: 'none' }}
                value={mutilNode}
                onChange={onNodeChange}
                placeholder={i18n.t('please enter a valid ip, separated by a semicolon or a space')}
              />

              <Radio.Group
                value={nodeType}
                onChange={(e: any) => {
                  setNodeType(e.target.value);
                }}
                buttonStyle="solid"
              >
                {['app', 'lb', 'master'].map((item: string) => {
                  return (
                    <Radio.Button key={item} value={item}>
                      {item}
                    </Radio.Button>
                  );
                })}
              </Radio.Group>
              <Select
                mode="multiple"
                placeholder={i18n.t('cmp:please select the label')}
                value={tagList}
                onChange={onTagChange}
              >
                {map(nodeLabels, (tag) => (
                  <Option key={tag}>{tag}</Option>
                ))}
              </Select>
              <div className="set-buttons">
                <Button onClick={clearSet}>{i18n.t('reset')}</Button>
                <Button onClick={onSet} type="primary">
                  {i18n.t('setting')}
                </Button>
              </div>
            </div>
          </div>
        );
      },
    },
    {
      name: `${formPrefix}.nodes`,
      itemProps: { type: 'hidden' },
    },
    {
      getComp: () => {
        return (
          <EditableTable
            form={form}
            data={dataList}
            columns={addColums}
            add={(n: any) => {
              handleNode('add', n);
            }}
            edit={(n: any) => {
              handleNode('edit', n);
            }}
            del={(n: any) => {
              handleNode('del', n);
            }}
          />
        );
      },
    },
  ];

  return (
    <FormUnitContainer title={i18n.t('cmp:node list')} curRef={curRef}>
      {isReadonly ? (
        <ReadonlyForm fieldsList={fieldsList} data={data} />
      ) : (
        <RenderPureForm list={fieldsList} form={form} layout="vertical" className="deploy-form-render" />
      )}
    </FormUnitContainer>
  );
};
