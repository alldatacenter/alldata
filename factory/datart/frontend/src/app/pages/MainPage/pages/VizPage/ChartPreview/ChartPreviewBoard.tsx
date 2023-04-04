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

import { message, Spin } from 'antd';
import useModal from 'antd/lib/modal/useModal';
import ChartDrillContextMenu from 'app/components/ChartDrill/ChartDrillContextMenu';
import ChartDrillPaths from 'app/components/ChartDrill/ChartDrillPaths';
import { ChartIFrameContainer } from 'app/components/ChartIFrameContainer';
import { InteractionMouseEvent } from 'app/components/FormGenerator/constants';
import { VizHeader } from 'app/components/VizHeader';
import { ChartInteractionEvent } from 'app/constants';
import ChartDrillContext from 'app/contexts/ChartDrillContext';
import { useCacheWidthHeight } from 'app/hooks/useCacheWidthHeight';
import useChartInteractions from 'app/hooks/useChartInteractions';
import useDebouncedLoadingStatus from 'app/hooks/useDebouncedLoadingStatus';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import ChartManager from 'app/models/ChartManager';
import { useWorkbenchSlice } from 'app/pages/ChartWorkbenchPage/slice';
import { selectAvailableSourceFunctions } from 'app/pages/ChartWorkbenchPage/slice/selectors';
import { fetchAvailableSourceFunctionsForChart } from 'app/pages/ChartWorkbenchPage/slice/thunks';
import { useMainSlice } from 'app/pages/MainPage/slice';
import { IChart } from 'app/types/Chart';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import {
  chartSelectionEventListener,
  drillDownEventListener,
  pivotTableDrillEventListener,
  tablePagingAndSortEventListener,
} from 'app/utils/ChartEventListenerHelper';
import { generateShareLinkAsync, makeDownloadDataTask } from 'app/utils/fetch';
import { getChartDrillOption } from 'app/utils/internalChartHelper';
import {
  FC,
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import styled from 'styled-components/macro';
import { BORDER_RADIUS, SPACE_LG } from 'styles/StyleConstants';
import { isEmptyArray } from 'utils/object';
import { urlSearchTransfer } from 'utils/urlSearchTransfer';
import useDisplayJumpVizDialog from '../hooks/useDisplayJumpVizDialog';
import useDisplayViewDetail from '../hooks/useDisplayViewDetail';
import useQSLibUrlHelper from '../hooks/useQSLibUrlHelper';
import { useSaveAsViz } from '../hooks/useSaveAsViz';
import { useVizSlice } from '../slice';
import {
  selectPreviewCharts,
  selectPublishLoading,
  selectSelectedItems,
  selectVizs,
} from '../slice/selectors';
import {
  deleteViz,
  fetchDataSetByPreviewChartAction,
  initChartPreviewData,
  publishViz,
  removeTab,
  updateFilterAndFetchDataset,
  updateGroupAndFetchDataset,
} from '../slice/thunks';
import { ChartPreview } from '../slice/types';
import ControllerPanel from './components/ControllerPanel';

const ChartPreviewBoard: FC<{
  backendChartId: string;
  orgId: string;
  filterSearchUrl?: string;
  allowDownload?: boolean;
  allowShare?: boolean;
  allowManage?: boolean;
  hideTitle?: boolean;
}> = memo(
  ({
    backendChartId,
    orgId,
    filterSearchUrl,
    allowDownload,
    allowShare,
    allowManage,
    hideTitle,
  }) => {
    // NOTE: avoid initialize width or height is zero that cause echart sampling calculation issue.
    const defaultChartContainerWH = 1;
    const {
      cacheWhRef: ref,
      cacheW,
      cacheH,
    } = useCacheWidthHeight({
      initH: defaultChartContainerWH,
      initW: defaultChartContainerWH,
    });
    useWorkbenchSlice();
    const { actions: vizAction } = useVizSlice();
    const { actions } = useMainSlice();
    const chartManager = ChartManager.instance();
    const dispatch = useDispatch();
    const { parse } = useQSLibUrlHelper();
    const [version, setVersion] = useState<string>();
    const previewCharts = useSelector(selectPreviewCharts);
    const publishLoading = useSelector(selectPublishLoading);
    const selectedItems = useSelector(selectSelectedItems);
    const availableSourceFunctions = useSelector(
      selectAvailableSourceFunctions,
    );
    const [chartPreview, setChartPreview] = useState<ChartPreview>();
    const chartConfigRef = useRef(chartPreview?.chartConfig);
    const [chart, setChart] = useState<IChart>();
    const [loadingStatus, setLoadingStatus] = useState<boolean>(false);
    const drillOptionRef = useRef<IChartDrillOption>();
    const t = useI18NPrefix('viz.main');
    const tg = useI18NPrefix('global');
    const saveAsViz = useSaveAsViz();
    const history = useHistory();
    const vizs = useSelector(selectVizs);
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

    useEffect(() => {
      const jumpFilterParams: PendingChartDataRequestFilter[] = parse(
        filterSearchUrl,
        { ignoreQueryPrefix: true },
      )?.filters;
      const jumpVariableParams: Record<string, any[]> = parse(filterSearchUrl, {
        ignoreQueryPrefix: true,
      })?.variables;
      const filterSearchParams = filterSearchUrl
        ? urlSearchTransfer.toParams(filterSearchUrl)
        : undefined;

      dispatch(
        initChartPreviewData({
          backendChartId,
          orgId,
          filterSearchParams,
          jumpFilterParams,
          jumpVariableParams,
        }),
      );
    }, [dispatch, orgId, backendChartId, filterSearchUrl, parse]);

    useEffect(() => {
      const sourceId = chartPreview?.backendChart?.view.sourceId;
      if (sourceId) {
        dispatch(fetchAvailableSourceFunctionsForChart(sourceId));
      }
    }, [chartPreview?.backendChart?.view.sourceId, dispatch]);

    useEffect(() => {
      const newChartPreview = previewCharts.find(
        c => c.backendChartId === backendChartId,
      );
      if (newChartPreview && newChartPreview.version !== version) {
        setVersion(newChartPreview.version);
        setChartPreview(newChartPreview);

        chartConfigRef.current = newChartPreview?.chartConfig;
        drillOptionRef.current = getChartDrillOption(
          newChartPreview?.chartConfig?.datas,
          drillOptionRef?.current,
        );

        if (
          !chart ||
          chart?.meta?.id !==
            newChartPreview?.backendChart?.config?.chartGraphId
        ) {
          const newChart = chartManager.getById(
            newChartPreview?.backendChart?.config?.chartGraphId,
          );
          setChart(newChart);
        }
      }
    }, [
      backendChartId,
      chart,
      chartManager,
      chartPreview?.backendChart?.id,
      previewCharts,
      version,
    ]);

    const handleDrillOptionChange = useCallback(
      (option: IChartDrillOption) => {
        drillOptionRef.current = option;
        dispatch(
          updateFilterAndFetchDataset({
            backendChartId,
            chartPreview,
            payload: null,
            drillOption: drillOptionRef?.current,
          }),
        );
      },
      [backendChartId, chartPreview, dispatch],
    );

    const chartRightClickDrillThroughSetting = useMemo(() => {
      const drillThroughSetting = getDrillThroughSetting(
        chartConfigRef?.current?.interactions,
        [],
      );
      const hasSelectedItems = !isEmptyArray(selectedItems?.[backendChartId]);
      return Boolean(
        drillThroughSetting?.rules?.filter(
          r => r.event === InteractionMouseEvent.Right,
        ).length,
      ) && hasSelectedItems
        ? drillThroughSetting!
        : undefined;
    }, [backendChartId, getDrillThroughSetting, selectedItems]);

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
        const computedFields = (
          chartPreview?.backendChart?.config.computedFields || []
        ).concat(chartPreview?.backendChart?.view.computedFields || []);

        const view = {
          id: chartPreview?.backendChart?.view?.id || '',
          config: chartPreview?.backendChart?.view.config || {},
          meta: chartPreview?.backendChart?.view.meta,
          computedFields,
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
          selectedItems: selectedItems?.[backendChartId],
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
      backendChartId,
      chartPreview?.chartConfig?.interactions,
      selectedItems,
      getDrillThroughSetting,
      handleDrillThroughEvent,
      buildDrillThroughEventParams,
    ]);

    const chartRightClickViewDetailSetting = useMemo(() => {
      const viewDetailSetting = getViewDetailSetting(
        chartPreview?.chartConfig?.interactions,
        [],
      );
      const hasSelectedItems = !isEmptyArray(selectedItems?.[backendChartId]);
      return viewDetailSetting?.event === InteractionMouseEvent.Right &&
        hasSelectedItems
        ? viewDetailSetting
        : undefined;
    }, [
      backendChartId,
      chartPreview?.chartConfig?.interactions,
      getViewDetailSetting,
      selectedItems,
    ]);

    const handleViewDataChange = useCallback(() => {
      if (!chartRightClickViewDetailSetting) {
        return;
      }
      return () => {
        const rightClickEvent = {
          selectedItems: selectedItems?.[backendChartId],
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
      backendChartId,
      handleViewDataEvent,
      buildViewDataEventParams,
    ]);

    const registerChartEvents = useCallback(
      (chart, backendChartId) => {
        chart?.registerMouseEvents([
          {
            name: 'click',
            callback: param => {
              if (
                param?.interactionType === ChartInteractionEvent.PagingOrSort
              ) {
                tablePagingAndSortEventListener(param, p => {
                  dispatch(
                    fetchDataSetByPreviewChartAction({
                      ...p,
                      backendChartId,
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
                dispatch(
                  vizAction.changeSelectedItems({
                    backendChartId,
                    data: p,
                  }),
                );
              });
            },
          },
          {
            name: 'contextmenu',
            callback: param => {},
          },
        ]);
      },
      [
        dispatch,
        handleDrillOptionChange,
        handleDrillThroughEvent,
        handleViewDataEvent,
        buildDrillThroughEventParams,
        buildViewDataEventParams,
        vizAction,
        drillOptionRef,
      ],
    );

    useEffect(() => {
      registerChartEvents(chart, backendChartId);
    }, [backendChartId, chart, registerChartEvents]);

    const handleGotoWorkbenchPage = () => {
      history.push({
        pathname: `/organizations/${orgId}/vizs/chartEditor`,
        search: `dataChartId=${backendChartId}&chartType=dataChart&container=dataChart`,
      });
    };

    const handleFilterChange = (type, payload) => {
      dispatch(
        updateFilterAndFetchDataset({
          backendChartId,
          chartPreview,
          payload,
          drillOption: drillOptionRef?.current,
        }),
      );
    };

    const handleGenerateShareLink = async ({
      expiryDate,
      authenticationMode,
      roles,
      users,
      rowPermissionBy,
    }: {
      expiryDate: string;
      authenticationMode: string;
      roles: string[];
      users: string[];
      rowPermissionBy: string;
    }) => {
      const result = await generateShareLinkAsync({
        expiryDate,
        authenticationMode,
        roles,
        users,
        rowPermissionBy,
        vizId: backendChartId,
        vizType: 'DATACHART',
      });
      return result;
    };

    const handleCreateDownloadDataTask = async downloadType => {
      if (!chartPreview) {
        return;
      }

      const builder = new ChartDataRequestBuilder(
        {
          id: chartPreview?.backendChart?.view?.id || '',
          config: chartPreview?.backendChart?.view.config || {},
          meta: chartPreview?.backendChart?.view?.meta,
          computedFields:
            chartPreview?.backendChart?.config.computedFields || [],
          type: chartPreview?.backendChart?.view?.type || 'SQL',
        },
        chartPreview?.chartConfig?.datas,
        chartPreview?.chartConfig?.settings,
        {},
        false,
        chartPreview?.backendChart?.config?.aggregation,
      );
      const folderId = vizs.filter(
        v => v.relId === chartPreview?.backendChart?.id,
      )[0].id;

      dispatch(
        makeDownloadDataTask({
          downloadParams: [
            {
              ...builder.build(),
              ...{
                vizId:
                  downloadType === 'EXCEL'
                    ? chartPreview?.backendChart?.id
                    : folderId,
                vizName: chartPreview?.backendChart?.name,
                analytics: false,
                vizType: 'dataChart',
              },
            },
          ],
          imageWidth: cacheW,
          downloadType,
          fileName: chartPreview?.backendChart?.name || 'chart',
          resolve: () => {
            dispatch(actions.setDownloadPolling(true));
          },
        }),
      );
    };

    const handlePublish = useCallback(() => {
      if (chartPreview?.backendChart) {
        dispatch(
          publishViz({
            id: chartPreview.backendChart.id,
            vizType: 'DATACHART',
            publish: chartPreview.backendChart.status === 1 ? true : false,
            resolve: () => {
              message.success(
                chartPreview.backendChart?.status === 2
                  ? t('unpublishSuccess')
                  : t('publishSuccess'),
              );
            },
          }),
        );
      }
    }, [dispatch, chartPreview?.backendChart, t]);

    const handleSaveAsVizs = useCallback(() => {
      saveAsViz(chartPreview?.backendChartId as string, 'DATACHART');
    }, [saveAsViz, chartPreview?.backendChartId]);

    const handleReloadData = useCallback(async () => {
      setLoadingStatus(true);
      await dispatch(
        fetchDataSetByPreviewChartAction({
          backendChartId,
        }),
      );
      setLoadingStatus(false);
    }, [dispatch, backendChartId]);

    const handleAddToDashBoard = useCallback(
      (dashboardId, dashboardType) => {
        const currentChartPreview = previewCharts.find(
          c => c.backendChartId === backendChartId,
        );

        try {
          history.push({
            pathname: `/organizations/${orgId}/vizs/${dashboardId}/boardEditor`,
            state: {
              widgetInfo: JSON.stringify({
                chartType: '',
                dataChart: currentChartPreview?.backendChart,
                dataview: currentChartPreview?.backendChart?.view,
                dashboardType,
              }),
            },
          });
        } catch (error) {
          throw error;
        }
      },
      [previewCharts, history, backendChartId, orgId],
    );

    const redirect = useCallback(
      tabKey => {
        if (tabKey) {
          history.push(`/organizations/${orgId}/vizs/${tabKey}`);
        } else {
          history.push(`/organizations/${orgId}/vizs`);
        }
      },
      [history, orgId],
    );

    const handleRecycleViz = useCallback(() => {
      dispatch(
        deleteViz({
          params: { id: backendChartId, archive: true },
          type: 'DATACHART',
          resolve: () => {
            message.success(tg('operation.archiveSuccess'));
            dispatch(removeTab({ id: backendChartId, resolve: redirect }));
          },
        }),
      );
    }, [backendChartId, dispatch, redirect, tg]);

    const handleDateLevelChange = (type, payload) => {
      dispatch(
        updateGroupAndFetchDataset({
          backendChartId,
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
      <StyledChartPreviewBoard>
        {!hideTitle && (
          <VizHeader
            chartName={chartPreview?.backendChart?.name}
            status={chartPreview?.backendChart?.status}
            publishLoading={publishLoading}
            onGotoEdit={handleGotoWorkbenchPage}
            onPublish={handlePublish}
            onGenerateShareLink={handleGenerateShareLink}
            onDownloadData={handleCreateDownloadDataTask}
            onSaveAsVizs={handleSaveAsVizs}
            onReloadData={handleReloadData}
            onAddToDashBoard={handleAddToDashBoard}
            onRecycleViz={handleRecycleViz}
            allowDownload={allowDownload}
            allowShare={allowShare}
            allowManage={allowManage}
            orgId={orgId}
            backendChartId={backendChartId}
          />
        )}
        <PreviewBlock>
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
            <div>
              <ControllerPanel
                viewId={chartPreview?.backendChart?.viewId}
                view={chartPreview?.backendChart?.view}
                chartConfig={chartPreview?.chartConfig}
                onChange={handleFilterChange}
              />
            </div>
            <ChartWrapper ref={ref}>
              <Spin wrapperClassName="spinWrapper" spinning={loadingStatus}>
                <ChartDrillContextMenu
                  chartConfig={chartPreview?.chartConfig!}
                  metas={chartPreview?.backendChart?.view?.meta}
                >
                  <ChartIFrameContainer
                    key={backendChartId}
                    containerId={backendChartId}
                    dataset={dataset}
                    chart={chart!}
                    config={chartPreview?.chartConfig!}
                    drillOption={drillOptionRef.current}
                    selectedItems={selectedItems[backendChartId]}
                    width={cacheW}
                    height={cacheH}
                    isLoadingData={isLoadingData}
                  />
                </ChartDrillContextMenu>
              </Spin>
            </ChartWrapper>
            <StyledChartDrillPathsContainer>
              <ChartDrillPaths chartConfig={chartPreview?.chartConfig!} />
            </StyledChartDrillPathsContainer>
          </ChartDrillContext.Provider>
        </PreviewBlock>
        {viewDetailPanelContextHolder}
        {jumpDialogContextHolder}
        {openJumpVizDialogModalContextHolder}
      </StyledChartPreviewBoard>
    );
  },
);

export default ChartPreviewBoard;
const StyledChartPreviewBoard = styled.div`
  display: flex;
  flex: 1;
  flex-flow: column;
  height: 100%;
  iframe {
    flex-grow: 1000;
  }
`;
const PreviewBlock = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  padding: ${SPACE_LG};
  overflow: hidden;
  box-shadow: ${p => p.theme.shadowBlock};
`;

const ChartWrapper = styled.div`
  position: relative;
  display: flex;
  flex: 1;
  background-color: ${p => p.theme.componentBackground};
  border-radius: ${BORDER_RADIUS};
  .chart-drill-menu-container {
    height: 100%;
  }
  .spinWrapper {
    width: 100%;
    height: 100%;
    .ant-spin-container {
      width: 100%;
      height: 100%;
    }
  }
`;

const StyledChartDrillPathsContainer = styled.div`
  background-color: ${p => p.theme.componentBackground};
`;
