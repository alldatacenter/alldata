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
import i18n from 'i18n';
import { genEdges, genNodes } from 'msp/env-overview/topology/pages/topology/utils';
import ErdaIcon from 'common/components/erda-icon';
import './topology-overview.scss';

export type INodeKey = 'node' | 'service' | 'addon' | 'unhealthyService' | 'freeService' | 'circularDependencies';

interface IProps {
  data: { nodes: TOPOLOGY.INode[] };
  onClick: (data: INodeKey) => void;
}

const genEle = (nodes: TOPOLOGY.INode[]) => {
  const edge = genEdges(nodes);
  const node = genNodes(nodes, edge);
  return { node, edge };
};

const structure: { title: string; content: { key: INodeKey; name: string }[] }[] = [
  {
    title: i18n.t('msp:topology overview'),
    content: [
      {
        name: i18n.t('node'),
        key: 'node',
      },
      {
        name: i18n.t('service'),
        key: 'service',
      },
      {
        name: i18n.t('common:middleware'),
        key: 'addon',
      },
    ],
  },
  {
    title: i18n.t('msp:topology analysis'),
    content: [
      {
        name: i18n.t('msp:unhealthy service'),
        key: 'unhealthyService',
      },
      {
        name: i18n.t('msp:free service'),
        key: 'freeService',
      },
      {
        name: i18n.t('msp:circular dependency'),
        key: 'circularDependencies',
      },
    ],
  },
];

export const Title: React.FC = ({ children }) => {
  return (
    <div className="leading-8 text-white px-4 py-2 w-full overflow-hidden overflow-ellipsis whitespace-nowrap">
      {children}
    </div>
  );
};

interface ICardsProps {
  defaultActiveKey?: string;
  activeKey?: string;
  onClick?: (key: string) => void;
  canSelect?: boolean;
  list: {
    key: string;
    label: string;
    value: string | number;
    unit?: string;
  }[];
}

export const Cards: React.FC<ICardsProps> = ({ list, defaultActiveKey, onClick, activeKey, canSelect = true }) => {
  const [selectKey, setSelectKey] = React.useState(defaultActiveKey);
  React.useEffect(() => {
    setSelectKey(activeKey);
  }, [activeKey]);
  const handleClick = (key: string) => {
    if (!canSelect) {
      return;
    }
    if (!activeKey) {
      setSelectKey(key);
    }
    onClick?.(key);
  };
  return (
    <div className="flex justify-start flex-wrap items-center rounded-sm pl-3">
      {list.map((item) => {
        const { key, label, value, unit } = item;
        return (
          <div
            key={key}
            className={`flex-shrink-0 m-1 text-center card-item py-3 border border-solid ${
              canSelect && key === selectKey ? 'border-purple-deep' : 'border-transparent'
            } ${canSelect ? 'cursor-pointer hover:border-purple-deep' : ''}`}
            onClick={() => {
              handleClick(key);
            }}
          >
            <p className="text-white text-xl m-0 py-0.5">
              <span>{value}</span>
              {unit ? <span className="text-xs text-white-6 ml-1">{unit}</span> : null}
            </p>
            <p className="text-white-6 text-xs m-0 py-0.5">{label}</p>
          </div>
        );
      })}
    </div>
  );
};

const TopologyContent: React.FC<IProps> = ({ data, onClick }) => {
  const [selectKey, setSelectKey] = React.useState('node');
  const handleClick = (key: INodeKey) => {
    setSelectKey(key);
    onClick(key);
  };
  const values = React.useMemo<{ [key in INodeKey]: number }>(() => {
    const temp = {
      node: 0,
      addon: 0,
      service: 0,
      freeService: 0,
      unhealthyService: 0,
      circularDependencies: 0,
    };
    if (data.nodes) {
      const { node } = genEle(data.nodes);
      temp.node = node.length;
      node.forEach((item) => {
        if (item.data?.metaData) {
          const { parentCount, childrenCount, isCircular, isService, isUnhealthy, isAddon } = item.data;
          if (isUnhealthy && isService) {
            temp.unhealthyService = temp.unhealthyService + 1;
          }
          if (isService) {
            temp.service = temp.service + 1;
          }
          if (isAddon) {
            temp.addon = temp.addon + 1;
          }
          if (parentCount === 0 && childrenCount === 0 && isService) {
            temp.freeService = temp.freeService + 1;
          }
          if (isCircular) {
            temp.circularDependencies = temp.circularDependencies + 1;
          }
        }
      });
    }
    return temp;
  }, [data.nodes]);
  return (
    <div>
      {structure.map((item) => {
        const list = item.content.map((t) => {
          return {
            key: t.key,
            label: t.name,
            value: values[t.key] ?? 0,
          };
        });
        return (
          <div className="w-full mb-4 last:mb-0">
            <Title>{item.title}</Title>
            <Cards list={list} activeKey={selectKey} onClick={handleClick} />
          </div>
        );
      })}
    </div>
  );
};

export const TopologyOverviewWrapper: React.FC = ({ children }) => {
  const [expand, setExpand] = React.useState(true);
  return (
    <div className={`topology-overview relative ${expand ? 'expand' : 'collapse'}`}>
      <div
        className="absolute h-8 w-5 bg-white-1 top-1/2 -mt-4 -right-5 z-10 cursor-pointer flex justify-center items-center text-white-4 hover:text-white"
        onClick={() => {
          setExpand((prevState) => !prevState);
        }}
      >
        <ErdaIcon type={expand ? 'zuofan' : 'youfan'} />
      </div>
      <div className="content">{children}</div>
    </div>
  );
};

const TopologyOverview: React.FC<IProps> = (props) => {
  return (
    <TopologyOverviewWrapper>
      <TopologyContent {...props} />
    </TopologyOverviewWrapper>
  );
};

export default TopologyOverview;
