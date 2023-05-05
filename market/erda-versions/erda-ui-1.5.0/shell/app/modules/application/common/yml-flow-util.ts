// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import PointComponentAbstract from 'application/common/components/point-component-abstract';
import { sortBy, uniqueId, forEach, get, omit, map, last } from 'lodash';
import i18n from 'i18n';

export const getEnvFromRefName = (branch: string) => {
  if (!branch) {
    return 'DEV';
  }

  if (branch.match(/^master$|^support/g)) {
    return 'PROD';
  } else if (branch.match(/^release|^hotfix/g)) {
    return 'STAGING';
  } else if (branch.match(/^develop$/g)) {
    return 'TEST';
  } else if (branch.match(/^feature/g)) {
    return 'DEV';
  }

  return 'DEV';
};

export const getAddonPlanCN = (plan = ''): string => {
  const [, config] = plan.split(':');

  switch (config) {
    case 'basic':
    case 'small':
      return i18n.t('dop:basic edition');
    case 'professional':
    case 'medium':
      return i18n.t('dop:professional edition');
    case 'ultimate':
    case 'large':
      return i18n.t('dop:ultimate edition');
    default:
      return i18n.t('dop:ultimate edition');
  }
};

export const convertAddonPlan = (config: string): string => {
  switch (config) {
    case 'small':
      return 'basic';
    case 'medium':
      return 'professional';
    case 'large':
      return 'ultimate';
    default:
      return config;
  }
};

export const sortByLineType = (list: any[]) => {
  return sortBy(list, (item: any) => item.type !== 'polyline');
};

let count = 0;
export const getItemName = () => {
  count += 1;
  return `${i18n.t('dop:new node')}${count}`;
};

export const randomId = () => {
  return uniqueId();
};

