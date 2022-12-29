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

import { DataChart } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import ChartDataView from 'app/types/ChartDataView';
import { createContext, FC, memo, useContext, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { setWidgetSampleDataAction } from '../../actions/widgetAction';
import {
  selectAvailableSourceFunctionsMap,
  selectDataChartById,
  selectViewMap,
} from '../../pages/Board/slice/selector';
import { BoardState } from '../../pages/Board/slice/types';
import { WidgetContext } from './WidgetProvider';

// 支持作为 点击事件的 触发器的图表ID
export const SupportTriggerChartIds: string[] = [
  'cluster-column-chart',
  'cluster-bar-chart',
  'stack-column-chart',
  'stack-bar-chart',
  'percentage-stack-column-chart',
  'percentage-stack-bar-chart',
  'line-chart',
  'area-chart',
  'stack-area-chart',
  'scatter',
  'pie-chart',
  'doughnut-chart',
  'rose-chart',
  'funnel-chart',
  'double-y',
  'normal-outline-map-chart',
  'scatter-outline-map-chart',
  'fenzu-table',
  'mingxi-table',
];
export const WidgetChartContext = createContext<{
  dataChart: DataChart | undefined;
  chartDataView?: ChartDataView;
  availableSourceFunctions?: string[];
  supportTrigger: boolean;
}>({
  dataChart: {} as DataChart,
  availableSourceFunctions: undefined,
  supportTrigger: true,
});

export const WidgetChartProvider: FC<{
  boardEditing: boolean;
  widgetId: string;
}> = memo(({ boardEditing, widgetId, children }) => {
  const { datachartId } = useContext(WidgetContext);
  const dispatch = useDispatch();
  useEffect(() => {
    if (!datachartId) return;
    if (!widgetId) return;
    dispatch(
      setWidgetSampleDataAction({ boardEditing, datachartId, wid: widgetId }),
    );
  }, [boardEditing, datachartId, dispatch, widgetId]);
  const dataChart = useSelector((state: { board: BoardState }) =>
    selectDataChartById(state, datachartId),
  );
  const availableSourceFunctionsMap = useSelector(
    selectAvailableSourceFunctionsMap,
  );
  const viewMap = useSelector(selectViewMap);
  const availableSourceFunctions =
    availableSourceFunctionsMap[viewMap[dataChart?.viewId]?.sourceId];
  const supportTrigger = SupportTriggerChartIds.includes(
    dataChart?.config?.chartGraphId,
  );
  const chartDataView = viewMap[dataChart?.viewId];

  return (
    <WidgetChartContext.Provider
      value={{
        dataChart,
        availableSourceFunctions,
        supportTrigger,
        chartDataView,
      }}
    >
      {children}
    </WidgetChartContext.Provider>
  );
});
