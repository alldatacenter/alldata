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

import { isEmpty } from 'lodash';
import React from 'react';
import { Terminal } from 'common';
import { getClusterDetail } from 'cmp/services/cluster';
import { getOrgFromPath } from 'common/utils';
import i18n from 'i18n';
import './terminal.scss';

interface IProps {
  clusterName: string;
  host: string;
  user: string;
  port: string;
  containerId?: string;
  instanceTerminal?: boolean;
}

const ServiceTerminal = (props: IProps) => {
  const { clusterName, instanceTerminal = false, host, user, port, containerId } = props;
  const [clusterDetail, setClusterDetail] = React.useState<ORG_CLUSTER.ICluster | null>(null);

  React.useEffect(() => {
    if (clusterName) {
      getClusterDetail({ clusterName }).then((res) => {
        setClusterDetail(res?.data);
      });
    }
  }, [clusterName]);

  if (isEmpty(clusterDetail)) {
    return (
      <div className="service-terminal">
        <span className="terminal-info">{i18n.t('cmp:getting cluster information')}...</span>
      </div>
    );
  }

  let initData = {};
  if (!instanceTerminal) {
    initData = {
      name: 'ssh',
      args: {
        host,
        port: Number.isNaN(+port) ? 22 : +port,
        user: user || 'root',
      },
    };
  } else {
    initData = {
      name: 'docker',
      args: {
        host,
        port: 2375,
        container: containerId,
      },
    };
    if (!initData.args.host) {
      return (
        <div className="service-terminal">
          <span className="terminal-info">{i18n.t('cmp:cannot find current container')}</span>
        </div>
      );
    }
    if (!initData.args.container) {
      return (
        <div className="service-terminal">
          <span className="terminal-info">{i18n.t('cmp:cannot find current host')}</span>
        </div>
      );
    }
  }

  const replaceProtocol = (value: string) => value.replace('http', 'ws');
  const _params = {
    url: `${replaceProtocol(window.location.protocol)}//${
      window.location.host
    }/api/${getOrgFromPath()}/terminal?clusterName=${clusterName}`,
    initData,
  };

  return (
    <div className="service-terminal">
      <Terminal params={_params} />
    </div>
  );
};

export default ServiceTerminal;
