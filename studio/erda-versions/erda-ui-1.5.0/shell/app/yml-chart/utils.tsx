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

import i18n from 'i18n';
import { forEach, get, omit, map, isEmpty } from 'lodash';

export const isDiceYml = (fileName: string) => {
  return fileName.match(/^dice_?\w*.yml$/g) !== null;
};
export const isPipelineYml = (fileName: string) => {
  return fileName.match(/^pipeline_?\w*\.yml$/g) !== null;
};

// 此部分逻辑基本拷贝原来逻辑，方便后面如果整体删除原来代码；
const resourceUnitMap = {
  cpu: i18n.t('core'),
  mem: 'MB',
};

export const getDefaultVersionConfig = (actionConfigs: DEPLOY.ActionConfig[]) => {
  if (isEmpty(actionConfigs)) {
    return undefined;
  }
  const defaultConfig = actionConfigs.find((config) => config.isDefault);
  return defaultConfig || actionConfigs[0];
};

export const getResource = (task: IStageTask, actionConfig: DEPLOY.ActionConfig | undefined) => {
  if (!actionConfig) {
    return null;
  }
  return mergeActionAndResource(actionConfig, task);
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

  const [jobParams] = Object.values(get(actionConfig, 'dice.jobs') || {});
  let configParams;
  const isCustomScript = actionConfig.name === 'custom-script';
  const readOnlyParams = convertJobsParams(jobParams as any, isCustomScript, task);
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

const convertJobsParams = (jobParams: { image: string; resources: any }, isCustomScript: boolean, task: IStageTask) => {
  const { image, resources = {} } = jobParams || {};
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
