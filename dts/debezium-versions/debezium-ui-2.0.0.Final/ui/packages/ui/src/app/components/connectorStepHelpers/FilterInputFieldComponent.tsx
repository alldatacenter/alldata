import {
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
import {
  ExclamationCircleIcon,
  HelpIcon,
  InfoCircleIcon,
} from '@patternfly/react-icons';
import React from 'react';

export interface IFilterInputFieldComponentProps {
  fieldName: string;
  filterValues: Map<string, unknown>;
  setFormData: (formData: Map<string, string>) => void;
  formData: Map<string, string>;
  invalidMsg: Map<string, string>;
  fieldExcludeList: string;
  fieldIncludeList: string;
  fieldPlaceholder: string;
  i18nFilterFieldLabel: string;
  i18nFilterFieldHelperText: string;
  i18nInclude: string;
  i18nExclude: string;
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
  const FIELD_EXCLUDE = 'fieldExclude';
  const FIELD_INCLUDE = 'fieldInclude';

  const [filterField, setFilterField] = React.useState<string>(
    getFieldExpression(
      props.filterValues,
      props.fieldExcludeList,
      props.fieldIncludeList
    )
  );

  const [fieldSelected, setFieldSelected] = React.useState<string>(
    props.filterValues.has(props.fieldExcludeList)
      ? FIELD_EXCLUDE
      : FIELD_INCLUDE
  );

  const handleParentFilter = (val: string) => {
    setFilterField(val);
  };

  const handleParentToggle = (isSelected: any, event: any) => {
    const id = event.currentTarget.id;
    setFieldSelected(id);
  };

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
      if (filterField) {
        formDataCopy.set(props.fieldExcludeList, filterField);
      } else {
        formDataCopy.delete(props.fieldExcludeList);
        setFieldSelected(FIELD_INCLUDE);
      }
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
      label={props.i18nFilterFieldLabel}
      fieldId="field_filter"
      helperText={
        fieldSelected === FIELD_EXCLUDE ? (
          <Text
            component={TextVariants.h4}
            className="child-selection-step_info"
          >
            <InfoCircleIcon />
            {props.i18nFilterFieldHelperText}
          </Text>
        ) : (
          ''
        )
      }
      labelIcon={
        <Popover
          bodyContent={
            <div>
              {props.i18nFilterFieldInfoMsg}
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
          <TextInput
            value={filterField}
            validated={
              props.invalidMsg?.size !== 0 &&
              getInvalidFilterMsg(props.fieldName, props.invalidMsg)
                ? 'error'
                : 'default'
            }
            type="text"
            id="field_filter"
            aria-describedby="field_filter-helper"
            name="field_filter"
            onChange={handleParentFilter}
            placeholder={`e.g ${props.fieldPlaceholder}1, ${props.fieldPlaceholder}2`}
          />
        </FlexItem>
        <FlexItem>
          <ToggleGroup aria-label="Include Exclude field toggle group">
            <ToggleGroupItem
              buttonId={FIELD_INCLUDE}
              isSelected={!!filterField && fieldSelected === FIELD_INCLUDE}
              onChange={handleParentToggle}
              onClick={(e) => e.preventDefault()}
              text={props.i18nInclude}
              isDisabled={!filterField}
            />
            <ToggleGroupItem
              buttonId={FIELD_EXCLUDE}
              isSelected={!!filterField && fieldSelected === FIELD_EXCLUDE}
              onChange={handleParentToggle}
              onClick={(e) => e.preventDefault()}
              text={props.i18nExclude}
              isDisabled={!filterField}
            />
          </ToggleGroup>
        </FlexItem>
      </Flex>
    </FormGroup>
  );
};
