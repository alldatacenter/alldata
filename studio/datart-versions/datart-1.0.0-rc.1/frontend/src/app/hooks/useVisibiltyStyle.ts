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

const useVisibilityStyle = (
  visibility = true,
): [{ transform: string; position: string }, Function] => {
  const toggle = isVisible => {
    return isVisible
      ? {
          transform: 'none',
          position: 'relative',
        }
      : {
          transform: 'translate(-9999px, -9999px)',
          position: 'absolute',
        };
  };

  const style = toggle(visibility);
  return [style, toggle];
};

export default useVisibilityStyle;
