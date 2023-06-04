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

import useModal from 'antd/lib/modal/useModal';
import ChartDrillContextMenu from 'app/components/ChartDrill/ChartDrillContextMenu';
import ChartDrillPaths from 'app/components/ChartDrill/ChartDrillPaths';
import { ChartIFrameContainer } from 'app/components/ChartIFrameContainer';
import { InteractionMouseEvent } from 'app/components/FormGenerator/constants';
import { ChartInteractionEvent } from 'app/constants';
import useChartInteractions from 'app/hooks/useChartInteractions';
import useDebouncedLoadingStatus from 'app/hooks/useDebouncedLoadingStatus';
import useMount from 'app/hooks/useMount';
import useResizeObserver from 'app/hooks/useResizeObserver';
import ChartManager from 'app/models/ChartManager';
import useDisplayJumpVizDialog from 'app/pages/MainPage/pages/VizPage/hooks/useDisplayJumpVizDialog';
import useDisplayViewDetail from 'app/pages/MainPage/pages/VizPage/hooks/useDisplayViewDetail';
import { IChart } from 'app/types/Chart';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import {
  chartSelectionEventListener,
  drillDownEventListener,
  pivotTableDrillEventListener,
  tablePagingAndSortEventListener,
} from 'app/utils/ChartEventListenerHelper';
import { getChartDrillOption } from 'app/utils/internalChartHelper';
import { FC, memo, useCallback, useMemo, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { isEmptyArray } from 'utils/object';
import ChartDrillContext from '../../../contexts/ChartDrillContext';
import ControllerPanel from '../../MainPage/pages/VizPage/ChartPreview/components/ControllerPanel';
import {
  ChartPreview,
  FilterSearchParams,
} from '../../MainPage/pages/VizPage/slice/types';
import { HeadlessBrowserIdentifier } from '../components/HeadlessBrowserIdentifier';
import { shareActions } from '../slice';
import {
  selectHeadlessBrowserRenderSign,
  selectSelectedItems,
  selectShareExecuteTokenMap,
} from '../slice/selectors';
import {
  fetchShareDataSetByPreviewChartAction,
  updateFilterAndFetchDatasetForShare,
  updateGroupAndFetchDatasetForShare,
} from '../slice/thunks';

const TitleHeight = 100;

const ChartPreviewBoardForShare: FC<{
  chartPreview?: ChartPreview;
  orgId: string;
  filterSearchParams?: FilterSearchParams;
  availableSourceFunctions?: string[];
}> = memo(
  ({ chartPreview, orgId, filterSearchParams, availableSourceFunctions }) => {
    const dispatch = useDispatch();
    const drillOptionRef = useRef<IChartDrillOption>();
    const [chart] = useState<IChart | undefined>(() => {
      const currentChart = ChartManager.instance().getById(
        chartPreview?.backendChart?.config?.chartGraphId,
      );
      return currentChart;
    });
    const {
      ref,
      width = 0,
      height = 0,
    } = useResizeObserver<HTMLDivElement>({
      refreshMode: 'debounce',
      refreshRate: 500,
    });
    const { ref: controlRef, height: controlH = 0 } =
      useResizeObserver<HTMLDivElement>({
        refreshMode: 'debounce',
        refreshRate: 500,
      });
    const headlessBrowserRenderSign = useSelector(
      selectHeadlessBrowserRenderSign,
    );
    const selectedItems = useSelector(selectSelectedItems);
    const shareExecuteTokenMap = useSelector(selectShareExecuteTokenMap);
    const chartConfigRef = useRef(chartPreview?.chartConfig);
    const [openViewDetailPanel, viewDetailPanelContextHolder] =
      useDisplayViewDetail();
    const [openJumpVizDialogModal, openJumpVizDialogModalContextHolder] =
      useDisplayJumpVizDialog();
    const [jumpDialogModal, jumpDialogContextHolder] = useModal();
    const {
      getDrillThroughSetting,
      getViewDetailSetting,
      handleDrillThroughEvent,
      handleViewDataEvent,
    } = useChartInteractions({
      openViewDetailPanel: openViewDetailPanel as Function,
      openJumpUrlDialogModal: jumpDialogModal.info,
      openJumpVizDialogModal: openJumpVizDialogModal as Function,
    });
    const isLoadingData = useDebouncedLoadingStatus({
      isLoading: chartPreview?.isLoadingData,
    });

    useMount(() => {
      if (!chartPreview) {
        return;
      }
      drillOptionRef.current = getChartDrillOption(
        chartPreview?.chartConfig?.datas,
        drillOptionRef?.current,
      );
      dispatch(
        fetchShareDataSetByPreviewChartAction({
          preview: chartPreview,
          filterSearchParams,
        }),
      );
      registerChartEvents(chart);
    });

    const buildDrillThroughEventParams = useCallback(
      (
        clickEventParams,
        targetEvent: InteractionMouseEvent,
        ruleId?: string,
      ) => {
        const drillThroughSetting = getDrillThroughSetting(
          chartConfigRef?.current?.interactions,
          [],
        );
        return {
          drillOption: drillOptionRef?.current,
          drillThroughSetting,
          clickEventParams,
          targetEvent,
          orgId,
          view: {
            id: chartPreview?.backendChart?.view?.id || '',
            config: chartPreview?.backendChart?.view.config || {},
            meta: chartPreview?.backendChart?.view.meta,
            computedFields:
              chartPreview?.backendChart?.config.computedFields || [],
          },
          queryVariables: chartPreview?.backendChart?.queryVariables || [],
          computedFields: chartPreview?.backendChart?.config.computedFields,
          aggregation: chartPreview?.backendChart?.config?.aggregation,
          chartConfig: chartConfigRef?.current,
          ruleId,
          isJumpUrlOnly: true,
        };
      },
      [orgId, chartPreview, getDrillThroughSetting],
    );

    const buildViewDataEventParams = useCallback(
      (clickEventParams, targetEvent: InteractionMouseEvent) => {
        const viewDetailSetting = getViewDetailSetting(
          chartConfigRef?.current?.interactions,
          [],
        );
        const view = {
          id: chartPreview?.backendChart?.view?.id || '',
          config: chartPreview?.backendChart?.view.config || {},
          meta: chartPreview?.backendChart?.view.meta,
          computedFields:
            chartPreview?.backendChart?.config.computedFields || [],
        };
        return {
          drillOption: drillOptionRef.current,
          clickEventParams,
          targetEvent,
          viewDetailSetting,
          chartConfig: chartConfigRef?.current,
          view,
        };
      },
      [chartPreview?.backendChart, getViewDetailSetting],
    );

    const chartRightClickViewDetailSetting = useMemo(() => {
      const viewDetailSetting = getViewDetailSetting(
        chartPreview?.chartConfig?.interactions,
        [],
      );
      const hasSelectedItems = !isEmptyArray(selectedItems);
      return viewDetailSetting?.event === InteractionMouseEvent.Right &&
        hasSelectedItems
        ? viewDetailSetting
        : undefined;
    }, [
      chartPreview?.chartConfig?.interactions,
      getViewDetailSetting,
      selectedItems,
    ]);

    const chartRightClickDrillThroughSetting = useMemo(() => {
      const drillThroughSetting = getDrillThroughSetting(
        chartConfigRef?.current?.interactions,
        [],
      );
      const hasSelectedItems = !isEmptyArray(selectedItems);
      return Boolean(
        drillThroughSetting?.rules?.filter(
          r => r.event === InteractionMouseEvent.Right,
        ).length,
      ) && hasSelectedItems
        ? drillThroughSetting!
        : undefined;
    }, [getDrillThroughSetting, selectedItems]);

    const handleDrillThroughChange = useCallback(() => {
      const drillThroughSetting = getDrillThroughSetting(
        chartPreview?.chartConfig?.interactions,
        [],
      );
      if (!drillThroughSetting) {
        return;
      }

      return ruleId => {
        const rightClickEvent = {
          selectedItems: selectedItems,
        };
        handleDrillThroughEvent(
          buildDrillThroughEventParams(
            rightClickEvent,
            InteractionMouseEvent.Right,
            ruleId,
          ),
        );
      };
    }, [
      chartPreview?.chartConfig?.interactions,
      selectedItems,
      getDrillThroughSetting,
      handleDrillThroughEvent,
      buildDrillThroughEventParams,
    ]);

    const handleViewDataChange = useCallback(() => {
      if (!chartRightClickViewDetailSetting) {
        return;
      }
      return () => {
        const rightClickEvent = {
          selectedItems: selectedItems,
        };
        handleViewDataEvent(
          buildViewDataEventParams(
            rightClickEvent,
            InteractionMouseEvent.Right,
          ),
        );
      };
    }, [
      chartRightClickViewDetailSetting,
      selectedItems,
      handleViewDataEvent,
      buildViewDataEventParams,
    ]);

    const registerChartEvents = chart => {
      chart?.registerMouseEvents([
        {
          name: 'click',
          callback: param => {
            if (param?.interactionType === ChartInteractionEvent.PagingOrSort) {
              tablePagingAndSortEventListener(param, p => {
                dispatch(
                  fetchShareDataSetByPreviewChartAction({
                    ...p,
                    preview: chartPreview!,
                    filterSearchParams,
                  }),
                );
              });
              return;
            }
            handleDrillThroughEvent(
              buildDrillThroughEventParams(param, InteractionMouseEvent.Left),
            );
            handleViewDataEvent(
              buildViewDataEventParams(param, InteractionMouseEvent.Left),
            );
            drillDownEventListener(drillOptionRef?.current, param, p => {
              drillOptionRef.current = p;
              handleDrillOptionChange?.(p);
            });
            pivotTableDrillEventListener(param, p => {
              handleDrillOptionChange(p);
            });
            chartSelectionEventListener(param, p => {
              dispatch(shareActions.changeSelectedItems(p));
            });
          },
        },
      ]);
    };

    const handleFilterChange = (type, payload) => {
      dispatch(
        updateFilterAndFetchDatasetForShare({
          backendChartId: chartPreview?.backendChart?.id!,
          chartPreview,
          payload,
          drillOption: drillOptionRef?.current,
        }),
      );
    };

    const handleDrillOptionChange = (option: IChartDrillOption) => {
      drillOptionRef.current = option;
      dispatch(
        updateFilterAndFetchDatasetForShare({
          backendChartId: chartPreview?.backendChart?.id!,
          chartPreview,
          payload: null,
          drillOption: drillOptionRef?.current,
        }),
      );
    };

    const handleDateLevelChange = (type, payload) => {
      dispatch(
        updateGroupAndFetchDatasetForShare({
          backendChartId: chartPreview?.backendChart?.id!,
          payload: payload,
          drillOption: drillOptionRef?.current,
        }),
      );
    };
    const dataset = useMemo(() => {
      if (
        !chartPreview?.backendChart?.viewId &&
        chartPreview?.backendChart?.config.sampleData
      ) {
        return chartPreview?.backendChart?.config.sampleData;
      }
      return chartPreview?.dataset;
    }, [
      chartPreview?.backendChart?.config.sampleData,
      chartPreview?.backendChart?.viewId,
      chartPreview?.dataset,
    ]);

    return (
      <StyledChartPreviewBoardForShare>
        <div ref={controlRef}>
          <ControllerPanel
            viewId={chartPreview?.backendChart?.viewId}
            chartConfig={chartPreview?.chartConfig}
            onChange={handleFilterChange}
            view={chartPreview?.backendChart?.view}
            executeToken={shareExecuteTokenMap}
          />
        </div>
        <ChartDrillContext.Provider
          value={{
            drillOption: drillOptionRef.current,
            availableSourceFunctions,
            viewDetailSetting: chartRightClickViewDetailSetting,
            drillThroughSetting: chartRightClickDrillThroughSetting,
            onDrillOptionChange: handleDrillOptionChange,
            onDateLevelChange: handleDateLevelChange,
            onDrillThroughChange: handleDrillThroughChange(),
            onViewDataChange: handleViewDataChange(),
          }}
        >
          <div style={{ width: '100%', height: '100%' }} ref={ref}>
            <ChartDrillContextMenu
              chartConfig={chartPreview?.chartConfig}
              metas={chartPreview?.backendChart?.view?.meta}
            >
              <ChartIFrameContainer
                key={chartPreview?.backendChart?.id!}
                containerId={chartPreview?.backendChart?.id!}
                dataset={dataset}
                chart={chart!}
                config={chartPreview?.chartConfig!}
                drillOption={drillOptionRef.current}
                selectedItems={selectedItems}
                width={width}
                height={height}
                isLoadingData={isLoadingData}
              />
            </ChartDrillContextMenu>
          </div>
          <StyledChartDrillPathsContainer>
            <ChartDrillPaths chartConfig={chartPreview?.chartConfig} />
          </StyledChartDrillPathsContainer>
        </ChartDrillContext.Provider>
        {viewDetailPanelContextHolder}
        {jumpDialogContextHolder}
        {openJumpVizDialogModalContextHolder}
        <HeadlessBrowserIdentifier
          renderSign={headlessBrowserRenderSign}
          width={Number(width) || 0}
          height={Number(width) + Number(controlH) + TitleHeight || 0}
        />
      </StyledChartPreviewBoardForShare>
    );
  },
);

export default ChartPreviewBoardForShare;

const StyledChartPreviewBoardForShare = styled.div`
  display: flex;
  flex-flow: column;
  width: 100%;
  height: 100%;

  .chart-drill-menu-container {
    height: 100%;
  }

  iframe {
    flex-grow: 1000;
  }
`;

const StyledChartDrillPathsContainer = styled.div`
  background-color: ${p => p.theme.componentBackground};
`;
