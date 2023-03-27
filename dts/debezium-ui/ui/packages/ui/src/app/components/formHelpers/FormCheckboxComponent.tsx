import './FormCheckboxComponent.css';
import { HelpInfoIcon } from './HelpInfoIcon';
import { Checkbox, Split, SplitItem } from '@patternfly/react-core';
import { useField } from 'formik';
import * as React from 'react';

export interface IFormCheckboxComponentProps {
  label: string;
  fieldId: string;
  name: string;
  description: string;
  isChecked: boolean;
  propertyChange: (name: string, selection: any) => void;
  setFieldValue: (
    field: string,
    value: any,
    shouldValidate?: boolean | undefined
  ) => void;
}

export const FormCheckboxComponent: React.FunctionComponent<
  IFormCheckboxComponentProps
> = (props) => {
  const [field] = useField(props);
  const handleChange = (value: boolean) => {
    props.setFieldValue(field.name, value);
  };
  return (
    <Split>
      <SplitItem>
        <Checkbox
          className="form-checkbox-component"
          id={field.name}
          name={field.name}
          aria-label={props.label}
          label={props.label}
          isChecked={field.value}
          onChange={handleChange}
        />
      </SplitItem>
      <SplitItem>
        <HelpInfoIcon label={props.label} description={props.description} />
      </SplitItem>
    </Split>
  );
};
