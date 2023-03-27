import './FilterExcludeFieldComponent.css';
import {
  Button,
  Flex,
  FlexItem,
  FormGroup,
  Popover,
  Text,
  TextInput,
  TextVariants,
} from '@patternfly/react-core';
import { ExclamationCircleIcon, HelpIcon } from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';
import _ from 'lodash';
import React from 'react';

export interface IFilterExcludeFieldComponentProps {
  fieldName: string;
  filterValues: Map<string, unknown>;
  setFormData: (formData: Map<string, string>) => void;
  formData: Map<string, string>;
  invalidMsg: Map<string, string>;
  fieldExcludeList: string;
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
  fieldExclude: string
): string => {
  return (data.get(fieldExclude) as string) || '';
};

export const FilterExcludeFieldComponent: React.FunctionComponent<
  IFilterExcludeFieldComponentProps
> = (props) => {
  const { t } = useTranslation();
  const [filterField, setFilterField] = React.useState<string>(
    getFieldExpression(props.filterValues, props.fieldExcludeList)
  );

  const handleParentFilter = (val: string) => {
    setFilterField(val);
  };

  React.useEffect(() => {
    setFilterField(
      getFieldExpression(props.filterValues, props.fieldExcludeList)
    );
  }, [props.filterValues, props.fieldExcludeList]);

  React.useEffect(() => {
    const formDataCopy = new Map<string, string>(props.formData);
    filterField
      ? formDataCopy.set(props.fieldExcludeList, filterField)
      : formDataCopy.delete(props.fieldExcludeList);
    props.setFormData(formDataCopy);
  }, [filterField]);

  return (
    <FormGroup
      label={t('filterFieldLabel', {
        field: _.capitalize(props.fieldName),
      })}
      fieldId="field_filter"
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
      helperText={
        !!filterField ?
        (
          <Text component={TextVariants.h4} className="child-selection-step_info">
          {t('filterExcludeOnlyHelperText', {
            field: props.fieldName,
          })}
        </Text>
        ) : <Text
        className="no-filter-configured"
      >
        {`No ${props.fieldName} filter configured.`}
      </Text>
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
        <FlexItem className={'filter_exclude_field_component-input'}>
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
        <FlexItem>
          <></>
        </FlexItem>
      </Flex>
    </FormGroup>
  );
};
