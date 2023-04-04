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
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { migrateViewConfig } from 'app/migration/ViewConfig/migrationViewDetailConfig';
import { CommonFormTypes } from 'globalConstants';
import { useCallback, useContext } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { request2 } from 'utils/request';
import { errorHandle, getInsertedNodeIndex } from 'utils/utils';
import { View } from '../../../../../types/View';
import { SaveFormContext } from '../SaveFormContext';
import {
  selectAllSourceDatabaseSchemas,
  selectCurrentEditingViewAttr,
  selectViews,
} from '../slice/selectors';
import { saveView } from '../slice/thunks';
import { ViewViewModel } from '../slice/types';
import { transformModelToViewModel } from '../utils';

export function useSaveAsView() {
  const t = useI18NPrefix('view.editor');
  const tg = useI18NPrefix('global');
  const { showSaveForm } = useContext(SaveFormContext);
  const dispatch = useDispatch();

  const viewsData = useSelector(selectViews);
  const allDatabaseSchemas = useSelector(selectAllSourceDatabaseSchemas);
  const sourceId = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'sourceId' }),
  ) as string;

  const getViewData = useCallback(async (viewId): Promise<View> => {
    try {
      const { data } = await request2<View>(`/views/${viewId}`);
      data.config = migrateViewConfig(data.config);
      return data;
    } catch (error) {
      errorHandle(error);
      return {} as View;
    }
  }, []);

  const saveAsView = useCallback(
    async (viewId: string) => {
      let viewData: ViewViewModel = await getViewData(viewId).then(data => {
        return transformModelToViewModel(data, allDatabaseSchemas[sourceId]);
      });

      const { name, parentId, config } = viewData;

      showSaveForm({
        type: CommonFormTypes.SaveAs,
        visible: true,
        initialValues: {
          name: name + '_' + tg('copy'),
          parentId,
          config,
        },
        parentIdLabel: t('folder'),
        onSave: (values, onClose) => {
          let index = getInsertedNodeIndex(values, viewsData);

          viewData = Object.assign({}, viewData, {
            ...values,
            parentId: values.parentId || null,
            index,
            previewResults: [],
          });

          dispatch(
            saveView({
              resolve: onClose,
              isSaveAs: true,
              currentView: viewData,
            }),
          );
        },
      });
    },
    [
      dispatch,
      getViewData,
      showSaveForm,
      t,
      tg,
      viewsData,
      allDatabaseSchemas,
      sourceId,
    ],
  );
  return saveAsView;
}
