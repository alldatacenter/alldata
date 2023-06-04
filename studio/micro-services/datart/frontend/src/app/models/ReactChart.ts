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

import Chart from './Chart';
import ReactLifecycleAdapter from './ReactLifecycleAdapter';

export default class ReactChart extends Chart {
  private _adapter;

  constructor(wrapper, props) {
    super(
      props?.id || 'react-table',
      props?.name || '表格',
      props?.icon || 'table',
    );
    this._adapter = new ReactLifecycleAdapter(wrapper);
  }

  get adapter() {
    return this._adapter;
  }

  public onMount(options, context?): void {
    if (options.containerId === undefined || !context.document) {
      return;
    }
    this.adapter?.mounted(
      context.document.getElementById(options.containerId),
      options,
      context,
    );
  }

  public onUnMount(options, context?): void {
    this.adapter?.unmount();
  }
}
