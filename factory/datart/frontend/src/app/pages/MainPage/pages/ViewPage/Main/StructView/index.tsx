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
  CaretRightOutlined,
  CloseOutlined,
  DeleteOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { Button, Form, message, Space, Spin, Tooltip } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { CommonFormTypes } from 'globalConstants';
import produce from 'immer';
import { memo, useCallback, useContext, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components';
import {
  FONT_SIZE_ICON_MD,
  FONT_WEIGHT_MEDIUM,
  LEVEL_1,
  LEVEL_5,
  SPACE_MD,
  SPACE_SM,
  SPACE_TIMES,
  SPACE_UNIT,
  SPACE_XL,
  SPACE_XS,
} from 'styles/StyleConstants';
import { isEqualObject } from 'utils/object';
import { getInsertedNodeIndex } from 'utils/utils';
import {
  StructViewJoinType,
  ViewStatus,
  ViewViewModelStages,
} from '../../constants';
import { EditorContext } from '../../EditorContext';
import { SaveFormContext } from '../../SaveFormContext';
import { useViewSlice } from '../../slice';
import {
  selectAllSourceDatabaseSchemas,
  selectCurrentEditingViewAttr,
  selectViews,
} from '../../slice/selectors';
import { runSql, saveView } from '../../slice/thunks';
import { JoinTableProps, StructViewQueryProps } from '../../slice/types';
import { handleStringScriptToObject, isNewView } from '../../utils';
import { Toolbar } from '../Editor/Toolbar';
import SelectDataSource from './components/SelectDataSource';
import SelectJoinColumns from './components/SelectJoinColumns';
import SelectJoinType from './components/SelectJoinType';

interface StructViewProps {
  allowManage: boolean;
  allowEnableViz: boolean | undefined;
}

export const StructView = memo(
  ({ allowManage, allowEnableViz }: StructViewProps) => {
    const { actions } = useViewSlice();
    const dispatch = useDispatch();
    const { initActions } = useContext(EditorContext);
    const { showSaveForm } = useContext(SaveFormContext);
    const t = useI18NPrefix(`view.structView`);

    const structure = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'script' }),
    ) as StructViewQueryProps;
    const id = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'id' }),
    ) as string;
    const sourceId = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'sourceId' }),
    ) as string;
    const stage = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'stage' }),
    ) as ViewViewModelStages;
    const status = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'status' }),
    ) as ViewStatus;
    const viewsData = useSelector(selectViews);
    const allDatabaseSchemas = useSelector(selectAllSourceDatabaseSchemas);
    const [form] = Form.useForm();

    const handleStructureChange = useCallback(
      (table: any, type: 'MAIN' | 'JOINS', index?: number) => {
        dispatch(
          actions.changeCurrentEditingView({
            script:
              type === 'MAIN'
                ? {
                    ...structure,
                    ...table,
                    joins:
                      !table.table ||
                      isEqualObject(structure.table, table.table)
                        ? structure.joins
                        : [],
                  }
                : produce(structure, draft => {
                    draft.joins[index!] = table;
                  }),
          }),
        );
        if (type === 'MAIN' && table.sourceId) {
          dispatch(
            actions.changeCurrentEditingView({
              sourceId: table.sourceId,
            }),
          );
        }
      },
      [structure, dispatch, actions],
    );

    const handleAddTableJoin = useCallback(() => {
      dispatch(
        actions.changeCurrentEditingView({
          script: produce(structure, draft => {
            draft.joins.push({ joinType: StructViewJoinType.LeftJoin });
          }),
        }),
      );
    }, [structure, dispatch, actions]);

    const handleTableJoinType = useCallback(
      (type: StructViewJoinType, index: number) => {
        dispatch(
          actions.changeCurrentEditingView({
            script: produce(structure, draft => {
              draft.joins[index].joinType = type;
            }),
          }),
        );
      },
      [structure, dispatch, actions],
    );

    const handleTableJoin = useCallback(
      (table: any, type: 'MAIN' | 'JOINS', index: number) => {
        handleStructureChange(
          {
            ...structure.joins[index],
            ...table,
            conditions: structure.joins[index].conditions || [
              { left: [], right: [] },
            ],
          },
          type,
          index,
        );
      },
      [structure, handleStructureChange],
    );

    const handleTableJoinColumns = useCallback(
      (
        columnName: [string],
        type: 'left' | 'right',
        joinIndex: number,
        joinConditionIndex: number,
      ) => {
        handleStructureChange(
          produce(structure.joins[joinIndex], draft => {
            draft.conditions![joinConditionIndex][type] = columnName;
          }),
          'JOINS',
          joinIndex,
        );
      },
      [structure, handleStructureChange],
    );

    const handleTableJoinAddColumns = useCallback(
      (index: number) => {
        handleStructureChange(
          produce(structure.joins[index], draft => {
            draft.conditions!.push({ left: [], right: [] });
          }),
          'JOINS',
          index,
        );
      },
      [handleStructureChange, structure.joins],
    );

    const handleDeleteJoinsItem = useCallback(
      index => {
        structure.joins[index]?.conditions?.forEach((_, i) => {
          form.setFieldsValue({
            ['left' + index + i]: '',
            ['right' + index + i]: '',
          });
        });

        dispatch(
          actions.changeCurrentEditingView({
            script: produce(structure, draft => {
              draft.joins.splice(index, 1);
            }),
          }),
        );
      },
      [structure, dispatch, actions, form],
    );

    const handleInterimRunSql = useCallback(
      async (type?: 'MAIN' | 'JOINS', joinIndex?: number) => {
        try {
          let joins: JoinTableProps[] = [];

          if (joinIndex !== undefined) {
            joins = structure.joins.slice(0, joinIndex + 1);
          } else {
            joins = structure.joins;
          }
          for (let j = 0; j < joins.length; j++) {
            const join = joins[j];
            if (!join.table || !join.table.length) {
              throw new Error('请选择表格');
            }
            if (type === 'JOINS' && join.conditions) {
              for (let i = 0; i < join.conditions.length; i++) {
                const condition = join.conditions[i];
                if (
                  !condition.left ||
                  !condition.left.length ||
                  !condition.right ||
                  !condition.right.length
                ) {
                  await form.validateFields();
                }
              }
            }
          }

          if (!type) {
            await form.validateFields();
          }
          let script: StructViewQueryProps = {
            table: [],
            columns: [],
            joins: [],
          };

          if (type === 'MAIN') {
            script.table = structure.table;
            script.columns = structure.columns;
          } else if (type === 'JOINS' && joinIndex !== undefined) {
            script.table = structure.table;
            script.columns = structure.columns;
            script.joins = [structure.joins[joinIndex]];
          } else {
            script = structure;
          }
          dispatch(runSql({ id, isFragment: !!type, script }));
        } catch (errorInfo: any) {
          errorInfo.message && message.error(errorInfo.message);
        }
      },
      [dispatch, id, structure, form],
    );

    const handleDeleteConditions = useCallback(
      (joinIndex, conditionsIndex) => {
        dispatch(
          actions.changeCurrentEditingView({
            script: produce(structure, draft => {
              draft.joins[joinIndex].conditions?.splice(conditionsIndex, 1);
            }),
          }),
        );
        form.setFieldsValue({
          ['left' + joinIndex + conditionsIndex]: '',
          ['right' + joinIndex + conditionsIndex]: '',
        });
      },
      [actions, dispatch, structure, form],
    );

    const save = useCallback(
      (resolve?) => {
        dispatch(saveView({ resolve }));
      },
      [dispatch],
    );

    const callSave = useCallback(() => {
      if (
        status !== ViewStatus.Archived &&
        stage === ViewViewModelStages.Saveable
      ) {
        if (isNewView(id)) {
          showSaveForm({
            type: CommonFormTypes.Edit,
            visible: true,
            parentIdLabel: t('file'),
            initialValues: {
              name: '',
              parentId: '',
              config: {},
            },
            onSave: (values, onClose) => {
              let index = getInsertedNodeIndex(values, viewsData);
              dispatch(
                actions.changeCurrentEditingView({
                  ...values,
                  parentId: values.parentId || null,
                  index,
                }),
              );
              save(onClose);
            },
          });
        } else {
          save();
        }
      }
    }, [
      dispatch,
      actions,
      stage,
      status,
      id,
      save,
      showSaveForm,
      viewsData,
      t,
    ]);

    useEffect(() => {
      initActions({ onRun: handleInterimRunSql, onSave: callSave });
    }, [initActions, callSave, handleInterimRunSql]);

    useEffect(() => {
      if (typeof structure === 'string') {
        dispatch(
          actions.initCurrentEditingStructViewScript({
            script: handleStringScriptToObject(
              structure,
              allDatabaseSchemas[sourceId],
            ),
          }),
        );
      }
    }, [sourceId, allDatabaseSchemas, actions, dispatch, structure]);

    useEffect(() => {
      structure.joins?.forEach((join, index) => {
        join?.conditions?.forEach((condition, i) => {
          form.setFieldsValue({
            ['left' + index + i]: condition.left.slice(-1),
            ['right' + index + i]: condition.right.slice(-1),
          });
        });
      });
    }, [structure.joins, form]);

    return (
      <StructContainer>
        {typeof structure === 'string' ? (
          <LoadingWrap>
            <Spin />
          </LoadingWrap>
        ) : (
          <>
            <Toolbar
              type={'STRUCT'}
              allowManage={allowManage}
              allowEnableViz={allowEnableViz}
            />
            <ConfigPanel>
              <Form form={form} name="StructViewForm">
                <ProcessLine>
                  <ProcessItem>
                    <ProcessItemLabel>
                      <Tooltip title={t('runStep')} placement="left">
                        <Button
                          className="run-fragment"
                          icon={<CaretRightOutlined />}
                          onClick={() =>
                            allowManage && handleInterimRunSql('MAIN')
                          }
                        />
                      </Tooltip>
                      {t('main')}
                    </ProcessItemLabel>
                    <ProcessItemContent>
                      <Space>
                        <SelectDataSource
                          type="MAIN"
                          sourceId={sourceId}
                          structure={structure}
                          allowManage={allowManage}
                          onChange={handleStructureChange}
                        />
                      </Space>
                    </ProcessItemContent>
                  </ProcessItem>

                  {structure.joins.map((join, i) => {
                    return (
                      <ProcessItem>
                        <ProcessItemLabel>
                          <Tooltip title={t('runStep')} placement="left">
                            <Button
                              className="run-fragment"
                              icon={<CaretRightOutlined />}
                              onClick={() =>
                                allowManage && handleInterimRunSql('JOINS', i)
                              }
                            />
                          </Tooltip>
                          {t('join')}
                        </ProcessItemLabel>
                        <ProcessItemContent>
                          <TableRelation>
                            <SelectDataSource
                              joinTable={join}
                              structure={structure}
                              allowManage={allowManage}
                              renderType="READONLY"
                            />
                            <SelectJoinType
                              type={join.joinType!}
                              onChange={type => {
                                allowManage && handleTableJoinType(type, i);
                              }}
                            />
                            <SelectDataSource
                              type="JOINS"
                              joinTable={join}
                              sourceId={sourceId}
                              structure={structure}
                              allowManage={allowManage}
                              onChange={(table, type) =>
                                handleTableJoin(table, type, i)
                              }
                            />
                          </TableRelation>
                          {join.table && (
                            <>
                              <SelectJoinColumnLabel>
                                {t('selectJoinColumn')}
                              </SelectJoinColumnLabel>
                              <JoinConditionWrapper>
                                {join.conditions?.map(
                                  ({ left, right }, ind) => {
                                    return (
                                      <JoinConditionLine>
                                        <SelectJoinColumns
                                          structure={structure}
                                          joinTable={join}
                                          onChange={(
                                            columnName,
                                            type,
                                            joinConditionIndex,
                                          ) =>
                                            handleTableJoinColumns(
                                              columnName,
                                              type,
                                              i,
                                              joinConditionIndex,
                                            )
                                          }
                                          conditionsIndex={ind}
                                          joinIndex={i}
                                          sourceId={sourceId}
                                          allowManage={allowManage}
                                        />
                                        <Space className="action">
                                          {!!left.length &&
                                            !!right.length &&
                                            ind ===
                                              join.conditions!.length - 1 && (
                                              <Button
                                                type="link"
                                                size="small"
                                                icon={<PlusOutlined />}
                                                onClick={() =>
                                                  allowManage &&
                                                  handleTableJoinAddColumns(i)
                                                }
                                              />
                                            )}
                                          {ind ===
                                            join.conditions!.length - 1 &&
                                            ind > 0 && (
                                              <Button
                                                danger
                                                type="link"
                                                size="small"
                                                icon={<CloseOutlined />}
                                                onClick={() =>
                                                  allowManage &&
                                                  handleDeleteConditions(i, ind)
                                                }
                                              />
                                            )}
                                        </Space>
                                      </JoinConditionLine>
                                    );
                                  },
                                )}
                              </JoinConditionWrapper>
                            </>
                          )}
                        </ProcessItemContent>
                        <ProcessItemAction>
                          <Button
                            danger
                            size="small"
                            className="delete-item"
                            icon={<DeleteOutlined />}
                            onClick={() =>
                              allowManage && handleDeleteJoinsItem(i)
                            }
                          />
                        </ProcessItemAction>
                      </ProcessItem>
                    );
                  })}
                </ProcessLine>

                {!!structure.table.length && (
                  <>
                    <Button
                      disabled={
                        !!structure.joins.length &&
                        !structure.joins[structure.joins.length - 1]?.table
                      }
                      type="link"
                      className="join"
                      icon={<PlusOutlined />}
                      onClick={allowManage ? handleAddTableJoin : undefined}
                    >
                      {t('addJoin')}
                    </Button>
                    <Button
                      type="primary"
                      className="run"
                      icon={<CaretRightOutlined />}
                      onClick={() => handleInterimRunSql()}
                    >
                      {t('run')}
                    </Button>
                  </>
                )}
              </Form>
            </ConfigPanel>
          </>
        )}
      </StructContainer>
    );
  },
);

