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

import { Link } from 'react-router-dom';
import React from 'react';
import { goTo } from 'common/utils';
import i18n, { isZh } from 'i18n';
import { map } from 'lodash';
import auditTemplates from './audit-templates.json'; // online editor: https://codesandbox.io/embed/staging-framework-4u3oh

const parseReg = /\[[@a-zA-Z\u4e00-\u9fa5]+](\([a-zA-Z]+\))?/g;
const replaceReg = /<<(\w+)>>/g;
const expressionReg = /{[^{}]+}/g;
// eslint-disable-next-line no-console
const logErr = console.error;
export default (record: AUDIT.Item, extraTemplates = {}) => {
  const templates = { ...auditTemplates, ...extraTemplates };
  const { templateName, scopeType, appId, projectId, context, result } = record;
  // 后端把pipelineID改为了pipelineId，兼容下
  const fullContext = { ...context, projectId, appId, env: 'test', pipelineID: context?.pipelineId, scopeType } as Obj;
  // excel中使用如下格式，链接参数需要在context同级或子级存在，前端定一个map，用urlKey对应到url链接，链接参数单独放一列
  // [@动态文字](urlKey) 或 [静态文字](urlKey) 或 [@动态文字]，示例：
  // 执行[流水线](pipeline) -> 执行[流水线](./project/{projectId}/app/{appId}/pipeline/{pipelineID})
  // 1. 执行[流水线](pipeline) -> 执行[流水线](./project/{projectId}/app/{appId}/pipeline/{pipelineID})
  // 2. 在项目[@projectName]中 -> 在项目projectName中
  // 3. 在应用 [@projectName](project) / [@appName](app) 中 -> 在应用[projectName](./project/{projectId})/[appName](./xx)中
  // 如果有<<scopeType>>，会替换为对应的scope
  // 支持{表达式}形式
  const parse = (str: string) => {
    let copyStr = str;
    const expressionList = copyStr.match(expressionReg);
    (expressionList || []).forEach((m) => {
      const expression = m.slice(1, -1);
      const parsedExp = expression.replaceAll('@', 'context.');
      let executeResult = '';
      try {
        // eslint-disable-next-line no-new-func
        executeResult = new Function('context', `return ${parsedExp}`)(fullContext);
      } catch (error) {
        logErr(`execute audit expression: ${expression} error in template: ${templateName}`);
      }
      copyStr = copyStr.replace(`{${expression}}`, executeResult);
    });
    const replaceList = copyStr.match(replaceReg);
    // <<dynamicKey>> 支持国际化,比如： <<issueType>> => <<replaceMap[context[issueType]]>>
    (replaceList || []).forEach((m) => {
      const key = m.slice(2, -2);
      const dynamicKey = fullContext[key];
      if (!dynamicKey) {
        logErr(`audit dynamicKey: ${key} not exist`, record);
        return;
      }
      const lowerDynamicKey = dynamicKey.toLowerCase();
      switch (key) {
        case 'scopeType': {
          const replaceMap = {
            sys: i18n.t('platform'),
            org: `${i18n.t('org')} [@orgName]`,
            project: `${i18n.t('project')} [@projectName](project) `,
            app: `${i18n.t('application')} [@projectName](project) / [@appName](app) `,
          };
          copyStr = copyStr.replace(`<<${key}>>`, replaceMap[lowerDynamicKey]);
          break;
        }
        case 'issueType': {
          const replaceMap = {
            epic: i18n.t('dop:milestone'),
            requirement: i18n.t('requirement'),
            task: i18n.t('task'),
            bug: i18n.t('bug'),
            ticket: i18n.t('dop:ticket'),
          };
          // 如果是需求池里的需求，把链接替换一下
          if (fullContext.iterationId === -1) {
            copyStr = copyStr.replace('(issueDetail)', '(backlog)');
          }
          copyStr = copyStr.replace(`<<${key}>>`, replaceMap[lowerDynamicKey]);
          break;
        }

        default:
          break;
      }
    });
    const matchList = copyStr.match(parseReg);
    if (!matchList) {
      return copyStr;
    }
    const contentList: any[] = [];
    let after = copyStr;
    (matchList || []).forEach((m, mIndex) => {
      // eg: [contextKey](urlKey)
      const [_contextKey, urlKey] = m.slice(1, -1).split('](');
      const isDynamic = _contextKey.startsWith('@');
      const contextKey = isDynamic ? _contextKey.slice(1) : _contextKey;
      const contextValue = isDynamic ? fullContext[contextKey] : contextKey;
      if (!contextValue) {
        logErr(
          `property [${contextKey}] not exist in context in audit template [${record.templateName}], record:`,
          record,
        );
      }
      if (urlKey && !goTo.resolve[urlKey]) {
        logErr(`audit urlKey: ${urlKey} not exist in goTo`, record);
        contentList.push({
          value: contextValue || '',
          Comp: (_props: { value: string }) => (
            <span key={`${String(mIndex)}-1`} className="font-bold">
              {_props.value}
            </span>
          ),
        });
        return;
      }
      const [before] = after.split(m);
      after = after.slice(before.length + m.length);
      contentList.push(before);
      // 如果是users，展示为用户头像列表
      if (_contextKey === '@users') {
        const userList = map(contextValue, (userInfo) => {
          const { nick, name } = userInfo;
          return nick || name;
        });
        contentList.push(userList.join(', '));
      } else if (urlKey) {
        contentList.push({
          value: contextValue,
          Comp: (_props: { value: string }) => (
            <Link
              key={`${String(mIndex)}-2`}
              target="_blank"
              className="font-bold"
              to={goTo.resolve[urlKey](fullContext)}
            >
              {_props.value}
            </Link>
          ),
        });
      } else {
        // 没有()部分，就只替换，不加链接
        contentList.push({
          value: contextValue,
          Comp: (_props: { value: string }) => (
            <span key={`${String(mIndex)}-3`} className="font-bold">
              {_props.value}
            </span>
          ),
        });
      }
    });
    return contentList.concat([after]);
  };

  const target = templates[templateName];
  if (!target) {
    logErr(`audit template:${templateName} not exist`);
    return templateName;
  }
  const tpl = result === 'success' ? target.success : target.fail;

  if (!tpl) {
    logErr(`audit template:${templateName}-${result} not exist, did you forgot to set 'result' field ?`);
    return templateName;
  }

  return parse(isZh() ? tpl.zh : tpl.en);
};
