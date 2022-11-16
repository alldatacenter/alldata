import './DataOptionsStep.css';
import {
  ConnectorProperty,
  PropertyValidationResult,
} from '@debezium/ui-models';
import { DataOptionsComponent } from 'components';
import { Form, Formik, useFormikContext } from 'formik';
import _ from 'lodash';
import * as React from 'react';
import { formatPropertyDefinitions, PropertyCategory } from 'shared';

export interface IDataOptionsStepProps {
  propertyDefinitions: ConnectorProperty[];
  propertyValues: Map<string, string>;
  invalidMsg: PropertyValidationResult[];
  i18nAdvancedMappingPropertiesText: string;
  i18nMappingPropertiesText: string;
  i18nSnapshotPropertiesText: string;
  setDataOptionsValid: () => void;
  setDataStepsValid: () => void;
  onValidateProperties: (
    connectorProperties: Map<string, string>,
    propertyCategory: PropertyCategory
  ) => void;
  clearValidationError: () => void;
}

const FormSubmit: React.FunctionComponent<any> = React.forwardRef(
  (props, ref) => {
    const { dirty, submitForm, validateForm } = useFormikContext();
    React.useImperativeHandle(ref, () => ({
      validate() {
        validateForm();
        submitForm();
      },
    }));
    React.useEffect(() => {
      if (dirty) {
        props.setDataOptionsValid(!dirty);
        props.setDataStepsValid(0);
      }
    }, [props.setDataOptionsValid, props.setDataStepsValid, dirty]);
    return null;
  }
);

export const DataOptionsStep: React.FC<any> = React.forwardRef((props, ref) => {
  const mappingGeneralPropertyDefinitions = formatPropertyDefinitions(
    props.propertyDefinitions.filter(
      (defn: any) => defn.category === PropertyCategory.DATA_OPTIONS_GENERAL
    )
  );
  const mappingAdvancedPropertyDefinitions = formatPropertyDefinitions(
    props.propertyDefinitions.filter(
      (defn: any) => defn.category === PropertyCategory.DATA_OPTIONS_ADVANCED
    )
  );
  const snapshotPropertyDefinitions = formatPropertyDefinitions(
    props.propertyDefinitions.filter(
      (defn: any) => defn.category === PropertyCategory.DATA_OPTIONS_SNAPSHOT
    )
  );

  const getInitialValues = (combined: any) => {
    const combinedValue: any = {};
    const userValues: Map<string, string> = new Map([...props.propertyValues]);

    combined.map(
      (key: { name: string; defaultValue: string; type: string }) => {
        if (!combinedValue[key.name]) {
          if (userValues.size === 0) {
            key.defaultValue === undefined
              ? (combinedValue[key.name] =
                  key.type === 'INT' || key.type === 'LONG' ? 0 : '')
              : (combinedValue[key.name] = key.defaultValue);
          } else {
            combinedValue[key.name] = userValues.get(
              key.name.replace(/&/g, '.')
            );
          }
        }
      }
    );

    return combinedValue;
  };

  const initialValues = getInitialValues(
    _.union(
      mappingGeneralPropertyDefinitions,
      mappingAdvancedPropertyDefinitions,
      snapshotPropertyDefinitions
    )
  );

  const handleSubmit = (valueMap: Map<string, string>) => {
    const dataValueMap: Map<string, string> = new Map();
    for (const dataValue of props.propertyDefinitions) {
      dataValueMap.set(
        dataValue.name.replace(/&/g, '.'),
        valueMap[dataValue.name]
      );
    }
    props.onValidateProperties(
      dataValueMap,
      PropertyCategory.DATA_OPTIONS_GENERAL
    );
  };

  return (
    <div className={'data-options-component-page'}>
      <Formik
        initialValues={initialValues}
        onSubmit={(values) => {
          handleSubmit(values);
        }}
        enableReinitialize={true}
      >
        {({ errors, touched, setFieldValue }) => (
          <Form className="pf-c-form">
            <DataOptionsComponent
              propertyDefinitions={props.propertyDefinitions}
              propertyValues={props.propertyValues}
              i18nAdvancedMappingPropertiesText={
                props.i18nAdvancedMappingPropertiesText
              }
              i18nMappingPropertiesText={props.i18nMappingPropertiesText}
              i18nSnapshotPropertiesText={props.i18nSnapshotPropertiesText}
              invalidMsg={props.invalidMsg}
              setFieldValue={setFieldValue}
              errors={errors}
              touched={touched}
              clearValidationError={props.clearValidationError}
            />
            <FormSubmit
              ref={ref}
              setDataOptionsValid={props.setDataOptionsValid}
              setDataStepsValid={props.setDataStepsValid}
            />
          </Form>
        )}
      </Formik>
    </div>
  );
});
