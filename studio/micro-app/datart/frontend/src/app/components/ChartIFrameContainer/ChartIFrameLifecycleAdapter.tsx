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

import { LoadingOutlined } from '@ant-design/icons';
import { Spin } from 'antd';
import { useFrame } from 'app/components/ReactFrameComponent';
import { ChartLifecycle } from 'app/constants';
import usePrefixI18N from 'app/hooks/useI18NPrefix';
import { IChart } from 'app/types/Chart';
import { ChartConfig, SelectedItem } from 'app/types/ChartConfig';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import {
  CSSProperties,
  FC,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import styled from 'styled-components/macro';
import { uuidv4 } from 'utils/utils';
import ChartIFrameEventBroker from './ChartIFrameEventBroker';
import ChartIFrameResourceLoader from './ChartIFrameResourceLoader';

enum ContainerStatus {
  INIT,
  SUCCESS,
  FAILED,
  LOADING,
}

const ChartIFrameLifecycleAdapter: FC<{
  dataset: any;
  chart: IChart;
  config: ChartConfig;
  style: CSSProperties;
  isShown?: boolean;
  drillOption?: IChartDrillOption;
  selectedItems?: SelectedItem[];
  widgetSpecialConfig?: any;
  isLoadingData?: boolean;
}> = ({
  dataset,
  chart,
  config,
  style,
  isShown = true,
  drillOption,
  selectedItems,
  widgetSpecialConfig,
  isLoadingData = false,
}) => {
  const [chartResourceLoader] = useState(() => new ChartIFrameResourceLoader());
  const eventBrokerRef = useRef<ChartIFrameEventBroker>();
  const [containerStatus, setContainerStatus] = useState(ContainerStatus.INIT);
  const { document, window } = useFrame();
  const [containerId] = useState(() => `datart-${uuidv4()}`);
  const translator = usePrefixI18N();

  const buildBrokerOption = useCallback(() => {
    return {
      containerId,
      dataset,
      config,
      widgetSpecialConfig,
      drillOption,
      selectedItems,
    } as BrokerOption;
  }, [
    config,
    containerId,
    dataset,
    drillOption,
    selectedItems,
    widgetSpecialConfig,
  ]);

  const buildBrokerContext = useCallback(() => {
    return {
      document,
      window,
      width: style?.width,
      height: style?.height,
      translator,
    } as BrokerContext;
  }, [document, style?.height, style?.width, translator, window]);

  /**
   * Chart Mount Event
   * Dependency: 'chart?.meta?.id', 'isShown'
   */
  useEffect(() => {
    if (
      !isShown ||
      !chart ||
      !document ||
      !window ||
      !config ||
      containerStatus === ContainerStatus.LOADING
    ) {
      return;
    }

    setContainerStatus(ContainerStatus.LOADING);
    (async () => {
      chartResourceLoader
        .loadResource(document, chart?.getDependencies?.())
        .then(_ => {
          chart.init(config);
          const newBrokerRef = new ChartIFrameEventBroker();
          newBrokerRef.register(chart);
          newBrokerRef.publish(
            ChartLifecycle.Mounted,
            buildBrokerOption(),
            buildBrokerContext(),
          );
          eventBrokerRef.current = newBrokerRef;
          setContainerStatus(ContainerStatus.SUCCESS);
        })
        .catch(_ => {
          setContainerStatus(ContainerStatus.FAILED);
        });
    })();

    return function cleanup() {
      setContainerStatus(ContainerStatus.INIT);
      eventBrokerRef?.current?.publish(
        ChartLifecycle.UnMount,
        buildBrokerOption(),
        buildBrokerContext(),
      );
      eventBrokerRef?.current?.dispose();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chart?.meta?.id, isShown]);

  /**
   * Chart Update Event
   * Dependency: 'config', 'dataset', 'widgetSpecialConfig',
   * 'containerStatus', 'document', 'window', 'isShown', 'drillOption', 'selectedItems'
   */
  useEffect(() => {
    if (
      !isShown ||
      !document ||
      !window ||
      !config ||
      !dataset ||
      isLoadingData ||
      containerStatus !== ContainerStatus.SUCCESS
    ) {
      return;
    }
    eventBrokerRef.current?.publish(
      ChartLifecycle.Updated,
      buildBrokerOption(),
      buildBrokerContext(),
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    config,
    dataset,
    widgetSpecialConfig,
    containerStatus,
    document,
    window,
    isShown,
    drillOption,
    selectedItems,
    isLoadingData,
  ]);

  /**
   * Chart Resize Event
   * Dependency: 'style.width', 'style.height', 'document', 'window', 'isShown'
   */
  useEffect(() => {
    if (
      !isShown ||
      !document ||
      !window ||
      !config ||
      !dataset ||
      isLoadingData ||
      containerStatus !== ContainerStatus.SUCCESS
    ) {
      return;
    }

    eventBrokerRef.current?.publish(
      ChartLifecycle.Resize,
      buildBrokerOption(),
      buildBrokerContext(),
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    style.width,
    style.height,
    document,
    window,
    isShown,
    containerStatus,
    isLoadingData,
  ]);

  return (
    <Spin
      spinning={containerStatus === ContainerStatus.LOADING}
      indicator={<LoadingOutlined spin />}
      delay={500}
    >
      <StyledChartLifecycleAdapter
        id={containerId}
        style={{ width: style?.width, height: style?.height }}
      />
    </Spin>
  );
};

export default ChartIFrameLifecycleAdapter;

const StyledChartLifecycleAdapter = styled.div``;
