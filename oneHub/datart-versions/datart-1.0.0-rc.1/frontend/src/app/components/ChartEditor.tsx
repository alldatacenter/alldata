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

import { ExclamationCircleOutlined } from '@ant-design/icons';
import { Modal } from 'antd';
import { DownloadFileType, RUNTIME_DATE_LEVEL_KEY } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import useMount from 'app/hooks/useMount';
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import ChartManager from 'app/models/ChartManager';
import { useWorkbenchSlice } from 'app/pages/ChartWorkbenchPage/slice';
import { ChartConfigReducerActionType } from 'app/pages/ChartWorkbenchPage/slice/constant';
import {
  aggregationSelector,
  backendChartSelector,
  chartConfigSelector,
  currentDataViewSelector,
  datasetsSelector,
  selectAvailableSourceFunctions,
  selectSelectedItems,
  shadowChartConfigSelector,
} from 'app/pages/ChartWorkbenchPage/slice/selectors';
import {
  fetchAvailableSourceFunctionsForChart,
  initWorkbenchAction,
  refreshDatasetAction,
  updateChartAction,
  updateChartConfigAndRefreshDatasetAction,
} from 'app/pages/ChartWorkbenchPage/slice/thunks';
import { useAddViz } from 'app/pages/MainPage/pages/VizPage/hooks/useAddViz';
import { SaveForm } from 'app/pages/MainPage/pages/VizPage/SaveForm';
import {
  SaveFormContext,
  useSaveFormContext,
} from 'app/pages/MainPage/pages/VizPage/SaveFormContext';
import { IChart } from 'app/types/Chart';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { ChartDTO } from 'app/types/ChartDTO';
import {
  chartSelectionEventListener,
  drillDownEventListener,
  pivotTableDrillEventListener,
  richTextContextEventListener,
  tablePagingAndSortEventListener,
} from 'app/utils/ChartEventListenerHelper';
import {
  clearRuntimeDateLevelFieldsInChartConfig,
  filterCurrentUsedComputedFields,
  getValue,
} from 'app/utils/chartHelper';
import { makeDownloadDataTask } from 'app/utils/fetch';
import {
  getChartDrillOption,
  transferChartConfigs,
} from 'app/utils/internalChartHelper';
import { updateBy } from 'app/utils/mutation';
import { CommonFormTypes } from 'globalConstants';
import { FC, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import styled from 'styled-components/macro';
import { LEVEL_100 } from 'styles/StyleConstants';
import { CloneValueDeep, isEmptyArray } from 'utils/object';
import ChartWorkbench from '../pages/ChartWorkbenchPage/components/ChartWorkbench/ChartWorkbench';
import {
  DataChart,
  DataChartConfig,
  WidgetContentChartType,
} from '../pages/DashBoardPage/pages/Board/slice/types';

const { confirm } = Modal;

export interface ChartEditorBaseProps {
  dataChartId: string;
  orgId: string;
  container: 'widget' | 'dataChart';
  chartType: WidgetContentChartType;
  widgetId?: string;
  defaultViewId?: string;
  originChart?: ChartDTO | DataChart;
}

export interface HistoryState {
  dataChartId: string;
  orgId: string;
  container: 'widget' | 'dataChart';
  chartType: WidgetContentChartType;
}

export interface ChartEditorMethodsProps {
  onClose?: () => void;
  onSaveInWidget?: (
    chartType: WidgetContentChartType,
    dataChart: DataChart,
    view,
  ) => void;
  onSaveInDataChart?: (orgId: string, dataChartId: string) => void;
}
export type ChartEditorProps = ChartEditorBaseProps & ChartEditorMethodsProps;

export const ChartEditor: FC<ChartEditorProps> = ({
  originChart,
  orgId,
  container,
  dataChartId,
  chartType,
  defaultViewId,
  widgetId,
  onClose,
  onSaveInWidget,
  onSaveInDataChart,
}) => {
  const saveFormContextValue = useSaveFormContext();
  const { actions } = useWorkbenchSlice();
  const dispatch = useDispatch();
  const dataset = useSelector(datasetsSelector);
  const dataview = useSelector(currentDataViewSelector);
  const chartConfig = useSelector(chartConfigSelector);
  const shadowChartConfig = useSelector(shadowChartConfigSelector);
  const backendChart = useSelector(backendChartSelector);
  const aggregation = useSelector(aggregationSelector);
  const availableSourceFunctions = useSelector(selectAvailableSourceFunctions);
  const selectedItems = useSelector(selectSelectedItems);
  const [chart, setChart] = useState<IChart>();
  const drillOptionRef = useRef<IChartDrillOption>();
  const [allowQuery, setAllowQuery] = useState<boolean>(false);
  const history = useHistory();
  const addVizFn = useAddViz({
    showSaveForm: saveFormContextValue.showSaveForm,
  });
  const tg = useI18NPrefix('global');

  const expensiveQuery = useMemo(() => {
    try {
      return dataview?.config
        ? Boolean(JSON.parse(dataview.config || '{}').expensiveQuery)
        : false;
    } catch (error) {
      console.log(error);
      return false;
    }
  }, [dataview]);

  useMount(
    () => {
      if (
        (container === 'dataChart' && !dataChartId) ||
        (container === 'widget' && !originChart)
      ) {
        // Note: add default chart if new to editor
        const currentChart = ChartManager.instance().getDefaultChart();
        handleChartChange(currentChart);
      }

      if (container === 'dataChart') {
        dispatch(
          initWorkbenchAction({
            backendChartId: dataChartId,
            orgId,
          }),
        );
      } else {
        //   container === 'widget'
        if (chartType === 'widgetChart') {
          dispatch(
            initWorkbenchAction({
              orgId,
              backendChart: originChart as ChartDTO,
            }),
          );

          if (!originChart) {
            dispatch(actions.updateChartAggregation(true));
          }
        } else {
          // chartType === 'dataChart'
          dispatch(
            initWorkbenchAction({
              orgId,
              backendChartId: dataChartId,
            }),
          );
        }
      }
    },
    () => {
      dispatch(actions.resetWorkbenchState({}));
    },
  );

  useEffect(() => {
    if (backendChart?.config?.chartGraphId) {
      const currentChart = ChartManager.instance().getById(
        backendChart?.config?.chartGraphId,
      );
      registerChartEvents(currentChart);
      setChart(currentChart);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [backendChart?.config?.chartGraphId]);

  useEffect(() => {
    if (!isEmptyArray(chartConfig?.datas) && !drillOptionRef.current) {
      drillOptionRef.current = getChartDrillOption(chartConfig?.datas);
    }
  }, [chartConfig?.datas, drillOptionRef]);

  useEffect(() => {
    if (dataview?.sourceId) {
      dispatch(fetchAvailableSourceFunctionsForChart(dataview.sourceId));
    }
  }, [dataview?.sourceId, dispatch]);

  const handleDrillOptionChange = useCallback(
    (option: IChartDrillOption) => {
      drillOptionRef.current = option;
      dispatch(refreshDatasetAction({ drillOption: option }));
    },
    [dispatch],
  );

  const registerChartEvents = useCallback(
    chart => {
      chart?.registerMouseEvents([
        {
          name: 'click',
          callback: param => {
            drillDownEventListener(drillOptionRef?.current, param, p => {
              drillOptionRef.current = p;
              handleDrillOptionChange?.(p);
            });
            tablePagingAndSortEventListener(param, p => {
              dispatch(refreshDatasetAction(p));
            });
            richTextContextEventListener(
              chart.config.styles[1].rows[0] || {},
              param,
              p => {
                dispatch(updateChartConfigAndRefreshDatasetAction(p));
              },
            );
            pivotTableDrillEventListener(param, p => {
              handleDrillOptionChange(p);
            });
            chartSelectionEventListener(param, p => {
              dispatch(actions.changeSelectedItems(p));
            });
          },
        },
      ]);
    },
    [dispatch, handleDrillOptionChange, actions],
  );

  const clearDataConfig = useCallback(() => {
    const currentChart = chart?.meta?.id
      ? ChartManager.instance().getById(chart?.meta?.id)
      : ChartManager.instance().getDefaultChart();
    let targetChartConfig = CloneValueDeep(currentChart?.config);

    // clear操作不希望重置paging信息
    getValue(targetChartConfig?.settings || [], ['paging'], 'rows').forEach(
      v => {
        v.value = getValue(chartConfig?.settings || [], ['paging', v.key]);
      },
    );

    registerChartEvents(currentChart);
    setChart(currentChart);

    const finalChartConfig = transferChartConfigs(
      targetChartConfig,
      targetChartConfig,
    );

    dispatch(actions.updateShadowChartConfig({}));
    dispatch(
      actions.updateChartConfig({
        type: ChartConfigReducerActionType.INIT,
        payload: {
          init: finalChartConfig,
        },
      }),
    );
    drillOptionRef.current = getChartDrillOption(
      chartConfig?.datas,
      drillOptionRef.current,
    );
  }, [
    dispatch,
    chart?.meta?.id,
    registerChartEvents,
    chartConfig?.datas,
    chartConfig?.settings,
    actions,
  ]);

  const handleChartChange = (c: IChart) => {
    registerChartEvents(c);
    setChart(c);

    const targetChartConfig = CloneValueDeep(c.config);
    const finalChartConfig = clearRuntimeDateLevelFieldsInChartConfig(
      transferChartConfigs(targetChartConfig, shadowChartConfig || chartConfig),
    );

    const computedFields = updateBy(dataview?.computedFields || [], draft => {
      draft.forEach((v, i) => {
        delete draft[i][RUNTIME_DATE_LEVEL_KEY];
      });
    });

    dispatch(actions.updateCurrentDataViewComputedFields(computedFields));

    dispatch(
      actions.updateChartConfig({
        type: ChartConfigReducerActionType.INIT,
        payload: {
          init: finalChartConfig,
        },
      }),
    );
    drillOptionRef.current = getChartDrillOption(
      finalChartConfig?.datas,
      drillOptionRef.current,
      true,
    );

    if (selectedItems.length) {
      dispatch(actions.changeSelectedItems([]));
    }
    if (!expensiveQuery) {
      dispatch(refreshDatasetAction({ drillOption: drillOptionRef?.current }));
    } else {
      setAllowQuery(true);
    }
  };

  const handleChartConfigChange = useCallback(
    (type, payload) => {
      if (expensiveQuery) {
        dispatch(
          actions.updateChartConfig({
            type,
            payload: payload,
          }),
        );
        dispatch(actions.updateShadowChartConfig(null));
        setAllowQuery(payload.needRefresh);
        return true;
      }

      if (payload.value.key === 'enableExpandRow') {
        dispatch(
          updateChartConfigAndRefreshDatasetAction({
            payload: {
              ancestors: [1],
              value: {
                ...chartConfig?.datas?.[1]!,
                drillable: payload.value.value as boolean,
              },
            },
            type: ChartConfigReducerActionType.DATA,
            needRefresh: true,
            updateDrillOption: config => {
              drillOptionRef.current = getChartDrillOption(
                config?.datas,
                drillOptionRef.current,
                true,
              );
              return drillOptionRef.current;
            },
          }),
        );
      }
      dispatch(
        updateChartConfigAndRefreshDatasetAction({
          type,
          payload,
          needRefresh: payload.needRefresh,
          updateDrillOption: config => {
            drillOptionRef.current = getChartDrillOption(
              config?.datas,
              drillOptionRef.current,
            );
            return drillOptionRef.current;
          },
        }),
      );
    },
    [dispatch, expensiveQuery, chartConfig?.datas, actions],
  );

  const handleDataViewChanged = useCallback(
    (clear?: boolean) => {
      if (clear) {
        clearDataConfig();
      }
    },
    [clearDataConfig],
  );

  const handleAggregationState = useCallback(() => {
    clearDataConfig();
  }, [clearDataConfig]);

  const buildDataChart = useCallback(() => {
    const dataChartConfig: DataChartConfig = {
      chartConfig: chartConfig!,
      chartGraphId: chart?.meta.id!,
      computedFields: dataview?.computedFields || [],
      aggregation,
    };

    const dataChart: DataChart = {
      id: dataChartId,
      name: backendChart?.name || '',
      viewId: dataview?.id || '',
      orgId: orgId,
      config: dataChartConfig,
      status: 1,
      description: '',
    };
    return dataChart;
  }, [
    backendChart?.name,
    chart,
    chartConfig,
    dataChartId,
    dataview,
    orgId,
    aggregation,
  ]);

  const saveToWidget = useCallback(() => {
    const dataChart = buildDataChart();
    onSaveInWidget?.(chartType, dataChart, dataview);
  }, [buildDataChart, chartType, dataview, onSaveInWidget]);

  const saveChart = useCallback(async () => {
    if (container === 'dataChart') {
      if (dataChartId) {
        await dispatch(
          updateChartAction({
            name: backendChart?.name,
            viewId: dataview?.id,
            graphId: chart?.meta?.id,
            chartId: dataChartId,
            index: 0,
            parentId: 0,
            aggregation: aggregation,
          }),
        );
        onSaveInDataChart?.(orgId, dataChartId);
      } else {
        const computedFields = filterCurrentUsedComputedFields(
          chartConfig,
          dataview?.computedFields?.filter(v => !v.isViewComputedFields) || [],
        );
        try {
          addVizFn({
            vizType: 'DATACHART',
            type: CommonFormTypes.Add,
            visible: true,
            initialValues: {
              config: JSON.stringify({
                aggregation,
                chartConfig: chartConfig,
                chartGraphId: chart?.meta?.id,
                computedFields,
              }),
              viewId: dataview?.id,
              avatar: chart?.meta?.id,
            },
            callback: folder => {
              folder &&
                history.push(`/organizations/${orgId}/vizs/${folder.relId}`);
            },
          });
        } catch (error) {
          throw error;
        }
      }
    } else if (container === 'widget') {
      if (chartType === 'widgetChart') {
        saveToWidget();
      } else {
        // dataChart
        confirm({
          title: tg('button.saveConfirm'),
          icon: <ExclamationCircleOutlined />,
          async onOk() {
            dispatch(
              updateChartAction({
                name: backendChart?.name,
                viewId: dataview?.id,
                graphId: chart?.meta?.id,
                chartId: dataChartId,
                index: 0,
                parentId: 0,
                aggregation,
              }),
            );
            saveToWidget();
          },
          onCancel() {
            console.log('Cancel');
          },
        });
      }
    }
  }, [
    container,
    dispatch,
    backendChart?.name,
    dataview?.id,
    chart?.meta?.id,
    dataChartId,
    onSaveInDataChart,
    orgId,
    chartType,
    saveToWidget,
    aggregation,
    addVizFn,
    chartConfig,
    dataview?.computedFields,
    history,
    tg,
  ]);

  const saveChartToDashBoard = useCallback(
    (dashboardId, dashboardType) => {
      const dataChart = buildDataChart();
      try {
        history.push({
          pathname: `/organizations/${orgId}/vizs/${dashboardId}/boardEditor`,
          state: {
            widgetInfo: JSON.stringify({
              chartType,
              dataChart,
              dataview,
              dashboardType,
            }),
          },
        });
      } catch (error) {
        throw error;
      }
    },
    [history, buildDataChart, chartType, dataview, orgId],
  );

  const handleRefreshDataset = useCallback(async () => {
    await dispatch(
      refreshDatasetAction({ drillOption: drillOptionRef?.current }),
    );
    setAllowQuery(false);
  }, [dispatch, drillOptionRef]);

  const handleCreateDownloadDataTask = useCallback(async () => {
    if (!dataview?.id) {
      return;
    }
    const isWidget = dataChartId.includes('widget');
    const builder = new ChartDataRequestBuilder(
      dataview,
      chartConfig?.datas,
      chartConfig?.settings,
      {},
      true,
      aggregation,
    );
    dispatch(
      makeDownloadDataTask({
        downloadParams: [
          {
            ...builder.build(),
            ...{
              analytics: dataChartId ? false : true,
              vizName: backendChart?.name || 'chart',
              vizId: isWidget ? widgetId : dataChartId,
              vizType: isWidget ? 'widget' : 'dataChart',
            },
          },
        ],
        fileName: backendChart?.name || 'chart',
        downloadType: DownloadFileType.Excel,
        resolve: () => {
          dispatch(actions.setChartEditorDownloadPolling(true));
        },
      }),
    );
  }, [
    aggregation,
    backendChart?.name,
    chartConfig?.datas,
    chartConfig?.settings,
    dataChartId,
    dataview,
    dispatch,
    actions,
    widgetId,
  ]);

  const handleDateLevelChange = (type, payload) => {
    dispatch(
      updateChartConfigAndRefreshDatasetAction({
        type,
        payload,
        needRefresh: payload.needRefresh,
        updateDrillOption: config => {
          drillOptionRef.current = getChartDrillOption(
            config?.datas,
            drillOptionRef.current,
          );
          return drillOptionRef.current;
        },
      }),
    );
  };

  return (
    <StyledChartWorkbenchPage>
      <SaveFormContext.Provider value={saveFormContextValue}>
        <ChartWorkbench
          header={{
            name: backendChart?.name || originChart?.name,
            orgId,
            container,
            onSaveChart: saveChart,
            onSaveChartToDashBoard: saveChartToDashBoard,
            onGoBack: () => {
              onClose?.();
            },
            onChangeAggregation: handleAggregationState,
          }}
          drillOption={drillOptionRef?.current}
          selectedItems={selectedItems}
          aggregation={aggregation}
          chart={chart}
          dataset={dataset}
          dataview={dataview}
          chartConfig={chartConfig}
          defaultViewId={defaultViewId}
          expensiveQuery={expensiveQuery}
          allowQuery={allowQuery}
          availableSourceFunctions={availableSourceFunctions}
          onChartChange={handleChartChange}
          onChartConfigChange={handleChartConfigChange}
          onChartDrillOptionChange={handleDrillOptionChange}
          onDataViewChange={handleDataViewChanged}
          onRefreshDataset={handleRefreshDataset}
          onCreateDownloadDataTask={handleCreateDownloadDataTask}
          onDateLevelChange={handleDateLevelChange}
        />
        <SaveForm
          width={400}
          formProps={{
            labelAlign: 'left',
            labelCol: { offset: 1, span: 6 },
            wrapperCol: { span: 15 },
          }}
          okText={tg('button.save')}
        />
      </SaveFormContext.Provider>
    </StyledChartWorkbenchPage>
  );
};

export default ChartEditor;

const StyledChartWorkbenchPage = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: ${LEVEL_100};
  display: flex;
  min-width: 0;
  min-height: 0;
`;
