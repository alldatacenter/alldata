import './Properties.css';
import { ConnectorProperty } from '@debezium/ui-models';
import {
  ExpandableSection,
  Grid,
  GridItem,
  Title,
} from '@patternfly/react-core';
import { ConfigurationMode, FormComponent } from 'components';
import { Form, Formik } from 'formik';
import _ from 'lodash';
import React from 'react';
import { useTranslation } from 'react-i18next';
import { PropertyCategory } from 'shared';
import { getObject } from 'src/app/utils/ResolveSchemaRef';

export interface IPropertiesProps {
  connectorType: string;
  uiPath: ConfigurationMode;
  configuration: Map<string, unknown>;
  propertyDefinitions: ConnectorProperty[];
  onChange: (configuration: Map<string, unknown>, isValid: boolean) => void;
}

const getInitialObject = (propertyList: ConnectorProperty[]) => {
  const returnObj = {};
  propertyList.forEach((property) => {
    if (!property.name.includes('.')) {
      returnObj[property.name] = property.defaultValue || '';
    } else {
      const schema = '/' + property.name.replace('.', '/');
      getObject(returnObj, schema, property.defaultValue || '');
    }
  });
  return returnObj;
};

const checkIfRequired = (
  propertyList: ConnectorProperty[],
  property: string
): boolean => {
  const matchProp = _.find(propertyList, (obj) => obj.name === property);
  return matchProp ? matchProp.isMandatory : false;
};

const setValidation = (
  values: any,
  propertyList: ConnectorProperty[],
  requiredTest: string
) => {
  const errors = {};

  propertyList.forEach((property) => {
    if (property.isMandatory && !values[property.name]) {
      errors[property.name] = `${property.displayName} ${requiredTest}`;
    }
  });
  return errors;
};

const getBasicProperty = (
  propertyList: ConnectorProperty[]
): ConnectorProperty[] => {
  const propertyDefinitionsCopy = _.cloneDeep(propertyList);
  return propertyDefinitionsCopy.filter(
    (defn: any) => defn.category === PropertyCategory.BASIC
  );
};

const getAdvanceGeneralProperty = (
  propertyList: ConnectorProperty[]
): ConnectorProperty[] => {
  const propertyDefinitionsCopy = _.cloneDeep(propertyList);
  return propertyDefinitionsCopy.filter(
    (defn: any) =>
      defn.category === PropertyCategory.ADVANCED_GENERAL ||
      defn.category === PropertyCategory.ADVANCED_SSL
  );
};
const getAdvanceReplicationProperty = (
  propertyList: ConnectorProperty[]
): ConnectorProperty[] => {
  const propertyDefinitionsCopy = _.cloneDeep(propertyList);
  return propertyDefinitionsCopy.filter(
    (defn: any) => defn.category === PropertyCategory.ADVANCED_REPLICATION
  );
};
const getAdvancePublicationProperty = (
  propertyList: ConnectorProperty[]
): ConnectorProperty[] => {
  const propertyDefinitionsCopy = _.cloneDeep(propertyList);
  return propertyDefinitionsCopy.filter(
    (defn: any) => defn.category === PropertyCategory.ADVANCED_PUBLICATION
  );
};

