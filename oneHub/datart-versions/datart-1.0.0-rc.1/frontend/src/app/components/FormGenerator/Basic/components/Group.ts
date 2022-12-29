import styled from 'styled-components/macro';
import { SPACE_XS } from 'styles/StyleConstants';

export const Group = styled.div`
  display: flex;
  flex: 1;
  margin-bottom: ${SPACE_XS};

  &:last-of-type {
    margin-bottom: 0;
  }

  .ant-select {
    margin-left: ${SPACE_XS};
    overflow: hidden;

    &:first-of-type {
      margin-left: 0;
    }
  }
`;

export const WithColorPicker = styled.div`
  display: flex;
  align-items: center;

  > div:first-of-type {
    margin-bottom: 0;
  }

  > .color-picker {
    margin-left: ${SPACE_XS};
  }
`;
