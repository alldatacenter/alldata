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
import { isEmpty, get, isNumber, debounce } from 'lodash';
import { Popover, Tooltip } from 'antd';
import { Icon as CustomIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { secondsToTime } from 'common/utils';
import { useEffectOnce } from 'react-use';
import { ciNodeStatusSet, ciStatusMap } from '../config';
import classnames from 'classnames';
import { approvalStatusMap } from 'application/pages/deploy-list/deploy-list';
import i18n from 'i18n';
import userStore from 'user/stores';
import { WithAuth } from 'user/common';

import './pipeline-node.scss';

interface IProps {
  data: Obj;
  className?: string;
  onClickNode: (data: any, arg?: any) => void;
}

const { executeStatus } = ciNodeStatusSet;
const PipelineNode = (props: IProps) => {
  const { data, onClickNode, className = '' } = props;
  const curUserId = userStore.useStore((s) => s.loginUser.id);

  const intervalRef = React.useRef(null as any);

  const [{ time }, updater] = useUpdate({
    time: 0,
  });

  useEffectOnce(() => {
    setTime();
    return () => {
      clearInterval(intervalRef.current);
      updater.time(0);
    };
  });

  React.useEffect(() => {
    setTime();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  const setTime = () => {
    if (data.status === 'Running') {
      let currentTime = time;
      if (time <= data.costTimeSec) {
        currentTime = data.costTimeSec;
      }
      updater.time(currentTime);
      clearInterval(intervalRef.current);
      intervalRef.current = setInterval(() => {
        updater.time((_i: number) => _i + 1);
      }, 1000);
    } else {
      clearInterval(intervalRef.current);
      updater.time(data.costTimeSec);
    }
  };

  const isEmptyExtraInfo = () => {
    if (isEmpty(data.result) || (!data.result.version && !data.result.metadata && !data.result.errors)) {
      return false;
    }
    return true;
  };

  const renderTooltipTitle = (): any => {
    const { result } = data;
    const detailInfo = [];

    if (result && isEmptyExtraInfo()) {
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
      // detailInfo.push(`<h4>版本: ${version.ref || ''}</h4>`);
      if (!isEmpty(metadata)) {
        const temp: any[] = [];
        (metadata || []).forEach(
          (m: any, index: number) =>
            !skip.includes(m.name) &&
            temp.push(
              <div key={`meta-${String(index)}`} className="app-pipeline-chart-msg-item">
                <span className="app-pipeline-chart-msg-item-name">{m.name}</span>
                <pre>{m.value}</pre>
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
            <div key={`error-${String(idx)}`} className="app-pipeline-chart-msg-item">
              <span className="app-pipeline-chart-msg-item-name error">{error.name || 'error'}</span>
              <pre>{error.value || error.msg}</pre>
            </div>
          )),
        );
        // <pre className="flow-chart-err-block">
        //   {(errors || []).map((e: any, index: number) => <div key={`tooltip-${index}`}><code>{e.msg || e.code}</code></div>)}
        // </pre>
      }
      // if (!isEmpty(errors)) {
      //   detailInfo.push(<h4 className="mt-2">{i18n.t('error')}</h4>);
      //   detailInfo.push(
      //     <pre className="flow-chart-err-block">
      //       {(errors || []).map((e: any, index: number) => <div key={`tooltip-${index}`}><code>{e.msg || e.code}</code></div>)}
      //     </pre>
      //   );
      // }

      return {
        title: null,
        content: (
          <div key={data.id} className="panel-info">
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
    }

    return null;
  };

  const renderOperation = () => {
    const { isType, status, result, costTimeSec } = data;

    const operations = [];
    // 右侧跳转链接图标
    if (status === 'Success') {
      if (isType('it') || isType('ut')) {
        operations.push(['link', 'test-link', i18n.t('test')]);
      }
      // if (name === 'sonar') {
      //   operations.push(this.['link', 'sonar-link', '代码质量']);
      // }
    }

    if (status === 'Running' || (executeStatus.includes(status) && isNumber(costTimeSec) && costTimeSec !== -1)) {
      operations.push(['log', 'log', i18n.t('log')]);
    }

    if (result) {
      const { metadata } = result;
      if (metadata != null) {
        const runtimeIDObj = metadata.find((a: any) => a.name === 'runtimeID');
        if (runtimeIDObj) {
          operations.push(['link', 'link', i18n.t('overview')]);
        }
        const releaseIDObj = metadata.find((a: any) => a.name === 'releaseID');
        if (releaseIDObj) {
          operations.push(['link', 'release-link', i18n.t('dop:version details')]);
        }
        const publisherIDObj = metadata.find((a: any) => a.name === 'publisherID');
        if (publisherIDObj) {
          operations.push(['link', 'publisher-link', i18n.t('publisher:publisher content')]);
        }
      }
    }

    return <>{operations.map(([icon, mark, tip]) => getIconOperation(icon, mark, tip))}</>;
  };

  const renderConfirm = () => {
    const { status, result } = data;
    const operations = [];
    let hasAuth = false;

    if (result) {
      const { metadata } = result;
      if (metadata != null) {
        if (status === approvalStatusMap.WaitApprove.value) {
          operations.push(['duigou', 'accept', i18n.t('accept')]);
          operations.push(['gb', 'reject', i18n.t('reject')]);
        }
        const processorIDObj = metadata.find((a: any) => a.name === 'processor_id');
        if (processorIDObj) {
          hasAuth = JSON.parse(processorIDObj.value).includes(curUserId);
        }
      }
    }

    if (!operations.length) {
      return null;
    }

    return (
      <div className="flex justify-between items-center pipeline-item-extra-op-list">
        {operations.map(([icon, mark, tip]) => {
          const clickFunc = debounce((e: any) => clickIcon(e, mark), 300);
          return (
            <WithAuth key={mark} pass={hasAuth}>
              <Tooltip title={tip}>
                <span
                  className="pipeline-item-extra-op flex flex-wrap justify-center items-center"
                  onClick={(e: any) => {
                    e.persist();
                    clickFunc(e);
                  }}
                >
                  <CustomIcon type={icon} />
                  {tip}
                </span>
              </Tooltip>
            </WithAuth>
          );
        })}
      </div>
    );
  };

  const getIconOperation = (icon: string, mark: string, tip: string) => {
    const clickFunc = debounce((e: any) => clickIcon(e, mark), 300);
    return (
      <Tooltip key={mark} title={tip}>
        <span
          className="hover-active"
          onClick={(e: any) => {
            e.persist();
            clickFunc(e);
          }}
        >
          <CustomIcon type={icon} />
        </span>
      </Tooltip>
    );
  };

  const clickIcon = (e: any, mark: string) => {
    e.stopPropagation();
    onClickNode && onClickNode(data, mark);
  };

  let titleContent = null;

  const status = ciStatusMap[data.status];
  const itemStatus = ciStatusMap[data.status].color;
  const statusContent = (
    <span className="flex-1">
      <span className="yaml-editor-item-status" style={{ background: itemStatus.toLowerCase() }} />
      <span className="inline-flex justify-between items-center">{status ? status.text : '-'}</span>
    </span>
  );
  if (data.name || data.displayName) {
    const titleText = data.displayName ? `${data.displayName}: ${data.name}` : data.name;
    titleContent = (
      <div className="app-pipeline-chart-node-title nowrap">
        <Tooltip title={titleText}>{titleText}</Tooltip>
      </div>
    );
  }

  const mergedClassNames = classnames(
    'app-pipeline-chart-node',
    className,
    data.status === 'Disabled' ? 'disabled-item' : '',
  );

  const timeContent =
    time >= 0 ? (
      <span className="flex items-center">
        <CustomIcon type="shijian" />
        <span>{secondsToTime(time || data.costTimeSec)}</span>
      </span>
    ) : null;

  const logoUrl = get(data, 'logoUrl');
  const icon = logoUrl ? (
    <img src={logoUrl} alt="logo" className="pipeline-item-logo" />
  ) : (
    <CustomIcon className="pipeline-item-logo" type="wfw" color />
  );

  const Container = isEmptyExtraInfo() ? Popover : React.Fragment;
  return (
    <Container {...renderTooltipTitle()}>
      <div onClick={() => onClickNode && onClickNode(data, 'node')} className={mergedClassNames}>
        <div className="flex justify-between items-center p-3">
          {icon}
          <div className="yaml-editor-item-content py-0 px-1">
            <div className="flex justify-between items-center">
              {titleContent}
              {renderOperation()}
            </div>
            <div className="flex justify-between items-center">
              {statusContent}
              {timeContent}
            </div>
            {renderConfirm()}
          </div>
        </div>
      </div>
    </Container>
  );
};

export default PipelineNode;
