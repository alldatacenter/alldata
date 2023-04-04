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

import { ChartInteractionEvent } from 'app/constants';
import { KEYBOARD_EVENT_NAME } from 'globalConstants';
import { ChartSelectionManager } from '../ChartSelectionManager';

describe('ChartSelectionManager Test', () => {
  test('should init model', () => {
    const manager = new ChartSelectionManager([]);
    expect(manager).not.toBeNull();
    expect(manager.selectedItems).toEqual([]);
    expect(manager.isMultiSelect).toEqual(false);
  });

  test('should update selected items', () => {
    const manager = new ChartSelectionManager([]);
    const expectItem = {
      index: 'ssss',
      data: { rowData: { id: 1 } },
    };
    manager.updateSelectedItems([expectItem]);
    expect(manager).not.toBeNull();
    expect(manager.selectedItems).toEqual([expectItem]);
    expect(manager.isMultiSelect).toEqual(false);
  });

  test('should attach window listener', () => {
    const manager = new ChartSelectionManager([]);
    const mockWindow = {
      addEventListener: jest.fn(),
    } as any;
    manager.attachWindowListeners(mockWindow);
    expect(mockWindow.addEventListener.mock.calls.length).toBe(2);
    expect(mockWindow.addEventListener.mock.calls[0][0]).toBe('keydown');
    expect(mockWindow.addEventListener.mock.calls[1][0]).toBe('keyup');
  });

  test('should remove window listener', () => {
    const manager = new ChartSelectionManager([]);
    const mockWindow = {
      removeEventListener: jest.fn(),
    } as any;
    manager.removeWindowListeners(mockWindow);
    expect(mockWindow.removeEventListener.mock.calls.length).toBe(2);
    expect(mockWindow.removeEventListener.mock.calls[0][0]).toBe('keydown');
    expect(mockWindow.removeEventListener.mock.calls[1][0]).toBe('keyup');
  });

  test('should attach ZRender listener', () => {
    const manager = new ChartSelectionManager([]);
    const mockOnFunction = jest.fn();
    const mockChart = {
      getZr: () => ({
        on: mockOnFunction,
      }),
    } as any;
    manager.attachZRenderListeners(mockChart);
    expect(mockChart.getZr().on.mock.calls.length).toBe(1);
    expect(mockChart.getZr().on.mock.calls[0][0]).toBe('click');
  });

  test('should remove ZRender listener', () => {
    const manager = new ChartSelectionManager([]);
    const mockOnFunction = jest.fn();
    const mockChart = {
      getZr: () => ({
        off: mockOnFunction,
      }),
    } as any;
    manager.removeZRenderListeners(mockChart);
    expect(mockChart.getZr().off.mock.calls.length).toBe(1);
    expect(mockChart.getZr().off.mock.calls[0][0]).toBe('click');
  });

  test('should attach ECharts listener', () => {
    const manager = new ChartSelectionManager([]);
    const mockOnFunction = jest.fn();
    const mockChart = {
      on: mockOnFunction,
    } as any;
    manager.attachEChartsListeners(mockChart);
    expect(mockChart.on.mock.calls.length).toBe(1);
    expect(mockChart.on.mock.calls[0][0]).toBe('click');
  });

  test('should invoke callback with empty selected items when ZRender call click event', () => {
    const mockMouseEvents = [
      {
        name: 'click' as any,
        callback: jest.fn(),
      },
    ];
    let zRenderHandler;
    const onImpl = (_, fn) => {
      zRenderHandler = fn;
    };
    const manager = new ChartSelectionManager(mockMouseEvents);
    manager.updateSelectedItems([
      {
        index: 's',
        data: { rowData: { id: 1 } },
      },
    ]);
    const mockChart = {
      getZr: () => ({
        on: onImpl,
      }),
    } as any;
    manager.attachZRenderListeners(mockChart);
    zRenderHandler({});
    expect(mockMouseEvents[0].callback.mock.calls.length).toBe(1);
    expect(mockMouseEvents[0].callback.mock.calls[0][0]).toEqual({
      interactionType: ChartInteractionEvent.UnSelect,
      selectedItems: [],
    });
  });

  test('should not invoke callback when ZRender has target', () => {
    const mockMouseEvents = [
      {
        name: 'click' as any,
        callback: jest.fn(),
      },
    ];
    let zRenderHandler;
    const onImpl = (_, fn) => {
      zRenderHandler = fn;
    };
    const manager = new ChartSelectionManager(mockMouseEvents);
    manager.updateSelectedItems([
      {
        index: 's',
        data: { rowData: { id: 1 } },
      },
    ]);
    const mockChart = {
      getZr: () => ({
        on: onImpl,
      }),
    } as any;
    manager.attachZRenderListeners(mockChart);
    zRenderHandler({ target: {} });
    expect(mockMouseEvents[0].callback.mock.calls.length).toBe(0);
  });

  test('should not invoke callback when no selected', () => {
    const mockMouseEvents = [
      {
        name: 'click' as any,
        callback: jest.fn(),
      },
    ];
    let zRenderHandler;
    const onImpl = (_, fn) => {
      zRenderHandler = fn;
    };
    const manager = new ChartSelectionManager(mockMouseEvents);
    const mockChart = {
      getZr: () => ({
        on: onImpl,
      }),
    } as any;
    manager.attachZRenderListeners(mockChart);
    zRenderHandler({});
    expect(mockMouseEvents[0].callback.mock.calls.length).toBe(0);
  });

  test('should get multi-selected state when ctrl key + keydown event', () => {
    const manager = new ChartSelectionManager([]);
    let keydownHandler, keyupHandler;
    const mockWindow = {
      addEventListener: (event, handler) => {
        event === 'keydown'
          ? (keydownHandler = handler)
          : (keyupHandler = handler);
      },
    } as any;
    manager.attachWindowListeners(mockWindow);
    keydownHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
  });

  test('should get single-selected state when ctrl key + keyup event', () => {
    const manager = new ChartSelectionManager([]);
    let keydownHandler, keyupHandler;
    const mockWindow = {
      addEventListener: (event, handler) => {
        event === 'keydown'
          ? (keydownHandler = handler)
          : (keyupHandler = handler);
      },
    } as any;
    manager.attachWindowListeners(mockWindow);
    keydownHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
    keyupHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keyup' });
    expect(manager.isMultiSelect).toBe(false);
  });

  test('should get multi-selected state when command key + keydown event', () => {
    const manager = new ChartSelectionManager([]);
    let keydownHandler, keyupHandler;
    const mockWindow = {
      addEventListener: (event, handler) => {
        event === 'keydown'
          ? (keydownHandler = handler)
          : (keyupHandler = handler);
      },
    } as any;
    manager.attachWindowListeners(mockWindow);
    keydownHandler({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
  });

  test('should get single-selected state when command key + keyup event', () => {
    const manager = new ChartSelectionManager([]);
    let keydownHandler, keyupHandler;
    const mockWindow = {
      addEventListener: (event, handler) => {
        event === 'keydown'
          ? (keydownHandler = handler)
          : (keyupHandler = handler);
      },
    } as any;
    manager.attachWindowListeners(mockWindow);
    keydownHandler({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
    keyupHandler({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keyup' });
    expect(manager.isMultiSelect).toBe(false);
  });

  test('should get multi-selected state when ctrl/command key + keydown event multi-times', () => {
    const manager = new ChartSelectionManager([]);
    let keydownHandler, keyupHandler;
    const mockWindow = {
      addEventListener: (event, handler) => {
        event === 'keydown'
          ? (keydownHandler = handler)
          : (keyupHandler = handler);
      },
    } as any;
    manager.attachWindowListeners(mockWindow);
    keydownHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    keydownHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    keydownHandler({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keydown' });
    keydownHandler({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
  });

  test('should get single-selected state when ctrl/command event + keyup event multi-times', () => {
    const manager = new ChartSelectionManager([]);
    let keydownHandler, keyupHandler;
    const mockWindow = {
      addEventListener: (event, handler) => {
        event === 'keydown'
          ? (keydownHandler = handler)
          : (keyupHandler = handler);
      },
    } as any;
    manager.attachWindowListeners(mockWindow);
    keydownHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    expect(manager.isMultiSelect).toBe(true);
    keyupHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keyup' });
    keyupHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keyup' });
    keyupHandler({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keyup' });
    keyupHandler({ key: KEYBOARD_EVENT_NAME.COMMAND, type: 'keyup' });
    expect(manager.isMultiSelect).toBe(false);
  });

  test('should get one selected items when ECharts click event invoke', () => {
    const manager = new ChartSelectionManager([]);
    let clickEventHandler;
    const mockChart = {
      on: (_, handler) => (clickEventHandler = handler),
    } as any;
    manager.attachEChartsListeners(mockChart);
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([{ index: 'a,b', data: [1, 2] }]);
  });

  test('should change selected item when select another one with no key down event', () => {
    const manager = new ChartSelectionManager([]);
    let clickEventHandler;
    const mockChart = {
      on: (_, handler) => (clickEventHandler = handler),
    } as any;
    manager.attachEChartsListeners(mockChart);
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    clickEventHandler({ componentIndex: 'c', dataIndex: 'd', data: [1, 2] });
    expect(manager.selectedItems).toEqual([{ index: 'c,d', data: [1, 2] }]);
  });

  test('should get two selected items when select twice with ctrl key', () => {
    const manager = new ChartSelectionManager([]);
    let clickEventHandler;
    const mockChart = {
      on: (_, handler) => (clickEventHandler = handler),
    } as any;
    let keydownHandler, keyupHandler;
    const mockWindow = {
      addEventListener: (event, handler) => {
        event === 'keydown'
          ? (keydownHandler = handler)
          : (keyupHandler = handler);
      },
    } as any;
    manager.attachWindowListeners(mockWindow);
    manager.attachEChartsListeners(mockChart);
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    keydownHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    clickEventHandler({ componentIndex: 'c', dataIndex: 'd', data: [3, 4] });
    expect(manager.selectedItems).toEqual([
      { index: 'a,b', data: [1, 2] },
      { index: 'c,d', data: [3, 4] },
    ]);
  });

  test('should remove one when unselected one item with ctrl + keydown event', () => {
    const manager = new ChartSelectionManager([]);
    let clickEventHandler;
    const mockChart = {
      on: (_, handler) => (clickEventHandler = handler),
    } as any;
    let keydownHandler, keyupHandler;
    const mockWindow = {
      addEventListener: (event, handler) => {
        event === 'keydown'
          ? (keydownHandler = handler)
          : (keyupHandler = handler);
      },
    } as any;
    manager.attachWindowListeners(mockWindow);
    manager.attachEChartsListeners(mockChart);
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    keydownHandler({ key: KEYBOARD_EVENT_NAME.CTRL, type: 'keydown' });
    clickEventHandler({ componentIndex: 'c', dataIndex: 'd', data: [3, 4] });
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([{ index: 'c,d', data: [3, 4] }]);
  });

  test('should set empty when select one and unselect that', () => {
    const manager = new ChartSelectionManager([]);
    let clickEventHandler;
    const mockChart = {
      on: (_, handler) => (clickEventHandler = handler),
    } as any;
    let keydownHandler, keyupHandler;
    const mockWindow = {
      addEventListener: (event, handler) => {
        event === 'keydown'
          ? (keydownHandler = handler)
          : (keyupHandler = handler);
      },
    } as any;
    manager.attachWindowListeners(mockWindow);
    manager.attachEChartsListeners(mockChart);
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([]);
  });

  test('should get callback dat with selected state when ECharts click event invoke', () => {
    const mockMouseEvents = [
      {
        name: 'click' as any,
        callback: jest.fn(),
      },
    ];
    const manager = new ChartSelectionManager(mockMouseEvents);
    let clickEventHandler;
    const mockChart = {
      on: (_, handler) => (clickEventHandler = handler),
    } as any;
    manager.attachEChartsListeners(mockChart);
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([{ index: 'a,b', data: [1, 2] }]);
    expect(mockMouseEvents[0].callback.mock.calls.length).toBe(1);
    expect(mockMouseEvents[0].callback.mock.calls[0][0]).toEqual({
      componentIndex: 'a',
      dataIndex: 'b',
      data: [1, 2],
      interactionType: ChartInteractionEvent.Select,
      selectedItems: [{ index: 'a,b', data: [1, 2] }],
    });
  });

  test('should get callback data with unselected state when current selected items is empty', () => {
    const mockMouseEvents = [
      {
        name: 'click' as any,
        callback: jest.fn(),
      },
    ];
    const manager = new ChartSelectionManager(mockMouseEvents);
    let clickEventHandler;
    const mockChart = {
      on: (_, handler) => (clickEventHandler = handler),
    } as any;
    manager.attachEChartsListeners(mockChart);
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    clickEventHandler({ componentIndex: 'a', dataIndex: 'b', data: [1, 2] });
    expect(manager.selectedItems).toEqual([]);
    expect(mockMouseEvents[0].callback.mock.calls.length).toBe(2);
    expect(mockMouseEvents[0].callback.mock.calls[1][0]).toEqual({
      componentIndex: 'a',
      dataIndex: 'b',
      data: [1, 2],
      interactionType: ChartInteractionEvent.UnSelect,
      selectedItems: [],
    });
  });
});