export const linePosition = {
  line(currentPoint: any, nextPoint: any, { info }: PointComponentAbstract<any, any>) {
    return {
      from: {
        x: currentPoint.position.x + Math.round(0.5 * info.ITEM_WIDTH),
        y: currentPoint.position.y + info.ITEM_HEIGHT,
      },
      to: {
        x: nextPoint.position.x + Math.round(0.5 * info.ITEM_WIDTH),
        y: nextPoint.position.y,
      },
    };
  },
  polyline(item: any, { info }: PointComponentAbstract<any, any>) {
    return [
      {
        x: item.from.x,
        y: item.from.y + Math.round(info.ITEM_MARGIN_BOTTOM / 2) - info.RX,
        a: `${getDefaultA(info.RX)} ${item.to.x - item.from.x > 0 ? 0 : 1} ${
          info.RX * (item.to.x - item.from.x > 0 ? 1 : -1)
        } ${info.RX}`,
      },
      {
        x: item.to.x + info.RX * (item.to.x - item.from.x > 0 ? -1 : 1),
        y: item.to.y - Math.round(info.ITEM_MARGIN_BOTTOM / 2),
        a: `${getDefaultA(info.RX)} ${item.to.x - item.from.x > 0 ? 1 : 0} ${
          info.RX * (item.to.x - item.from.x > 0 ? 1 : -1)
        } ${info.RX}`,
      },
    ];
  },
  polylineAcrossPoint(item: any, { info }: PointComponentAbstract<any, any>) {
    return [
      {
        x: item.from.x,
        y: item.from.y + Math.round(info.ITEM_MARGIN_BOTTOM / 2) - info.RX,
        a: `${getDefaultA(info.RX)} ${item.to.x - item.from.x > 0 ? 0 : 1} ${
          info.RX * (item.to.x - item.from.x > 0 ? 1 : -1)
        } ${info.RX}`,
      },
      {
        x:
          item.from.x +
          (item.to.x - item.from.x > 0 ? 1 : -1) * ((info.ITEM_MARGIN_RIGHT + info.ITEM_WIDTH) / 2 - info.RX),
        y: item.from.y + Math.round(info.ITEM_MARGIN_BOTTOM / 2),
        a: `${getDefaultA(info.RX)} ${item.to.x - item.from.x > 0 ? 1 : 0} ${
          info.RX * (item.to.x - item.from.x > 0 ? 1 : -1)
        } ${info.RX}`,
      },
      {
        x: item.from.x + ((item.to.x - item.from.x <= 0 ? -1 : 1) * (info.ITEM_MARGIN_RIGHT + info.ITEM_WIDTH)) / 2,
        y: item.to.y - Math.round(info.ITEM_MARGIN_BOTTOM / 2) - info.RX,
        a: `${getDefaultA(info.RX)} ${item.to.x - item.from.x >= 0 ? 0 : 1} ${
          info.RX * (item.to.x - item.from.x >= 0 ? 1 : -1)
        } ${info.RX}`,
      },
      {
        x: item.to.x + (item.to.x - item.from.x >= 0 ? -1 : 1) * info.RX,
        y: item.to.y - Math.round(info.ITEM_MARGIN_BOTTOM / 2),
        a: `${getDefaultA(info.RX)} ${item.to.x - item.from.x >= 0 ? 1 : 0} ${
          info.RX * (item.to.x - item.from.x >= 0 ? 1 : -1)
        } ${info.RX}`,
      },
    ];
  },
  top: {
    bottom(item: any, { info }: PointComponentAbstract<any, any>) {
      const dy = Math.abs(item.from.y - item.to.y);
      const dx = Math.abs(item.from.x - item.to.x);
      const onRight = item.to.x > item.from.x ? 1 : -1;

      const rx = (info.RX > dx / 2 ? dx / 2 : info.RX) / 2;

      return [
        {
          x: item.from.x,
          y: item.from.y - Math.round(dy / 2) + rx,
          a: `${getDefaultA(rx)} ${item.to.x > item.from.x ? 1 : 0} ${rx * onRight} ${-rx}`,
        },
        {
          x: item.to.x + -onRight * rx,
          y: item.from.y - Math.round(dy / 2),
          a: `${getDefaultA(rx)} ${item.to.x > item.from.x ? 0 : 1} ${onRight * rx} ${-rx}`,
        },
      ];
    },
    left(item: any, { info }: PointComponentAbstract<any, any>) {
      const dy = Math.abs(item.from.y - item.to.y);
      const dx = Math.abs(item.from.x - item.to.x);
      const rx = Math.abs(item.from.x - item.to.x);

      const secondX = item.to.x > item.from.x ? item.from.x + dx / 2 - info.RX : item.to.x - 20;
      const thirdX = item.to.x > item.from.x ? item.from.x + dx / 2 : item.to.x - info.RX - 20;
      if (rx / 2 <= info.RX) {
        return [
          {
            x: item.from.x,
            y: item.to.y + rx / 2,
            a: `${rx / 2} ${rx / 2} 0 0 1 ${rx / 2} ${-rx / 2}`,
          },
        ];
      }
      return [
        {
          x: item.from.x,
          y: item.from.y - Math.round(dy / 2) + info.RX,
          a: `${getDefaultA(info.RX)} ${item.to.x > item.from.x ? 1 : 0} ${
            (item.to.x > item.from.x ? 1 : -1) * info.RX
          } ${-info.RX}`,
        },
        {
          x: secondX,
          y: item.from.y - Math.round(dy / 2),
          a: `${getDefaultA(info.RX)} ${item.to.x > item.from.x ? 0 : 1} ${
            (item.to.x > item.from.x ? 1 : -1) * info.RX
          } ${-info.RX}`,
        },
        {
          x: thirdX,
          y: item.to.y + info.RX,
          a: `${getDefaultA(info.RX)} 1 ${info.RX} ${-info.RX}`,
        },
      ];
    },
    right(item: any, { info }: PointComponentAbstract<any, any>) {
      const dy = Math.abs(item.from.y - item.to.y);
      const dx = Math.abs(item.from.x - item.to.x);
      const rx = Math.abs(item.from.x - item.to.x);
      if (rx / 2 <= info.RX) {
        return [
          {
            x: item.from.x,
            y: item.to.y + rx / 2,
            a: `${rx / 2} ${rx / 2} 0 0 ${item.to.x - item.from.x > 0 ? 1 : 0} ${-rx / 2} ${-rx / 2}`,
          },
        ];
      }
      return [
        {
          x: item.from.x,
          y: item.from.y - Math.round(dy / 2) + info.RX,
          a: `${getDefaultA(info.RX)} ${item.to.x - item.from.x > 0 ? 1 : 0} ${
            (item.to.x > item.from.x ? 1 : -1) * info.RX
          } ${-info.RX}`,
        },
        {
          x: item.to.x > item.from.x ? item.to.x + info.RX : item.from.x - dx / 2,
          y: item.from.y - Math.round(dy / 2),
          a: `${getDefaultA(info.RX)} ${item.to.x > item.from.x ? 0 : 1} ${
            (item.to.x > item.from.x ? 1 : -1) * info.RX
          } ${-info.RX}`,
        },
        {
          x: item.to.x > item.from.x ? item.to.x + 2 * info.RX : item.from.x - dx / 2 - info.RX,
          y: item.to.y + info.RX,
          a: `${getDefaultA(info.RX)} 0 ${-info.RX} ${-info.RX}`,
        },
      ];
    },
  },
  bottom: {
    top(item: any, { info }: PointComponentAbstract<any, any>) {
      const dy = Math.abs(item.from.y - item.to.y);
      const dx = Math.abs(item.from.x - item.to.x);
      const onRight = item.to.x > item.from.x ? 1 : -1;
      if (dx / 2 < info.RX) {
        return [];
      }
      return [
        {
          x: item.from.x,
          y: item.from.y + Math.round(dy / 2) - info.RX,
          a: `${getDefaultA(info.RX)} ${item.to.x > item.from.x ? 0 : 1} ${info.RX * onRight} ${info.RX}`,
        },
        {
          x: item.to.x + (item.to.x > item.from.x ? -1 : 1) * info.RX,
          y: item.from.y + Math.round(dy / 2),
          a: `${getDefaultA(info.RX)} ${item.to.x > item.from.x ? 1 : 0} ${onRight * info.RX} ${info.RX}`,
        },
      ];
    },
    left(item: any, { info }: PointComponentAbstract<any, any>) {
      const dy = Math.abs(item.from.y - item.to.y);
      const dx = Math.abs(item.from.x - item.to.x);
      const secondX =
        item.from.x + (item.to.x > item.from.x ? 1 : -2) * (dx / 2) + (item.to.x > item.from.x ? 0 : -1) * info.RX;
      return [
        {
          x: item.from.x,
          y: item.from.y + Math.round(dy / 2) - info.RX,
          a: `${getDefaultA(info.RX)} ${item.to.x > item.from.x ? 0 : 1} ${
            info.RX * (item.to.x > item.from.x ? 1 : -1)
          } ${info.RX}`,
        },
        {
          x: secondX,
          y: item.from.y + Math.round(dy / 2),
          a: `${getDefaultA(info.RX)} ${item.to.x > item.from.x ? 1 : 0} ${
            (item.to.x > item.from.x ? 1 : -1) * info.RX
          } ${info.RX}`,
        },
        {
          x: secondX + (item.to.x > item.from.x ? 1 : -1) * info.RX,
          y: item.to.y - info.RX,
          a: `${getDefaultA(info.RX)} 0 ${info.RX} ${info.RX}`,
        },
      ];
    },
    right(item: any, { info }: PointComponentAbstract<any, any>) {
      const dy = Math.abs(item.from.y - item.to.y);
      const dx = Math.abs(item.from.x - item.to.x);
      return [
        {
          x: item.from.x,
          y: item.from.y + Math.round(dy / 2) - info.RX,
          a: `${getDefaultA(info.RX)} 1 ${info.RX * (item.to.x > item.from.x ? 1 : -1)} ${info.RX}`,
        },
        {
          x: item.from.x - dx / 2 + info.RX,
          y: item.from.y + Math.round(dy / 2),
          a: `${getDefaultA(info.RX)} 0 ${-info.RX} ${info.RX}`,
        },
        {
          x: item.from.x - Math.round(dx / 2),
          y: item.to.y - info.RX,
          a: `${getDefaultA(info.RX)} 1 ${-info.RX} ${info.RX}`,
        },
      ];
    },
  },
  left: {
    right(item: any, { info }: PointComponentAbstract<any, any>) {
      const dx = Math.abs(item.from.x - item.to.x);
      if (dx / 2 < info.RX) {
        return [
          {
            x: item.from.x - dx / 2,
            y: item.from.y,
            a: `${dx / 2} ${dx / 2} 0 0 ${item.to.y > item.from.y ? 1 : 0} ${-dx / 2} ${dx / 2}`,
          },
        ];
      }
      return [
        {
          x: item.from.x - dx / 2 + info.RX,
          y: item.from.y,
          a: `${getDefaultA(info.RX)} ${item.to.y > item.from.y ? 0 : 1} ${-info.RX} ${
            info.RX * (item.to.y > item.from.y ? 1 : -1)
          }`,
        },
        {
          x: item.from.x - dx / 2,
          y: item.to.y + (item.from.y > item.to.y ? 1 : -1) * info.RX,
          a: `${getDefaultA(info.RX)} ${item.to.y > item.from.y ? 1 : 0} ${-info.RX} ${
            info.RX * (item.to.y > item.from.y ? 1 : -1)
          }`,
        },
      ];
    },
    left(item: any, { info }: PointComponentAbstract<any, any>) {
      const dx = Math.abs(item.from.x - item.to.x);
      return [
        {
          x: item.from.x - dx / 2 + info.RX,
          y: item.from.y,
          a: `${getDefaultA(info.RX)} ${item.to.y > item.from.y ? 0 : 1} ${-info.RX} ${
            info.RX * (item.to.y > item.from.y ? 1 : -1)
          }`,
        },
        {
          x: item.from.x - dx / 2,
          y: item.to.y + (item.from.y > item.to.y ? 1 : -1) * info.RX,
          a: `${getDefaultA(info.RX)} ${item.to.y > item.from.y ? 1 : 0} ${-info.RX} ${
            info.RX * (item.to.y > item.from.y ? 1 : -1)
          }`,
        },
      ];
    },
    top(item: any, { info }: PointComponentAbstract<any, any>) {
      const dx = Math.abs(item.from.x - item.to.x);
      const dy = Math.abs(item.from.y - item.to.y);

      if (dx < info.RX || dy < info.RX) {
        return [
          {
            x: item.to.x + info.RX,
            y: item.from.y,
            a: `${getDefaultA(info.RX)} 0 ${-info.RX} ${info.RX}`,
          },
        ];
      }

      return [
        {
          x: item.from.x - dx / 2 + info.RX,
          y: item.from.y,
          a: `${getDefaultA(info.RX)} 0 ${-info.RX} ${info.RX}`,
        },
        {
          x: item.from.x - dx / 2,
          y: item.from.y + dy / 2 - info.RX,
          a: `${getDefaultA(info.RX)} 1 ${-info.RX} ${info.RX}`,
        },
        {
          x: item.to.x + info.RX,
          y: item.from.y + dy / 2,
          a: `${getDefaultA(info.RX)} 0 ${-info.RX} ${info.RX}`,
        },
      ];
    },
    bottom(item: any, { info }: PointComponentAbstract<any, any>) {
      const dx = Math.abs(item.from.x - item.to.x);
      const dy = Math.abs(item.from.y - item.to.y);
      if (dx < info.RX || dy < info.RX) {
        return [];
      }
      return [
        {
          x: item.from.x - dx / 2 + info.RX,
          y: item.from.y,
          a: `${getDefaultA(info.RX)} 1 ${-info.RX} ${-info.RX}`,
        },
        {
          x: item.from.x - dx / 2,
          y: item.from.y - dy / 2 + info.RX,
          a: `${getDefaultA(info.RX)} 0 ${-info.RX} ${-info.RX}`,
        },
        {
          x: item.to.x + info.RX,
          y: item.from.y - dy / 2,
          a: `${getDefaultA(info.RX)} 1 ${-info.RX} ${-info.RX}`,
        },
      ];
    },
  },
  right: {
    left(item: any, { info }: PointComponentAbstract<any, any>) {
      const dx = Math.abs(item.from.x - item.to.x);
      return [
        {
          x: item.from.x + dx / 2 - info.RX,
          y: item.from.y,
          a: `${getDefaultA(info.RX)} ${item.to.y - item.from.y > 0 ? 1 : 0} ${info.RX} ${
            (item.to.y > item.from.y ? 1 : -1) * info.RX
          }`,
        },
        {
          x: item.from.x + dx / 2,
          y: item.to.y + (item.from.y > item.to.y ? 1 : -1) * info.RX,
          a: `${getDefaultA(info.RX)} ${item.to.y - item.from.y > 0 ? 0 : 1} ${info.RX} ${
            info.RX * (item.to.y > item.from.y ? 1 : -1)
          }`,
        },
      ];
    },
    right(item: any, { info }: PointComponentAbstract<any, any>) {
      const dx = Math.abs(item.from.x - item.to.x);
      return [
        {
          x: item.from.x + dx / 2 - info.RX,
          y: item.from.y,
          a: `${getDefaultA(info.RX)} ${item.to.y - item.from.y > 0 ? 1 : 0} ${info.RX} ${
            (item.to.y > item.from.y ? 1 : -1) * info.RX
          }`,
        },
        {
          x: item.from.x + dx / 2,
          y: item.to.y + (item.from.y > item.to.y ? 1 : -1) * info.RX,
          a: `${getDefaultA(info.RX)} ${item.to.y - item.from.y > 0 ? 0 : 1} ${info.RX} ${
            info.RX * (item.to.y > item.from.y ? 1 : -1)
          }`,
        },
      ];
    },
    bottom(item: any, { info }: PointComponentAbstract<any, any>) {
      const dx = Math.abs(item.from.x - item.to.x);
      const dy = Math.abs(item.from.y - item.to.y);
      if (dx < info.RX || dy < info.RX) {
        return [];
      }
      const rx = (info.RX > dx / 2 ? dx / 2 : info.RX) / 2;
      return [
        {
          x: item.from.x + dx / 2 - rx,
          y: item.from.y,
          a: `${getDefaultA(rx)} 0 ${rx} ${-rx}`,
        },
        {
          x: item.from.x + dx / 2,
          y: item.from.y - dy / 2 + rx,
          a: `${getDefaultA(rx)} 1 ${rx} ${-rx}`,
        },
        {
          x: item.to.x - rx,
          y: item.from.y - dy / 2,
          a: `${getDefaultA(rx)} 0 ${rx} ${-rx}`,
        },
      ];
    },
    top(item: any, { info }: PointComponentAbstract<any, any>) {
      const dx = Math.abs(item.from.x - item.to.x);
      const dy = Math.abs(item.from.y - item.to.y);
      if (dx < info.RX || dy < info.RX) {
        return [];
      }
      const rx = (info.RX > dx / 2 ? dx / 2 : info.RX) / 2;
      return [
        {
          x: item.from.x + dx / 2 - rx,
          y: item.from.y,
          a: `${getDefaultA(rx)} 1 ${rx} ${rx}`,
        },
        {
          x: item.from.x + dx / 2,
          y: item.from.y + dy / 2 - rx,
          a: `${getDefaultA(rx)} 0 ${rx} ${rx}`,
        },
        {
          x: item.to.x - rx,
          y: item.from.y + dy / 2,
          a: `${getDefaultA(rx)} 1 ${rx} ${rx}`,
        },
      ];
    },
  },
};

