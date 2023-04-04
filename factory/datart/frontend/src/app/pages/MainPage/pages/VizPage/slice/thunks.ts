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
import {
  Dashboard,
  DataChart,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { mainActions } from 'app/pages/MainPage/slice';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { ChartDTO } from 'app/types/ChartDTO';
import { convertToChartDto } from 'app/utils/ChartDtoHelper';
import { filterSqlOperatorName } from 'app/utils/internalChartHelper';
import { RootState } from 'types';
import { request2 } from 'utils/request';
import { vizActions } from '.';
import { selectSelectedTab, selectVizs } from './selectors';
import {
  AddStoryboardParams,
  AddVizParams,
  ChartPreview,
  DeleteStoryboardParams,
  DeleteVizParams,
  EditFolderParams,
  EditStoryboardParams,
  FilterSearchParams,
  Folder,
  FolderViewModel,
  PublishVizParams,
  SaveAsDashboardParams,
  StoryboardBase,
  StoryboardSimple,
  UnarchiveVizParams,
  VizState,
} from './types';

export const getFolders = createAsyncThunk<Folder[], string>(
  'viz/getFolders',
  async orgId => {
    const response = await request2<Folder[]>(`/viz/folders?orgId=${orgId}`);
    return response?.data;
  },
);

export const getStoryboards = createAsyncThunk<StoryboardSimple[], string>(
  'viz/getStoryboards',
  async orgId => {
    const { data } = await request2<StoryboardSimple[]>(
      `viz/storyboards?orgId=${orgId}`,
    );
    return data;
  },
);

export const getArchivedDatacharts = createAsyncThunk<DataChart[], string>(
  'viz/getArchivedDatacharts',
  async orgId => {
    const { data } = await request2<DataChart[]>(
      `/viz/archived/datachart/${orgId}`,
    );
    return data;
  },
);

export const getArchivedDashboards = createAsyncThunk<Dashboard[], string>(
  'viz/getArchivedDashboards',
  async orgId => {
    const { data } = await request2<Dashboard[]>(
      `/viz/archived/dashboard/${orgId}`,
    );
    return data;
  },
);

export const getArchivedStoryboards = createAsyncThunk<
  StoryboardSimple[],
  string
>('viz/getArchivedStoryboards', async orgId => {
  const { data } = await request2<StoryboardSimple[]>(
    `/viz/archived/storyboard/${orgId}`,
  );
  return data;
});

export const addStoryboard = createAsyncThunk<
  StoryboardSimple,
  AddStoryboardParams,
  { state: RootState }
>('viz/addStoryboard', async ({ storyboard, resolve }) => {
  const { data } = await request2<StoryboardSimple>({
    url: `/viz/storyboards`,
    method: 'POST',
    data: storyboard,
  });

  resolve();
  return data;
});

export const editStoryboard = createAsyncThunk<
  StoryboardBase,
  EditStoryboardParams
>('viz/editStoryboard', async ({ storyboard, resolve }) => {
  await request2<StoryboardBase>({
    url: `/viz/storyboards/${storyboard.id}`,
    method: 'PUT',
    data: storyboard,
  });
  resolve();
  return storyboard;
});

export const deleteStoryboard = createAsyncThunk<
  boolean,
  DeleteStoryboardParams
>('viz/deleteStoryboard', async ({ id, archive, resolve }) => {
  const { data } = await request2<boolean>({
    url: `/viz/storyboards/${id}`,
    method: 'DELETE',
    params: { archive },
  });
  resolve();
  return data;
});

export const addViz = createAsyncThunk<Folder, AddVizParams>(
  'viz/addViz',
  async ({ viz, type }, { dispatch }) => {
    if (type === 'TEMPLATE') {
      const { data } = await request2<Folder>({
        url: `/viz/import/template?parentId=${viz.parentId || ''}&orgId=${
          viz.orgId
        }&name=${viz.name}`,
        method: 'POST',
        data: viz.file,
      });
      dispatch(getFolders(viz.orgId as string));
      return data;
    } else {
      const { data } = await request2<Folder>({
        url: `/viz/${type.toLowerCase()}s`,
        method: 'POST',
        data: viz,
      });
      return data;
    }
  },
);

export const editFolder = createAsyncThunk<
  FolderViewModel,
  EditFolderParams,
  { state: RootState }
>('viz/editFolder', async ({ folder, resolve }, { getState }) => {
  const folders = selectVizs(getState());
  const origin = folders.find(({ id }) => id === folder.id)!;
  const merged = { ...origin, ...folder };
  await request2<boolean>({
    url: `/viz/folders/${folder.id}`,
    method: 'PUT',
    data: merged,
  });
  resolve();
  return merged;
});

export const unarchiveViz = createAsyncThunk<void, UnarchiveVizParams>(
  'viz/unarchiveViz',
  async (
    { params: { id, name, vizType, parentId, index }, resolve },
    thunkAPI,
  ) => {
    await request2<boolean>({
      url: `/viz/unarchive/${id}`,
      method: 'PUT',
      params: { vizType, newName: name, parentId, index },
    });
    resolve();
  },
);

export const deleteViz = createAsyncThunk<boolean, DeleteVizParams>(
  'viz/deleteViz',
  async ({ params: { id, archive }, type, resolve }) => {
    const { data } = await request2<boolean>({
      url: `/viz/${type.toLowerCase()}s/${id}`,
      method: 'DELETE',
      params: { archive },
    });
    resolve();
    return data;
  },
);

export const publishViz = createAsyncThunk<null, PublishVizParams>(
  'viz/publishViz',
  async ({ id, vizType, publish, resolve }) => {
    await request2<boolean>({
      url: `/viz/${publish ? 'publish' : 'unpublish'}/${id}`,
      method: 'PUT',
      params: { vizType },
    });
    resolve();
    return null;
  },
);

export const removeTab = createAsyncThunk<
  null,
  { id: string; resolve: (selectedTab: string) => void },
  { state: RootState }
>('viz/removeTab', async ({ id, resolve }, { getState, dispatch }) => {
  dispatch(vizActions.closeTab(id));
  const selectedTab = selectSelectedTab(getState());
  resolve(selectedTab ? selectedTab.id : '');
  return null;
});

export const closeAllTabs = createAsyncThunk<
  null,
  { resolve: (selectedTab: string) => void },
  { state: RootState }
>('viz/closeAllTabs', async ({ resolve }, { getState, dispatch }) => {
  dispatch(vizActions.closeAllTabs());
  resolve('');
  return null;
});

export const closeOtherTabs = createAsyncThunk<
  null,
  { id: string; resolve: (selectedTab: string) => void },
  { state: RootState }
>('viz/closeOtherTabs', async ({ id, resolve }, { getState, dispatch }) => {
  dispatch(vizActions.closeOtherTabs(id));
  const selectedTab = selectSelectedTab(getState());
  resolve(selectedTab ? selectedTab.id : '');
  return null;
});

export const initChartPreviewData = createAsyncThunk<
  { backendChartId: string },
  {
    backendChartId: string;
    orgId: string;
    filterSearchParams?: FilterSearchParams;
    jumpFilterParams?: PendingChartDataRequestFilter[];
    jumpVariableParams?: Record<string, any[]>;
  }
>(
  'viz/initChartPreviewData',
  async (
    {
      backendChartId,
      filterSearchParams,
      jumpFilterParams,
      jumpVariableParams,
    },
    thunkAPI,
  ) => {
    await thunkAPI.dispatch(
      fetchVizChartAction({
        backendChartId,
        filterSearchParams,
        jumpFilterParams,
      }),
    );
    if (backendChartId) {
      await thunkAPI.dispatch(
        fetchDataSetByPreviewChartAction({
          backendChartId,
          jumpVariableParams,
        }),
      );
    }
    return { backendChartId };
  },
);

export const fetchVizChartAction = createAsyncThunk(
  'viz/fetchVizChartAction',
  async (arg: {
    backendChartId;
    filterSearchParams?: FilterSearchParams;
    jumpFilterParams?: PendingChartDataRequestFilter[];
  }) => {
    const response = await request2<
      Omit<ChartDTO, 'config'> & { config: string }
    >({
      method: 'GET',
      url: `viz/datacharts/${arg.backendChartId}`,
    });
    return {
      data: convertToChartDto(response?.data),
      filterSearchParams: arg.filterSearchParams,
      jumpFilterParams: arg.jumpFilterParams,
    };
  },
);
export const exportChartTpl = createAsyncThunk(
  'viz/exportChartTpl',
  async (arg: { chartId; dataset; callBack }, thunkAPI) => {
    const vizState = (thunkAPI.getState() as RootState)?.viz as VizState;
    const { chartId, dataset, callBack } = arg;
    const dataChart = vizState.chartPreviews.find(
      item => item.backendChartId === chartId,
    );
    const newConf = { ...dataChart?.backendChart?.config };
    newConf.sampleData = dataset;
    // newConf.
    const newChart = {
      avatar: newConf.chartGraphId, //
      config: JSON.stringify(newConf),
    };
    await request2<any>({
      url: `viz/export/datachart/template`,
      method: 'POST',
      data: { datachart: newChart },
    });
    callBack();
    thunkAPI.dispatch(mainActions.setDownloadPolling(true));
  },
);
export const fetchDataSetByPreviewChartAction = createAsyncThunk(
  'viz/fetchDataSetByPreviewChartAction',
  async (
    arg: {
      backendChartId: string;
      pageInfo?;
      sorter?: { column: string; operator: string; aggOperator?: string };
      drillOption?: IChartDrillOption;
      jumpVariableParams?: Record<string, any[]>;
    },
    thunkAPI,
  ) => {
    const vizState = (thunkAPI.getState() as RootState)?.viz as VizState;
    const currentChartPreview = vizState?.chartPreviews?.find(
      c => c.backendChartId === arg.backendChartId,
    );
    if (!currentChartPreview?.backendChart?.view.id) {
      return {
        backendChartId: currentChartPreview?.backendChartId,
        data: currentChartPreview?.backendChart?.config.sampleData || [],
      };
    }
    const builder = new ChartDataRequestBuilder(
      {
        id: currentChartPreview?.backendChart?.view.id || '',
        config: currentChartPreview?.backendChart?.view.config || {},
        meta: currentChartPreview?.backendChart?.view.meta,
        computedFields:
          currentChartPreview?.backendChart?.config?.computedFields || [],
        type: currentChartPreview?.backendChart?.view.type || 'SQL',
      },
      currentChartPreview?.chartConfig?.datas,
      currentChartPreview?.chartConfig?.settings,
      arg.pageInfo,
      false,
      currentChartPreview?.backendChart?.config?.aggregation,
    );

    const data = builder
      .addExtraSorters(arg?.sorter ? [arg?.sorter as any] : [])
      .addDrillOption(arg?.drillOption)
      .addVariableParams(arg?.jumpVariableParams)
      .build();

    const response = await request2({
      method: 'POST',
      url: `data-provider/execute`,
      data,
    });
    return {
      backendChartId: currentChartPreview?.backendChartId,
      data: filterSqlOperatorName(data, response.data) || [],
    };
  },
);

export const updateFilterAndFetchDataset = createAsyncThunk(
  'viz/updateFilterAndFetchDataset',
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
      vizActions.updateChartPreviewFilter({
        backendChartId: arg.backendChartId,
        payload: arg.payload,
      }),
    );
    await thunkAPI.dispatch(
      fetchDataSetByPreviewChartAction({
        backendChartId: arg.backendChartId,
        drillOption: arg.drillOption,
      }),
    );

    return {
      backendChartId: arg.backendChartId,
    };
  },
);

export const updateGroupAndFetchDataset = createAsyncThunk(
  'viz/updateGroupAndFetchDataset',
  async (
    arg: {
      backendChartId: string;
      payload;
      drillOption?: IChartDrillOption;
    },
    thunkAPI,
  ) => {
    await thunkAPI.dispatch(
      vizActions.updateChartPreviewGroup({
        backendChartId: arg.backendChartId,
        payload: arg.payload,
      }),
    );
    await thunkAPI.dispatch(
      fetchDataSetByPreviewChartAction({
        backendChartId: arg.backendChartId,
        drillOption: arg.drillOption,
      }),
    );

    return {
      backendChartId: arg.backendChartId,
    };
  },
);

export const saveAsDashboard = createAsyncThunk<Folder, SaveAsDashboardParams>(
  'viz/saveAsDashboard',
  async ({ viz, dashboardId }) => {
    const { data } = await request2<Folder>({
      url: `/viz/dashboards/${dashboardId}/copy`,
      method: 'PUT',
      data: viz,
    });
    return data;
  },
);
