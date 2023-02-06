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
import { Input, InputNumber, Tooltip, Button, Modal, Drawer, Row, Col, Table } from 'antd';
import { groupBy, isNaN, isEmpty, filter, get, map, round } from 'lodash';
import classNames from 'classnames';
import { IF, Icon as CustomIcon, TagsRow, TableActions } from 'common';
import { useUpdate } from 'common/use-hooks';
import { ColumnProps } from 'core/common/interface';
import { getFormatter } from 'charts/utils/formatter';
import HealthPoint from 'project/common/components/health-point';
import Terminal from 'dcos/common/containers/terminal';
import MachineTagForm from './tag-form';
import MachineOffLineModal from './machine-offline-modal';
import orgMachineStore from '../../stores/machine';
// import { OperationLog } from 'org/pages/cluster-manage/operation-history';
import { ClusterLog } from 'app/modules/cmp/pages/cluster-manage/cluster-log';
import { customTagColor } from 'dcos/common/config';
import './machine-table.scss';

const { confirm } = Modal;
const compareClass = (rate: number) => {
  if (rate > 60 && rate < 80) {
    return 'text-warning';
  } else if (rate >= 80) {
    return 'text-danger';
  }
  return 'text-success';
};

const countPercent = (used: number, total: number) => {
  const percent = Math.ceil((used / total) * 100);
  const statusClass = compareClass(percent || 0);
  return { percent, statusClass };
};

const ProgressItem = ({ percent, used, total, unit, unitType }: any) => (
  <div className="machine-percent">
    <Tooltip
      placement="top"
      title={
        unitType
          ? `${getFormatter(unitType).format(used)} / ${getFormatter(unitType).format(total)}`
          : `${round(used, 2)} ${unit} / ${round(total, 2)} ${unit}`
      }
    >
      <div
        className={classNames({
          'cursor-pointer': true,
          'machine-percent-bar': true,
          'machine-percent-error-bar': percent >= 100,
        })}
        style={{ width: `${percent}%` }}
      >
        <span>{`${percent}%`}</span>
      </div>
    </Tooltip>
  </div>
);

export const DoubleProgressItem = ({ usedPercent, requestPercent, usage, request, total, unit, unitType }: any) => {
  const requestEle = (
    <Tooltip
      key="request"
      placement="top"
      title={
        unitType
          ? `${i18n.t('cmp:allocation')}: ${getFormatter(unitType).format(request)} / ${getFormatter(unitType).format(
              total,
            )}`
          : `${i18n.t('cmp:allocation')}: ${round(request, 2)} ${unit} / ${round(total, 2)} ${unit}`
      }
    >
      <div
        className="machine-percent-bar machine-percent-bottom-bar cursor-pointer"
        style={{ width: `${requestPercent}%` }}
      >
        <span>{`${requestPercent}%`}</span>
      </div>
    </Tooltip>
  );
  const usedEle = (
    <Tooltip
      key="used"
      placement="top"
      title={
        unitType
          ? `${i18n.t('usage')}${getFormatter(unitType).format(usage)} / ${getFormatter(unitType).format(total)}`
          : `${i18n.t('usage')}${round(usage, 2)} ${unit} / ${round(total, 2)} ${unit}`
      }
    >
      <div
        className={classNames({
          'cursor-pointer': true,
          'machine-percent-bar': true,
          'machine-percent-top-bar': true,
          'machine-percent-error-bar': usedPercent >= requestPercent,
        })}
        style={{ width: `${usedPercent}%` }}
      >
        <span>{`${usedPercent}%`}</span>
      </div>
    </Tooltip>
  );
  return (
    <div className="machine-percent double-machine-percent">
      {usedPercent > requestPercent ? [usedEle, requestEle] : [requestEle, usedEle]}
    </div>
  );
};

const defaultParams = {
  user: 'root',
  port: '22',
};

interface IOperation {
  record: any;
}

class Operation extends React.PureComponent<IOperation> {
  state = {
    ...defaultParams,
    visible: false,
  };

  setValue = (key: string, value: string) => {
    this.setState({
      [key]: value,
    });
  };

