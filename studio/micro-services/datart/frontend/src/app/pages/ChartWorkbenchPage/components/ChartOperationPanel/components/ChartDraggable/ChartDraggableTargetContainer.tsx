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
  ChartDataSectionFieldActionType,
  ChartDataSectionType,
  ChartDataViewFieldCategory,
  DataViewFieldType,
} from 'app/constants';
import ChartDrillContext from 'app/contexts/ChartDrillContext';
import useFieldActionModal from 'app/hooks/useFieldActionModal';
import ChartAggregationContext from 'app/pages/ChartWorkbenchPage/contexts/ChartAggregationContext';
import ChartDatasetContext from 'app/pages/ChartWorkbenchPage/contexts/ChartDatasetContext';
import VizDataViewContext from 'app/pages/ChartWorkbenchPage/contexts/ChartDataViewContext';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { ChartDataConfigSectionProps } from 'app/types/ChartDataConfigSection';
import { getColumnRenderName } from 'app/utils/chartHelper';
import { reachLowerBoundCount } from 'app/utils/internalChartHelper';
import { updateBy, updateByKey } from 'app/utils/mutation';
import { CHART_DRAG_ELEMENT_TYPE } from 'globalConstants';
import { rgba } from 'polished';
import { FC, memo, useContext, useEffect, useState } from 'react';
import { DropTargetMonitor, useDrop } from 'react-dnd';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  FONT_SIZE_SUBTITLE,
  LINE_HEIGHT_HEADING,
  SPACE,
  SPACE_MD,
  SPACE_SM,
  SPACE_XS,
} from 'styles/StyleConstants';
import { ValueOf } from 'types';
import { uuidv4 } from 'utils/utils';
import ChartDraggableElement from './ChartDraggableElement';
import ChartDraggableElementField from './ChartDraggableElementField';
import ChartDraggableElementHierarchy from './ChartDraggableElementHierarchy';
import { getDefaultAggregate, updateDataConfigByField } from './utils';

type DragItem = {
  index?: number;
};

