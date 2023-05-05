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
import { Breadcrumb, Spin, Select, Input } from 'antd';
import { cloneDeep, map, isEmpty, debounce } from 'lodash';
import { JsonChecker, IF, Holder, ErdaIcon } from 'common';
import { connectCube } from 'common/utils';
import PureServiceList from './service-list';
import AssociatedAddons from '../associated-addon';
import './index.scss';
import dcosServiceStore from 'dcos/stores/services';
import clusterStore from 'cmp/stores/cluster';
import { useLoading } from 'core/stores/loading';

const { Option } = Select;

const ENV_MAP = {
  dev: { enName: 'DEV', cnName: i18n.t('cmp:development environment') },
  test: { enName: 'TEST', cnName: i18n.t('test environment') },
  staging: { enName: 'STAGING', cnName: i18n.t('staging environment') },
  prod: { enName: 'PROD', cnName: i18n.t('cmp:production environment') },
};

class ServiceManager extends React.Component {
  constructor(props) {
    super();
    this.reqSt = -1;
    this.state = {
      path: [{ q: '', name: '' }],
      cluster: '',
      environment: 'dev',
      ip: undefined,
      serviceList: [],
      containerList: [],
    };
    this.debounceGetServiceList = debounce(props.getServiceList, 300);
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    const { serviceList, containerList } = nextProps;
    const nextState = {};
    if (serviceList !== prevState.serviceList) {
      nextState.serviceList = cloneDeep(serviceList);
    }
    if (containerList !== prevState.containerList) {
      nextState.containerList = cloneDeep(containerList);
    }
    if (Object.keys(nextState).length !== 0) {
      return nextState;
    }
    return null;
  }

  componentDidMount() {
    this.props.getClusterList().then((list) => {
      !isEmpty(list) &&
        this.setState(
          {
            cluster: list[0].name,
            path: [{ q: list[0].name, name: list[0].name }],
          },
          () => {
            this.getServiceList();
          },
        );
    });
  }

  getServiceList = () => {
    const { environment, path, ip } = this.state;
    this.props.getServiceList({
      paths: path,
      environment,
      ip,
    });
  };

  componentDidUpdate(prevProps, prevState, snapshot) {
    const { serviceList, runtimeStatus, serviceReqStatus, metrics } = this.props;
    if (serviceList !== prevProps.serviceList) {
      clearInterval(this.reqSt);
      if (['runtime', 'service'].includes(this.curLevel())) {
        this.reqRuntimeStatus();
        this.reqSt = setInterval(() => this.reqRuntimeStatus(), 5000);
      }
    }
    if (runtimeStatus !== prevProps.runtimeStatus) {
      this.combineStatuToService(runtimeStatus);
    }

    if (metrics !== prevProps.metrics) {
      const { cpu = {}, mem = {} } = metrics;
      cpu.loading === false && mem.loading === false && this.combineMetricsToList(this.formatMetricsToObj(metrics)); // 两部分数据都返回后才开始combine数据
    }

    if (!serviceReqStatus) {
      clearInterval(this.reqSt);
    }
  }

  componentWillUnmount() {
    clearInterval(this.reqSt);
  }

  onJsonShow = (visible) => {
    const { runtimeJson, getRuntimeJson } = this.props;
    const runtimeId = this.getLevel('runtime').id;
    visible && runtimeJson === null && runtimeId !== 'unknown' && getRuntimeJson({ runtimeId });
  };

  combineStatuToService = (runtimeStatus) => {
    let { serviceList } = this.state;
    if (this.curLevel('runtime')) {
      // runtime的status在runtimeStatus中runtimeId为key对象中
      serviceList = serviceList.map((item) => {
        let status = '';
        try {
          status = runtimeStatus[item.id].status || '';
        } catch (e) {
          status = '';
        }
        return { ...item, status };
      });
    } else if (this.curLevel('service')) {
      // service的status在runtimeStatus对应runtimeId为key的对象中的more字段中
      const runtimeId = this.getLevel('runtime').id;
      serviceList = serviceList.map((item) => {
        let status = '';
        try {
          status = runtimeStatus[runtimeId].more[item.name] || '';
        } catch (e) {
          status = '';
        }
        return { ...item, status };
      });
    }
    this.setState({
      serviceList,
    });
  };

