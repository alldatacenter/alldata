import {
  BarChartOutlined,
  FolderFilled,
  FolderOpenFilled,
  FundFilled,
} from '@ant-design/icons';
import ChartManager from 'app/models/ChartManager';
import { useCallback } from 'react';
import styled from 'styled-components/macro';
import { FONT_SIZE_TITLE } from 'styles/StyleConstants';

function useGetVizIcon() {
  const chartManager = ChartManager.instance();
  const chartIcons = chartManager.getAllChartIcons();

  return useCallback(
    ({ relType, avatar, subType }) => {
      switch (relType) {
        case 'DASHBOARD':
          return subType !== null ? (
            renderIcon(subType === 'free' ? 'CombinedShape' : 'kanban')
          ) : (
            <FundFilled />
          );
        case 'DATACHART':
          return avatar ? renderIcon(chartIcons[avatar]) : <BarChartOutlined />;
        default:
          return p => (p.expanded ? <FolderOpenFilled /> : <FolderFilled />);
      }
    },
    [chartIcons],
  );
}

export default useGetVizIcon;

export const renderIcon = (iconStr: string) => {
  if (/^<svg/.test(iconStr) || /^<\?xml/.test(iconStr)) {
    return <SVGImageRender iconStr={iconStr} />;
  }
  if (/svg\+xml;base64/.test(iconStr)) {
    return <Base64ImageRender iconStr={iconStr} />;
  }
  return <SVGFontIconRender iconStr={iconStr} />;
};

const SVGFontIconRender = ({ iconStr }) => {
  return (
    <StyledSVGFontIcon
      className={`iconfont icon-${!iconStr ? 'chart' : iconStr}`}
    />
  );
};

const SVGImageRender = ({ iconStr }) => {
  const encodedStr = window.encodeURIComponent(iconStr);
  return (
    <StyledInlineSVGIcon
      alt="svg icon"
      style={{ height: FONT_SIZE_TITLE, width: FONT_SIZE_TITLE }}
      src={`data:image/svg+xml;utf8,${encodedStr}`}
    />
  );
};

const Base64ImageRender = ({ iconStr }) => {
  return (
    <StyledBase64Icon
      alt="svg icon"
      style={{ height: FONT_SIZE_TITLE, width: FONT_SIZE_TITLE }}
      src={iconStr}
    />
  );
};

const StyledInlineSVGIcon = styled.img``;

const StyledSVGFontIcon = styled.i``;

const StyledBase64Icon = styled.img``;
