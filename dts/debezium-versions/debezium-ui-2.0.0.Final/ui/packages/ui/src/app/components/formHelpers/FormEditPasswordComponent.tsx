import { FormGroup, TextInput } from '@patternfly/react-core';
import { HelpInfoIcon } from 'components';
import { useTranslation } from 'react-i18next';
import _ from 'lodash';
import React, { FC } from 'react';

export interface IFormEditPasswordComponentProps {
  label: string;
  description: string | '';
  fieldId: string;
  name: string;
  isRequired: boolean;
  initialValues: any;
}
export const FormEditPasswordComponent: FC<IFormEditPasswordComponentProps> = (
  props
) => {
  const { t } = useTranslation();
  return (
    <FormGroup
      label={props.label}
      isRequired={props.isRequired}
      labelIcon={
        <HelpInfoIcon label={props.label} description={props.description} />
      }
      fieldId={props.label}
      helperText={t("editPasswordHelperText")}
    >
      <TextInput
        name={props.label}
        value={''}
        aria-label={props.label}
      />
    </FormGroup>
  );
};
