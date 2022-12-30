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

import { Spin } from 'antd';
import { Split } from 'app/components';
import { useAccess, useCascadeAccess } from 'app/pages/MainPage/Access';
import debounce from 'lodash/debounce';
import React, {
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { getPath } from 'utils/utils';
import {
  PermissionLevels,
  ResourceTypes,
} from '../../PermissionPage/constants';
import SelectView from '../components/SelectViewType';
import { UNPERSISTED_ID_PREFIX } from '../constants';
import { EditorContext } from '../EditorContext';
import { useViewSlice } from '../slice';
import { selectCurrentEditingViewAttr, selectViews } from '../slice/selectors';
import { getSchemaBySourceId } from '../slice/thunks';
import { Editor } from './Editor';
import { Outputs } from './Outputs';
import { Properties } from './Properties';
import { StructView } from './StructView';

export const Workbench = memo(() => {
  const dispatch = useDispatch();
  const { editorInstance } = useContext(EditorContext);
  const { actions } = useViewSlice();

  const views = useSelector(selectViews);
  const id = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'id' }),
  ) as string;
  const parentId = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'parentId' }),
  ) as string;
  const sourceId = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'sourceId' }),
  ) as string;
  const viewType = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'type' }),
  ) as string;

  const path = useMemo(
    () =>
      views
        ? getPath(
            views as Array<{ id: string; parentId: string }>,
            { id, parentId },
            ResourceTypes.View,
          )
        : [],
    [views, id, parentId],
  );
  const managePermission = useCascadeAccess({
    module: ResourceTypes.View,
    path,
    level: PermissionLevels.Manage,
  });
  const unpersistedNewView = id.includes(UNPERSISTED_ID_PREFIX);
  const allowManage = managePermission(true) || unpersistedNewView;
  const allowEnableViz = useAccess({
    type: 'module',
    module: ResourceTypes.Viz,
    id: '',
    level: PermissionLevels.Enable,
  })(true);

  useEffect(() => {
    editorInstance?.layout();
  }, [editorInstance, allowManage]);

  /* eslint-disable-next-line */
  const onResize = useCallback(
    debounce(() => {
      editorInstance?.layout();
    }, 300),
    [editorInstance],
  );

  const editorResize = useCallback(
    sizes => {
      editorInstance?.layout();
    },
    [editorInstance],
  );

  const handleSelectViewType = useCallback(
    viewType => {
      dispatch(
        actions.changeCurrentEditingView({
          type: viewType,
          script:
            viewType === 'STRUCT'
              ? {
                  table: [],
                  columns: [],
                  joins: [],
                }
              : '',
        }),
      );
    },
    [dispatch, actions],
  );

  useEffect(() => {
    window.addEventListener('resize', onResize, false);
    return () => {
      window.removeEventListener('resize', onResize);
    };
  }, [onResize]);

  useEffect(() => {
    if (sourceId) {
      dispatch(getSchemaBySourceId(sourceId));
    }
  }, [dispatch, sourceId]);

  return (
    <Wrapper>
      <Development
        direction="vertical"
        gutterSize={0}
        className="datart-split"
        onDrag={editorResize}
      >
        <EditorWrap>
          {!viewType ? (
            unpersistedNewView ? (
              <SelectView selectViewType={handleSelectViewType} />
            ) : (
              <LoadingWrap>
                <Spin />
              </LoadingWrap>
            )
          ) : viewType !== 'STRUCT' ? (
            <Editor allowManage={allowManage} allowEnableViz={allowEnableViz} />
          ) : (
            <StructView
              allowManage={allowManage}
              allowEnableViz={allowEnableViz}
            />
          )}
        </EditorWrap>
        <Outputs />
      </Development>
      <Properties viewType={viewType} allowManage={allowManage} />
    </Wrapper>
  );
});

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  min-height: 0;
`;
const EditorWrap = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
`;
const Development = styled(Split)`
  display: flex;
  flex: 1;
  flex-direction: column;
`;

const LoadingWrap = styled.div`
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  background-color: ${p => p.theme.componentBackground};
`;
