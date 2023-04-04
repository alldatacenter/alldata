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

import {
  InteractionAction,
  InteractionCategory,
} from 'app/components/FormGenerator/constants';
import {
  CrossFilteringSetting,
  DrillThroughSetting,
  ViewDetailSetting,
} from 'app/components/FormGenerator/Customize/Interaction/types';
import { ChartInteractionEvent } from 'app/constants';
import useDrillThrough from 'app/hooks/useDrillThrough';
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import { getStyles, getValue } from 'app/utils/chartHelper';
import {
  buildClickEventBaseFilters,
  getJumpFiltersByInteractionRule,
  getJumpOperationFiltersByInteractionRule,
  getLinkFiltersByInteractionRule,
  getVariablesByInteractionRule,
  variableToFilter,
} from 'app/utils/internalChartHelper';
import qs from 'qs';
import { useCallback } from 'react';
import { isEmpty, isEmptyArray } from 'utils/object';
import { urlSearchTransfer } from 'utils/urlSearchTransfer';

const useChartInteractions = (props: {
  openViewDetailPanel?: Function;
  openJumpVizDialogModal?: Function;
  openJumpUrlDialogModal?: Function;
}) => {
  const {
    openNewTab,
    openBrowserTab,
    getDialogContent,
    redirectByUrl,
    openNewByUrl,
    getDialogContentByUrl,
  } = useDrillThrough();

  const getDrillThroughSetting = (
    chartInteractions,
    boardInteractions?,
  ): DrillThroughSetting | null => {
    const enableBoardDrillThrough = getValue(boardInteractions || [], [
      'drillThrough',
    ]);
    if (enableBoardDrillThrough) {
      return getStyles(
        boardInteractions || [],
        ['drillThrough'],
        ['setting'],
      )?.[0];
    }
    const enableChartDrillThrough = getValue(chartInteractions || [], [
      'drillThrough',
    ]);
    if (enableChartDrillThrough) {
      return getStyles(
        chartInteractions || [],
        ['drillThrough'],
        ['setting'],
      )?.[0];
    } else {
      return null;
    }
  };

  const getCrossFilteringSetting = (
    chartInteractions,
    boardInteractions?,
  ): CrossFilteringSetting | null => {
    const enableBoardCrossFiltering = getValue(boardInteractions || [], [
      'crossFiltering',
    ]);
    if (enableBoardCrossFiltering) {
      return getStyles(
        boardInteractions || [],
        ['crossFiltering'],
        ['setting'],
      )?.[0];
    }
    const enableChartCrossFiltering = getValue(chartInteractions || [], [
      'crossFiltering',
    ]);
    if (enableChartCrossFiltering) {
      return getStyles(
        chartInteractions || [],
        ['crossFiltering'],
        ['setting'],
      )?.[0];
    } else {
      return null;
    }
  };

  const getViewDetailSetting = (
    chartInteractions,
    boardInteractions?,
  ): ViewDetailSetting | null => {
    const enableBoardViewDetail = getValue(boardInteractions || [], [
      'viewDetail',
    ]);
    if (enableBoardViewDetail) {
      return getStyles(
        boardInteractions || [],
        ['viewDetail'],
        ['setting'],
      )?.[0];
    }
    const enableChartViewDetail = getValue(chartInteractions || [], [
      'viewDetail',
    ]);
    if (enableChartViewDetail) {
      return getStyles(
        chartInteractions || [],
        ['viewDetail'],
        ['setting'],
      )?.[0];
    } else {
      return null;
    }
  };

  const handleDrillThroughEvent = useCallback(
    ({
      drillOption,
      drillThroughSetting,
      clickEventParams,
      targetEvent,
      ruleId,
      orgId,
      view,
      queryVariables,
      computedFields,
      aggregation,
      chartConfig,
      isJumpUrlOnly,
    }) => {
      if (drillThroughSetting) {
        const sourceChartFilters = new ChartDataRequestBuilder(
          {
            id: view?.id || '',
            config: view?.config || {},
            meta: view?.meta,
            computedFields: computedFields || [],
            type: view?.type,
          },
          chartConfig?.datas,
          chartConfig?.settings,
          {},
          false,
          aggregation,
        )
          .addDrillOption(drillOption)
          .getColNameStringFilter();
        const sourceChartNonAggFilters = (sourceChartFilters || []).filter(
          f => !Boolean(f.aggOperator),
        );
        const hasNoSelectedItems = isEmptyArray(
          clickEventParams?.selectedItems,
        );

        if (hasNoSelectedItems) {
          return;
        }

        (drillThroughSetting?.rules || [])
          .filter(rule => rule.event === targetEvent)
          .filter(rule => isEmpty(ruleId) || rule.id === ruleId)
          .filter(
            rule =>
              !isJumpUrlOnly ||
              rule?.category === InteractionCategory.JumpToUrl,
          )
          .forEach(rule => {
            const clickFilters = buildClickEventBaseFilters(
              clickEventParams?.selectedItems?.map(item => item?.data?.rowData),
              rule,
              drillOption,
              chartConfig?.datas,
            );
            const relId = rule?.[rule.category!]?.relId;
            if (rule.category === InteractionCategory.JumpToChart) {
              const urlFilters = getJumpOperationFiltersByInteractionRule(
                clickFilters,
                sourceChartFilters,
                rule,
              );
              const clickVariables = getVariablesByInteractionRule(
                queryVariables,
                rule,
              );
              const urlFiltersStr: string = qs.stringify({
                filters: urlFilters || [],
                variables: clickVariables,
              });
              if (rule?.action === InteractionAction.Redirect) {
                openNewTab(orgId, relId, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Window) {
                openBrowserTab(orgId, relId, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Dialog) {
                const modalContent = getDialogContent(
                  orgId,
                  relId,
                  'DATACHART',
                  urlFiltersStr,
                );
                props?.openJumpVizDialogModal?.(modalContent as any);
              }
            } else if (rule.category === InteractionCategory.JumpToDashboard) {
              const variableFilters = variableToFilter(
                getVariablesByInteractionRule(queryVariables, rule),
              );
              const urlFilters = getJumpFiltersByInteractionRule(
                clickFilters,
                sourceChartNonAggFilters,
                variableFilters,
                rule,
              );
              Object.assign(urlFilters, { isMatchByName: true });
              const urlFiltersStr: string =
                urlSearchTransfer.toUrlString(urlFilters);
              if (rule?.action === InteractionAction.Redirect) {
                openNewTab(orgId, relId, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Window) {
                openBrowserTab(orgId, relId, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Dialog) {
                const modalContent = getDialogContent(
                  orgId,
                  relId,
                  'DASHBOARD',
                  urlFiltersStr,
                );
                props?.openJumpVizDialogModal?.(modalContent as any);
              }
            } else if (rule.category === InteractionCategory.JumpToUrl) {
              const variableFilters = variableToFilter(
                getVariablesByInteractionRule(queryVariables, rule),
              );
              const urlFilters = getJumpFiltersByInteractionRule(
                clickFilters,
                sourceChartNonAggFilters,
                variableFilters,
                rule,
              );
              Object.assign(urlFilters, { isMatchByName: true });
              const urlFiltersStr: string =
                urlSearchTransfer.toUrlString(urlFilters);
              const url = rule?.[rule.category!]?.url;
              if (rule?.action === InteractionAction.Redirect) {
                redirectByUrl(url, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Window) {
                openNewByUrl(url, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Dialog) {
                const modalContent = getDialogContentByUrl(url, urlFiltersStr);
                props?.openJumpUrlDialogModal?.(modalContent as any);
              }
            }
          });
      }
    },
    [
      openNewTab,
      openBrowserTab,
      getDialogContent,
      props,
      redirectByUrl,
      openNewByUrl,
      getDialogContentByUrl,
    ],
  );

  const handleCrossFilteringEvent = useCallback(
    (
      {
        drillOption,
        crossFilteringSetting,
        clickEventParams,
        targetEvent,
        view,
        queryVariables,
        computedFields,
        aggregation,
        chartConfig,
      },
      callback,
    ) => {
      if (
        !crossFilteringSetting ||
        crossFilteringSetting?.event !== targetEvent
      ) {
        return null;
      }
      let nonAggChartFilters = new ChartDataRequestBuilder(
        {
          id: view?.id || '',
          config: view?.config || {},
          meta: view?.meta,
          computedFields: computedFields || [],
        },
        chartConfig?.datas,
        chartConfig?.settings,
        {},
        false,
        aggregation,
      )
        .addDrillOption(drillOption)
        .getColNameStringFilter()
        ?.filter(f => !Boolean(f.aggOperator));

      const linkParams = (crossFilteringSetting?.rules || []).map(rule => {
        const variableFilters = variableToFilter(
          getVariablesByInteractionRule(queryVariables, rule),
        );
        const clickFilters = buildClickEventBaseFilters(
          clickEventParams?.selectedItems?.map(item => item?.data?.rowData),
          rule,
          drillOption,
          chartConfig?.datas,
        );
        const filters = getLinkFiltersByInteractionRule(
          clickFilters,
          nonAggChartFilters,
          variableFilters,
          rule,
        );
        const variables = getVariablesByInteractionRule(queryVariables, rule);
        const isUnSelectedAll =
          clickEventParams?.interactionType === ChartInteractionEvent.UnSelect;
        return {
          rule,
          isUnSelectedAll,
          filters,
          variables,
        };
      });
      callback?.(linkParams);
    },
    [],
  );

  const handleViewDataEvent = useCallback(
    ({
      drillOption,
      clickEventParams,
      targetEvent,
      viewDetailSetting,
      chartConfig,
      view,
      authToken,
    }) => {
      if (viewDetailSetting?.event === targetEvent) {
        const clickFilters = buildClickEventBaseFilters(
          clickEventParams?.selectedItems?.map(item => item?.data?.rowData),
          undefined,
          drillOption,
          chartConfig?.datas,
        );
        const hasNoSelectedItems = isEmptyArray(
          clickEventParams?.selectedItems,
        );
        if (hasNoSelectedItems) {
          return;
        }
        (props?.openViewDetailPanel as any)({
          currentDataView: view,
          chartConfig: chartConfig,
          drillOption: drillOption,
          viewDetailSetting: viewDetailSetting,
          clickFilters: clickFilters,
          authToken,
        });
      }
    },
    [props?.openViewDetailPanel],
  );

  return {
    getDrillThroughSetting,
    getCrossFilteringSetting,
    getViewDetailSetting,
    handleDrillThroughEvent,
    handleCrossFilteringEvent,
    handleViewDataEvent,
  };
};

export default useChartInteractions;
