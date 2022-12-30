import './FormMaskHashSaltComponent.css';
import { HelpInfoIcon } from './HelpInfoIcon';
import { MaskHashSaltItem } from './MaskHashSaltItem';
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

export interface IFormMaskHashSaltComponentProps {
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

export const FormMaskHashSaltComponent: React.FunctionComponent<
  IFormMaskHashSaltComponentProps
> = (props) => {
  const [field] = useField(props);

  const getItemRows = () => {
    return field.value?.split('#$');
  };

  const handleMaskHashSaltItemChanged = (
    rowId: number,
    maskHashSaltValue: string
  ) => {
    // Break into rows
    const rows = [...getItemRows()];
    // replace element with updated content
    rows[rowId] = maskHashSaltValue;
    // Join elements back together
    const newValue = rows.join('#$');
    // Set new value
    props.setFieldValue(field.name, newValue, true);
  };

  const handleDeleteMaskHashSaltItem = (rowIndex: number) => {
    // Break into rows
    const rows = [...getItemRows()];
    rows.splice(rowIndex, 1);
    // Join elements back together
    const newValue = rows.join('#$');
    // Set new value
    props.setFieldValue(field.name, newValue, true);
  };

  const onAddDefinition = () => {
    const newValue = field.value + '#$';
    props.setFieldValue(field.name, newValue, true);
  };

  /**
   * Return column segment from the supplied string
   * Format of string : columns&&hash||salt
   *   columns - first segment, ended with '&&'
   *   hash    - second segment, preceeded by '&&' and ended with '||'
   *   salt    - third segment, preceeded by '||'
   * @param val the 3 segment string
   */
  const getColsValue = (val: string) => {
    if (val && val.includes('&&')) {
      return val.split('&&')[0];
    }
    return '';
  };

  /**
   * Return hash segment from the supplied string
   * Format of string : columns&&hash||salt
   *   columns - first segment, ended with '&&'
   *   hash    - second segment, preceeded by '&&' and ended with '||'
   *   salt    - third segment, preceeded by '||'
   * @param val the 3 segment string
   */
  const getHashValue = (val: string) => {
    let hashVal = '';
    if (val && val.includes('&&')) {
      const trailing = val.split('&&')[1];
      if (trailing) {
        hashVal = trailing.split('||')[0];
      }
    }
    return hashVal;
  };

  /**
   * Return salt segment from the supplied string
   * Format of string : columns&&hash||salt
   *   columns - first segment, ended with '&&'
   *   hash    - second segment, preceeded by '&&' and ended with '||'
   *   salt    - third segment, preceeded by '||'
   * @param val the 3 segment string
   */
  const getSaltValue = (val: string) => {
    let saltVal = '';
    if (val && val.includes('||')) {
      const trailing = val.split('||')[1];
      saltVal = trailing ? trailing : '';
    }
    return saltVal;
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
        <Stack hasGutter={true} className={'form-mask-hash-salt-component'}>
          {getItemRows()?.map((row: string, idx: number) => (
            <StackItem key={idx}>
              <MaskHashSaltItem
                rowId={idx}
                columnsValue={getColsValue(row)}
                hashValue={getHashValue(row)}
                saltValue={getSaltValue(row)}
                canDelete={getItemRows().length > 1}
                i18nRemoveDefinitionTooltip={props.i18nRemoveDefinitionTooltip}
                maskHashSaltItemChanged={handleMaskHashSaltItemChanged}
                deleteMaskHashSaltItem={handleDeleteMaskHashSaltItem}
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
