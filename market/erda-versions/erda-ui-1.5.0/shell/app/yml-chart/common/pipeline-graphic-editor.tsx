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
import { map, get, omit, difference, isEmpty, flatten } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import { NodeType, CHART_NODE_SIZE, NodeEleMap } from '../config';
import { YmlChart, externalKey, CHART_CONFIG } from '../chart';
import i18n from 'i18n';
import { notify } from 'common/utils';
import DefaultPipelineNodeDrawer, { IPipelineNodeDrawerProps } from './pipeline-node-drawer';
import DefaultInParamsDrawer, { IInPramasDrawerProps } from './in-params-drawer';
import DefaultOutParamsDrawer, { IOutParamsDrawerProps } from './out-params-drawer';

export interface IPipelineGraphicEditorProps {
  ymlObj: PIPELINE.IPipelineYmlStructure;
  editing: boolean;
  nameKey?: string;
  onDeleteData: (arg: any) => void;
  addDrawerProps?: Obj;
  onAddData: (arg: any) => void;
  onAddInParams?: (arg: any) => void;
  onAddOutParams?: (arg: any) => void;
  PipelineNodeDrawer?: (props: IPipelineNodeDrawerProps) => JSX.Element;
  InParamsDrawer?: (props: IInPramasDrawerProps) => JSX.Element;
  OutParamsDrawer?: (props: IOutParamsDrawerProps) => JSX.Element;
  chartProps?: IChartProps;
}

interface IPureEditor {
  ymlObj: PIPELINE.IPipelineYmlStructure;
  editing: boolean;
  onDeleteData: (arg: any) => void;
  onClickNode: (arg: any) => void;
  chartProps?: IChartProps;
}

export interface IChartProps {
  nodeEleMap?: {
    [pro: string]: React.ReactNode;
  };
  chartSize?: {
    [pro: string]: {
      WIDTH: number;
      HEIGHT: number;
    };
  };
}

export const PurePipelineGraphicEditor = (props: IPureEditor) => {
  const { ymlObj, editing, onDeleteData, onClickNode, chartProps = {} } = props;
  const [{ displayData, stagesData, inParamsData, outParamsData, dataKey }, , update] = useUpdate({
    displayData: resetData({}, editing) as any[][],
    stagesData: [],
    inParamsData: [],
    outParamsData: [],
    dataKey: 1,
  });

  React.useEffect(() => {
    const { stages = [], params: inParams = [], outputs: outParams = [] } = ymlObj || {};
    update({
      stagesData: stages || [],
      inParamsData: inParams,
      outParamsData: outParams,
    });
  }, [update, ymlObj]);

  React.useEffect(() => {
    update((prev) => ({
      displayData: resetData({ stagesData, inParamsData, outParamsData }, editing),
      dataKey: prev.dataKey + 1,
    }));
  }, [stagesData, inParamsData, outParamsData, editing, update]);

  const chartConfig = {
    NODE: {
      ...CHART_NODE_SIZE,
      ...(chartProps.chartSize || {}),
    },
    // 编辑chart时，节点层之间新加插入节点的节点，故上下间距不一样
    MARGIN: editing ? CHART_CONFIG.MARGIN.EDITING : CHART_CONFIG.MARGIN.NORMAL,
  };
  return (
    <YmlChart
      chartId="pipeline-editor"
      data={displayData}
      editing={editing}
      chartConfig={chartConfig}
      key={dataKey}
      onClickNode={onClickNode}
      onDeleteNode={onDeleteData}
      ymlObj={ymlObj}
      external={{ nodeEleMap: { ...NodeEleMap, ...(chartProps.nodeEleMap || {}) } }}
    />
  );
};