export const Properties: React.FC<IPropertiesProps> = (props) => {
  const { t } = useTranslation();

  const [initialValues, setInitialValues] = React.useState(
    getInitialObject(props.propertyDefinitions)
  );
  const [basicExpanded, setBasicExpanded] = React.useState<boolean>(true);
  const [advancedExpanded, setAdvancedExpanded] = React.useState<boolean>(true);

  const [basicPropertyDefinitions] = React.useState<ConnectorProperty[]>(
    getBasicProperty(props.propertyDefinitions)
  );
  const [advancedGeneralPropertyDefinitions] = React.useState<
    ConnectorProperty[]
  >(getAdvanceGeneralProperty(props.propertyDefinitions));
  const [advancedReplicationPropertyDefinitions] = React.useState<
    ConnectorProperty[]
  >(getAdvanceReplicationProperty(props.propertyDefinitions));
  const [advancedPublicationPropertyDefinitions] = React.useState<
    ConnectorProperty[]
  >(getAdvancePublicationProperty(props.propertyDefinitions));

  const validateForm = (values: any) => {
    const formEntries = Object.entries(values).reduce(
      (a, [k, v]) => (initialValues[k] === v || (a[k] = v), a),
      {}
    );
    const formValues = new Map(Object.entries(formEntries));

    const configCopy = props.configuration
      ? new Map<string, unknown>(props.configuration)
      : new Map<string, unknown>();

    const updatedConfiguration = new Map([
      ...Array.from(configCopy.entries()),
      ...Array.from(formValues.entries()),
    ]);
    const finalConfiguration = new Map();
    updatedConfiguration.forEach((value: any, key: any) => {
      finalConfiguration.set(key.replace(/&/g, '.'), value);
    });
    props.onChange(
      finalConfiguration,
      isFormValid(new Map(Object.entries(values)))
    );
    return setValidation(values, props.propertyDefinitions, t('isRequired'));
  };

  const isFormValid = (formData: Map<string, unknown>): boolean => {
    let isValid = true;
    if (formData && formData.size !== 0) {
      formData.forEach((value: unknown, key: string) => {
        if (
          !value &&
          initialValues.hasOwnProperty(key) &&
          checkIfRequired(props.propertyDefinitions, key)
        ) {
          isValid = false;
        }
      });
    }
    return isValid;
  };

  const onToggleBasic = (isExpanded: boolean) => {
    setBasicExpanded(isExpanded);
  };

  const onToggleAdvanced = (isExpanded: boolean) => {
    setAdvancedExpanded(isExpanded);
  };

  const handlePropertyChange = (propName: string, propValue: any) => {
    // handling for property change if needed.
  };

  React.useEffect(() => {
    const initialValuesCopy = JSON.parse(JSON.stringify(initialValues));

    let isValid = true;
    const updatedConfiguration = new Map();
    if (props.configuration && props.configuration.size !== 0) {
      props.configuration.forEach((value: any, key: any) => {
        updatedConfiguration.set(key, value);
      });
    }
    Object.keys(initialValues).forEach((key: string) => {
      if (updatedConfiguration.get(key.replace(/[&]/g, '.'))) {
        initialValuesCopy[key] = updatedConfiguration.get(
          key.replace(/[&]/g, '.')
        );
      } else if (checkIfRequired(props.propertyDefinitions, key)) {
        initialValues[key]
          ? updatedConfiguration.set(
              key.replace(/[&]/g, '.'),
              initialValues[key]
            )
          : (isValid = false);
      }
    });
    setInitialValues(initialValuesCopy);
    props.onChange(updatedConfiguration, isValid);
  }, []);

  return (
    <div className={'properties-step-page'}>
      <Formik
        validateOnChange={true}
        enableReinitialize={true}
        initialValues={initialValues}
        validate={validateForm}
        onSubmit={() => {
          //
        }}
      >
        {({ errors, touched, setFieldValue }) => (
          <Form className="pf-c-form">
            <>
              <Grid>
                <GridItem lg={9} sm={12}>
                  <ExpandableSection
                    toggleText={t('basicPropertiesText')}
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
                                uiPath={props.uiPath}
                                initialValues={initialValues}
                                propertyDefinition={propertyDefinition}
                                propertyChange={handlePropertyChange}
                                setFieldValue={setFieldValue}
                                helperTextInvalid={
                                  errors[propertyDefinition.name]
                                }
                                invalidMsg={[]}
                                validated={
                                  errors[propertyDefinition.name] &&
                                  touched[propertyDefinition.name] &&
                                  errors[propertyDefinition.name]
                                    ? 'error'
                                    : 'default'
                                }
                              />
                            </GridItem>
                          );
                        }
                      )}
                    </Grid>
                  </ExpandableSection>
                  {(advancedGeneralPropertyDefinitions.length > 0 ||
                    advancedReplicationPropertyDefinitions.length > 0 ||
                    advancedPublicationPropertyDefinitions.length > 0) && (
                    <ExpandableSection
                      toggleText={t('advancedPropertiesText')}
                      onToggle={onToggleAdvanced}
                      isExpanded={advancedExpanded}
                    >
                      <GridItem span={9}>
                        <Grid
                          hasGutter={true}
                          className={'properties-step-expansion-content'}
                        >
                          {advancedGeneralPropertyDefinitions.map(
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
                                    uiPath={props.uiPath}
                                    initialValues={initialValues}
                                    propertyDefinition={propertyDefinition}
                                    propertyChange={handlePropertyChange}
                                    setFieldValue={setFieldValue}
                                    helperTextInvalid={
                                      errors[propertyDefinition.name]
                                    }
                                    invalidMsg={[]}
                                    validated={
                                      errors[propertyDefinition.name] &&
                                      touched[propertyDefinition.name] &&
                                      errors[propertyDefinition.name]
                                        ? 'error'
                                        : 'default'
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
                          {t('advancedReplicationPropertiesText')}
                        </Title>
                      ) : null}
                      <GridItem span={9}>
                        <Grid
                          hasGutter={true}
                          className={'properties-step-expansion-content'}
                        >
                          {advancedReplicationPropertyDefinitions.map(
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
                                    uiPath={props.uiPath}
                                    initialValues={initialValues}
                                    propertyDefinition={propertyDefinition}
                                    propertyChange={handlePropertyChange}
                                    setFieldValue={setFieldValue}
                                    helperTextInvalid={
                                      errors[propertyDefinition.name]
                                    }
                                    invalidMsg={[]}
                                    validated={
                                      errors[propertyDefinition.name] &&
                                      touched[propertyDefinition.name] &&
                                      errors[propertyDefinition.name]
                                        ? 'error'
                                        : 'default'
                                    }
                                  />
                                </GridItem>
                              );
                            }
                          )}
                        </Grid>
                      </GridItem>
                      {/* TODO: handle correctly*/}
                      {/* {showPublication && ( */}
                      {true && (
                        <>
                          {advancedPublicationPropertyDefinitions.length > 0 ? (
                            <Title
                              headingLevel="h2"
                              className="properties-step-grouping"
                            >
                              {t('advancedPublicationPropertiesText')}
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
                                        uiPath={props.uiPath}
                                        initialValues={initialValues}
                                        propertyDefinition={propertyDefinition}
                                        propertyChange={handlePropertyChange}
                                        setFieldValue={setFieldValue}
                                        helperTextInvalid={
                                          errors[propertyDefinition.name]
                                        }
                                        invalidMsg={[]}
                                        validated={
                                          errors[propertyDefinition.name] &&
                                          touched[propertyDefinition.name] &&
                                          errors[propertyDefinition.name]
                                            ? 'error'
                                            : 'default'
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
          </Form>
        )}
      </Formik>
    </div>
  );
};
