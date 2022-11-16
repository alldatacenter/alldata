import { FormGroup, Select, SelectVariant } from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { HelpInfoIcon } from 'components';
import * as React from 'react';

export interface ITypeSelectorComponentProps {
  label: string;
  description: string;
  isDisabled: boolean;
  fieldId: string;
  isRequired: boolean;
  options: any[];
  value: any;
  setFieldValue: (value: any) => void;
  isInvalid: boolean;
  invalidText: string;
}

export const TypeSelectorComponent = ({
  label,
  description,
  fieldId,
  isRequired,
  isDisabled,
  options,
  value,
  setFieldValue,
  isInvalid,
  invalidText,
}: ITypeSelectorComponentProps) => {
  const [isOpen, setOpen] = React.useState<boolean>(false);

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
      setFieldValue(selection);
    }
  };

  const onFilter = (_, textInput) => {
    if (textInput === '') {
      return options;
    } else {
      const filteredGroups = options
        .map((group) => {
          const filteredGroup = React.cloneElement(group, {
            children: group.props?.children?.filter((item) => {
              return item.props?.value
                ?.toLowerCase()
                .includes(textInput.toLowerCase());
            }),
          });
          if (filteredGroup.props?.children?.length > 0) {
            return filteredGroup;
          }
          return null;
        })
        .filter(Boolean);
      return filteredGroups;
    }
  };

  return (
    <FormGroup
      label={label}
      isRequired={isRequired}
      fieldId={fieldId}
      labelIcon={<HelpInfoIcon label={label} description={description} />}
      helperTextInvalid={invalidText}
      helperTextInvalidIcon={<ExclamationCircleIcon />}
      validated={isInvalid ? 'error' : 'default'}
    >
      <Select
        variant={SelectVariant.single}
        aria-label="Select type"
        isDisabled={isDisabled}
        onToggle={onToggle}
        onSelect={onSelect}
        selections={value}
        isOpen={isOpen}
        placeholderText="Select type"
        validated={isInvalid ? 'error' : 'default'}
        onFilter={onFilter}
        isGrouped={true}
        hasInlineFilter={true}
      >
        {options}
      </Select>
    </FormGroup>
  );
};
