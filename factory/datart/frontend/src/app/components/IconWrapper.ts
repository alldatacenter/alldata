import styled from 'styled-components/macro';
import { SPACE_TIMES } from 'styles/StyleConstants';

export const IW = styled.div<{ size?: string; fontSize: string }>`
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: ${p => p.size || SPACE_TIMES(8)};
  height: ${p => p.size || SPACE_TIMES(8)};
  font-size: ${p => p.fontSize};

  > i {
    font-size: ${p => p.fontSize};
  }
`;
