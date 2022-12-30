import { DataOptions } from './DataOptions';
import { FilterConfig } from './FilterConfig';
import { Properties } from './Properties';
// import { RuntimeOptions } from './RuntimeOptions';
import { ConnectorProperty } from '@debezium/ui-models';
import i18n from 'i18n';
import * as React from 'react';
import { I18nextProvider } from 'react-i18next';
import { BrowserRouter } from 'react-router-dom';
import {
  getAdvancedPropertyDefinitions,
  getBasicPropertyDefinitions,
  getDataOptionsPropertyDefinitions,
  getRuntimeOptionsPropertyDefinitions,
  getFilterConfigurationPageContent,
} from 'shared';
import { getPropertiesData } from 'src/app/utils/FormatCosProperties';
import { ConfigurationMode } from 'components';

/**
 * Represents a connector type supported by the API
 * @export
 * @interface IConnectorType
 */
export interface IConnectorType {
  /**
   *
   * @type {string}
   * @memberof IConnectorType
   */
  id?: string;
  /**
   *
   * @type {string}
   * @memberof IConnectorType
   */
  kind?: string;
  /**
   *
   * @type {string}
   * @memberof IConnectorType
   */
  href?: string;
  /**
   * Name of the connector type.
   * @type {string}
   * @memberof IConnectorType
   */
  name: string;
  /**
   * Version of the connector type.
   * @type {string}
   * @memberof IConnectorType
   */
  version: string;
  /**
   * A description of the connector.
   * @type {string}
   * @memberof IConnectorType
   */
  description?: string;
  /**
   * A json schema that can be used to validate a connectors connector_spec field.
   * @type {object}
   * @memberof IConnectorType
   */
  schema?: object;
}

export interface IDebeziumConfiguratorProps {
  activeStep: number;
  connector: IConnectorType;
  uiPath: ConfigurationMode;
  configuration: Map<string, unknown>;
  onChange: (configuration: Map<string, unknown>, isValid: boolean) => void;
}

/**
 * Get the filter properties passed via connector prop
 * @param connectorData
 * @param selectedConnector
 */
const getFilterInitialValues = (
  connectorData: Map<string, unknown>,
  selectedConnector: string
): Map<string, string> => {
  const configCopy = connectorData
    ? new Map<string, unknown>(connectorData)
    : new Map<string, unknown>();
  const returnVal = new Map<string, string>();
  if (configCopy && configCopy.size !== 0) {
    const filterConfigurationPageContentObj: any =
      getFilterConfigurationPageContent(selectedConnector || '');
    filterConfigurationPageContentObj.fieldArray.forEach((fieldObj: any) => {
      configCopy.get(`${fieldObj.field}.include.list`) &&
        returnVal.set(
          `${fieldObj.field}.include.list`,
          configCopy.get(`${fieldObj.field}.include.list`) as string
        );
      configCopy.get(`${fieldObj.field}.exclude.list`) &&
        returnVal.set(
          `${fieldObj.field}.exclude.list`,
          configCopy.get(`${fieldObj.field}.exclude.list`) as string
        );
    });
  }
  return returnVal;
};

export const DebeziumConfigurator: React.FC<IDebeziumConfiguratorProps> = (
  props
) => {
  const PROPERTIES_STEP_ID = 0;
  const FILTER_CONFIGURATION_STEP_ID = 1;
  const DATA_OPTIONS_STEP_ID = 2;
  // const RUNTIME_OPTIONS_STEP_ID = 3;

  const [connectorProperties] = React.useState<ConnectorProperty[]>(
    getPropertiesData(props.connector)
  );

  const [filterValues, setFilterValues] = React.useState<Map<string, string>>(
    getFilterInitialValues(props.configuration, props.connector.name.toLowerCase())
  );
  const [isValidFilter, setIsValidFilter] = React.useState<boolean>(true);

  const clearFilterFields = (
    configObj: Map<string, unknown>
  ): Map<string, unknown> => {
    const filterConfigurationPageContentObj: any =
      getFilterConfigurationPageContent(props.connector.name.toLowerCase());
    filterConfigurationPageContentObj.fieldArray.forEach((fieldObj: any) => {
      configObj.delete(`${fieldObj.field}.include.list`) ||
        configObj.delete(`${fieldObj.field}.exclude.list`);
    });
    return configObj;
  };

  // Update the filter values
  const handleFilterUpdate = (filterValue: Map<string, string>) => {
    const filterVal = new Map(filterValue);
    setFilterValues(filterVal);
    const configCopy = props.configuration
      ? new Map<string, unknown>(props.configuration)
      : new Map<string, unknown>();
    const configVal = clearFilterFields(configCopy);
    const updatedConfiguration = new Map([
      ...Array.from(configVal.entries()),
      ...Array.from(filterVal.entries()),
    ]);
    props.onChange(updatedConfiguration, true);
  };

  // Enable the filter step next button initially
  React.useEffect(() => {
    props.activeStep === 1 &&
      props.onChange(props.configuration, isValidFilter);
  }, [isValidFilter, props.activeStep]);

  function chooseStep(stepId: number) {
    switch (stepId) {
      case PROPERTIES_STEP_ID:
        return (
          <Properties
            connectorType={
              props.connector?.schema
                ? props.connector?.schema['x-connector-id']
                : ''
            }
            uiPath={props.uiPath}
            configuration={props.configuration}
            onChange={(conf: Map<string, unknown>, status: boolean) =>
              props.onChange(conf, status)
            }
            propertyDefinitions={[
              ...getBasicPropertyDefinitions(connectorProperties, true),
              ...getAdvancedPropertyDefinitions(connectorProperties),
            ]}
          />
        );
      case FILTER_CONFIGURATION_STEP_ID:
        return (
          <FilterConfig
            uiPath={props?.uiPath}
            filterValues={filterValues}
            updateFilterValues={handleFilterUpdate}
            connectorType={
              props.connector?.schema
                ? props.connector?.schema['x-connector-id']
                : ''
            }
            setIsValidFilter={setIsValidFilter}
          />
        );
      case DATA_OPTIONS_STEP_ID:
        return (
          <DataOptions
            uiPath={props?.uiPath}
            configuration={props.configuration}
            onChange={(conf: Map<string, unknown>, status: boolean) =>
              props.onChange(conf, status)
            }
            propertyDefinitions={getDataOptionsPropertyDefinitions(
              connectorProperties
            )}
            runtimePropertyDefinitions={getRuntimeOptionsPropertyDefinitions(
              connectorProperties
            )}
          />
        );
      // case RUNTIME_OPTIONS_STEP_ID:
      //   return (
      //     <RuntimeOptions
      //       configuration={props.configuration}
      //       onChange={(conf: Map<string, unknown>, status: boolean) =>
      //         props.onChange(conf, status)
      //       }
      //       propertyDefinitions={getRuntimeOptionsPropertyDefinitions(
      //         connectorProperties
      //       )}
      //     />
      //   );
      default:
        return <></>;
    }
  }

  return (
    <BrowserRouter>
      <I18nextProvider i18n={i18n}>
        {chooseStep(props.activeStep)}
      </I18nextProvider>
    </BrowserRouter>
  );
};

export default DebeziumConfigurator;