const StructContainer = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  background: ${p => p.theme.componentBackground};
`;

const ConfigPanel = styled.div`
  flex: 1;
  padding: ${SPACE_MD};
  overflow: auto;

  .join {
    display: block;
    margin: 0 0 ${SPACE_MD} 92px;
  }

  .run {
    display: block;
    margin: 0 0 ${SPACE_MD} 108px;
  }
`;

const TableRelation = styled.div`
  display: flex;
  flex-shrink: 0;
  align-items: center;
  align-self: flex-start;
  padding: ${SPACE_SM} 0;
`;

const JoinConditionWrapper = styled.div`
  padding: ${SPACE_SM} 0;
  margin-right: ${SPACE_XL};
`;

const JoinConditionLine = styled.div`
  display: flex;
  align-items: center;
  margin-top: ${SPACE_XS};

  &:first-of-type {
    margin-top: 0;
  }

  .action {
    margin-left: ${SPACE_XS};
    visibility: hidden;
  }

  &:hover {
    .action {
      visibility: visible;
    }
  }
`;

const SelectJoinColumnLabel = styled.span`
  display: flex;
  align-self: flex-start;
  justify-content: center;
  width: ${SPACE_TIMES(30)};
  padding: ${SPACE_SM} 0;
  line-height: 32px;
  color: ${p => p.theme.textColorLight};
