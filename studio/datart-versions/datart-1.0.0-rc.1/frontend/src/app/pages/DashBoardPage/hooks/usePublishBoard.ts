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

import { message } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { publishViz } from 'app/pages/MainPage/pages/VizPage/slice/thunks';
import { VizType } from 'app/pages/MainPage/pages/VizPage/slice/types';
import { storyActions } from 'app/pages/StoryBoardPage/slice';
import { useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { boardActions } from '../pages/Board/slice';
export const usePublishBoard = (
  vizId: string,
  type: VizType,
  status: number,
) => {
  const dispatch = useDispatch();
  const t = useI18NPrefix('viz.main');
  const handlePublish = useCallback(
    callback => {
      dispatch(
        publishViz({
          id: vizId,
          vizType: type,
          publish: status === 1 ? true : false,
          resolve: () => {
            message.success(
              `${status === 2 ? t('unpublishSuccess') : t('publishSuccess')}`,
            );
            callback();
          },
        }),
      );
    },
    [dispatch, status, t, type, vizId],
  );
  const publishBoard = useCallback(() => {
    const cb = () => {
      dispatch(
        boardActions.changeBoardPublish({
          boardId: vizId,
          publish: status === 1 ? 2 : 1,
        }),
      );
    };
    handlePublish(cb);
  }, [handlePublish, dispatch, vizId, status]);
  const publishStory = useCallback(() => {
    const cb = () => {
      dispatch(
        storyActions.changeBoardPublish({
          stroyId: vizId,
          publish: status === 1 ? 2 : 1,
        }),
      );
    };
    handlePublish(cb);
  }, [handlePublish, dispatch, vizId, status]);
  return {
    publishBoard,
    publishStory,
  };
};
