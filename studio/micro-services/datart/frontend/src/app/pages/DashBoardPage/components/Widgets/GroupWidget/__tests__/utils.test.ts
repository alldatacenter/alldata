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

import { RectConfig } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { getParentRect } from '../utils';

describe('getParentRect Test', () => {
  test('should get a rect by args', () => {
    const widgetMap = {
      a1: {
        id: 'a1',
        config: { rect: { x: 1, y: 1, width: 11, height: 11 } as RectConfig },
      },
      b2: {
        id: 'b2',
        config: { rect: { x: 5, y: 5, width: 55, height: 55 } as RectConfig },
      },
    };
    const ids = ['a1', 'b2'];

    const res = getParentRect({
      childIds: ids,
      widgetMap: widgetMap as any,
      preRect: { x: 0, y: 0, width: 5, height: 6 },
    });
    expect(res).toEqual({
      x: 1,
      y: 1,
      height: 59,
      width: 59,
    } as RectConfig);
  });
});
