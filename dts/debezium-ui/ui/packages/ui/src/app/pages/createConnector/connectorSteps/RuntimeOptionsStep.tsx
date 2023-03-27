import './RuntimeOptionsStep.css';
import {
  ConnectorProperty,
  PropertyValidationResult,
} from '@debezium/ui-models';
import { RuntimeOptionsComponent } from 'components';
import { Form, Formik, useFormikContext } from 'formik';
import _ from 'lodash';
import * as React from 'react';
import { formatPropertyDefinitions, PropertyCategory } from 'shared';
import * as Yup from 'yup';

export interface IRuntimeOptionsStepProps {
  propertyDefinitions: ConnectorProperty[];
  propertyValues: Map<string, string>;
  invalidMsg: PropertyValidationResult[];
  i18nIsRequiredText: string;
  i18nEngineProperties: string;
  i18nHeartbeatProperties: string;
  setRuntimeOptionsValid: () => void;
  setRuntimeStepsValid: () => void;
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
        props.setRuntimeOptionsValid(!dirty);
        props.setRuntimeStepsValid(0);
      }
    }, [props.setRuntimeOptionsValid, props.setRuntimeStepsValid, dirty]);
    return null;
  }
);

export const RuntimeOptionsStep: React.FC<any> = React.forwardRef(
  (props, ref) => {
    const basicValidationSchema = {};

    const enginePropertyDefinitions = formatPropertyDefinitions(
      props.propertyDefinitions.filter(
        (defn: ConnectorProperty) =>
          defn.category === PropertyCategory.RUNTIME_OPTIONS_ENGINE
      )
    );
    const heartbeatPropertyDefinitions = formatPropertyDefinitions(
      props.propertyDefinitions.filter(
        (defn: ConnectorProperty) =>
          defn.category === PropertyCategory.RUNTIME_OPTIONS_HEARTBEAT
      )
    );

    // Just added String and Password type
    enginePropertyDefinitions.map((key: any) => {
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
      }
      if (key.isMandatory) {
        basicValidationSchema[key.name] = basicValidationSchema[
          key.name
        ].required(`${key.name} ${props.i18nIsRequiredText}`);
      }
    });

    const validationSchema = Yup.object().shape({ ...basicValidationSchema });

    const getInitialValues = (combined: any) => {
      const combinedValue: any = {};
      const userValues: Map<string, string> = new Map([
        ...props.propertyValues,
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
      _.union(enginePropertyDefinitions, heartbeatPropertyDefinitions)
    );

    const handleSubmit = (valueMap: Map<string, string>) => {
      const runtimeValueMap: Map<string, string> = new Map();
      for (const runtimeValue of props.propertyDefinitions) {
        runtimeValueMap.set(
          runtimeValue.name.replace(/&/g, '.'),
          valueMap[runtimeValue.name]
        );
      }
      props.onValidateProperties(
        runtimeValueMap,
        PropertyCategory.RUNTIME_OPTIONS_ENGINE
      );
    };

    return (
      <div className={'runtime-options-component-page'}>
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
              <RuntimeOptionsComponent
                propertyDefinitions={props.propertyDefinitions}
                propertyValues={props.propertyValues}
                i18nEngineProperties={props.i18nEngineProperties}
                i18nHeartbeatProperties={props.i18nHeartbeatProperties}
                i18nIsRequiredText={props.i18nIsRequiredText}
                invalidMsg={props.invalidMsg}
                setFieldValue={setFieldValue}
                errors={errors}
                touched={touched}
                clearValidationError={props.clearValidationError}
              />
              <FormSubmit
                ref={ref}
                setRuntimeOptionsValid={props.setRuntimeOptionsValid}
                setRuntimeStepsValid={props.setRuntimeStepsValid}
              />
            </Form>
          )}
        </Formik>
      </div>
    );
  }
);
