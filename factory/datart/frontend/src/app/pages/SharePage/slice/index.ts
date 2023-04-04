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
import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ChartDataSectionType } from 'app/constants';
import { migrateChartConfig } from 'app/migration';
import ChartManager from 'app/models/ChartManager';
import {
  FilterSearchParams,
  VizType,
} from 'app/pages/MainPage/pages/VizPage/slice/types';
import { transferChartConfig } from 'app/pages/MainPage/pages/VizPage/slice/utils';
import { ChartConfig, SelectedItem } from 'app/types/ChartConfig';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import { ChartDTO } from 'app/types/ChartDTO';
import { mergeToChartConfig } from 'app/utils/ChartDtoHelper';
import { FilterSqlOperator } from 'globalConstants';
import { useInjectReducer } from 'utils/@reduxjs/injectReducer';
import { Omit } from 'utils/object';
import {
  fetchAvailableSourceFunctionsForShare,
  fetchShareDataSetByPreviewChartAction,
  fetchShareVizInfo,
  getOauth2Clients,
} from './thunks';
import { ExecuteToken, SharePageState, ShareVizInfo } from './types';

export const initialState: SharePageState = {
  needVerify: false,
  vizType: undefined,
  shareToken: '',
  executeToken: '',
  executeTokenMap: {},
  sharePassword: undefined,
  chartPreview: { isLoadingData: false },
  headlessBrowserRenderSign: false,
  pageWidthHeight: [0, 0],
  shareDownloadPolling: false,
  loginLoading: false,
  oauth2Clients: [],
  availableSourceFunctions: [],
  selectedItems: [],
};

