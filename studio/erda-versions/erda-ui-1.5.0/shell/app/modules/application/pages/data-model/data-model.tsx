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
import { goTo, connectCube } from 'common/utils';
import { DataList } from '../../common/components/data-list';
import i18n from 'i18n';
import dataTaskStore from 'application/stores/dataTask';
import { useLoading } from 'core/stores/loading';
import { useUnmount } from 'react-use';

interface IProps {
  getBusinessScope: any;
  getBusinessProcesses: any;
  isFetching: boolean;
  businessProcessList: IModel[];
  dataModelPaging: IPaging;
  modelBusinessScope: { businessDomain: any; dataDomains: any[]; marketDomains: any[] };
}

interface IModel {
  enName: string;
  cnName: string;
  desc?: string;
  table: string;
  file: string;
}

const pageConfig = {
  iconType: 'ywgc',
  domainName: 'dataDomain',
  domainPlaceholder: i18n.t('dop:select data domain'),
};

const DataModel = (props: IProps) => {
  const onItemClick = (item: IModel) => {
    goTo(`./starChart/${encodeURIComponent(item.file)}`);
  };

  const { getBusinessProcesses, isFetching, businessProcessList, dataModelPaging, modelBusinessScope } = props;

  return (
    <div className="data-model">
      <DataList
        compName="model"
        getList={getBusinessProcesses}
        isFetching={isFetching}
        list={businessProcessList}
        listPaging={dataModelPaging}
        pageConfig={pageConfig}
        onItemClick={onItemClick}
        businessScope={modelBusinessScope}
      />
    </div>
  );
};

const Mapper = () => {
  const [businessProcessList, modelBusinessScope, dataModelPaging] = dataTaskStore.useStore((s) => [
    s.businessProcessList,
    s.modelBusinessScope,
    s.businessProcessPaging,
  ]);
  const [isFetching] = useLoading(dataTaskStore, ['getBusinessProcesses']);
  const { getBusinessProcesses } = dataTaskStore.effects;
  const { clearBusinessProcesses } = dataTaskStore.reducers;
  useUnmount(() => {
    clearBusinessProcesses();
  });
  return {
    businessProcessList,
    modelBusinessScope,
    dataModelPaging,
    isFetching,
    getBusinessProcesses,
  };
};

const DataModelWrapper = connectCube(DataModel, Mapper);
export { DataModelWrapper as DataModel };
