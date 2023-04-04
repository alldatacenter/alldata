/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { useCallback } from 'react';
import { useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';

export function useStartAnalysis() {
  const history = useHistory();
  const orgId = useSelector(selectOrgId);

  const startAnalysis = useCallback(
    viewId => {
      history.push({
        pathname: `/organizations/${orgId}/vizs/chartEditor`,
        search: `dataChartId=&chartType=dataChart&container=dataChart&defaultViewId=${viewId}`,
      });
    },
    [history, orgId],
  );

  return startAnalysis;
}
