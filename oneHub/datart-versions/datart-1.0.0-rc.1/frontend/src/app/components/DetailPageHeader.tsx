import { Space } from 'antd';
import classnames from 'classnames';
import { ReactElement } from 'react';
import styled from 'styled-components/macro';
import {
  FONT_SIZE_ICON_SM,
  FONT_WEIGHT_MEDIUM,
  LINE_HEIGHT_BODY,
  LINE_HEIGHT_ICON_SM,
  SPACE_LG,
  SPACE_SM,
  SPACE_XS,
} from 'styles/StyleConstants';

interface DetailPageHeaderProps {
  title?: string;
  description?: string;
  actions?: ReactElement;
  disabled?: boolean;
}

export function DetailPageHeader({
  title,
  description,
  actions,
  disabled,
}: DetailPageHeaderProps) {
  return (
    <Wrapper>
      <Title>
        <h1 className={classnames({ disabled })}>{title}</h1>
        <Space>{actions}</Space>
      </Title>
      {description && <Description>{description}</Description>}
    </Wrapper>
  );
}

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  padding: ${SPACE_SM} ${SPACE_LG};
  background-color: ${p => p.theme.componentBackground};
  box-shadow: ${p => p.theme.shadowSider};
`;

const Title = styled.div`
  display: flex;
  flex-shrink: 0;
  align-items: center;

  h1 {
    flex: 1;
    font-size: ${FONT_SIZE_ICON_SM};
    font-weight: ${FONT_WEIGHT_MEDIUM};
    line-height: ${LINE_HEIGHT_ICON_SM};

    &.disabled {
      color: ${p => p.theme.textColorLight};
    }
  }
`;

const Description = styled.p`
  flex-shrink: 0;
  padding: ${SPACE_XS} ${SPACE_LG};
  line-height: ${LINE_HEIGHT_BODY};
  color: ${p => p.theme.textColorLight};
  background-color: ${p => p.theme.componentBackground};
`;
