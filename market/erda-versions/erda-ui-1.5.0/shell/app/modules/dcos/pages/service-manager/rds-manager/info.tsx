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
import { Spin } from 'antd';
import cloudServiceStore from 'dcos/stores/cloud-service';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import { useEffectOnce } from 'react-use';
import InfoBox from '../info-box';

const Info = () => {
  const RDSDetails = cloudServiceStore.useStore((s) => s.RDSDetails);
  const { getRDSDetails } = cloudServiceStore.effects;
  const { clearRDSDetails } = cloudServiceStore.reducers;

  const [rdsID, query] = routeInfoStore.useStore((s) => [s.params.rdsID, s.query as any]);
  const [isFetching] = useLoading(cloudServiceStore, ['getRDSDetails']);

  useEffectOnce(() => {
    const detailsQueryBody = {
      id: rdsID,
      query,
    };
    getRDSDetails(detailsQueryBody);
    return () => clearRDSDetails();
  });
  return (
    <Spin spinning={isFetching}>
      <InfoBox details={RDSDetails} pannelProps={{ isMultiColumn: false }} />
    </Spin>
  );
};

export default Info;
