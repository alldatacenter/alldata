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

import { EmptyFiller } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { memo, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useRouteMatch } from 'react-router-dom';
import styled from 'styled-components/macro';
import { selectOrgId } from '../../../slice/selectors';
import { useMemberSlice } from '../../MemberPage/slice';
import { useSourceSlice } from '../../SourcePage/slice';
import { getSources } from '../../SourcePage/slice/thunks';
import { useVariableSlice } from '../../VariablePage/slice';
import { selectEditingViews } from '../slice/selectors';
import { getViewDetail } from '../slice/thunks';
import { Tabs } from './Tabs';
import { Workbench } from './Workbench';

export const Main = memo(({ sliderVisible }: { sliderVisible: boolean }) => {
  useSourceSlice();
  useMemberSlice();
  useVariableSlice();

  const dispatch = useDispatch();
  const {
    params: { viewId },
  } = useRouteMatch<{ viewId: string }>();

  const orgId = useSelector(selectOrgId);
  const editingViews = useSelector(selectEditingViews);

  const t = useI18NPrefix('view');

  useEffect(() => {
    dispatch(getSources(orgId));
  }, [dispatch, orgId]);

  useEffect(() => {
    if (viewId) {
      dispatch(getViewDetail({ viewId }));
    }
  }, [dispatch, viewId, orgId]);

  return (
    <Container className={sliderVisible ? 'close' : ''}>
      {editingViews.length > 0 ? (
        <>
          <Tabs />
          <Workbench />
        </>
      ) : (
        <EmptyFiller title={t('empty')} />
      )}
    </Container>
  );
});

const Container = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  &.close {
    width: calc(100% - 30px) !important;
    min-width: calc(100% - 30px) !important;
    padding-left: 30px;
  }
`;
