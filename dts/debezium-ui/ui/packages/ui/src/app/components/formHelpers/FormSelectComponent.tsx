import { HelpInfoIcon } from './HelpInfoIcon';
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { useField } from 'formik';
import * as React from 'react';

export interface IFormSelectComponentProps {
  label: string;
  name: string;
  description: string;
  fieldId: string;
  helperTextInvalid: string;
  isRequired: boolean;
  options: string[];
  validated?: 'default' | 'success' | 'warning' | 'error' | undefined;
  propertyChange: (name: string, selection: any) => void;
  setFieldValue: (
    field: string,
    value: any,
    shouldValidate?: boolean | undefined
  ) => void;
}

export const FormSelectComponent = (props: IFormSelectComponentProps) => {
  const {
    label,
    isRequired,
    description,
    fieldId,
    helperTextInvalid,
    options,
    propertyChange,
    setFieldValue,
  } = props;

  const [isOpen, setOpen] = React.useState<boolean>(false);
  const [field] = useField(props);

  const onToggle = (open: boolean) => {
    setOpen(open);
  };

  const clearSelection = () => {
    setOpen(false);
  };

  const onSelect = (e: any, selection: any, isPlaceholder: any) => {
    if (isPlaceholder) {
      clearSelection();
    } else {
      setOpen(false);
      setFieldValue(field.name, selection);
      propertyChange(field.name, selection);
    }
  };

  const selectOptions = options.map((value: any) => {
    return { value };
  });

  return (
    <FormGroup
      label={label}
      isRequired={isRequired}
      labelIcon={<HelpInfoIcon label={label} description={description} />}
      helperTextInvalid={helperTextInvalid}
      helperTextInvalidIcon={<ExclamationCircleIcon />}
      fieldId={field.name}
      name={fieldId}
      validated={props.validated}
    >
      <Select
        variant={SelectVariant.single}
        placeholderText="Select an option"
        aria-label="Select Input"
        onToggle={onToggle}
        onSelect={onSelect}
        selections={field.value}
        isOpen={isOpen}
        validated={props.validated}
      >
        {selectOptions.map((option: any, index) => (
          <SelectOption
            key={index}
            value={option.value}
            isPlaceholder={option.isPlaceholder}
          />
        ))}
      </Select>
    </FormGroup>
  );
};
