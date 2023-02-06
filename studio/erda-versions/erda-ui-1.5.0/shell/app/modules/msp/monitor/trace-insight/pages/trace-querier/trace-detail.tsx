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

/*
 * @Author: unknow
 * @Date: 2018-12-24 22:19:44
 * @Last Modified by: licao
 * @Last Modified time: 2019-02-21 19:18:29
 * Just copy from old trace-detail
 */

import { each, includes, flattenDeep, map, replace, round } from 'lodash';
import React from 'react';
import { Modal, Table, Spin, Tooltip } from 'antd';
import { Ellipsis } from 'common';
import TraceDetailFilter from './trace-detail-filter';
import './trace-detail.scss';

interface ISpanDetailProps {
  [pro: string]: any;
  spanDetail?: any;
  getSpanDetailContent?: (args?: any) => any;
}

const SpanDetail = (props: ISpanDetailProps) => {
  const { spanDetailContent, getSpanDetailContent } = props;
  const { durationStr, tags, operationName: spanName = '' } = spanDetailContent.span;
  const columns2 = [
    {
      title: 'Key',
      dataIndex: 'key',
      key: 'key',
    },
    {
      title: 'Value',
      dataIndex: 'value',
      key: 'value',
      render: (text: string) => <Ellipsis overlayClassName="trace-value-tooltips" title={text} />,
    },
  ];
  const data = map(tags, (value, key) => ({ key, value }));

  return (
    <Modal
      visible={spanDetailContent.visible}
      onCancel={() => getSpanDetailContent && getSpanDetailContent({ span: spanDetailContent, visible: false })}
      className="span-detail-modal"
      width={920}
      title={[
        spanName && spanName.length > 95 ? (
          <Tooltip key="spanName" title={spanName} overlayClassName="full-span-name">
            <h3 className="title">{spanName}</h3>
          </Tooltip>
        ) : (
          <h3 key="spanName">{spanName}</h3>
        ),
        <h4 key="aka" className="sub-title">
          AKA: {tags?.service_name} {durationStr}
        </h4>,
      ]}
      footer=""
    >
      <Table
        className="no-operation second-table"
        rowKey="key"
        columns={columns2}
        dataSource={data}
        pagination={false}
        scroll={{ x: '100%' }}
      />
    </Modal>
  );
};

interface IProps {
  [pro: string]: any;
  trace?: any;
  isFetching?: boolean;
}
interface IState {
  traceTree: any[];
}

class TraceDetail extends React.Component<IProps, IState> {
  state = {
    traceTree: [],
  };

  findChildren = (_ispanId: any, children: any, spans: any) => {
    const tree: any[] = [];
    this.findChildren.bind(this);
    each(spans, (i) => {
      if (includes(children, i.spanId)) {
        tree.push(i.spanId);
        if (i.children) {
          tree.push(this.findChildren(i.spanId, i.children, spans));
        }
      }
    });
    return tree;
  };

  expandSpan = ({ spanId, children, isExpand }: any) => {
    const {
      traceDetailContent: { spans: oriSpans },
    } = this.props;
    const { traceTree } = this.state;
    const spans = traceTree.length ? traceTree : oriSpans;

    const spanArray = flattenDeep(this.findChildren(spanId, children, spans));
    const rootId = spans[0].spanId;
    const converted = map(spans, (i) => {
      const t = { ...i };
      if (i.spanId === spanId) {
        t.isExpand = !isExpand;
      }
      if (includes(spanArray, i.spanId) && i.spanId !== rootId) {
        t.isShow = !isExpand;
        t.isExpand = !isExpand;
      }
      t.state = 'tre';
      return t;
    });
    this.setState({
      traceTree: converted,
    });
  };

  roundTimeString = (timeString: string, n: number) => {
    const floatReg = /(^(-|\+)?\d+(\.\d+)?)/g;
    const roundNum = (timeString.match(floatReg) || []).pop() as any;

    return replace(timeString, floatReg, `${round(roundNum, n)}`);
  };