export const slice = createSlice({
  name: 'share',
  initialState,
  reducers: {
    saveShareInfo: (state, action: PayloadAction<{ token; pwd }>) => {
      state.shareToken = action.payload.token;
      state.sharePassword = action.payload.pwd;
    },
    saveNeedVerify: (state, action: PayloadAction<boolean>) => {
      state.needVerify = action.payload;
    },
    setVizType: (state, action: PayloadAction<VizType | undefined>) => {
      state.vizType = action.payload;
    },
    setShareDownloadPolling: (state, action: PayloadAction<boolean>) => {
      state.shareDownloadPolling = action.payload;
    },
    setExecuteTokenMap: (
      state,
      action: PayloadAction<{
        executeToken: Record<string, ExecuteToken>;
      }>,
    ) => {
      const { executeToken } = action.payload;
      state.executeTokenMap = executeToken;
    },
    setSubVizTokenMap: (
      state,
      action: PayloadAction<{
        subVizToken: Record<string, ExecuteToken> | null;
      }>,
    ) => {
      const { subVizToken } = action.payload;
      state.subVizTokenMap = subVizToken || undefined;
    },
    setDataChart: (
      state,
      action: PayloadAction<{
        data: ShareVizInfo;
        filterSearchParams?: FilterSearchParams;
        isMatchByName?: boolean;
      }>,
    ) => {
      const { data, filterSearchParams, isMatchByName } = action.payload;
      const vizDetail = data.vizDetail as ChartDTO;
      const chartConfigDTO = vizDetail.config;
      const currentChart = ChartManager.instance().getById(
        chartConfigDTO?.chartGraphId,
      );
      let chartConfig = currentChart?.config as ChartConfig;
      const jumpFilters: PendingChartDataRequestFilter[] = Object.entries(
        Omit(filterSearchParams, ['type', 'isMatchByName']),
      ).map(entity => {
        return {
          column: entity[0],
          sqlOperator: FilterSqlOperator.In,
          values: entity[1]?.map(v => ({
            value: v,
            valueType: 'STRING',
          })),
        };
      });
      if (currentChart) {
        chartConfig = transferChartConfig(
          mergeToChartConfig(
            currentChart?.config,
            migrateChartConfig(chartConfigDTO),
          ),
          filterSearchParams,
          isMatchByName,
          jumpFilters,
        );
      }
      const executeToken = data.executeToken;
      const executeKey = vizDetail?.viewId;
      if (executeKey) {
        state.executeToken = executeToken?.[executeKey]?.authorizedToken;
      }
      state.chartPreview = {
        ...state.chartPreview,
        chartConfig: chartConfig,
        backendChart: vizDetail,
      };
    },
    updateChartPreviewFilter(
      state,
      action: PayloadAction<{ backendChartId: string; payload }>,
    ) {
      const chartPreview = state.chartPreview;
      if (chartPreview) {
        const filterSection = chartPreview?.chartConfig?.datas?.find(
          section => section.type === ChartDataSectionType.Filter,
        );
        if (filterSection) {
          const filterRowIndex = filterSection.rows?.findIndex(
            r => r?.uid === action.payload?.payload?.value?.uid,
          );

          if (filterRowIndex !== undefined && filterRowIndex > -1) {
            if (
              filterSection &&
              filterSection.rows &&
              filterSection.rows?.[filterRowIndex]
            ) {
              filterSection.rows[filterRowIndex] =
                action.payload?.payload?.value;
            }
          }
        }
      }
    },
    updateChartPreviewGroup(
      state,
      action: PayloadAction<{ backendChartId: string; payload }>,
    ) {
      if (state.chartPreview) {
        const groupSection = state.chartPreview?.chartConfig?.datas?.find(
          section => section.type === ChartDataSectionType.Group,
        );
        if (groupSection) {
          groupSection.rows = action.payload.payload?.value?.rows;
        }
      }
    },
    updateComputedFields(
      state,
      action: PayloadAction<{
        backendChartId: string;
        computedFields: any;
      }>,
    ) {
      if (state.chartPreview && state.chartPreview?.backendChart?.config) {
        state.chartPreview.backendChart.config.computedFields =
          action.payload.computedFields;
      }
    },
    changeSelectedItems(state, { payload }: PayloadAction<SelectedItem[]>) {
      state.selectedItems = payload;
    },
    savePageTitle: (state, action: PayloadAction<{ title: string }>) => {
      state.title = action.payload.title;
    },
  },
  extraReducers: builder => {
    builder
      .addCase(fetchShareVizInfo.pending, state => {
        state.loginLoading = true;
      })
      .addCase(fetchShareVizInfo.fulfilled, state => {
        state.loginLoading = false;
      })
      .addCase(fetchShareVizInfo.rejected, state => {
        state.loginLoading = false;
      })
      .addCase(fetchShareDataSetByPreviewChartAction.pending, state => {
        if (state.chartPreview) {
          state.chartPreview.isLoadingData = true;
        }
      })
      .addCase(fetchShareDataSetByPreviewChartAction.rejected, state => {
        if (state.chartPreview) {
          state.chartPreview.isLoadingData = false;
        }
        state.headlessBrowserRenderSign = true;
      })
      .addCase(
        fetchShareDataSetByPreviewChartAction.fulfilled,
        (state, { payload }) => {
          state.chartPreview = {
            ...state.chartPreview,
            isLoadingData: false,
            dataset: payload as any,
          };
          state.selectedItems = [];
          state.headlessBrowserRenderSign = true;
        },
      )
      .addCase(getOauth2Clients.fulfilled, (state, action) => {
        state.oauth2Clients = action.payload.map(x => ({
          name: Object.keys(x)[0],
          value: x[Object.keys(x)[0]],
        }));
      })
      .addCase(
        fetchAvailableSourceFunctionsForShare.fulfilled,
        (state, { payload }) => {
          state.availableSourceFunctions = payload;
        },
      );
  },
});

export const { actions: shareActions, reducer } = slice;

export const useShareSlice = () => {
  useInjectReducer({ key: slice.name, reducer: slice.reducer });
  return { shareActions: slice.actions };
};
