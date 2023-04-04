import { LoadingOutlined } from '@ant-design/icons';
import { Empty, Tree as AntTree, TreeProps as AntTreeProps } from 'antd';
import classnames from 'classnames';
import styled from 'styled-components/macro';
import {
  FONT_SIZE_BODY,
  FONT_SIZE_TITLE,
  FONT_WEIGHT_MEDIUM,
  FONT_WEIGHT_REGULAR,
  SPACE,
  SPACE_TIMES,
  SPACE_XS,
} from 'styles/StyleConstants';

interface TreeProps extends AntTreeProps {
  loading: boolean;
}

export function Tree({ loading, treeData, ...treeProps }: TreeProps) {
  return (
    <Wrapper
      className={classnames({ container: loading || !treeData?.length })}
    >
      {loading ? (
        <LoadingOutlined />
      ) : (
        treeData &&
        (treeData.length ? (
          <StyledDirectoryTree
            showIcon
            blockNode
            treeData={treeData}
            {...treeProps}
          />
        ) : (
          <Empty />
        ))
      )}
    </Wrapper>
  );
}

export { TreeTitle } from './TreeTitle';

const Wrapper = styled.div`
  flex: 1;
  overflow-y: auto;

  &.container {
    display: flex;
    align-items: center;
    justify-content: center;
  }
`;

const StyledDirectoryTree = styled(AntTree)`
  &.ant-tree {
    font-weight: ${FONT_WEIGHT_MEDIUM};
    color: ${p => p.theme.textColorSnd};

    .ant-tree-switcher {
      line-height: 38px;
    }

    .ant-tree-switcher-noop:before {
      position: absolute;
      top: 50%;
      left: 50%;
      width: ${SPACE};
      height: ${SPACE};
      content: '';
      background-color: ${p => p.theme.borderColorEmphasis};
      border-radius: 50%;
      transform: translate(-50%, -50%);
    }

    .ant-tree-treenode {
      align-items: center;
      padding: 2px 0 2px ${SPACE_XS};

      .ant-tree-node-content-wrapper {
        display: flex;
        align-items: center;
        min-width: 0;
        padding-left: 0;
        line-height: 38px;

        &:hover {
          background: none;
        }

        .ant-tree-iconEle {
          display: flex;
          flex-shrink: 0;
          align-items: center;
          justify-content: center;
          color: ${p => p.theme.textColorDisabled};
        }

        .ant-tree-title {
          flex: 1;
          padding: 0 0 0 ${SPACE};
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        &.ant-tree-node-selected {
          font-weight: ${FONT_WEIGHT_MEDIUM};
          color: ${p => p.theme.primary};
          background: none;
        }

        .ant-tree-icon__docu {
          display: none;
        }
      }

      &:hover {
        color: ${p => p.theme.primary};

        .ant-tree-node-content-wrapper {
          .ant-tree-iconEle {
            color: ${p => p.theme.primary};
          }
        }
      }
    }

    .ant-tree-treenode-selected {
      background-color: ${p => p.theme.bodyBackground};

      .ant-tree-switcher {
        color: ${p => p.theme.primary};
      }

      &.ant-tree-treenode {
        .ant-tree-node-content-wrapper {
          .ant-tree-iconEle {
            color: ${p => p.theme.primary};
          }
        }
      }
    }

    .ant-tree-checkbox {
      margin-top: 0;
    }

    &.check-list {
      min-width: ${SPACE_TIMES(40)};
      padding: ${SPACE};
      color: ${p => p.theme.textColor};

      .ant-tree-switcher {
        display: none;
      }
    }

    &.medium {
      font-weight: ${FONT_WEIGHT_REGULAR};

      .ant-tree-switcher {
        width: ${SPACE_TIMES(5)};
        line-height: 28px;
      }

      .ant-tree-switcher-noop:before {
        width: 3px;
        height: 3px;
      }

      .ant-tree-treenode {
        .ant-tree-node-content-wrapper {
          line-height: 28px;

          .ant-tree-iconEle {
            width: ${SPACE_TIMES(5)};

            .iconfont,
            .anticon {
              font-size: ${FONT_SIZE_TITLE};
            }
          }
        }
      }

      .ant-tree-indent-unit {
        width: ${SPACE_TIMES(5)};
      }
    }

    &.small {
      font-weight: ${FONT_WEIGHT_REGULAR};

      .ant-tree-switcher {
        width: ${SPACE_TIMES(4)};
        line-height: 24px;
      }

      .ant-tree-switcher-noop:before {
        width: 3px;
        height: 3px;
      }

      .ant-tree-treenode {
        .ant-tree-node-content-wrapper {
          line-height: 24px;

          .ant-tree-iconEle {
            width: ${SPACE_TIMES(4)};

            .iconfont,
            .anticon {
              font-size: ${FONT_SIZE_BODY};
            }
          }
        }
      }

      .ant-tree-indent-unit {
        width: ${SPACE_TIMES(4)};
      }
    }
  }
`;
