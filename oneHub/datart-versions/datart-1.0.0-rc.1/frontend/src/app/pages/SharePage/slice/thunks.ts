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
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import { handleServerBoardAction } from 'app/pages/DashBoardPage/pages/Board/slice/asyncActions';
import {
  ServerDashboard,
  VizRenderMode,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import {
  ChartPreview,
  FilterSearchParams,
} from 'app/pages/MainPage/pages/VizPage/slice/types';
import { handleServerStoryAction } from 'app/pages/StoryBoardPage/slice/actions';
import { ServerStoryBoard } from 'app/pages/StoryBoardPage/slice/types';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { convertToChartDto } from 'app/utils/ChartDtoHelper';
import { fetchAvailableSourceFunctionsAsyncForShare } from 'app/utils/fetch';
import { RootState } from 'types';
import persistence from 'utils/persistence';
import { request2 } from 'utils/request';
import { shareActions } from '.';
import { ShareVizInfo } from './types';

export const fetchShareVizInfo = createAsyncThunk(
  'share/fetchShareVizInfo',
  async (
    {
      shareToken,
      sharePassword,
      filterSearchParams,
      renderMode,
      userName,
      passWord,
      authorizedToken,
    }: {
      shareToken?: string;
      sharePassword?: string;
      filterSearchParams?: FilterSearchParams;
      renderMode?: VizRenderMode;
      userName?: string;
      passWord?: string;
      authorizedToken?: string;
    },
    thunkAPI,
  ) => {
    const authenticationMode = filterSearchParams?.type?.join();
    const isMatchByName = !!filterSearchParams?.isMatchByName;

    let data = {} as ShareVizInfo;
    const response = await request2<ShareVizInfo>({
      url: `/shares/${shareToken}/viz`,
      method: 'POST',
      data: {
        authenticationMode,
        authenticationCode: sharePassword,
        id: shareToken,
        username: userName,
        password: passWord,
        authorizedToken,
      },
    });
    data = response.data;
    await thunkAPI.dispatch(shareActions.setVizType(data.vizType));
    if (authenticationMode === 'CODE') {
      persistence.session.save(shareToken, sharePassword);
    }
    await thunkAPI.dispatch(shareActions.saveNeedVerify(false));
    await thunkAPI.dispatch(
      shareActions.saveShareInfo({
        token: data.executeToken,
        pwd: sharePassword,
      }),
    );

    await thunkAPI.dispatch(
      shareActions.setExecuteTokenMap({
        executeToken: data.executeToken,
      }),
    );

    switch (data.vizType) {
      case 'DATACHART':
        const shareVizInfo = {
          ...data,
          vizDetail: convertToChartDto(data.vizDetail),
        };
        thunkAPI.dispatch(
          shareActions.setDataChart({
            data: shareVizInfo,
            filterSearchParams,
            isMatchByName,
          }),
        );
        break;
      case 'DASHBOARD':
        const serverBoard = data.vizDetail as ServerDashboard;
        // setExecuteTokenMap
        thunkAPI.dispatch(
          handleServerBoardAction({
            data: serverBoard,
            renderMode: renderMode || 'share',
            filterSearchMap: {
              params: filterSearchParams,
              isMatchByName: true,
            },
            executeToken: data.executeToken,
          }),
        );
        break;
      case 'STORYBOARD':
        thunkAPI.dispatch(
          shareActions.setSubVizTokenMap({
            subVizToken: data.subVizToken,
          }),
        );

        thunkAPI.dispatch(
          handleServerStoryAction({
            data: data.vizDetail as ServerStoryBoard,
            renderMode: 'read',
            storyId: data.vizDetail.id,
          }),
        );
        break;
      default:
        break;
    }
    return { data, filterSearchParams };
  },
);

export const fetchShareDataSetByPreviewChartAction = createAsyncThunk(
  'share/fetchDataSetByPreviewChartAction',
  async (
    args: {
      preview: ChartPreview;
      pageInfo?: any;
      sorter?: { column: string; operator: string; aggOperator?: string };
      drillOption?: IChartDrillOption;
      filterSearchParams?: FilterSearchParams;
    },
    thunkAPI,
  ) => {
    const state = thunkAPI.getState() as RootState;
    const shareState = state.share;
    if (!args.preview?.backendChart?.view.id) {
      return;
    }
    const builder = new ChartDataRequestBuilder(
      {
        id: args.preview?.backendChart?.view.id || '',
        config: args.preview?.backendChart?.view.config || {},
        meta: args?.preview?.backendChart?.view?.meta,
        computedFields:
          args.preview?.backendChart?.config?.computedFields || [],
        type: args.preview?.backendChart?.view.type || 'SQL',
      },
      args.preview?.chartConfig?.datas,
      args.preview?.chartConfig?.settings,
      args.pageInfo,
      false,
      args.preview?.backendChart?.config?.aggregation,
    );
    const executeParam = builder
      .addExtraSorters(args?.sorter ? [args?.sorter as any] : [])
      .addDrillOption(args?.drillOption)
      .build();

    const response = await request2({
      method: 'POST',
      url: `shares/execute`,
      params: {
        executeToken:
          shareState?.shareToken[executeParam.viewId]['authorizedToken'],
      },
      data: executeParam,
    });
    return response.data;
  },
);

export const updateFilterAndFetchDatasetForShare = createAsyncThunk(
  'share/updateFilterAndFetchDatasetForShare',
  async (
    arg: {
      backendChartId: string;
      chartPreview?: ChartPreview;
      payload;
      drillOption?: IChartDrillOption;
    },
    thunkAPI,
  ) => {
    await thunkAPI.dispatch(
      shareActions.updateChartPreviewFilter({
        backendChartId: arg.backendChartId,
        payload: arg.payload,
      }),
    );
    const state = thunkAPI.getState() as RootState;
    const shareState = state.share;
    await thunkAPI.dispatch(
      fetchShareDataSetByPreviewChartAction({
        preview: shareState?.chartPreview!,
        drillOption: arg.drillOption,
      }),
    );
    return {
      backendChartId: arg.backendChartId,
    };
  },
);

export const updateGroupAndFetchDatasetForShare = createAsyncThunk(
  'share/updateGroupAndFetchDatasetForShare',
  async (
    arg: {
      backendChartId: string;
      chartPreview?: ChartPreview;
      payload;
      drillOption?: IChartDrillOption;
    },
    thunkAPI,
  ) => {
    await thunkAPI.dispatch(
      shareActions.updateChartPreviewGroup({
        backendChartId: arg.backendChartId,
        payload: arg.payload,
      }),
    );
    const state = thunkAPI.getState() as RootState;
    const shareState = state.share;
    await thunkAPI.dispatch(
      fetchShareDataSetByPreviewChartAction({
        preview: shareState?.chartPreview!,
        drillOption: arg.drillOption,
      }),
    );
    return {
      backendChartId: arg.backendChartId,
    };
  },
);

export const getOauth2Clients = createAsyncThunk<[]>(
  'app/getOauth2Clients',
  async () => {
    const { data } = await request2<[]>({
      url: '/tpa/getOauth2Clients',
      method: 'GET',
    });
    return data;
  },
);

export const fetchAvailableSourceFunctionsForShare = createAsyncThunk<
  string[],
  { sourceId: string; executeToken: string }
>(
  'workbench/fetchAvailableSourceFunctionsAsyncForShare',
  async ({ sourceId, executeToken }) => {
    try {
      return await fetchAvailableSourceFunctionsAsyncForShare(
        sourceId,
        executeToken,
      );
    } catch (err) {
      throw err;
    }
  },
);
