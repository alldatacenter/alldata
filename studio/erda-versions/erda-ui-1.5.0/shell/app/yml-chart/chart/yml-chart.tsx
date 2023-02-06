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
// @ts-ignore
import Snap from 'snapsvg-cjs';
import { externalKey, CHART_CONFIG, CHART_NODE_SIZE } from './config';
import { renderSvgChart } from './yml-chart-utils';
import { get } from 'lodash';
import './yml-chart.scss';

export interface IData {
  [externalKey]: {
    nodeType: string;
  };
}

interface IChartConfig {
  NODE?: {
    [pro: string]: { WIDTH: number; HEIGHT: number };
  };
  MARGIN?: { X: number; Y: number };
  PADDING?: { X: number; Y: number };
}

interface IProps {
  [pro: string]: any;
  chartId: string;
  data: IData[][];
  editing?: boolean;
  border?: boolean;
  chartConfig: IChartConfig;
  zoom?: number;
  onClickNode?: (...data: any) => void;
  onDeleteNode?: (data: any) => void;
  external?: Obj;
}

// const onDrag = false;
export const YmlChart = (props: IProps) => {
  const {
    data,
    zoom = 1,
    chartConfig: propsChartConfig,
    editing = false,
    external = {},
    chartId: id = 'yml-svg',
    border,
    ...rest
  } = props;
  const svgRef = React.useRef(null);
  const svgGroupRef = React.useRef(null);
  const boxRef = React.useRef(null);

  const chartConfig = {
    ...CHART_CONFIG,
    MARGIN: CHART_CONFIG.MARGIN.NORMAL,
    ...propsChartConfig,
    NODE: {
      ...CHART_NODE_SIZE,
      ...(get(propsChartConfig, 'NODE') || {}),
    },
  };

  React.useEffect(() => {
    svgRef.current = Snap(`#${id}`);
    const curSvg = svgRef.current as any;
    // 拖动代码：备用
    if (curSvg) {
      // 兼容safari:
      svgGroupRef.current = curSvg
        .g()
        .drag
        // (dx: number, dy: number) => { // onmove
        //    onDrag = true;
        //    this.attr({
        //      transform: this.data('origTransform') + (this.data('origTransform') ? 'T' : 't') + [dx, dy],
        //    });
        // },
        // () => {  onstart
        //   // this.data('origTransform', this.transform().local);
        // },
        // () => {  onend
        //    setTimeout(() => {
        //      onDrag = false;
        //    }, 0);
        // }
        ();
      // 在g标签下建一个透明的rect占满g标签内部,用于承载drag的载体，否则g.drag只能作用于节点。
      const mask = curSvg.rect(0, 0, '300%', '300%').attr({ fill: 'transparent' });
      const curG = svgGroupRef.current as any;
      curG.append(mask);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 缩放部分代码：备用
  // React.useEffect(() => {
  //   const curG = svgGroupRef.current as any;
  //   const scale = new Snap.Matrix();
  //   scale.scale(zoom, zoom);
  //   /** safari中svg标签不支持transform元素。
  //   * 解决：
  //   *    新建一个g标签，将所有子svg图都添加到该group
  //   *    通过控制g标签的transform来达到缩放全局的需求
  //   */
  //   curG.transform(scale);
  // }, [zoom]);

  React.useEffect(() => {
    const { chartHeight, chartWidth } = renderSvgChart(data || [], svgRef.current, svgGroupRef.current, chartConfig, {
      chartId: id,
      editing,
      ...rest,
      ...external,
    });
    const curBox = boxRef.current as any;
    curBox.style.width = `${chartWidth}px`;
    curBox.style.height = `${chartHeight}px`;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  return (
    <div className={`yml-svg-container-box rounded h-full w-full ${border ? 'border-all' : ''}`}>
      <div className={`yml-svg-container ${editing ? 'editing' : ''}`} ref={boxRef}>
        <svg id={id} width="100%" height="100%" className={'yml-svg'} />
      </div>
    </div>
  );
};
