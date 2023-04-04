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

import { Form, Modal } from 'antd';
import { Split } from 'app/components';
import { ChartDataViewFieldCategory, DataViewFieldType } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { BoardContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardProvider';
import widgetManagerInstance from 'app/pages/DashBoardPage/components/WidgetManager';
import {
  getCanLinkControlWidgets,
  getViewIdsInControlConfig,
  makeControlRelations,
} from 'app/pages/DashBoardPage/components/Widgets/ControllerWidget/config';
import { selectViewMap } from 'app/pages/DashBoardPage/pages/Board/slice/selector';
import {
  ControllerWidgetContent,
  RelatedView,
  WidgetControllerPanelParams,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import {
  convertToWidgetMap,
  getOtherStringControlWidgets,
} from 'app/pages/DashBoardPage/utils/widget';
import produce from 'immer';
import React, {
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_XS } from 'styles/StyleConstants';
import { WidgetActionContext } from '../../../../components/ActionProvider/WidgetActionProvider';
import { editBoardStackActions, editDashBoardInfoActions } from '../../slice';
import { selectSortAllWidgets } from '../../slice/selectors';
import {
  addWidgetsToEditBoard,
  getEditControllerOptions,
} from '../../slice/thunk';
import { WidgetControlForm } from './ControllerConfig';
import { RelatedViewForm } from './RelatedViewForm';
import { RelatedWidgetItem, RelatedWidgets } from './RelatedWidgets';
import {
  getInitWidgetController,
  postControlConfig,
  preformatControlConfig,
} from './utils';

const ControllerWidgetPanel: React.FC<WidgetControllerPanelParams> = memo(
  ({ type, widgetId, controllerType }) => {
    const dispatch = useDispatch();
    const t = useI18NPrefix('viz.common.enum.controllerFacadeTypes');
    const tGMT = useI18NPrefix(`global.modal.title`);

    const { boardType, queryVariables } = useContext(BoardContext);

    const hasQueryControl = false;
    const { onRefreshWidgetsByController } = useContext(WidgetActionContext);
    const allWidgets = useSelector(selectSortAllWidgets);
    const widgets = useMemo(
      () => getCanLinkControlWidgets(allWidgets).filter(t => t.id !== widgetId),
      [allWidgets, widgetId],
    );
    const otherStrTypeController = useMemo(
      () => getOtherStringControlWidgets(allWidgets, widgetId),
      [allWidgets, widgetId],
    );

    const widgetMap = useMemo(
      () => convertToWidgetMap(allWidgets),
      [allWidgets],
    );
    const viewMap = useSelector(selectViewMap);
    //
    const [relatedWidgets, setRelatedWidgets] = useState<RelatedWidgetItem[]>(
      [],
    );

    const [visible, setVisible] = useState(false);
    useEffect(() => {
      const hide = !type || type === 'hide';
      setVisible(!hide);
    }, [type]);
    const [form] = Form.useForm<ControllerWidgetContent>();

    const curFilterWidget = useMemo(
      () => widgetMap[widgetId] || undefined,
      [widgetId, widgetMap],
    );

    const refreshLinkedWidgets = useCallback(
      (widget: Widget) => {
        if (hasQueryControl) return;
        onRefreshWidgetsByController(widget);
      },
      [onRefreshWidgetsByController, hasQueryControl],
    );
    const getFormRelatedViews = useCallback(() => {
      return form?.getFieldValue('relatedViews') as RelatedView[];
    }, [form]);

    const setViewsRelatedView = useCallback(
      (relatedWidgets: RelatedWidgetItem[]) => {
        const relatedViews = getFormRelatedViews();
        const nextRelatedViews: RelatedView[] = [];
        relatedWidgets.forEach(widgetItem => {
          const oldViewItem = relatedViews?.find(
            view => view.viewId === widgetItem.viewId && viewMap[view.viewId],
          );
          const newViewItem = nextRelatedViews.find(
            view => view.viewId === widgetItem.viewId && viewMap[view.viewId],
          );
          if (!newViewItem) {
            if (oldViewItem) {
              nextRelatedViews.push({ ...oldViewItem });
            } else {
              const view = viewMap[widgetItem.viewId];
              if (!view) return;
              const relatedView: RelatedView = {
                viewId: view.id,
                relatedCategory: ChartDataViewFieldCategory.Field,
                fieldValue: '',
                fieldValueType: DataViewFieldType.STRING,
              };
              nextRelatedViews.push(relatedView);
            }
          }
        });
        return nextRelatedViews;
      },
      [getFormRelatedViews, viewMap],
    );
    const setFormRelatedViews = useCallback(
      (nextRelatedViews: RelatedView[]) => {
        form?.setFieldsValue({ relatedViews: nextRelatedViews });
      },
      [form],
    );
    // 初始化数据
    useEffect(() => {
      if (!curFilterWidget || !curFilterWidget?.relations) {
        form.setFieldsValue({
          config: preformatControlConfig(
            getInitWidgetController(controllerType),
            controllerType!,
          ),
          relatedViews: [],
        });

        return;
      }
      const confContent = curFilterWidget.config
        .content as ControllerWidgetContent;
      const oldRelations = curFilterWidget?.relations;
      const oldRelatedWidgetIds = oldRelations
        .filter(t => widgetMap[t.targetId])
        .map(t => {
          const tt: RelatedWidgetItem = {
            widgetId: t.targetId,
            viewId: widgetMap[t.targetId].viewIds?.[0],
          };
          return tt;
        });
      setRelatedWidgets(oldRelatedWidgetIds);
      setFormRelatedViews(setViewsRelatedView(oldRelatedWidgetIds));
      const preRelatedViews = confContent.relatedViews?.filter(t => t.viewId);
      form?.setFieldsValue({ relatedViews: preRelatedViews });

      const { config } = confContent;
      form.setFieldsValue({
        ...confContent,
        relatedViews: preRelatedViews,
        config: preformatControlConfig(config, controllerType!),
      });
    }, [
      curFilterWidget,
      controllerType,
      form,
      widgetMap,
      setFormRelatedViews,
      setViewsRelatedView,
    ]);

    const onFinish = useCallback(
      (values: ControllerWidgetContent) => {
        if (!controllerType) return;
        setVisible(false);
        const { relatedViews, config, name } = values;
        if (type === 'add') {
          let newRelations = makeControlRelations({
            sourceId: undefined,
            relatedWidgets: relatedWidgets,
            widgetMap,
            config: config,
          });
          const content: ControllerWidgetContent = {
            type: controllerType!,
            relatedViews: relatedViews,
            name: name,
            config: postControlConfig(config, controllerType!),
          };
          const viewIds = getViewIdsInControlConfig(config);

          let newWidget = widgetManagerInstance.toolkit(controllerType).create({
            boardType,
            name: name,
            relations: newRelations,
            content: content,
            viewIds: viewIds,
          });

          dispatch(addWidgetsToEditBoard([newWidget]));
          dispatch(getEditControllerOptions(newWidget.id));

          refreshLinkedWidgets(newWidget as any);
        } else if (type === 'edit') {
          let newRelations = makeControlRelations({
            sourceId: curFilterWidget.id,
            relatedWidgets: relatedWidgets,
            widgetMap,
            config: config,
          });

          const nextContent: ControllerWidgetContent = {
            ...(curFilterWidget.config.content as ControllerWidgetContent),
            name,
            relatedViews,
            config: postControlConfig(config, controllerType!),
          };

          const newWidget = produce(curFilterWidget, draft => {
            draft.relations = newRelations;
            draft.config.name = name;
            draft.config.content = nextContent;
            draft.viewIds = getViewIdsInControlConfig(config);
          });

          dispatch(editBoardStackActions.updateWidget(newWidget));
          dispatch(getEditControllerOptions(newWidget.id));
          refreshLinkedWidgets(newWidget);
        }
      },
      [
        boardType,
        controllerType,
        curFilterWidget,
        dispatch,
        refreshLinkedWidgets,
        relatedWidgets,
        type,
        widgetMap,
      ],
    );
    const onSubmit = useCallback(() => {
      form.submit();
    }, [form]);

    const formItemStyles = {
      labelCol: { span: 4 },
      wrapperCol: { span: 20 },
    };
    const afterClose = useCallback(() => {
      form.resetFields();
      setRelatedWidgets([]);
      dispatch(
        editDashBoardInfoActions.changeControllerPanel({
          type: 'hide',
          widgetId: '',
          controllerType: undefined,
        }),
      );
    }, [dispatch, form]);
    const onChangeRelatedWidgets = (values: string[]) => {
      const relatedWidgets = values.map(t => {
        const item: RelatedWidgetItem = {
          widgetId: t,
          viewId: widgetMap[t].viewIds?.[0],
        };
        return item;
      });
      setRelatedWidgets(relatedWidgets);
      setFormRelatedViews(setViewsRelatedView(relatedWidgets));
    };

    return (
      <Modal
        title={`${tGMT(type)}${t(controllerType || '')}`}
        visible={visible}
        onOk={onSubmit}
        centered
        destroyOnClose
        width={1160}
        afterClose={afterClose}
        onCancel={() => setVisible(false)}
      >
        <Form
          form={form}
          size="middle"
          {...formItemStyles}
          requiredMark={false}
          onFinish={onFinish}
          preserve
        >
          <Container className="datart-split">
            <div>
              {visible && (
                <WidgetControlForm
                  controllerType={controllerType!}
                  otherStrFilterWidgets={otherStrTypeController}
                  boardType={boardType}
                  viewMap={viewMap}
                  form={form}
                  boardVizs={allWidgets}
                  wid={widgetId}
                />
              )}
            </div>
            <div className="split-left">
              <RelatedWidgets
                relatedWidgets={relatedWidgets}
                widgets={widgets}
                onChange={onChangeRelatedWidgets}
              />
              <RelatedViewForm
                controllerType={controllerType!}
                form={form}
                viewMap={viewMap}
                queryVariables={queryVariables}
                getFormRelatedViews={getFormRelatedViews}
              />
            </div>
          </Container>
        </Form>
      </Modal>
    );
  },
);

export default ControllerWidgetPanel;
const Container = styled(Split)`
  display: flex;
  flex: 1;

  .split-left {
    padding: ${SPACE_XS};
    background-color: ${p => p.theme.componentBackground};
    border-right: 1px solid ${p => p.theme.borderColorSplit};
  }
`;
