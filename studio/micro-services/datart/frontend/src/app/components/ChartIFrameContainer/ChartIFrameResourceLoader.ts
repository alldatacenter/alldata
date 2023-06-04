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

import { loadScript } from 'app/utils/resource';

class ChartIFrameResourceLoader {
  private resources: string[] = [];

  loadResource(doc, deps?: string[]): Promise<any[]> {
    const unloadedDeps = (deps || []).filter(d => !this.resources.includes(d));
    return this.loadDependencies(doc, unloadedDeps);
  }

  private loadDependencies(doc, deps): Promise<any[]> {
    this.resources = this.resources.concat(deps);
    const loadableDeps = deps.map(d => loadScript(d, doc));
    return Promise.all(loadableDeps);
  }

  public dispose() {
    this.resources = [];
  }
}

export default ChartIFrameResourceLoader;
