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

import ChartEditor from 'app/components/ChartEditor';
import {
  DataChart,
  WidgetContentChartType,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { selectVizs } from 'app/pages/MainPage/pages/VizPage/slice/selectors';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { BOARD_SELF_CHART_PREFIX } from 'globalConstants';
import { useCallback, useContext, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { uuidv4 } from 'utils/utils';
import { addDataChartWidgets, addWrapChartWidget } from '../../../slice/thunk';
import ChartSelectModalModal from '../../ChartSelectModal';
import { BoardToolBarContext } from '../context/BoardToolBarContext';
import { ChartWidgetDropdown } from './ChartWidgetDropdown';

export const AddChart = () => {
  const dispatch = useDispatch();
  const { boardId, boardType } = useContext(BoardToolBarContext);
  const orgId = useSelector(selectOrgId);
  const chartOptionsMock = useSelector(selectVizs);
  const chartOptions = useMemo(
    () => chartOptionsMock.filter(item => item.relType !== 'DASHBOARD'),
    [chartOptionsMock],
  );

  const [dataChartVisible, setDataChartVisible] = useState<boolean>(false);
  const [widgetChartVisible, setWidgetChartVisible] = useState<boolean>(false);

  const onSelectedDataCharts = useCallback(
    (chartIds: string[]) => {
      dispatch(addDataChartWidgets({ boardId, chartIds, boardType }));
      setDataChartVisible(false);
    },
    [boardId, boardType, dispatch],
  );
  const onShowCharts = useCallback(() => {
    setDataChartVisible(true);
  }, []);
  const onCreateCharts = useCallback(() => {
    setWidgetChartVisible(true);
  }, []);
  const onCancelAddChart = useCallback(() => setWidgetChartVisible(false), []);
  const saveChartToWidget = useCallback(
    (chartType: WidgetContentChartType, dataChart: DataChart, view) => {
      dispatch(
        addWrapChartWidget({
          boardId,
          chartId: dataChart.id,
          boardType,
          dataChart,
          view,
        }),
      );
      setWidgetChartVisible(false);
    },
    [boardId, boardType, dispatch],
  );
  return (
    <>
      <ChartWidgetDropdown onSelect={onShowCharts} onCreate={onCreateCharts} />

      <ChartSelectModalModal
        dataCharts={chartOptions}
        visible={dataChartVisible}
        onSelectedCharts={onSelectedDataCharts}
        onCancel={() => setDataChartVisible(false)}
      />
      {widgetChartVisible && (
        <ChartEditor
          dataChartId={`${BOARD_SELF_CHART_PREFIX}${boardId}_${uuidv4()}`} // widget id issue #1890: generate uuid from frontend for own/link chart
          orgId={orgId}
          chartType="widgetChart"
          container="widget"
          onClose={onCancelAddChart}
          onSaveInWidget={saveChartToWidget}
        />
      )}
    </>
  );
};
