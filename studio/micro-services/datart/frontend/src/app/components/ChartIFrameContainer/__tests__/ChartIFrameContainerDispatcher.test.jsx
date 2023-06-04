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

import ChartIFrameContainerDispatcher from '../ChartIFrameContainerDispatcher';

describe('ChartIFrameContainerDispatcher Test', () => {
  test('should get new instance if not init', () => {
    const instance = ChartIFrameContainerDispatcher.instance();
    expect(instance).not.toBeNull();
  });

  test('should create new container and show current container if not exist', () => {
    const instance = ChartIFrameContainerDispatcher.instance();
    const chart = {};
    const dataset = {};
    const config = {};
    const style = {};
    const containers = instance.getContainers(
      'id=1',
      chart,
      dataset,
      config,
      style,
    );
    expect(containers.length).toEqual(1);
    expect(containers[0].props.style).toEqual({
      transform: 'none',
      position: 'relative',
    });
  });

  test('should switch new container if id matched', () => {
    const instance = ChartIFrameContainerDispatcher.instance();
    const chart = {};
    const dataset = {};
    const config = {};
    const style = {};
    instance.getContainers('id=1', chart, dataset, config, style);
    const containers = instance.getContainers(
      'id=2',
      chart,
      dataset,
      config,
      style,
    );
    expect(containers.length).toEqual(2);
    expect(containers[0].props.style).toEqual({
      transform: 'translate(-9999px, -9999px)',
      position: 'absolute',
    });
    expect(containers[1].props.style).toEqual({
      transform: 'none',
      position: 'relative',
    });
  });
});
