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
import SimpleLogRoller from './simple-log-roller';
import LogSearch from './simple-log-search';
import './simple-log.scss';

class SimpleLog extends React.Component {
  constructor(props) {
    super(props);

    const query = {};
    if (props.requestId) {
      query.requestId = props.requestId;
    }
    if (props.applicationId) {
      query.applicationId = props.applicationId;
    }
    this.state = {
      query,
    };
  }

  setSearch = (query) => {
    this.setState({
      query,
    });
  };

  render() {
    const { query } = this.state;
    return (
      <div className="log-insight">
        <LogSearch setSearch={this.setSearch} formData={query} />
        <SimpleLogRoller query={query} logKey="log-insight" />
      </div>
    );
  }
}

export default SimpleLog;
