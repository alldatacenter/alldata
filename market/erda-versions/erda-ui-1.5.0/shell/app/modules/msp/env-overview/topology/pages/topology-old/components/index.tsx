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
import { Spin } from 'antd';
import { isEmpty } from 'lodash';
import { EmptyHolder } from 'common';
import TopologyEle from './topology-ele';
import BoxEle from './box-ele';

import './index.scss';

interface ITopologyParent {
  id: string;
  metric: {
    [proKey: string]: any;
    count: number;
  };
}

interface INodeExternal {
  x: number;
  y: number;
  deepth: number;
  uniqName: string;
  inTotal: number;
  outTotal: number;
  id: string;
  group: string;
  subGroup: string;
  groupLevel: number;
  metric: {
    [proKey: string]: any;
    count: number;
  };
  parents: ITopologyParent[];
}
export interface ITopologyNode {
  [propKey: string]: any;
  id: string;
  name: string;
  parent: string;
  topologyExternal: INodeExternal;
}

interface IProps {
  nodeExternalParam: Obj;
  data: TOPOLOGY.ITopologyResp;
  isFetching: boolean;
  nodeEle: React.ReactNode;
  linkTextEle: React.ReactNode;
  scale: number;
  boxEle?: React.ReactNode;
  onClickNode?: (arg: any) => void;
  isTopologyOverview?: true;
  setScale: (val: number) => void;
}

const getScaleByWidth = (width: number) => {
  if (width <= 1440) {
    return 0.6;
  } else if (width > 1440 && width < 1920) {
    return 0.8;
  } else {
    return 1;
  }
};

const TopologyChart = (props: IProps) => {
  const {
    data,
    isFetching,
    nodeExternalParam,
    onClickNode,
    nodeEle,
    linkTextEle,
    boxEle = BoxEle,
    setScale,
    scale,
  } = props;
  const [chartKey, setChartKey] = React.useState(1);
  const chartBoxRef = React.useRef(null);

  React.useEffect(() => {
    setChartKey((preChartKey) => preChartKey + 1);
  }, [data]);

  const setSize = (width: number, height: number) => {
    // 获取chart图大小，根据尺寸设置初始scale。
    const curBoxRef = chartBoxRef.current as any;
    if (curBoxRef) {
      const [boxWidth, boxHeight] = [curBoxRef.offsetWidth, curBoxRef.offsetHeight];

      // 若chart实际大小比box大120%，则相应的缩小
      if (boxWidth * 1.2 < width || boxHeight * 1.2 < height) {
        // const initScale = Math.min(boxWidth / width, boxHeight / height);
        const initScale = getScaleByWidth(window.innerWidth);
        setScale(initScale);
      }
    }
  };

  return (
    <div className="topology-chart spin-full-height flex-3">
      <div className="chart-box" ref={chartBoxRef}>
        <Spin spinning={isFetching}>
          {isEmpty(data?.nodes) ? (
            <EmptyHolder />
          ) : (
            <TopologyEle
              {...props}
              nodeEle={nodeEle}
              linkTextEle={linkTextEle}
              boxEle={boxEle}
              onClickNode={onClickNode}
              key={chartKey}
              setSize={setSize}
              data={data}
              setScale={setScale}
              zoom={scale}
              nodeExternalParam={{ ...nodeExternalParam, scale }}
            />
          )}
        </Spin>
      </div>
    </div>
  );
};

export default TopologyChart;
