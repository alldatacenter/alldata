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
import { connectCube } from 'common/utils';
import { DataList } from '../../common/components/data-list';
import { TableDrawer } from '../../common/components/table-drawer';
import i18n from 'i18n';
import { useUnmount } from 'react-use';
import dataTaskStore from 'application/stores/dataTask';
import { useLoading } from 'core/stores/loading';

interface IProps {
  getOutputTables: any;
  isFetching: boolean;
  outputTableList: any[];
  dataMarketPaging: IPaging;
  tableAttrsList: any[];
  getTableAttrs: any;
  isFetchingTable: boolean;
  tableAttrsPaging: IPaging;
  marketBusinessScope: { businessDomain: any; dataDomains: any[]; marketDomains: any[] };
}

const pageConfig = {
  iconType: 'bg',
  domainName: 'marketDomain',
  domainPlaceholder: i18n.t('dop:select a market area'),
};

const DataMarket = (props: IProps) => {
  const [drawerVisible, setDrawerVisible] = React.useState(false);
  const [selectedTable, setSelectedTable] = React.useState(undefined);

  const showDrawer = (selectedItem: any) => {
    setDrawerVisible(true);
    setSelectedTable(selectedItem || undefined);
  };

  const closeDrawer = () => {
    setDrawerVisible(false);
    setSelectedTable(undefined);
  };

  const {
    getOutputTables,
    isFetching,
    outputTableList,
    dataMarketPaging,
    getTableAttrs,
    tableAttrsList,
    isFetchingTable,
    tableAttrsPaging,
    marketBusinessScope,
  } = props;

  return (
    <div className="data-model">
      <DataList
        compName="market"
        getList={getOutputTables}
        isFetching={isFetching}
        list={outputTableList}
        listPaging={dataMarketPaging}
        pageConfig={pageConfig}
        onItemClick={showDrawer}
        businessScope={marketBusinessScope}
      />
      <TableDrawer
        selectedItem={selectedTable}
        drawerVisible={drawerVisible}
        getTableAttrs={getTableAttrs}
        tableAttrsList={tableAttrsList}
        isFetching={isFetchingTable}
        tableAttrsPaging={tableAttrsPaging}
        closeDrawer={closeDrawer}
      />
    </div>
  );
};

const Mapper = () => {
  const [outputTableList, tableAttrsList, marketBusinessScope, dataMarketPaging, tableAttrsPaging] =
    dataTaskStore.useStore((s) => [
      s.outputTableList,
      s.tableAttrsList,
      s.marketBusinessScope,
      s.outputTablePaging,
      s.tableAttrsPaging,
    ]);
  const [isFetching, isFetchingTable] = useLoading(dataTaskStore, ['getOutputTables', 'getTableAttrs']);
  const { getOutputTables, getTableAttrs } = dataTaskStore.effects;
  const { clearOutputTables } = dataTaskStore.reducers;
  useUnmount(() => {
    clearOutputTables();
  });
  return {
    outputTableList,
    tableAttrsList,
    tableAttrsPaging,
    dataMarketPaging,
    marketBusinessScope,
    isFetching,
    isFetchingTable,
    getOutputTables,
    getTableAttrs,
  };
};

const DataMarketWrapper = connectCube(DataMarket, Mapper);
export { DataMarketWrapper as DataMarket };
