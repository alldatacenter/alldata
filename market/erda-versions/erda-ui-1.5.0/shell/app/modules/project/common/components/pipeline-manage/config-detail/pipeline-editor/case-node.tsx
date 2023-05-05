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
import i18n from 'i18n';
import { Menu, Dropdown, Tooltip } from 'antd';
import { Icon as CustomIcon } from 'common';
import { get, isEmpty, map, find } from 'lodash';
import { getSnippetNodeDetail } from 'project/services/auto-test-case';
import { SCOPE_AUTOTEST, scopeMap } from 'project/common/components/pipeline-manage/config';
import routeInfoStore from 'core/stores/route';
import './case-node.scss';

export interface IProps {
  data: Obj;
  editing: boolean;
  onClickNode: (data: any, arg: any) => void;
  onDeleteNode: (data: any) => void;
}

const getOutputs = (data: AUTO_TEST.ISnippetDetailRes) => {
  let outputs = [] as string[];
  map(data, (item) => {
    if (!isEmpty(item.outputs)) {
      outputs = outputs.concat(item.outputs);
    }
  });
  return outputs;
};

const noop = () => {};

export const nodeSize = { WIDTH: 280, HEIGHT: 84 };
export const CaseNode = (props: IProps) => {
  const { data, editing, onClickNode = noop, onDeleteNode = noop, ...rest } = props;

  const projectId = routeInfoStore.getState((s) => s.params.projectId);

  const [outputsDetail, setOutputsDetail] = React.useState([] as string[]);
  const [loaded, setLoaded] = React.useState(false);

  React.useEffect(() => {
    setOutputsDetail([]);
    setLoaded(false);
  }, [data]);

  const menu = (
    <Menu
      onClick={({ domEvent, key }: any) => {
        domEvent && domEvent.stopPropagation();
        if (key === 'delete') {
          onDeleteNode(data);
        }
      }}
    >
      <Menu.Item key="delete">{i18n.t('delete')}</Menu.Item>
    </Menu>
  );

  const onClick = () => {
    onClickNode(data, { editing, ...rest });
  };

  const getContent = (scope: string) => {
    let contentStr = '';
    switch (scope) {
      case scopeMap.autoTest.scope:
        {
          const apiCount = get(data, 'snippet_config.labels.apiCount');
          if (apiCount) {
            contentStr = ` ${i18n.t('number of API')}: ${apiCount}`;
          }
        }
        break;
      default:
        break;
    }
    return contentStr;
  };

  let content = ' ';
  let name = ' ';
  let IconComp = data.logoUrl ? (
    <img src={data.logoUrl} className="w-full h-full" />
  ) : (
    <CustomIcon type={'jiedian'} color className="w-full h-full" />
  );

  const curNodeScope = get(data, 'snippet_config.labels.snippet_scope') || SCOPE_AUTOTEST;
  const scopeObj: Obj = find(map(scopeMap), { scope: curNodeScope }) || {};
  switch (data?.type) {
    case 'api-test': {
      const url = get(data, 'params.url');
      const method = get(data, 'params.method');
      content = `${method} ${url}`;
      name = `${data.displayName || data.type}: ${data.alias}`;
      break;
    }
    case 'snippet':
      name = `${scopeObj.name}: ${data.alias}`;
      content = getContent(curNodeScope);
      IconComp = (
        <CustomIcon
          type={scopeMap[curNodeScope] ? scopeMap[curNodeScope].icon : 'jiedian'}
          color
          className="w-full h-full"
        />
      );
      break;
    default:
      name = `${data.displayName || data.type}: ${data.alias}`;
  }

  const tooltipTxt = (
    <>
      <div>{name}</div>
      {content && <div>{content}</div>}
      {outputsDetail.length ? (
        <>
          <div>{i18n.t('dop:output parameter')}:</div>
          {map(outputsDetail, (item, idx) => (
            <div key={`${item}${idx}`}>&nbsp;&nbsp;{item}</div>
          ))}
        </>
      ) : null}
    </>
  );

  const onVisibleChange = (vis: boolean) => {
    if (vis && !loaded) {
      const { snippet_config, alias, type } = data;
      const snippetConfigs = [] as any[];
      if (type === 'snippet' && !isEmpty(snippet_config)) {
        snippetConfigs.push({
          alias,
          ...snippet_config,
          labels: {
            ...(get(snippet_config, 'labels') || {}),
            projectID: projectId,
          },
        });
      } else {
        snippetConfigs.push({
          alias,
          source: 'action',
          name: type,
          labels: {
            actionJson: JSON.stringify(data),
            actionVersion: data.version,
            projectID: projectId,
          },
        });
      }
      (getSnippetNodeDetail({ snippetConfigs }) as unknown as Promise<any>).then((res: any) => {
        setOutputsDetail(getOutputs(res.data));
        setLoaded(true);
      });
    }
  };

  const getLoopRender = () => {
    const { loop = {} } = data;
    if (!isEmpty(loop) && loop.break) {
      const { strategy = {} } = loop;
      const tip = (
        <div onClick={(e: any) => e.stopPropagation()}>
          <div className="font-bold">{i18n.t('dop:loop strategy')}</div>
          {loop.break && <div className="pl-2">{`${i18n.t('dop:condition to end loop')}: ${loop.break}`}</div>}
          {strategy.max_times && (
            <div className="pl-2">{`${i18n.t('dop:maximum number of loop')}: ${strategy.max_times}`}</div>
          )}
          {strategy.decline_ratio && (
            <div className="pl-2">{`${i18n.t('dop:decline ratio')}: ${strategy.decline_ratio}`}</div>
          )}
          {strategy.decline_limit_sec && (
            <div className="pl-2">{`${i18n.t('dop:Maximum value of decline')}: ${strategy.decline_limit_sec}${i18n.t(
              'common:second(s)',
            )}`}</div>
          )}
          {strategy.interval_sec && (
            <div className="pl-2">{`${i18n.t('dop:interval')}: ${strategy.interval_sec}${i18n.t(
              'common:second(s)',
            )}`}</div>
          )}
        </div>
      );
      return (
        <Tooltip title={tip}>
          <CustomIcon
            className="text-desc text-base hover-active"
            type="xunhuan"
            onClick={(e) => e.stopPropagation()}
          />
        </Tooltip>
      );
    }
    return null;
  };

  return (
    <Tooltip title={editing ? undefined : tooltipTxt} onVisibleChange={onVisibleChange}>
      <div className="yml-chart-node test-case-node flex flex-col justify-center" onClick={onClick}>
        <div className={'case-title'}>
          <div className="title-icon mr-3">{IconComp}</div>
          <div className="title-txt flex flex-col justify-center text-normal">
            <span className="nowrap text-base font-bold name">{name}</span>
          </div>
          {editing ? (
            <div>
              <Dropdown trigger={['click']} overlay={menu}>
                <CustomIcon type="more" onClick={(e) => e.stopPropagation()} />
              </Dropdown>
            </div>
          ) : (
            getLoopRender()
          )}
        </div>
        <div className="nowrap mt-2">{content}</div>
      </div>
    </Tooltip>
  );
};
