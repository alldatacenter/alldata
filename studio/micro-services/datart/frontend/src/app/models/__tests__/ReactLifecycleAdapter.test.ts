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

import ReactLifecycleAdapter from '../ReactLifecycleAdapter';

jest.mock('react-dom', () => ({
  render: jest.fn(),
  unmountComponentAtNode: jest.fn(),
}));

jest.mock('react', () => ({
  createElement: (...args) => args,
}));

describe('ReactLifecycleAdapter Tests', () => {
  test('should get correct adapter', () => {
    const mockRealComponent = jest.fn();
    const dependencies = ['echarts'];
    const adapter = new ReactLifecycleAdapter(mockRealComponent);
    adapter.registerImportDependencies(dependencies);
    expect(adapter).not.toBeNull();
    expect(adapter.mounted).not.toBeUndefined();
    expect(adapter.resize).not.toBeUndefined();
    expect(adapter.updated).not.toBeUndefined();
    expect(adapter.unmount).not.toBeUndefined();
  });

  test('should invoke events', () => {
    const mockRealComponent = jest.fn();
    const dependencies = ['echarts'];
    const adapter = new ReactLifecycleAdapter(mockRealComponent);
    adapter.registerImportDependencies(dependencies);
    adapter.mounted(null, null, null);
    adapter.updated(null, null);
    adapter.resize(null, null);
    adapter.unmount();
    expect(adapter).not.toBeNull();
  });

  test('should render real component when it is not a function', () => {
    const mockRealComponent = '<div>Real</div>';
    const dependencies = ['echarts'];
    const adapter = new ReactLifecycleAdapter(mockRealComponent);
    adapter.registerImportDependencies(dependencies);
    adapter.mounted(null, null, null);
    expect(adapter).not.toBeNull();
  });
});
