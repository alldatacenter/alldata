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
import {
  deleteViz,
  removeTab,
} from 'app/pages/MainPage/pages/VizPage/slice/thunks';
import { VizType } from 'app/pages/MainPage/pages/VizPage/slice/types';
import { useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';

export const useRecycleViz = (orgId: string, vizId: string, type: VizType) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const tg = useI18NPrefix('global');
  const redirect = useCallback(
    tabKey => {
      if (tabKey) {
        history.push(`/organizations/${orgId}/vizs/${tabKey}`);
      } else {
        history.push(`/organizations/${orgId}/vizs`);
      }
    },
    [history, orgId],
  );
  const recycleViz = useCallback(() => {
    dispatch(
      deleteViz({
        params: { id: vizId, archive: true },
        type: type,
        resolve: () => {
          message.success(tg('operation.archiveSuccess'));
          dispatch(removeTab({ id: vizId, resolve: redirect }));
        },
      }),
    );
  }, [dispatch, vizId, type, tg, redirect]);
  return recycleViz;
};
