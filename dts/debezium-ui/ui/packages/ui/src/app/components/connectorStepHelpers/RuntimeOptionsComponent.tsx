import {
  ConnectorProperty,
  PropertyValidationResult,
} from '@debezium/ui-models';
import { ExpandableSection, Grid, GridItem } from '@patternfly/react-core';
import { FormComponent } from 'components';
import { FormikErrors, FormikTouched } from 'formik';
import _ from 'lodash';
import * as React from 'react';
import { formatPropertyDefinitions, PropertyCategory } from 'shared';
import * as Yup from 'yup';

export interface IRuntimeOptionsComponentProps {
  propertyDefinitions: ConnectorProperty[];
  propertyValues: Map<string, string>;
  invalidMsg: PropertyValidationResult[];
  i18nIsRequiredText: string;
  i18nEngineProperties: string;
  i18nHeartbeatProperties: string;
  setFieldValue: (
    field: string,
    value: any,
    shouldValidate?: boolean | undefined
  ) => void;
  errors: FormikErrors<any>;
  touched: FormikTouched<any>;
  clearValidationError: () => void;
}

export const RuntimeOptionsComponent: React.FC<IRuntimeOptionsComponentProps> =
  React.forwardRef((props, ref) => {
    const [engineExpanded, setEngineExpanded] = React.useState<boolean>(true);
    const [heartbeatExpanded, setHeartbeatExpanded] =
      React.useState<boolean>(false);

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

    const handlePropertyChange = (propName: string, propValue: any) => {
      // TODO: handling for property change if needed.
    };

    const onToggleEngine = (isExpanded: boolean) => {
      setEngineExpanded(isExpanded);
    };

    const onToggleHeartbeat = (isExpanded: boolean) => {
      setHeartbeatExpanded(isExpanded);
    };

    return (
      <>
        <Grid>
          <GridItem lg={9} sm={12}>
            <ExpandableSection
              toggleText={
                engineExpanded
                  ? props.i18nEngineProperties
                  : props.i18nEngineProperties
              }
              onToggle={onToggleEngine}
              isExpanded={engineExpanded}
            >
              <Grid
                hasGutter={true}
                className={'runtime-options-component-expansion-content'}
              >
                {enginePropertyDefinitions.map(
                  (propertyDefinition: ConnectorProperty, index) => {
                    return (
                      <GridItem
                        key={index}
                        lg={propertyDefinition.gridWidthLg}
                        sm={propertyDefinition.gridWidthSm}
                      >
                        <FormComponent
                          propertyDefinition={propertyDefinition}
                          setFieldValue={props.setFieldValue}
                          helperTextInvalid={
                            props.errors[propertyDefinition.name]
                          }
                          invalidMsg={props.invalidMsg}
                          validated={
                            props.errors[propertyDefinition.name] &&
                            props.touched[propertyDefinition.name]
                              ? 'error'
                              : 'default'
                          }
                          propertyChange={handlePropertyChange}
                          clearValidationError={props.clearValidationError}
                        />
                      </GridItem>
                    );
                  }
                )}
              </Grid>
            </ExpandableSection>
            <ExpandableSection
              toggleText={
                heartbeatExpanded
                  ? props.i18nHeartbeatProperties
                  : props.i18nHeartbeatProperties
              }
              onToggle={onToggleHeartbeat}
              isExpanded={heartbeatExpanded}
            >
              <Grid
                hasGutter={true}
                className={'runtime-options-component-expansion-content'}
              >
                {heartbeatPropertyDefinitions.map(
                  (propertyDefinition: ConnectorProperty, index) => {
                    return (
                      <GridItem
                        key={index}
                        lg={propertyDefinition.gridWidthLg}
                        sm={propertyDefinition.gridWidthSm}
                      >
                        <FormComponent
                          propertyDefinition={propertyDefinition}
                          propertyChange={handlePropertyChange}
                          setFieldValue={props.setFieldValue}
                          helperTextInvalid={
                            props.errors[propertyDefinition.name]
                          }
                          invalidMsg={props.invalidMsg}
                          validated={
                            props.errors[propertyDefinition.name] &&
                            props.touched[propertyDefinition.name]
                              ? 'error'
                              : 'default'
                          }
                          clearValidationError={props.clearValidationError}
                        />
                      </GridItem>
                    );
                  }
                )}
              </Grid>
            </ExpandableSection>
          </GridItem>
        </Grid>
      </>
    );
  });
