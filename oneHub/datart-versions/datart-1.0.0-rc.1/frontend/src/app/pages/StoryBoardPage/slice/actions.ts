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
import { VizRenderMode } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { storyActions } from '.';
import {
  formatStory,
  getInitStoryPageInfoMap,
  getStoryPage,
  getStoryPageMap,
} from '../utils';
import { ServerStoryBoard } from './types';

export const handleServerStoryAction =
  (params: {
    data: ServerStoryBoard;
    renderMode: VizRenderMode;
    storyId: string;
  }) =>
  async dispatch => {
    const { data, storyId } = params;
    let story = formatStory(data);
    const pages = getStoryPage(data.storypages || []);
    const storyPageMap = getStoryPageMap(pages);
    const storyPageInfoMap = getInitStoryPageInfoMap(pages);
    dispatch(storyActions.setStoryBoard(story));
    dispatch(storyActions.setStoryPageInfoMap({ storyId, storyPageInfoMap }));
    dispatch(storyActions.setStoryPageMap({ storyId, storyPageMap }));
  };
