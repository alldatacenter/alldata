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
import PureServiceList from '../components/service-list';
import projectResourceStore from 'project/stores/resource';
import { useLoading } from 'core/stores/loading';

interface IProps {
  paths: Array<{
    q: string;
    name: string;
  }>;
  startLevel: string;
  into: () => void;
}

const ServiceList = (props: IProps) => {
  const { getServiceList } = projectResourceStore.effects;
  const serviceList = projectResourceStore.useStore((s) => s.serviceList);
  const [isFetching] = useLoading(projectResourceStore, ['getServiceList']);
  React.useEffect(() => {
    getServiceList({ paths: props.paths, startLevel: props.startLevel });
  }, [getServiceList, props.paths, props.startLevel]);

  return <PureServiceList {...props} depth={props.paths.length} serviceList={serviceList} isFetching={isFetching} />;
};

export default React.memo(ServiceList);
