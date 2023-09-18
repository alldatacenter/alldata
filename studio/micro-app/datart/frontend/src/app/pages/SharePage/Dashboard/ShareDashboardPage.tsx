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

import useMount from 'app/hooks/useMount';
import useRouteQuery from 'app/hooks/useRouteQuery';
import ChartManager from 'app/models/ChartManager';
import { login } from 'app/slice/thunks';
import { ChartDataRequest } from 'app/types/ChartDataRequest';
import {
  downloadShareDataChartFile,
  loadShareTask,
  makeShareDownloadDataTask,
} from 'app/utils/fetch';
import { StorageKeys } from 'globalConstants';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useLocation, useRouteMatch } from 'react-router-dom';
import styled from 'styled-components';
import { getToken } from 'utils/auth';
import persistence from 'utils/persistence';
import { urlSearchTransfer } from 'utils/urlSearchTransfer';
import { uuidv4 } from 'utils/utils';
import { BoardLoading } from '../../DashBoardPage/components/BoardLoading';
import { useBoardSlice } from '../../DashBoardPage/pages/Board/slice';
import { selectShareBoard } from '../../DashBoardPage/pages/Board/slice/selector';
import { VizRenderMode } from '../../DashBoardPage/pages/Board/slice/types';
import { useEditBoardSlice } from '../../DashBoardPage/pages/BoardEditor/slice';
import { FilterSearchParams } from '../../MainPage/pages/VizPage/slice/types';
import PasswordModal from '../components/PasswordModal';
import ShareLoginModal from '../components/ShareLoginModal';
import { shareActions, useShareSlice } from '../slice';
import {
  selectNeedVerify,
  selectShareExecuteTokenMap,
  selectSharePassword,
  selectShareVizType,
} from '../slice/selectors';
import { fetchShareVizInfo } from '../slice/thunks';
import DashboardForShare from './DashboardForShare';

function ShareDashboardPage() {
  const { shareActions: actions } = useShareSlice();
  useEditBoardSlice();
  useBoardSlice();

  const dispatch = useDispatch();
  const location = useLocation();
  const { params }: { params: { token: string } } = useRouteMatch();
  const search = location.search;
  const shareToken = params.token;

  const [shareClientId, setShareClientId] = useState('');
  const executeTokenMap = useSelector(selectShareExecuteTokenMap);
  const needVerify = useSelector(selectNeedVerify);
  const sharePassword = useSelector(selectSharePassword);
  const shareBoard = useSelector(selectShareBoard);
  const vizType = useSelector(selectShareVizType);
  const logged = !!getToken();

  const shareType = useRouteQuery({
    key: 'type',
  });
  // in timed task eager=true for disable board lazyLoad
  const eager = useRouteQuery({
    key: 'eager',
  });
  const renderMode: VizRenderMode = eager ? 'schedule' : 'share';
  const searchParams = useMemo(() => {
    return urlSearchTransfer.toParams(search);
  }, [search]);

  useEffect(() => {
    if (shareBoard?.name) {
      dispatch(shareActions.savePageTitle({ title: shareBoard?.name }));
    }
  }, [shareBoard?.name, dispatch]);

  const loadVizData = () => {
    if (shareType === 'CODE') {
      const previousPassword = persistence.session.get(shareToken);

      if (previousPassword) {
        fetchShareVizInfoImpl(shareToken, previousPassword, searchParams);
      } else {
        dispatch(actions.saveNeedVerify(true));
      }
    } else if (shareType === 'LOGIN' && !logged) {
      dispatch(actions.saveNeedVerify(true));
    } else {
      fetchShareVizInfoImpl(shareToken, undefined, searchParams);
    }
  };
  useMount(() => {
    ChartManager.instance()
      .load()
      .then(() => loadVizData())
      .catch(err => console.error('Fail to load customize charts with ', err));
  });

  const fetchShareVizInfoImpl = useCallback(
    (
      token?: string,
      pwd?: string,
      params?: FilterSearchParams,
      loginUser?: string,
      loginPwd?: string,
      authorizedToken?: string,
    ) => {
      dispatch(
        fetchShareVizInfo({
          shareToken: token,
          sharePassword: pwd,
          filterSearchParams: params,
          renderMode,
          userName: loginUser,
          passWord: loginPwd,
          authorizedToken,
        }),
      );
    },
    [dispatch, renderMode],
  );

  const onLoadShareTask = useMemo(() => {
    const clientId = localStorage.getItem(StorageKeys.ShareClientId);
    if (clientId) {
      setShareClientId(clientId);
    } else {
      const id = uuidv4();
      setShareClientId(id);
      localStorage.setItem(StorageKeys.ShareClientId, uuidv4());
    }
    const executeToken = Object.values(executeTokenMap)[0]?.authorizedToken;
    return () =>
      loadShareTask({
        shareToken: executeToken,
        clientId: shareClientId,
      });
  }, [executeTokenMap, shareClientId]);

  const onMakeShareDownloadDataTask = useCallback(
    (downloadParams: ChartDataRequest[], fileName: string) => {
      if (shareClientId && executeTokenMap) {
        dispatch(
          makeShareDownloadDataTask({
            clientId: shareClientId,
            executeToken: executeTokenMap,
            downloadParams: downloadParams,
            shareToken,
            fileName: fileName,
            resolve: () => {
              dispatch(actions.setShareDownloadPolling(true));
            },
            password: sharePassword,
          }),
        );
      }
    },
    [
      shareClientId,
      shareToken,
      sharePassword,
      executeTokenMap,
      dispatch,
      actions,
    ],
  );

  const onDownloadFile = useCallback(
    task => {
      const executeToken = Object.values(executeTokenMap)[0]?.authorizedToken;
      downloadShareDataChartFile({
        downloadId: task.id,
        shareToken: executeToken,
      }).then(() => {
        dispatch(actions.setShareDownloadPolling(true));
      });
    },
    [executeTokenMap, dispatch, actions],
  );

  const handleLogin = useCallback(
    values => {
      dispatch(
        login({
          params: values,
          resolve: () => {
            fetchShareVizInfoImpl(
              shareToken,
              undefined,
              searchParams,
              values.username,
              values.password,
            );
          },
        }),
      );
    },
    [dispatch, fetchShareVizInfoImpl, searchParams, shareToken],
  );

  return (
    <StyledWrapper className="datart-viz">
      <ShareLoginModal
        visible={Boolean(needVerify) && shareType === 'LOGIN'}
        onChange={handleLogin}
      />
      <PasswordModal
        visible={Boolean(needVerify) && shareType === 'CODE'}
        onChange={sharePassword => {
          fetchShareVizInfoImpl(shareToken, sharePassword, searchParams);
        }}
      />
      {!vizType && !needVerify && (
        <div className="loading-container">
          <BoardLoading />
        </div>
      )}

      {!Boolean(needVerify) && shareBoard && (
        <DashboardForShare
          dashboard={shareBoard}
          allowDownload={false}
          loadVizData={loadVizData}
          onMakeShareDownloadDataTask={onMakeShareDownloadDataTask}
          renderMode={renderMode}
          filterSearchUrl={''}
          onLoadShareTask={onLoadShareTask}
          onDownloadFile={onDownloadFile}
        />
      )}
    </StyledWrapper>
  );
}
export default ShareDashboardPage;
const StyledWrapper = styled.div`
  width: 100%;
  height: 100vh;
  .loading-container {
    display: flex;
    height: 100vh;
  }
`;
