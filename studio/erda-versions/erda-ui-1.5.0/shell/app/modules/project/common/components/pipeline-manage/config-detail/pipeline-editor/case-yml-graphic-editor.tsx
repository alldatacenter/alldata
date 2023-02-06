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
import { Drawer, Tabs } from 'antd';
import i18n from 'i18n';
import { get } from 'lodash';
import { PipelineGraphicEditor } from 'app/yml-chart/common/pipeline-graphic-editor';
import { PipelineNodeForm } from 'yml-chart/common/pipeline-node-drawer';
import { CaseTreeSelector } from './case-tree-selector';
import './case-yml-graphic-editor.scss';

interface ICaseNodeDrawer {
  nodeData: null | AUTO_TEST.ICaseDetail;
  editing: boolean;
  isCreate?: boolean;
  addDrawerProps: Obj;
  otherTaskAlias?: string[];
  onSubmit?: (options: any) => void;
  closeDrawer: () => void;
  visible: boolean;
  scope: string;
}

const CaseNodeDrawer = (props: ICaseNodeDrawer) => {
  const {
    visible,
    editing,
    nodeData: propsNodeData,
    isCreate,
    addDrawerProps,
    otherTaskAlias,
    onSubmit,
    closeDrawer,
    scope,
  } = props;

  let title = i18n.t('dop:new node');
  if (!isCreate) {
    title = `${editing ? i18n.t('edit') : i18n.t('common:view')} ${get(propsNodeData, 'alias') || ''}`;
  }
  const [key, setKey] = React.useState(1);
  const [chosenKey, setChosenKey] = React.useState('addNode');

  const curNodeType = get(propsNodeData, 'type');

  React.useEffect(() => {
    if (visible) {
      setChosenKey(curNodeType === 'snippet' ? 'addCaseRef' : 'addNode');
    }
  }, [visible, curNodeType]);

  React.useEffect(() => {
    setKey((prev) => prev + 1);
  }, [visible]);

  const { excludeAction, curCaseId } = addDrawerProps || {};
  return (
    <Drawer
      width={560}
      visible={visible}
      onClose={closeDrawer}
      maskClosable={!editing}
      title={title}
      destroyOnClose
      className="case-node-drawer"
    >
      {excludeAction ? (
        <CaseTreeSelector
          closeDrawer={closeDrawer}
          key={key}
          scope={scope}
          editing={editing}
          otherTaskAlias={otherTaskAlias}
          curCaseId={curCaseId}
          nodeData={propsNodeData}
          onChange={(node: any) => {
            onSubmit && onSubmit(node);
          }}
        />
      ) : editing ? (
        <Tabs
          className="h-full case-node-chosen-tabs"
          activeKey={chosenKey}
          onChange={(aKey: string) => editing && setChosenKey(aKey)}
        >
          <Tabs.TabPane
            tab={i18n.t('add {name}', { name: i18n.t('task') })}
            key="addNode"
            disabled={!isCreate && chosenKey === 'addCaseRef'}
          >
            <PipelineNodeForm
              key={key}
              {...props}
              {...(curNodeType === 'snippet' ? { isCreate: true, nodeData: null } : {})}
            />
          </Tabs.TabPane>
          <Tabs.TabPane
            tab={i18n.t('dop:node reference')}
            key="addCaseRef"
            disabled={!isCreate && chosenKey === 'addNode'}
          >
            <CaseTreeSelector
              closeDrawer={closeDrawer}
              key={key}
              scope={scope}
              curCaseId={curCaseId}
              editing={editing}
              otherTaskAlias={otherTaskAlias}
              nodeData={propsNodeData}
              onChange={(node: any) => {
                onSubmit && onSubmit(node);
              }}
            />
          </Tabs.TabPane>
        </Tabs>
      ) : chosenKey === 'addNode' ? (
        <PipelineNodeForm
          key={key}
          {...props}
          {...(curNodeType === 'snippet' ? { isCreate: true, nodeData: null } : {})}
        />
      ) : chosenKey === 'addCaseRef' ? (
        <CaseTreeSelector
          key={key}
          scope={scope}
          curCaseId={curCaseId}
          otherTaskAlias={otherTaskAlias}
          closeDrawer={closeDrawer}
          nodeData={propsNodeData}
          editing={editing}
          onChange={(node: any) => {
            onSubmit && onSubmit(node);
          }}
        />
      ) : null}
    </Drawer>
  );
};

const CaseYmlGraphicEditor = (props: any) => {
  const { addDrawerProps, scope } = props;
  return (
    <PipelineGraphicEditor
      {...props}
      PipelineNodeDrawer={(p: any) => <CaseNodeDrawer {...p} addDrawerProps={addDrawerProps} scope={scope} />}
    />
  );
};

export default CaseYmlGraphicEditor;
