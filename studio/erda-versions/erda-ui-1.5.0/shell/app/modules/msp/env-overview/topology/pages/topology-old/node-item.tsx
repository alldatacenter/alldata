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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import classnames from 'classnames';
import { INode } from './components/topology-utils-v1';
import { Tooltip, Dropdown, Menu } from 'antd';
import { floor, map } from 'lodash';
import { Icon as CustomIcon, IF } from 'common';
import { goTo } from 'common/utils';
import { SVGICONS } from 'charts/components/svg-icon';
// import { getErrorDetail, getExceptionDetail } from '../../services/topology';
import { IMeshType } from '../service-mesh/service-mesh-drawer';
import routeInfoStore from 'core/stores/route';
import topologyServiceStore from 'msp/stores/topology-service-analyze';
import topologyStore from '../../stores/topology';
import serviceAnalyticsStore from 'msp/stores/service-analytics';
import i18n from 'i18n';
import './node-item.scss';
import monitorCommonStore from 'common/stores/monitorCommon';

interface INodeEle {
  [pro: string]: any;
  node: INode;
  terminusKey: string;
  timeSpan: ITimeSpan;
  nodeStyle: {
    width: number;
    height: number;
  };
  onHover: (...args: any) => void;
  outHover: (...args: any) => void;
  onClick: (...args: any) => void;
}
const MenuItem = Menu.Item;

const nameMap = {
  ConfigCenter: i18n.t('dop:configCenter'),
  RegisterCenter: i18n.t('dop:registration center'),
  APIGateway: i18n.t('dop:apiGateway'),
};

export const getRelativeNodes = (node: any, external: any) => {
  const { groupNodeMap } = external;
  const { category, parents: curParents = [] } = node;
  if (category === 'microservice') {
    const fullParents = map(curParents, 'id');
    const mRelativeNode: string[] = [];
    const mUnRelativeNode: string[] = [];
    map(groupNodeMap, (item) => {
      const {
        parents,
        topologyExternal: { uniqName },
        id,
      } = item;
      const beParentId = map(parents, 'id');
      if (fullParents.includes(id) || beParentId.includes(node.id)) {
        mRelativeNode.push(uniqName);
      } else {
        mUnRelativeNode.push(uniqName);
      }
    });
    return {
      relativeNode: mRelativeNode,
      unRelativeNode: mUnRelativeNode,
    };
  }
  return null;
};

