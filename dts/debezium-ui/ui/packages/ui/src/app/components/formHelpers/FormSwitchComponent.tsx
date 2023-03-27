import { HelpInfoIcon } from './HelpInfoIcon';
import { Split, SplitItem, Switch } from '@patternfly/react-core';
import { useField } from 'formik';
import * as React from 'react';

export interface IFormSwitchComponentProps {
  label: string;
  description: string;
  fieldId: string;
  name: string;
  isChecked: boolean;
  propertyChange: (name: string, selection: any) => void;
  setFieldValue: (
    field: string,
    value: any,
    shouldValidate?: boolean | undefined
  ) => void;
}

export const FormSwitchComponent: React.FunctionComponent<
  IFormSwitchComponentProps
> = (props) => {
  const [field] = useField(props);
  const handleChange = (value: boolean) => {
    props.propertyChange(field.name, value);
    props.setFieldValue(field.name, value);
  };
  return (
    <Split>
      <SplitItem>
        <Switch
          id={field.name + Math.random()}
          name={field.name}
          aria-label={props.label}
          label={props.label}
          labelOff={props.label}
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
