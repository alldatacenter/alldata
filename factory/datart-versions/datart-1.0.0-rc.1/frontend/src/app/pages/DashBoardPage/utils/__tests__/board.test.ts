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

import { BoardConfigValue } from '../../components/BoardProvider/BoardConfigProvider';
import { MIN_MARGIN, MIN_PADDING } from '../../constants';
import { getBoardMarginPadding } from '../board';

describe('test getBoardMarginPadding', () => {
  const boardConfig = {
    margin: [10, 10],
    padding: [20, 20],
    mMargin: [12, 12],
    mPadding: [13, 13],
  };
  it('should colsKey=lg ', () => {
    expect(
      getBoardMarginPadding(boardConfig as BoardConfigValue, 'lg'),
    ).toEqual({
      curMargin: boardConfig.margin,
      curPadding: boardConfig.padding,
    });
  });
  it('should colsKey=sm has mobileData', () => {
    expect(
      getBoardMarginPadding(boardConfig as BoardConfigValue, 'sm'),
    ).toEqual({
      curMargin: boardConfig.mMargin,
      curPadding: boardConfig.mPadding,
    });
  });
  it('should colsKey=sm has no mobileData', () => {
    const config = {
      ...boardConfig,
      mMargin: undefined,
      mPadding: undefined,
    };
    expect(getBoardMarginPadding(config as any, 'sm')).toEqual({
      curMargin: [MIN_MARGIN, MIN_MARGIN],
      curPadding: [MIN_PADDING, MIN_PADDING],
    });
  });
});