  formatMetricsToObj = (metrics) => {
    const metricsObj = {};
    const { cpu, mem } = metrics;
    (cpu.data || []).forEach((cItem) => {
      cItem.tag && (metricsObj[cItem.tag] = { cpuUsagePercent: cItem.data / 100 || 0, diskUsage: 0 });
    });
    (mem.data || []).forEach((mItem) => {
      if (mItem.tag) {
        !metricsObj[mItem.tag] && (metricsObj[mItem.tag] = {});
        metricsObj[mItem.tag].memUsage = mItem.data || 0;
      }
    });
    return metricsObj;
  };

  combineMetricsToList = (metricsObj) => {
    let { containerList, serviceList } = this.state;
    if (this.curLevel('container')) {
      // 当前层级在container上，
      containerList = containerList.map((item) => {
        let metrics = null;
        try {
          metrics = metricsObj[item.containerId] || null;
        } catch (e) {
          metrics = null;
        }
        return { ...item, metrics };
      });
      this.setState({
        containerList,
      });
    } else {
      const combineKye = this.curLevel('service') ? 'name' : 'id';
      serviceList = serviceList.map((item) => {
        let metrics = null;
        try {
          metrics = metricsObj[item[combineKye]] || null;
        } catch (e) {
          metrics = null;
        }
        return { ...item, metrics };
      });
      this.setState({
        serviceList,
      });
    }
  };

  handleEnvChange = (environment) => {
    this.setState({ environment }, () => {
      this.getServiceList();
    });
  };

  handleClusterChange = (cluster) => {
    this.setState(
      {
        cluster,
        path: [{ q: cluster, name: cluster }],
      },
      () => {
        this.getServiceList();
      },
    );
  };

  handleIpChange = (e) => {
    this.setState({ ip: e.target.value }, () => {
      const { environment, path, ip } = this.state;
      this.debounceGetServiceList({
        paths: path,
        environment,
        ip,
      });
    });
  };

  reqRuntimeStatus = () => {
    let runtimeIds = '';
    if (this.curLevel('runtime')) {
      // runtime,批量查询runtime的状态
      runtimeIds = map(this.state.serviceList, 'id').join(',');
    } else if (this.curLevel('service')) {
      // service,查询单个runtime状态
      runtimeIds = this.getLevel('runtime').id;
    }
    if (runtimeIds && runtimeIds !== 'unknown') {
      this.props.getRuntimeStatus({ runtimeIds });
    } else {
      clearInterval(this.reqSt);
    }
  };

  into = (p) => {
    if (this.curLevel('runtime')) this.props.clearRuntimeJson();
    this.setState(
      {
        path: this.state.path.concat(p),
      },
      () => {
        const depth = this.state.path.length;
        if (depth < 5) {
          return this.getServiceList();
        }
        this.props.getContainerList(this.state.path);
      },
    );
  };

  backTo = (depth) => {
    if (this.curLevel('runtime')) this.props.clearRuntimeJson();
    this.setState(
      {
        path: this.state.path.slice(0, depth + 1),
      },
      () => {
        this.getServiceList();
      },
    );
  };

  curLevel = (lev) => {
    const levArr = ['project', 'application', 'runtime', 'service', 'container'];
    const curLev = levArr[this.state.path.length - 1];
    return lev ? lev === curLev : curLev;
  };

  getLevel = (lev) => {
    const { path } = this.state;
    const levs = {
      project: path[1] ? { id: path[1].q, name: path[1].name } : null,
      application: path[2] ? { id: path[2].q, name: path[2].name } : null,
      runtime: path[3] ? { id: path[3].q, name: path[3].name } : null,
      service: path[4] ? { id: path[4].q, name: path[4].name } : null,
    };
    return levs[lev] || null;
  };

