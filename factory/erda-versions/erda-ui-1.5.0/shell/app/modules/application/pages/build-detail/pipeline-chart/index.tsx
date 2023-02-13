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
import { YmlChart, externalKey } from 'app/yml-chart/chart';
import { map } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import PipelineNode from './pipeline-node';

interface IProps {
  changeType: string;
  data: PIPELINE.IPipelineDetail;
  onClickNode: (node: PIPELINE.ITask, mark: string) => void;
}

const notUpdateChanges = ['flow', 'stage'];
const CHART_NODE_SIZE = {
  pipeline: {
    WIDTH: 280,
    HEIGHT: 74,
  },
};

export const AppPipelineChart = (props: IProps) => {
  const { data, onClickNode, changeType } = props;
  const [{ displayData, stagesData, dataKey }, updater, update] = useUpdate({
    displayData: resetData({}) as any[][],
    stagesData: [],
    dataKey: 1,
  });

  React.useEffect(() => {
    if (notUpdateChanges.includes(changeType)) {
      return;
    }
    const { pipelineStages = [], pipelineTaskActionDetails = {} } = data || {};
    const stageTask = [] as PIPELINE.ITask[][];
    map(pipelineStages, ({ pipelineTasks }) => {
      stageTask.push(
        map(pipelineTasks, (item) => {
          const node = { ...item } as any;
          if (pipelineTaskActionDetails[node.type]) {
            node.displayName = pipelineTaskActionDetails[node.type].displayName;
            node.logoUrl = pipelineTaskActionDetails[node.type].logoUrl;
          }
          node.isType = function isType(type: string, isPrefixMatch?: boolean) {
            const isEqualType = type === node.type;
            return isPrefixMatch ? (node.type && node.type.startsWith(`${type}-`)) || isEqualType : isEqualType;
          };
          node.findInMeta = function findInMeta(fn: any) {
            if (!node.result || node.result.metadata == null) {
              return null;
            }
            return node.result.metadata.find(fn);
          };
          return node;
        }),
      );
    });

    update({
      stagesData: stageTask,
    });
  }, [update, data, changeType]);

  React.useEffect(() => {
    update((prev) => ({
      displayData: resetData({ stagesData }),
      dataKey: prev.dataKey + 1,
    }));
  }, [stagesData, update]);

  const chartConfig = {
    NODE: CHART_NODE_SIZE,
  };
  return (
    <YmlChart
      chartId="app-pipeline"
      data={displayData}
      border
      chartConfig={chartConfig}
      key={`pipeline-${dataKey}`}
      onClickNode={onClickNode}
      external={{
        nodeEleMap: {
          pipeline: PipelineNode,
        },
      }}
    />
  );
};

export default AppPipelineChart;

interface IResetObj {
  stagesData?: any[][];
}
export const resetData = (data: IResetObj) => {
  const { stagesData = [] } = data || {};
  const reData = [] as any[][];

  map(stagesData, (item, index) => {
    reData.push(
      map(item, (subItem, subIndex) => ({
        ...subItem,
        [externalKey]: { nodeType: 'pipeline', name: subItem.alias, xIndex: index, yIndex: subIndex },
      })),
    );
  });
  return reData;
};
