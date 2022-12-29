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

import { ConsoleSqlOutlined, PartitionOutlined } from '@ant-design/icons';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { memo } from 'react';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  FONT_SIZE_HEADING,
  FONT_SIZE_TITLE,
  FONT_WEIGHT_MEDIUM,
  LINE_HEIGHT_HEADING,
  LINE_HEIGHT_LABEL,
  SPACE_LG,
  SPACE_MD,
  SPACE_TIMES,
  SPACE_XS,
} from 'styles/StyleConstants';

interface selectViewTypeProps {
  selectViewType: (viewType: string) => void;
}

const SelectViewType = memo(({ selectViewType }: selectViewTypeProps) => {
  const viewTypeList = ['STRUCT', 'SQL'];
  const t = useI18NPrefix('view.structView');

  return (
    <Wrapper>
      <Title>{t('title')}</Title>
      <ViewTypeList>
        {viewTypeList.map((v, i) => {
          return (
            <ViewTypeItem onClick={() => selectViewType(v)} key={i}>
              {v === 'STRUCT' ? (
                <PartitionOutlined className="icon" />
              ) : (
                <ConsoleSqlOutlined className="icon" />
              )}
              <h4>{t(v)}</h4>
              <p>{t(`${v}_DESC`)}</p>
            </ViewTypeItem>
          );
        })}
      </ViewTypeList>
    </Wrapper>
  );
});

export default SelectViewType;

const Wrapper = styled.div`
  flex: 1;
  padding: ${SPACE_XS} ${SPACE_LG};
  background-color: ${p => p.theme.componentBackground};
`;

const Title = styled.h2`
  padding: ${SPACE_MD} 0;
  font-size: ${FONT_SIZE_TITLE};
`;

const ViewTypeList = styled.div`
  display: flex;
`;

const ViewTypeItem = styled.div`
  position: relative;
  width: ${SPACE_TIMES(64)};
  padding: ${SPACE_MD} ${SPACE_MD} ${SPACE_LG} ${SPACE_TIMES(12)};
  margin-right: ${SPACE_MD};
  cursor: pointer;
  border: 1px solid ${p => p.theme.borderColorSplit};
  border-radius: ${BORDER_RADIUS};

  .icon {
    position: absolute;
    top: ${SPACE_LG};
    left: ${SPACE_MD};
    margin-right: ${SPACE_XS};
    font-size: ${FONT_SIZE_HEADING};
    color: ${p => p.theme.primary};
  }

  h4 {
    font-weight: ${FONT_WEIGHT_MEDIUM};
    line-height: ${LINE_HEIGHT_HEADING};
    color: ${p => p.theme.textColorSnd};
  }

  p {
    line-height: ${LINE_HEIGHT_LABEL};
    color: ${p => p.theme.textColorLight};
  }

  &:hover {
    border: 1px solid ${p => p.theme.primary};
  }
`;
