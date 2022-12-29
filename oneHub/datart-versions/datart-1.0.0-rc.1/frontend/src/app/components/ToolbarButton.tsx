import { Button, ButtonProps } from 'antd';
import { lighten } from 'polished';
import styled from 'styled-components/macro';
import { FONT_SIZE_BASE } from 'styles/StyleConstants';
import { mergeClassNames } from 'utils/utils';

export interface ToolbarButtonProps extends ButtonProps {
  fontSize?: number;
  iconSize?: number;
  color?: string;
  disabled?: boolean;
}

export function ToolbarButton({
  fontSize = FONT_SIZE_BASE,
  iconSize = FONT_SIZE_BASE * 1.125,
  color,
  disabled,
  ...buttonProps
}: ToolbarButtonProps) {
  return (
    <Wrapper
      fontSize={fontSize}
      iconSize={iconSize}
      color={color}
      disabled={disabled}
    >
      <Button
        type="link"
        disabled={disabled}
        {...buttonProps}
        className={mergeClassNames(buttonProps.className, 'btn')}
      />
    </Wrapper>
  );
}

const Wrapper = styled.span<ToolbarButtonProps>`
  .btn {
    color: ${p =>
      p.disabled
        ? p.theme.textColorDisabled
        : p.color || p.theme.textColorLight};

    &:hover,
    &:focus {
      color: ${p =>
        p.disabled
          ? p.theme.textColorDisabled
          : p.color || p.theme.textColorLight};
      background-color: ${p => p.theme.bodyBackground};
    }

    &:active {
      color: ${p =>
        p.disabled
          ? p.theme.textColorDisabled
          : p.color
          ? lighten(0.1, p.color)
          : p.theme.textColorSnd};
      background-color: ${p => p.theme.emphasisBackground};
    }

    .anticon {
      font-size: ${p => p.iconSize}px;
    }
  }
`;
