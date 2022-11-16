import './FilterConfigStep.css';
import { DataCollection, FilterValidationResult } from '@debezium/ui-models';
import { Services } from '@debezium/ui-services';
import {
  ActionGroup,
  Button,
  Divider,
  Form,
  Text,
  TextVariants,
} from '@patternfly/react-core';
import {
  FilterTreeComponent,
  FilterExcludeFieldComponent,
  FilterInputFieldComponent,
  NoPreviewFilterField,
} from 'components';
import _ from 'lodash';
import React, { SetStateAction } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ConfirmationButtonStyle,
  ConfirmationDialog,
  fetch_retry,
  getFilterConfigurationPageContent,
  mapToObject,
} from 'shared';

export interface IFilterConfigStepProps {
  propertyValues: Map<string, string>;
  filterValues: Map<string, string>;
  connectorType: string;
  updateFilterValues: (data: Map<string, string>) => void;
  setIsValidFilter: (val: SetStateAction<boolean>) => void;
  selectedConnectorType: string;
}

const formatResponseData = (data: DataCollection[]) => {
  return data.reduce((acc: any, next) => {
    const inx = _.findIndex(acc, { name: next.namespace, id: next.namespace });
    if (inx !== -1) {
      acc[inx].children.push({
        name: next.name,
        id: next.namespace + '_' + next.name,
      });
    } else {
      const newObj = {
        name: next.namespace,
        id: next.namespace,
        children: [
          {
            name: next.name,
            id: next.namespace + '_' + next.name,
          },
        ],
      };
      acc.push(newObj);
    }
    return acc;
  }, []);
};

export const FilterConfigStep: React.FunctionComponent<
  IFilterConfigStepProps
