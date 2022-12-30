import './FormMaskOrTruncateComponent.css';
import { HelpInfoIcon } from './HelpInfoIcon';
import { MaskOrTruncateItem } from './MaskOrTruncateItem';
import {
  Button,
  FormGroup,
  InputGroup,
  Stack,
  StackItem,
  Tooltip,
} from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { useField } from 'formik';
import * as React from 'react';

export interface IFormMaskOrTruncateComponentProps {
  label: string;
  description: string;
  name: string;
  fieldId: string;
  helperTextInvalid?: any;
  isRequired: boolean;
  validated: 'default' | 'success' | 'warning' | 'error';
  i18nAddDefinitionText: string;
  i18nAddDefinitionTooltip: string;
  i18nRemoveDefinitionTooltip: string;
  propertyChange: (name: string, selection: any) => void;
  setFieldValue: (
    field: string,
    value: any,
    shouldValidate?: boolean | undefined
  ) => void;
}

export const FormMaskOrTruncateComponent: React.FunctionComponent<
  IFormMaskOrTruncateComponentProps
> = (props) => {
  const [field] = useField(props);

  const getItemRows = () => {
    return field.value?.split('@^');
  };

  const handleMaskTruncateItemChanged = (
    rowId: number,
    maskTruncateValue: string
  ) => {
    // Break into rows
    const rows = [...getItemRows()];
    // replace element with updated content
    rows[rowId] = maskTruncateValue;
    // Join elements back together
    const newValue = rows.join('@^');
    // Set new value
    props.setFieldValue(field.name, newValue, true);
    props.propertyChange(field.name, newValue);
  };

  const handleDeleteMaskTruncateItem = (rowIndex: number) => {
    // Break into rows
    const rows = [...getItemRows()];
    rows.splice(rowIndex, 1);
    // Join elements back together
    const newValue = rows.join('@^');
    // Set new value
    props.setFieldValue(field.name, newValue, true);
    props.propertyChange(field.name, newValue);
  };

  const onAddDefinition = () => {
    const newValue = field.value + '@^';
    props.setFieldValue(field.name, newValue, true);
    props.propertyChange(field.name, newValue);
  };

  const getColumnsValue = (row: string) => {
    if (row && row.includes('&&')) {
      return row.split('&&')[0];
    }
    return '';
  };

  const getNValue = (row: string) => {
    if (row && row.includes('&&')) {
      return row.split('&&')[1];
    }
    return '';
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
        <Stack hasGutter={true} className={'form-mask-or-truncate-component'}>
          {getItemRows()?.map((row: string, idx: number) => (
            <StackItem key={idx}>
              <MaskOrTruncateItem
                rowId={idx}
                columnsValue={getColumnsValue(row)}
                nValue={getNValue(row)}
                canDelete={getItemRows().length > 1}
                i18nRemoveDefinitionTooltip={props.i18nRemoveDefinitionTooltip}
                maskTruncateItemChanged={handleMaskTruncateItemChanged}
                deleteMaskTruncateItem={handleDeleteMaskTruncateItem}
              />
            </StackItem>
          ))}
          <StackItem>
            <Tooltip
              position={'right'}
              content={props.i18nAddDefinitionTooltip}
            >
              <Button variant="link" onClick={onAddDefinition}>
                {props.i18nAddDefinitionText}
              </Button>
            </Tooltip>
          </StackItem>
        </Stack>
      </InputGroup>
    </FormGroup>
  );
};
