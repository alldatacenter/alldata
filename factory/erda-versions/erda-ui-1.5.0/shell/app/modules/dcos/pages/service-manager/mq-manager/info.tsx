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
  const MQDetails = cloudServiceStore.useStore((s) => s.MQDetails);
  const [mqID, region] = routeInfoStore.useStore((s) => [s.params.mqID, s.query.region]);
  const [isFetching] = useLoading(cloudServiceStore, ['getMQDetails']);
  const { getMQDetails } = cloudServiceStore.effects;
  const { clearMQDetails } = cloudServiceStore.reducers;

  useEffectOnce(() => {
    const detailsQueryBody = {
      id: mqID,
      query: { region: region || '' },
    };
    getMQDetails(detailsQueryBody);
    return () => clearMQDetails();
  });

  return (
    <Spin spinning={isFetching}>
      <InfoBox details={MQDetails} pannelProps={{ isMultiColumn: false }} />
    </Spin>
  );
};

export default Info;