// 节点组件
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const NodeEle = ({ node, onHover, outHover, onClick, timeSpan, terminusKey, nodeStyle, ...rest }: INodeEle) => {
  const [hoverFlag, setHoverFlag] = React.useState(null as any);
  // const [requestFlag, setRequestFlag] = React.useState(false);
  // const [detailInfo, setDetailInfo] = React.useState({} as any);
  const {
    name,
    id,
    type,
    applicationId,
    serviceName,
    applicationName,
    runtimeName,
    metric: { rt, count, error_rate, running, stopped },
  } = node;
  const { width, height } = nodeStyle;
  const style = { width, height };
  const [params, curentRoute] = routeInfoStore.useStore((s) => [s.params, s.currentRoute]);
  const metaData = monitorCommonStore.useStore((s) => s.globalTimeSelectSpan.data);
  const scale = topologyStore.useStore((s) => s.scale);
  const activedNode = topologyServiceStore.useStore((s) => s.activedNode);
  const serviceId = serviceAnalyticsStore.useStore((s) => s.serviceId);
  const isServicePage = curentRoute.path.includes('service');

  React.useEffect(() => {
    if (hoverFlag === null) return;
    if (hoverFlag) {
      // getDetailInfo();
      onHover(node);
    } else {
      outHover(node);
    }
  }, [hoverFlag]);

  // const getDetailInfo = () => {
  //   if (!isEmpty(detailInfo) || requestFlag || type !== 'Service') return;
  //   const start = timeSpan.startTimeMs;
  //   const end = timeSpan.endTimeMs;
  //   setRequestFlag(true);
  //   Promise.all([
  //     getExceptionDetail({
  //       sum: 'count',
  //       align: false,
  //       start,
  //       end,
  //       filter_terminus_key: terminusKey,
  //       filter_runtime_name: runtimeName,
  //       filter_service_name: serviceName,
  //       filter_application_id: applicationId,
  //     }),
  //     getErrorDetail({
  //       filter_error: true,
  //       sum: 'elapsed_count',
  //       align: false,
  //       start,
  //       end,
  //       filter_target_terminus_key: terminusKey,
  //       filter_target_runtime_name: runtimeName,
  //       filter_target_service_name: serviceName,
  //       filter_target_application_id: applicationId,
  //       field_gte_http_status_code_min: 500,
  //     }),
  //   ]).then(([exceptionResult, errorResult]) => {
  //     if (exceptionResult.success && errorResult.success) {
  //       const exception = get(exceptionResult, 'data.results[0].data[0]["sum.count"].data') || 0;
  //       const error = get(errorResult, 'data.results[0].data[0]["sum.elapsed_count"].data') || 0;
  //       setDetailInfo({ exception, error });
  //     } else {
  //       setRequestFlag(false);
  //     }
  //   }).catch(() => {
  //     setRequestFlag(false);
  //   });
  // };

  // 暂时使用和原svgIcon中的一致图标
  const iconObj = SVGICONS[type.toLowerCase()] || SVGICONS.addon;
  const iconImg = iconObj.img.replace('image:///', '/');
  const TipText = () => {
    return (
      <div className="topology-node-tip">
        <div>
          {type === 'Service' ? i18n.t('service') : i18n.t('name')}: {name}
        </div>
        {runtimeName && <div>Runtime: {runtimeName}</div>}
        {applicationName && (
          <div>
            {i18n.t('application')}: {applicationName}
          </div>
        )}
        {/* { detailInfo.error !== undefined && <div>{i18n.t('msp:http error request')}: {detailInfo.error}</div> }
        { detailInfo.exception !== undefined && <div>{i18n.t('msp:application error')}: {detailInfo.exception}</div> } */}
      </div>
    );
  };
  const ServiceTipText = () => {
    return (
      <div className="topology-node-tip">
        <div>
          {i18n.t('service')}: {name}
        </div>
        {runtimeName && <div>Runtime: {runtimeName}</div>}
        {applicationName && (
          <div>
            {i18n.t('application')}: {applicationName}
          </div>
        )}
        {!!stopped && (
          <div>
            {i18n.t('msp:stopped instance')}: {stopped}
          </div>
        )}
        {!!running && (
          <div>
            {i18n.t('msp:running instance')}: {running}
          </div>
        )}
        {!!count && (
          <div>
            {i18n.t('call times')}: {count}
          </div>
        )}
        {!!error_rate && (
          <div>
            {i18n.t('msp:request error rate')}: {floor(error_rate, 2)}%
          </div>
        )}
        <div>
          {i18n.t('response time')}: {floor(rt, 2)}(ms)
        </div>
        <div>
          {i18n.t('type')}: {type}
        </div>
      </div>
    );
  };

  const isMeshNode = node.serviceMesh === 'on';

  const nodeOperations = (
    <Menu>
      <MenuItem
        onClick={({ domEvent }: any) => {
          domEvent.stopPropagation();
          rest.toggleDrawer(IMeshType.circuitBreaker, node);
        }}
      >
        {i18n.t('msp:circuit breaker and current limiting')}
      </MenuItem>
      <MenuItem
        onClick={({ domEvent }: any) => {
          domEvent.stopPropagation();
          rest.toggleDrawer(IMeshType.faultInject, node);
        }}
      >
        {i18n.t('msp:fault inject')}
      </MenuItem>
    </Menu>
  );

  // 当缩放比例小于 0.5 时，为小节点模式
  if (scale <= 0.5) {
    if (type === 'Service') {
      return (
        <Tooltip title={ServiceTipText}>
          <div
            className={'topology-node'}
            onClick={onClick}
            style={style}
            onMouseEnter={() => setHoverFlag(true)}
            onMouseLeave={() => setHoverFlag(false)}
          >
            <div className="node-title small-node-title font-bold">
              {stopped ? (
                <>
                  <CustomIcon type="wks1" className="error-icon mr-5" />
                  <span>
                    {stopped}/{running}
                  </span>
                </>
              ) : (
                <>
                  <CustomIcon type="cg" className="common-icon mr-5" />
                  <span>{running}</span>
                </>
              )}
            </div>
            <div className="node-info">
              <div className="info-item">
                <span className="info-value small-info-value font-bold">
                  <IF check={error_rate}>
                    <span className="text-danger">{error_rate}%</span>/<span>{count}</span>
                    <IF.ELSE />
                    {count}
                  </IF>
                </span>
              </div>
            </div>
          </div>
        </Tooltip>
      );
    }

    return (
      <Tooltip title={TipText}>
        <div
          className={'topology-node simple-node'}
          onClick={isServicePage ? undefined : onClick}
          style={style}
          onMouseEnter={() => setHoverFlag(true)}
          onMouseLeave={() => setHoverFlag(false)}
        >
          <div className="node-title small-node-title">
            <div className="node-icon">
              <img src={iconImg} />
            </div>
          </div>
        </div>
      </Tooltip>
    );
  }

  if (type === 'ConfigCenter' || type === 'RegisterCenter') {
    return (
      <Tooltip title={TipText}>
        <div
          className={'topology-node simple-node'}
          onClick={isServicePage ? undefined : onClick}
          style={style}
          onMouseEnter={() => setHoverFlag(true)}
          onMouseLeave={() => setHoverFlag(false)}
        >
          <div className="node-title">
            <div className="node-icon">
              <img src={iconImg} />
            </div>
            <div className="node-name">
              <span className="text font-bold">{nameMap[type] || name}</span>
              <span className="sub-text">{runtimeName}</span>
              <span className="sub-text">{applicationName}</span>
            </div>
            {isMeshNode ? (
              <div className="h-full">
                <CustomIcon type="sz" onClick={(e: any) => e.stopPropagation()} />
              </div>
            ) : null}
          </div>
        </div>
      </Tooltip>
    );
  }

  return (
    <Tooltip title={type === 'Service' ? ServiceTipText : TipText}>
      <div
        className={classnames({
          'topology-node': true,
          actived: id === activedNode?.id || (isServicePage ? node.serviceId === serviceId : undefined),
        })}
        onClick={isServicePage ? undefined : onClick}
        style={style}
        onMouseEnter={() => setHoverFlag(true)}
        onMouseLeave={() => setHoverFlag(false)}
      >
        <div className="node-title">
          <div className="node-icon">
            <img src={iconImg} />
          </div>
          <div className="node-name">
            <span className="text font-bold">{nameMap[type] || name}</span>
            {type === 'Service' ? (
              stopped ? (
                <div>
                  <CustomIcon type="wks1" style={{ color: 'red' }} />
                  <span className="text-danger">{stopped}</span>/
                  <span>
                    {running} {i18n.t('instance')}
                  </span>
                </div>
              ) : (
                <span>
                  {running} {i18n.t('instance')}
                </span>
              )
            ) : (
              <>
                <span className="sub-text">{runtimeName}</span>
                <span className="sub-text">{applicationName}</span>
              </>
            )}
          </div>
          {isMeshNode ? (
            <div className="h-full node-operation">
              <Dropdown overlayClassName="topology-node-dropdown" overlay={nodeOperations}>
                <CustomIcon
                  className="text-lg operation-item pl-2 pr-2 pb-2"
                  type="sz"
                  onClick={(e: any) => e.stopPropagation()}
                />
              </Dropdown>
            </div>
          ) : null}
        </div>
        <div className="node-info">
          <div className="info-item">
            <span className="info-value font-bold">
              <IF check={error_rate}>
                <span className="text-danger">{floor(error_rate, 2)}%</span>/<span>{count}</span>
                <IF.ELSE />
                {count}
              </IF>
            </span>
            <span className="info-key">
              <IF check={error_rate}>
                <span className="text-danger">{i18n.t('msp:request error rate')}</span>/
                <span>{i18n.t('call times')}</span>
                <IF.ELSE />
                {i18n.t('call times')}
              </IF>
            </span>
          </div>
          <div className="info-item-separate" />
          <div className="info-item">
            <span className="info-value font-bold">{floor(rt, 2)}</span>
            <span className="info-key">{i18n.t('response time')}(ms)</span>
          </div>
        </div>
      </div>
    </Tooltip>
  );
};
export default NodeEle;
