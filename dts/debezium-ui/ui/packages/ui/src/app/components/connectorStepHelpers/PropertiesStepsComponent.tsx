import {
  ConnectorProperty,
  PropertyValidationResult,
} from '@debezium/ui-models';
import {
  ExpandableSection,
  Grid,
  GridItem,
  Split,
  SplitItem,
  Text,
  TextContent,
  Title,
} from '@patternfly/react-core';
import { FormComponent, ConnectorTypeComponent } from 'components';
import { FormikErrors, FormikTouched } from 'formik';
import _ from 'lodash';
import * as React from 'react';
import {
  formatPropertyDefinitions,
  PropertyCategory,
  PropertyName,
} from 'shared';

export interface IPropertiesStepProps {
  connectorType: string | undefined;
  basicPropertyDefinitions: ConnectorProperty[];
  basicPropertyValues: Map<string, string>;
  advancedPropertyDefinitions: ConnectorProperty[];
  advancedPropertyValues: Map<string, string>;
  invalidMsg: PropertyValidationResult[];
  i18nAdvancedPropertiesText: string;
  i18nAdvancedPublicationPropertiesText: string;
  i18nAdvancedReplicationPropertiesText: string;
  i18nBasicPropertiesText: string;
  clearValidationError: () => void;
  setFieldValue: (
    field: string,
    value: any,
    shouldValidate?: boolean | undefined
  ) => void;
  errors: FormikErrors<any>;
  touched: FormikTouched<any>;
}

export const PropertiesStepsComponent: React.FC<IPropertiesStepProps> =
  React.forwardRef((props, ref) => {
    const [basicExpanded, setBasicExpanded] = React.useState<boolean>(true);
    const [advancedExpanded, setAdvancedExpanded] =
      React.useState<boolean>(false);
    const [showPublication, setShowPublication] = React.useState(true);

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

    const onToggleBasic = (isExpanded: boolean) => {
      setBasicExpanded(isExpanded);
    };

    const onToggleAdvanced = (isExpanded: boolean) => {
      setAdvancedExpanded(isExpanded);
    };

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

    const handlePropertyChange = (propName: string, propValue: any) => {
      Object.entries(initialValues).forEach(([key]) => {
        if (key === propName) {
          initialValues[key] = propValue;
        }
      });

      propName = propName.replace(/\_/g, '.');
      if (propName === PropertyName.PLUGIN_NAME) {
        setShowPublication(propValue === 'Pgoutput');
      }
      return initialValues;
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

    return (
      <>
        <Grid hasGutter={true} className="connector-name-form">
          {namePropertyDefinitions.map(
            (propertyDefinition: ConnectorProperty, index: any) => {
              return (
                <GridItem key={index} lg={4} sm={12}>
                  <FormComponent
                    propertyDefinition={propertyDefinition}
                    propertyChange={handlePropertyChange}
                    setFieldValue={props.setFieldValue}
                    helperTextInvalid={props.errors[propertyDefinition.name]}
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
          <GridItem key={'connType'} lg={12} sm={12}>
            <Split>
              <SplitItem>
                <TextContent>
                  <Text className={'connector-type-label'}>
                    Connector type:
                  </Text>
                </TextContent>
              </SplitItem>
              <SplitItem>
                <ConnectorTypeComponent
                  connectorType={props.connectorType}
                  showIcon={false}
                />
              </SplitItem>
            </Split>
          </GridItem>
        </Grid>
        <Grid>
          <GridItem lg={9} sm={12}>
            <ExpandableSection
              toggleText={
                basicExpanded
                  ? props.i18nBasicPropertiesText
                  : props.i18nBasicPropertiesText
              }
              onToggle={onToggleBasic}
              isExpanded={basicExpanded}
            >
              <Grid
                hasGutter={true}
                className={'properties-step-expansion-content'}
              >
                {basicPropertyDefinitions.map(
                  (propertyDefinition: ConnectorProperty, index: any) => {
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
            {props.advancedPropertyDefinitions &&
              props.advancedPropertyDefinitions.length > 0 && (
                <ExpandableSection
                  toggleText={
                    advancedExpanded
                      ? props.i18nAdvancedPropertiesText
                      : props.i18nAdvancedPropertiesText
                  }
                  onToggle={onToggleAdvanced}
                  isExpanded={advancedExpanded}
                >
                  <GridItem span={9}>
                    <Grid
                      hasGutter={true}
                      className={'properties-step-expansion-content'}
                    >
                      {advancedGeneralPropertyDefinitions.map(
                        (propertyDefinition: ConnectorProperty, index: any) => {
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
                                clearValidationError={
                                  props.clearValidationError
                                }
                              />
                            </GridItem>
                          );
                        }
                      )}
                    </Grid>
                  </GridItem>
                  {advancedReplicationPropertyDefinitions.length > 0 ? (
                    <Title
                      headingLevel="h2"
                      className="properties-step-grouping"
                    >
                      {props.i18nAdvancedReplicationPropertiesText}
                    </Title>
                  ) : null}
                  <GridItem span={9}>
                    <Grid
                      hasGutter={true}
                      className={'properties-step-expansion-content'}
                    >
                      {advancedReplicationPropertyDefinitions.map(
                        (propertyDefinition: ConnectorProperty, index: any) => {
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
                                clearValidationError={
                                  props.clearValidationError
                                }
                              />
                            </GridItem>
                          );
                        }
                      )}
                    </Grid>
                  </GridItem>
                  {showPublication && (
                    <>
                      {advancedPublicationPropertyDefinitions.length > 0 ? (
                        <Title
                          headingLevel="h2"
                          className="properties-step-grouping"
                        >
                          {props.i18nAdvancedPublicationPropertiesText}
                        </Title>
                      ) : null}
                      <GridItem span={9}>
                        <Grid
                          hasGutter={true}
                          className={'properties-step-expansion-content'}
                        >
                          {advancedPublicationPropertyDefinitions.map(
                            (
                              propertyDefinition: ConnectorProperty,
                              index: any
                            ) => {
                              return (
                                <GridItem
                                  key={index}
                                  lg={propertyDefinition.gridWidthLg}
                                  sm={propertyDefinition.gridWidthSm}
                                >
                                  <FormComponent
                                    propertyDefinition={propertyDefinition}
                                    setFieldValue={props.setFieldValue}
                                    propertyChange={handlePropertyChange}
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
                                    clearValidationError={
                                      props.clearValidationError
                                    }
                                  />
                                </GridItem>
                              );
                            }
                          )}
                        </Grid>
                      </GridItem>
                    </>
                  )}
                </ExpandableSection>
              )}
          </GridItem>
        </Grid>
      </>
    );
  });