  openModal = (content: any) => {
    confirm({
      className: 'terminal-confirm-modal',
      content,
      title: i18n.t('cmp:confirm login config'),
      onOk: this.openTerminal,
      onCancel: this.resetDefaultUserInfo,
    });
  };

  resetDefaultUserInfo = () => {
    this.setState({ ...defaultParams });
  };

  openTerminal = () => {
    this.setState({ visible: true });
  };

  closeSlidePanel = () => {
    this.setState({ visible: false, ...defaultParams });
  };

  render() {
    const { user, port, visible } = this.state;
    const { record } = this.props;

    const content = (
      <div className="terminal-confirm">
        <Row gutter={20}>
          <Col span={6}>
            <span className="label">User: </span>
          </Col>
          <Col span={18}>
            <Input defaultValue={defaultParams.user} onChange={(e) => this.setValue('user', e.target.value)} />
          </Col>
        </Row>
        <Row gutter={20}>
          <Col span={6}>
            <span className="label">Port: </span>
          </Col>
          <Col span={18}>
            <Input defaultValue={defaultParams.port} onChange={(e) => this.setValue('port', e.target.value)} />
          </Col>
        </Row>
      </div>
    );

    return (
      <React.Fragment>
        <span className="table-operations-btn" onClick={() => this.openModal(content)}>
          {i18n.t('console')}
        </span>
        <Drawer title={i18n.t('console')} visible={visible} onClose={this.closeSlidePanel} width="80%" destroyOnClose>
          <Terminal host={record.ip} user={user} port={port} clusterName={record.clusterName} />
        </Drawer>
      </React.Fragment>
    );
  }
}

interface IFilterDropdownProps {
  selectedKeys: any[];
  dataKey: string;
  placeholder?: string;
  setSelectedKeys: (arg: any) => void;
  confirm: () => void;
  clearFilters: () => void;
  handleSearch: (...arg: any) => void;
  handleReset: (...arg: any) => void;
}

const FilterDropdownNumber = (props: IFilterDropdownProps) => {
  const {
    setSelectedKeys,
    selectedKeys,
    confirm: filterConfirm,
    clearFilters,
    handleSearch,
    handleReset,
    dataKey,
  } = props;
  const initialVal = selectedKeys[0] || [];
  const [more, setMore] = React.useState(initialVal[0] || '');
  const [less, setLess] = React.useState(initialVal[1] || '');
  React.useEffect(() => {
    setSelectedKeys([[more || '', less || '']]);
  }, [more, less, setSelectedKeys]);
  return (
    <div className="filter-dropdown">
      <div className="row">
        <span className="label">{i18n.t('greater than')}</span>
        <InputNumber
          size="small"
          value={more}
          onChange={(val) => setMore(val)}
          onKeyDown={(e) => {
            if (e.key.toLocaleLowerCase() === 'enter') {
              handleSearch(selectedKeys, filterConfirm, dataKey);
            }
          }}
        />
        <span className="unit">%</span>
      </div>
      <div className="row">
        <span className="label">{i18n.t('less than')}</span>
        <InputNumber
          size="small"
          value={less}
          onChange={(val) => setLess(val)}
          onKeyDown={(e) => {
            if (e.key.toLocaleLowerCase() === 'enter') {
              handleSearch(selectedKeys, filterConfirm, dataKey);
            }
          }}
        />
        <span className="unit">%</span>
      </div>
      <div className="row button-wrap">
        <Button
          size="small"
          onClick={() => {
            setMore('');
            setLess('');
            handleReset(clearFilters, dataKey);
          }}
        >
          {i18n.t('rest')}
        </Button>
        <Button size="small" type="primary" onClick={() => handleSearch(selectedKeys, filterConfirm, dataKey)}>
          {i18n.t('done')}
        </Button>
      </div>
    </div>
  );
};

