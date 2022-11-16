import './FilterConfig.css';
import {
  ActionGroup,
  Button,
  Divider,
  Form,
  FormGroup,
  Popover,
  Text,
  TextVariants,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import {
  ConfigurationMode,
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
  getFilterConfigurationPageContent,
} from 'shared';

export interface IFilterConfigProps {
  uiPath: ConfigurationMode;
  filterValues: Map<string, string>;
  connectorType: string;
  updateFilterValues: (data: Map<string, string>) => void;
  setIsValidFilter: (val: SetStateAction<boolean>) => void;
}

const getPropertyValue = (config: Map<string, string>, filter: string) => {
  let key = '';
  [...config.keys()].forEach((k) => {
    if (k.includes(filter)) {
      key = k;
    }
  });
  return config.get(key);
};

export const FilterConfig: React.FunctionComponent<IFilterConfigProps> = (
  props
) => {
  const { t } = useTranslation();
  const [formData, setFormData] = React.useState<Map<string, string>>(
    new Map(props.filterValues)
  );
  const [invalidMsg] = React.useState<Map<string, string>>(new Map());
  const [showClearDialog, setShowClearDialog] = React.useState<boolean>(false);

  const applyFilter = () => {
    props.updateFilterValues(formData);
    props.setIsValidFilter(true);
  };

  const clearFilter = () => {
    setShowClearDialog(true);
  };

  const doCancel = () => {
    setShowClearDialog(false);
  };

  const noPropertySet = (name: string) => (
    <Text className={'form-text-component_no-property'}>
      {t('propertyNotConfigured', { name })}
    </Text>
  );

  const doClear = () => {
    props.setIsValidFilter(true);
    setFormData(new Map());
    props.updateFilterValues(new Map());
    setShowClearDialog(false);
  };

  React.useEffect(() => {
    _.isEqual(props.filterValues, formData)
      ? props.setIsValidFilter(true)
      : props.setIsValidFilter(false);
  }, [formData]);

  const filterConfigurationPageContentObj: any =
    getFilterConfigurationPageContent(props.connectorType || '');

  return (
    <div className="filter-config-page">
      <Text component={TextVariants.h2}>
        {t('filterPageHeadingText', {
          parent: filterConfigurationPageContentObj.fieldArray[0].field,
          child: filterConfigurationPageContentObj.fieldArray[1].field,
        })}
      </Text>
      <Form className="child-selection-step_form">
        {props.uiPath === ConfigurationMode.VIEW ? (
          <>
            {filterConfigurationPageContentObj.fieldArray.map(
              (fieldFilter: any) => {
                return (
                  <FormGroup
                    key={fieldFilter.field}
                    label={t('filterFieldLabel', {
                      field: _.capitalize(fieldFilter.field),
                    })}
                    fieldId={'field_filter'}
                    isRequired={false}
                    labelIcon={
                      <Popover
                        bodyContent={
                          <div>
                            {t('filterFieldInfoMsg', {
                              field: fieldFilter.field,
                              sampleVal: fieldFilter.valueSample,
                            })}
                            <br />
                            <a
                              href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions"
                              target="_blank"
                            >
                              More Info
                            </a>
                          </div>
                        }
                      >
                        <button
                          aria-label="More info for filter field"
                          onClick={(e) => e.preventDefault()}
                          aria-describedby="simple-form-filter"
                          className="pf-c-form__group-label-help"
                        >
                          <HelpIcon noVerticalAlign={true} />
                        </button>
                      </Popover>
                    }
                  >
                    {getPropertyValue(props.filterValues, fieldFilter.field) ? (
                      <Text component={TextVariants.p}>
                        {getPropertyValue(
                          props.filterValues,
                          fieldFilter.field
                        )}
                      </Text>
                    ) : (
                      noPropertySet(
                        t('filterFieldLabel', {
                          field: _.capitalize(fieldFilter.field),
                        })
                      )
                    )}
                  </FormGroup>
                );
              }
            )}
          </>
        ) : (
          <>
            {filterConfigurationPageContentObj.fieldArray.map(
              (fieldFilter: any) =>
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
                    i18nShowFilter={t('showFilter', {
                      field: fieldFilter.field,
                    })}
                    i18nHideFilter={t('hideFilter', {
                      field: fieldFilter.field,
                    })}
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
                        i18nFilterExcludeFieldLabel={t(
                          'filterExcludeFieldLabel',
                          {
                            field: _.capitalize(fieldFilter.field),
                          }
                        )}
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
          </>
        )}
      </Form>
      <Divider />
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
    </div>
  );
};
