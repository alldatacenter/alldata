import { Form, Grid, GridItem } from '@patternfly/react-core';
import { FormComponent } from 'components';
import { Formik, useFormikContext } from 'formik';
import _ from 'lodash';
import React from 'react';
import * as Yup from 'yup';

export interface ITransformConfigProps {
  transformConfigOptions: any[];
  transformConfigValues?: any;
  updateTransform: (key: number, field: string, value: any) => void;
  setIsTransformDirty: (data: boolean) => void;
  transformType: string;
  transformNo: number;
  setConfigComplete: (data: boolean) => void;
}

const FormSubmit: React.FunctionComponent<any> = React.forwardRef(
  (props, ref) => {
    const { dirty, submitForm, validateForm } = useFormikContext();

    React.useImperativeHandle(ref, () => ({
      async validate() {
        const valid = await validateForm();
        const validPromise = new Promise((resolve, reject) => {
          if (_.isEmpty(valid)) {
            props.setConfigComplete(true);
            resolve('done');
          } else {
            props.setConfigComplete(false);
            reject('fail');
          }
        });
        submitForm();
        return validPromise;
      },
    }));
    React.useEffect(() => {
      if (dirty) {
        props.setIsTransformDirty(true);
      }
    }, [dirty]);
    return null;
  }
);

export const TransformConfig = React.forwardRef<any, ITransformConfigProps>(
  (props, ref) => {
    const getInitialValues = (configurations: any) => {
      const combinedValue: any = {};
      const userValues = { ...props.transformConfigValues };

      for (const config of configurations) {
        if (!combinedValue[config.name]) {
          if (_.isEmpty(userValues)) {
            config.defaultValue === undefined
              ? (combinedValue[config.name] =
                  config.type === 'INT' || config.type === 'LONG' ? 0 : '')
              : (combinedValue[config.name] = config.defaultValue);
          } else {
            combinedValue[config.name] =
              userValues[config.name.replace(/&/g, '.')];
          }
        }
      }
      return combinedValue;
    };

    const basicValidationSchema = {};

    const transformConfigurationList = [...props.transformConfigOptions];

    transformConfigurationList.map((key: any) => {
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
      // TODO: isMandatory is missing in backend response
      // if (key.isMandatory) {
      //   basicValidationSchema[key.name] = basicValidationSchema[key.name].required(`${key.title} required`);
      // }
    });
    const validationSchema = Yup.object().shape({ ...basicValidationSchema });

    const handleSubmit = (value: any) => {
      const basicValue = {};
      for (const basicVal of props.transformConfigOptions) {
        basicValue[basicVal.name.replace(/&/g, '.')] = value[basicVal.name];
      }
      props.updateTransform(props.transformNo, 'config', basicValue);
    };

    return (
      <>
        <Formik
          initialValues={getInitialValues(props.transformConfigOptions)}
          validationSchema={validationSchema}
          onSubmit={(values) => {
            handleSubmit(values);
          }}
          enableReinitialize={true}
        >
          {({ errors, touched, setFieldValue }) => (
            <Form className="pf-c-form">
              <Grid hasGutter={true}>
                {props.transformConfigOptions.map((configuration, index) => {
                  return (
                    <GridItem
                      key={props.transformType + configuration.name}
                      lg={configuration.gridWidthLg}
                      sm={configuration.gridWidthSm}
                    >
                      <FormComponent
                        propertyDefinition={configuration}
                        // tslint:disable-next-line: no-empty
                        propertyChange={() => {}}
                        setFieldValue={setFieldValue}
                        helperTextInvalid={errors[configuration.name]}
                        invalidMsg={[]}
                        validated={
                          errors[configuration.name] &&
                          touched[configuration.name]
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
                setIsTransformDirty={props.setIsTransformDirty}
                setConfigComplete={props.setConfigComplete}
              />
            </Form>
          )}
        </Formik>
      </>
    );
  }
);
