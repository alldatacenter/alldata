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

import { useEffect, useState } from 'react';

/**
 * Lazy initialized state when dependency object has correct value.
 * @param {*} stateTransformer required, get/transform to new state value
 * @param {*} shouldUpdate required, if update new state compare function
 * with previous and current dependency value
 * @param {*} dependency required, dependency value
 * @param {*} defaultState optional, default state value
 * @return {*} [state, setState]
 */
const useComputedState: <TS>(
  stateTransformer: () => TS,
  shouldUpdate: (prev, next) => boolean,
  dependency: any,
  defaultState?: TS,
) => any = (stateTransformer, shouldUpdate, dependency, defaultState) => {
  const [prevDep, setPrevDep] = useState();
  const [state, setState] = useState(defaultState);

  useEffect(() => {
    if (shouldUpdate(prevDep, dependency)) {
      const newState = stateTransformer();
      setPrevDep(dependency);
      setState(newState);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dependency]);

  return [state, setState];
};

export default useComputedState;