  render() {
    const { isTraceDetailContentFetching, traceDetailContent } = this.props;
    return (
      <div className="trace-detail-container">
        <TraceDetailFilter {...this.props} expandSpan={this.expandSpan} />
        <div className="trace-items-cont">
          <div id="timeLabel" className="span">
            <div className="handle">Services</div>
            <div className="duration-container">
              {map(traceDetailContent.timeMarkers, (i) => {
                return (
                  <div key={i.time} className={`time-marker time-marker-${i.index}`}>
                    {this.roundTimeString(i.time, 3)}
                  </div>
                );
              })}
            </div>
          </div>
          <Spin spinning={isTraceDetailContentFetching}>
            {map(this.state.traceTree.length ? this.state.traceTree : traceDetailContent.spans, (span) => {
              const {
                spanId,
                depthClass,
                parentId,
                spanName,
                serviceNames,
                duration,
                durationStr,
                children,
                errorType,
                binaryAnnotations,
                annotations,
                depth,
                isExpand,
                isShow,
                operationName,
                tags,
              } = span;
              return (
                <div
                  key={`span${spanId}`}
                  id={spanId}
                  className={`span service-span depth-${depthClass} ${isShow ? '' : 'hidden'}`}
                  data-keys="id,spanName,serviceNames,serviceName,durationStr,duration"
                  data-id={spanId}
                  data-parent-id={parentId}
                  data-span-name={spanName}
                  data-service-name={span.serviceName}
                  data-service-names={serviceNames}
                  data-duration-str={durationStr}
                  data-duration={duration}
                  data-children={children}
                  data-error-type={errorType}
                >
                  <div className="handle" onClick={() => this.expandSpan({ spanId, children, isExpand })}>
                    <div className="service-name" style={{ marginLeft: `${depth}px` }}>
                      {children ? <span className="expander">{isExpand ? '-' : '+'}</span> : ''}
                      <Tooltip title={tags.service_name}>
                        <span className="service-name-text">{tags.service_name}</span>
                      </Tooltip>
                    </div>
                  </div>

                  <div
                    className="duration-container"
                    onClick={() => this.props.getSpanDetailContent({ span, visible: true })}
                  >
                    {map(traceDetailContent.timeMarkers, (i, index) => {
                      return (
                        <div key={index} className={`time-marker time-marker-${index}`}>
                          .
                        </div>
                      );
                    })}
                    <div className="duration" style={{ left: `${span.left}%`, width: `${span.width}%` }}>
                      {map(annotations, (annotation, index) => {
                        const { isCore, left, value, endpoint, timestamp, relativeTime, serviceName } = annotation;
                        return (
                          <div
                            key={`annotation${index}`}
                            className={`annotation${isCore ? 'core' : ''}`}
                            style={{ left: `${left}%` }}
                            title={value}
                            data-keys="endpoint,value,timestamp,relativeTime,serviceName"
                            data-endpoint={endpoint}
                            data-value={value}
                            data-timestamp={timestamp}
                            data-relative-time={relativeTime}
                            data-service-name={serviceName}
                          />
                        );
                      })}
                    </div>
                    <Tooltip title={tags.spanName} overlayClassName="span-tooltip" arrowPointAtCenter>
                      <span className="span-name" style={{ left: `${span.left}%`, width: `${100 - span.left}%` }}>
                        {durationStr} : {operationName}
                      </span>
                    </Tooltip>
                  </div>
                  {map(binaryAnnotations, (binaryAnnotation) => {
                    const { key, value, annotationType } = binaryAnnotation;
                    return (
                      <div
                        key={`binaryAnnotation${key}`}
                        className="binary-annotation"
                        data-keys="key,value,type"
                        data-key={key}
                        data-value={value}
                        data-type={annotationType}
                      />
                    );
                  })}
                </div>
              );
            })}
          </Spin>
        </div>
        <SpanDetail {...this.props} />
      </div>
    );
  }
}

export default TraceDetail;