> = (props) => {
  const { t } = useTranslation();
  const [formData, setFormData] = React.useState<Map<string, string>>(
    new Map()
  );
  const [treeData, setTreeData] = React.useState<any[]>([]);
  const [invalidMsg, setInvalidMsg] = React.useState<Map<string, string>>(
    new Map()
  );
  const [columnOrFieldFilter, setColumnOrFieldFilter] =
    React.useState<string>('');
  const [childNo, setChildNo] = React.useState<number>(0);
  const [showClearDialog, setShowClearDialog] = React.useState<boolean>(false);

  const [loading, setLoading] = React.useState(true);
  const [apiError, setApiError] = React.useState<boolean>(false);
  const [errorMsg, setErrorMsg] = React.useState<Error>(new Error());

  const connectorService = Services.getConnectorService();

  const applyFilter = () => {
    getFilterSchema(true);
  };

  const clearFilter = () => {
    setShowClearDialog(true);
  };

  const isColumnOrFieldFilterApplied = (formVal: Map<string, string>) => {
    let includeFilter = '';
    formVal.forEach((val, key) => {
      if (key.includes('column') || key.includes('field')) {
        includeFilter = key.includes('column') ? 'column' : 'field';
      }
    });
    setColumnOrFieldFilter(includeFilter);
  };

  const getFilterSchema = (
    saveFilter: boolean,
    filterExpression: Map<string, string> = formData
  ) => {
    setLoading(true);
    if (apiError) {
      setApiError(false);
      setErrorMsg(new Error());
    }
    fetch_retry(connectorService.validateFilters, connectorService, [
      props.connectorType,
      mapToObject(new Map([...props.propertyValues, ...filterExpression])),
    ])
      .then((result: FilterValidationResult) => {
        if (result.status === 'INVALID') {
          const errorMap = new Map();
          for (const e of result.propertyValidationResults) {
            errorMap.set(e.property, e.message);
          }
          setInvalidMsg(errorMap);
          props.setIsValidFilter(false);
          setTreeData([]);
          setChildNo(result.matchedCollections.length);
        } else {
          saveFilter && props.updateFilterValues(filterExpression);
          props.setIsValidFilter(true);
          setInvalidMsg(new Map());
          setChildNo(result.matchedCollections.length);
          setTreeData(formatResponseData(result.matchedCollections));
        }
        isColumnOrFieldFilterApplied(filterExpression);
        setLoading(false);
      })
      .catch((err: React.SetStateAction<Error>) => {
        setApiError(true);
        setErrorMsg(err);
      });
  };

  const doCancel = () => {
    setShowClearDialog(false);
  };

  const doClear = () => {
    if (apiError) {
      setApiError(false);
      setErrorMsg(new Error());
    }
    props.setIsValidFilter(true);
    setFormData(new Map());
    getFilterSchema(true, new Map());
    setShowClearDialog(false);
  };

  React.useEffect(() => {
    getFilterSchema(false, props.filterValues);
    setFormData(new Map(props.filterValues));
  }, []);

  React.useEffect(() => {
    if (formData.size === 0 || _.isEqual(formData, props.filterValues)) {
      props.setIsValidFilter(true);
    } else {
      props.setIsValidFilter(false);
    }
  }, [formData]);

  const filterConfigurationPageContentObj: any =
    getFilterConfigurationPageContent(props.selectedConnectorType || '');

  return (
    <>
      <Text component={TextVariants.h2}>
        {t('filterPageHeadingText', {
          parent: filterConfigurationPageContentObj.fieldArray[0].field,
          child: filterConfigurationPageContentObj.fieldArray[1].field,
        })}
      </Text>
      <Form className="child-selection-step_form">
        {filterConfigurationPageContentObj.fieldArray.map((fieldFilter: any) =>
          fieldFilter.preview ? (
            <FilterInputFieldComponent
              key={fieldFilter.field}
              fieldName={fieldFilter.field}
              filterValues={props.filterValues}
              setFormData={setFormData}
              formData={formData}
              invalidMsg={invalidMsg}
              fieldExcludeList={`${fieldFilter.field}.exclude.list`}
              fieldIncludeList={`${fieldFilter.field}.include.list`}
              fieldPlaceholder={fieldFilter.valueSample}
              i18nFilterFieldLabel={t('filterFieldLabel', {
                field: _.capitalize(fieldFilter.field),
              })}
              i18nFilterFieldHelperText={t('filterFieldHelperText', {
                field: fieldFilter.field,
              })}
              i18nInclude={t('include')}
              i18nExclude={t('exclude')}
              i18nFilterFieldInfoMsg={t('filterFieldInfoMsg', {
                field: fieldFilter.field,
                sampleVal: fieldFilter.valueSample,
              })}
            />
          ) : (
            <NoPreviewFilterField
              key={fieldFilter.field}
              i18nShowFilter={t('showFilter', { field: fieldFilter.field })}
              i18nHideFilter={t('hideFilter', { field: fieldFilter.field })}
            >
              {fieldFilter.excludeFilter ? (
                <FilterExcludeFieldComponent
                  fieldName={fieldFilter.field}
                  filterValues={props.filterValues}
                  setFormData={setFormData}
                  formData={formData}
                  invalidMsg={invalidMsg}
                  fieldExcludeList={`${fieldFilter.field}.exclude.list`}
                  fieldPlaceholder={fieldFilter.valueSample}
                  i18nFilterExcludeFieldLabel={t('filterExcludeFieldLabel', {
                    field: _.capitalize(fieldFilter.field),
                  })}
                  i18nFilterFieldInfoMsg={t('filterFieldInfoMsg', {
                    field: `${fieldFilter.field} exclude`,
                    sampleVal: fieldFilter.valueSample,
                  })}
                />
              ) : (
                <FilterInputFieldComponent
                  fieldName={fieldFilter.field}
                  filterValues={props.filterValues}
                  setFormData={setFormData}
                  formData={formData}
                  invalidMsg={invalidMsg}
                  fieldExcludeList={`${fieldFilter.field}.exclude.list`}
                  fieldIncludeList={`${fieldFilter.field}.include.list`}
                  fieldPlaceholder={fieldFilter.valueSample}
                  i18nFilterFieldLabel={t('filterFieldLabel', {
                    field: _.capitalize(fieldFilter.field),
                  })}
                  i18nFilterFieldHelperText={t('filterFieldHelperText', {
                    field: fieldFilter.field,
                  })}
                  i18nInclude={t('include')}
                  i18nExclude={t('exclude')}
                  i18nFilterFieldInfoMsg={t('filterFieldInfoMsg', {
                    field: fieldFilter.field,
                    sampleVal: fieldFilter.valueSample,
                  })}
                />
              )}
            </NoPreviewFilterField>
          )
        )}
        <ActionGroup>
          <Button variant="secondary" onClick={applyFilter}>
            {t('apply')}
          </Button>
          <Button variant="link" isInline={true} onClick={clearFilter}>
            {t('clearFilters')}
          </Button>
        </ActionGroup>
      </Form>
      <Divider />
      <FilterTreeComponent
        treeData={treeData}
        loading={loading}
        apiError={apiError}
        errorMsg={errorMsg}
        columnOrFieldFilter={!!columnOrFieldFilter}
        invalidMsg={invalidMsg}
        childNo={childNo}
        filterValues={props.filterValues}
        clearFilter={clearFilter}
        i18nApiErrorTitle={t('apiErrorTitle')}
        i18nApiErrorMsg={t('apiErrorMsg')}
        i18nNoMatchingTables={t('noMatchingTables', {
          name: filterConfigurationPageContentObj.fieldArray[1].field,
        })}
        i18nNoMatchingFilterExpMsg={t('noMatchingFilterExpMsg', {
          name: filterConfigurationPageContentObj.fieldArray[1].field,
        })}
        i18nInvalidFilters={t('invalidFilters')}
        i18nInvalidFilterExpText={t('invalidFilterExpText', {
          name: filterConfigurationPageContentObj.fieldArray[1].field,
        })}
        i18nInvalidFilterText={t('invalidFilterText', {
          name: filterConfigurationPageContentObj.fieldArray[1].field,
        })}
        i18nMatchingFilterExpMsg={t('matchingFilterExpMsg', {
          name: filterConfigurationPageContentObj.fieldArray[1].field,
        })}
        i18nClearFilterText={t('clearFilterText', {
          parent: filterConfigurationPageContentObj.fieldArray[0].field,
          child: filterConfigurationPageContentObj.fieldArray[1].field,
        })}
        i18nClearFilters={t('clearFilters')}
        i18nFilterExpressionResultText={t('filterExpressionResultText', {
          name: filterConfigurationPageContentObj.fieldArray[1].field,
        })}
        i18nColumnOrFieldFilter={_.capitalize(
          t('columnOrFieldFilter', { fieldName: columnOrFieldFilter })
        )}
      />
      <ConfirmationDialog
        buttonStyle={ConfirmationButtonStyle.NORMAL}
        i18nCancelButtonText={t('cancel')}
        i18nConfirmButtonText={t('clear')}
        i18nConfirmationMessage={t('clearFilterConfMsg')}
        i18nTitle={t('clearFilters')}
        showDialog={showClearDialog}
        onCancel={doCancel}
        onConfirm={doClear}
      />
    </>
  );
};
