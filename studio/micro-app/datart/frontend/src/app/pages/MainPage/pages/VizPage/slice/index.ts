import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ChartDataSectionType } from 'app/constants';
import { migrateChartConfig } from 'app/migration';
import ChartManager from 'app/models/ChartManager';
import { SelectedItem } from 'app/types/ChartConfig';
import { mergeToChartConfig } from 'app/utils/ChartDtoHelper';
import { useInjectReducer } from 'utils/@reduxjs/injectReducer';
import { CloneValueDeep } from 'utils/object';
import { uuidv4 } from 'utils/utils';
import {
  addStoryboard,
  addViz,
  deleteViz,
  editFolder,
  editStoryboard,
  fetchDataSetByPreviewChartAction,
  fetchVizChartAction,
  getArchivedDashboards,
  getArchivedDatacharts,
  getArchivedStoryboards,
  getFolders,
  getStoryboards,
  initChartPreviewData,
  publishViz,
  saveAsDashboard,
  unarchiveViz,
  updateFilterAndFetchDataset,
  updateGroupAndFetchDataset,
} from './thunks';
import { ArchivedViz, VizState, VizTab } from './types';
import { transferChartConfig } from './utils';
export const initialState: VizState = {
  vizs: [],
  hasVizFetched: false,
  storyboards: [],
  vizListLoading: false,
  storyboardListLoading: false,
  saveFolderLoading: false,
  saveStoryboardLoading: false,
  publishLoading: false,
  archivedDatacharts: [],
  archivedDashboards: [],
  archivedStoryboards: [],
  archivedDatachartLoading: false,
  archivedDashboardLoading: false,
  archivedStoryboardLoading: false,
  tabs: [],
  selectedTab: '',
  dataChartListLoading: false,
  chartPreviews: [],
  selectedItems: {} as Record<string, SelectedItem[]>,
};

