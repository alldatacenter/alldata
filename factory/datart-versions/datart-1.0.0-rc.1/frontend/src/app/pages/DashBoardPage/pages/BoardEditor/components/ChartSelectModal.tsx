/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Input, Modal } from 'antd';
import { Tree } from 'app/components';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useGetVizIcon from 'app/hooks/useGetVizIcon';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { selectWidgetInfoDatachartId } from 'app/pages/DashBoardPage/pages/BoardEditor/slice/selectors';
import { Folder } from 'app/pages/MainPage/pages/VizPage/slice/types';
import React, { useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { listToTree } from 'utils/utils';

export interface IProps {
  // dataCharts: DataChart[];
  dataCharts: Folder[];
  visible: boolean;
  onSelectedCharts: (selectedIds: string[]) => void;
  onCancel: () => void;
}

const ChartSelectModalModal: React.FC<IProps> = props => {
  const t = useI18NPrefix(`viz.board.action`);
  const { visible, onSelectedCharts, onCancel, dataCharts } = props;
  const [selectedDataChartIds, setSelectedDataChartIds] = useState<string[]>(
    [],
  );
  const [selectedDataChartRelIds, setSelectedDataChartRelIds] = useState<
    string[]
  >([]);
  const WidgetInfoDatachartIds = useSelector(selectWidgetInfoDatachartId); //zh dashboard中已存在图表的datachartId en: The datachartId of the existing chart in the dashboard
  const getIcon = useGetVizIcon();

  const treeData = useMemo(
    () =>
      listToTree(
        dataCharts.map(v => ({
          ...v,
          isFolder: v.relType === 'FOLDER',
          disabled: WidgetInfoDatachartIds.includes(v.relId),
        })),
        null,
        [],
        { getIcon },
      ),
    [WidgetInfoDatachartIds, dataCharts, getIcon],
  );

  const { filteredData: filteredTreeData, debouncedSearch: treeSearch } =
    useDebouncedSearch(treeData, (keywords, d) => {
      return d.name.toLowerCase().includes(keywords.toLowerCase());
    });

  const onOk = () => {
    onSelectedCharts(selectedDataChartRelIds);
  };

  const onSelectChart = (SelectKeys, nodeData) => {
    let Ids: string[] = [],
      RelIds: string[] = [];

    nodeData.checkedNodes
      .filter(
        node => !node.isFolder && !WidgetInfoDatachartIds.includes(node.relId),
      )
      .forEach(val => {
        RelIds.push(val.relId);
        Ids.push(val.id);
      }); //zh 去除文件类型和已有图表的数据 en: Remove file types and data from existing charts

    setSelectedDataChartIds(Ids);
    setSelectedDataChartRelIds(RelIds);
  };

  const setDefaultChartsIds = () => {
    let ChartsIds: any = [];
    treeData?.forEach(treeNode => {
      let checkedLength = 0;

      if (treeNode.disabled) {
        //zh  dashboard中已经含有该图表 en:The chart is already in the dashboard
        ChartsIds.push(treeNode.id);
      }

      treeNode?.children?.forEach(v => {
        if (v.disabled) {
          //zh dashboard中已经含有该图表 en:The chart is already in the dashboard
          checkedLength = checkedLength + 1;
          ChartsIds.push(v.id);
        }
      });

      if (checkedLength === treeNode?.children?.length) {
        // zh:如果该目录里的图表都被选中，那么目录也要被选中并且不可点击 en: If the charts in the catalog are all selected, then the catalog must also be selected and not clickable
        treeNode.disabled = true;
        ChartsIds.push(treeNode.id);
      }
    });
    return ChartsIds;
  };

  let defaultChartsIds = useMemo(setDefaultChartsIds, [treeData]);

  useEffect(() => {
    if (!visible) {
      setSelectedDataChartIds([]);
    }
  }, [visible]);

  return (
    <Modal
      title={t('importExistingDataCharts')}
      visible={visible}
      onOk={onOk}
      centered
      onCancel={onCancel}
      okButtonProps={{ disabled: !selectedDataChartIds.length }}
      cancelButtonProps={{ disabled: false }}
    >
      <InputWrap>
        <Input onChange={treeSearch} placeholder={t('searchValue')} />
      </InputWrap>
      <Tree
        loading={false}
        showIcon
        checkable
        defaultExpandAll={true}
        onCheck={onSelectChart}
        treeData={filteredTreeData}
        checkedKeys={[...defaultChartsIds, ...selectedDataChartIds]}
        height={300}
      />
    </Modal>
  );
};

export default ChartSelectModalModal;

const InputWrap = styled.div`
  padding: 0 20px;
  margin-bottom: 10px;
`;
