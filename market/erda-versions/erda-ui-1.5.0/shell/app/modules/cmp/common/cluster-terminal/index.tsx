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
import { Button } from 'antd';

import { createTerm as createChannelTerm, destroyTerm as destoryChannelTerm, ITerminal } from './xterm-channel';
import { createTerm as createBinaryTerm, destroyTerm as destoryBinaryTerm } from './xterm-binary';
import { useEffectOnce } from 'react-use';
import './index.scss';
import i18n from 'i18n';

interface IProps {
  params: IWSParams;
  extraOptions?: JSX.Element[];
}

const terminalMap = {
  channel: {
    createTerm: createChannelTerm,
    destroyTerm: destoryChannelTerm,
  },
  binary: {
    createTerm: createBinaryTerm,
    destroyTerm: destoryBinaryTerm,
  },
};

interface IWSParams {
  url: string;
  subProtocol: 'channel' | 'binary';
}

export const ClusterTerminal = (props: IProps) => {
  const { params, extraOptions } = props;

  const terminalRef = React.useRef<HTMLDivElement>(null);
  const term = React.useRef<ITerminal>();
  const [max, setMax] = React.useState(false);
  const [initLoading, setInitLoading] = React.useState(true);

  React.useEffect(() => {
    term.current?.fit();
  }, [max]);

  const changeSize = () => setMax(!max);

  useEffectOnce(() => {
    if (terminalRef.current) {
      setTimeout(() => {
        term.current = terminalMap[params.subProtocol]?.createTerm(terminalRef.current, params);
        // wait for term init finished
        setTimeout(() => {
          term.current?.fit();
          setInitLoading(false);
        }, 0);
      }, 0);
    }
    return () => term.current && terminalMap[params.subProtocol]?.destroyTerm(term.current);
  });

  return (
    <div ref={terminalRef} className={`cluster-terminal-container ${max ? ' show-max' : ''}`}>
      {initLoading ? <span className="text-white">{i18n.t('connecting')}...</span> : null}
      <div className="cluster-terminal-control btn-line-rtl">
        <Button className="resize-button" onClick={changeSize} type="ghost">
          {max ? i18n.t('exit full screen') : i18n.t('full screen')}
        </Button>
        {extraOptions}
      </div>
    </div>
  );
};
