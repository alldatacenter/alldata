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

import { createAsyncThunk } from '@reduxjs/toolkit';
import migrationViewConfig from 'app/migration/ViewConfig/migrationViewConfig';
import beginViewModelMigration from 'app/migration/ViewConfig/migrationViewModelConfig';
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import { ChartConfig, ChartDataConfig } from 'app/types/ChartConfig';
import { ChartDataRequest } from 'app/types/ChartDataRequest';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { ChartDTO } from 'app/types/ChartDTO';
import { View } from 'app/types/View';
import {
  buildUpdateChartRequest,
  convertToChartDto,
} from 'app/utils/ChartDtoHelper';
import { filterCurrentUsedComputedFields } from 'app/utils/chartHelper';
import { fetchAvailableSourceFunctionsAsync } from 'app/utils/fetch';
import { filterSqlOperatorName } from 'app/utils/internalChartHelper';
import { request2 } from 'utils/request';
import { rejectHandle } from 'utils/utils';
import workbenchSlice, { initState } from '.';
import { ChartConfigPayloadType } from './types';

export const initWorkbenchAction = createAsyncThunk(
  'workbench/initWorkbenchAction',
  async (
    arg: {
      backendChartId?: string;
      backendChart?: ChartDTO;
      orgId?: string;
    },
    thunkAPI,
  ) => {
    try {
      if (arg.orgId) {
        await thunkAPI.dispatch(fetchDataViewsAction({ orgId: arg.orgId }));
      }
      if (arg.backendChartId) {
        await thunkAPI.dispatch(
          workbenchSlice.actions.saveBackendChartId(arg.backendChartId),
        );
        await thunkAPI.dispatch(
          fetchChartAction({ chartId: arg.backendChartId }),
        );
        await thunkAPI.dispatch(refreshDatasetAction({}));
      } else if (arg.backendChart) {
        await thunkAPI.dispatch(
          fetchChartAction({ backendChart: arg.backendChart }),
        );
        await thunkAPI.dispatch(refreshDatasetAction({}));
      }
    } catch (error) {
      return rejectHandle(error, thunkAPI.rejectWithValue);
    }
  },
);

export const fetchDataSetAction = createAsyncThunk(
  'workbench/fetchDataSetAction',
  async (arg: ChartDataRequest, thunkAPI) => {
    let errorData: any = null;
    const response = await request2(
      {
        method: 'POST',
        url: `data-provider/execute`,
        data: arg,
      },
      {},
      {
        onRejected: error => {
          errorData = error.response;
        },
      },
    );
    if (errorData) {
      return thunkAPI.rejectWithValue(errorData?.data);
    } else {
      return filterSqlOperatorName(arg, response.data);
    }
  },
);

export const fetchDataViewsAction = createAsyncThunk(
  'workbench/fetchDataViewsAction',
  async (arg: { orgId }) => {
    const response = await request2<any[]>({
      method: 'GET',
      url: `views`,
      params: arg,
    });
    return response.data;
  },
);

export const fetchViewDetailAction = createAsyncThunk(
  'workbench/fetchViewDetailAction',
  async (arg: { viewId }) => {
    const response = await request2<View>({
      method: 'GET',
      url: `views/${arg}`,
    });
    if (response?.data) {
      response.data = migrationViewConfig(response.data);
    }
    if (response?.data?.model) {
      response.data.model = beginViewModelMigration(
        response.data?.model,
        response.data.type,
      );
    }
    return response.data;
  },
);

export const updateChartConfigAndRefreshDatasetAction = createAsyncThunk(
  'workbench/updateChartConfigAndRefreshDatasetAction',
  async (
    arg: {
      type: string;
      payload: ChartConfigPayloadType;
      needRefresh?: boolean;
      updateDrillOption: (
        config?: ChartConfig,
      ) => IChartDrillOption | undefined;
    },
    thunkAPI,
  ) => {
    try {
      await thunkAPI.dispatch(workbenchSlice.actions.updateChartConfig(arg));
      await thunkAPI.dispatch(
        workbenchSlice.actions.updateShadowChartConfig(null),
      );
      const state = thunkAPI.getState() as any;
      const workbenchState = state.workbench as typeof initState;
      const chartConfig = workbenchState.chartConfig;
      const drillOpt = arg.updateDrillOption?.(chartConfig);
      if (arg.needRefresh) {
        thunkAPI.dispatch(refreshDatasetAction({ drillOption: drillOpt }));
      }
    } catch (error) {
      return rejectHandle(error, thunkAPI.rejectWithValue);
    }
  },
);

export const refreshDatasetAction = createAsyncThunk(
  'workbench/refreshDatasetAction',
  async (
    arg: {
      dataOption?: ChartDataConfig[];
      drillOption?: IChartDrillOption;
      pageInfo?;
      sorter?: { column: string; operator: string; aggOperator?: string };
    },
    thunkAPI,
  ) => {
    const state = thunkAPI.getState() as any;
    const workbenchState = state.workbench as typeof initState;

    if (!workbenchState.currentDataView?.id) {
      return;
    }
    const datas = arg?.dataOption || workbenchState.chartConfig?.datas;

    const builder = new ChartDataRequestBuilder(
      workbenchState.currentDataView,
      datas,
      workbenchState.chartConfig?.settings,
      arg?.pageInfo,
      true,
      workbenchState.aggregation,
    );
    const requestParams = builder
      .addExtraSorters(arg?.sorter ? [arg?.sorter as any] : [])
      .addDrillOption(arg?.drillOption)
      .build();
    return thunkAPI.dispatch(fetchDataSetAction(requestParams));
  },
);

export const fetchChartAction = createAsyncThunk<
  ChartDTO,
  { chartId?: string; backendChart?: ChartDTO },
  any
>('workbench/fetchChartAction', async arg => {
  if (arg?.chartId) {
    const response = await request2<
      Omit<ChartDTO, 'config'> & { config: string }
    >({
      method: 'GET',
      url: `viz/datacharts/${arg.chartId}`,
    });
    return convertToChartDto(response?.data);
  }
  return arg.backendChart as any;
});

export const updateChartAction = createAsyncThunk(
  'workbench/updateChartAction',
  async (
    arg: { name; viewId; graphId; chartId; index; parentId; aggregation },
    thunkAPI,
  ) => {
    const state = thunkAPI.getState() as any;
    const workbenchState = state.workbench as typeof initState;
    const computedFields = filterCurrentUsedComputedFields(
      workbenchState.chartConfig,
      workbenchState.currentDataView?.computedFields?.filter(
        v => !v.isViewComputedFields,
      ),
    );

    const requestBody = buildUpdateChartRequest({
      chartId: arg.chartId,
      aggregation: arg.aggregation,
      chartConfig: workbenchState.chartConfig,
      graphId: arg.graphId,
      index: arg.index,
      parentId: arg.parentId,
      name: arg.name,
      viewId: arg.viewId,
      computedFields,
    });

    const response = await request2<{
      data: boolean;
    }>({
      method: 'PUT',
      url: `viz/datacharts/${arg.chartId}`,
      data: requestBody,
    });
    return response.data;
  },
);

export const fetchAvailableSourceFunctionsForChart = createAsyncThunk<
  string[],
  string
>('workbench/fetchAvailableSourceFunctionsForChart', async sourceId => {
  try {
    const data = await fetchAvailableSourceFunctionsAsync(sourceId);
    return data;
  } catch (err) {
    throw err;
  }
});