const FilterDropdownInput = (props: IFilterDropdownProps) => {
  const {
    setSelectedKeys,
    selectedKeys,
    confirm: filterConfirm,
    clearFilters,
    handleSearch,
    handleReset,
    dataKey,
    placeholder = '',
  } = props;
  return (
    <div className="filter-dropdown">
      <Input
        placeholder={placeholder}
        value={selectedKeys[0]}
        onChange={(e) => setSelectedKeys(e.target.value ? [e.target.value] : [])}
        onPressEnter={() => handleSearch(selectedKeys, filterConfirm, dataKey)}
      />
      <div className="row button-wrap">
        <Button size="small" onClick={() => handleReset(clearFilters, dataKey)}>
          {i18n.t('rest')}
        </Button>
        <Button size="small" type="primary" onClick={() => handleSearch(selectedKeys, filterConfirm, dataKey)}>
          {i18n.t('done')}
        </Button>
      </div>
    </div>
  );
};

const filterFunc = {
  include: (record: ORG_MACHINE.IMachine, val = '', key: string) => {
    return record[key].includes(val);
  },
  includeMult: (record: ORG_MACHINE.IMachine, val: string[] = [], key: string) => {
    if (isEmpty(val)) {
      return true;
    }
    let res = true;
    val.forEach((item) => {
      if (!record[key].includes(item)) res = false;
    });
    return res;
  },
  numberRange: (record: ORG_MACHINE.IMachine, val: string[] = [], key: string) => {
    const [more, less] = val[0] || ([] as any);
    const curVal = get(record, key);
    if (!isNaN(curVal)) {
      if (more && Number(curVal) <= Number(more)) {
        return false;
      }
      if (less && Number(curVal) >= Number(less)) {
        return false;
      }
    }
    return true;
  },
};

const FirstColTitle = ({ filterMap }: { filterMap: Record<string, any> }) => {
  const curFilter = (filterMap as any).ip;
  if (!isEmpty(curFilter)) {
    return (
      <div className="title-with-search">
        <span className="main-title">IP </span>
        <br />
        <span className="search-value">{`${curFilter[0]}`}</span>
      </div>
    );
  }
  return <span>IP</span>;
};

interface IProps {
  list: ORG_MACHINE.IMachine[];
  isFetching: boolean;
  gotoMachineMonitor: (arg: any) => void;
  gotoMachineTasks: (arg: any) => void;
}

