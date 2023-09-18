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
import React, {
  memo,
  RefObject,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { useDispatch, useSelector } from 'react-redux';
import Reveal from 'reveal.js';
import 'reveal.js/dist/reveal.css';
import RevealZoom from 'reveal.js/plugin/zoom/plugin';
import styled from 'styled-components/macro';
import { LEVEL_20, WHITE } from 'styles/StyleConstants';
import { uuidv4 } from 'utils/utils';
import { storyActions } from '../../../StoryBoardPage/slice';
import { makeSelectStoryPagesById } from '../../../StoryBoardPage/slice/selectors';
import { getPageContentDetail } from '../../../StoryBoardPage/slice/thunks';
import {
  StoryBoard,
  StoryBoardState,
} from '../../../StoryBoardPage/slice/types';
import { selectSubVizTokenMap } from '../../slice/selectors';
import StoryPageItem from './StoryPageItem';

const { Content } = Layout;

export const StoryPlayerForShare: React.FC<{
  storyBoard: StoryBoard;
  shareToken: string;
}> = memo(({ storyBoard, shareToken }) => {
  const { id: storyId } = storyBoard;
  const domId = useMemo(() => uuidv4(), []);
  const revealRef = useRef<any>();
  const dispatch = useDispatch();
  const [currentPageIndex, setCurrentPageIndex] = useState(0);
  const fullRef: RefObject<HTMLDivElement> = useRef(null);
  const pageMap = useSelector((state: { storyBoard: StoryBoardState }) =>
    makeSelectStoryPagesById(state, storyId),
  );
  const subVizTokenMap = useSelector(selectSubVizTokenMap);

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

  useEffect(() => {
    if (sortedPages.length === 0) {
      return;
    }
    const pageId = sortedPages[0].id;
    dispatch(
      storyActions.changePageSelected({
        storyId,
        pageId,
        multiple: false,
      }),
    );
  }, [dispatch, sortedPages, storyId]);
  const autoSlide = useMemo(() => {
    if (storyBoard?.config?.autoPlay?.auto) {
      return storyBoard?.config?.autoPlay?.delay * 1000;
    }
    return null;
  }, [storyBoard?.config?.autoPlay?.auto, storyBoard?.config?.autoPlay?.delay]);
  useEffect(() => {
    if (sortedPages.length > 0) {
      revealRef.current = new Reveal(document.getElementById(domId), {
        hash: false,
        history: false,
        controls: true,
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
        autoSlide: autoSlide,
        transition: 'convex',
        // backgroundTransition: 'fade',
        transitionSpeed: 'slow',
        viewDistance: 100,
        plugins: [RevealZoom],
        keyboard: {
          27: () => {
            // disabled esc
          }, // do something custom when ESC is pressed
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
  }, [domId, changePage, sortedPages.length, autoSlide, dispatch]);

  useEffect(() => {
    const curPage = sortedPages[currentPageIndex];
    if (!curPage || !curPage.relId || !curPage.relType) {
      return;
    }
    const { relId, relType, id } = curPage;
    const vizToken = subVizTokenMap?.[id];
    if (!vizToken) {
      return;
    }
    dispatch(
      getPageContentDetail({
        shareToken,
        relId,
        relType,
        vizToken: vizToken,
      }),
    );
  }, [
    currentPageIndex,
    dispatch,
    sortedPages,
    pageMap,
    subVizTokenMap,
    shareToken,
  ]);

  return (
    <DndProvider backend={HTML5Backend}>
      <Wrapper ref={fullRef}>
        <Content>
          <div id={domId} className="reveal">
            <div className="slides">
              {sortedPages.map(page => (
                <StoryPageItem key={page.id} page={page} />
              ))}
            </div>
          </div>
        </Content>
      </Wrapper>
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
  background-color: ${WHITE};

  .tool-bar {
    height: 100px;
  }
  & .reveal .slides {
    text-align: left;
  }
`;