export const ChartDraggableTargetContainer: FC<ChartDataConfigSectionProps> =
  memo(function ChartDraggableTargetContainer({
    ancestors,
    modalSize,
    config,
    translate: t = (...args) => args?.[0],
    onConfigChanged,
  }) {
    const { dataset } = useContext(ChartDatasetContext);
    const { drillOption } = useContext(ChartDrillContext);
    const { dataView, availableSourceFunctions } =
      useContext(VizDataViewContext);
    const [currentConfig, setCurrentConfig] = useState(config);
    const [showModal, contextHolder] = useFieldActionModal({
      i18nPrefix: 'viz.palette.data.enum.actionType',
    });
    const { aggregation } = useContext(ChartAggregationContext);

    useEffect(() => {
      setCurrentConfig(config);
    }, [config]);

    const [{ isOver, canDrop }, drop] = useDrop(
      () => ({
        accept: [
          CHART_DRAG_ELEMENT_TYPE.DATASET_COLUMN,
          CHART_DRAG_ELEMENT_TYPE.DATASET_COLUMN_GROUP,
          CHART_DRAG_ELEMENT_TYPE.DATA_CONFIG_COLUMN,
        ],
        drop(item: ChartDataSectionField & DragItem, monitor) {
          let items = Array.isArray(item) ? item : [item];
          let needDelete = true;

          if (
            monitor.getItemType() === CHART_DRAG_ELEMENT_TYPE.DATASET_COLUMN
          ) {
            const currentColumns: ChartDataSectionField[] = (
              currentConfig.rows || []
            ).concat(
              items.map(val => {
                let config: ChartDataSectionField = {
                  uid: uuidv4(),
                  ...val,
                  aggregate: getDefaultAggregate(val, currentConfig),
                };

                return config;
              }),
            );
            updateCurrentConfigColumns(currentConfig, currentColumns, true);
          } else if (
            monitor.getItemType() ===
            CHART_DRAG_ELEMENT_TYPE.DATASET_COLUMN_GROUP
          ) {
            const hierarchyChildFields = items?.[0]?.children || [];
            const currentColumns: ChartDataSectionField[] = (
              currentConfig.rows || []
            ).concat(
              hierarchyChildFields.map(val => ({
                uid: uuidv4(),
                ...val,
                aggregate: getDefaultAggregate(val, currentConfig),
              })),
            );
            updateCurrentConfigColumns(currentConfig, currentColumns, true);
          } else if (
            monitor.getItemType() === CHART_DRAG_ELEMENT_TYPE.DATA_CONFIG_COLUMN
          ) {
            const originItemIndex = (currentConfig.rows || []).findIndex(
              r => r.uid === item.uid,
            );
            if (originItemIndex > -1) {
              const needRefreshData =
                currentConfig?.type === ChartDataSectionType.Group;
              needDelete = false;
              const currentColumns = updateBy(
                currentConfig?.rows || [],
                draft => {
                  draft.splice(originItemIndex, 1);
                  item.aggregate = getDefaultAggregate(item, currentConfig);
                  return draft.splice(item?.index!, 0, item);
                },
              );
              updateCurrentConfigColumns(
                currentConfig,
                currentColumns,
                needRefreshData,
              );
            } else {
              const currentColumns = updateBy(
                currentConfig?.rows || [],
                draft => {
                  item.aggregate = getDefaultAggregate(item, currentConfig);
                  return draft.splice(item?.index!, 0, item);
                },
              );
              updateCurrentConfigColumns(currentConfig, currentColumns);
            }
          }

          return { delete: needDelete };
        },
        canDrop: (item: ChartDataSectionField, monitor) => {
          let items = Array.isArray(item) ? item : [item];

          if (
            [CHART_DRAG_ELEMENT_TYPE.DATASET_COLUMN_GROUP].includes(
              monitor.getItemType() as any,
            ) &&
            ![
              ChartDataSectionType.Group,
              ChartDataSectionType.Color,
              ChartDataSectionType.Mixed,
            ].includes(currentConfig.type as ChartDataSectionType)
          ) {
            return false;
          }

          if (
            currentConfig?.disableAggregateComputedField &&
            item.category === 'aggregateComputedField'
          ) {
            return false;
          }

          if (currentConfig.allowSameField && !aggregation) {
            return true;
          }

          if (
            typeof currentConfig.actions === 'object' &&
            !items.every(val => val.type in (currentConfig.actions || {}))
          ) {
            //zh: 判断现在拖动的数据项是否可以拖动到当前容器中 en: Determine whether the currently dragged data item can be dragged into the current container
            return false;
          }

          const currentSectionDimensionRowNames = currentConfig.rows
            ?.filter(
              r =>
                !items
                  ?.map(i => i?.uid)
                  ?.filter(Boolean)
                  ?.includes(r?.uid),
            )
            ?.map(col => col.colName);

          if (
            items[0].category ===
            ChartDataViewFieldCategory.DateLevelComputedField
          ) {
            if (
              ![
                ChartDataSectionType.Group,
                ChartDataSectionType.Color,
                ChartDataSectionType.Mixed,
              ].includes(currentConfig.type as ChartDataSectionType)
            ) {
              return false;
            }
            return currentSectionDimensionRowNames
              ? currentSectionDimensionRowNames.every(
                  v => !v?.includes(items[0].colName),
                )
              : true;
          }

          if (aggregation && items[0].type === DataViewFieldType.NUMERIC) {
            return true;
          }

          const isNotExist = items.every(
            i => !currentSectionDimensionRowNames?.includes(i.colName),
          );
          return isNotExist;
        },
        collect: (monitor: DropTargetMonitor) => ({
          isOver: monitor.isOver(),
          canDrop: monitor.canDrop(),
        }),
      }),
      [onConfigChanged, currentConfig, dataView, dataset],
    );

    const updateCurrentConfigColumns = (
      currentConfig,
      newColumns,
      refreshDataset = false,
    ) => {
      const newCurrentConfig = updateByKey(currentConfig, 'rows', newColumns);
      setCurrentConfig(newCurrentConfig);
      onConfigChanged?.(ancestors, newCurrentConfig, refreshDataset);
    };

    const onDraggableItemMove = (dragIndex: number, hoverIndex: number) => {
      if (!canDrop) {
        return;
      }
      const draggedItem = currentConfig.rows?.[dragIndex];
      if (draggedItem) {
        const newCurrentConfig = updateBy(currentConfig, draft => {
          const columns = draft.rows || [];
          columns.splice(dragIndex, 1);
          columns.splice(hoverIndex, 0, draggedItem);
        });
        setCurrentConfig(newCurrentConfig);
      }
    };

    const handleOnDeleteItem = config => () => {
      if (config.uid) {
        let newCurrentConfig = updateBy(currentConfig, draft => {
          draft.rows = draft.rows?.filter(c => c.uid !== config.uid);
          if (
            config.category ===
            ChartDataViewFieldCategory.DateLevelComputedField
          ) {
            draft.replacedConfig = config;
          }
        });
        setCurrentConfig(newCurrentConfig);
        onConfigChanged?.(ancestors, newCurrentConfig, true);
      }
    };

    const renderDropItems = () => {
      if (
        !currentConfig.rows ||
        !currentConfig?.rows?.filter(Boolean)?.length
      ) {
        const fieldCount = reachLowerBoundCount(currentConfig?.limit, 0);
        if (fieldCount > 0) {
          return (
            <DropPlaceholder>
              {t('dropCount', undefined, { count: fieldCount })}
            </DropPlaceholder>
          );
        }
        return <DropPlaceholder>{t('drop')}</DropPlaceholder>;
      }

      return currentConfig.rows?.map((columnConfig, index) => {
        return (
          <ChartDraggableElement
            key={columnConfig.uid}
            index={index}
            config={columnConfig}
            content={() => {
              const contentProps = {
                modalSize: modalSize,
                config: currentConfig,
                columnConfig,
                ancestors: ancestors,
                aggregation: aggregation,
                availableSourceFunctions,
                onConfigChanged: onConfigChanged,
                handleOpenActionModal: handleOpenActionModal,
              };
              return columnConfig.category ===
                ChartDataViewFieldCategory.Hierarchy ? (
                <ChartDraggableElementHierarchy {...contentProps} />
              ) : (
                <ChartDraggableElementField {...contentProps} />
              );
            }}
            moveCard={onDraggableItemMove}
            onDelete={handleOnDeleteItem(columnConfig)}
          ></ChartDraggableElement>
        );
      });
    };

    const renderDrillFilters = () => {
      if (currentConfig?.type !== ChartDataSectionType.Filter) {
        return;
      }
      return getDillConditions()?.map(drill => {
        const field = drill.field;
        return (
          <StyledDillFilter type={field.type}>
            {getColumnRenderName(field)}
          </StyledDillFilter>
        );
      });
    };

    const getDillConditions = () => {
      return drillOption
        ?.getAllDrillDownFields()
        ?.filter(drill => Boolean(drill?.condition));
    };

    const handleFieldConfigChanged = (
      columnUid: string,
      fieldConfig: ChartDataSectionField,
      needRefresh?: boolean,
    ) => {
      if (!fieldConfig) {
        return;
      }
      const newConfig = updateDataConfigByField(
        columnUid,
        currentConfig,
        fieldConfig,
      );
      onConfigChanged?.(ancestors, newConfig, needRefresh);
    };

    const handleOpenActionModal =
      (uid: string) =>
      (actionType: ValueOf<typeof ChartDataSectionFieldActionType>) => {
        (showModal as Function)(
          uid,
          actionType,
          currentConfig,
          handleFieldConfigChanged,
          dataset,
          dataView,
          modalSize,
          aggregation,
        );
      };

    return (
      <StyledContainer ref={drop} isOver={isOver} canDrop={canDrop}>
        {renderDropItems()}
        {renderDrillFilters()}
        {contextHolder}
      </StyledContainer>
    );
  });

export default ChartDraggableTargetContainer;

const StyledContainer = styled.div<{
  isOver: boolean;
  canDrop: boolean;
}>`
  padding: ${SPACE_SM};
  color: ${p => p.theme.textColorLight};
  text-align: center;
  background-color: ${props =>
    props.canDrop
      ? rgba(props.theme.success, 0.25)
      : props.isOver
      ? rgba(props.theme.error, 0.25)
      : props.theme.emphasisBackground};
  border-radius: ${BORDER_RADIUS};

  .draggable-element:last-child {
    margin-bottom: 0;
  }
`;

const StyledDillFilter = styled.div<{
  type: DataViewFieldType;
}>`
  padding: ${SPACE_XS} ${SPACE_MD};
  margin-bottom: ${SPACE};
  font-size: ${FONT_SIZE_SUBTITLE};
  color: ${p => p.theme.componentBackground};
  cursor: move;
  background: ${p =>
    p.type === DataViewFieldType.NUMERIC ? p.theme.success : p.theme.info};
  border-radius: ${BORDER_RADIUS};
`;

const DropPlaceholder = styled.p`
  line-height: ${LINE_HEIGHT_HEADING};
`;
