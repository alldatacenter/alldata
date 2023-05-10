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
import { get, find, set, floor, isEmpty, reduce, flatten, map, filter } from 'lodash';
import i18n from 'i18n';
import './link-text.scss';

interface ILinkTextProps {
  textUnderLine: boolean;
  data: {
    showText: string;
    mqCount: any;
  };
  id: string;
  onHover: (...args: any) => void;
  outHover: (...args: any) => void;
}

const getOutCountTotal = (node: TOPOLOGY.INode, originData: TOPOLOGY.INode[]) => {
  const allParents = flatten(map(originData, 'parents'));
  const curOutList = filter(allParents, { id: node.id });
  return reduce(curOutList, (sum, { metric: { count } }) => count + sum, 0) || 1;
};

export const linkTextHoverAction = (isHover: boolean, textEle: any, external?: any) => {
  const { isRelative, hoverNode, targetNode, sourceNode, originData } = external;
  if (isEmpty(textEle)) return;
  // 当前为mqCount显示，则不展示百分比
  const mqCount = get(textEle, 'dataset.mqCount');
  const showText = get(textEle, 'dataset.showText');
  if (isHover && isRelative) {
    if (mqCount === true || mqCount === 'true') return;
    const inCountTotal = get(hoverNode, 'metric.count', 1);
    const outCountTotal = getOutCountTotal(hoverNode, originData.nodes);

    let percent = 0;
    const curParent: any = find(targetNode.parents, { id: sourceNode.id }) || { metric: { count: 0 } };
    // hover节点是起点，则计算占出的百分比
    if (hoverNode.id === sourceNode.id) {
      percent = (curParent.metric.count / outCountTotal) * 100;
    } else if (hoverNode.id === targetNode.id) {
      // hover节点是终点，则计算占入的百分比
      percent = (curParent.metric.count / inCountTotal) * 100;
    }
    set(textEle, 'innerHTML', `${showText}<br />${floor(percent, 2)}%`);
  } else {
    set(textEle, 'innerHTML', showText || '');
  }
};

const LinkText = (props: ILinkTextProps) => {
  const { textUnderLine, data, id, onHover, outHover } = props;
  const mqCount = get(data, 'metric.mqCount');
  const countText = mqCount ? i18n.t('consume count') : i18n.t('call count');
  const showText = `${countText}:${get(data, 'metric.count', 0)}`;
  return (
    <div
      onMouseEnter={onHover}
      data-mq-count={mqCount}
      onMouseLeave={outHover}
      className={`line-text ${textUnderLine ? 'under-line' : 'on-line'}`}
      data-show-text={showText}
      id={id}
    >
      {showText}
    </div>
  );
};

export default LinkText;
