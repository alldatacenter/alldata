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
import { map, isEmpty, get, find } from 'lodash';
import { Popover, Dropdown, Menu, Tooltip } from 'antd';
import { Icon as CustomIcon } from 'common';
import { ciNodeStatusSet, ciStatusMap } from 'project/common/components/pipeline-manage/run-detail/config';
import { scopeMap } from 'project/common/components/pipeline-manage/config';
import i18n from 'i18n';
import './run-case-node.scss';

export interface IProps {
  data: Obj;
  onClickNode: (data: any, arg?: any) => void;
}

const { executeStatus } = ciNodeStatusSet;
export const runNodeSize = { WIDTH: 280, HEIGHT: 84 };
export const RunCaseNode = (props: IProps) => {
  const { data, onClickNode } = props;
  // const { logoUrl } = data;

  // const icon = logoUrl ? <img src={logoUrl} alt='logo' className='pipeline-item-icon' /> : <CustomIcon className="pipeline-item-icon" type="wfw" color />;

  const curNodeScope = get(data, 'snippet_config.labels.snippet_scope');
  const scopeObj = curNodeScope ? find(map(scopeMap), { scope: curNodeScope }) : {};

  // const content = ' ';
  let name = ' ';
  let IconComp = data.logoUrl ? (
    <img src={data.logoUrl} className="w-full h-full" />
  ) : (
    <CustomIcon type={'jiedian'} color className="w-full h-full" />
  );
  switch (data?.type) {
    // case 'api-test': {
    //   const url = get(data, 'params.url');
    //   const method = get(data, 'params.method');
    //   content = `${method} ${url}`;
    //   name = data.displayName || data.type;
    //   break;
    // }
    case 'snippet':
      name = `${get(scopeObj, 'name') || i18n.t('dop:node reference')}: ${data.name}`;
      IconComp = (
        <CustomIcon
          type={scopeMap[curNodeScope] ? scopeMap[curNodeScope].icon : 'jiedian'}
          color
          className="w-full h-full"
        />
      );
      break;
    default:
      name = `${data.displayName || data.type}: ${data.name}`;
  }

  const onClick = (target: string) => {
    onClickNode(data, target);
  };

  const getMenu = () => {
    // const { status } = data;
    const operations = [];

    const logId = get(data, 'extra.uuid');
    if (data.type === 'snippet') {
      operations.push({ key: 'log', name: i18n.t('check detail') });
    } else if (logId) {
      // status === 'Running' || executeStatus.includes(status)) {
      operations.push({ key: 'log', name: i18n.t('check log') });
    }
    const metadata = get(data, 'result.metadata') || [];
    if (data.type !== 'snippet' && !isEmpty(metadata)) {
      const api_request = find(metadata, { name: 'api_request' });
      if (api_request) operations.push({ key: 'result', name: i18n.t('dop:execute result') });
    }
    return operations;
  };

  const renderMenu = (operations: any[]) => {
    if (operations.length) {
      return (
        <Menu
          onClick={({ domEvent, key }: any) => {
            domEvent && domEvent.stopPropagation();
            onClick(key);
          }}
        >
          {map(operations, (op) => (
            <Menu.Item key={op.key}>{op.name}</Menu.Item>
          ))}
        </Menu>
      );
    }
    return null;
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

  const isEmptyExtraInfo = () => {
    if (isEmpty(data.result) || (!data.result.version && !data.result.metadata && !data.result.errors)) {
      return false;
    }
    return true;
  };

  const status = ciStatusMap[data.status] || ciStatusMap.Unknown;
  const statusContent = (
    <span className={`mt-2 test-case-status-box rounded ${status.color.toLowerCase()}`}>
      <span className="test-case-result-status" style={{ background: status.color.toLowerCase() }} />
      <span className="inline-flex justify-between items-center">{status ? status.text : '-'}</span>
    </span>
  );

  const Container = isEmptyExtraInfo() ? Popover : React.Fragment;
  const menus = getMenu();

  return (
    <Container {...renderTooltipTitle()}>
      <div
        onClick={() => onClick('node')}
        className={`yml-chart-node test-case-result-node flex flex-col justify-center ${
          data.status === 'Disabled' ? 'disabled-item' : ''
        }`}
      >
        <div className={'case-title'}>
          <div className="title-icon mr-3">{IconComp}</div>
          <div className="title-txt flex flex-col justify-center text-normal">
            <Tooltip title={name}>
              <span className="nowrap text-base font-bold name">{name}</span>
            </Tooltip>
          </div>

          <div>
            {isEmpty(menus) ? null : (
              <Dropdown trigger={['click']} overlay={renderMenu(menus)}>
                <CustomIcon type="more" onClick={(e) => e.stopPropagation()} />
              </Dropdown>
            )}
          </div>
        </div>
        {/* <Tooltip title={content}>
          <div className='nowrap mt-2'>{content}</div>
        </Tooltip> */}
        {statusContent}
      </div>
    </Container>
  );
};
