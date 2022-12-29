import { LeftOutlined, MoreOutlined, SearchOutlined } from '@ant-design/icons';
import { Input, Space, Tooltip } from 'antd';
import {
  MenuListItem,
  MenuWrapper,
  Popup,
  ToolbarButton,
} from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { ReactElement, useCallback, useState } from 'react';
import styled from 'styled-components/macro';
import {
  FONT_SIZE_SUBTITLE,
  FONT_SIZE_TITLE,
  FONT_WEIGHT_MEDIUM,
  LINE_HEIGHT_ICON_LG,
  PRIMARY,
  SPACE,
  SPACE_MD,
  SPACE_TIMES,
  SPACE_UNIT,
  SPACE_XS,
} from 'styles/StyleConstants';
import { AddButton } from './AddButton';

export interface ListTitleProps {
  key?: string;
  title?: string;
  subTitle?: string;
  className?: string;
  search?: boolean;
  back?: boolean;
  add?: {
    items: Array<{ key: string; text: string }>;
    icon?: ReactElement;
    callback: (menuClickHandler?: { key: string }) => void;
  };
  more?: {
    items: Array<{
      key: string;
      text: string;
      prefix?: ReactElement;
      suffix?: ReactElement;
    }>;
    callback: (
      key: string,
      onPrevious?: () => void,
      onNext?: () => void,
    ) => void;
  };
  onSearch?: (e) => void;
  onPrevious?: () => void;
  onNext?: () => void;
}

export function ListTitle({
  title,
  subTitle,
  search,
  add,
  more,
  back,
  className,
  onSearch,
  onPrevious,
  onNext,
}: ListTitleProps) {
  const [searchbarVisible, setSearchbarVisible] = useState(false);
  const t = useI18NPrefix('components.listTitle');
  const toggleSearchbar = useCallback(() => {
    setSearchbarVisible(!searchbarVisible);
  }, [searchbarVisible]);

  const moreMenuClick = useCallback(
    ({ key }) => {
      more?.callback(key, onPrevious, onNext);
    },
    [more, onPrevious, onNext],
  );

  const backClick = useCallback(() => {
    onPrevious && onPrevious();
  }, [onPrevious]);

  return (
    <Wrapper className={className}>
      <Title className="title">
        {back && (
          <span className="back" onClick={backClick}>
            <LeftOutlined />
          </span>
        )}
        {title && <h3>{title}</h3>}
        {subTitle && <h5>{subTitle}</h5>}
        <Space size={SPACE_UNIT}>
          {search && (
            <Tooltip title={t('search')} placement="bottom">
              <ToolbarButton
                size="small"
                icon={<SearchOutlined />}
                color={searchbarVisible ? PRIMARY : void 0}
                onClick={toggleSearchbar}
              />
            </Tooltip>
          )}
          {add && <AddButton dataSource={add} />}
          {more && (
            <Popup
              trigger={['click']}
              placement="bottomRight"
              content={
                <MenuWrapper
                  prefixCls="ant-dropdown-menu"
                  selectable={false}
                  onClick={moreMenuClick}
                >
                  {more.items.map(({ key, text, prefix, suffix }) => (
                    <MenuListItem
                      key={key}
                      {...(prefix && { prefix })}
                      {...(suffix && { suffix })}
                    >
                      {text}
                    </MenuListItem>
                  ))}
                </MenuWrapper>
              }
            >
              <ToolbarButton size="small" icon={<MoreOutlined />} />
            </Popup>
          )}
        </Space>
      </Title>
      <Searchbar visible={searchbarVisible}>
        <Input
          className="search-input"
          prefix={<SearchOutlined className="icon" />}
          placeholder={t('searchValue')}
          bordered={false}
          onChange={onSearch}
        />
      </Searchbar>
    </Wrapper>
  );
}

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  &.hidden {
    display: none;
  }
`;

const Title = styled.div`
  display: flex;
  flex-shrink: 0;
  align-items: center;
  padding: 0 ${SPACE_XS} 0 ${SPACE_MD};
  line-height: ${LINE_HEIGHT_ICON_LG};

  h3 {
    flex: 1;
    padding: ${SPACE_MD} 0;
    overflow: hidden;
    font-size: ${FONT_SIZE_TITLE};
    font-weight: ${FONT_WEIGHT_MEDIUM};
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  h5 {
    flex: 1;
    padding: ${SPACE_XS} 0;
    overflow: hidden;
    font-size: ${FONT_SIZE_SUBTITLE};
    font-weight: ${FONT_WEIGHT_MEDIUM};
    color: ${p => p.theme.textColorLight};
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .back {
    padding: 0 ${SPACE_TIMES(0.5)};
    margin-right: ${SPACE};
    font-size: ${FONT_SIZE_TITLE};
    color: ${p => p.theme.textColorLight};
    cursor: pointer;

    &:hover {
      color: ${p => p.theme.textColorSnd};
      background-color: ${p => p.theme.bodyBackground};
    }
  }

  .searching {
    color: ${p => p.theme.primary};
  }
`;

const Searchbar = styled.div<{ visible: boolean }>`
  display: ${p => (p.visible ? 'block' : 'none')};
  padding: ${SPACE} 0;

  .search-input {
    padding: ${SPACE} ${SPACE_MD};

    .icon {
      margin-right: ${SPACE_TIMES(1.5)};
      color: ${p => p.theme.textColorDisabled};
    }
  }
`;
