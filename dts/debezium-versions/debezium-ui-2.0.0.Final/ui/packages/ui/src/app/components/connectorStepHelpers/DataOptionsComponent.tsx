import {
  ConnectorProperty,
  PropertyValidationResult,
} from '@debezium/ui-models';
import {
  ExpandableSection,
  Grid,
  GridItem,
  Title,
} from '@patternfly/react-core';
import { FormComponent } from 'components';
import { FormikErrors, FormikTouched } from 'formik';
import _ from 'lodash';
import * as React from 'react';
import { formatPropertyDefinitions, PropertyCategory } from 'shared';

export interface IDataOptionsComponentProps {
  propertyDefinitions: ConnectorProperty[];
  propertyValues: Map<string, string>;
  invalidMsg: PropertyValidationResult[];
  i18nAdvancedMappingPropertiesText: string;
  i18nMappingPropertiesText: string;
  i18nSnapshotPropertiesText: string;
  setFieldValue: (
    field: string,
    value: any,
    shouldValidate?: boolean | undefined
  ) => void;
  errors: FormikErrors<any>;
  touched: FormikTouched<any>;
  clearValidationError: () => void;
}

export const DataOptionsComponent: React.FC<IDataOptionsComponentProps> =
  React.forwardRef((props, ref) => {
    const [snapshotExpanded, setSnapshotExpanded] =
      React.useState<boolean>(true);
    const [mappingExpanded, setMappingExpanded] =
      React.useState<boolean>(false);

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

    const onToggleSnapshot = (isExpanded: boolean) => {
      setSnapshotExpanded(isExpanded);
    };

    const onToggleMapping = (isExpanded: boolean) => {
      setMappingExpanded(isExpanded);
    };

    const handlePropertyChange = (propName: string, propValue: any) => {
      // TODO: handling for property change if needed.
    };

    return (
      <>
        <Grid>
          <GridItem lg={9} sm={12}>
            <ExpandableSection
              toggleText={
                snapshotExpanded
                  ? props.i18nSnapshotPropertiesText
                  : props.i18nSnapshotPropertiesText
              }
              onToggle={onToggleSnapshot}
              isExpanded={snapshotExpanded}
            >
              <Grid
                hasGutter={true}
                className={'data-options-component-expansion-content'}
              >
                {snapshotPropertyDefinitions.map(
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
            <ExpandableSection
              toggleText={
                mappingExpanded
                  ? props.i18nMappingPropertiesText
                  : props.i18nMappingPropertiesText
              }
              onToggle={onToggleMapping}
              isExpanded={mappingExpanded}
            >
              <Grid
                hasGutter={true}
                className={'data-options-component-expansion-content'}
              >
                {mappingGeneralPropertyDefinitions.map(
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
              <Title
                headingLevel="h3"
                className={'data-options-component-grouping'}
              >
                {props.i18nAdvancedMappingPropertiesText}
              </Title>
              <Grid
                hasGutter={true}
                className={'data-options-component-expansion-content'}
              >
                {mappingAdvancedPropertyDefinitions.map(
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
          </GridItem>
        </Grid>
      </>
    );
  });
