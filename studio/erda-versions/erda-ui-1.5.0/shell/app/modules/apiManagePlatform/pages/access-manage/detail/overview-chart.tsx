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
import CommonChart from 'apiManagePlatform/components/chart';
import { isEqual } from 'lodash';

interface IProps {
  queries: {
    endpoint: string;
    workspace: string;
    projectID: number;
  };
}

const OverviewChart = ({ queries }: IProps) => {
  return (
    <div className="overview-chart mb-3 border-all bg-white p-3">
      <div className="title text-base font-medium mb-3">{i18n.t('traffic overview')}</div>
      <CommonChart type="apim_summary" extraQuery={queries} />
    </div>
  );
};

export default React.memo(
  (props: IProps) => <OverviewChart {...props} />,
  (prevProps, nextProps) => {
    return isEqual(prevProps, nextProps);
  },
);
