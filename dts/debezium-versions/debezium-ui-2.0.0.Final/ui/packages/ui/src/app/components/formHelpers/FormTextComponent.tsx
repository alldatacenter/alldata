import './FormTextComponent.css';
import { FormGroup, Text, TextVariants } from '@patternfly/react-core';
import { HelpInfoIcon } from 'components';
import _ from 'lodash';
import React, { FC } from 'react';
import { useTranslation } from 'react-i18next';

const pointer = require('json-pointer');

export interface IFormTextComponentProps {
  label: string;
  description: string | '';
  fieldId: string;
  name: string;
  isRequired: boolean;
  initialValues: any;
}
export const FormTextComponent: FC<IFormTextComponentProps> = (props) => {
  const { t } = useTranslation();

  const noPropertySet = (name: string) => (
    <Text className={'form-text-component_no-property'}>
      {t('propertyNotConfigured', { name })}
    </Text>
  );

  const propertyValue = pointer.get(
    props.initialValues,
    '/' + props.name.replaceAll('.', '/')
  );
  return (
    <FormGroup
      label={props.label}
      isRequired={props.isRequired}
      labelIcon={
        <HelpInfoIcon label={props.label} description={props.description} />
      }
      fieldId={props.label}
    >
      {propertyValue ? (
        <Text component={TextVariants.p}>{propertyValue}</Text>
      ) : (
        noPropertySet(props.label)
      )}
    </FormGroup>
  );
};
