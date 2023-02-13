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
import { ORIGINAL_TYPE_MAP } from '../../constants';
import {
  BoardState,
  Dashboard,
  DataChart,
} from '../../pages/Board/slice/types';
import { Widget } from '../../types/widgetTypes';
const emptyWidgets = [];
export const getChartWidgets = (
  state: { board: BoardState },
  boardId: string,
) => {
  const widgetMap = state.board.widgetRecord;

  if (!widgetMap[boardId]) {
    return emptyWidgets;
  }
  let chartWidgets = Object.values(widgetMap[boardId]).filter(
    widget => widget.config.type === 'chart',
  );
  return chartWidgets;
};

export const getBoardTplData = (
  dataMap: Record<string, { id: string; name: string; data }>,
  boardTplData: {
    board: Dashboard;
    widgetMap: Record<string, Widget>;
    dataChartMap: Record<string, DataChart>;
  },
) => {
  const { board, widgetMap, dataChartMap } = boardTplData;
  const dashboard = {
    ...board,
    queryVariables: [],
    config: JSON.stringify(board.config) as any,
  } as Partial<Dashboard>;

  const widgets = Object.values(widgetMap).map(w => {
    const newWidgetConf = {
      ...w.config,
    };
    if (newWidgetConf.type === 'chart') {
      newWidgetConf.originalType = ORIGINAL_TYPE_MAP.ownedChart;
      const datachart = dataChartMap[w.datachartId || ''];

      if (datachart) {
        let newChart = {
          ...datachart,
          config: { ...datachart.config, sampleData: dataMap[w.id].data },
        };
        newChart.viewId = '';
        newChart.orgId = '';
        newWidgetConf.content = {
          dataChart: newChart,
        };
      }
    }
    const newWidget = {
      ...w,
      viewIds: [],
      datachartId: '',
      config: JSON.stringify(newWidgetConf) as any,
    };
    return newWidget;
  });
  return {
    dashboard,
    widgets,
  };
};
