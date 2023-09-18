import { createGlobalStyle } from 'styled-components/macro';
import {
  FONT_WEIGHT_MEDIUM,
  SPACE,
  SPACE_MD,
  SPACE_SM,
  SPACE_XS,
} from 'styles/StyleConstants';

export const DashboardTabStyle = createGlobalStyle`
  .ant-tabs{
    padding: 0 ${SPACE_XS};
    user-select: none;
    background-color: ${p => p.theme.componentBackground};
    border-bottom: 1px solid ${p => p.theme.borderColorSplit};

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

    .ant-tabs-tab-remove {
      margin-right: -${SPACE_SM};
      visibility: hidden;
    }

    &.ant-tabs-card {
      &.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab,
      &.ant-tabs-card > div > .ant-tabs-nav .ant-tabs-tab {
        padding: ${SPACE_XS} ${SPACE_MD};
        margin: ${SPACE_SM} ${SPACE};
        font-weight: ${FONT_WEIGHT_MEDIUM};
        color: ${p => p.theme.textColorSnd};
        background-color: ${p => p.theme.componentBackground};
        border: none;
        border-radius: 0;

        &:hover {
          background-color: ${p => p.theme.bodyBackground};

          .ant-tabs-tab-remove {
            visibility: visible;
          }
        }
      }

      &.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab-active,
      &.ant-tabs-card > div > .ant-tabs-nav .ant-tabs-tab-active {
        background-color: ${p => p.theme.bodyBackground};
      }
    }
  }
`;
