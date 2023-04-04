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
import { useCallback, useEffect, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useLocation, useRouteMatch } from 'react-router-dom';
import styled from 'styled-components';
import { getToken } from 'utils/auth';
import persistence from 'utils/persistence';
import { urlSearchTransfer } from 'utils/urlSearchTransfer';
import { BoardLoading } from '../../DashBoardPage/components/BoardLoading';
import { useBoardSlice } from '../../DashBoardPage/pages/Board/slice';
import { VizRenderMode } from '../../DashBoardPage/pages/Board/slice/types';
import { useEditBoardSlice } from '../../DashBoardPage/pages/BoardEditor/slice';
import { FilterSearchParams } from '../../MainPage/pages/VizPage/slice/types';
import { useStoryBoardSlice } from '../../StoryBoardPage/slice';
import { selectShareStoryBoard } from '../../StoryBoardPage/slice/selectors';
import PasswordModal from '../components/PasswordModal';
import ShareLoginModal from '../components/ShareLoginModal';
import { shareActions, useShareSlice } from '../slice';
import { selectNeedVerify, selectShareVizType } from '../slice/selectors';
import { fetchShareVizInfo } from '../slice/thunks';
import { StoryPlayerForShare } from './StoryPlayerForShare';

function ShareStoryPlayerPage() {
  const { shareActions: actions } = useShareSlice();
  useStoryBoardSlice();
  useBoardSlice();
  useEditBoardSlice();

  const dispatch = useDispatch();
  const location = useLocation();
  const { params }: { params: { token: string } } = useRouteMatch();
  const search = location.search;
  const shareToken = params.token;
  const logged = !!getToken();

  const needVerify = useSelector(selectNeedVerify);
  const shareStory = useSelector(selectShareStoryBoard);
  const vizType = useSelector(selectShareVizType);

  const shareType = useRouteQuery({
    key: 'type',
  });
  // in timed task eager=true for disable board lazyLoad
  const eager = useRouteQuery({
    key: 'eager',
  });
  const renderMode: VizRenderMode = eager ? 'schedule' : 'share';

  useEffect(() => {
    if (shareStory?.name) {
      dispatch(shareActions.savePageTitle({ title: shareStory?.name }));
    }
  }, [shareStory?.name, dispatch]);

  const searchParams = useMemo(() => {
    return urlSearchTransfer.toParams(search);
  }, [search]);

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
          fetchShareVizInfoImpl(shareToken, sharePassword);
        }}
      />
      {!vizType && !needVerify && (
        <div className="loading-container">
          <BoardLoading />
        </div>
      )}

      {!Boolean(needVerify) && shareStory && (
        <StoryPlayerForShare storyBoard={shareStory} shareToken={shareToken} />
      )}
    </StyledWrapper>
  );
}
export default ShareStoryPlayerPage;
const StyledWrapper = styled.div`
  width: 100%;
  height: 100vh;
  .loading-container {
    display: flex;
    height: 100vh;
  }
`;
