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
import { PayloadAction } from '@reduxjs/toolkit';
import { useInjectReducer } from 'utils/@reduxjs/injectReducer';
import { createSlice } from 'utils/@reduxjs/toolkit';
import { getInitStoryPageInfo } from './../utils';
import { StoryBoard, StoryBoardState, StoryPage, StoryPageInfo } from './types';

export const storyBoardInit: StoryBoardState = {
  storyMap: {},
  storyPageMap: {},
  storyPageInfoMap: {},
};
// storyActions
const storyBoardSlice = createSlice({
  name: 'storyBoard',
  initialState: storyBoardInit,
  reducers: {
    setStoryBoard(state, action: PayloadAction<StoryBoard>) {
      const storyBoard = action.payload;
      state.storyMap[storyBoard.id] = storyBoard;
    },
    setStoryPageMap(
      state,
      action: PayloadAction<{
        storyId: string;
        storyPageMap: Record<string, StoryPage>;
      }>,
    ) {
      const { storyId, storyPageMap } = action.payload;
      state.storyPageMap[storyId] = storyPageMap;
    },
    setStoryPageInfoMap(
      state,
      action: PayloadAction<{
        storyId: string;
        storyPageInfoMap: Record<string, StoryPageInfo>;
      }>,
    ) {
      const { storyId, storyPageInfoMap } = action.payload;
      state.storyPageInfoMap[storyId] = storyPageInfoMap;
    },
    changePageSelected(
      state,
      action: PayloadAction<{
        storyId: string;
        pageId: string;
        multiple: boolean;
      }>,
    ) {
      const { storyId, pageId, multiple } = action.payload;
      if (!storyId) return;
      if (!pageId) return;
      if (!multiple) {
        //单选
        Object.keys(state.storyPageInfoMap[storyId]).forEach(pageId => {
          state.storyPageInfoMap[storyId][pageId].selected = false;
        });
        if (
          state.storyPageInfoMap[storyId] &&
          state.storyPageInfoMap[storyId][pageId]
        ) {
          state.storyPageInfoMap[storyId][pageId].selected = true;
        }
      } else {
        // 多选
        if (
          state.storyPageInfoMap[storyId] &&
          state.storyPageInfoMap[storyId][pageId]
        ) {
          const oldSelected = state.storyPageInfoMap[storyId][pageId].selected;
          state.storyPageInfoMap[storyId][pageId].selected = !oldSelected;

          const selectedPages = Object.values(
            state.storyPageInfoMap[storyId],
          ).filter(page => page.selected);

          if (selectedPages.length === 0) {
            state.storyPageInfoMap[storyId][pageId].selected = true;
          }
        }
      }
    },
    clearPageSelected(state, action: PayloadAction<string>) {
      const storyId = action.payload;
      Object.keys(state.storyPageInfoMap[storyId]).forEach(pageId => {
        state.storyPageInfoMap[storyId][pageId].selected = false;
      });
    },
    // setStoryPageInfoMap
    deleteStoryPages(
      state,
      action: PayloadAction<{ storyId: string; pageIds: string[] }>,
    ) {
      const { storyId, pageIds } = action.payload;
      pageIds.forEach(id => {
        delete state.storyPageMap[storyId][id];
        delete state.storyPageInfoMap[storyId][id];
      });
    },
    addStoryPage(state, action: PayloadAction<StoryPage>) {
      const storyPage = action.payload;
      state.storyPageInfoMap[storyPage.storyboardId][storyPage.id] =
        getInitStoryPageInfo(storyPage.id);
      state.storyPageMap[storyPage.storyboardId][storyPage.id] = storyPage;
    },
    updateStoryPage(state, action: PayloadAction<StoryPage>) {
      const storyPage = action.payload;
      state.storyPageMap[storyPage.storyboardId][storyPage.id] = storyPage;
    },
    updateStoryPageNameAndThumbnail(
      state,
      action: PayloadAction<{
        storyId: string;
        pageId: string;
        name: string;
        thumbnail: string;
      }>,
    ) {
      const { storyId, pageId, name, thumbnail } = action.payload;
      state.storyPageMap[storyId][pageId].config.name = name || '';
      state.storyPageMap[storyId][pageId].config.thumbnail = thumbnail || '';
    },
    updateStory(state, action: PayloadAction<StoryBoard>) {
      const storyBoard = action.payload;
      state.storyMap[storyBoard.id] = storyBoard;
    },
    changeBoardPublish(
      state,
      action: PayloadAction<{ stroyId: string; publish: number }>,
    ) {
      const { stroyId, publish } = action.payload;
      // 1 发布了， 2 取消发布
      state.storyMap[stroyId].status = publish;
    },
  },
  extraReducers: builder => {},
});
export const { actions: storyActions } = storyBoardSlice;
export const useStoryBoardSlice = () => {
  useInjectReducer({
    key: 'storyBoard',
    reducer: storyBoardSlice.reducer,
  });
};
