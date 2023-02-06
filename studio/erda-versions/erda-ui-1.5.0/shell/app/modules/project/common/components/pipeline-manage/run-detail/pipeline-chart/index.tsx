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
import { YmlChart } from 'yml-chart/chart';
import { NodeEleMap, NodeType, externalKey } from 'yml-chart/config';
import { map } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import { RunCaseNode, runNodeSize } from './run-case-node';

interface IProps {
  data: PIPELINE.IPipelineDetail;
  scope: string;
  onClickNode: (node: PIPELINE.ITask, mark: string) => void;
}

export const CasePipelineChart = (props: IProps) => {
  const { data, onClickNode } = props;
  const [{ displayData, stagesData, inParamsData, outParamsData, dataKey }, , update] = useUpdate({
    displayData: resetData({}) as any[][],
    stagesData: [],
    inParamsData: [],
    outParamsData: [],
    dataKey: 1,
  });

  React.useEffect(() => {
    const { pipelineStages = [], runParams = [], pipelineTaskActionDetails = {} } = data || {};
    const stageTask = [] as PIPELINE.ITask[][];
    map(pipelineStages, ({ pipelineTasks }) => {
      stageTask.push(
        map(pipelineTasks, (item) => {
          const node = { ...item };
          if (pipelineTaskActionDetails?.[node.type]) {
            node.displayName = pipelineTaskActionDetails[node.type].displayName;
            node.logoUrl = pipelineTaskActionDetails[node.type].logoUrl;
          }
          return node;
        }),
      );
    });

    update({
      stagesData: stageTask,
      inParamsData: runParams,
    });
  }, [update, data]);

  React.useEffect(() => {
    update((prev) => ({
      displayData: resetData({ stagesData, inParamsData, outParamsData }),
      dataKey: prev.dataKey + 1,
    }));
  }, [stagesData, inParamsData, outParamsData, update]);
  const chartConfig = {
    NODE: {
      pipeline: runNodeSize,
    },
  };

  return (
    <YmlChart
      chartId="pipline-result"
      data={displayData}
      border
      chartConfig={chartConfig}
      key={`pipeline-${dataKey}`}
      onClickNode={onClickNode}
      external={{
        nodeEleMap: {
          pipeline: RunCaseNode,
          startNode: () => <NodeEleMap.startNode disabled />,
          endNode: () => <NodeEleMap.endNode disabled />,
        },
      }}
    />
  );
};

export default CasePipelineChart;

// 非编辑状态下: 插入开始节点，结束节点
// 编辑状态下：插入开始节点、结束节点、层与层之间插入添加行节点
interface IResetObj {
  stagesData?: any[][];
  inParamsData?: PIPELINE.IPipelineInParams[];
  outParamsData?: PIPELINE.IPipelineOutParams[];
}
export const resetData = (data: IResetObj) => {
  const { stagesData = [], inParamsData = [], outParamsData = [] } = data || {};
  const reData = [
    [{ data: inParamsData, [externalKey]: { nodeType: NodeType.startNode } }], // 插入开始节点
  ] as any[][];

  map(stagesData, (item, index) => {
    reData.push(
      map(item, (subItem, subIndex) => ({
        ...subItem,
        [externalKey]: { nodeType: 'pipeline', name: subItem.alias, xIndex: index, yIndex: subIndex },
      })),
    );
  });
  reData.push([{ data: outParamsData, [externalKey]: { nodeType: NodeType.endNode } }]); // 添加结束节点
  return reData;
};
