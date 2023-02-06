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
import DiceFlowChart from 'application/common/components/dice-yaml-editor';
import { DiceFlowType } from 'application/common/components/dice-yaml-editor-type';
import { IDiceYamlEditorItem } from 'application/common/components/dice-yaml-editor-item';
import { connectCube } from 'common/utils';
import { TableDrawer } from '../../common/components/table-drawer';

import './index.scss';
import dataTaskStore from 'application/stores/dataTask';
import { useLoading } from 'core/stores/loading';

const BORDER_COLOR = '#333';

interface IProps {
  isFetchingTable: boolean;
  tableAttrsList: any[];
  tableAttrsPaging: IPaging;
  getStarChartSource: () => Promise<any>;
  getTableAttrs: () => Promise<any>;
}

const convertStarsToFlowDataSource = (star: any) => {
  if (!star) return null;
  const dependsItems: IDiceYamlEditorItem[] = [];
  const centerItems: IDiceYamlEditorItem[] = [];
  // center 的 data 中存的是分组信息
  centerItems.push({
    id: 1111,
    data: star.relationGroup,
    title: star.table,
    name: star.cnName || '-',
    lineTo: star.dims ? star.dims.map((i: any) => i.table) : [],
  });

  if (star.dims) {
    star.dims.forEach((item: any, index: number) => {
      dependsItems.push({
        id: index,
        data: item,
        title: item.table,
        name: item.cnName || '-',
        lineTo: [],
      });
    });
  }

  return {
    centerItems,
    dependsItems,
  };
};

const ModelStarChart = (props: any) => {
  const { isFetchingTable, tableAttrsList, getTableAttrs, tableAttrsPaging, getStarChartSource } = props;
  const [source, setSource]: any = React.useState(undefined);
  const [starSource, setStarSource] = React.useState(undefined);
  const [drawerVisible, setDrawerVisible] = React.useState(false);
  const [selectedItem, setSelectedItem] = React.useState(undefined);
  const [centerRelatedGroup, setCenterRelatedGroup] = React.useState(undefined);

  React.useEffect(() => {
    getStarChartSource().then((data: any) => {
      const convertedStarSource = convertStarsToFlowDataSource(data);
      setSource(convertedStarSource);
      setStarSource(data);
    });
  }, [getStarChartSource]);

  const onClickNode = (item: any, type?: string) => {
    const isSelectedCenter = type === 'center';
    const param = isEmpty(item) || isSelectedCenter ? starSource : item;
    setCenterRelatedGroup(isSelectedCenter ? item : undefined);
    setSelectedItem(param);
    setDrawerVisible(true);
  };

  const closeDrawer = () => {
    setDrawerVisible(false);
  };

  return (
    <div className="flow-container">
      <DiceFlowChart
        lineStyle={{ borderColor: BORDER_COLOR }}
        type={DiceFlowType.DATA_MARKET}
        editing={false}
        dataSource={source}
        clickItem={onClickNode}
      />
      <TableDrawer
        selectedItem={selectedItem}
        centerRelatedGroup={centerRelatedGroup}
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
  const [tableAttrsList, tableAttrsPaging] = dataTaskStore.useStore((s) => [s.tableAttrsList, s.tableAttrsPaging]);
  const [isFetching, isFetchingTable] = useLoading(dataTaskStore, ['getStarChartSource', 'getTableAttrs']);
  const { getStarChartSource, getTableAttrs } = dataTaskStore.effects;

  return {
    tableAttrsList,
    tableAttrsPaging,
    isFetching,
    isFetchingTable,
    getStarChartSource,
    getTableAttrs,
  };
};

const ModelStarWrapper = connectCube(ModelStarChart, Mapper);
export { ModelStarWrapper as ModelStarChart };
