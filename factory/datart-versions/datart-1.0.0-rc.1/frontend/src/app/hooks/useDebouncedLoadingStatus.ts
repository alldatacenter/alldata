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

import { useRef, useState } from 'react';

export const useDebouncedLoadingStatus = ({ isLoading }) => {
  const [lazyLoading, setLazyLoading] = useState(false);
  const timerIdRef = useRef();

  if (isLoading) {
    if (timerIdRef.current) {
      return lazyLoading;
    }
    timerIdRef.current = setTimeout(() => {
      setLazyLoading(true);
    }, 500) as any;
  } else {
    if (timerIdRef.current) {
      clearTimeout(timerIdRef.current);
      timerIdRef.current = undefined;
    }
    if (lazyLoading) {
      setLazyLoading(false);
    }
  }
  return lazyLoading;
};

export default useDebouncedLoadingStatus;
