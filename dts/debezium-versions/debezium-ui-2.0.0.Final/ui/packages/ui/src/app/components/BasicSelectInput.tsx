import './BasicSelectInput.css';
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from '@patternfly/react-core';
import * as React from 'react';

export interface IBasicSelectInputProps {
  label?: string;
  fieldId: string;
  options: string[];
  propertyChange: (name: string, selection?: any) => void;
}

export const BasicSelectInput = (props: IBasicSelectInputProps) => {
  const { label, fieldId, options, propertyChange } = props;

  const [isOpen, setOpen] = React.useState<boolean>(false);
  const [selected, setSelected] = React.useState<boolean>(false);

  const onToggle = (open: boolean) => {
    setOpen(open);
  };

  const clearSelection = () => {
    setSelected(false);
    setOpen(false);
  };

  const onSelect = (e: any, selection: any, isPlaceholder: any) => {
    if (isPlaceholder) {
      clearSelection();
    } else {
      setSelected(selection);
      setOpen(false);
      propertyChange(selection);
    }
  };

  return (
    <FormGroup label={label} fieldId={fieldId} name={fieldId}>
      <Select
        className="basic-select-input"
        variant={SelectVariant.single}
        aria-label="Select Input"
        onToggle={onToggle}
        onSelect={onSelect}
        selections={selected}
        isOpen={isOpen}
      >
        {options.map((option: any, index) => (
          <SelectOption
            key={index}
            value={option}
            isPlaceholder={option.isPlaceholder}
          />
        ))}
      </Select>
    </FormGroup>
  );
};
