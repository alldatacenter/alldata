import { PageLoader } from '..';
import './FilterTreeComponent.css';
import {
  Alert,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStateVariant,
  Title,
  TreeView,
} from '@patternfly/react-core';
import { ExclamationCircleIcon, SearchIcon } from '@patternfly/react-icons';
import React from 'react';
import { ApiError, WithLoader } from 'shared';

export interface IFilterTreeComponentProps {
  treeData: any[];
  loading: boolean;
  apiError: boolean;
  errorMsg: Error;
  columnOrFieldFilter: boolean;
  invalidMsg: Map<string, string> | undefined;
  childNo: number;
  filterValues: Map<string, string>;
  clearFilter: () => void;
  i18nApiErrorTitle?: string;
  i18nApiErrorMsg?: string;
  i18nNoMatchingTables: string;
  i18nInvalidFilters: string;
  i18nInvalidFilterExpText: string;
  i18nInvalidFilterText: string;
  i18nMatchingFilterExpMsg: string;
  i18nNoMatchingFilterExpMsg: string;
  i18nClearFilterText: string;
  i18nClearFilters: string;
  i18nFilterExpressionResultText: string;
  i18nColumnOrFieldFilter: string;
}
export const FilterTreeComponent: React.FunctionComponent<
  IFilterTreeComponentProps
> = (props) => {
  const [activeItems, setActiveItems] = React.useState<any>();

  const getColumnOrFieldMsg = () => {
    return props.columnOrFieldFilter
      ? `. ${props.i18nColumnOrFieldFilter}`
      : '';
  };

  const onClick = (evt: any, treeViewItem: any, parentItem: any) => {
    setActiveItems([treeViewItem, parentItem]);
  };
  return (
    <WithLoader
      error={props.apiError}
      loading={props.loading}
      loaderChildren={<PageLoader />}
      errorChildren={
        <ApiError
          i18nErrorTitle={props.i18nApiErrorTitle}
          i18nErrorMsg={props.i18nApiErrorMsg}
          error={props.errorMsg}
        />
      }
    >
      {() =>
        props.treeData.length === 0 ? (
          props.invalidMsg?.size === 0 ? (
            <>
              <Alert
                variant={'warning'}
                isInline={true}
                title={props.i18nNoMatchingFilterExpMsg + getColumnOrFieldMsg()}
              >
                <p>
                  {props.i18nClearFilterText + ' '}
                  <a onClick={props.clearFilter}>{props.i18nClearFilters}</a>
                </p>
              </Alert>
              <EmptyState variant={EmptyStateVariant.small}>
                <EmptyStateIcon icon={SearchIcon} />
                <Title headingLevel="h4" size="lg">
                  {props.i18nNoMatchingTables}
                </Title>
                <EmptyStateBody className="filter-tree-component_emptyBody">
                  {props.i18nNoMatchingFilterExpMsg}
                </EmptyStateBody>
              </EmptyState>
            </>
          ) : (
            <>
              <Alert
                variant={'danger'}
                isInline={true}
                title={props.i18nInvalidFilterText}
              />
              <EmptyState variant={EmptyStateVariant.small}>
                <EmptyStateIcon icon={ExclamationCircleIcon} />
                <Title headingLevel="h4" size="lg">
                  {props.i18nInvalidFilters}
                </Title>
                <EmptyStateBody className="filter-tree-component_emptyBody">
                  {props.i18nInvalidFilterExpText}
                </EmptyStateBody>
              </EmptyState>
            </>
          )
        ) : (
          <>
            <Alert
              variant={'info'}
              isInline={true}
              title={
                `${props.childNo} ${props.i18nMatchingFilterExpMsg}` +
                getColumnOrFieldMsg()
              }
            >
              {props.filterValues.size !== 0 ? (
                <p>
                  {props.i18nClearFilterText + ' '}
                  <a onClick={props.clearFilter}>{props.i18nClearFilters}</a>
                </p>
              ) : (
                ''
              )}
            </Alert>

            <TreeView
              data={props.treeData}
              activeItems={activeItems}
              onSelect={onClick}
              hasBadges={true}
            />
          </>
        )
      }
    </WithLoader>
  );
};
