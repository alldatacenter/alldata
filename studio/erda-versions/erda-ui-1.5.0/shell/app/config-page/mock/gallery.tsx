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

import { Col, Row } from 'antd';
import { camel2DashName } from 'app/common/utils';
import { ErrorBoundary, FileEditor } from 'common';
import React from 'react';
import { containerMap as componentsMap } from '../components';

const noop = () => null;
const Mock = () => {
  const [Comps, setComps] = React.useState({});
  React.useEffect(() => {
    Object.keys(componentsMap).map((key) => {
      const filename = camel2DashName(key);
      // /* @vite-ignore */
      import(`../components/${filename}/${filename}.mock`)
        .then((module) => {
          const mockData = module.default;
          let Component = componentsMap[key];
          Component = mockData ? Component : noop;
          // eslint-disable-next-line no-console
          console.log('mock data', key, mockData);
          setComps((prev) => ({
            ...prev,
            [key]: {
              Component,
              mockData,
            },
          }));
        })
        .catch(() => {
          // eslint-disable-next-line no-console
          console.log(`component missing mock data: `, filename);
        });
    });
  }, []);
  return (
    <div>
      {Object.keys(Comps).map((key) => {
        const Comp = Comps[key];
        const pureConfig = (Array.isArray(Comp.mockData) ? Comp.mockData : [Comp.mockData || {}]).map(
          ({ _meta, ...rest }: any) => rest,
        );
        return (
          <section className="component-gallery-item" key={key}>
            <h2>{key}</h2>
            <Row className="blame-content h-full">
              <Col span={16}>
                <ErrorBoundary>
                  {Array.isArray(Comp.mockData) ? (
                    Comp.mockData.map((mockItem: any, i) => {
                      return (
                        <div key={i} className="mt-2">
                          <h3>
                            {mockItem._meta.title} <span className="text-sm text-desc">{mockItem._meta.desc}</span>
                          </h3>
                          <Comp.Component {...mockItem} />
                        </div>
                      );
                    })
                  ) : (
                    <Comp.Component {...Comp.mockData} />
                  )}
                </ErrorBoundary>
              </Col>
              <Col span={8} className="h-full">
                <FileEditor
                  fileExtension={'json'}
                  value={JSON.stringify(pureConfig, null, 2)}
                  // onChange={(v) => console.log(v)}
                  maxLines={20}
                />
              </Col>
            </Row>
          </section>
        );
      })}
    </div>
  );
};
export default Mock;
