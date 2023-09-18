import { TreeNodeProps } from 'antd';
import { ChartConfig, SelectedItem } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { ChartDTO } from 'app/types/ChartDTO';
import { ReactElement } from 'react';

export type VizType = [
  'DATACHART',
  'DASHBOARD',
  'FOLDER',
  'STORYBOARD',
  'TEMPLATE',
][number];

export interface VizState {
  vizs: FolderViewModel[];
  hasVizFetched: boolean;
  storyboards: StoryboardViewModel[];
  vizListLoading: boolean;
  storyboardListLoading: boolean;
  saveFolderLoading: boolean;
  saveStoryboardLoading: boolean;
  publishLoading: boolean;
  archivedDatacharts: ArchivedViz[];
  archivedDashboards: ArchivedViz[];
  archivedStoryboards: ArchivedViz[];
  archivedDatachartLoading: boolean;
  archivedDashboardLoading: boolean;
  archivedStoryboardLoading: boolean;
  tabs: VizTab[];
  selectedTab: string;
  dataChartListLoading: boolean;
  chartPreviews: ChartPreview[];
  selectedItems: Record<string, SelectedItem[]>;
}

export interface Folder {
  createBy: string;
  createTime: string;
  id: string;
  index: number;
  name: string;
  orgId: string;
  parentId: string | null;
  relId: string;
  relType: VizType;
  status: number;
  updateBy: string;
  updateTime: string;
}

export interface FolderViewModel extends Folder {
  deleteLoading: boolean;
}

export interface Storyboard {
  config?: string;
  createBy?: string;
  createTime?: string;
  id: string;
  name: string;
  orgId: string;
  parentId: string | null;
  permission?: number;
  status?: number;
  updateBy?: string;
  updateTime?: string;
}

export interface StoryboardSimple extends Storyboard {
  isFolder: boolean;
  index: number | null;
}

export interface StoryboardViewModel extends StoryboardSimple {
  deleteLoading: boolean;
}

export interface SelectStoryboardTree {
  getIcon: (
    o: StoryboardViewModel,
  ) => ReactElement | ((props: TreeNodeProps) => ReactElement);
  getDisabled?: (o: StoryboardViewModel) => boolean;
}

export interface SelectVizStoryboardTree {
  id?: string;
  getDisabled: (o: StoryboardViewModel, path: string[]) => boolean;
}

export interface SelectArchivedTree {
  getDisabled?: (o: ArchivedViz) => boolean;
}

export interface ArchivedViz {
  id: string;
  name: string;
  vizType: VizType;
  deleteLoading: boolean;
  parentId: string | null;
  isFolder: boolean;
  index: number | null;
}

export interface ChartPreview {
  version?: string;
  backendChartId?: string;
  backendChart?: ChartDTO;
  dataset?: ChartDataSetDTO;
  chartConfig?: ChartConfig;
  isLoadingData?: boolean;
}

export interface VizTab {
  id: string;
  name: string;
  type: VizType;
  parentId: string | null;
  permissionId?: string;
  search?: string;
}

export interface AddVizParams {
  viz: {
    name: string;
    index: number | null;
    description?: string;
    parentId?: string | null;
    orgId: string;
    file?: FormData;
  };
  type: VizType;
}

export interface EditFolderParams {
  folder: Partial<FolderViewModel>;
  resolve: () => void;
}

export interface UnarchiveVizParams {
  params: {
    id: string;
    name: string;
    vizType: VizType;
    parentId: string | null;
    index: number | null;
  };
  resolve: () => void;
}

export interface DeleteVizParams {
  params: {
    id: string;
    archive: boolean;
  };
  type: VizType;
  resolve: () => void;
}

export interface PublishVizParams {
  id: string;
  vizType: VizType;
  publish: boolean;
  resolve: () => void;
}

export interface StoryboardBase {
  index: number;
  name: string;
  parentId: string | null;
  id: string;
}

export interface AddStoryboardParams {
  storyboard: Pick<
    StoryboardSimple,
    'name' | 'parentId' | 'index' | 'config' | 'orgId' | 'isFolder'
  >;
  resolve: () => void;
}

export interface EditStoryboardParams {
  storyboard: StoryboardBase;
  resolve: () => void;
}

export interface DeleteStoryboardParams {
  id: string;
  archive: boolean;
  resolve: () => void;
}

export interface FilterSearchParams {
  [k: string]: string[];
}
export interface FilterSearchParamsWithMatch {
  params?: FilterSearchParams;
  isMatchByName?: boolean;
}

export interface SelectVizTree {
  getIcon: (
    o: FolderViewModel,
  ) => ReactElement | ((props: TreeNodeProps) => ReactElement);
  getDisabled?: (o: FolderViewModel) => boolean;
}

export interface SelectVizFolderTree {
  id?: string;
  getDisabled: (o: FolderViewModel, path: string[]) => boolean;
}

export interface SaveAsDashboardParams {
  viz: {
    id: string;
    name: string;
    index: number | null;
    config: string;
    parentId?: string | null;
  };
  dashboardId?: string;
}
