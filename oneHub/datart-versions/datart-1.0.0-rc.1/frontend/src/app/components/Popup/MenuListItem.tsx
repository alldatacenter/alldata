import { Menu, MenuItemProps } from 'antd';
import { cloneElement, ReactElement, ReactNode } from 'react';
import styled, { css } from 'styled-components/macro';
import { LINE_HEIGHT_HEADING, SPACE, SPACE_XS } from 'styles/StyleConstants';
import { mergeClassNames } from 'utils/utils';

const WrapperStyle = css`
  line-height: ${LINE_HEIGHT_HEADING};

  &.selected {
    background-color: ${p => p.theme.emphasisBackground};
  }

  .ant-dropdown-menu-submenu-title {
    line-height: ${LINE_HEIGHT_HEADING};
  }
`;

interface MenuListItemProps extends Omit<MenuItemProps, 'prefix'> {
  prefix?: ReactElement;
  suffix?: ReactElement;
  sub?: boolean;
}

export function MenuListItem({
  prefix,
  suffix,
  sub,
  ...menuProps
}: MenuListItemProps) {
  return sub ? (
    <Menu.SubMenu
      css={WrapperStyle}
      {...menuProps}
      title={
        <ListItem prefix={prefix} suffix={suffix}>
          {menuProps.title}
        </ListItem>
      }
    >
      {menuProps.children}
    </Menu.SubMenu>
  ) : (
    <Menu.Item css={WrapperStyle} {...menuProps}>
      <ListItem prefix={prefix} suffix={suffix}>
        {menuProps.children}
      </ListItem>
    </Menu.Item>
  );
}

interface ListItemProps {
  prefix?: ReactElement;
  suffix?: ReactElement;
  children?: ReactNode;
}

function ListItem({ prefix, suffix, children }: ListItemProps) {
  return (
    <StyledListItem>
      {prefix &&
        cloneElement(prefix, {
          className: mergeClassNames(prefix.props.className, 'prefix'),
        })}
      {children}
      {suffix &&
        cloneElement(suffix, {
          className: mergeClassNames(suffix.props.className, 'suffix'),
        })}
    </StyledListItem>
  );
}

const StyledListItem = styled.div`
  display: flex;
  align-items: center;

  > .prefix {
    flex-shrink: 0;
    margin-right: ${SPACE_XS};
  }

  > .suffix {
    flex-shrink: 0;
    padding: 0 ${SPACE};
  }

  > .prefix,
  > .suffix {
    &.icon {
      color: ${p => p.theme.textColorLight};
    }
  }

  > p {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
  }
`;
