/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Tooltip } from 'antd';
import { IW } from 'app/components';
import { ChartDataSectionType } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import ChartManager from 'app/models/ChartManager';
import { IChart } from 'app/types/Chart';
import classnames from 'classnames';
import { FC, memo, useCallback } from 'react';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  FONT_SIZE_ICON_MD,
  SPACE_TIMES,
} from 'styles/StyleConstants';

const ChartGraphIcon: FC<{
  chart?: IChart;
  isActive?: boolean;
  isMatchRequirement?: boolean;
  onChartChange: (chart: IChart) => void;
}> = memo(({ chart, isActive, isMatchRequirement, onChartChange }) => {
  const t = useI18NPrefix(`viz.palette.graph`);

  const handleChartChange = useCallback(
    chartId => () => {
      const chart = ChartManager.instance().getById(chartId);

      if (!!chart) {
        onChartChange(chart);
      }
    },
    [onChartChange],
  );

  const renderIcon = ({
    ...args
  }: {
    iconStr;
    isMatchRequirement;
    isActive;
  }) => {
    if (/^<svg/.test(args?.iconStr) || /^<\?xml/.test(args?.iconStr)) {
      return <SVGImageRender {...args} />;
    }
    if (/svg\+xml;base64/.test(args?.iconStr)) {
      return <Base64ImageRender {...args} />;
    }
    return <SVGFontIconRender {...args} />;
  };

  const renderChartRequirements = requirements => {
    const lintMessages = requirements?.flatMap((requirement, index) => {
      return [ChartDataSectionType.Group, ChartDataSectionType.Aggregate].map(
        type => {
          const limit = requirement[type.toLocaleLowerCase()];
          const getMaxValueStr = limit =>
            !!limit && +limit >= 999 ? 'N' : limit;

          return (
            <li key={type + index}>
              {Number.isInteger(limit)
                ? t('onlyAllow', undefined, {
                    type: t(type),
                    num: getMaxValueStr(limit),
                  })
                : Array.isArray(limit) && limit.length === 2
                ? t('allowRange', undefined, {
                    type: t(type),
                    start: limit?.[0],
                    end: getMaxValueStr(limit?.[1]),
                  })
                : null}
            </li>
          );
        },
      );
    });
    return <ul>{lintMessages}</ul>;
  };

  return (
    <Tooltip
      key={chart?.meta?.id}
      title={
        <>
          {t(chart?.meta?.name!, true)}
          {renderChartRequirements(chart?.meta?.requirements)}
        </>
      }
    >
      <StyledChartIconWrapper
        fontSize={FONT_SIZE_ICON_MD}
        size={SPACE_TIMES(9)}
        className={classnames({
          active: isActive,
        })}
        onClick={handleChartChange(chart?.meta?.id)}
      >
        {renderIcon({
          iconStr: chart?.meta?.icon,
          isMatchRequirement,
          isActive,
        })}
      </StyledChartIconWrapper>
    </Tooltip>
  );
});

export default ChartGraphIcon;

const SVGFontIconRender = ({ iconStr, isMatchRequirement }) => {
  return (
    <StyledSVGFontIcon
      isMatchRequirement={isMatchRequirement}
      className={`iconfont icon-${!iconStr ? 'chart' : iconStr}`}
    />
  );
};

const SVGImageRender = ({ iconStr, isMatchRequirement, isActive }) => {
  const encodedStr = window.encodeURIComponent(iconStr);
  return (
    <StyledInlineSVGIcon
      alt="svg icon"
      style={{ height: FONT_SIZE_ICON_MD, width: FONT_SIZE_ICON_MD }}
      src={`data:image/svg+xml;utf8,${encodedStr}`}
      isMatchRequirement={isMatchRequirement}
    />
  );
};

const Base64ImageRender = ({ iconStr, isMatchRequirement, isActive }) => {
  return (
    <StyledBase64Icon
      alt="svg icon"
      style={{ height: FONT_SIZE_ICON_MD, width: FONT_SIZE_ICON_MD }}
      src={iconStr}
      isMatchRequirement={isMatchRequirement}
    />
  );
};

const StyledChartIconWrapper = styled(IW)`
  margin: ${SPACE_TIMES(0.5)};
  cursor: pointer;
  border-radius: ${BORDER_RADIUS};

  &:hover,
  &.active {
    color: ${p => p.theme.componentBackground};
    background-color: ${p => p.theme.primary};
  }
`;

const StyledInlineSVGIcon = styled.img<{ isMatchRequirement?: boolean }>`
  opacity: ${p => (p.isMatchRequirement ? 1 : 0.4)};
`;

const StyledSVGFontIcon = styled.i<{ isMatchRequirement?: boolean }>`
  opacity: ${p => (p.isMatchRequirement ? 1 : 0.4)};
`;

const StyledBase64Icon = styled.i<{
  isMatchRequirement?: boolean;
  alt: any;
  src: any;
}>`
  opacity: ${p => (p.isMatchRequirement ? 1 : 0.4)};
`;
