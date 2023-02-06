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
import { isEmpty } from 'lodash';
import { EmptyHolder } from 'common';
import { OpenAPI } from 'openapi-types';
import APIPreviewV2 from './api-preview-2.0';
import APIPreviewV3 from './api-preview-3.0';
import './api-detail.scss';

interface IProps {
  specProtocol: API_MARKET.SpecProtocol;
  dataSource: Merge<OpenAPI.Operation, { _method: string; _path: string }>;
  extra?: React.ReactNode;
}

const ApiDetail = ({ dataSource, extra, specProtocol }: IProps) => {
  if (isEmpty(dataSource)) {
    return (
      <div className="mt-8">
        <EmptyHolder relative style={{ justifyContent: 'start' }} />
      </div>
    );
  }

  if (specProtocol && specProtocol.includes('oas3')) {
    // 3.0版本
    return <APIPreviewV3 dataSource={dataSource} extra={extra} />;
  } else {
    return <APIPreviewV2 dataSource={dataSource} extra={extra} />;
  }
};

export default ApiDetail;
