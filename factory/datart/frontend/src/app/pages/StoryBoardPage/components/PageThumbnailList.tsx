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
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useSelector } from 'react-redux';
import { StoryContext } from '../contexts/StoryContext';
import { selectStoryPageInfoById } from '../slice/selectors';
import { StoryBoardState, StoryPage } from '../slice/types';
import ThumbnailItem from './ThumbnailItem';
export type NameCard = {
  index: number;
  name: string;
  thumbnail: string;
  id: string;
  relId: string;
  relType: string;
  editing: boolean;
  selected: boolean;
  storyId: string;
};
export interface PageThumbnailListProps {
  sortedPages: StoryPage[];
  canDrag: boolean;
  onPageClick: (index: number, pageId: string, multiple: boolean) => void;
  onDeletePages?: (ids: string[]) => void;
  onMoveCard?: (dragId, dropId) => void;
}
const PageThumbnailList: React.FC<PageThumbnailListProps> = ({
  sortedPages,
  canDrag,
  onPageClick,
  onDeletePages,
  onMoveCard,
}) => {
  const { storyId: stroyBoardId } = useContext(StoryContext);
  const pageInfoMap = useSelector((state: { storyBoard: StoryBoardState }) =>
    selectStoryPageInfoById(state, stroyBoardId),
  );
  const selectedIds = useMemo(() => {
    return Object.values(pageInfoMap)
      .filter(info => info.selected)
      .map(info => info.id);
  }, [pageInfoMap]);
  const [cards, setCards] = useState<NameCard[]>([]);
  useEffect(() => {
    const card = sortedPages.map(page => ({
      index: page.config.index,
      id: page.id,
      storyId: stroyBoardId,
      name: page.config.name || '',
      thumbnail: page.config.thumbnail || '',
      editing: pageInfoMap[page.id].selected || false,
      selected: pageInfoMap[page.id]?.selected || false,
      relId: page.relId,
      relType: page.relType,
    }));
    setCards(card);
  }, [pageInfoMap, sortedPages, stroyBoardId]);
  const [cardMoveEvent, setCardMoveEvent] = useState<{
    dragId?: string;
    dropId?: string;
  }>({});

  useEffect(() => {
    const deleteSlides = (e: KeyboardEvent) => {
      if (e.keyCode !== 8 && e.keyCode !== 46) {
        return;
      }
      if (!selectedIds.length) {
        return;
      }
      onDeletePages && onDeletePages(selectedIds);
    };

    window.addEventListener('keydown', deleteSlides, false);
    return () => {
      window.removeEventListener('keydown', deleteSlides, false);
    };
  }, [onDeletePages, selectedIds]);
  const pageClick = useCallback(
    (index: number, pageId: string) => (e: React.MouseEvent) => {
      const { shiftKey, metaKey, altKey } = e;
      const multiple = shiftKey || metaKey || altKey;
      onPageClick(index, pageId, multiple);
    },
    [onPageClick],
  );

  const moveCard = useCallback((dragPageId: string, hoverPageId: string) => {
    setCardMoveEvent({
      dragId: dragPageId,
      dropId: hoverPageId,
    });
  }, []);

  const moveEnd = useCallback(() => {
    onMoveCard?.(cardMoveEvent?.dragId, cardMoveEvent?.dropId);
  }, [cardMoveEvent, onMoveCard]);

  return (
    <>
      {cards.map((page, index) => (
        <div key={page.id} onClick={pageClick(index, page.id)}>
          <ThumbnailItem
            key={page.id}
            index={index}
            selected={page.selected}
            page={page}
            canDrag={canDrag}
            moveCard={moveCard}
            moveEnd={moveEnd}
          />
        </div>
      ))}
    </>
  );
};
export default PageThumbnailList;
