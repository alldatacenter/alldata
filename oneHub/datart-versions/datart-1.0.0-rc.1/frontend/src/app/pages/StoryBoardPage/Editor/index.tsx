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
import { Layout, Modal } from 'antd';
import { Split } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useSplitSizes } from 'app/hooks/useSplitSizes';
import { useBoardSlice } from 'app/pages/DashBoardPage/pages/Board/slice';
import { useEditBoardSlice } from 'app/pages/DashBoardPage/pages/BoardEditor/slice';
import { StoryContext } from 'app/pages/StoryBoardPage/contexts/StoryContext';
import { dispatchResize } from 'app/utils/dispatchResize';
import React, {
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import { useParams } from 'react-router-dom';
import Reveal from 'reveal.js';
import 'reveal.js/dist/reveal.css';
import RevealZoom from 'reveal.js/plugin/zoom/plugin';
import styled from 'styled-components/macro';
import { LEVEL_20, SPACE_MD } from 'styles/StyleConstants';
import { uuidv4 } from 'utils/utils';
import PageThumbnailList from '../components/PageThumbnailList';
import StoryPageItem from '../components/StoryPageItem';
import { storyActions, useStoryBoardSlice } from '../slice';
import {
  makeSelectStoryBoardById,
  makeSelectStoryPagesById,
} from '../slice/selectors';
import {
  addStoryPages,
  deleteStoryPage,
  getPageContentDetail,
  getStoryDetail,
  updateStroyBoardPagesByMoveEvent,
} from '../slice/thunks';
import { StoryBoardState } from '../slice/types';
import { StoryToolBar } from './StoryToolBar';

const { Content } = Layout;
export const StoryEditor: React.FC<{}> = memo(() => {
  useBoardSlice();
  useEditBoardSlice();
  useStoryBoardSlice();
  const dispatch = useDispatch();
  const { orgId, storyId } = useParams<{ orgId: string; storyId: string }>();
  useEffect(() => {
    dispatch(getStoryDetail(storyId));
  }, [dispatch, storyId]);
  const t = useI18NPrefix(`viz.board.setting`);
  const history = useHistory();
  const histState = history.location.state as any;
  const domId = useMemo(() => uuidv4(), []);
  const revealRef = useRef<any>();

  const [currentPageIndex, setCurrentPageIndex] = useState(0);
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

  const changePage = useCallback(
    e => {
      const { indexh: slideIdx } = e;
      setCurrentPageIndex(slideIdx);
      const pageId = sortedPages[slideIdx].id;
      dispatch(
        storyActions.changePageSelected({
          storyId,
          pageId,
          multiple: false,
        }),
      );
    },
    [dispatch, sortedPages, storyId],
  );

  const onPageClick = useCallback(
    (index: number, pageId: string, multiple: boolean) => {
      if (!multiple) {
        revealRef.current.slide(index);
      } else {
        dispatch(
          storyActions.changePageSelected({
            storyId,
            pageId,
            multiple: true,
          }),
        );
      }
    },
    [dispatch, storyId],
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

  const onDeletePages = useCallback(
    (pageIds: string[]) => {
      Modal.confirm({
        title: pageIds.length > 1 ? t('delPagesTip') : t('delPageTip'),
        onOk: () => {
          pageIds.forEach(pageId => {
            dispatch(deleteStoryPage({ storyId, pageId }));
          });
        },
      });
    },
    [dispatch, storyId, t],
  );

  useEffect(() => {
    if (sortedPages.length === 0) {
      return;
    }
    if (!sortedPages[currentPageIndex]) {
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

  const onCloseEditor = useCallback(() => {
    history.push(`/organizations/${orgId}/vizs/${storyId}`);
  }, [history, orgId, storyId]);
  useEffect(() => {
    if (sortedPages.length > 0) {
      revealRef.current = new Reveal(document.getElementById(domId), {
        hash: false,
        history: false,
        controls: false,
        controlsLayout: 'none',
        slideNumber: 'c/t',
        controlsTutorial: false,
        progress: false,
        loop: true,
        width: '100%',
        height: '100%',
        margin: 0,
        minScale: 1,
        maxScale: 1,
        autoSlide: null,
        transition: 'convex',
        // backgroundTransition: 'fade',
        transitionSpeed: 'slow',
        viewDistance: 100,
        plugins: [RevealZoom],
        keyboard: {
          70: () => {},
        },
      });
      revealRef.current?.initialize();
      if (revealRef.current) {
        revealRef.current.addEventListener('slidechanged', changePage);
      }
      return () => {
        revealRef.current.removeEventListener('slidechanged', changePage);
      };
    }

    // "none" | "fade" | "slide" | "convex" | "concave" | "zoom"
  }, [domId, changePage, sortedPages.length]);

  useEffect(() => {
    const curPage = sortedPages[currentPageIndex];
    if (!curPage || !curPage.relId || !curPage.relType) {
      return;
    }
    const { relId, relType } = curPage;
    dispatch(getPageContentDetail({ relId, relType }));
  }, [currentPageIndex, dispatch, sortedPages]);

  const addPages = useCallback(async () => {
    if (histState && histState.addDashboardId) {
      await dispatch(
        addStoryPages({ storyId, relIds: [histState.addDashboardId] }),
      );

      //react router remove location state
      if (history.location.state && histState.transaction) {
        let state = { ...histState };
        delete state.transaction;
        history.replace({ ...history.location, state });
      }
    }
  }, [dispatch, histState, storyId, history]);

  const handleMoveCard = useCallback(
    async (dragId, dropId) => {
      dispatch(
        updateStroyBoardPagesByMoveEvent({
          storyId,
          sortedPages,
          event: { dragId, dropId },
        }),
      );
    },
    [dispatch, sortedPages, storyId],
  );

  useEffect(() => {
    addPages();
  }, [addPages]);

  return (
    <DndProvider backend={HTML5Backend}>
      <StoryContext.Provider
        value={{
          name: story?.name,
          storyId: storyId,
          editing: false,
          orgId: orgId,
          status: 1,
          allowShare: false,
        }}
      >
        <Wrapper>
          <StoryToolBar onCloseEditor={onCloseEditor} />
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
                canDrag={true}
                sortedPages={sortedPages}
                onPageClick={onPageClick}
                onDeletePages={onDeletePages}
                onMoveCard={handleMoveCard}
              />
            </PageListWrapper>
            <Content>
              <div id={domId} className="reveal">
                <div className="slides">
                  {sortedPages.map((page, index) => (
                    <StoryPageItem
                      key={page.id}
                      page={page}
                      renderMode="read"
                    />
                  ))}
                </div>
              </div>
            </Content>
          </Container>
        </Wrapper>
      </StoryContext.Provider>
    </DndProvider>
  );
});

const Wrapper = styled.div`
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: ${LEVEL_20};
  display: flex;
  flex-direction: column;
  background-color: ${p => p.theme.bodyBackground};
`;

const Container = styled(Split)`
  display: flex;
  flex: 1;

  .reveal-box {
    width: 100%;
    height: 100%;
  }
  & .reveal .slides {
    text-align: left;
  }
`;

const PageListWrapper = styled.div`
  padding: ${SPACE_MD} ${SPACE_MD} 0;
  overflow-y: auto;
`;