`;

const LoadingWrap = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100px;
`;

const ProcessLine = styled.div`
  position: relative;

  &:before {
    position: absolute;
    top: 28px;
    left: ${SPACE_TIMES(18)};
    z-index: ${LEVEL_1};
    width: 4px;
    height: calc(100% - 56px);
    content: '';
    background-color: ${p => p.theme.borderColorSplit};
  }
`;

const ProcessItem = styled.div`
  position: relative;
  display: flex;

  .run-fragment {
    position: absolute;
    top: ${SPACE_SM};
    left: ${SPACE_MD};
    display: none;
  }

  .delete-item {
    display: none;
    font-size: ${FONT_SIZE_ICON_MD};
  }

  &:hover {
    background-color: ${p => p.theme.bodyBackground};

    .run-fragment {
      display: inline-block;
    }
    .delete-item {
      display: inline-block;
    }
  }

  &:before {
    position: absolute;
    top: 22px;
    left: ${SPACE_TIMES(17)};
    z-index: ${LEVEL_5};
    width: ${SPACE_TIMES(3)};
    height: ${SPACE_TIMES(3)};
    content: '';
    background-color: ${p => p.theme.primary};
    border-radius: 50%;
  }

  &:after {
    position: absolute;
    top: ${22 + SPACE_UNIT / 2}px;
    left: ${SPACE_TIMES(17.5)};
    z-index: ${LEVEL_5};
    width: ${SPACE_TIMES(2)};
    height: ${SPACE_TIMES(2)};
    content: '';
    background-color: ${p => p.theme.componentBackground};
    border-radius: 50%;
  }
`;

const ProcessItemLabel = styled.p`
  position: relative;
  flex-shrink: 0;
  width: ${SPACE_TIMES(40)};
  padding: ${SPACE_SM} 0 ${SPACE_SM} ${SPACE_TIMES(26)};
  font-weight: ${FONT_WEIGHT_MEDIUM};
  line-height: 32px;
  color: ${p => p.theme.textColorSnd};
`;

const ProcessItemContent = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  padding-left: ${SPACE_XL};
`;

const ProcessItemAction = styled.div`
  display: flex;
  flex-shrink: 0;
  align-items: center;
  margin: 0 ${SPACE_XL};
`;