const MachineTable = ({ list, gotoMachineMonitor, gotoMachineTasks, isFetching = false }: IProps) => {
  const { deleteMachine } = orgMachineStore.effects;
  const [dataSource, setDataSource] = React.useState([]);
  // const [labelOpt, setLabelOptArr] = React.useState([]);
  const [filterList, setFilterList] = React.useState([]);
  const [machineOffLineVis, setmachineOffLineVis] = React.useState(false);
  const [curMachine, setCurMachine] = React.useState(null as any);
  const [curRecordID, setCurRecordID] = React.useState('');
  React.useEffect(() => {
    const groupMachines = groupBy(list, (item) => item.status === 'normal');
    const { true: normalMachines, false: abnormalMachines } = groupMachines;
    const originData = (abnormalMachines || []).concat(normalMachines || []);
    const reData = originData.map((item) => {
      const { diskTotal, diskUsage } = item;
      const diskProportion = countPercent(diskUsage, diskTotal);
      return { ...item, diskProportion };
    });
    setDataSource(reData as any);
    // setLabelOptArr(sortBy(remove(uniq(labelStr.split(',')), a => !!a)) as any);
  }, [list]);

  const [filterMap, setFilterMap] = React.useState({});
  const setFilters = (key: string, val?: any) => {
    setFilterMap({ ...filterMap, [key]: val });
  };
  // const [labelFilterVis, setLabelFilterVis] = React.useState(false);
  // const labelSelectRef = React.useRef(null);

  const [stateMap, updater, update] = useUpdate({
    recordData: null,
  });

  React.useEffect(() => {
    const reeData = filter(dataSource, (record) => {
      let res = true;
      map(filterMap, (filterVal, key) => {
        let filterFun = null;
        if (key === 'labels') {
          filterFun = filterFunc.includeMult;
        } else if (key === 'ip') {
          filterFun = filterFunc.include;
        } else {
          filterFun = filterFunc.numberRange;
        }
        if (res) res = filterFun && filterFun(record, filterVal, key);
      });
      return res;
    });
    setFilterList(reeData);
  }, [filterMap, dataSource]);

  const handleSearch = (selectedKeys: string[], filterConfirm: Function, dataKey: string) => {
    filterConfirm();
    setFilters(dataKey, selectedKeys);
  };
  const handleReset = (clearFilters: Function, dataKey: string) => {
    clearFilters();
    setFilters(dataKey);
  };

  const closeMachineOffLine = () => {
    setCurMachine(null);
    setmachineOffLineVis(false);
  };
  const machineOffLine = (postData: ORG_MACHINE.IDeleteMachineBody) => {
    deleteMachine(postData).then((res) => {
      const { recordID } = res;
      setCurRecordID(recordID);
    });
    closeMachineOffLine();
  };

  const getInputFilter = (key: string, conf: object) => {
    return {
      filterDropdown: (_props: any) => (
        <FilterDropdownInput
          {..._props}
          {...conf}
          handleSearch={handleSearch}
          handleReset={handleReset}
          dataKey={key}
        />
      ),
      filterIcon: (filtered: boolean) => {
        return <CustomIcon type="search" className={`filter-search-icon ${filtered ? 'active' : 'normal'}`} />;
      },
    };
  };

  const getNumberFilter = (key: string) => {
    return {
      filterDropdown: (_props: any) => (
        <FilterDropdownNumber {..._props} handleSearch={handleSearch} handleReset={handleReset} dataKey={key} />
      ),
      filterIcon: (filtered: boolean) => {
        return <CustomIcon type="search" className={`filter-search-icon ${filtered ? 'active' : 'normal'}`} />;
      },
    };
  };

  const getNumberTitle = (key: string, defaultTitle: string) => {
    const [more, less] = (get(filterMap, `${key}`) || [[]])[0];
    const moreTip = more ? `${i18n.t('greater than')}${more}%` : '';
    const lessTip = less ? `${i18n.t('less than')}${less}%` : '';
    if (moreTip || lessTip) {
      return (
        <div className="title-with-search">
          <span className="main-title">{defaultTitle} </span>
          <br />
          <IF check={!!moreTip}>
            <span className="search-value">{`${moreTip}`}</span> <br />
          </IF>
          <IF check={!!lessTip}>
            <span className="search-value">{`${lessTip}`}</span>
          </IF>
        </div>
      );
    }
    return defaultTitle;
  };

  const columns: Array<ColumnProps<ORG_MACHINE.IMachine>> = [
    {
      title: <FirstColTitle filterMap={filterMap} />,
      width: 160,
      fixed: 'left',
      dataIndex: 'ip',
      // ...getInputFilter('ip', { placeholder: '根据IP搜索' }),
      sorter: (a: ORG_MACHINE.IMachine, b: ORG_MACHINE.IMachine) =>
        Number(a.ip.replace(/\./g, '')) - Number(b.ip.replace(/\./g, '')),
      render: (value: string, record: ORG_MACHINE.IMachine) => {
        const { status, abnormalMsg } = record;
        return (
          <div>
            <span className="status-pointer">
              {status === 'normal' ? null : (
                <HealthPoint
                  type="machine"
                  status={status === 'normal' ? 'normal' : 'warning'}
                  msg={abnormalMsg || i18n.t('unknown state')}
                />
              )}
            </span>
            <span className="font-bold hover-table-text status nowrap" onClick={() => gotoMachineMonitor(record)}>
              {value}
            </span>
          </div>
        );
      },
    },
    {
      title: i18n.t('number of instance'),
      dataIndex: 'tasks',
      width: 176,
      sorter: (a: ORG_MACHINE.IMachine, b: ORG_MACHINE.IMachine) => Number(a.tasks) - Number(b.tasks),
      render: (_val: string, record: ORG_MACHINE.IMachine) => {
        return (
          <span onClick={() => gotoMachineTasks(record)} className="font-bold hover-table-text">
            {record.tasks}
          </span>
          // record.tasks
        );
      },
    },
    {
      title: 'CPU',
      width: 120,
      dataIndex: 'cpuAllocatable',
      // sorter: (a: ORG_MACHINE.IMachine, b: ORG_MACHINE.IMachine) => Number(a.cpuUsage / a.cpuTotal) - Number(b.cpuUsage / b.cpuTotal),
      render: (_, data: ORG_MACHINE.IMachine) => {
        const { cpuAllocatable, cpuUsage, cpuRequest, cpuUsagePercent, cpuDispPercent } = data;

        return (
          <div className="percent-row">
            {DoubleProgressItem({
              usedPercent: Math.ceil(cpuUsagePercent),
              requestPercent: Math.ceil(cpuDispPercent),
              usage: cpuUsage,
              request: cpuRequest,
              total: cpuAllocatable,
              unit: i18n.t('core'),
            })}
          </div>
        );
      },
    },
    {
      title: i18n.t('memory'),
      width: 120,
      dataIndex: 'memProportion',
      // sorter: (a: ORG_MACHINE.IMachine, b: ORG_MACHINE.IMachine) => Number(a.memUsage / a.memTotal) - Number(b.memUsage / b.memTotal),
      render: (_, data: ORG_MACHINE.IMachine) => {
        const { memAllocatable, memUsage, memRequest, memUsagePercent, memDispPercent } = data;
        return (
          <div className="percent-row">
            {DoubleProgressItem({
              usedPercent: Math.ceil(memUsagePercent),
              requestPercent: Math.ceil(memDispPercent),
              usage: memUsage,
              request: memRequest,
              total: memAllocatable,
              unitType: 'STORAGE',
            })}
          </div>
        );
      },
    },
    {
      title: i18n.t('cmp:Disk usage'),
      width: 120,
      dataIndex: 'diskProportion',
      sorter: (a: ORG_MACHINE.IMachine, b: ORG_MACHINE.IMachine) =>
        Number(a.diskUsage / a.diskTotal) - Number(b.diskUsage / b.diskTotal),
      render: (value: string, data: ORG_MACHINE.IMachine) => {
        const { diskTotal, diskUsage } = data;
        const { percent } = value as any;
        return (
          <div className="percent-row">
            {ProgressItem({
              percent,
              used: diskUsage,
              total: diskTotal,
              unitType: 'STORAGE',
            })}
          </div>
        );
      },
    },
    {
      title: i18n.t('load'),
      dataIndex: 'load5',
      width: 96,
      sorter: (a: ORG_MACHINE.IMachine, b: ORG_MACHINE.IMachine) => Number(a.load5) - Number(b.load5),
    },
    {
      title: <span className="main-title">{i18n.t('tags')} </span>,
      dataIndex: 'labels',
      className: 'machine-labels',
      render: (labels: string) => {
        return <TagsRow labels={labels.split(',').map((l) => ({ label: l, color: customTagColor[l] }))} />;
      },
    },
    {
      title: i18n.t('operations'),
      dataIndex: 'id',
      key: 'operation',
      fixed: 'right',
      width: 160,
      render: (_id: string, record: ORG_MACHINE.IMachine) => {
        return (
          <TableActions>
            <span className="table-operations-btn" onClick={() => update({ recordData: record })}>
              {i18n.t('set tags')}
            </span>
            {/* <Operation record={record} /> */}
            <span
              onClick={() => {
                setCurMachine(record);
                setmachineOffLineVis(true);
              }}
            >
              {i18n.t('cmp:offline')}
            </span>
          </TableActions>
        );
      },
    },
  ];

  return (
    <div className="machine-table">
      <Table
        className="machine-list-table"
        loading={isFetching}
        rowKey={(record, index) => `${record.ip}-${index}`}
        pagination={false}
        bordered
        columns={columns}
        scroll={{ x: 1500 }}
        dataSource={filterList}
      />
      <MachineTagForm
        machine={stateMap.recordData as ORG_MACHINE.IMachine}
        visible={!!stateMap.recordData}
        onCancel={() => updater.recordData(null)}
      />
      <MachineOffLineModal
        visible={machineOffLineVis}
        onCancel={closeMachineOffLine}
        formData={curMachine}
        onSubmit={machineOffLine}
      />
      <ClusterLog recordID={curRecordID} onClose={() => setCurRecordID('')} />
    </div>
  );
};

export default React.memo(MachineTable, (prev, next) => prev.list === next.list && prev.isFetching === next.isFetching);
