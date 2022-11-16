import { HelpInfoIcon } from './HelpInfoIcon';
import {
  FormGroup,
  Grid,
  GridItem,
  InputGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
} from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { useField } from 'formik';
import * as React from 'react';

const durationUnitOptions = [
  <SelectOption key={0} value="Milliseconds" />,
  <SelectOption key={1} value="Seconds" />,
  <SelectOption key={2} value="Minutes" />,
  <SelectOption key={3} value="Hours" />,
  <SelectOption key={4} value="Days" />,
];

function getDurationUnitFactor(durationOption: any) {
  switch (durationOption) {
    case 'Milliseconds':
      return 1;
    case 'Seconds':
      return 1000;
    case 'Minutes':
      return 60000;
    case 'Hours':
      return 3600000;
    case 'Days':
      return 86400000;
    default:
      return 1;
  }
}

// Determines initial units.  Will auto-select the units which result in the lowest int value
function getInitialDurationUnits(value: any) {
  if (value === 0) {
    return 'Milliseconds';
  } else if (value / 86400000 >= 1.0 && Number.isInteger(value / 86400000)) {
    return 'Days';
  } else if (value / 3600000 >= 1.0 && Number.isInteger(value / 3600000)) {
    return 'Hours';
  } else if (value / 60000 >= 1.0 && Number.isInteger(value / 60000)) {
    return 'Minutes';
  } else if (value / 1000 >= 1.0 && Number.isInteger(value / 1000)) {
    return 'Seconds';
  } else {
    return 'Milliseconds';
  }
}

function calculateDuration(durationUnits: any, initialValue: number) {
  return initialValue / getDurationUnitFactor(durationUnits);
}

function calculateValue(durationUnits: any, value: number) {
  return value * getDurationUnitFactor(durationUnits);
}

export interface IFormDurationComponentProps {
  label: string;
  description: string;
  name: string;
  fieldId: string;
  helperTextInvalid?: any;
  isRequired: boolean;
  validated: 'default' | 'success' | 'warning' | 'error';
  propertyChange: (name: string, selection: any) => void;
  setFieldValue: (
    field: string,
    value: any,
    shouldValidate?: boolean | undefined
  ) => void;
}

/**
 * Duration component - allows user to enter a number and corresponding time units
 * - The duration value is always calculated into milliseconds (required by backend)
 * - decimals and negative values are not allowed
 * @param props the component properties
 */
export const FormDurationComponent: React.FunctionComponent<
  IFormDurationComponentProps
> = (props) => {
  const [field] = useField(props);
  const [durationUnits, setDurationUnits] = React.useState<string>();
  const [isOpen, setIsOpen] = React.useState(false);
  const handleToggle = () => {
    setIsOpen(!isOpen);
  };

  React.useEffect(() => {
    const initialDurationUnits = getInitialDurationUnits(field.value);
    setDurationUnits(initialDurationUnits);
  });
  const onSelect = (e: any, selectedDurationUnits: any) => {
    setIsOpen(false);

    const inputValue = calculateDuration(durationUnits, field.value);
    setDurationUnits(selectedDurationUnits);
    props.setFieldValue(
      field.name,
      calculateValue(selectedDurationUnits, inputValue),
      true
    );
    props.propertyChange(
      field.name,
      calculateValue(selectedDurationUnits, inputValue)
    );
  };

  const handleKeyPress = (keyEvent: KeyboardEvent) => {
    // do not allow entry of '.' or '-'
    if (keyEvent.key === '.' || keyEvent.key === '-') {
      keyEvent.preventDefault();
    }
  };

  const handleTextInputChange = (
    val: string,
    event: React.FormEvent<HTMLInputElement>
  ) => {
    props.setFieldValue(
      field.name,
      calculateValue(durationUnits, parseInt(val, 10)),
      true
    );
  };
  const handleTextInputBlur = (event: React.ChangeEvent<HTMLInputElement>) => {
    props.setFieldValue(
      field.name,
      calculateValue(durationUnits, event.target.valueAsNumber),
      true
    );
  };
  const id = field.name;

  return (
    <FormGroup
      label={props.label}
      isRequired={props.isRequired}
      labelIcon={
        <HelpInfoIcon label={props.label} description={props.description} />
      }
      helperTextInvalid={props.helperTextInvalid}
      helperTextInvalidIcon={<ExclamationCircleIcon />}
      fieldId={id}
      validated={props.validated}
    >
      <InputGroup>
        <Grid>
          <GridItem span={6}>
            <TextInput
              min={'0'}
              data-testid={id}
              id={id}
              type={'number'}
              value={`${calculateDuration(durationUnits, field.value)}`}
              validated={props.validated}
              onChange={handleTextInputChange}
              onBlur={handleTextInputBlur}
              onKeyPress={(event) => handleKeyPress(event as any)}
            />
          </GridItem>
          <GridItem span={6}>
            <Select
              variant={SelectVariant.single}
              aria-label="Select Duration Units"
              onToggle={handleToggle}
              onSelect={onSelect}
              selections={durationUnits}
              isOpen={isOpen}
            >
              {durationUnitOptions}
            </Select>
          </GridItem>
        </Grid>
      </InputGroup>
    </FormGroup>
  );
};
