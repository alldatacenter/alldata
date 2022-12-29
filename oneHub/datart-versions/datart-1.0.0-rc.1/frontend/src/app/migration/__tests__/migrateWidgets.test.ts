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

import { FONT_DEFAULT } from 'app/constants';
import { ServerRelation } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import {
  beta0,
  beta4_2,
  convertWidgetRelationsToObj,
  RC0,
} from '../BoardConfig/migrateWidgets';
import { WidgetBeta3 } from '../BoardConfig/types';
import {
  APP_VERSION_BETA_0,
  APP_VERSION_BETA_4_1,
  APP_VERSION_BETA_4_2,
  APP_VERSION_RC_0,
} from '../constants';

describe('test migrateWidgets ', () => {
  test('should return undefined  when widget.config.type === filter', () => {
    const widget1 = {
      config: {
        type: 'filter',
      },
    };
    expect(beta0(widget1 as WidgetBeta3)).toBeUndefined();
  });
  test('should return self  when widget.config.type !== filter', () => {
    const widget2 = {
      config: {
        type: 'chart',
      },
    } as WidgetBeta3;
    expect(beta0(widget2 as WidgetBeta3)).toEqual(widget2);
  });

  test('should return widget.config.nameConfig', () => {
    const widget1 = {
      config: {},
    } as WidgetBeta3;
    const widget2 = {
      config: {
        nameConfig: FONT_DEFAULT,
        version: APP_VERSION_BETA_0,
      },
    } as any;
    expect(beta0(widget1 as WidgetBeta3)).toMatchObject(widget2);
  });

  test('should return Array Type about assistViewFields', () => {
    const widget1 = {
      config: {
        type: 'controller',
        content: {
          config: {
            assistViewFields: 'id1###id2',
          },
        },
      },
    };
    const widget2 = {
      config: {
        type: 'controller',
        content: {
          config: {
            assistViewFields: ['id1', 'id2'],
          },
        },
      },
    };
    expect(beta0(widget1 as unknown as WidgetBeta3)).toMatchObject(widget2);
  });

  test('convertWidgetRelationsToObj parse Relation.config', () => {
    const relations1 = [
      {
        targetId: '11',
        config: '{}',
        sourceId: '22',
      },
    ] as ServerRelation[];
    const relations2 = [
      {
        targetId: '11',
        config: {},
        sourceId: '22',
      },
    ] as ServerRelation[];
    expect(convertWidgetRelationsToObj(relations1)).toMatchObject(relations2);
  });

  test('should migrate when widget is owned chart for version APP_VERSION_BETA_4_2', () => {
    const widget1 = {
      config: {
        version: APP_VERSION_BETA_4_1,
        originalType: 'ownedChart',
        customConfig: {},
      },
    } as any;
    const result = beta4_2('auto', widget1 as any);
    expect(result?.config?.version).toBe(APP_VERSION_BETA_4_2);
    expect(result?.config?.customConfig?.interactions?.length).toBe(3);
  });

  test('should migrate when widget is linked chart for version APP_VERSION_BETA_4_2', () => {
    const widget1 = {
      config: {
        version: APP_VERSION_BETA_4_1,
        originalType: 'linkedChart',
        customConfig: {},
      },
    } as any;
    const result = beta4_2('auto', widget1 as any);
    expect(result?.config?.version).toBe(APP_VERSION_BETA_4_2);
    expect(result?.config?.customConfig?.interactions?.length).toBe(3);
  });

  test('should not migrate when widget version is APP_VERSION_BETA_4_2', () => {
    const widget1 = {
      config: {
        version: APP_VERSION_BETA_4_2,
        originalType: 'linkedChart',
        customConfig: {},
      },
    } as any;
    const result = beta4_2('auto', widget1 as any);
    expect(result?.config?.version).toBe(APP_VERSION_BETA_4_2);
    expect(result?.config?.customConfig?.interactions?.length).toBe(undefined);
  });

  test('should not migrate when widget is not chart', () => {
    const widget1 = {
      config: {
        version: APP_VERSION_BETA_4_2,
        originalType: 'controller',
        customConfig: {},
      },
    } as any;
    const result = beta4_2('auto', widget1 as any);
    expect(result?.config?.version).toBe(APP_VERSION_BETA_4_2);
    expect(result?.config?.customConfig?.interactions?.length).toBe(undefined);
  });

  test('should add name fields for widget computedFields ', () => {
    const widget = {
      config: {
        content: {
          dataChart: {
            config: {
              computedFields: [{ id: '1' }],
            },
          },
        },
      },
    };
    const result = RC0(widget as any);
    expect(result?.config?.content?.dataChart?.config?.version).toBe(
      APP_VERSION_RC_0,
    );
    expect(result).toMatchObject({
      config: {
        content: {
          dataChart: {
            config: {
              computedFields: [{ id: '1', name: '1' }],
              version: APP_VERSION_RC_0,
            },
          },
        },
      },
    });

    const widget1 = {
      config: {
        content: {
          dataChart: {
            config: {},
          },
        },
      },
    };
    const result1 = RC0(widget1 as any);

    expect(result1).toMatchObject({
      config: {
        content: {
          dataChart: {
            config: {},
          },
        },
      },
    });
  });
});
