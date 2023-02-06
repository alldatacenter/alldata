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

import { Button } from 'antd';
import { useUpdate } from 'common/use-hooks';
import { goTo } from 'common/utils';
import React from 'react';
import { getSplitPathBy } from '../util';
import BranchSelect from './branch-select';
import i18n from 'i18n';
import { ErdaIcon } from 'common';
import './branch-compare.scss';
import routeInfoStore from 'core/stores/route';
import repoStore from 'application/stores/repo';

const RepoBranchCompare = () => {
  const [info] = repoStore.useStore((s) => [s.info]);
  const { branches: branch = '' } = routeInfoStore.useStore((s) => s.params);
  const [from, to] = branch.split('...');
  const [state, updater, update] = useUpdate({
    from: decodeURIComponent(from) || '',
    to: decodeURIComponent(to) || '',
  });
  React.useEffect(() => {
    update({
      from: decodeURIComponent(from),
      to: decodeURIComponent(to),
    });
  }, [from, to, update]);

  const switchBranch = () => {
    update({
      from: state.to,
      to: state.from,
    });
  };
  const onChange = (type: string) => (value: string) => {
    updater[type](value);
  };
  const goToCompare = () => {
    goTo(`${getSplitPathBy('compare').before}/${encodeURIComponent(state.from)}...${encodeURIComponent(state.to)}`);
  };
  const { branches, tags } = info;
  return (
    <div className="repo-branch-compare" key={window.location.pathname}>
      <BranchSelect {...{ branches, tags, current: encodeURIComponent(state.from) }} onChange={onChange('from')}>
        <span>{i18n.t('dop:based on source')}:</span>
        <span className="branch-name font-bold nowrap">{state.from || null}</span>
        {state.from ? <ErdaIcon type="caret-down" size="20" /> : null}
      </BranchSelect>
      <span className="switch-branch" onClick={switchBranch}>
        <ErdaIcon type="switch" />
      </span>
      <BranchSelect {...{ branches, tags, current: state.to }} onChange={onChange('to')}>
        <span>{i18n.t('compare')}:</span>
        <span className="branch-name font-bold nowrap">{state.to || null}</span>
        {state.to ? <ErdaIcon type="caret-down" size="20" /> : null}
      </BranchSelect>
      <Button className="compare-button" type="primary" onClick={goToCompare} disabled={!state.from || !state.to}>
        {i18n.t('compare')}
      </Button>
    </div>
  );
};

export default React.memo(RepoBranchCompare);
