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

import ChartEditor, { ChartEditorBaseProps } from 'app/components/ChartEditor';
import useMount from 'app/hooks/useMount';
import ChartManager from 'app/models/ChartManager';
import { useAppSlice } from 'app/slice';
import React, { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Redirect,
  Route,
  Switch,
  useHistory,
  useRouteMatch,
} from 'react-router';
import styled from 'styled-components/macro';
import { NotFoundPage } from '../NotFoundPage';
import { StoryEditor } from '../StoryBoardPage/Editor';
import { StoryPlayer } from '../StoryBoardPage/Player';
import { AccessRoute } from './AccessRoute';
import { Background } from './Background';
import { Navbar } from './Navbar';
import { ConfirmInvitePage } from './pages/ConfirmInvitePage';
import { MemberPage } from './pages/MemberPage';
import { OrgSettingPage } from './pages/OrgSettingPage';
import { PermissionPage } from './pages/PermissionPage';
import { ResourceTypes } from './pages/PermissionPage/constants';
import { ResourceMigrationPage } from './pages/ResourceMigrationPage';
import { SchedulePage } from './pages/SchedulePage';
import { SourcePage } from './pages/SourcePage';
import { VariablePage } from './pages/VariablePage';
import { ViewPage } from './pages/ViewPage';
import { useViewSlice } from './pages/ViewPage/slice';
import { VizPage } from './pages/VizPage';
import { useVizSlice } from './pages/VizPage/slice';
import { initChartPreviewData } from './pages/VizPage/slice/thunks';
import { useMainSlice } from './slice';
import { selectOrgId } from './slice/selectors';
import {
  getDataProviders,
  getLoggedInUserPermissions,
  getUserSettings,
} from './slice/thunks';
import { MainPageRouteParams } from './types';

export function MainPage() {
  useAppSlice();
  const { actions } = useMainSlice();
  const { actions: vizActions } = useVizSlice();
  const { actions: viewActions } = useViewSlice();
  const dispatch = useDispatch();
  const organizationMatch = useRouteMatch<MainPageRouteParams>(
    '/organizations/:orgId',
  );
  const orgId = useSelector(selectOrgId);
  const history = useHistory();
  // loaded first time

  useMount(
    () => {
      ChartManager.instance()
        .load()
        .catch(err =>
          console.error('Fail to load customize charts with ', err),
        );
      dispatch(getUserSettings(organizationMatch?.params.orgId));
      dispatch(getDataProviders());
    },
    () => {
      dispatch(actions.clear());
    },
  );

  useEffect(() => {
    if (orgId) {
      dispatch(vizActions.clear());
      dispatch(viewActions.clear());
      dispatch(getLoggedInUserPermissions(orgId));
    }
  }, [dispatch, vizActions, viewActions, orgId]);

  const onSaveInDataChart = useCallback(
    (orgId: string, backendChartId: string) => {
      dispatch(
        initChartPreviewData({
          backendChartId,
          orgId,
        }),
      );
      history.push(`/organizations/${orgId}/vizs/${backendChartId}`);
    },
    [dispatch, history],
  );

  return (
    <AppContainer>
      <Background />
      <Navbar />
      {orgId && (
        <Switch>
          <Route path="/" exact>
            <Redirect to={`/organizations/${orgId}`} />
          </Route>
          <Route path="/confirminvite" component={ConfirmInvitePage} />
          <Route path="/organizations/:orgId" exact>
            <Redirect
              to={`/organizations/${organizationMatch?.params.orgId}/vizs`}
            />
          </Route>
          <Route
            path="/organizations/:orgId/vizs/chartEditor"
            render={res => {
              const hisSearch = new URLSearchParams(res.location.search);
              const hisState = {
                dataChartId: hisSearch.get('dataChartId') || '',
                chartType: hisSearch.get('chartType') || 'dataChart',
                container: hisSearch.get('container') || 'dataChart',
                defaultViewId: hisSearch.get('defaultViewId') || '',
              } as ChartEditorBaseProps;
              return (
                <AccessRoute module={ResourceTypes.Viz}>
                  <ChartEditor
                    dataChartId={hisState.dataChartId}
                    orgId={orgId}
                    chartType={hisState.chartType}
                    container={hisState.container}
                    defaultViewId={hisState.defaultViewId}
                    onClose={() => history.go(-1)}
                    onSaveInDataChart={onSaveInDataChart}
                  />
                </AccessRoute>
              );
            }}
          />

          <Route
            path="/organizations/:orgId/vizs/storyPlayer/:storyId"
            render={() => <StoryPlayer />}
          />
          <Route
            path="/organizations/:orgId/vizs/storyEditor/:storyId"
            render={() => <StoryEditor />}
          />
          <Route
            path="/organizations/:orgId/vizs/:vizId?"
            render={() => (
              <AccessRoute module={ResourceTypes.Viz}>
                <VizPage />
              </AccessRoute>
            )}
          />
          <Route
            path="/organizations/:orgId/views/:viewId?"
            render={() => (
              <AccessRoute module={ResourceTypes.View}>
                <ViewPage />
              </AccessRoute>
            )}
          />
          <Route
            path="/organizations/:orgId/sources"
            render={() => (
              <AccessRoute module={ResourceTypes.Source}>
                <SourcePage />
              </AccessRoute>
            )}
          />
          <Route
            path="/organizations/:orgId/schedules"
            render={() => (
              <AccessRoute module={ResourceTypes.Schedule}>
                <SchedulePage />
              </AccessRoute>
            )}
          />
          <Route
            path="/organizations/:orgId/members"
            render={() => (
              <AccessRoute module={ResourceTypes.User}>
                <MemberPage />
              </AccessRoute>
            )}
          />
          <Route
            path="/organizations/:orgId/roles"
            render={() => (
              <AccessRoute module={ResourceTypes.User}>
                <MemberPage />
              </AccessRoute>
            )}
          />
          <Route path="/organizations/:orgId/permissions" exact>
            <Redirect
              to={`/organizations/${organizationMatch?.params.orgId}/permissions/subject`}
            />
          </Route>
          <Route
            path="/organizations/:orgId/permissions/:viewpoint"
            render={() => (
              <AccessRoute module={ResourceTypes.Manager}>
                <PermissionPage />
              </AccessRoute>
            )}
          />
          <Route
            path="/organizations/:orgId/variables"
            render={() => (
              <AccessRoute module={ResourceTypes.Manager}>
                <VariablePage />
              </AccessRoute>
            )}
          />
          <Route
            path="/organizations/:orgId/orgSettings"
            render={() => (
              <AccessRoute module={ResourceTypes.Manager}>
                <OrgSettingPage />
              </AccessRoute>
            )}
          />
          <Route
            path="/organizations/:orgId/resourceMigration"
            render={() => (
              <AccessRoute module={ResourceTypes.Manager}>
                <ResourceMigrationPage />
              </AccessRoute>
            )}
          />
          <Route path="*" component={NotFoundPage} />
        </Switch>
      )}
    </AppContainer>
  );
}

const AppContainer = styled.main`
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  background-color: ${p => p.theme.bodyBackground};
`;
