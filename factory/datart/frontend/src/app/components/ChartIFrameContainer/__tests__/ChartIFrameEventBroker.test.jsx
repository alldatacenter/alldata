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

import ChartIFrameEventBroker from '../ChartIFrameEventBroker';

describe('ChartIFrameEventBroker Tests', () => {
  let broker = null;
  let mockOptions, mockContext;
  beforeEach(() => {
    mockOptions = { dataset: [], chartConfig: {} };
    mockContext = {
      window: {},
      document: {},
      width: 100,
      height: 100,
    };
    broker = new ChartIFrameEventBroker();
  });
  afterEach(() => {
    broker = null;
    mockOptions = null;
    mockContext = null;
  });

  test('should register chart lifecycle event correctly', () => {
    const chart = {
      onMount: jest.fn().mockImplementation(),
      onUpdated: jest.fn().mockImplementation(),
      onResize: jest.fn().mockImplementation(),
      onUnMount: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    expect(broker._chart).toEqual(chart);
    expect(broker._listeners.get('mounted')).toEqual(chart.onMount);
    expect(broker._listeners.get('updated')).toEqual(chart.onUpdated);
    expect(broker._listeners.get('resize')).toEqual(chart.onResize);
    expect(broker._listeners.get('unmount')).toEqual(chart.onUnMount);
  });

  test('should subscribe chart lifecycle event standalone', () => {
    const mountFn = jest.fn().mockImplementation();
    broker.subscribe('mounted', mountFn);
    expect(broker._listeners.get('mounted')).toEqual(mountFn);
  });

  test('should call chart onMount lifecycle when mounted', () => {
    const chart = {
      onMount: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('mounted', mockOptions, mockContext);
    expect(chart.onMount).toHaveBeenCalledTimes(1);
    expect(chart.onMount).toHaveBeenCalledWith(mockOptions, mockContext);
  });

  test('should call chart onUpdated lifecycle after mounted', async () => {
    const chart = {
      state: 'ready',
      onUpdated: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('updated', mockOptions, mockContext);

    // NOTE: the milliseconds is from EVENT_ACTION_DELAY_MS
    await new Promise(r => setTimeout(r, 300));
    expect(chart.onUpdated).toHaveBeenCalledTimes(1);
    expect(chart.onUpdated).toHaveBeenCalledWith(mockOptions, mockContext);
  });

  test('should invoke once onUpdated lifecycle with many update events', async () => {
    const chart = {
      state: 'ready',
      onUpdated: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('updated', mockOptions, mockContext);
    broker.publish('updated', mockOptions, mockContext);
    broker.publish('updated', mockOptions, mockContext);

    // NOTE: the milliseconds is from EVENT_ACTION_DELAY_MS
    await new Promise(r => setTimeout(r, 300));
    expect(chart.onUpdated).toHaveBeenCalledTimes(1);
    expect(chart.onUpdated).toHaveBeenCalledWith(mockOptions, mockContext);
  });

  test('should not call chart onUpdated lifecycle before mounted', () => {
    const chart = {
      onUpdated: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('updated', mockOptions, mockContext);
    expect(chart.onUpdated).toHaveBeenCalledTimes(0);
  });

  test('should call chart onResize lifecycle after mounted', async () => {
    const chart = {
      state: 'ready',
      onResize: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('resize', mockOptions, mockContext);

    // NOTE: the milliseconds is from EVENT_ACTION_DELAY_MS
    await new Promise(r => setTimeout(r, 300));
    expect(chart.onResize).toHaveBeenCalledTimes(1);
    expect(chart.onResize).toHaveBeenCalledWith(mockOptions, mockContext);
  });

  test('should invoke once onResize lifecycle with many resize events', async () => {
    const chart = {
      state: 'ready',
      onResize: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('resize', mockOptions, mockContext);
    broker.publish('resize', mockOptions, mockContext);
    broker.publish('resize', mockOptions, mockContext);

    // NOTE: the milliseconds is from EVENT_ACTION_DELAY_MS
    await new Promise(r => setTimeout(r, 300));
    expect(chart.onResize).toHaveBeenCalledTimes(1);
    expect(chart.onResize).toHaveBeenCalledWith(mockOptions, mockContext);
  });

  test('should not call chart onResize lifecycle before mounted', () => {
    const chart = {
      onResize: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('resize', mockOptions, mockContext);
    expect(chart.onResize).toHaveBeenCalledTimes(0);
  });

  test('should call chart onUnMount lifecycle after mounted', () => {
    const chart = {
      state: 'ready',
      onUnMount: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('unmount', mockOptions, mockContext);
    expect(chart.onUnMount).toHaveBeenCalledTimes(1);
    expect(chart.onUnMount).toHaveBeenCalledWith(mockOptions, mockContext);
  });

  test('should not call chart onUnMount lifecycle before mounted', () => {
    const chart = {
      onUnMount: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('unmount', mockOptions, mockContext);
    expect(chart.onUnMount).toHaveBeenCalledTimes(0);
  });

  test('should not call event when event name is not correct', () => {
    const chart = {
      state: 'mounting',
      onUnMount: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('unknown', mockOptions, mockContext);
    expect(chart.onUnMount).toHaveBeenCalledTimes(0);
  });

  test('should not call event after disposed', () => {
    const chart = {
      state: 'mounting',
      onUnMount: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.dispose();
    broker.publish('unknown', mockOptions, mockContext);
    expect(chart.onUnMount).toHaveBeenCalledTimes(0);
  });

  test('should set state to ready after mounted', () => {
    const chart = {
      state: '',
      onMount: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('mounted', mockOptions, mockContext);
    expect(chart.onMount).toHaveBeenCalledTimes(1);
    expect(chart.state).toEqual('ready');
  });

  test('should set state to ready after updated', async () => {
    const chart = {
      state: 'ready',
      onUpdated: jest.fn().mockImplementation(),
    };
    broker.register(chart);
    broker.publish('updated', mockOptions, mockContext);

    // NOTE: the milliseconds is from EVENT_ACTION_DELAY_MS
    await new Promise(r => setTimeout(r, 300));
    expect(chart.onUpdated).toHaveBeenCalledTimes(1);
    expect(chart.state).toEqual('ready');
  });
});
