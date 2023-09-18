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
import { CommonFormTypes } from 'globalConstants';
import { useCallback, useContext } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { request2 } from 'utils/request';
import { getInsertedNodeIndex } from 'utils/utils';
import { SaveFormContext, SaveFormModel } from '../SaveFormContext';
import { selectVizs } from '../slice/selectors';
import { addViz, saveAsDashboard } from '../slice/thunks';
import { VizType } from '../slice/types';

export function useSaveAsViz() {
  const { showSaveForm } = useContext(SaveFormContext);
  const vizsData = useSelector(selectVizs);
  const dispatch = useDispatch();
  const tg = useI18NPrefix('global');

  const getVizDetail = useCallback(
    async (backendChartId: string, type: string) => {
      const { data } = await request2<any>(
        {
          method: 'GET',
          url: `viz/${type.toLowerCase()}s/${backendChartId}`,
        },
        undefined,
        {
          onRejected(error) {
            return {} as any;
          },
        },
      );
      return data;
    },
    [],
  );

  const saveAsViz = useCallback(
    async (vizId: string, type: VizType) => {
      let vizData = await getVizDetail(vizId, type).then(data => {
        return data;
      });
      const boardType = JSON.parse(vizData.config || '{}')?.type;

      showSaveForm({
        vizType: type,
        type: CommonFormTypes.SaveAs,
        visible: true,
        initialValues: {
          ...vizData,
          parentId: vizData.parentId || void 0,
          name: vizData.name + '_' + tg('copy'),
          boardType: boardType,
        },
        onSave: async (values: SaveFormModel, onClose) => {
          let index = getInsertedNodeIndex(values, vizsData);
          let requestData: any = [];

          if (type === 'DATACHART') {
            requestData = Object.assign({}, vizData, {
              ...values,
              parentId: values.parentId || null,
              index,
              avatar: JSON.parse(vizData.config)?.chartGraphId,
            });

            await dispatch(
              addViz({
                viz: requestData,
                type,
              }),
            );
            onClose?.();
          } else {
            requestData = {
              config: vizData.config,
              id: vizData.id,
              index,
              name: values.name,
              orgId: vizData.orgId,
              parentId: values.parentId || null,
              permissions: vizData.permissions,
              subType: boardType,
              boardType: boardType,
            };

            await dispatch(
              saveAsDashboard({
                viz: requestData,
                dashboardId: vizData.id,
              }),
            );
            onClose?.();
          }
        },
      });
    },
    [showSaveForm, tg, vizsData, dispatch, getVizDetail],
  );

  return saveAsViz;
}