const slice = createSlice({
  name: 'viz',
  initialState,
  reducers: {
    addTab(state, action: PayloadAction<VizTab>) {
      const index = state.tabs.findIndex(t => t.id === action.payload.id);
      if (index === -1) {
        state.tabs.push(action.payload);
      } else if (state.tabs[index].search !== action.payload.search) {
        state.tabs[index] = action.payload;
      }
      state.selectedTab = action.payload.id;
    },
    closeTab(state, action: PayloadAction<string>) {
      const index = state.tabs.findIndex(t => t.id === action.payload);
      state.tabs = state.tabs.filter(t => t.id !== action.payload);
      if (state.selectedTab === action.payload) {
        state.selectedTab =
          state.tabs.length > 0
            ? state.tabs[index]?.id || state.tabs[state.tabs.length - 1].id
            : '';
      }
    },
    closeOtherTabs(state: VizState, action: PayloadAction<string>) {
      const currentTab = state.tabs.find(t => t.id === action.payload);
      state.tabs = state.tabs.filter(t => t.id === action.payload);
      state.selectedTab = currentTab?.id || '';
    },
    closeAllTabs(state) {
      state.tabs = [];
      state.selectedTab = '';
    },
    updateChartPreviewFilter(
      state,
      action: PayloadAction<{ backendChartId: string; payload }>,
    ) {
      const chartPreview = state.chartPreviews.find(
        c => c.backendChartId === action.payload.backendChartId,
      );
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
      const chartPreview = state.chartPreviews.find(
        c => c.backendChartId === action.payload.backendChartId,
      );

      if (chartPreview) {
        const groupSection = chartPreview?.chartConfig?.datas?.find(
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
      const chartPreview = state.chartPreviews.find(
        c => c.backendChartId === action.payload.backendChartId,
      );

      if (chartPreview && chartPreview?.backendChart?.config) {
        chartPreview.backendChart.config.computedFields =
          action.payload.computedFields;
      }
    },
    clear(state) {
      Object.entries(initialState).forEach(([key, value]) => {
        state[key] = value;
      });
    },
    changeSelectedItems(
      state,
      {
        payload,
      }: PayloadAction<{ backendChartId: string; data: SelectedItem[] }>,
    ) {
      state.selectedItems[payload.backendChartId] = payload.data;
    },
  },
  extraReducers: builder => {
    // getFolders
    builder.addCase(getFolders.pending, state => {
      state.vizListLoading = true;
    });
    builder.addCase(getFolders.fulfilled, (state, action) => {
      state.vizListLoading = false;
      state.hasVizFetched = true;
      state.vizs = action.payload.map(f => ({ ...f, deleteLoading: false }));
    });
    builder.addCase(getFolders.rejected, state => {
      state.vizListLoading = false;
      state.hasVizFetched = true;
    });

    // getStoryboards
    builder.addCase(getStoryboards.pending, state => {
      state.storyboardListLoading = true;
    });
    builder.addCase(getStoryboards.fulfilled, (state, action) => {
      state.storyboardListLoading = false;
      state.storyboards = action.payload.map(s => ({
        ...s,
        deleteLoading: false,
      }));
    });
    builder.addCase(getStoryboards.rejected, state => {
      state.storyboardListLoading = false;
    });

    // getArchivedDatacharts
    builder.addCase(getArchivedDatacharts.pending, state => {
      state.archivedDatachartLoading = true;
    });
    builder.addCase(getArchivedDatacharts.fulfilled, (state, action) => {
      state.archivedDatachartLoading = false;
      state.archivedDatacharts = action.payload.map(({ id, name }) => ({
        id,
        name,
        vizType: 'DATACHART',
        deleteLoading: false,
        index: null,
        isFolder: false,
        parentId: null,
      }));
    });
    builder.addCase(getArchivedDatacharts.rejected, state => {
      state.archivedDatachartLoading = false;
    });

    // getArchivedDashboards
    builder.addCase(getArchivedDashboards.pending, state => {
      state.archivedDashboardLoading = true;
    });
    builder.addCase(getArchivedDashboards.fulfilled, (state, action) => {
      state.archivedDashboardLoading = false;
      state.archivedDashboards = action.payload.map(({ id, name }) => ({
        id,
        name,
        vizType: 'DASHBOARD',
        deleteLoading: false,
        index: null,
        isFolder: false,
        parentId: null,
      }));
    });
    builder.addCase(getArchivedDashboards.rejected, state => {
      state.archivedDashboardLoading = false;
    });

    // getArchivedStoryboards
    builder.addCase(getArchivedStoryboards.pending, state => {
      state.archivedStoryboardLoading = true;
    });
    builder.addCase(getArchivedStoryboards.fulfilled, (state, action) => {
      state.archivedStoryboardLoading = false;
      state.archivedStoryboards = action.payload.map(({ id, name }) => ({
        id,
        name,
        vizType: 'STORYBOARD',
        deleteLoading: false,
        index: null,
        isFolder: false,
        parentId: null,
      }));
    });
    builder.addCase(getArchivedStoryboards.rejected, state => {
      state.archivedStoryboardLoading = false;
    });

    // addViz
    builder.addCase(addViz.pending, state => {
      state.saveFolderLoading = true;
    });
    builder.addCase(addViz.fulfilled, (state, action) => {
      state.saveFolderLoading = false;
      state.vizs.push({ ...action.payload, deleteLoading: false });
    });
    builder.addCase(addViz.rejected, state => {
      state.saveFolderLoading = false;
    });

    // editFolder
    builder.addCase(editFolder.pending, state => {
      state.saveFolderLoading = true;
    });
    builder.addCase(editFolder.fulfilled, (state, action) => {
      state.saveFolderLoading = false;
      state.vizs = state.vizs.map(v =>
        v.id === action.payload.id ? action.payload : v,
      );
    });
    builder.addCase(editFolder.rejected, state => {
      state.saveFolderLoading = false;
    });

    // publishViz
    builder.addCase(publishViz.pending, state => {
      state.publishLoading = true;
    });
    builder.addCase(publishViz.fulfilled, (state, action) => {
      state.publishLoading = false;
      const status = action.meta.arg.publish ? 2 : 1;

      // if (action.meta.arg.vizType !== 'STORYBOARD') {
      //   const viz = state.vizs.find(({ id }) => id === action.meta.arg.id);
      //   if (viz) {
      //     viz.status = status;
      //   }
      // }

      switch (action.meta.arg.vizType) {
        case 'DATACHART':
          const index = state.chartPreviews?.findIndex(
            c => c.backendChartId === action.meta.arg.id,
          );
          if (index >= 0 && state.chartPreviews[index].backendChart) {
            state.chartPreviews[index] = {
              ...state.chartPreviews[index],
              backendChart: {
                ...state.chartPreviews[index].backendChart!,
                status,
              },
              version: uuidv4(),
            };
          }
          break;
        case 'DASHBOARD':
          // handle in DashboardPage/slice
          break;
        case 'STORYBOARD':
          // const storyboard = state.storyboards.find(
          //   ({ id }) => id === action.meta.arg.id,
          // );
          // if (storyboard) {
          //   storyboard.status = status;
          // }

          // handle in StoryboardPage/slice
          break;
      }
    });
    builder.addCase(publishViz.rejected, state => {
      state.publishLoading = false;
    });

    // unarchiveViz
    builder.addCase(unarchiveViz.pending, (state, action) => {
      const { id, vizType } = action.meta.arg.params;
      let viz: undefined | ArchivedViz;
      switch (vizType) {
        case 'DASHBOARD':
          viz = state.archivedDashboards.find(d => d.id === id);
          break;
        case 'DATACHART':
          viz = state.archivedDatacharts.find(d => d.id === id);
          break;
        case 'STORYBOARD':
          viz = state.archivedStoryboards.find(s => s.id === id);
          break;
      }
      if (viz) {
        viz.deleteLoading = true;
      }
    });
    builder.addCase(unarchiveViz.fulfilled, (state, action) => {
      const { id, vizType } = action.meta.arg.params;
      switch (vizType) {
        case 'DASHBOARD':
          state.archivedDashboards = state.archivedDashboards.filter(
            d => d.id !== id,
          );
          break;
        case 'DATACHART':
          state.archivedDatacharts = state.archivedDatacharts.filter(
            d => d.id !== id,
          );
          break;
        case 'STORYBOARD':
          state.archivedStoryboards = state.archivedStoryboards.filter(
            s => s.id !== id,
          );
          break;
      }
    });
    builder.addCase(unarchiveViz.rejected, (state, action) => {
      const { id, vizType } = action.meta.arg.params;
      let viz: undefined | ArchivedViz;
      switch (vizType) {
        case 'DASHBOARD':
          viz = state.archivedDashboards.find(d => d.id === id);
          break;
        case 'DATACHART':
          viz = state.archivedDatacharts.find(d => d.id === id);
          break;
        case 'STORYBOARD':
          viz = state.archivedStoryboards.find(s => s.id === id);
          break;
      }
      if (viz) {
        viz.deleteLoading = false;
      }
    });

    // deleteViz
    builder.addCase(deleteViz.pending, (state, action) => {
      const {
        params: { id, archive },
        type,
      } = action.meta.arg;
      if (archive) {
        if (type === 'STORYBOARD') {
          const storyboard = state.storyboards.find(s => s.id === id);
          if (storyboard) {
            storyboard.deleteLoading = true;
          }
        } else {
          const viz = state.vizs.find(v => v.relId === id);
          if (viz) {
            viz.deleteLoading = true;
          }
        }
      } else {
        if (type === 'DASHBOARD') {
          const dashboard = state.archivedDashboards.find(a => a.id === id);
          if (dashboard) {
            dashboard.deleteLoading = true;
          }
        } else if (type === 'DATACHART') {
          const datachart = state.archivedDatacharts.find(a => a.id === id);
          if (datachart) {
            datachart.deleteLoading = true;
          }
        } else if (type === 'STORYBOARD') {
          const storyboard = state.storyboards.find(a => a.id === id);
          if (storyboard) {
            storyboard.deleteLoading = true;
          }
          const archivedStoryboard = state.archivedStoryboards.find(
            a => a.id === id,
          );
          if (archivedStoryboard) {
            archivedStoryboard.deleteLoading = true;
          }
        } else {
          const folder = state.vizs.find(v => v.id === id);
          if (folder) {
            folder.deleteLoading = true;
          }
        }
      }
    });
    builder.addCase(deleteViz.fulfilled, (state, action) => {
      const {
        params: { id, archive },
        type,
      } = action.meta.arg;
      if (archive) {
        if (type === 'STORYBOARD') {
          state.storyboards = state.storyboards.filter(s => s.id !== id);
        } else {
          state.vizs = state.vizs.filter(v => v.relId !== id);
        }
      } else {
        if (type === 'DASHBOARD') {
          state.archivedDashboards = state.archivedDashboards.filter(
            a => a.id !== id,
          );
        } else if (type === 'DATACHART') {
          state.archivedDatacharts = state.archivedDatacharts.filter(
            a => a.id !== id,
          );
        } else if (type === 'STORYBOARD') {
          state.storyboards = state.storyboards.filter(a => a.id !== id);
          state.archivedStoryboards = state.archivedStoryboards.filter(
            a => a.id !== id,
          );
        } else {
          state.vizs = state.vizs.filter(v => v.id !== id);
        }
      }
    });
    builder.addCase(deleteViz.rejected, (state, action) => {
      const {
        params: { id, archive },
        type,
      } = action.meta.arg;
      if (archive) {
        if (type === 'STORYBOARD') {
          const storyboard = state.storyboards.find(s => s.id === id);
          if (storyboard) {
            storyboard.deleteLoading = false;
          }
        } else {
          const viz = state.vizs.find(v => v.relId === id);
          if (viz) {
            viz.deleteLoading = false;
          }
        }
      } else {
        if (type === 'DASHBOARD') {
          const dashboard = state.archivedDashboards.find(a => a.id === id);
          if (dashboard) {
            dashboard.deleteLoading = false;
          }
        } else if (type === 'DATACHART') {
          const datachart = state.archivedDatacharts.find(a => a.id === id);
          if (datachart) {
            datachart.deleteLoading = false;
          }
        } else if (type === 'STORYBOARD') {
          const storyboard = state.storyboards.find(s => s.id === id);
          if (storyboard) {
            storyboard.deleteLoading = false;
          }
          const archivedStoryboard = state.archivedStoryboards.find(
            a => a.id === id,
          );
          if (archivedStoryboard) {
            archivedStoryboard.deleteLoading = false;
          }
        } else {
          const folder = state.vizs.find(v => v.id === id);
          if (folder) {
            folder.deleteLoading = false;
          }
        }
      }
    });

    // Copy Dashboard
    builder.addCase(saveAsDashboard.pending, state => {
      state.saveFolderLoading = true;
    });
    builder.addCase(saveAsDashboard.fulfilled, (state, action) => {
      state.saveFolderLoading = false;
      state.vizs.push({ ...action.payload, deleteLoading: false });
    });
    builder.addCase(saveAsDashboard.rejected, state => {
      state.saveFolderLoading = false;
    });

    // addStoryboard
    builder.addCase(addStoryboard.pending, state => {
      state.saveStoryboardLoading = true;
    });
    builder.addCase(addStoryboard.fulfilled, (state, action) => {
      state.saveStoryboardLoading = false;
      state.storyboards.push({ ...action.payload, deleteLoading: false });
    });
    builder.addCase(addStoryboard.rejected, state => {
      state.saveStoryboardLoading = false;
    });

    // editStoryboard
    builder.addCase(editStoryboard.pending, state => {
      state.saveStoryboardLoading = true;
    });
    builder.addCase(editStoryboard.fulfilled, (state, action) => {
      state.saveStoryboardLoading = false;
      state.storyboards = state.storyboards.map(s =>
        s.id === action.meta.arg.storyboard.id
          ? { ...s, ...action.payload, deleteLoading: false }
          : s,
      );
    });
    builder.addCase(editStoryboard.rejected, state => {
      state.saveStoryboardLoading = false;
    });

    // chart preview
    builder.addCase(initChartPreviewData.fulfilled, (state, action) => {
      const index = state.chartPreviews?.findIndex(
        c => c.backendChartId === action.payload?.backendChartId,
      );
      if (index < 0) {
        return;
      }
      state.chartPreviews[index] = {
        ...state.chartPreviews[index],
        version: uuidv4(),
      };
    });
    builder.addCase(fetchVizChartAction.fulfilled, (state, action) => {
      const newChartDto = CloneValueDeep(action.payload.data);
      const jumpFilterParams = action.payload.jumpFilterParams;
      const filterSearchParams = action.payload.filterSearchParams;
      const index = state.chartPreviews?.findIndex(
        c => c.backendChartId === newChartDto?.id,
      );

      const currentChart = ChartManager.instance().getById(
        newChartDto?.config?.chartGraphId,
      );

      if (index < 0) {
        state.chartPreviews.push({
          backendChartId: newChartDto?.id,
          backendChart: newChartDto,
          chartConfig: currentChart
            ? transferChartConfig(
                mergeToChartConfig(
                  currentChart?.config,
                  migrateChartConfig(newChartDto?.config),
                ),
                filterSearchParams,
                false,
                jumpFilterParams,
              )
            : undefined,
        });
      } else {
        const prevChartPreview = state.chartPreviews[index];
        state.chartPreviews[index] = {
          ...prevChartPreview,
          backendChart: newChartDto,
          chartConfig: transferChartConfig(
            mergeToChartConfig(
              currentChart?.config,
              migrateChartConfig(newChartDto?.config),
            ),
            filterSearchParams,
            false,
            jumpFilterParams,
          ),
        };
      }
    });
    builder.addCase(
      fetchDataSetByPreviewChartAction.pending,
      (state, action: { meta: { arg: { backendChartId: string } } }) => {
        const index = state.chartPreviews?.findIndex(
          c => c.backendChartId === action?.meta?.arg?.backendChartId,
        );
        if (index !== undefined) {
          state.chartPreviews[index].isLoadingData = true;
          state.chartPreviews[index].version = uuidv4();
        }
      },
    );
    builder.addCase(
      fetchDataSetByPreviewChartAction.rejected,
      (state, action: { meta: { arg: { backendChartId: string } } }) => {
        const index = state.chartPreviews?.findIndex(
          c => c.backendChartId === action?.meta?.arg?.backendChartId,
        );
        if (index !== undefined) {
          state.chartPreviews[index].isLoadingData = false;
          state.chartPreviews[index].version = uuidv4();
        }
      },
    );
    builder.addCase(
      fetchDataSetByPreviewChartAction.fulfilled,
      (state, action: PayloadAction<{ backendChartId; data }>) => {
        const index = state.chartPreviews?.findIndex(
          c => c.backendChartId === action.payload?.backendChartId,
        );
        state.selectedItems[action.payload?.backendChartId] = [];
        if (index < 0) {
          state.chartPreviews.push({
            backendChartId: action.payload?.backendChartId,
            isLoadingData: false,
            dataset: action.payload?.data,
          });
          return;
        }
        state.chartPreviews[index] = {
          ...state.chartPreviews[index],
          dataset: action.payload?.data,
          isLoadingData: false,
          version: uuidv4(),
        };
      },
    );
    builder.addCase(updateFilterAndFetchDataset.fulfilled, (state, action) => {
      const index = state.chartPreviews?.findIndex(
        c => c.backendChartId === action.payload?.backendChartId,
      );
      if (index < 0) {
        return;
      }
      state.chartPreviews[index] = {
        ...state.chartPreviews[index],
        version: uuidv4(),
      };
    });
    builder.addCase(updateGroupAndFetchDataset.fulfilled, (state, action) => {
      const index = state.chartPreviews?.findIndex(
        c => c.backendChartId === action.payload?.backendChartId,
      );
      if (index < 0) {
        return;
      }
      state.chartPreviews[index] = {
        ...state.chartPreviews[index],
        version: uuidv4(),
      };
    });
  },
});

export const { actions: vizActions, reducer } = slice;

export const useVizSlice = () => {
  useInjectReducer({ key: slice.name, reducer: slice.reducer });
  return { actions: slice.actions };
};
