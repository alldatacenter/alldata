import './FormInputComponent.css';
import { HelpInfoIcon } from './HelpInfoIcon';
import { FormGroup, NumberInput, TextInput } from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { useField } from 'formik';
import * as React from 'react';

export interface IFormInputComponentProps {
  label: string;
  infoText: string | '';
  fieldId: string;
  name: string;
  infoTitle: string | '';
  helperTextInvalid?: any;
  helperText: string;
  type: any;
  isRequired: boolean;
  validated?: 'default' | 'success' | 'warning' | 'error' | undefined;
  clearValidationError: () => void;
}
export const FormInputComponent: React.FunctionComponent<
  IFormInputComponentProps
> = (props) => {
  const [field] = useField(props);

  const handleKeyPress = (keyEvent: KeyboardEvent) => {
    // disallow entry of "." and "-" for NON-NEG-INT or NON-NEG-LONG or POS-INT
    // disallow entry of "." for INT or LONG
    if (
      ((props.type === 'NON-NEG-INT' ||
        props.type === 'NON-NEG-LONG' ||
        props.type === 'POS-INT') &&
        (keyEvent.key === '.' || keyEvent.key === '-')) ||
      ((props.type === 'INT' || props.type === 'LONG') && keyEvent.key === '.')
    ) {
      keyEvent.preventDefault();
    }
  };

  const minValue = (propType: string) => {
    let result: number | null | undefined = null;
    switch (propType) {
      case 'NON-NEG-INT':
      case 'NON-NEG-LONG':
        result = 0;
        break;
      case 'POS-INT':
        result = 1;
        break;
      default:
        result = undefined;
        break;
    }
    return result;
  };

  return (
    <FormGroup
      label={props.label}
      isRequired={props.isRequired}
      labelIcon={
        <HelpInfoIcon label={props.label} description={props.infoText} />
      }
      helperTextInvalid={props.helperTextInvalid}
      helperTextInvalidIcon={<ExclamationCircleIcon />}
      fieldId={field.name}
      validated={props.validated}
      helperText={props.helperText}
    >
      {props.type === 'INT' ||
      props.type === 'LONG' ||
      props.type === 'NON-NEG-INT' ||
      props.type === 'NON-NEG-LONG' ||
      props.type === 'POS-INT' ? (
        <NumberInput
          className="my-class"
          value={field.value}
          widthChars={10}
          onChange={(e) => {
            field.onChange(field.name)(e);
            props.clearValidationError();
          }}
          inputName={field.name}
          inputAriaLabel={field.name}
          minusBtnAriaLabel="dbz_port_minus"
          plusBtnAriaLabel="dbz_port_plus"
          min={minValue(props.type)}
          onKeyPress={(event) => handleKeyPress(event as any)}
        />
      ) : (
        <TextInput
          name={field.name}
          onChange={(e) => {
            field.onChange(field.name)(e);
            props.clearValidationError();
          }}
          value={field.value}
          onBlur={(e) => {
            field.onBlur(field.name)(e);
          }}
          aria-label={field.name}
          validated={props.validated}
          type={
            props.type === 'PASSWORD'
              ? 'password'
              : 'text'
          }
          onKeyPress={(event) => handleKeyPress(event as any)}
        />
      )}
    </FormGroup>
  );
};
