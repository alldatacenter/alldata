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
import { createSelector } from '@reduxjs/toolkit';
import { RootState } from 'types';
import { storyBoardInit } from '.';

const selectPropsId = (_: unknown, id: string) => id;

export const storyBoardState = (state: RootState) =>
  state.storyBoard || storyBoardInit;

export const selectStoryBoardMap = createSelector(
  [storyBoardState],
  state => state.storyMap,
);
export const selectStoryPageMap = createSelector(
  [storyBoardState],
  state => state.storyPageMap,
);
export const selectStoryPageInfoMap = createSelector(
  [storyBoardState],
  state => state.storyPageInfoMap,
);

export const makeSelectStoryBoardById = createSelector(
  selectStoryBoardMap,
  selectPropsId,
  (selectStoryBoardRecord, id) => selectStoryBoardRecord[id],
);

export const makeSelectStoryPagesById = createSelector(
  selectStoryPageMap,
  selectPropsId,
  (selectStoryPageRecord, id) => selectStoryPageRecord[id] || {},
);

export const selectStoryPageInfoById = createSelector(
  selectStoryPageInfoMap,
  selectPropsId,
  (pageInfoMap, id) => pageInfoMap[id] || {},
);

export const selectSelectedPageIds = createSelector(
  storyBoardState,
  selectPropsId,
  (BoardState, id) => {
    const pageMap = BoardState.storyPageInfoMap[id] || {};
    return Object.values(pageMap)
      .filter(info => info.selected)
      .map(info => info.id);
  },
);

export const selectShareStoryBoard = createSelector(
  storyBoardState,
  storyState => {
    return Object.values(storyState.storyMap)[0] || undefined;
  },
);
