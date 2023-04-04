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
  AlignCenterOutlined,
  CaretRightOutlined,
  CopyFilled,
  MonitorOutlined,
  PauseOutlined,
  SaveFilled,
  SettingFilled,
} from '@ant-design/icons';
import { Divider, Dropdown, Menu, Select, Space, Tooltip } from 'antd';
import { ToolbarButton } from 'app/components';
import { Chronograph } from 'app/components/Chronograph';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { CommonFormTypes } from 'globalConstants';
import React, { memo, useCallback, useContext, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import { format } from 'sql-formatter';
import styled from 'styled-components/macro';
import {
  INFO,
  LEVEL_1,
  SPACE,
  SPACE_TIMES,
  SPACE_XS,
  WARNING,
} from 'styles/StyleConstants';
import { getInsertedNodeIndex } from 'utils/utils';
import { isParentIdEqual } from '../../../../slice/utils';
import { selectSources } from '../../../SourcePage/slice/selectors';
import {
  PREVIEW_SIZE_LIST,
  ViewStatus,
  ViewViewModelStages,
} from '../../constants';
import { EditorContext } from '../../EditorContext';
import { useSaveAsView } from '../../hooks/useSaveAsView';
import { useStartAnalysis } from '../../hooks/useStartAnalysis';
import { SaveFormContext } from '../../SaveFormContext';
import { useViewSlice } from '../../slice';
import {
  selectCurrentEditingViewAttr,
  selectViews,
} from '../../slice/selectors';
import { saveView } from '../../slice/thunks';
import { isNewView } from '../../utils';
interface ToolbarProps {
  allowManage: boolean;
  allowEnableViz: boolean | undefined;
  type: 'STRUCT' | 'SQL';
}

export const Toolbar = memo(
  ({ allowManage, allowEnableViz, type }: ToolbarProps) => {
    const { actions } = useViewSlice();
    const dispatch = useDispatch();
    const { onRun, onSave } = useContext(EditorContext);
    const { showSaveForm } = useContext(SaveFormContext);
    const sources = useSelector(selectSources);
    const history = useHistory();
    const histState = history.location.state as any;
    const viewsData = useSelector(selectViews);
    const t = useI18NPrefix('view.editor');
    const saveAsView = useSaveAsView();
    const startAnalysis = useStartAnalysis();
    const id = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'id' }),
    ) as string;
    const name = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'name' }),
    ) as string;
    const parentId = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'parentId' }),
    ) as string;
    const config = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'config' }),
    ) as object;
    const sourceId = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'sourceId' }),
    ) as string;
    const stage = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'stage' }),
    ) as ViewViewModelStages;
    const status = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'status' }),
    ) as ViewStatus;
    const script = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'script' }),
    ) as string;
    const fragment = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'fragment' }),
    ) as string;
    const size = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'size' }),
    ) as number;
    const error = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'error' }),
    ) as string;
    const ViewIndex = useSelector(state =>
      selectCurrentEditingViewAttr(state, { name: 'index' }),
    ) as number;
    const isArchived = status === ViewStatus.Archived;

    const formatSQL = useCallback(() => {
      dispatch(
        actions.changeCurrentEditingView({
          script: format(script, {
            denseOperators: true,
            logicalOperatorNewline: 'before',
          }),
        }),
      );
    }, [dispatch, actions, script]);

    const showEdit = useCallback(() => {
      showSaveForm({
        type: CommonFormTypes.Edit,
        visible: true,
        initialValues: {
          name,
          parentId,
          config,
        },
        parentIdLabel: t('folder'),
        onSave: (values, onClose) => {
          let index = ViewIndex;

          if (isParentIdEqual(parentId, values.parentId)) {
            index = getInsertedNodeIndex(values, viewsData);
          }

          dispatch(
            actions.changeCurrentEditingView({
              ...values,
              parentId: values.parentId || null,
              index,
            }),
          );
          dispatch(saveView({ resolve: onClose }));
        },
      });
    }, [
      showSaveForm,
      actions,
      dispatch,
      name,
      parentId,
      config,
      viewsData,
      ViewIndex,
      t,
    ]);

    const sourceChange = useCallback(
      value => {
        dispatch(actions.changeCurrentEditingView({ sourceId: value }));
      },
      [dispatch, actions],
    );

    const sizeMenuClick = useCallback(
      ({ key }) => {
        dispatch(actions.changeCurrentEditingView({ size: Number(key) }));
      },
      [dispatch, actions],
    );

    useEffect(() => {
      if (histState?.sourcesId && sources) {
        sourceChange(histState.sourcesId);
      }
    }, [histState?.sourcesId, sourceChange, sources]);

    return (
      <Container>
        <Operates>
          <Space split={<Divider type="vertical" className="divider" />}>
            {type === 'SQL' && (
              <>
                {allowManage && (
                  <Select
                    placeholder={t('source')}
                    value={sourceId}
                    bordered={false}
                    disabled={isArchived}
                    onChange={sourceChange}
                    className="source"
                  >
                    {sources.map(({ id, name }) => (
                      <Select.Option key={id} value={id}>
                        {name}
                      </Select.Option>
                    ))}
                  </Select>
                )}
                <Space>
                  <Tooltip
                    title={
                      <TipTitle
                        title={[
                          `${fragment ? t('runSelection') : t('run')}`,
                          t('runWinTip'),
                          t('runMacTip'),
                        ]}
                      />
                    }
                    placement="bottom"
                  >
                    <ToolbarButton
                      icon={
                        stage === ViewViewModelStages.Running ? (
                          <PauseOutlined />
                        ) : (
                          <CaretRightOutlined />
                        )
                      }
                      color={fragment ? WARNING : INFO}
                      onClick={onRun}
                    />
                  </Tooltip>
                  <Tooltip title={t('beautify')} placement="bottom">
                    <ToolbarButton
                      icon={<AlignCenterOutlined />}
                      disabled={isArchived}
                      onClick={formatSQL}
                    />
                  </Tooltip>
                </Space>
              </>
            )}
            <Dropdown
              trigger={['click']}
              overlay={
                <Menu onClick={sizeMenuClick}>
                  {PREVIEW_SIZE_LIST.map(s => (
                    <Menu.Item key={s}>{s}</Menu.Item>
                  ))}
                </Menu>
              }
            >
              <ToolbarButton size="small">{`Limit: ${size}`}</ToolbarButton>
            </Dropdown>
            <Chronograph
              running={stage === ViewViewModelStages.Running}
              status={
                error
                  ? 'error'
                  : stage >= ViewViewModelStages.Running
                  ? stage === ViewViewModelStages.Running
                    ? 'processing'
                    : 'success'
                  : 'default'
              }
            />
          </Space>
        </Operates>

        <Actions>
          <Space>
            {allowManage && (
              <Tooltip
                title={
                  <TipTitle
                    title={[t('save'), t('saveWinTip'), t('saveMacTip')]}
                  />
                }
                placement="bottom"
              >
                <ToolbarButton
                  icon={<SaveFilled />}
                  disabled={
                    isArchived || stage !== ViewViewModelStages.Saveable
                  }
                  color={INFO}
                  onClick={onSave}
                />
              </Tooltip>
            )}
            {allowManage && (
              <Tooltip title={t('info')} placement="bottom">
                <ToolbarButton
                  icon={<SettingFilled />}
                  disabled={isArchived || isNewView(id)}
                  color={INFO}
                  onClick={showEdit}
                />
              </Tooltip>
            )}
            {allowManage && (
              <Tooltip title={t('saveAs')} placement="bottom">
                <ToolbarButton
                  icon={<CopyFilled />}
                  onClick={() => saveAsView(id)}
                  disabled={isNewView(id)}
                  color={INFO}
                />
              </Tooltip>
            )}
            {/* <Tooltip title={t('saveFragment')} placement="bottom">
            <ToolbarButton icon={<SnippetsFilled />} />
          </Tooltip> */}
            {allowEnableViz && (
              <Tooltip title={t('startAnalysis')} placement="bottom">
                <ToolbarButton
                  disabled={isNewView(id)}
                  icon={<MonitorOutlined />}
                  color={INFO}
                  onClick={() => {
                    startAnalysis(id);
                  }}
                />
              </Tooltip>
            )}
          </Space>
        </Actions>
      </Container>
    );
  },
);

const Container = styled.div`
  z-index: ${LEVEL_1};
  display: flex;
  flex-shrink: 0;
  align-items: center;
  padding: ${SPACE} ${SPACE_XS};
  background-color: ${p => p.theme.componentBackground};
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};

  .source {
    width: ${SPACE_TIMES(40)};
  }

  .size {
    width: ${SPACE_TIMES(40)};
  }

  .divider {
    border-color: ${p => p.theme.borderColorBase};
  }
`;

const Operates = styled.div`
  display: flex;
  flex: 1;
`;

const Actions = styled.div`
  display: flex;
  flex-shrink: 0;
`;

const TipTitle = ({ title }: { title: string[] }) => {
  return (
    <TipTitleWrapper>
      {title.map((s, index) => (
        <p key={index}>{s}</p>
      ))}
    </TipTitleWrapper>
  );
};

const TipTitleWrapper = styled.div`
  display: flex;
  flex-direction: column;
`;
