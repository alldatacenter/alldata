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

/* eslint-disable react/no-this-in-sfc */
/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import { Spin } from 'antd';
import { renderTopology } from './topology-utils-v2';
// @ts-ignore
import Snap from 'snapsvg-cjs';
import { throttle } from 'lodash';
import topologyStore from 'msp/env-overview/topology/stores/topology';
import './topology-ele.scss';

interface IProps {
  data: TOPOLOGY.ITopologyResp;
  zoom: number;
  nodeExternalParam: Obj;
  nodeEle: React.ReactNode;
  linkTextEle: React.ReactNode;
  boxEle: React.ReactNode;
  onClickNode: (arg: TOPOLOGY.INode) => void;
  setSize: (...args: any) => void;
  setScale: (arg: any) => void;
}

let onDrag = false;
const emptyFun = () => {};
const TopologyEle = (props: IProps) => {
  const {
    data,
    zoom,
    setSize,
    onClickNode = emptyFun,
    nodeExternalParam,
    nodeEle,
    linkTextEle,
    boxEle,
    setScale,
  } = props;
  const [isParsing, setIsParsing] = React.useState(false);
  const svgRef = React.useRef(null);
  const svgGroupRef = React.useRef(null);
  const boxRef = React.useRef<HTMLDivElement>(null);
  const worker = React.useRef<Worker>();
  const { setTopologySize } = topologyStore.reducers;
  const handleWheel = React.useCallback(
    throttle(
      (e: any, scale: number) => {
        if (e.deltaY > 0 && scale < 1.8) {
          setScale(scale + 0.05);
        }
        if (e.deltaY < 0 && scale > 0.2) {
          setScale(scale - 0.05);
        }
      },
      100,
      { trailing: false },
    ),
    [],
  );

  const setSvgSize = React.useCallback(() => {
    const curBox = boxRef.current;
    const width = curBox?.offsetWidth ? `${curBox.offsetWidth}px` : '100%';
    const height = curBox?.offsetHeight ? `${curBox.offsetHeight}px` : '100%';
    const curSvg = svgRef.current as any;
    if (curSvg?.node) {
      curSvg.node.setAttribute('width', width);
      curSvg.node.setAttribute('height', height);
    }
  }, []);

  React.useEffect(() => {
    worker.current = new Worker(new URL('./data-handler.worker.ts', import.meta.url));
    svgRef.current = Snap('#topology-svg');
    const curSvg = svgRef.current as any;
    // 兼容safari:
    if (curSvg) {
      svgGroupRef.current = curSvg.g();
      curSvg.drag(
        (dx: number, dy: number) => {
          // onmove
          const svgGroup = svgGroupRef.current;
          onDrag = true;
          svgGroup.attr({
            transform: svgGroup.data('origTransform') + (svgGroup.data('origTransform') ? 'T' : 't') + [dx, dy],
          });
        },
        () => {
          // onstart
          const svgGroup = svgGroupRef.current;
          svgGroup.data('origTransform', svgGroup.transform().local);
        },
        () => {
          // onend
          setTimeout(() => {
            onDrag = false;
          }, 0);
        },
      );

      // 在g标签下建一个透明的rect占满g标签内部,用于承载drag的载体，否则g.drag只能作用于节点。
      const mask = curSvg.rect(0, 0, '300%', '300%').attr({ fill: 'transparent' });
      const curG = svgGroupRef.current as any;
      curG.append(mask);
    }
    window.addEventListener('resize', setSvgSize);
    return () => {
      worker.current?.postMessage({ type: 'close' });
      worker.current?.terminate();
      window.removeEventListener('resize', setSvgSize);
    };
  }, []);

  React.useEffect(() => {
    const curG = svgGroupRef.current as any;
    const scale = new Snap.Matrix();
    scale.scale(zoom, zoom);
    /** safari中svg标签不支持transform元素。
     * 解决：
     *    新建一个g标签，将所有子svg图都添加到该group
     *    通过控制g标签的transform来达到缩放全局的需求
     */
    curG.transform(scale);
  }, [zoom]);

  React.useEffect(() => {
    setIsParsing(true);
    worker.current?.addEventListener('message', (e) => {
      setIsParsing(false);
      const { containerWidth, containerHeight } = renderTopology(e.data, svgRef.current, svgGroupRef.current, {
        ...nodeExternalParam,
        nodeEle,
        boxEle,
        linkTextEle,
        onClickNode: (detail: any) => {
          if (!onDrag) onClickNode(detail);
        },
      });
      setSvgSize();
      setTopologySize(containerWidth, containerHeight);
    });
    worker.current?.postMessage({ type: 'open', data: data.nodes });
    // setSize(containerWidth, containerHeight);
  }, [data]);

  return (
    <Spin spinning={isParsing}>
      <div className="svg-chart-container">
        <div
          className="svg-box"
          ref={boxRef}
          onWheel={(e) => {
            e.persist();
            handleWheel(e, zoom);
          }}
        >
          <svg
            id="topology-svg"
            width="100%"
            height="100%"
            className={`w-full h-full topology-svg ${zoom > 1 ? 'enlarge' : ''}`}
          />
        </div>
      </div>
    </Spin>
  );
};

export default TopologyEle;
