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

import { FormGroupLayoutMode } from '../constants';
import * as utils from '../utils';

describe('itemLayoutComparer Tests', () => {
  test('should compare as different obj when translate changed', () => {
    const mockDatas = [1, 2];
    const result = utils.itemLayoutComparer(
      {
        ancestors: [],
        data: mockDatas,
        translate: (...rest) => {
          return '';
        },
      },
      {
        ancestors: [],
        data: mockDatas,
        translate: (...rest) => {
          return '';
        },
      },
    );
    expect(result).toBeFalsy();
  });

  test('should compare as different obj when data changed', () => {
    const mockTranslator: any = () => {};
    const result = utils.itemLayoutComparer(
      { ancestors: [], data: [1], translate: mockTranslator },
      { ancestors: [], data: [2], translate: mockTranslator },
    );
    expect(result).toBeFalsy();
  });

  test('should compare as different obj when dataConfigs changed', () => {
    const mockTranslator: any = () => {};
    const mockDatas = [1, 2];
    const result = utils.itemLayoutComparer(
      {
        ancestors: [],
        data: mockDatas,
        translate: mockTranslator,
        dataConfigs: [{ key: '1' }],
      },
      {
        ancestors: [],
        data: mockDatas,
        translate: mockTranslator,
        dataConfigs: [{ key: '2' }],
      },
    );
    expect(result).toBeFalsy();
  });

  test('should compare as same obj when onChange changed', () => {
    const mockDatas = [1, 2];
    const result = utils.itemLayoutComparer(
      {
        ancestors: [],
        data: mockDatas,
        onChange: (...rest) => {
          return '';
        },
      },
      {
        ancestors: [],
        data: mockDatas,
        onChange: (...rest) => {
          return '';
        },
      },
    );
    expect(result).toBeTruthy();
  });

  test('should compare as same obj when ancestors changed', () => {
    const mockTranslator: any = () => {};
    const mockDatas = [1, 2];
    const result = utils.itemLayoutComparer(
      { ancestors: [1], data: mockDatas, translate: mockTranslator },
      { ancestors: [2], data: mockDatas, translate: mockTranslator },
    );
    expect(result).toBeTruthy();
  });
});

describe('groupLayoutComparer Tests', () => {
  test('should compare as different obj when mode changed', () => {
    const mockDatas = [1, 2];
    const result = utils.groupLayoutComparer(
      {
        mode: FormGroupLayoutMode.INNER,
        ancestors: [],
        data: mockDatas,
      },
      {
        mode: FormGroupLayoutMode.OUTER,
        ancestors: [],
        data: mockDatas,
      },
    );
    expect(result).toBeFalsy();
  });

  test('should compare as different obj when dependency changed', () => {
    const mockDatas = [1, 2];
    const result = utils.groupLayoutComparer(
      {
        mode: FormGroupLayoutMode.INNER,
        dependency: 'a',
        ancestors: [],
        data: mockDatas,
      },
      {
        mode: FormGroupLayoutMode.INNER,
        dependency: 'b',
        ancestors: [],
        data: mockDatas,
      },
    );
    expect(result).toBeFalsy();
  });

  test('should be call itemLayoutComparer when mode and dependency not changed', () => {
    const result = utils.groupLayoutComparer(
      {
        mode: FormGroupLayoutMode.INNER,
        dependency: 'a',
        ancestors: [],
        data: [1],
      },
      {
        mode: FormGroupLayoutMode.INNER,
        dependency: 'a',
        ancestors: [],
        data: [2],
      },
    );
    expect(result).toBeFalsy();
  });
});

describe('invokeDependencyWatcher Tests', () => {
  test('should get empty value when action is null', () => {
    const result = utils.invokeDependencyWatcher(null, 'key', 1, []);
    expect(result).toEqual({});
  });

  test('should get safe values with permission keys', () => {
    const mockAction = obj => {
      return obj;
    };
    const result = utils.invokeDependencyWatcher(mockAction, 'key', 1, ['key']);
    expect(result).toEqual({ key: 1 });
  });

  test('should not get safe values without permission keys', () => {
    const mockAction = obj => {
      return obj;
    };
    const result = utils.invokeDependencyWatcher(mockAction, 'key', 1, []);
    expect(result).toEqual({});
  });
});
