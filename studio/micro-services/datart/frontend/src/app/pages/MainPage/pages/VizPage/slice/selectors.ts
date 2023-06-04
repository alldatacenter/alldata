import { createSelector } from '@reduxjs/toolkit';
import { RootState } from 'types';
import { listToTree } from 'utils/utils';
import { initialState } from '.';
import { VizResourceSubTypes } from '../../PermissionPage/constants';
import {
  SelectArchivedTree,
  SelectStoryboardTree,
  SelectVizFolderTree,
  SelectVizStoryboardTree,
  SelectVizTree,
} from './types';

const selectDomain = (state: RootState) => state.viz || initialState;

export const selectVizs = createSelector(
  [selectDomain],
  vizState => vizState.vizs,
);

export const makeSelectVizTree = () =>
  createSelector(
    [
      selectVizs,
      (_, props: SelectVizTree) => props.getIcon,
      (_, props: SelectVizTree) => props.getDisabled,
    ],
    (vizs, getIcon, getDisabled) => {
      return listToTree(
        vizs.map(v => ({ ...v, isFolder: v.relType === 'FOLDER' })),
        null,
        [VizResourceSubTypes.Folder],
        { getIcon, getDisabled },
      );
    },
  );

export const makeSelectVizFolderTree = () =>
  createSelector(
    [
      selectVizs,
      (_, props: SelectVizFolderTree) => props.id,
      (_, props: SelectVizFolderTree) => props.getDisabled,
    ],
    (vizs, id, getDisabled) =>
      listToTree(
        vizs &&
          vizs
            .filter(v => v.relType === 'FOLDER' && v.id !== id)
            .map(v => ({ ...v, isFolder: true })),
        null,
        [VizResourceSubTypes.Folder],
        { getDisabled },
      ),
  );

export const selectStoryboards = createSelector(
  [selectDomain],
  vizState => vizState.storyboards,
);

export const makeSelectStoryboradTree = () =>
  createSelector(
    [
      selectStoryboards,
      (_, props: SelectStoryboardTree) => props.getIcon,
      (_, props: SelectStoryboardTree) => props.getDisabled,
    ],
    (storyboards, getIcon, getDisabled) =>
      listToTree(storyboards, null, [VizResourceSubTypes.Storyboard], {
        getIcon,
        getDisabled,
      }),
  );

export const selectArchivedDatacharts = createSelector(
  [selectDomain],
  vizState => vizState.archivedDatacharts,
);

export const selectArchivedDashboards = createSelector(
  [selectDomain],
  vizState => vizState.archivedDashboards,
);

export const selectArchivedStoryboards = createSelector(
  [selectDomain],
  vizState => vizState.archivedStoryboards,
);

export const makeSelectArchivedStoryboradsTree = () =>
  createSelector(
    [
      selectArchivedStoryboards,
      (_, props: SelectArchivedTree) => props.getDisabled,
    ],
    (archivedStoryboards, getDisabled) =>
      listToTree(archivedStoryboards, null, [VizResourceSubTypes.Storyboard], {
        getDisabled,
      }),
  );
export const makeSelectArchivedDashboardsTree = () =>
  createSelector(
    [
      selectArchivedDashboards,
      (_, props: SelectArchivedTree) => props.getDisabled,
    ],
    (archivedDashboards, getDisabled) =>
      listToTree(archivedDashboards, null, [VizResourceSubTypes.Storyboard], {
        getDisabled,
      }),
  );
export const makeSelectArchivedDatachartsTree = () =>
  createSelector(
    [
      selectArchivedDatacharts,
      (_, props: SelectArchivedTree) => props.getDisabled,
    ],
    (archivedDatacharts, getDisabled) =>
      listToTree(archivedDatacharts, null, [VizResourceSubTypes.Storyboard], {
        getDisabled,
      }),
  );

export const makeSelectStoryboradFolderTree = () =>
  createSelector(
    [
      selectStoryboards,
      (_, props: SelectVizStoryboardTree) => props.id,
      (_, props: SelectVizStoryboardTree) => props.getDisabled,
    ],
    (storyboards, id, getDisabled) =>
      listToTree(
        storyboards && storyboards.filter(v => v.isFolder && v.id !== id),
        null,
        [VizResourceSubTypes.Storyboard],
        { getDisabled },
      ),
  );

export const selectVizListLoading = createSelector(
  [selectDomain],
  vizState => vizState.vizListLoading,
);

export const selectStoryboardListLoading = createSelector(
  [selectDomain],
  vizState => vizState.storyboardListLoading,
);

export const selectSaveFolderLoading = createSelector(
  [selectDomain],
  vizState => vizState.saveFolderLoading,
);

export const selectSaveStoryboardLoading = createSelector(
  [selectDomain],
  vizState => vizState.saveStoryboardLoading,
);

export const selectPublishLoading = createSelector(
  [selectDomain],
  vizState => vizState.publishLoading,
);

export const selectArchivedDatachartLoading = createSelector(
  [selectDomain],
  vizState => vizState.archivedDatachartLoading,
);

export const selectArchivedDashboardLoading = createSelector(
  [selectDomain],
  vizState => vizState.archivedDashboardLoading,
);

export const selectArchivedStoryboardLoading = createSelector(
  [selectDomain],
  vizState => vizState.archivedStoryboardLoading,
);

export const selectTabs = createSelector(
  [selectDomain],
  vizState => vizState.tabs,
);

export const selectSelectedTab = createSelector([selectDomain], vizState =>
  vizState.tabs.find(t => t.id === vizState.selectedTab),
);

export const selectPreviewCharts = createSelector(
  [selectDomain],
  vizState => vizState.chartPreviews,
);

export const selectHasVizFetched = createSelector(
  [selectDomain],
  vizState => vizState.hasVizFetched,
);

export const selectSelectedItems = createSelector(
  [selectDomain],
  vizState => vizState.selectedItems,
);
