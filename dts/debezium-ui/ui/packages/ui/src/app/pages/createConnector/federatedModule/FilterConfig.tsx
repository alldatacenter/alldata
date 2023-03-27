import './FilterConfig.css';
import {
  ActionGroup,
  Alert,
  Button,
  Flex,
  FlexItem,
  Form,
  FormGroup,
  Label,
  Popover,
  Text,
  TextVariants,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import {
  ConfigurationMode,
  FilterExcludeFieldComponent,
  FilterInputFieldComponent,
} from 'components';
import _ from 'lodash';
import React from 'react';
import { useTranslation } from 'react-i18next';
import {
  checkForContradictingFilters,
  ConfirmationButtonStyle,
  ConfirmationDialog,
  getFilterConfigurationPageContent,
} from 'shared';

export interface IFilterConfigProps {
  uiPath: ConfigurationMode;
  filterValues: Map<string, string>;
  connectorType: string;
  updateFilterValues: (data: Map<string, string>) => void;
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

const getPropertyFilterType = (config: Map<string, string>, filter: string) => {
  let key = '';
  [...config.keys()].forEach((k) => {
    if (k.includes(filter)) {
      key = k;
    }
  });
  return key.split('.')[1];
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
  const [showContradictingFilterAlert, setShowContradictingFilterAlert] =
    React.useState<boolean>(false);

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
    setFormData(new Map());
    props.updateFilterValues(new Map());
    setShowClearDialog(false);
  };

  React.useEffect(() => {
    !_.isEqual(props.filterValues, formData) &&
      props.updateFilterValues(formData);
    setShowContradictingFilterAlert(
      checkForContradictingFilters(formData, props.connectorType)
    );
  }, [formData]);

  const filterConfigurationPageContentObj: any =
    getFilterConfigurationPageContent(props.connectorType || '');

  return (
    <>
      {showContradictingFilterAlert && (
        <Alert
          variant="info"
          isInline
          title={t('contradictingFilterMsg')}
        />
      )}
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
                    helperText={
                      !!getPropertyValue(
                        props.filterValues,
                        fieldFilter.field
                      ) &&
                      (getPropertyFilterType(
                        props.filterValues,
                        fieldFilter.field
                      ) === 'exclude'
                        ? t('filterExcludeFieldHelperText', {
                            field: fieldFilter.field,
                          })
                        : t('filterIncludeFieldHelperText', {
                            field: fieldFilter.field,
                          }))
                    }
                    labelIcon={
                      <Popover
                        bodyContent={
                          <div style={{ whiteSpace: 'pre-line' }}>
                            {t('filterFieldInfoMsg', {
                              field: `${fieldFilter.field} exclude`,
                              sampleVal: fieldFilter.valueSample,
                            })}
                            <Button
                              variant="link"
                              isInline
                              target={'_blank'}
                              component="a"
                              href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions"
                            >
                              Learn more
                            </Button>
                            &nbsp;about regular expressions.
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
                      <Flex className="pf-u-pt-xs">
                        <FlexItem>
                          <Label variant="outline">
                            {_.capitalize(
                              getPropertyFilterType(
                                props.filterValues,
                                fieldFilter.field
                              )
                            )}
                          </Label>
                        </FlexItem>
                        <FlexItem>
                          <Text component={TextVariants.p}>
                            {getPropertyValue(
                              props.filterValues,
                              fieldFilter.field
                            )}
                          </Text>
                        </FlexItem>
                      </Flex>
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
                fieldFilter.excludeFilter ? (
                  <FilterExcludeFieldComponent
                    key={fieldFilter.field}
                    fieldName={fieldFilter.field}
                    filterValues={props.filterValues}
                    setFormData={setFormData}
                    formData={formData}
                    invalidMsg={invalidMsg}
                    fieldExcludeList={`${fieldFilter.field}.exclude.list`}
                    fieldPlaceholder={fieldFilter.valueSample}
                    i18nFilterFieldInfoMsg={t('filterFieldInfoMsg', {
                      field: `${fieldFilter.field} exclude`,
                      sampleVal: fieldFilter.valueSample,
                    })}
                  />
                ) : (
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
                    i18nFilterFieldInfoMsg={t('filterFieldInfoMsg', {
                      field: fieldFilter.field,
                      sampleVal: fieldFilter.valueSample,
                    })}
                  />
                )
            )}
            <ActionGroup>
              <Button variant="link" isInline={true} onClick={clearFilter}>
                {t('clearFilters')}
              </Button>
            </ActionGroup>
          </>
        )}
      </Form>
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
