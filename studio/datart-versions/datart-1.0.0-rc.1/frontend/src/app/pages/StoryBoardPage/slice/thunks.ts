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
import { migrateStoryPageConfig } from 'app/migration/StoryConfig/migrateStoryPageConfig';
import { getBoardDetail } from 'app/pages/DashBoardPage/pages/Board/slice/thunk';
import { selectVizs } from 'app/pages/MainPage/pages/VizPage/slice/selectors';
import { ExecuteToken } from 'app/pages/SharePage/slice/types';
import { updateBy } from 'app/utils/mutation';
import { RootState } from 'types';
import { isEmpty } from 'utils/object';
import { request2 } from 'utils/request';
import { storyActions } from '.';
import { getInitStoryPageConfig } from '../utils';
import { handleServerStoryAction } from './actions';
import { makeSelectStoryPagesById } from './selectors';
import {
  ServerStoryBoard,
  StoryBoard,
  StoryPage,
  StoryPageOfServer,
  StoryPageRelType,
} from './types';

export const getStoryDetail = createAsyncThunk<null, string>(
  'storyBoard/getStoryDetail',
  async (storyId, { getState, dispatch }) => {
    if (!storyId) {
      return null;
    }
    await dispatch(fetchStoryDetail(storyId));
    return null;
  },
);
export const fetchStoryDetail = createAsyncThunk<undefined, string>(
  'storyBoard/fetchStoryDetail',
  async (storyId, { getState, dispatch }) => {
    if (!storyId) {
      return;
    }
    const { data } = await request2<ServerStoryBoard>({
      url: `viz/storyboards/${storyId}`,
      method: 'get',
    });
    dispatch(
      handleServerStoryAction({
        data,
        renderMode: 'read',
        storyId,
      }),
    );
    return undefined;
  },
);
export const getPageContentDetail = createAsyncThunk<
  null,
  {
    relId: string;
    relType: StoryPageRelType;
    vizToken?: ExecuteToken;
    shareToken?: string;
  }
>(
  'storyBoard/getPageContentDetail',
  async ({ relId, relType, vizToken, shareToken }, { getState, dispatch }) => {
    if (!relId) {
      return null;
    }
    if (relType === 'DASHBOARD') {
      dispatch(getBoardDetail({ dashboardRelId: relId, vizToken, shareToken }));
    }
    if (relType === 'DATACHART') {
      // TODO
      // dispatch(getBoardDetail(relId));
    }
    return null;
  },
);
// addPages
export const addStoryPages = createAsyncThunk<
  null,
  { storyId: string; relIds: string[] },
  { state: RootState }
>(
  'storyBoard/addStoryPages',
  async ({ storyId, relIds }, { getState, dispatch }) => {
    const rootState = getState();
    const vizs = selectVizs(rootState);
    const pageMap = makeSelectStoryPagesById(rootState, storyId);
    const pageIndexArr = Object.values(pageMap).map(
      page => page.config.index || -1,
    );
    let maxIndex = pageIndexArr.length ? Math.max(...pageIndexArr) : -1;
    relIds.forEach(async relId => {
      maxIndex++;
      const viz = vizs.find(viz => viz.relId === relId);
      if (viz) {
        const { relType } = viz;
        let pageConfig = getInitStoryPageConfig(maxIndex);
        if (relType === 'DASHBOARD') {
          pageConfig.name = viz?.name;
          // TODO
          // pageConfig.thumbnail = viz?.thumbnail;
        }
        const newPage: StoryPageOfServer = {
          id: '',
          config: JSON.stringify(pageConfig),
          relId,
          relType: relType as StoryPageRelType,
          storyboardId: storyId,
        };
        dispatch(addStoryPage(newPage));
      }
    }, []);
    return null;
  },
);
// addPage
export const addStoryPage = createAsyncThunk<
  null,
  StoryPageOfServer,
  { state: RootState }
>('storyBoard/addStoryPage', async (pageOfServer, { getState, dispatch }) => {
  const { data } = await request2<StoryPageOfServer>({
    url: `viz/storypages`,
    method: 'post',
    data: pageOfServer,
  });
  const page = {
    ...data,
    config: migrateStoryPageConfig(data.config),
  } as StoryPage;
  dispatch(storyActions.addStoryPage(page));
  return null;
});

export const deleteStoryPage = createAsyncThunk<
  null,
  { storyId: string; pageId: string },
  { state: RootState }
>(
  'storyBoard/deleteStoryPage',
  async ({ storyId, pageId }, { getState, dispatch }) => {
    const { data } = await request2<boolean>({
      url: `viz/storypages/${pageId}`,
      method: 'delete',
    });
    if (data) {
      dispatch(storyActions.deleteStoryPages({ storyId, pageIds: [pageId] }));
    }
    return null;
  },
);
export const updateStoryPage = createAsyncThunk<
  null,
  { storyId: string; storyPage: StoryPage },
  { state: RootState }
>(
  'storyBoard/updateStoryPage',
  async ({ storyId, storyPage }, { getState, dispatch }) => {
    const { data } = await request2<boolean>({
      url: `/viz/storypages/${storyId}`,
      method: 'put',
      data: { ...storyPage, config: JSON.stringify(storyPage.config) },
    });
    if (data) {
      dispatch(storyActions.updateStoryPage(storyPage));
    }
    return null;
  },
);
export const updateStroyBoardPagesByMoveEvent = createAsyncThunk<
  null,
  { storyId: string; sortedPages: StoryPage[]; event: { dragId; dropId } },
  { state: RootState }
>(
  'storyBoard/updateStroyBoardPagesByMoveEvent',
  async ({ storyId, sortedPages, event }, { getState, dispatch }) => {
    const dropPageIndex = sortedPages?.findIndex(p => p.id === event.dropId);
    const dragPageIndex = sortedPages?.find(p => p.id === event.dragId);
    if (dragPageIndex && dropPageIndex && dropPageIndex > -1) {
      const newSortedPages = sortedPages.filter(p => p.id !== event?.dragId);
      newSortedPages.splice(dropPageIndex, 0, dragPageIndex);

      newSortedPages?.forEach((p, index) => {
        if (!isEmpty(p.config.index)) {
          const newPage = updateBy(p, draft => {
            draft.config.index = index;
          });
          dispatch(updateStoryPage({ storyId, storyPage: newPage }));
        }
      });
    }
    return null;
  },
);
export const updateStory = createAsyncThunk<
  null,
  { story: StoryBoard },
  { state: RootState }
>('storyBoard/updateStory', async ({ story }, { getState, dispatch }) => {
  const { data } = await request2<boolean>({
    url: `/viz/storyboards/${story.id}`,
    method: 'put',
    data: { ...story, config: JSON.stringify(story.config) },
  });
  if (data) {
    dispatch(storyActions.updateStory(story));
  }
  return null;
});
