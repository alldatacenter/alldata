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

import { Select, Spin } from 'antd';
import { Holder, IF } from 'common';
import { connectCube } from 'common/utils';
import { map, isEmpty, forEach } from 'lodash';
import React from 'react';
import TestDetail from './test-detail';
import { BuildLog } from 'application/pages/build-detail/build-log';
import i18n from 'i18n';
import './test-detail.scss';
import applicationTestStore from 'application/stores/test';
import { useLoading } from 'core/stores/loading';

interface IProps {
  testDetail: ITestDetail;
  isFetching: boolean;
  getTestDetail: () => Promise<any>;
}
interface IState {
  chosenSuiteIndex: number;
  suites: ISuite[];
  preProps: IProps;
  logVisible: boolean;
}
interface ITestDetail {
  [k: string]: any;
  suites: ISuite[];
}

interface ISuite {
  [k: string]: any;
  tests?: ITest[];
}

interface ITest {
  [k: string]: any;
  name: string;
  key?: string;
}

const { Option } = Select;

const getValidSuites = (suites: ISuite[]): ISuite[] => {
  const suitesList: ISuite[] = [];
  forEach(suites, (item) => {
    if (!isEmpty(item)) {
      suitesList.push(item);
    }
  });
  return suitesList;
};

class TestDetailContainer extends React.Component<IProps, IState> {
  constructor(props: IProps) {
    super(props);
    const { testDetail } = props;
    this.state = {
      chosenSuiteIndex: 0,
      suites: getValidSuites(testDetail.suites || []),
      logVisible: false,
      preProps: props,
    };
  }

  static getDerivedStateFromProps(nextProps: IProps, prevState: IState) {
    const { testDetail } = nextProps;
    if (testDetail !== prevState.preProps.testDetail) {
      return { suites: getValidSuites(testDetail.suites || []) };
    }
    return null;
  }

  componentDidMount(): void {
    this.props.getTestDetail();
  }

  changeDetail = (chosenSuiteIndex: number) => {
    this.setState({ chosenSuiteIndex });
  };

  toggleLog = () => {
    this.setState({
      logVisible: !this.state.logVisible,
    });
  };

  render() {
    const { suites, chosenSuiteIndex, logVisible } = this.state;
    const detailOptions = map(suites, (suite, index) => ({ value: index, text: suite.name || index }));
    const currentSuite = suites[chosenSuiteIndex] || null;
    const { testDetail, isFetching } = this.props;
    return (
      <Spin spinning={isFetching}>
        <div className="test-detail-header flex justify-between items-center">
          <IF check={!isEmpty(detailOptions)}>
            <Select
              onChange={(v) => this.changeDetail(+v)}
              dropdownMatchSelectWidth={false}
              defaultValue={chosenSuiteIndex}
              className="test-selector"
            >
              {map(detailOptions, (opt) => (
                <Option value={opt.value} key={`${opt.value}`}>
                  {opt.text}
                </Option>
              ))}
            </Select>
          </IF>

          <IF check={testDetail && testDetail.uuid}>
            <span className="test-log hover-active" onClick={this.toggleLog}>
              {i18n.t('log')}
            </span>
          </IF>
        </div>
        <Holder
          when={!currentSuite}
          page
          relative
          tip={`${i18n.t('dop:no data, please confirm if there is a corresponding test code')}(UT/IT)`}
        >
          <TestDetail key={chosenSuiteIndex} suite={currentSuite} />
        </Holder>
        <BuildLog visible={logVisible} hideLog={this.toggleLog} logId={testDetail.uuid} />
      </Spin>
    );
  }
}

const Mapper = () => {
  const testDetail = applicationTestStore.useStore((s) => s.testDetail);
  const [isFetching] = useLoading(applicationTestStore, ['getTestDetail']);
  const { getTestDetail } = applicationTestStore.effects;
  return {
    testDetail,
    isFetching,
    getTestDetail,
  };
};
export default connectCube(TestDetailContainer, Mapper);
