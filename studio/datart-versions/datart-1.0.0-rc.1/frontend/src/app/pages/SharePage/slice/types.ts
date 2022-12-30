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
import { ServerDashboard } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import {
  ChartPreview,
  VizType,
} from 'app/pages/MainPage/pages/VizPage/slice/types';
import { ServerStoryBoard } from 'app/pages/StoryBoardPage/slice/types';
import { SelectedItem } from 'app/types/ChartConfig';
import { ChartDTO } from 'app/types/ChartDTO';

export interface SharePageState {
  needVerify?: boolean;
  vizType?: VizType;
  shareToken: string;
  executeToken?: string;
  executeTokenMap: Record<string, ExecuteToken>;
  subVizTokenMap?: Record<string, ExecuteToken>;
  sharePassword?: string;
  chartPreview?: ChartPreview;
  headlessBrowserRenderSign: boolean;
  pageWidthHeight: [number, number];
  shareDownloadPolling: boolean;
  loginLoading: boolean;
  oauth2Clients: Array<{ name: string; value: string }>;
  availableSourceFunctions?: string[];
  selectedItems: SelectedItem[];
  title?: string;
}
export interface ShareVizInfo {
  vizType: VizType | undefined;
  vizDetail: ChartDTO | ServerDashboard | ServerStoryBoard;
  download: boolean;
  executeToken: Record<string, ExecuteToken>;
  subVizToken: null | Record<string, ExecuteToken>;
  shareToken: Record<string, ExecuteToken>;
}

export interface ShareExecuteParams {
  executeToken: string | undefined;
  password: string | undefined;
}
export interface ExecuteToken {
  authorizedToken: string;
}
