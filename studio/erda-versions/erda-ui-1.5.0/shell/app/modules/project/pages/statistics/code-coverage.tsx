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
import DiceConfigPage from 'app/config-page';
import { getUrlQuery } from 'config-page/utils';
import { updateSearch } from 'common/utils';
import routeInfoStore from 'core/stores/route';

const CodeCoverage = () => {
  const [{ projectId }, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const [urlQuery, setUrlQuery] = React.useState(query);

  React.useEffect(() => {
    updateSearch({ ...urlQuery });
  }, [urlQuery]);

  const inParams = { projectId, ...urlQuery };

  const urlQueryChange = (val: Obj) => setUrlQuery((prev: Obj) => ({ ...prev, ...getUrlQuery(val) }));

  return (
    <DiceConfigPage
      scenarioType={'code-coverage'}
      scenarioKey={'code-coverage'}
      inParams={inParams}
      customProps={{
        envFilter: {
          op: {
            onStateChange: urlQueryChange,
          },
        },
        timeSelector: {
          op: {
            onChange: urlQueryChange,
          },
        },
      }}
    />
  );
};

export default CodeCoverage;