export const PipelineGraphicEditor = (props: IPipelineGraphicEditorProps) => {
  const {
    ymlObj,
    editing,
    onDeleteData,
    onAddData,
    onAddInParams,
    onAddOutParams,
    nameKey = 'alias',
    addDrawerProps,
    PipelineNodeDrawer = DefaultPipelineNodeDrawer,
    InParamsDrawer = DefaultInParamsDrawer,
    OutParamsDrawer = DefaultOutParamsDrawer,
    ...rest
  } = props;
  const [
    { chosenNode, inParamsNode, outParamsNode, isCreate, drawerVis, inParamsDrawerVis, outParamsDrawerVis },
    updater,
    update,
  ] = useUpdate({
    chosenNode: null as any,
    isCreate: false,
    drawerVis: false,
    inParamsDrawerVis: false,
    outParamsDrawerVis: false,
    inParamsNode: null as any,
    outParamsNode: null as any,
  });

  const onClickNode = (nodeData: any) => {
    const { [externalKey]: externalData } = nodeData;
    if (externalData.nodeType === NodeType.startNode) {
      update({
        inParamsNode: nodeData,
        inParamsDrawerVis: true,
      });
    } else if (externalData.nodeType === NodeType.endNode) {
      const useable = !isEmpty(get(ymlObj, 'stages'));
      if (!useable) {
        return notify('warning', i18n.t('dop:please add tasks first, and then configure outputs'));
      }
      update({
        outParamsNode: nodeData,
        outParamsDrawerVis: true,
      });
    } else {
      update({
        isCreate: externalData.nodeType === NodeType.addRow || externalData.nodeType === NodeType.addNode,
        chosenNode: nodeData,
        drawerVis: true,
      });
    }
  };

  const onDeleteNode = (nodeData: any) => {
    onDeleteData(nodeData);
  };

  const closeDrawer = () => {
    update({
      chosenNode: null,
      drawerVis: false,
    });
  };

  const onSubmit = (newData: any) => {
    onAddData({
      node: chosenNode,
      data: newData,
    });
    closeDrawer();
  };

  const setInParams = (inP: PIPELINE.IPipelineInParams[]) => {
    onAddInParams?.(inP);
    updater.inParamsDrawerVis(false);
  };

  const setOutParams = (outP: PIPELINE.IPipelineOutParams[]) => {
    onAddOutParams?.(outP);
    updater.outParamsDrawerVis(false);
  };

  const curNodeData = React.useMemo(() => {
    return chosenNode ? omit(chosenNode, externalKey) : chosenNode;
  }, [chosenNode]);
  const allAlias = map(flatten(get(ymlObj, 'stages') || []), nameKey);

  // 用于排除新节点的别名同其他节点一致
  const otherTaskAlias = drawerVis ? difference(allAlias, [get(chosenNode, nameKey)]) : [];

  const { showInParams = false, showOutParams = false, ...restDrawerProps } = addDrawerProps || {};

  return (
    <>
      <PurePipelineGraphicEditor
        {...rest}
        ymlObj={ymlObj}
        editing={editing}
        onClickNode={onClickNode}
        onDeleteData={onDeleteNode}
      />
      <PipelineNodeDrawer
        {...restDrawerProps}
        visible={drawerVis}
        isCreate={isCreate}
        closeDrawer={closeDrawer}
        nodeData={curNodeData as IStageTask}
        editing={editing}
        otherTaskAlias={otherTaskAlias as string[]}
        onSubmit={onSubmit}
      />
      {showInParams && InParamsDrawer ? (
        <InParamsDrawer
          visible={inParamsDrawerVis}
          nodeData={inParamsNode as any}
          editing={editing}
          closeDrawer={() => update({ inParamsDrawerVis: false, inParamsNode: null })}
          onSubmit={setInParams}
        />
      ) : null}
      {showOutParams && OutParamsDrawer ? (
        <OutParamsDrawer
          visible={outParamsDrawerVis}
          nodeData={outParamsNode as any}
          editing={editing}
          closeDrawer={() => update({ outParamsDrawerVis: false, outParamsNode: null })}
          onSubmit={setOutParams}
          ymlObj={ymlObj}
        />
      ) : null}
    </>
  );
};

// 非编辑状态下: 插入开始节点，结束节点
// 编辑状态下：插入开始节点、结束节点、层与层之间插入添加行节点
interface IResetObj {
  stagesData?: any[][];
  inParamsData?: PIPELINE.IPipelineInParams[];
  outParamsData?: PIPELINE.IPipelineOutParams[];
}
export const resetData = (data: IResetObj, isEditing = false) => {
  const { stagesData = [], inParamsData = [], outParamsData = [] } = data || {};
  const reData = [
    [{ data: inParamsData, [externalKey]: { nodeType: NodeType.startNode } }], // 插入开始节点
  ] as any[][];
  if (stagesData.length === 0) {
    isEditing && reData.push([{ [externalKey]: { nodeType: NodeType.addRow, insertPos: 0 } }]); // 中间追加添加行
  } else if (isEditing) {
    reData.push([{ [externalKey]: { nodeType: NodeType.addRow, insertX: 0 } }]); // 中间追加添加行
    map(stagesData, (item, index) => {
      reData.push(
        map(item, (subItem, subIndex) => ({
          ...subItem,
          [externalKey]: { nodeType: 'pipeline', name: subItem.alias, xIndex: index, yIndex: subIndex },
        })),
      );
      reData.push([{ [externalKey]: { nodeType: NodeType.addRow, insertPos: index + 1 } }]); // 末尾追加添加行
    });
  } else {
    map(stagesData, (item, index) => {
      reData.push(
        map(item, (subItem, subIndex) => ({
          ...subItem,
          [externalKey]: { nodeType: 'pipeline', name: subItem.alias, xIndex: index, yIndex: subIndex },
        })),
      );
    });
  }
  reData.push([{ data: outParamsData, [externalKey]: { nodeType: NodeType.endNode } }]); // 添加结束节点
  return reData;
};