/**
 * 获取 path arc 前段字符串模版
 */
const getDefaultA = (rx: number) => {
  return `${rx} ${rx} 0 0`;
};

const resourceUnitMap = {
  cpu: i18n.t('core'),
  mem: 'MB',
};

const convertJobsParams = (jobParams: { image: string; resources: any }, isCustomScript: boolean, task: IStageTask) => {
  const { image, resources } = jobParams;
  const filteredResources = omit(resources, 'disk');
  const readOnlyParams: any[] = [
    {
      name: 'image',
      readOnly: !isCustomScript,
      default: task.image || image,
    },
    {
      name: 'resources',
      type: 'struct',
      struct: Object.entries(filteredResources).map(([key, value]) => ({
        name: key,
        unit: resourceUnitMap[key],
        default: value,
      })),
    },
  ];
  return readOnlyParams;
};

export const mergeActionAndResource = (actionConfig: DEPLOY.ActionConfig | undefined, task: IStageTask) => {
  if (!actionConfig) {
    return null;
  }

  const result: any = {
    desc: actionConfig.readme,
    type: actionConfig.type,
    data: {},
  };

  const [jobParams] = Object.values(get(actionConfig, 'dice.jobs'));
  let configParams;
  const isCustomScript = actionConfig.name === 'custom-script';
  const readOnlyParams = convertJobsParams(jobParams, isCustomScript, task);
  const { resources } = task;
  if (resources) {
    const originResource = readOnlyParams[1].struct;
    readOnlyParams[1].struct = map(originResource, ({ name, ...rest }) => ({ ...rest, name, value: resources[name] }));
  }
  if (isCustomScript) {
    readOnlyParams.splice(1, 0, {
      name: 'commands',
      default: task.commands,
      type: 'string_array',
    });
  } else {
    configParams = get(actionConfig, 'spec.params') || [];
  }

  if (configParams) {
    result.data.params = {};
    forEach(configParams, (config: any) => {
      if (!task.params) {
        result.data.params[config.name] = {
          ...config,
          value: undefined,
        };
      } else if (config.type === 'struct' && Array.isArray(config.struct)) {
        const v = task.params[config.name];
        const subStruct = map(config.struct, (item) => {
          return { ...item, value: get(v, item.name) };
        });
        result.data.params[config.name] = {
          ...config,
          struct: subStruct,
        };
      } else {
        result.data.params[config.name] = {
          ...config,
          value: task.params[config.name],
        };
      }
    });
  }
  if (readOnlyParams) {
    forEach(readOnlyParams, (config: any) => {
      result.data[config.name] = {
        ...config,
      };
    });
  }
  if (isCustomScript) {
    result.data = readOnlyParams;
  }
  return result;
};

