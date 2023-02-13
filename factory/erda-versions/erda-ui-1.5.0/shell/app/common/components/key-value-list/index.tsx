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

import React from 'react';
import { map } from 'lodash';
import Markdown from 'common/utils/marked';
import './index.scss';

interface IParam {
  data: any;
  title?: string;
  className?: string;
  depth?: number;
  shrink?: boolean;
  markdownTextFields?: string[];
  textRender?: (k: string, v: string) => string | JSX.Element | null;
  listRender?: (b: string[]) => React.ReactNodeArray | JSX.Element;
}
const KeyValueList = ({
  data,
  title,
  className = '',
  depth = 1,
  textRender,
  listRender,
  shrink = false,
  markdownTextFields = [],
  ...rest
}: IParam) => {
  return (
    <div
      key={(title || '') + depth}
      className={`key-value-list depth-${depth} ${className} ${shrink ? 'shrink' : ''}`}
      {...rest}
    >
      {title !== undefined && <div className="title">{title}</div>}
      {map(data, (v, k) => {
        if (typeof v === 'object') {
          if (Array.isArray(v)) {
            if (typeof v[0] === 'object') {
              return v.map((subV, i) =>
                KeyValueList({
                  data: subV,
                  title: i === 0 ? k : undefined,
                  className: `sub${i % 2 === 1 ? ' odd' : ''}`,
                  listRender,
                  textRender,
                  depth: depth + 1,
                }),
              );
            }
            return (
              <div className="k-v-row" key={k}>
                <span className="key">{k}</span>
                <span className="value"> {listRender ? listRender(v) : v.join(' , ')} </span>
              </div>
            );
          }
          // 普通类型放前面
          const normalType = {};
          const objType = {};
          map(v, (objV, objK) => {
            if (typeof objV === 'object') {
              objType[objK] = objV;
            } else {
              normalType[objK] = objV;
            }
          });

          return (
            <React.Fragment key={k}>
              {map(normalType, (normalV, normalK) => {
                return (
                  <div className="k-v-row" key={normalK}>
                    <span className="key">{normalK}</span>
                    {markdownTextFields.includes(normalK) ? (
                      <span
                        className="w-7/10 overflow-auto max-h-72"
                        dangerouslySetInnerHTML={{ __html: Markdown(normalV || '') }}
                      />
                    ) : (
                      <span className="value"> {textRender ? textRender(normalK, normalV) : String(normalV)} </span>
                    )}
                  </div>
                );
              })}
              {Object.keys(objType).length
                ? KeyValueList({
                    data: objType,
                    title: k,
                    className: 'sub',
                    listRender,
                    textRender,
                    depth: depth + 1,
                  })
                : null}
            </React.Fragment>
          );
        }
        return (
          <div className="k-v-row" key={k}>
            <span className="key">{k}</span>
            <span className="value"> {textRender ? textRender(k, v) : String(v)} </span>
          </div>
        );
      })}
    </div>
  );
};

export default KeyValueList;
