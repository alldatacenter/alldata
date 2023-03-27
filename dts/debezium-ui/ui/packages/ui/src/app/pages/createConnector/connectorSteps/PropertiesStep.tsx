import './PropertiesStep.css';
import {
  ConnectorProperty,
  PropertyValidationResult,
} from '@debezium/ui-models';
import { PropertiesStepsComponent } from 'components';
import { Form, Formik, useFormikContext } from 'formik';
import _ from 'lodash';
import * as React from 'react';
import { formatPropertyDefinitions, PropertyCategory } from 'shared';
import * as Yup from 'yup';

export interface IPropertiesStepProps {
  connectorType: string | undefined;
  basicPropertyDefinitions: ConnectorProperty[];
  basicPropertyValues: Map<string, string>;
  advancedPropertyDefinitions: ConnectorProperty[];
  advancedPropertyValues: Map<string, string>;
  invalidMsg: PropertyValidationResult[];
  i18nIsRequiredText: string;
  i18nAdvancedPropertiesText: string;
  i18nAdvancedPublicationPropertiesText: string;
  i18nAdvancedReplicationPropertiesText: string;
  i18nBasicPropertiesText: string;
  setConnectionPropsValid: () => void;
  setConnectionStepsValid: () => void;
  onValidateProperties: (
    basicPropertyValues: Map<string, string>,
    advancePropertyValues: Map<string, string>
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
        props.setConnectionPropsValid(!dirty);
        props.setConnectionStepsValid(0);
      }
    }, [props.setConnectionPropsValid, props.setConnectionStepsValid, dirty]);
    return null;
  }
);

export const PropertiesStep: React.FC<any> = React.forwardRef((props, ref) => {
  const basicValidationSchema = {};

  console.log('PropertiesStep props', props.basicPropertyDefinitions);

  const namePropertyDefinitions = formatPropertyDefinitions(
    props.basicPropertyDefinitions.filter(
      (defn: any) => defn.category === PropertyCategory.CONNECTOR_NAME
    )
  );
  const basicPropertyDefinitions = formatPropertyDefinitions(
    props.basicPropertyDefinitions.filter(
      (defn: any) => defn.category === PropertyCategory.BASIC
    )
  );
  const advancedGeneralPropertyDefinitions = formatPropertyDefinitions(
    props.advancedPropertyDefinitions.filter(
      (defn: any) =>
        defn.category === PropertyCategory.ADVANCED_GENERAL ||
        defn.category === PropertyCategory.ADVANCED_SSL
    )
  );
  const advancedReplicationPropertyDefinitions = formatPropertyDefinitions(
    props.advancedPropertyDefinitions.filter(
      (defn: any) => defn.category === PropertyCategory.ADVANCED_REPLICATION
    )
  );
  const advancedPublicationPropertyDefinitions = formatPropertyDefinitions(
    props.advancedPropertyDefinitions.filter(
      (defn: any) => defn.category === PropertyCategory.ADVANCED_PUBLICATION
    )
  );

  const allBasicDefinitions = _.union(
    namePropertyDefinitions,
    basicPropertyDefinitions
  );
  allBasicDefinitions.map((key: any) => {
    if (key.type === 'STRING') {
      basicValidationSchema[key.name] = Yup.string();
    } else if (key.type === 'PASSWORD') {
      basicValidationSchema[key.name] = Yup.string();
    } else if (
      key.type === 'INT' ||
      key.type === 'LONG' ||
      key.type === 'NON-NEG-INT' ||
      key.type === 'NON-NEG-LONG' ||
      key.type === 'POS-INT'
    ) {
      basicValidationSchema[key.name] = Yup.number().strict();
    } else if (key.type === 'LIST') {
      basicValidationSchema[key.name] = Yup.string();
    }
    if (key.isMandatory) {
      basicValidationSchema[key.name] = basicValidationSchema[
        key.name
      ]?.required(`${key.displayName} ${props.i18nIsRequiredText}`);
    }
  });

  const validationSchema = Yup.object().shape({ ...basicValidationSchema });

  const getInitialValues = (combined: any) => {
    const combinedValue: any = {};
    const userValues: Map<string, string> = new Map([
      ...props.basicPropertyValues,
      ...props.advancedPropertyValues,
    ]);

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
      namePropertyDefinitions,
      basicPropertyDefinitions,
      advancedGeneralPropertyDefinitions,
      advancedReplicationPropertyDefinitions,
      advancedPublicationPropertyDefinitions
    )
  );
  const handleSubmit = (valueMap: Map<string, string>) => {
    // the basic properties
    const basicValueMap: Map<string, string> = new Map();
    for (const basicVal of props.basicPropertyDefinitions) {
      basicValueMap.set(
        basicVal.name.replace(/&/g, '.'),
        valueMap[basicVal.name]
      );
    }
    // the advance properties
    const advancedValueMap: Map<string, string> = new Map();
    for (const advancedValue of props.advancedPropertyDefinitions) {
      advancedValueMap.set(
        advancedValue.name.replace(/&/g, '.'),
        valueMap[advancedValue.name]
      );
    }
    props.onValidateProperties(basicValueMap, advancedValueMap);
  };

  return (
    <div className={'properties-step-page'}>
      <Formik
        initialValues={initialValues}
        validationSchema={validationSchema}
        onSubmit={(values) => {
          handleSubmit(values);
        }}
        enableReinitialize={true}
      >
        {({ errors, touched, setFieldValue }) => (
          <Form className="pf-c-form">
            <PropertiesStepsComponent
              connectorType={props.connectorType}
              basicPropertyDefinitions={props.basicPropertyDefinitions}
              basicPropertyValues={props.basicPropertyValues}
              advancedPropertyDefinitions={props.advancedPropertyDefinitions}
              advancedPropertyValues={props.advancedPropertyValues}
              invalidMsg={props.invalidMsg}
              i18nAdvancedPropertiesText={props.i18nAdvancedPropertiesText}
              i18nAdvancedPublicationPropertiesText={
                props.i18nAdvancedPublicationPropertiesText
              }
              i18nAdvancedReplicationPropertiesText={
                props.i18nAdvancedReplicationPropertiesText
              }
              i18nBasicPropertiesText={props.i18nBasicPropertiesText}
              setFieldValue={setFieldValue}
              errors={errors}
              touched={touched}
              clearValidationError={props.clearValidationError}
            />
            <FormSubmit
              ref={ref}
              setConnectionPropsValid={props.setConnectionPropsValid}
              setConnectionStepsValid={props.setConnectionStepsValid}
            />
          </Form>
        )}
      </Formik>
    </div>
  );
});
