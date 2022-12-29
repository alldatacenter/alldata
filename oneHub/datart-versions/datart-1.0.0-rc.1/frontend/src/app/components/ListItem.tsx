import { List } from 'antd';
import { ListItemProps } from 'antd/lib/list/Item';
import classnames from 'classnames';
import { memo, ReactNode } from 'react';
import styled from 'styled-components/macro';
import {
  FONT_WEIGHT_MEDIUM,
  SPACE_LG,
  SPACE_MD,
  SPACE_TIMES,
  SPACE_XS,
} from 'styles/StyleConstants';
const { Item } = List;

interface Props extends ListItemProps {
  selected?: boolean;
  withAvatar?: boolean;
  children?: ReactNode;
}

export const ListItem = memo(
  ({ selected, withAvatar = false, children, className, ...props }: Props) => {
    return (
      <StyledItem
        className={classnames({
          selected,
          'with-avatar': withAvatar,
          ...(className && { [className]: true }),
        })}
        {...props}
      >
        {children}
      </StyledItem>
    );
  },
);

const StyledItem = styled(Item)`
  padding: ${SPACE_XS} ${SPACE_MD} ${SPACE_XS} ${SPACE_LG};
  cursor: pointer;
  border-bottom: 0 !important;

  .ant-list-item-meta-title {
    margin: 0;
    overflow: hidden;
    font-weight: ${FONT_WEIGHT_MEDIUM};
    color: ${p => p.theme.textColorSnd};
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &.with-avatar {
    padding-left: ${SPACE_MD};
  }

  &.selected {
    background-color: ${p => p.theme.bodyBackground};
  }

  &.recycle {
    padding: ${SPACE_XS} ${SPACE_TIMES(0.5)} ${SPACE_XS} ${SPACE_TIMES(10)};
  }
  &.disabled {
    cursor: not-allowed;

    .ant-list-item-meta-title {
      color: ${p => p.theme.textColorDisabled};
    }
  }

  &.schedule {
    padding: ${SPACE_XS} 0 ${SPACE_XS} ${SPACE_MD};
  }

  .btn-hover {
    display: none;
  }

  &:hover,
  &.selected {
    .ant-list-item-meta-title {
      color: ${p => p.theme.primary};
    }

    .btn-hover {
      display: block;
    }
  }

  .ant-list-item-meta-description {
    overflow: hidden;
    color: ${p => p.theme.textColorLight};
    text-overflow: ellipsis;
  }

  .ant-list-item-action {
    margin-left: ${SPACE_LG};
  }

  .ant-btn-icon-only {
    width: ${SPACE_TIMES(5)};
    height: ${SPACE_TIMES(5)};
  }
`;
