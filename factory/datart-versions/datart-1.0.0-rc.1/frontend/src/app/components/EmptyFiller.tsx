import { Empty, EmptyProps, Spin } from 'antd';
import { memo, ReactNode } from 'react';
import styled from 'styled-components/macro';
import {
  FONT_SIZE_TITLE,
  LINE_HEIGHT_ICON_XL,
  SPACE_XS,
} from 'styles/StyleConstants';

interface EmptyFillerProps extends EmptyProps {
  title?: string;
  loading?: boolean;
  loadingIcon?: ReactNode;
}

export const EmptyFiller = memo(
  ({ title, loading, loadingIcon, ...emptyProps }: EmptyFillerProps) => {
    return (
      <Wrapper>
        <Empty {...emptyProps} description="" />
        <h3>
          {loading ? loadingIcon || <LoadingIcon /> : null}
          {title}
        </h3>
      </Wrapper>
    );
  },
);

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: ${p => p.theme.textColorDisabled};

  h3 {
    font-size: ${FONT_SIZE_TITLE};
    line-height: ${LINE_HEIGHT_ICON_XL};
  }
`;
const LoadingIcon = styled(Spin)`
  margin-right: ${SPACE_XS};
`;
