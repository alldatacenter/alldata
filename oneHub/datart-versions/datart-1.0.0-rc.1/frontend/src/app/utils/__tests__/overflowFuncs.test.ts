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

import {
  getAutoFunnelTopPosition,
  getIntervalShow,
  hadAxisLabelOverflowConfig,
} from '../chartHelper';

describe('test getIntervalShow return boolean', () => {
  it('getIntervalShow return true when arg is number', () => {
    expect(getIntervalShow(0)).toBeTruthy();
  });

  it('1. getIntervalShow return true when arg is string', () => {
    expect(getIntervalShow('0')).toBeTruthy();
  });

  it('getIntervalShow return false when arg is "auto"', () => {
    expect(getIntervalShow('auto')).toBeFalsy();
  });

  // Interval 已经经过处理，不可能为 undefined
  it('getIntervalShow return false when arg is null', () => {
    expect(getIntervalShow(null)).toBeFalsy();
  });
});

describe('test hadAxisLabelOverflowConfig return boolean', () => {
  const axisLabel = {
    overflow: 'break',
    interval: 0,
    show: true,
  };

  const getOptions = (label: any = null, horizon = false) => ({
    [horizon ? 'yAxis' : 'xAxis']: label ? [label] : null,
  });

  it('hadAxisLabelOverflowConfig return false when options is null', () => {
    expect(hadAxisLabelOverflowConfig(getOptions())).toBeFalsy();
  });

  it('hadAxisLabelOverflowConfig return false when options.xAxis[0].axisLabel is null', () => {
    expect(hadAxisLabelOverflowConfig(getOptions({}))).toBeFalsy();
  });

  it('hadAxisLabelOverflowConfig return false when get options.yAxis opts', () => {
    expect(hadAxisLabelOverflowConfig(getOptions({}, true))).toBeFalsy();
  });

  it('hadAxisLabelOverflowConfig return true when get options.xAxis', () => {
    expect(hadAxisLabelOverflowConfig(getOptions({ axisLabel }))).toBeTruthy();
  });

  it('hadAxisLabelOverflowConfig return false when get options.yAxis opts', () => {
    expect(hadAxisLabelOverflowConfig(getOptions({}, true))).toBeFalsy();
  });

  it('hadAxisLabelOverflowConfig return true when get options.yAxis', () => {
    expect(
      hadAxisLabelOverflowConfig(getOptions({ axisLabel }, true), true),
    ).toBeTruthy();
  });

  it('hadAxisLabelOverflowConfig return false when show false', () => {
    expect(
      hadAxisLabelOverflowConfig(
        getOptions({
          axisLabel: {
            axisLabel,
            show: false,
          },
        }),
      ),
    ).toBeFalsy();
  });

  it('hadAxisLabelOverflowConfig return false when interval "auto"', () => {
    expect(
      hadAxisLabelOverflowConfig(
        getOptions({
          axisLabel: {
            axisLabel,
            interval: 'auto',
          },
        }),
      ),
    ).toBeFalsy();
  });

  it('hadAxisLabelOverflowConfig return false when overflow null"', () => {
    expect(
      hadAxisLabelOverflowConfig(
        getOptions({
          axisLabel: {
            axisLabel,
            overflow: null,
          },
        }),
      ),
    ).toBeFalsy();
  });
});

describe('test getAutoFunnelTopPosition return number', () => {
  it('getAutoFunnelTopPosition return 8 when legendPos is not left or right', () => {
    expect(
      getAutoFunnelTopPosition({
        legendPos: 'top',
      } as any),
    ).toEqual(8);
  });
  it('getAutoFunnelTopPosition return 16 when legendPos is left or right and height is 0 or null', () => {
    expect(
      getAutoFunnelTopPosition({
        legendPos: 'left',
      } as any),
    ).toEqual(16);

    expect(
      getAutoFunnelTopPosition({
        legendPos: 'left',
        height: 0,
      } as any),
    ).toEqual(16);
  });

  it('getAutoFunnelTopPosition return 16 when sort is ascending', () => {
    expect(
      getAutoFunnelTopPosition({
        legendPos: 'left',
        height: 32,
        sort: 'ascending',
      } as any),
    ).toEqual(16);
  });

  it('getAutoFunnelTopPosition return 16 when chart.getHeight return null/0', () => {
    expect(
      getAutoFunnelTopPosition({
        legendPos: 'left',
        height: 32,
        sort: 'none',
        chart: {
          getHeight: () => 0,
        },
      } as any),
    ).toEqual(16);
  });

  it('getAutoFunnelTopPosition return chart.getHeight - height - marginBottom', () => {
    expect(
      getAutoFunnelTopPosition({
        legendPos: 'left',
        height: 100,
        sort: 'none',
        chart: {
          getHeight: () => 800,
        },
      } as any),
    ).toEqual(800 - 100 - 24);
  });
});
