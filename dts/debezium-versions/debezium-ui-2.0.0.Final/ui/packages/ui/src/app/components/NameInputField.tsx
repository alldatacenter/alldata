import { FormGroup, TextInput } from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { HelpInfoIcon } from 'components';
import * as React from 'react';

export interface INameInputFieldProps {
  label: string;
  description: string;
  fieldId: string;
  isRequired: boolean;
  value: string;
  inputType: 'text';
  name: string;
  placeholder: string;
  setFieldValue: (value: any, field: string) => void;
  isInvalid: boolean;
  invalidText: string;
}

export const NameInputField = (props: INameInputFieldProps) => {
  const {
    label,
    description,
    fieldId,
    isRequired,
    name,
    placeholder,
    inputType,
    value,
    setFieldValue,
    isInvalid,
    invalidText,
  } = props;

  const saveInputText = (val: any) => {
    setFieldValue(val, props.label);
  };

  return (
    <FormGroup
      label={label}
      isRequired={isRequired}
      fieldId={fieldId}
      labelIcon={<HelpInfoIcon label={props.label} description={description} />}
      helperTextInvalid={invalidText}
      helperTextInvalidIcon={<ExclamationCircleIcon />}
      validated={isInvalid ? 'error' : 'default'}
    >
      <TextInput
        type={inputType}
        id={fieldId}
        isRequired={isRequired}
        defaultValue={value}
        onChange={saveInputText}
        aria-label="name input"
        validated={isInvalid ? 'error' : 'default'}
        name={name}
        placeholder={placeholder}
      />
    </FormGroup>
  );
};
