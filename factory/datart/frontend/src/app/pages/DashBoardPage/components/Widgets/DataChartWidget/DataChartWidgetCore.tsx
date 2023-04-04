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
import {
  ChartDataSectionType,
  ChartDataViewFieldCategory,
  ChartInteractionEvent,
} from 'app/constants';
import ChartDrillContext from 'app/contexts/ChartDrillContext';
import { useCacheWidthHeight } from 'app/hooks/useCacheWidthHeight';
import useChartInteractions from 'app/hooks/useChartInteractions';
import { migrateChartConfig } from 'app/migration';
import { ChartDrillOption } from 'app/models/ChartDrillOption';
import ChartManager from 'app/models/ChartManager';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import useDisplayJumpVizDialog from 'app/pages/MainPage/pages/VizPage/hooks/useDisplayJumpVizDialog';
import useDisplayViewDetail from 'app/pages/MainPage/pages/VizPage/hooks/useDisplayViewDetail';
import { selectShareExecuteTokenMap } from 'app/pages/SharePage/slice/selectors';
import { IChart } from 'app/types/Chart';
import { ChartConfig } from 'app/types/ChartConfig';
import { ChartDetailConfigDTO } from 'app/types/ChartConfigDTO';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { mergeToChartConfig } from 'app/utils/ChartDtoHelper';
import {
  chartSelectionEventListener,
  drillDownEventListener,
  pivotTableDrillEventListener,
} from 'app/utils/ChartEventListenerHelper';
import {
  getRuntimeComputedFields,
  getRuntimeDateLevelFields,
  transformToDataSet,
} from 'app/utils/chartHelper';
import { getChartDrillOption } from 'app/utils/internalChartHelper';
import produce from 'immer';
import React, {
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { isEmptyArray } from 'utils/object';
import { uuidv4 } from 'utils/utils';
import { changeSelectedItems } from '../../../pages/BoardEditor/slice/actions/actions';
import { WidgetActionContext } from '../../ActionProvider/WidgetActionProvider';
import {
  boardDrillManager,
  EDIT_PREFIX,
} from '../../BoardDrillManager/BoardDrillManager';
import { BoardContext } from '../../BoardProvider/BoardProvider';
import { BoardScaleContext } from '../../FreeBoardBackground';
import { WidgetChartContext } from '../../WidgetProvider/WidgetChartProvider';
import { WidgetDataContext } from '../../WidgetProvider/WidgetDataProvider';
import { WidgetContext } from '../../WidgetProvider/WidgetProvider';
import { WidgetSelectionContext } from '../../WidgetProvider/WidgetSelectionProvider';

export const DataChartWidgetCore: React.FC<{}> = memo(() => {
  const { dataChart, availableSourceFunctions, chartDataView } =
    useContext(WidgetChartContext);
  const dispatch = useDispatch();
  const scale = useContext(BoardScaleContext);
  const { data: dataset } = useContext(WidgetDataContext);
  const { renderMode, orgId, queryVariables } = useContext(BoardContext);
  const selectedItems = useContext(WidgetSelectionContext);
  const executeTokenMap = useSelector(selectShareExecuteTokenMap);
  const widget = useContext(WidgetContext);
  const { dashboardId, id: wid } = widget;
  const bid = useMemo(() => {
    if (renderMode === 'edit') {
      return EDIT_PREFIX + dashboardId;
    }
    return dashboardId;
  }, [dashboardId, renderMode]);
  const containerId = useMemo(() => {
    return `${wid}_${uuidv4()}`;
  }, [wid]);
  const {
    onWidgetChartClick,
    onWidgetLinkEvent,
    onWidgetGetData,
    onWidgetDataUpdate,
    onUpdateWidgetSelectedItems,
  } = useContext(WidgetActionContext);
  const { cacheWhRef, cacheW, cacheH } = useCacheWidthHeight();
  const widgetRef = useRef<Widget>(widget);
  const drillOptionRef = useRef<IChartDrillOption>();
  const [openViewDetailPanel, viewDetailPanelContextHolder] =
    useDisplayViewDetail();
  const [openJumpVizDialogModal, openJumpVizDialogModalContextHolder] =
    useDisplayJumpVizDialog();
  const [jumpDialogModal, jumpDialogContextHolder] = useModal();
  const {
    getDrillThroughSetting,
    getCrossFilteringSetting,
    getViewDetailSetting,
    handleDrillThroughEvent,
    handleCrossFilteringEvent,
    handleViewDataEvent,
  } = useChartInteractions({
    openViewDetailPanel: openViewDetailPanel as Function,
    openJumpUrlDialogModal: jumpDialogModal.info,
    openJumpVizDialogModal: openJumpVizDialogModal as Function,
  });

  useEffect(() => {
    if (isEmptyArray(selectedItems)) {
      return;
    }
    // recomputed selected items with dataset values
    const dataConfigs = dataChart?.config?.chartConfig?.datas;
    const chartDataSet = transformToDataSet(
      dataset?.rows,
      dataset?.columns,
      dataConfigs,
    );
    const dimensionFields = (dataConfigs || [])
      .filter(
        c =>
          c.type === ChartDataSectionType.Group ||
          c.type === ChartDataSectionType.Color,
      )
      .flatMap(c => c.rows || []);
    const dimensionValuesByField = dimensionFields?.map(field => {
      return {
        fieldName: field.colName,
        values: chartDataSet?.map(row => row.getCell(field)),
      };
    });
    const newSelectedItems = selectedItems?.filter(item => {
      const rowData = item?.data?.rowData || {};
      return dimensionValuesByField.every(({ fieldName, values }) => {
        return (values || []).includes(rowData[fieldName]);
      });
    });
    if (selectedItems?.length !== newSelectedItems?.length) {
      // TODO(Stephen): to be finish rewrite chart selected implement.
      // onUpdateWidgetSelectedItems(widgetRef.current, newSelectedItems);
    }
  }, [
    dataChart?.config?.chartConfig?.datas,
    dataset,
    onUpdateWidgetSelectedItems,
    selectedItems,
  ]);

  useEffect(() => {
    widgetRef.current = widget;
  }, [widget]);

  const handleDateLevelChange = useCallback(
    (type, payload) => {
      const rows = getRuntimeDateLevelFields(payload.value?.rows);
      const dateLevelComputedFields = rows.filter(
        v => v.category === ChartDataViewFieldCategory.DateLevelComputedField,
      );
      const replacedConfig = payload.value.replacedConfig;
      // TODO delete computedFields,
      const computedFields = getRuntimeComputedFields(
        dateLevelComputedFields,
        replacedConfig,
        dataChart?.config?.computedFields,
        true,
      );
      if (dataChart?.id) {
        onWidgetDataUpdate({
          computedFields,
          payload,
          widgetId: dataChart.id,
        });
      }
      onWidgetGetData(widgetRef.current);
    },
    [
      onWidgetDataUpdate,
      dataChart?.config?.computedFields,
      dataChart?.id,
      onWidgetGetData,
    ],
  );

  const handleDrillOptionChange = useCallback(
    (option: IChartDrillOption) => {
      let drillOption;
      drillOption = option;
      drillOptionRef.current = drillOption;
      boardDrillManager.setWidgetDrill({ bid, wid, drillOption });
      onWidgetGetData(widgetRef.current);
    },

    [bid, onWidgetGetData, wid],
  );

  const widgetSpecialConfig = useMemo(() => {
    let linkFields: string[] = [];
    let jumpField: string = '';
    const { jumpConfig, linkageConfig } = widget.config;
    if (linkageConfig?.open) {
      linkFields = widget?.relations
        .filter(re => re.config.type === 'widgetToWidget')
        .map(item => item.config.widgetToWidget?.triggerColumn as string);
    }
    if (jumpConfig?.open) {
      jumpField = jumpConfig?.field?.jumpFieldName as string;
    }

    return {
      linkFields,
      jumpField,
    };
  }, [widget]);

  const buildDrillThroughEventParams = useCallback(
    (clickEventParams, targetEvent: InteractionMouseEvent, ruleId?: string) => {
      const drillThroughSetting = getDrillThroughSetting(
        dataChart?.config?.chartConfig?.interactions,
        widgetRef?.current?.config?.customConfig?.interactions,
      );
      const widgetViewQueryVariables = queryVariables?.filter(qv =>
        widgetRef?.current?.viewIds?.includes(qv?.viewId || ''),
      );
      return {
        drillOption: drillOptionRef?.current,
        drillThroughSetting,
        clickEventParams,
        targetEvent,
        orgId,
        view: chartDataView,
        queryVariables: widgetViewQueryVariables || [],
        computedFields: dataChart?.config?.computedFields,
        aggregation: dataChart?.config?.aggregation,
        chartConfig: dataChart?.config?.chartConfig,
        ruleId,
        isJumpUrlOnly: renderMode === 'share',
      };
    },
    [
      getDrillThroughSetting,
      dataChart?.config?.chartConfig,
      dataChart?.config?.computedFields,
      dataChart?.config?.aggregation,
      queryVariables,
      orgId,
      chartDataView,
      renderMode,
    ],
  );

  const buildCrossFilteringEventParams = useCallback(
    (clickEventParams, targetEvent: InteractionMouseEvent) => {
      const crossFilteringSetting = getCrossFilteringSetting(
        dataChart?.config?.chartConfig?.interactions,
        widgetRef?.current?.config?.customConfig?.interactions,
      );
      return {
        drillOption: drillOptionRef?.current,
        crossFilteringSetting,
        clickEventParams,
        targetEvent,
        orgId,
        view: chartDataView,
        computedFields: dataChart?.config?.computedFields,
        aggregation: dataChart?.config?.aggregation,
        chartConfig: dataChart?.config?.chartConfig,
        renderMode,
      };
    },
    [
      getCrossFilteringSetting,
      dataChart?.config?.chartConfig,
      dataChart?.config?.computedFields,
      dataChart?.config?.aggregation,
      orgId,
      chartDataView,
      renderMode,
    ],
  );

  const buildViewDataEventParams = useCallback(
    (clickEventParams, targetEvent: InteractionMouseEvent) => {
      const viewDetailSetting = getViewDetailSetting(
        dataChart?.config?.chartConfig?.interactions,
        widgetRef?.current?.config?.customConfig?.interactions,
      );
      let authToken;
      if (renderMode === 'share') {
        authToken = executeTokenMap?.[chartDataView?.id!]?.authorizedToken;
      }
      return {
        drillOption: drillOptionRef.current,
        viewDetailSetting,
        clickEventParams,
        targetEvent,
        chartConfig: dataChart?.config?.chartConfig,
        view: chartDataView,
        authToken,
      };
    },
    [
      getViewDetailSetting,
      dataChart?.config?.chartConfig,
      renderMode,
      chartDataView,
      executeTokenMap,
    ],
  );

  const boardRightClickDrillThroughSetting = useMemo(() => {
    const drillThroughSetting = getDrillThroughSetting(
      dataChart?.config?.chartConfig?.interactions,
      widgetRef?.current?.config?.customConfig?.interactions,
    );
    const hasSelectedItems = !isEmptyArray(selectedItems);
    return Boolean(
      drillThroughSetting?.rules?.filter(
        r => r.event === InteractionMouseEvent.Right,
      ).length,
    ) && hasSelectedItems
      ? drillThroughSetting!
      : undefined;
  }, [
    dataChart?.config?.chartConfig?.interactions,
    getDrillThroughSetting,
    selectedItems,
  ]);

  const handleDrillThroughChange = useCallback(() => {
    if (!boardRightClickDrillThroughSetting) {
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
    boardRightClickDrillThroughSetting,
    selectedItems,
    handleDrillThroughEvent,
    buildDrillThroughEventParams,
  ]);

  const boardRightClickCrossFilteringSetting = useMemo(() => {
    const crossFilteringSetting = getCrossFilteringSetting(
      dataChart?.config?.chartConfig?.interactions,
      widgetRef?.current?.config?.customConfig?.interactions,
    );
    return crossFilteringSetting?.event === InteractionMouseEvent.Right
      ? crossFilteringSetting
      : undefined;
  }, [dataChart?.config?.chartConfig?.interactions, getCrossFilteringSetting]);

  const handleCrossFilteringChange = useCallback(() => {
    if (!boardRightClickCrossFilteringSetting) {
      return;
    }
    return () => {
      const rightClickEvent = {
        selectedItems: selectedItems,
      };
      handleCrossFilteringEvent(
        buildCrossFilteringEventParams(
          rightClickEvent,
          InteractionMouseEvent.Right,
        ),
        onWidgetLinkEvent(widgetRef.current),
      );
    };
  }, [
    boardRightClickCrossFilteringSetting,
    selectedItems,
    handleCrossFilteringEvent,
    buildCrossFilteringEventParams,
    onWidgetLinkEvent,
  ]);

  const boardRightClickViewDetailSetting = useMemo(() => {
    const viewDetailSetting = getViewDetailSetting(
      dataChart?.config?.chartConfig?.interactions,
      widgetRef?.current?.config?.customConfig?.interactions,
    );
    const hasSelectedItems = !isEmptyArray(selectedItems);
    return viewDetailSetting?.event === InteractionMouseEvent.Right &&
      hasSelectedItems
      ? viewDetailSetting
      : undefined;
  }, [
    dataChart?.config?.chartConfig?.interactions,
    getViewDetailSetting,
    selectedItems,
  ]);

  const handleViewDataChange = useCallback(() => {
    if (!boardRightClickViewDetailSetting) {
      return;
    }
    return () => {
      const rightClickEvent = {
        selectedItems: selectedItems,
      };
      handleViewDataEvent(
        buildViewDataEventParams(rightClickEvent, InteractionMouseEvent.Right),
      );
    };
  }, [
    boardRightClickViewDetailSetting,
    selectedItems,
    handleViewDataEvent,
    buildViewDataEventParams,
  ]);

  const chart = useMemo(() => {
    if (!dataChart) {
      return null;
    }
    if (dataChart?.config?.chartGraphId) {
      try {
        const chartInstance = ChartManager.instance().getById(
          dataChart.config.chartGraphId,
        ) as IChart;

        if (chartInstance) {
          chartInstance.registerMouseEvents([
            {
              name: 'click',
              callback: (params: any) => {
                if (!params) {
                  return;
                }

                if (
                  params?.interactionType === ChartInteractionEvent.PagingOrSort
                ) {
                  onWidgetChartClick(widgetRef.current, params);
                  return;
                }

                handleDrillThroughEvent(
                  buildDrillThroughEventParams(
                    params,
                    InteractionMouseEvent.Left,
                  ),
                );
                handleCrossFilteringEvent(
                  buildCrossFilteringEventParams(
                    params,
                    InteractionMouseEvent.Left,
                  ),
                  onWidgetLinkEvent(widgetRef.current),
                );
                handleViewDataEvent(
                  buildViewDataEventParams(params, InteractionMouseEvent.Left),
                );
                drillDownEventListener(drillOptionRef?.current, params, p => {
                  drillOptionRef.current = p;
                  handleDrillOptionChange?.(p);
                });
                pivotTableDrillEventListener(params, p => {
                  handleDrillOptionChange(p);
                });
                chartSelectionEventListener(params, p => {
                  changeSelectedItems(dispatch, renderMode, p, wid);
                });
              },
            },
            {
              name: 'contextmenu',
              callback: param => {},
            },
          ]);
        }
        return chartInstance;
      } catch (error) {
        return null;
      }
    } else {
      return null;
    }
  }, [
    dataChart,
    dispatch,
    wid,
    renderMode,
    handleDrillOptionChange,
    onWidgetChartClick,
    handleCrossFilteringEvent,
    buildCrossFilteringEventParams,
    handleDrillThroughEvent,
    buildDrillThroughEventParams,
    handleViewDataEvent,
    buildViewDataEventParams,
    onWidgetLinkEvent,
  ]);

  const config = useMemo(() => {
    if (!chart?.config) return undefined;
    if (!dataChart?.config) return undefined;
    let chartConfig = produce(chart.config, draft => {
      mergeToChartConfig(
        draft,
        produce(dataChart?.config, draft => {
          migrateChartConfig(draft as ChartDetailConfigDTO);
        }) as ChartDetailConfigDTO,
      );
    });
    return chartConfig as ChartConfig;
  }, [chart?.config, dataChart?.config]);

  useEffect(() => {
    let drillOption = getChartDrillOption(
      config?.datas,
      drillOptionRef.current,
    ) as ChartDrillOption;
    drillOptionRef.current = drillOption;
    boardDrillManager.setWidgetDrill({
      bid,
      wid,
      drillOption,
    });
  }, [bid, config?.datas, wid]);

  const errText = useMemo(() => {
    if (!dataChart) {
      return `not found dataChart`;
    }
    if (!chart) {
      if (!dataChart?.config) {
        return `not found chart config`;
      }
      if (!dataChart?.config?.chartGraphId) {
        return `not found chartGraphId`;
      }
      return `not found chart by ${dataChart?.config?.chartGraphId}`;
    }
    return null;
  }, [chart, dataChart]);

  const chartFrame = useMemo(() => {
    if (!config) return null;
    if (cacheH <= 1 || cacheW <= 1) return null;
    if (errText) return errText;
    const drillOption = drillOptionRef.current;

    return (
      <ChartIFrameContainer
        dataset={dataset}
        chart={chart!}
        config={config}
        width={cacheW}
        height={cacheH}
        drillOption={drillOption}
        selectedItems={selectedItems}
        containerId={containerId}
        widgetSpecialConfig={widgetSpecialConfig}
        scale={scale}
      />
    );
  }, [
    config,
    cacheH,
    cacheW,
    errText,
    chart,
    dataset,
    selectedItems,
    containerId,
    widgetSpecialConfig,
    scale,
  ]);

  const drillContextVal = {
    drillOption: drillOptionRef.current,
    availableSourceFunctions,
    crossFilteringSetting: boardRightClickCrossFilteringSetting,
    viewDetailSetting: boardRightClickViewDetailSetting,
    drillThroughSetting: boardRightClickDrillThroughSetting,
    onDrillOptionChange: handleDrillOptionChange,
    onDateLevelChange: handleDateLevelChange,
    onDrillThroughChange: handleDrillThroughChange(),
    onCrossFilteringChange: handleCrossFilteringChange(),
    onViewDataChange: handleViewDataChange(),
  };

  return (
    <ChartDrillContext.Provider value={drillContextVal}>
      <ChartDrillContextMenu
        chartConfig={dataChart?.config.chartConfig}
        metas={chartDataView?.meta}
      >
        <StyledWrapper>
          <ChartFrameBox ref={cacheWhRef}>{chartFrame}</ChartFrameBox>
          <ChartDrillPaths chartConfig={dataChart?.config.chartConfig} />
          {viewDetailPanelContextHolder}
          {jumpDialogContextHolder}
          {openJumpVizDialogModalContextHolder}
        </StyledWrapper>
      </ChartDrillContextMenu>
    </ChartDrillContext.Provider>
  );
});
const StyledWrapper = styled.div`
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  width: 100%;
  height: 100%;
  .chart-drill-menu-container {
    height: 100%;
  }
`;
const ChartFrameBox = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
`;
