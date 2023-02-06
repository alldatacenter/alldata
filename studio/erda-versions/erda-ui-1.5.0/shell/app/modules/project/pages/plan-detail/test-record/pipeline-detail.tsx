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

import { BuildLog } from 'app/modules/application/pages/build-detail/build-log';
import PipelineChart from 'app/modules/application/pages/build-detail/pipeline-chart';
import { ciBuildStatusSet } from 'application/pages/build-detail/config';
import { EmptyHolder, ErdaIcon } from 'common';
import { isEmpty } from 'lodash';
import { Spin } from 'antd';
import React from 'react';
import i18n from 'i18n';

interface IProps {
  pipelineDetail: IPipelineDetail;
  isFetching: boolean;
  changeType: any;
}

interface IState {
  startStatus: string;
  logVisible: boolean;
  logProps: any;
  prevProps: IProps;
}

interface IPipelineDetail {
  extra: any;
  id: string;
  status: string;
  source: string;
  env: string;
  branch: string;
  ymlName: string;
  commitId: string;
  cronID: string;
  costTimeSec: string;
  commit: any;
  commitDetail: any;
  pipelineButton: any;
  pipelineCron: any;
  pipelineStages: any;
}

export class PipelineDetail extends React.Component<IProps, IState> {
  state = {
    startStatus: 'unstart', // unstart-未开始，start-已开始,end:执行完成或取消
    logVisible: false,
    logProps: {},
    prevProps: { pipelineDetail: {} },
  };

  static getDerivedStateFromProps(props: IProps, state: IState) {
    const nextState: any = { prevProps: props };
    const { pipelineDetail } = props;
    const { pipelineDetail: prevPipelineDetail } = state.prevProps;
    if (!isEmpty(pipelineDetail)) {
      if (pipelineDetail.id === prevPipelineDetail.id) {
        if (state.startStatus === 'unstart') {
          nextState.startStatus = ciBuildStatusSet.beforeRunningStatus.includes(pipelineDetail.status)
            ? 'unstart'
            : 'start';
        }
      } else {
        nextState.startStatus = ciBuildStatusSet.beforeRunningStatus.includes(pipelineDetail.status)
          ? 'unstart'
          : 'start';
      }
    }
    return nextState;
  }

  transData = (stages: any[]) => {
    const { startStatus } = this.state;
    return stages.map((f: any) => ({
      ...f,
      nodes: f.pipelineTasks.map((n: any) => {
        const node = { ...n, stage: f.name, starting: startStatus !== 'unstart' };
        node.isType = function isType(type: any, isPrefixMatch?: boolean) {
          const isEqualType = type === n.type;
          return isPrefixMatch ? (n.type && n.type.startsWith(`${type}-`)) || isEqualType : isEqualType;
        };
        node.findInMeta = function findInMeta(fn: any) {
          if (!node.result || node.result.metadata == null) {
            return null;
          }
          return node.result.metadata.find(fn);
        };
        return node;
      }),
    }));
  };

  onClickNode = (node: any, mark: any) => {
    switch (mark) {
      case 'log':
        this.setState({
          logVisible: true,
          logProps: {
            logId: node.extra.uuid,
            title: i18n.t('dop:interface test log'),
            customFetchAPIPrefix: `/api/apitests/pipeline/${node.pipelineID}/task/${node.id}/logs`,
            pipelineID: node.pipelineID,
            taskID: node.id,
            downloadAPI: '/api/apitests/logs/actions/download',
          },
        });
        break;
      default:
    }
  };

  hideLog = () => {
    this.setState({ logVisible: false });
  };

  render() {
    const { pipelineDetail, changeType, isFetching } = this.props;
    const { logVisible, logProps } = this.state;

    if (isEmpty(pipelineDetail)) {
      return <EmptyHolder relative style={{ justifyContent: 'start' }} />;
    }
    const { extra } = pipelineDetail;
    const stages = pipelineDetail.pipelineStages || [];
    const { showMessage = {} } = extra;

    return (
      <div className="full-spin-height">
        <Spin spinning={isFetching}>
          {showMessage && showMessage.msg ? (
            <div className="build-detail-err-msg">
              <div className="build-err-header">
                <ErdaIcon type="tishi" size="18px" className="build-err-icon" />
                <pre>{showMessage.msg}</pre>
              </div>
              <div className="build-err-stack">
                <ul style={{ listStyle: 'disc' }}>
                  {showMessage.stacks.map((stack: any) => (
                    <li>
                      <pre>{stack}</pre>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          ) : (
            <PipelineChart
              data={pipelineDetail as unknown as PIPELINE.IPipelineDetail}
              onClickNode={this.onClickNode}
              changeType={changeType}
            />
          )}
        </Spin>
        <BuildLog visible={logVisible} hideLog={this.hideLog} {...logProps} />
      </div>
    );
  }
}
