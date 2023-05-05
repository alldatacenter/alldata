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

import { isEmpty, get, mapKeys } from 'lodash';

interface IProps {
  [pro: string]: any;
  chosenAppGroup: any;
  appGroup: {
    [pro: string]: any;
    loading?: boolean;
  };
  chosenApp: any;
}

export const getFilterParams = (props: IProps, { type, prefix }: { type: string; prefix: string }) => {
  const { chosenAppGroup, appGroup } = props;

  const appGroupLoading = appGroup[type] && appGroup[type].loading === false;
  const curAppGroup = chosenAppGroup[type];
  let runtime_name;
  let service_name;
  if (!isEmpty(curAppGroup)) {
    [runtime_name, service_name] = curAppGroup;
  }

  const application_id = get(props, 'chosenApp.id');
  const filters = { application_id, runtime_name, service_name };
  const shouldLoad = application_id !== undefined && !!appGroupLoading;

  const filterQuery = mapKeys(filters, (_val, key) => `${prefix}${key}`);
  return { shouldLoad, filterQuery };
};