  render() {
    const { isFetchingClusters, isFetchingServices, isFetchingContainers, runtimeJson, list } = this.props;
    const { path, serviceList, containerList, environment, cluster, ip } = this.state;
    const jsonString = runtimeJson === null ? '' : JSON.stringify(runtimeJson, null, 2);

    // path.length: 0-service, 1-runtime, 2-service, 3-container
    return (
      <Spin spinning={isFetchingClusters}>
        <Holder when={isEmpty(list)}>
          <Breadcrumb
            separator={<ErdaIcon className="text-xs align-middle" type="right" size="14px" />}
            className="path-breadcrumb"
          >
            {path.map((p, i) => {
              const isLast = i === path.length - 1;
              return (
                <Breadcrumb.Item
                  key={i}
                  className={isLast ? '' : 'hover-active'}
                  onClick={() => {
                    if (!isLast) this.backTo(i);
                  }}
                >
                  {p.name}
                </Breadcrumb.Item>
              );
            })}
          </Breadcrumb>
          <div className="to-json">
            {path.length === 4 ? (
              <JsonChecker
                buttonText={i18n.t('runtime configs')}
                jsonString={jsonString}
                onToggle={this.onJsonShow}
                modalConfigs={{ title: i18n.t('runtime configs') }}
              />
            ) : null}
          </div>
          <IF check={path.length === 1}>
            <div className="filter-group mb-4 ml-3-group">
              <Select value={cluster} style={{ width: 300 }} onChange={this.handleClusterChange}>
                {map(list, (v) => (
                  <Option key={v.name} value={v.name}>
                    {v.displayName || v.name}
                  </Option>
                ))}
              </Select>
              <Input.Search
                allowClear
                value={ip}
                style={{ width: 150 }}
                placeholder={i18n.t('cmp:search by IP')}
                onChange={this.handleIpChange}
              />
              <Select value={environment} style={{ width: 120 }} onChange={this.handleEnvChange}>
                {map(ENV_MAP, (v, k) => (
                  <Option key={k} value={k}>
                    {v.cnName}
                  </Option>
                ))}
              </Select>
            </div>
          </IF>
        </Holder>
        <Spin spinning={isFetchingServices || isFetchingContainers}>
          <PureServiceList
            {...this.props}
            into={this.into}
            depth={path.length}
            serviceList={serviceList}
            containerList={containerList}
            haveMetrics={false}
            extraQuery={{ filter_cluster_name: cluster }}
          />
          {path.length === 2 ? (
            <AssociatedAddons projectId={path[1].q} environment={ENV_MAP[environment].enName} />
          ) : null}
        </Spin>
      </Spin>
    );
  }
}

const Mapper = () => {
  const [containerList, serviceList, runtimeJson, runtimeStatus, serviceReqStatus, metrics] = dcosServiceStore.useStore(
    (s) => [s.containerList, s.serviceList, s.runtimeJson, s.runtimeStatus, s.serviceReqStatus, s.metrics],
  );
  const list = clusterStore.useStore((s) => s.list);
  const { getClusterList } = clusterStore.effects;
  const { getContainerList, getServiceList, getRuntimeJson, getRuntimeStatus } = dcosServiceStore.effects;
  const { clearRuntimeJson, clearRuntimeStatus } = dcosServiceStore.reducers;
  const [isFetchingClusters] = useLoading(clusterStore, ['getClusterList']);
  const [isFetchingServices] = useLoading(dcosServiceStore, ['getServiceList']);
  const [isFetchingContainers] = useLoading(dcosServiceStore, ['getContainerList']);
  return {
    containerList,
    serviceList,
    runtimeJson,
    runtimeStatus,
    serviceReqStatus,
    metrics,
    list,
    getClusterList,
    isFetchingClusters,
    isFetchingServices,
    isFetchingContainers,
    getContainerList,
    getServiceList,
    getRuntimeJson,
    clearRuntimeJson,
    getRuntimeStatus,
    clearRuntimeStatus,
  };
};

export default connectCube(ServiceManager, Mapper);