export const getResource = (task: IStageTask, actionConfig: DEPLOY.ActionConfig | undefined) => {
  if (!actionConfig) {
    return null;
  }
  return mergeActionAndResource(actionConfig, task);
};

const clearNullFromArray = (array: any[]) => {
  const deleteItems: number[] = [];
  array.forEach((i: any, index: number) => {
    if (typeof i === 'object') {
      const keys: string[] = Object.keys(i);
      let deleteCount = 0;
      keys.forEach((key: string) => {
        if (!i[key]) {
          deleteCount += 1;
          // eslint-disable-next-line no-param-reassign
          delete i[key];
        } else if (i[key] instanceof Array) {
          // eslint-disable-next-line no-param-reassign
          i[key] = clearNullFromArray(i[key]);
        } else if (typeof i[key] === 'object') {
          clearNullValue(i[key]);
        }
      });
      if (deleteCount === keys.length) {
        deleteItems.push(index);
      }
    }
  });

  return array.filter((_i: any, index: number) => !deleteItems.includes(index));
};

export const clearNullValue = (json: any) => {
  forEach(json, (value: any, key: string) => {
    if (value === undefined) {
      // eslint-disable-next-line no-param-reassign
      delete json[key];
    } else if (value instanceof Array) {
      // eslint-disable-next-line no-param-reassign
      json[key] = clearNullFromArray(value);
    } else if (typeof value === 'object') {
      clearNullValue(value);
    }
  });
};

