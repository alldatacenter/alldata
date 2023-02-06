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
import { goTo } from 'common/utils';
import RepoMRForm from './components/repo-mr-form';
import RepoCompareDetail from './components/compare-detail';
import i18n from 'i18n';

import './repo-mr-creation.scss';

const RepoMRCreation = () => {
  const [showDiff, toggleDiff] = React.useState(false);
  const diffRef = React.useRef(null) as any;

  const moveToDiff = () => {
    diffRef && diffRef.current.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div className="mr-creation">
      <RepoMRForm
        onOk={() => goTo('../../open')}
        onCancel={() => goTo('../')}
        onBranchChange={() => toggleDiff(false)}
        onShowComparison={() => toggleDiff(true)}
        moveToDiff={moveToDiff}
      />
      {showDiff && (
        <React.Fragment>
          <div className="section-title" ref={diffRef}>
            {i18n.t('comparison results')}
          </div>
          <div className="mr-compare-diff">
            <RepoCompareDetail hideComment disableComment />
          </div>
        </React.Fragment>
      )}
    </div>
  );
};

export { RepoMRCreation };
