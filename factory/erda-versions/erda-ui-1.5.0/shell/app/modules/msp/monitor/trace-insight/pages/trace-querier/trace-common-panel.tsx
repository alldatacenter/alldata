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
import PropTypes from 'prop-types';
import { Card } from 'antd';
import { isString } from 'lodash';
import './trace-common-panel.scss';

interface IProps {
  title?: any;
  children?: any;
  className?: string;
}
const TraceCommonPanel = (props: IProps) => {
  const { title, children, className, ...otherProps } = props;

  return (
    <Card className={`${className} trace-common-panel`} bordered={false} {...otherProps}>
      <div className="mb-2">
        {isString(title) ? <h3 className="trace-common-panel-title font-medium">{title}</h3> : title || null}
      </div>
      <div className="card-body">{children}</div>
    </Card>
  );
};

TraceCommonPanel.propTypes = {
  title: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  className: PropTypes.string,
  children: PropTypes.element,
};

export default TraceCommonPanel;
