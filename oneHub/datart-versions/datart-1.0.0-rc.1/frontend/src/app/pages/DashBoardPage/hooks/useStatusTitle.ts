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

import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { TITLE_SUFFIX } from 'globalConstants';
import { useMemo } from 'react';
export const useStatusTitle = (name: string, status: number) => {
  const t = useI18NPrefix(`viz.action`);
  const title = useMemo(() => {
    const base = name;
    const suffix = TITLE_SUFFIX[status] ? `[${t(TITLE_SUFFIX[status])}]` : '';
    return base + suffix;
  }, [name, status, t]);
  return title;
};
