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
import { Col, Progress, Row } from 'antd';
import { colorToRgb } from 'common/utils';
import ErdaIcon from 'common/components/erda-icon';
import Ellipsis from 'common/components/ellipsis';
import EmptyHoder from 'common/components/empty-holder';
import { functionalColor } from 'common/constants';
import './index.scss';

const CP_TopN: React.FC<CP_DATA_RANK.Props> = (props) => {
  const {
    customOp,
    props: configProps,
    data: { list },
  } = props || {};
  const handleClick = (item: CP_DATA_RANK.IItem) => {
    customOp?.clickRow?.(item);
  };

  return (
    <div className="cp-data-rank h-full">
      <Row gutter={8} {...configProps.rowsProps} className="h-full">
        {list.map((listItem, index) => {
          const { color = functionalColor.info, titleIcon, backgroundIcon } = configProps.theme?.[index] || {};
          const { title, items, span } = listItem;
          return (
            <Col key={title} span={span} className="my-1">
              <div
                className="px-4 py-3 relative h-full items-wrapper"
                style={{ backgroundColor: colorToRgb(color, 0.04) }}
              >
                <div
                  className="absolute top-0 right-0 bg-icon-wrapper flex justify-center items-center"
                  style={{ color: colorToRgb(color, 0.1) }}
                >
                  {titleIcon || backgroundIcon ? <ErdaIcon size={44} type={backgroundIcon ?? titleIcon} /> : null}
                </div>
                <div className="mb-2 flex justify-start items-center">
                  {titleIcon ? (
                    <span style={{ color }} className="mr-2 flex items-center">
                      <ErdaIcon size={16} type={titleIcon} />
                    </span>
                  ) : null}

                  <p className="mb-0 flex-1 text-purple-dark font-medium overflow-hidden overflow-ellipsis whitespace-nowrap text-purple-dark ">
                    {title}
                  </p>
                </div>
                <div>
                  {!items?.length ? (
                    <EmptyHoder relative />
                  ) : (
                    items?.map((item) => {
                      const { name, value, unit, id, percent } = item;
                      return (
                        <div
                          key={id}
                          className={`${customOp?.clickRow ? 'cursor-pointer' : ''} flex flex-col mb-2 last:mb-0`}
                          onClick={() => {
                            handleClick(item);
                          }}
                        >
                          <div className="flex py-1">
                            <Ellipsis className="flex-1 text-purple-dark" title={name} />
                            <div className="ml-2">
                              <span className="text-purple-dark">{value}</span>
                              {unit ? <span className="text-sub text-xs ml-0.5">{unit}</span> : null}
                            </div>
                          </div>
                          <Progress strokeColor={color} percent={percent} showInfo={false} strokeWidth={4} />
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </Col>
          );
        })}
      </Row>
    </div>
  );
};

export default CP_TopN;