export const isEqualCustomizer = (value: any, other: any): any => {
  if ((typeof value === 'function' && typeof other === 'function') || value === other) {
    return true;
  }
};

export const isDiceYml = (fileName: string) => {
  return fileName.match(/^dice_?\w*.yml$/g) !== null;
};
export const isPipelineYml = (fileName: string) => {
  return fileName.match(/^pipeline_?\w*\.yml$/g) !== null;
};
export const isInDiceDirectory = (path: string) => {
  return path.startsWith('.dice/pipelines/') || path === '.dice/pipelines';
};
export const isYml = (fileName: string) => {
  return fileName.endsWith('.yml') || fileName.endsWith('.yaml');
};

export enum WORK_FLOW_TYPE {
  NORMAL = 'normal',
  DICE = 'dice',
  PIPELINE = 'pipeline',
  WORKFLOW = 'workflow',
}

export const getWorkFlowType = (fileName: string): WORK_FLOW_TYPE => {
  if (fileName.match(/^dice_?\w*.yml$/g)) {
    return WORK_FLOW_TYPE.DICE;
  }

  if (fileName.match(/^pipeline_?\w*.yml$/g)) {
    return WORK_FLOW_TYPE.PIPELINE;
  }

  if (fileName.match(/^.*.workflow$/g)) {
    return WORK_FLOW_TYPE.WORKFLOW;
  }

  return WORK_FLOW_TYPE.NORMAL;
};

/**
 * 如果文件名为
 * dice_*.yaml、pipeline_*.yml、*.workflow
 * 则渲染为工作流视图
 */
export const needRenderWorkFlowView = (name: string) => {
  return getFileName(name).match(/^pipeline_?\w*\.yml$|^dice_?\w*\.yml$|^.*\.workflow$/g) !== null;
};

export const isPipelineWorkflowYml = (name: string) => {
  return getFileName(name).match(/^pipeline_?\w*\.yml$|^.*\.workflow$/g) !== null;
};

const getFileName = (name: string) => {
  let useName = name;
  if (name.includes('/')) useName = last(name.split('/')) as string;
  return useName;
};
