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
import { throttle } from 'lodash';
import { connectCube } from 'common/utils';
import { ErdaIcon } from 'common';
import './deploy-cluster-log.scss';
import clusterStore from 'cmp/stores/cluster';
import { useLoading } from 'core/stores/loading';

interface IProps {
  deployClusterLog: string;
  fetchingDeployClusterLog: boolean;
  getDeployClusterLog: () => Promise<any>;
  clearDeployClusterLog: () => Promise<any>;
}
interface IState {
  fetching: boolean;
}

class DeployClusterLog extends React.Component<IProps, IState> {
  state = {
    fetching: true,
  };

  rolling = true;

  throttleScroll = throttle(() => this.onScroll(), 100);

  fetchTime = 5000;

  private preElm: any;

  private logTimeout: number | undefined;

  componentDidMount() {
    this.fetchLog();
  }

  componentWillUnmount() {
    this.props.clearDeployClusterLog();
    this.cancelRolling();
  }

  fetchLog = () => {
    this.setState({
      fetching: true,
    });

    this.props.getDeployClusterLog().then(() => {
      this.scrollToBottom();
      this.setState({
        fetching: false,
      });
    });
    this.logTimeout = setTimeout(this.fetchLog, this.fetchTime) as any;
  };

  scrollToBottom = () => {
    // safari下设置过大的数值无效，所以给一个理论上足够大的值
    if (this.preElm) this.preElm.scrollTop = 999999999;
  };

  onScroll() {
    const distanceToBottom = this.preElm.scrollHeight - this.preElm.scrollTop - this.preElm.clientHeight;
    if (distanceToBottom > 10) {
      this.cancelRolling();
    } else if (distanceToBottom === 0) {
      if (!this.rolling) {
        this.rolling = true;
        this.fetchLog();
      }
    }
  }

  cancelRolling = () => {
    if (this.logTimeout) {
      this.rolling = false;
      clearTimeout(this.logTimeout);
      this.logTimeout = undefined;
    }
  };

  render() {
    const { fetching } = this.state;
    return (
      <div
        className="deploy-cluster-log"
        ref={(ref) => {
          this.preElm = ref;
        }}
        onScroll={this.throttleScroll}
      >
        <pre>{this.props.deployClusterLog}</pre>
        {fetching && <ErdaIcon type="loading" className="log-state bottom" spin />}
      </div>
    );
  }
}

const mapper = () => {
  const deployClusterLog = clusterStore.useStore((s) => s.deployClusterLog);
  const { getDeployClusterLog } = clusterStore.effects;
  const { clearDeployClusterLog } = clusterStore.reducers;
  const [fetchingDeployClusterLog] = useLoading(clusterStore, ['getDeployClusterLog']);
  return {
    deployClusterLog,
    getDeployClusterLog,
    clearDeployClusterLog,
    fetchingDeployClusterLog,
  };
};
export default connectCube(DeployClusterLog, mapper);
