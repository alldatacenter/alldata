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
import { Drawer, Tabs, Tooltip } from 'antd';
import { map, isEmpty, get, find } from 'lodash';
import { EmptyHolder } from 'common';
import { useUpdate } from 'common/use-hooks';
import { BuildLog } from 'application/pages/build-detail/build-log';
import i18n from 'i18n';
import { ResultView } from './result-view';
import './snippet-detail.scss';

interface IProps {
  pipelineDetail: PIPELINE.IPipelineDetail;
  dataList?: PIPELINE.ITask;
  visible: boolean;
  onClose: () => void;
  detailType?: string;
}

const SnippetDetail = (props: IProps) => {
  const { pipelineDetail, visible, onClose, detailType = 'log', dataList = [] } = props;

  const [{ actKey, chosenData, logProps }, updater] = useUpdate({
    actKey: detailType,
    chosenData: {},
    logProps: {},
  });

  const isEmptyExtraInfo = (_data: any) => {
    if (isEmpty(_data.result) || (!_data.result.version && !_data.result.metadata && !_data.result.errors)) {
      return false;
    }
    return true;
  };

  const renderTooltipTitle = (_data: any): any => {
    const { result } = _data;
    const detailInfo = [<div className="mb-1">{`${i18n.t('name')}: ${_data.name}`}</div>] as any[];

    if (result && isEmptyExtraInfo(_data)) {
      const { errors: perError = [], metadata: preMetadata = [] } = result;
      const errors = [...perError] as any[];
      const metadata = [] as any[];
      const files = [] as any[];
      preMetadata.forEach((item: any) => {
        if (item.name && item.name.toLowerCase().startsWith('error.')) {
          errors.push(item);
        } else if (item.type === 'DiceFile') {
          files.push(item);
        } else {
          metadata.push(item);
        }
      });

      const skip = ['linkRuntime'];
      if (!isEmpty(metadata)) {
        const temp: any[] = [];
        (metadata || []).forEach(
          (m: any, index: number) =>
            !skip.includes(m.name) &&
            temp.push(
              <div key={`meta-${String(index)}`} className="test-case-node-msg">
                <span className="test-case-node-msg-name">{m.name}</span> {m.value}
              </div>,
            ),
        );
        if (temp.length) {
          detailInfo.push(<h4>{i18n.t('dop:details')}</h4>);
          detailInfo.push(...temp);
        }
      }
      if (!isEmpty(files)) {
        detailInfo.push(<h4 className="mt-2">{i18n.t('download')}</h4>);
        detailInfo.push(
          files.map((item, idx) =>
            item.value ? (
              <div className="table-operations" key={`file-${String(idx)}`}>
                <a className="table-operations-btn" download={item.value} href={`/api/files/${item.value}`}>
                  {item.name || item.value}
                </a>
              </div>
            ) : null,
          ),
        );
      }
      if (!isEmpty(errors)) {
        detailInfo.push(<h4 className="mt-2">{i18n.t('error')}</h4>);
        detailInfo.push(
          errors.map((error, idx) => (
            <div key={`error-${String(idx)}`} className="test-case-node-msg">
              <span className="test-case-node-msg-name error">{error.name || 'error'}</span>
              {error.value || error.msg}
            </div>
          )),
        );
      }
    }
    return {
      title: (
        <div key={_data.id} className="panel-info">
          {detailInfo.map((e: any, index: number) => (
            <div key={String(index)}>{e}</div>
          ))}
        </div>
      ),
      overlayStyle: result
        ? {
            width: 'auto',
            maxWidth: '420px',
            height: 'auto',
            maxHeight: '520px',
            minWidth: '200px',
            padding: '10px',
            overflow: 'auto',
            wordBreak: 'break-all',
          }
        : null,
      placement: 'right',
    };
  };

  React.useEffect(() => {
    if (!isEmpty(dataList)) {
      updater.chosenData(dataList[0]);
    }
  }, [dataList, updater]);

  React.useEffect(() => {
    updater.actKey(detailType);
  }, [detailType, updater]);

  const clickNode = (node: PIPELINE.ITask) => {
    updater.chosenData(node);
  };

  React.useEffect(() => {
    if (!isEmpty(chosenData)) {
      const pipelineID = get(pipelineDetail, 'id');
      const nodeId = chosenData.id;
      updater.logProps({
        logId: chosenData.extra.uuid,
        title: i18n.t('msp:log details'),
        customFetchAPIPrefix: `/api/apitests/pipeline/${pipelineID}/task/${nodeId}/logs`,
        pipelineID,
        taskID: nodeId,
        downloadAPI: '/api/apitests/logs/actions/download',
      });
    }
  }, [chosenData, pipelineDetail, updater]);

  const hasResult = find(get(chosenData, 'result.metadata') || [], { name: 'api_request' });
  return (
    <Drawer
      className="auto-test-snippet-drawer"
      title={i18n.t('detail')}
      width={'80%'}
      destroyOnClose
      onClose={onClose}
      visible={visible}
    >
      <div className="snippet-detail">
        <div className="left">
          {map(dataList as PIPELINE.ITask[], (data) => {
            return (
              <Tooltip key={data.id} {...renderTooltipTitle(data)}>
                <div
                  className={`snippet-item nowrap ${data.id === get(chosenData, 'id') ? 'is-active' : ''}`}
                  onClick={() => clickNode(data)}
                >
                  {data.name}
                </div>
              </Tooltip>
            );
          })}
        </div>
        <div className="right">
          <Tabs key={get(chosenData, 'id')} activeKey={actKey} onChange={(aK: string) => updater.actKey(aK)}>
            <Tabs.TabPane tab={i18n.t('log')} key="log">
              {actKey === 'log' && logProps.logId ? <BuildLog withoutDrawer {...logProps} /> : <EmptyHolder relative />}
            </Tabs.TabPane>
            <Tabs.TabPane tab={i18n.t('dop:execute result')} key="result">
              {hasResult ? <ResultView data={chosenData} /> : <EmptyHolder relative />}
            </Tabs.TabPane>
          </Tabs>
        </div>
      </div>
    </Drawer>
  );
};

export default SnippetDetail;
