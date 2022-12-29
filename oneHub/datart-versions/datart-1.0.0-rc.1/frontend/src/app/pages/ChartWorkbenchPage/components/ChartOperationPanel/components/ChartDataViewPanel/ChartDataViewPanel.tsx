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
  FormOutlined,
  InfoCircleOutlined,
  MoreOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import {
  Button,
  Input,
  Menu,
  message,
  Popover,
  Space,
  Tooltip,
  TreeSelect,
} from 'antd';
import { MenuListItem, ToolbarButton } from 'app/components';
import { Confirm, ConfirmProps } from 'app/components/Confirm';
import { ChartDataViewFieldCategory } from 'app/constants';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import useMount from 'app/hooks/useMount';
import useStateModal, { StateModalSize } from 'app/hooks/useStateModal';
import useToggle from 'app/hooks/useToggle';
import ChartDataViewContext from 'app/pages/ChartWorkbenchPage/contexts/ChartDataViewContext';
import workbenchSlice from 'app/pages/ChartWorkbenchPage/slice';
import {
  dataviewsSelector,
  makeDataviewTreeSelector,
} from 'app/pages/ChartWorkbenchPage/slice/selectors';
import { fetchViewDetailAction } from 'app/pages/ChartWorkbenchPage/slice/thunks';
import { useAccess, useCascadeAccess } from 'app/pages/MainPage/Access';
import {
  PermissionLevels,
  ResourceTypes,
} from 'app/pages/MainPage/pages/PermissionPage/constants';
import { ChartConfig } from 'app/types/ChartConfig';
import ChartDataView from 'app/types/ChartDataView';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { getAllColumnInMeta } from 'app/utils/chartHelper';
import { checkComputedFieldAsync } from 'app/utils/fetch';
import { updateByKey } from 'app/utils/mutation';
import {
  FC,
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import styled from 'styled-components/macro';
import {
  ORANGE,
  SPACE,
  SPACE_MD,
  SPACE_TIMES,
  SPACE_XS,
} from 'styles/StyleConstants';
import { getPath, modelListFormsTreeByTableName } from 'utils/utils';
import { getAllFieldsOfEachType } from '../../utils';
import { ChartDraggableSourceGroupContainer } from '../ChartDraggable';
import ChartComputedFieldSettingPanel from './components/ChartComputedFieldSettingPanel';

const ChartDataViewPanel: FC<{
  dataView?: ChartDataView;
  defaultViewId?: string;
  chartConfig?: ChartConfig;
  onDataViewChange?: (clear?: boolean) => void;
}> = memo(({ dataView, defaultViewId, chartConfig, onDataViewChange }) => {
  const t = useI18NPrefix(`viz.workbench.dataview`);
  const tView = useI18NPrefix('view');
  const dispatch = useDispatch();
  const history = useHistory();
  const [showModal, modalContextHolder] = useStateModal({});
  const { availableSourceFunctions } = useContext(ChartDataViewContext);
  const dataviewTreeSelector = useMemo(makeDataviewTreeSelector, []);
  const getSelectable = useCallback(v => !v.isFolder, []);
  const dataviewTreeData = useSelector(state =>
    dataviewTreeSelector(state, getSelectable),
  );
  const [confirmProps, setConfirmProps] = useState<ConfirmProps>({});

  const [isDisplayAddNewModal, setIsDisplayAddNewModal] = useToggle();
  const views = useSelector(dataviewsSelector);
  const [allMetaFields, setAllMetaFields] = useState<ChartDataViewMeta[]>([]);
  const [isGroup, setIsGroup] = useState<boolean>(true);
  const [sortType, setSortType] = useState<string>('byNameSort');
  const [isShowSearch, setIsShowSearch] = useState<boolean>(false);

  const path = useMemo(() => {
    return views?.length && dataView
      ? getPath(
          views as Array<{ id: string; parentId: string }>,
          { id: dataView.id, parentId: dataView.parentId },
          ResourceTypes.View,
        )
      : [];
  }, [views, dataView]);

  const managePermission = useCascadeAccess({
    module: ResourceTypes.View,
    path,
    level: PermissionLevels.Manage,
  });
  const allowManage = managePermission(true);
  const allowEnableView = useAccess({
    type: 'module',
    module: ResourceTypes.View,
    id: '',
    level: PermissionLevels.Enable,
  })(true);

  const { filteredData: filteredTreeData, debouncedSearch: treeSearch } =
    useDebouncedSearch(allMetaFields, (keywords, d) => {
      return d?.name.toLowerCase().includes(keywords.toLowerCase());
    });

  const handleDataViewChange = useCallback(
    value => {
      if (dataView?.id === value) {
        return false;
      }
      let Data = chartConfig?.datas?.filter(v => v.rows && v.rows.length);
      if (Data?.length) {
        setConfirmProps({
          visible: true,
          title: t('toggleViewTip'),
          width: 500,
          icon: <InfoCircleOutlined style={{ color: ORANGE }} />,
          footer: (
            <Space>
              <Button onClick={() => setConfirmProps({ visible: false })}>
                {t('cancel')}
              </Button>
              <Button
                onClick={() => {
                  onDataViewChange?.(true);
                  setConfirmProps({ visible: false });
                  dispatch(fetchViewDetailAction(value));
                }}
              >
                {t('empty')}
              </Button>
              <Button
                onClick={() => {
                  onDataViewChange?.();
                  setConfirmProps({ visible: false });
                  dispatch(fetchViewDetailAction(value));
                }}
                type="primary"
              >
                {t('reserve')}
              </Button>
            </Space>
          ),
        });
      } else {
        onDataViewChange?.();
        dispatch(fetchViewDetailAction(value));
      }
    },
    [chartConfig?.datas, dataView?.id, dispatch, onDataViewChange, t],
  );

  const filterDateViewTreeNode = useCallback(
    (inputValue, node) =>
      node.title.toLowerCase().includes(inputValue.toLowerCase()),
    [],
  );

  const handleAddNewOrUpdateComputedField = useCallback(
    async (field?: ChartDataViewMeta, originId?: string) => {
      if (!field) {
        return Promise.reject('field is empty');
      }

      try {
        await checkComputedFieldAsync(dataView?.sourceId, field.expression);
      } catch (error) {
        message.error(error as any);
        return;
      }

      const otherComputedFields = dataView?.computedFields?.filter(
        f => f.name !== originId,
      );
      const isNameConflict = !!otherComputedFields?.find(
        f => f.name === field?.name,
      );
      if (isNameConflict) {
        const nameConflictError = tView('computedFieldNameExistWarning');
        message.error(nameConflictError);
        return Promise.reject(nameConflictError);
      }

      const currentFieldIndex = (dataView?.computedFields || []).findIndex(
        f => f.name === originId,
      );

      if (currentFieldIndex >= 0) {
        const newComputedFields = updateByKey(
          dataView?.computedFields,
          currentFieldIndex,
          field,
        );
        dispatch(
          workbenchSlice.actions.updateCurrentDataViewComputedFields(
            newComputedFields!,
          ),
        );
        return;
      }
      const newComputedFields = (dataView?.computedFields || []).concat([
        field,
      ]);
      dispatch(
        workbenchSlice.actions.updateCurrentDataViewComputedFields(
          newComputedFields,
        ),
      );
    },
    [dataView?.computedFields, dataView?.sourceId, dispatch, tView],
  );

  const handleDeleteComputedField = fieldId => {
    const newComputedFields = (dataView?.computedFields || []).filter(
      f => f.name !== fieldId,
    );

    dispatch(
      workbenchSlice.actions.updateCurrentDataViewComputedFields(
        newComputedFields,
      ),
    );
  };

  const handleEditComputedField = fieldId => {
    const editField = (dataView?.computedFields || []).find(
      f => f.name === fieldId,
    );

    handleAddOrEditComputedField(editField);
  };

  const buildFieldsForComputedFieldSettingPanel = useCallback(
    meta => {
      if (dataView?.type === 'SQL') {
        return getAllColumnInMeta(meta);
      } else {
        const allColumn = getAllColumnInMeta(meta);
        const tableNameList: string[] = [];
        const columnNameObj: { [key: string]: any } = {};
        const columnTreeData: any = [];

        allColumn?.forEach((v, i) => {
          const path = v.path;
          const tableName = path?.slice(0, path?.length - 1).join('.') || '';
          if (!tableNameList.includes(tableName)) {
            tableNameList.push(tableName);
          }
        });

        allColumn?.forEach((v, i) => {
          const path = v.path;
          const tableName = path?.slice(0, path?.length - 1).join('.') || '';
          if (tableNameList.includes(tableName)) {
            const fieldName = path?.[path.length - 1];
            const columnNameArr = columnNameObj[tableName];
            columnNameObj[tableName] = columnNameArr
              ? [...columnNameArr, { title: fieldName, key: path }]
              : [{ title: fieldName, key: path }];
          }
        });

        tableNameList.forEach(v => {
          columnTreeData.push({
            title: v,
            key: v,
            selectable: false,
            children: columnNameObj[v],
          });
        });

        return columnTreeData;
      }
    },
    [dataView?.type],
  );

  const handleAddOrEditComputedField = useCallback(
    (field?: ChartDataViewMeta) => {
      (showModal as Function)({
        title: t('createComputedFields'),
        modalSize: StateModalSize.MIDDLE,
        content: onChange => (
          <ChartComputedFieldSettingPanel
            computedField={field}
            sourceId={dataView?.sourceId}
            fields={buildFieldsForComputedFieldSettingPanel(dataView?.meta)}
            variables={dataView?.meta?.filter(
              c => c.category === ChartDataViewFieldCategory.Variable,
            )}
            allComputedFields={dataView?.computedFields}
            viewType={dataView?.type}
            onChange={onChange}
          />
        ),
        onOk: newField =>
          handleAddNewOrUpdateComputedField(newField, field?.name),
        onButtonProps: { display: field?.isViewComputedFields },
      });
    },
    [
      buildFieldsForComputedFieldSettingPanel,
      dataView?.computedFields,
      dataView?.meta,
      dataView?.type,
      dataView?.sourceId,
      handleAddNewOrUpdateComputedField,
      showModal,
      t,
    ],
  );

  const GroupMetaFields = useCallback(
    sortType => {
      const {
        hierarchyFields,
        stringFields,
        numericFields,
        dateLevelFields,
        stringComputedFields,
        numericComputedFields,
        dateComputedFields,
      } = getAllFieldsOfEachType({
        sortType,
        dataView,
        availableSourceFunctions,
      });

      const columnTreeData = modelListFormsTreeByTableName(
        [...stringFields, ...dateLevelFields, ...numericFields],
        'analysisPage',
      );

      return [
        ...hierarchyFields,
        ...columnTreeData,
        ...stringComputedFields,
        ...numericComputedFields,
        ...dateComputedFields,
      ];
    },
    [availableSourceFunctions, dataView],
  );

  const noGroupMetaFields = useCallback(
    sortType => {
      const {
        hierarchyFields,
        dateLevelFields,
        stringFields,
        numericFields,
        stringComputedFields,
        numericComputedFields,
        dateComputedFields,
      } = getAllFieldsOfEachType({
        sortType,
        dataView,
        availableSourceFunctions,
      });
      return [
        ...hierarchyFields,
        ...stringFields,
        ...stringComputedFields,
        ...dateComputedFields,
        ...dateLevelFields,
        ...numericFields,
        ...numericComputedFields,
      ];
    },
    [availableSourceFunctions, dataView],
  );

  const buildAllMetaFields = useCallback(
    (isGroup: boolean, sortType) => {
      if (dataView?.type === 'SQL' || !isGroup) {
        setAllMetaFields(noGroupMetaFields(sortType));
      } else {
        setAllMetaFields(GroupMetaFields(sortType));
      }
    },
    [noGroupMetaFields, GroupMetaFields, dataView?.type],
  );

  const editView = useCallback(() => {
    let orgId = dataView?.orgId as string;
    let viewId = dataView?.id as string;
    history.push(`/organizations/${orgId}/views/${viewId}`);
  }, [dataView?.id, dataView?.orgId, history]);

  const handleConfirmVisible = useCallback(() => {
    (showModal as Function)({
      title: '',
      modalSize: StateModalSize.XSMALL,
      content: () => t('editViewTip'),
      onOk: editView,
    });
  }, [editView, showModal, t]);

  const handleClickMenu = useCallback(
    ({ key }) => {
      switch (key) {
        case 'createComputedFields':
          setIsDisplayAddNewModal();
          handleAddOrEditComputedField();
          break;
        case 'byGroup':
          setIsGroup(true);
          buildAllMetaFields(true, sortType);
          break;
        case 'byNoGroup':
          setIsGroup(false);
          buildAllMetaFields(false, sortType);
          break;
        case 'byNameSort':
          setSortType(key);
          buildAllMetaFields(isGroup, key);
          break;
        case 'byOriginalFieldSort':
          setSortType(key);
          buildAllMetaFields(isGroup, key);
          break;
        case 'searchField':
          setIsDisplayAddNewModal();
          setIsShowSearch(!isShowSearch);
          break;
        default:
          break;
      }
    },
    [
      handleAddOrEditComputedField,
      setIsDisplayAddNewModal,
      buildAllMetaFields,
      isGroup,
      sortType,
      isShowSearch,
    ],
  );

  useMount(() => {
    if (defaultViewId) {
      handleDataViewChange(defaultViewId);
    }
  });

  useEffect(() => {
    buildAllMetaFields(dataView?.type === 'STRUCT', 'byNameSort');
  }, [dataView?.type, buildAllMetaFields]);

  return (
    <StyledChartDataViewPanel>
      <Header>
        <Tooltip placement="topLeft" title={t('editView')}>
          <ToolbarButton
            disabled={!(allowEnableView && allowManage && dataView)}
            iconSize={14}
            icon={<FormOutlined />}
            size="small"
            onClick={handleConfirmVisible}
          />
        </Tooltip>
        <TreeSelect
          showSearch
          placeholder={t('plsSelectDataView')}
          className="view-selector"
          treeData={dataviewTreeData}
          value={dataView?.id}
          onChange={handleDataViewChange}
          filterTreeNode={filterDateViewTreeNode}
          bordered={false}
        />
        <Popover
          placement="bottomRight"
          visible={isDisplayAddNewModal}
          onVisibleChange={() => setIsDisplayAddNewModal()}
          trigger="click"
          overlayClassName="datart-popup"
          content={
            <Menu
              onClick={handleClickMenu}
              defaultSelectedKeys={[
                'byNameSort',
                isGroup ? 'byGroup' : 'byNoGroup',
              ]}
            >
              <MenuListItem key="searchField">{t('searchField')}</MenuListItem>
              <MenuListItem key="createComputedFields">
                {t('createComputedFields')}
              </MenuListItem>
              <MenuListItem
                disabled={dataView?.type !== 'STRUCT'}
                title={t('Group')}
                key="group"
                sub
              >
                <MenuListItem key="byGroup">
                  {t('byDataBaseGroup')}
                </MenuListItem>
                <MenuListItem key="byNoGroup">{t('noGroup')}</MenuListItem>
              </MenuListItem>
              <MenuListItem title={t('Sort')} key="sort" sub>
                <MenuListItem key="byNameSort">{t('byNameSort')}</MenuListItem>
                <MenuListItem key="byOriginalFieldSort">
                  {t('noSort')}
                </MenuListItem>
              </MenuListItem>
            </Menu>
          }
        >
          <ToolbarButton icon={<MoreOutlined />} size="small" />
        </Popover>
        {modalContextHolder}
      </Header>
      <StyleSearchbar visible={isShowSearch}>
        <Input
          autoFocus
          className="search-input"
          prefix={<SearchOutlined className="icon" />}
          placeholder={t('searchField')}
          bordered={false}
          onChange={treeSearch}
        />
      </StyleSearchbar>
      <Confirm {...confirmProps} />

      <ChartDraggableSourceGroupContainer
        meta={filteredTreeData as ChartDataViewMeta[]}
        onDeleteComputedField={handleDeleteComputedField}
        onEditComputedField={handleEditComputedField}
      />
    </StyledChartDataViewPanel>
  );
});

export default ChartDataViewPanel;

const StyledChartDataViewPanel = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: ${p => p.theme.componentBackground};
`;

const Header = styled.div`
  display: flex;
  flex-shrink: 0;
  align-items: center;
  padding: ${SPACE} ${SPACE_XS};

  .view-selector {
    flex: 1;
    overflow: hidden;
  }
`;

const StyleSearchbar = styled.div<{ visible: boolean }>`
  display: ${p => (p.visible ? 'block' : 'none')};
  padding: ${SPACE} 0;

  .search-input {
    padding: ${SPACE} ${SPACE_MD};

    .icon {
      margin-right: ${SPACE_TIMES(1.5)};
      color: ${p => p.theme.textColorDisabled};
    }
  }
`;
