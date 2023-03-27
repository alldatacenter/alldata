import {
  Button,
  Flex,
  FlexItem,
  FormGroup,
  Popover,
  Text,
  TextInput,
  TextVariants,
  ToggleGroup,
  ToggleGroupItem,
} from '@patternfly/react-core';
import { ExclamationCircleIcon, HelpIcon } from '@patternfly/react-icons';
import _ from 'lodash';
import React, { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import './FilterInputFieldComponent.css';

export interface IFilterInputFieldComponentProps {
  fieldName: string;
  filterValues: Map<string, unknown>;
  setFormData: (formData: Map<string, string>) => void;
  formData: Map<string, string>;
  invalidMsg: Map<string, string>;
  fieldExcludeList: string;
  fieldIncludeList: string;
  fieldPlaceholder: string;
  i18nFilterFieldInfoMsg: string;
}

const getInvalidFilterMsg = (
  filter: string,
  errorMsg: Map<string, string> | undefined
) => {
  let returnVal = '';
  errorMsg?.forEach((val, key) => {
    if (key.includes(filter)) {
      returnVal = val;
    }
  });
  return returnVal;
};

const getFieldExpression = (
  data: Map<string, unknown>,
  fieldExclude: string,
  fieldInclude: string
): string => {
  return (
    (data.get(fieldExclude) as string) ||
    (data.get(fieldInclude) as string) ||
    ''
  );
};

export const FilterInputFieldComponent: React.FunctionComponent<
  IFilterInputFieldComponentProps
> = (props) => {
  const { t } = useTranslation();
  const FIELD_EXCLUDE = 'fieldExclude';
  const FIELD_INCLUDE = 'fieldInclude';

  const [filterField, setFilterField] = React.useState<string>(
    getFieldExpression(
      props.filterValues,
      props.fieldExcludeList,
      props.fieldIncludeList
    )
  );

  const [fieldSelected, setFieldSelected] = React.useState<string>();

  const handleParentFilter = (val: string) => {
    setFilterField(val);
  };

  const handleParentToggle = (isSelected: any, event: any) => {
    const id = event.currentTarget.id;
    setFieldSelected(id);
  };

  useEffect(() => {
    props.filterValues.has(props.fieldExcludeList)
      ? FIELD_EXCLUDE
      : FIELD_INCLUDE;
  }, []);

  React.useEffect(() => {
    setFilterField(
      getFieldExpression(
        props.filterValues,
        props.fieldExcludeList,
        props.fieldIncludeList
      )
    );
    setFieldSelected(
      props.filterValues.has(props.fieldExcludeList)
        ? FIELD_EXCLUDE
        : FIELD_INCLUDE
    );
  }, [props.filterValues, props.fieldExcludeList, props.fieldIncludeList]);

  React.useEffect(() => {
    const formDataCopy = new Map<string, string>(props.formData);
    if (fieldSelected === FIELD_EXCLUDE) {
      formDataCopy.delete(props.fieldIncludeList);
      filterField
        ? formDataCopy.set(props.fieldExcludeList, filterField)
        : formDataCopy.delete(props.fieldExcludeList);
    } else {
      formDataCopy.delete(props.fieldExcludeList);
      filterField
        ? formDataCopy.set(props.fieldIncludeList, filterField)
        : formDataCopy.delete(props.fieldIncludeList);
    }
    props.setFormData(formDataCopy);
  }, [fieldSelected, filterField]);

  return (
    <FormGroup
      label={t('filterFieldLabel', {
        field: _.capitalize(props.fieldName),
      })}
      fieldId="field_filter"
      helperText={
        !!filterField ? (
          fieldSelected === FIELD_EXCLUDE ? (
            <Text
              component={TextVariants.h4}
              className="child-selection-step_info"
            >
              {t('filterExcludeFieldHelperText', {
                field: props.fieldName,
              })}
            </Text>
          ) : (
            <Text
              component={TextVariants.h4}
              className="child-selection-step_info"
            >
              {t('filterIncludeFieldHelperText', {
                field: props.fieldName,
              })}
            </Text>
          )
        ) : (
          <Text
              className="no-filter-configured"
            >
              {`No ${props.fieldName} filter configured.`}
            </Text>
          
        )
      }
      labelIcon={
        <Popover
          bodyContent={
            <div style={{ whiteSpace: 'pre-line' }}>
              {props.i18nFilterFieldInfoMsg}
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
      helperTextInvalid={
        props.invalidMsg?.size !== 0
          ? getInvalidFilterMsg(props.fieldName, props.invalidMsg)
          : ''
      }
      helperTextInvalidIcon={<ExclamationCircleIcon />}
      validated={
        props.invalidMsg?.size !== 0 &&
        getInvalidFilterMsg(props.fieldName, props.invalidMsg)
          ? 'error'
          : 'default'
      }
    >
      <Flex>
        <FlexItem>
          <ToggleGroup aria-label="Include Exclude field toggle group">
            <ToggleGroupItem
              buttonId={FIELD_INCLUDE}
              isSelected={fieldSelected === FIELD_INCLUDE}
              onChange={handleParentToggle}
              text={t('include')}
            />
            <ToggleGroupItem
              buttonId={FIELD_EXCLUDE}
              isSelected={fieldSelected === FIELD_EXCLUDE}
              onChange={handleParentToggle}
              text={t('exclude')}
            />
          </ToggleGroup>
        </FlexItem>
        <FlexItem className="filter-input-field">
          <TextInput
            value={filterField}
            validated={
              props.invalidMsg?.size !== 0 &&
              getInvalidFilterMsg(props.fieldName, props.invalidMsg)
                ? 'error'
                : 'default'
            }
            type="text"
            id={`${props.fieldName}-field_filter`}
            aria-describedby="field_filter-helper"
            name={`${props.fieldName}-field_filter`}
            onChange={handleParentFilter}
            placeholder={`e.g ${props.fieldPlaceholder}1, ${props.fieldPlaceholder}2`}
          />
        </FlexItem>
      </Flex>
    </FormGroup>
  );
};
