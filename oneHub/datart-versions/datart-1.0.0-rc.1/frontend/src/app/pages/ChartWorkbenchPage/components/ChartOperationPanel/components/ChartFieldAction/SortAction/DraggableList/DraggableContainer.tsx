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

import { updateBy } from 'app/utils/mutation';
import { FC } from 'react';
import styled from 'styled-components/macro';
import { DraggableItem } from './DraggableItem';

interface Source {
  id: number;
  text: string;
}

export const DraggableContainer: FC<{ source: Source[]; onChange }> = ({
  source,
  onChange,
}) => {
  const onDrop = (dragIndex: number, hoverIndex: number) => {
    const newSource = updateBy(source, draft => {
      const dragCard = draft[dragIndex];
      draft.splice(dragIndex, 1);
      draft.splice(hoverIndex, 0, dragCard);
      return draft;
    });

    onChange?.(newSource.map(s => s.text));
  };

  const renderDraggableItem = (item: Source, index: number) => {
    return (
      <DraggableItem
        key={item.id}
        index={index}
        id={item.id}
        text={item.text}
        onDrop={onDrop}
      />
    );
  };

  return (
    <StyledDiv>
      {source.map((item, i) => renderDraggableItem(item, i))}
    </StyledDiv>
  );
};

export default DraggableContainer;

const StyledDiv = styled.div`
  width: 100%;
  border: 1px dashed gray;
`;
