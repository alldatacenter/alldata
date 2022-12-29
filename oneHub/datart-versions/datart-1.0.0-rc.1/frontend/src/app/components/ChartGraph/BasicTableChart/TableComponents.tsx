import { InfoCircleOutlined } from '@ant-design/icons';
import { Tooltip } from 'antd';
import React from 'react';
import { Resizable } from 'react-resizable';
import styled from 'styled-components/macro';
import { BLUE, LEVEL_1 } from 'styles/StyleConstants';

export const TableComponentsTd = ({
  children,
  useColumnWidth,
  ...rest
}: any) => {
  if (rest.className.includes('ellipsis') && useColumnWidth) {
    return (
      <Tooltip placement="topLeft" title={children}>
        <Td {...rest}>{children}</Td>
      </Tooltip>
    );
  }
  return <Td {...rest}>{children}</Td>;
};

export const ResizableTitle = props => {
  const { onResize, width, ...restProps } = props;

  if (!width) {
    return <th {...restProps} />;
  }

  return (
    <Resizable
      width={width}
      height={0}
      handle={
        <ResizableHandleStyle
          onClick={e => {
            e.stopPropagation();
          }}
        />
      }
      onResize={onResize}
    >
      <th {...restProps} />
    </Resizable>
  );
};

export const TableColumnTitle = props => {
  const { desc, title, uid } = props;
  return (
    <TableColumnTitleStyle key={uid}>
      <span className="titleStyle" key={uid + 'title'}>
        {title}
      </span>
      {desc && (
        <Tooltip placement="top" key={uid + 'desc'} title={desc}>
          <InfoCircleOutlined />
        </Tooltip>
      )}
    </TableColumnTitleStyle>
  );
};

const TableColumnTitleStyle = styled.span`
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  .titleStyle {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

const ResizableHandleStyle = styled.span`
  position: absolute;
  right: -5px;
  bottom: 0;
  z-index: ${LEVEL_1};
  width: 10px;
  height: 100%;
  cursor: col-resize;
`;
const Td = styled.td<any>`
  ${p =>
    p.isLinkCell
      ? `
    :hover {
      color: ${BLUE};
      cursor: pointer;
    }
    `
      : p.isJumpCell
      ? `
        :hover {
          color: ${BLUE};
          cursor: pointer;
          text-decoration: underline;
        }
      `
      : null}
`;
