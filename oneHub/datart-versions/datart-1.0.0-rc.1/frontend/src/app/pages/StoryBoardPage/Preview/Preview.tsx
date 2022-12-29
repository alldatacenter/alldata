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
import { Layout } from 'antd';
import { Split } from 'app/components';
import { useSplitSizes } from 'app/hooks/useSplitSizes';
import { usePublishBoard } from 'app/pages/DashBoardPage/hooks/usePublishBoard';
import { selectPublishLoading } from 'app/pages/MainPage/pages/VizPage/slice/selectors';
import { StoryContext } from 'app/pages/StoryBoardPage/contexts/StoryContext';
import { dispatchResize } from 'app/utils/dispatchResize';
import React, { memo, useCallback, useEffect, useMemo, useState } from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import 'reveal.js/dist/reveal.css';
import styled from 'styled-components/macro';
import { SPACE_MD } from 'styles/StyleConstants';
import PageThumbnailList from '../components/PageThumbnailList';
import StoryHeader from '../components/StoryHeader';
import StoryPageItem from '../components/StoryPageItem';
import { storyActions } from '../slice';
import {
  makeSelectStoryBoardById,
  makeSelectStoryPagesById,
} from '../slice/selectors';
import { getPageContentDetail, getStoryDetail } from '../slice/thunks';
import { StoryBoardState } from '../slice/types';

const { Content } = Layout;

export const StoryPagePreview: React.FC<{
  orgId: string;
  storyId: string;
  allowShare?: boolean;
  allowManage?: boolean;
}> = memo(({ orgId, storyId, allowShare, allowManage }) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const [currentPageIndex, setCurrentPageIndex] = useState(0);
  const publishLoading = useSelector(selectPublishLoading);
  const story = useSelector((state: { storyBoard: StoryBoardState }) =>
    makeSelectStoryBoardById(state, storyId),
  );
  const pageMap = useSelector((state: { storyBoard: StoryBoardState }) =>
    makeSelectStoryPagesById(state, storyId),
  );

  const sortedPages = useMemo(() => {
    const sortedPages = Object.values(pageMap).sort(
      (a, b) => a.config.index - b.config.index,
    );
    return sortedPages;
  }, [pageMap]);

  const currentPage = useMemo(() => {
    const currentPage = sortedPages[currentPageIndex];
    return currentPage;
  }, [currentPageIndex, sortedPages]);

  const onPageClick = useCallback(
    (index: number, pageId: string, multiple: boolean) => {
      setCurrentPageIndex(index);
      dispatch(
        storyActions.changePageSelected({
          storyId,
          pageId,
          multiple: false,
        }),
      );
    },
    [dispatch, storyId],
  );

  const toggleEdit = useCallback(() => {
    history.push(`/organizations/${orgId}/vizs/storyEditor/${storyId}`);
  }, [history, orgId, storyId]);

  const playStory = useCallback(() => {
    window.open(`storyPlayer/${storyId}`, '_blank');
  }, [storyId]);

  const { publishStory } = usePublishBoard(
    storyId,
    'STORYBOARD',
    story?.status || 0,
  );

  const { sizes, setSizes } = useSplitSizes({
    limitedSide: 0,
    range: [150, 768],
  });

  const siderDragEnd = useCallback(
    sizes => {
      setSizes(sizes);
      dispatchResize();
    },

    [setSizes],
  );

  // 点击在加载
  useEffect(() => {
    const curPage = sortedPages[currentPageIndex];
    if (!curPage || !curPage.relId || !curPage.relType) {
      return;
    }
    const { relId, relType } = curPage;
    dispatch(getPageContentDetail({ relId, relType }));
  }, [currentPageIndex, dispatch, sortedPages, pageMap]);

  useEffect(() => {
    dispatch(getStoryDetail(storyId));
  }, [dispatch, storyId]);

  useEffect(() => {
    if (sortedPages.length === 0) {
      return;
    }
    const pageId = sortedPages[currentPageIndex].id;
    dispatch(
      storyActions.changePageSelected({
        storyId,
        pageId,
        multiple: false,
      }),
    );
  }, [dispatch, currentPageIndex, sortedPages, storyId]);

  return (
    <DndProvider backend={HTML5Backend}>
      <StoryContext.Provider
        value={{
          name: story?.name,
          storyId: storyId,
          editing: false,
          orgId: orgId,
          status: 1,
          allowShare: allowShare || false,
        }}
      >
        <Wrapper>
          <StoryHeader
            orgId={orgId}
            name={story?.name}
            playStory={playStory}
            status={story?.status}
            toggleEdit={toggleEdit}
            publishLoading={publishLoading}
            onPublish={publishStory}
            allowShare={allowShare}
            allowManage={allowManage}
          />
          <Container
            sizes={sizes}
            minSize={[256, 0]}
            maxSize={[768, Infinity]}
            gutterSize={0}
            onDragEnd={siderDragEnd}
            className="datart-split"
          >
            <PageListWrapper>
              <PageThumbnailList
                canDrag={false}
                sortedPages={sortedPages}
                onPageClick={onPageClick}
              />
            </PageListWrapper>
            <Content>
              {sortedPages.map(page => (
                <PreviewPage key={page.id} show={page.id === currentPage.id}>
                  <StoryPageItem
                    key={page.id}
                    page={page}
                    showZoomCtrl={true}
                    renderMode="read"
                  />
                </PreviewPage>
              ))}
            </Content>
          </Container>
        </Wrapper>
      </StoryContext.Provider>
    </DndProvider>
  );
});

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-flow: column;
  min-height: 0;
`;

const PageListWrapper = styled.div`
  padding: ${SPACE_MD} ${SPACE_MD} 0;
  overflow-y: auto;
`;

const Container = styled(Split)`
  display: flex;
  flex: 1;
`;

const PreviewPage = styled.div<{ show: boolean }>`
  display: ${p => (p.show ? 'flex' : 'none')};
  flex: 1;
  height: 100%;
`;
