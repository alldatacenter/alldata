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

import { Row, Col, Select, Table, Input } from 'antd';
import { Holder, CardContainer } from 'common';
import classNames from 'classnames';
import { secondsToTime } from 'common/utils';
import { map, filter, uniqueId, find, debounce, get, floor } from 'lodash';
import React from 'react';
import TestPieChart from './test-pie-chart';
import i18n from 'i18n';
import './test-detail.scss';

interface IProps {
  suite: ISuite;
}
interface IState {
  checkedCaseKey: string;
  isShowing: boolean;
  filterKey: string;
  searchKey: string;
  suite: ISuite;
  preProps: IProps;
  filterList: ITest[];
}

interface ISuite {
  [proName: string]: any;
  tests?: ITest[];
}

interface ITest {
  [proName: string]: any;
  name: string;
  key?: string;
}

const { Option } = Select;
const { Search } = Input;
const { ChartContainer } = CardContainer;

const convertTestCases = (tests: ITest[]) => map(tests, (item) => ({ ...item, key: uniqueId() }));

class TestDetail extends React.Component<IProps, IState> {
  onSearchKeyChange = debounce(
    (value: string) =>
      this.setState({ searchKey: value }, () => {
        this.changeFilterList();
      }),
    300,
  );

  constructor(props: IProps) {
    super(props);
    const { suite } = props;
    const stateSuite = { ...suite };
    stateSuite.tests = convertTestCases(suite.tests || []);
    this.state = {
      checkedCaseKey: get(stateSuite.tests, '[0].key') || '',
      isShowing: true,
      filterKey: 'all',
      searchKey: '',
      suite: stateSuite,
      preProps: props,
      filterList: stateSuite.tests,
    };
  }

  static getDerivedStateFromProps(nextProps: IProps, prevState: IState) {
    const { suite } = nextProps;
    if (suite !== prevState.preProps.suite) {
      suite.tests = convertTestCases(suite.tests || []);
      return { suite, checkedCaseKey: get(suite.tests, '[0].key') || '', filterList: suite.tests };
    }
    return null;
  }

  showTestInfo = (key: string | undefined, e?: React.MouseEvent<Element, MouseEvent> | undefined) => {
    if (e) e.stopPropagation();
    const { checkedCaseKey } = this.state;
    if (key && key !== checkedCaseKey) {
      this.setState({
        checkedCaseKey: key,
      });
    }
  };

  toggleErrorInfo = () => {
    // this.setState({ isShowing: !this.state.isShowing });
  };

  renderTestInfo = (test?: ITest | null) => {
    const { isShowing } = this.state;
    if (!test) return <div className="test-output" />;
    const { error, stdout } = test;
    const { message = '', body = '', type = '' } = error || {};
    const errorClass = classNames({ block: isShowing });
    const hasLog = stdout || message || body || type;

    return (
      <div className="test-output">
        <Holder when={!hasLog}>
          <pre>
            {stdout}
            <p>
              {type || body ? (
                <span className="error-type" onClick={() => this.toggleErrorInfo()}>
                  {type || 'error'}
                </span>
              ) : null}
            </p>
            <p className={`error-message ${errorClass}`}>{message ? `Error: ${message}` : ''}</p>
            <p className={`error-body ${errorClass}`}>{`${body}`}</p>
          </pre>
        </Holder>
      </div>
    );
  };

  changeFilterKey = (filterKey: string) => {
    this.setState({ filterKey }, () => {
      this.changeFilterList();
    });
  };

  changeFilterList = () => {
    const { suite, filterKey, searchKey, checkedCaseKey } = this.state;
    const { tests } = suite;
    const filterList = filter(tests, (item) => {
      const { status, name } = item;
      return name.toLowerCase().includes(searchKey.toLowerCase()) && (filterKey === 'all' || status === filterKey);
    });
    const checkedTestCase = find(filterList, { key: checkedCaseKey });
    const forUpdate: any = { filterList };
    if (!checkedTestCase) {
      // 当前选中的没匹配数据
      forUpdate.checkedCaseKey = get(filterList, '[0].key') || '';
    }
    this.setState({ ...forUpdate });
  };

  render() {
    const { checkedCaseKey, filterKey, suite, filterList } = this.state;
    const { totals, extra } = suite;
    const statuses = totals ? totals.statuses : {};
    const checkedTestCase = find(filterList, { key: checkedCaseKey }) || null;

    const result = map(statuses, (v, k) => ({ name: k, type: k, value: v }));
    const pieChartData = {
      loading: false,
      result,
    };
    const dataSource = map(extra, (v, k) => ({ env: k, value: v }));
    const cols = [
      {
        title: '',
        dataIndex: 'env',
      },
      {
        title: '',
        dataIndex: 'value',
        className: 'env-value',
      },
    ];
    const statusFilter = [
      { name: i18n.t('dop:all'), value: 'all', color: 'all' },
      { name: i18n.t('dop:pass'), value: 'passed', color: 'passed' },
      { name: i18n.t('dop:jump over'), value: 'skipped', color: 'skipped' },
      { name: i18n.t('failed'), value: 'failed', color: 'failed' },
      { name: i18n.t('error'), value: 'error', color: 'error' },
    ];
    return (
      <div className="application-test-detail">
        <div className="row-space" />
        <Row>
          <Col span={12} className="test-chart-container">
            <ChartContainer title={i18n.t('dop:use case statistics')}>
              <TestPieChart data={pieChartData} />
            </ChartContainer>
          </Col>
          <Col span={12} className="test-env-container">
            <ChartContainer title={i18n.t('test environment')}>
              <Table
                loading={false}
                dataSource={dataSource}
                columns={cols}
                rowKey="env"
                showHeader={false}
                pagination={false}
                scroll={{ x: '100%' }}
              />
            </ChartContainer>
          </Col>
        </Row>
        <div className="row-space" />
        <Row className="test-detail-row">
          <Col span={8} className="test-list-container">
            <ChartContainer title={i18n.t('dop:test case')}>
              <div className="filter">
                <Search
                  placeholder={i18n.t('dop:enter to filter use cases')}
                  onChange={(e) => this.onSearchKeyChange(e.target.value)}
                />
                <Select value={filterKey} onChange={this.changeFilterKey}>
                  {map(statusFilter, (item) => {
                    return (
                      <Option key={item.name} value={item.value}>
                        <span className="point-wrap">{item.name}</span>
                      </Option>
                    );
                  })}
                </Select>
              </div>
              <ul className="unit-test-list">
                {map(filterList, (item) => {
                  const { name, status, duration, key } = item;
                  const seconds = floor(parseInt(duration, 10) / 10 ** 9, 2); // duration为纳秒
                  return (
                    <li
                      className={`test-item ${key === checkedCaseKey ? 'active' : ''} ${status}`}
                      key={key}
                      onClick={(e) => this.showTestInfo(key, e)}
                    >
                      <span className="name">
                        <span className={`status-point ${status}`} />
                        {name}
                      </span>
                      <span className="time">
                        {duration !== 0 && seconds === 0
                          ? `${duration / 1000000}${i18n.t('dop:millisecond(s)')}`
                          : secondsToTime(seconds, true)}
                      </span>
                    </li>
                  );
                })}
                {filterList.length === 0 && <li className="test-item-nodata">{i18n.t('dop:no matching data')}</li>}
              </ul>
            </ChartContainer>
          </Col>
          <Col span={16} className="test-output-container">
            <ChartContainer title={i18n.t('dop:test output')}>
              {this.renderTestInfo(checkedTestCase as ITest)}
            </ChartContainer>
          </Col>
        </Row>
      </div>
    );
  }
}
export default TestDetail;
