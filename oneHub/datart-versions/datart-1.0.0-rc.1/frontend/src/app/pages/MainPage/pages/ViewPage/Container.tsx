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

import { Split } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useSplitSizes } from 'app/hooks/useSplitSizes';
import React, { useCallback, useContext, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { EditorContext } from './EditorContext';
import { Main } from './Main';
import { SaveForm } from './SaveForm';
import { Sidebar } from './Sidebar';

export function Container() {
  const { editorInstance } = useContext(EditorContext);
  const tg = useI18NPrefix('global');
  const [isDragging, setIsDragging] = useState(false);
  const [sliderVisible, setSliderVisible] = useState<boolean>(false);

  const editorResize = useCallback(() => {
    editorInstance?.layout();
  }, [editorInstance]);

  const { sizes, setSizes } = useSplitSizes({
    limitedSide: 0,
    range: [256, 768],
  });

  const siderDragEnd = useCallback(
    sizes => {
      setSizes(sizes);
      editorResize();
      setIsDragging(false);
    },
    [setIsDragging, setSizes, editorResize],
  );

  const siderDragStart = useCallback(() => {
    if (!isDragging) setIsDragging(true);
  }, [setIsDragging, isDragging]);

  const handleSliderVisible = useCallback(
    (status: boolean) => {
      setSliderVisible(status);
    },
    [setSliderVisible],
  );

  useEffect(() => {
    editorResize();
  }, [sliderVisible, editorResize]);

  return (
    <StyledContainer
      sizes={sizes}
      minSize={[256, 0]}
      maxSize={[768, Infinity]}
      gutterSize={0}
      onDragStart={siderDragStart}
      onDragEnd={siderDragEnd}
      className="datart-split"
      sliderVisible={sliderVisible}
    >
      <Sidebar
        width={sizes[0]}
        isDragging={isDragging}
        sliderVisible={sliderVisible}
        handleSliderVisible={handleSliderVisible}
      />
      <Main sliderVisible={sliderVisible} />
      <SaveForm
        formProps={{
          labelAlign: 'left',
          labelCol: { offset: 1, span: 8 },
          wrapperCol: { span: 13 },
        }}
        okText={tg('button.save')}
      />
    </StyledContainer>
  );
}

const StyledContainer = styled(Split)<{ sliderVisible: boolean }>`
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  .gutter-horizontal {
    display: ${p => (p.sliderVisible ? 'none' : 'block')};
  }
`;
