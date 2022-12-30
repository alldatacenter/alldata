import { createGlobalStyle } from 'styled-components/macro';
import { SPACE_SM, SPACE_TIMES, SPACE_XS } from 'styles/StyleConstants';

export const EditorTabsStyle = createGlobalStyle`
  .ant-tabs {
    user-select: none;
    background-color: ${p => p.theme.bodyBackground};

    & > .ant-tabs-nav,
    & > div > .ant-tabs-nav {
      .ant-tabs-nav-more {
        padding: ${SPACE_XS} ${SPACE_SM};
      }
    }

    &.ant-tabs-top > .ant-tabs-nav,
    &.ant-tabs-bottom > .ant-tabs-nav,
    &.ant-tabs-top > div > .ant-tabs-nav,
    &.ant-tabs-bottom > div > .ant-tabs-nav {
      margin: 0;
    }

    &.ant-tabs-top > .ant-tabs-nav::before,
    &.ant-tabs-bottom > .ant-tabs-nav::before,
    &.ant-tabs-top > div > .ant-tabs-nav::before,
    &.ant-tabs-bottom > div > .ant-tabs-nav::before {
      border-bottom: 0;
    }

    &.ant-tabs-card {
      .ant-tabs-tab {
        min-width: ${SPACE_TIMES(30)};
      }

      .ant-tabs-tab-btn {
        flex: 1;
      }

      .ant-tabs-tab-btn:active,
      .ant-tabs-tab-btn:focus,
      .ant-tabs-tab-remove:active,
      .ant-tabs-tab-remove:focus {
        color: ${p => p.theme.textColor};
      }

      &.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab,
      &.ant-tabs-card > div > .ant-tabs-nav .ant-tabs-tab {
        padding: ${SPACE_SM} ${SPACE_XS} ${SPACE_SM} ${SPACE_SM};
        margin: 0 !important;
        color: ${p => p.theme.textColorLight};
        background-color: ${p => p.theme.bodyBackground};
        border: none;
        border-radius: 0;
      }

      &.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab-active,
      &.ant-tabs-card > div > .ant-tabs-nav .ant-tabs-tab-active {
        background-color: ${p => p.theme.componentBackground};
      }

      .ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn {
        color: ${p => p.theme.textColor};
      }
    }
  }
`;
