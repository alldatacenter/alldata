import { Form, Grid, GridItem } from '@patternfly/react-core';
import { FormComponent } from 'components';
import { Formik, useFormikContext } from 'formik';
import _ from 'lodash';
import React from 'react';
import { formatPropertyDefinitions } from 'shared';
import * as Yup from 'yup';

export interface ITopicDefaultsProps {
  topicDefaultProperties: any[];
  topicDefaultValues?: any;
  updateTopicDefaults: (value: any) => void;
  setIsTopicCreationDirty: (data: boolean) => void;
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
        props.setIsTopicCreationDirty(true);
      }
    }, [dirty]);
    return null;
  }
);

export const TopicDefaults: React.FunctionComponent<any> = React.forwardRef(
  (props, ref) => {
    const getInitialValues = (properties: any) => {
      const combinedValues: any = {};

      for (const prop of properties) {
        if (!combinedValues[prop.name]) {
          if (
            _.isEmpty(props.topicDefaultValues) ||
            props.topicDefaultValues[prop.name] === undefined
          ) {
            prop.defaultValue === undefined
              ? (combinedValues[prop.name] =
                  prop.type === 'INT' || prop.type === 'LONG' ? 0 : '')
              : (combinedValues[prop.name] = prop.defaultValue);
          } else {
            combinedValues[prop.name] = props.topicDefaultValues[prop.name];
          }
        }
      }
      return combinedValues;
    };

    const propList = formatPropertyDefinitions(props.topicDefaultProperties);

    const initialValues = getInitialValues(propList);

    const basicValidationSchema = {};

    const topicDefaultList = [...propList];

    topicDefaultList.map((key: any) => {
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
        ].required(`${key.name} required`);
      }
    });
    const validationSchema = Yup.object().shape({ ...basicValidationSchema });

    const handleSubmit = (value: any) => {
      const newValues = {};
      for (const prop of props.topicDefaultProperties) {
        const newValue =
          prop.type === 'INT' ||
          prop.type === 'LONG' ||
          prop.type === 'NON-NEG-INT' ||
          prop.type === 'NON-NEG-LONG' ||
          prop.type === 'POS-INT'
            ? Number(value[prop.name])
            : value[prop.name];
        if (!prop.defaultValue || newValue !== prop.defaultValue) {
          newValues[prop.name] = newValue;
        }
      }
      props.updateTopicDefaults(newValues);
    };
    return (
      <>
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
              <Grid hasGutter={true}>
                {propList.map((prop, index) => {
                  return (
                    <GridItem
                      key={index}
                      lg={prop.gridWidthLg}
                      sm={prop.gridWidthSm}
                    >
                      <FormComponent
                        propertyDefinition={prop}
                        // tslint:disable-next-line: no-empty
                        propertyChange={() => {}}
                        setFieldValue={setFieldValue}
                        helperTextInvalid={errors[prop.name]}
                        invalidMsg={[]}
                        validated={
                          errors[prop.name] && touched[prop.name]
                            ? 'error'
                            : 'default'
                        }
                        // tslint:disable-next-line: no-empty
                        clearValidationError={() => {}}
                      />
                    </GridItem>
                  );
                })}
              </Grid>
              <FormSubmit
                ref={ref}
                setIsTopicCreationDirty={props.setIsTopicCreationDirty}
              />
            </Form>
          )}
        </Formik>
      </>
    );
  }
);
