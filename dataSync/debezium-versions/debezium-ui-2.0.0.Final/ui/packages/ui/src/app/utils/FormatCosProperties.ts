/* tslint:disable: no-string-literal */
import { resolveRef } from './ResolveSchemaRef';
import { ConnectorProperty } from '@debezium/ui-models';
import {
  formatPropertyDefinitions,
  getFormattedProperties,
  ConnectorTypeId,
} from 'shared';

const getType = (prop: any) => {
  let type = prop['type'];
  let format = prop['format'];

  // handle passwords, which have 'oneOf' attributes
  const oneOf = prop['oneOf'];
  if (oneOf && oneOf !== null) {
    for (const oneOfElem of oneOf) {
      const oneOfType = oneOfElem['type'];
      const oneOfFormat = oneOfElem['format'];
      if (oneOfFormat && oneOfFormat === 'password') {
        type = oneOfType;
        format = oneOfFormat;
        break;
      }
    }
  }

  if (type === 'string') {
    if (!format) {
      return 'STRING';
    } else if (format === 'password') {
      return 'PASSWORD';
    } else if (format === 'class') {
      return 'CLASS';
    } else if (format.indexOf('list') !== -1) {
      return 'LIST';
    } else {
      return 'STRING';
    }
  } else if (type === 'boolean') {
    return 'BOOLEAN';
  } else if (type === 'integer') {
    if (!format) {
      return 'INT';
    } else if (format === 'int32') {
      return 'INT';
    } else if (format === 'int64') {
      return 'LONG';
    } else {
      return 'INT';
    }
  }
  return 'STRING';
};

const getMandatory = (nullable: any) => {
  if (nullable === undefined || nullable === true) {
    return false;
  } else {
    return true;
  }
};

const setProperties = (property, parentObj?) => {
  const name =
    property['x-name'] === 'column.mask.hash.([^.]+).with.salt.(.+)'
      ? 'column.mask.hash'
      : property['x-name'];
  const nullable = property['nullable'];
  const connProp = {
    category: property['x-category'],
    description: property['description'],
    displayName: property['title'],
    name,
    isMandatory: getMandatory(nullable),
  } as ConnectorProperty;

  if (parentObj) {
    connProp['parentObj'] = parentObj;
  }

  connProp.type = getType(property);

  if (property['default'] !== `undefined`) {
    connProp.defaultValue = property['default'];
  }
  if (property['enum']) {
    connProp.allowedValues = property['enum'];
  }
  return connProp;
};

/**
 * Format the Connector properties passed via connector prop
 * @param connectorData
 * @returns ConnectorProperty[]
 */
export const getPropertiesData = (connectorData: any): ConnectorProperty[] => {
  const connProperties: ConnectorProperty[] = [];
  const schema = resolveRef(connectorData.schema, connectorData.schema);
  const schemaProperties = schema.properties;

  for (const propKey of Object.keys(schemaProperties)) {
    const prop = schemaProperties[propKey];
    if (prop['type'] === 'object') {
      for (const propertiesKey of Object.keys(prop.properties)) {
        const property = prop.properties[propertiesKey];

        connProperties.push(setProperties(property));
      }
    } else {
      connProperties.push(setProperties(prop));
    }
  }
  return formatPropertyDefinitions(
    getFormattedProperties(connProperties, ConnectorTypeId.POSTGRES)
  );
};

/**
 * Format the Connector properties passed via connector prop
 * @param connectorData
 * @returns ConnectorProperty[]
 */
 export const getPropertiesDataDownstream = (connectorData: any): ConnectorProperty[] => {
  const connProperties: ConnectorProperty[] = [];
  const schema = connectorData.components.schemas;
  const schemaDefinition = schema[Object.keys(schema)[0]];
  console.log(schema);
  const schemaProperties = schemaDefinition.properties;

  for (const propKey of Object.keys(schemaProperties)) {
    const prop = schemaProperties[propKey];
    if (prop['type'] === 'object') {
      for (const propertiesKey of Object.keys(prop.properties)) {
        const property = prop.properties[propertiesKey];

        connProperties.push(setProperties(property));
      }
    } else {
      connProperties.push(setProperties(prop));
    }
  }
  return formatPropertyDefinitions(
    getFormattedProperties(connProperties, ConnectorTypeId.POSTGRES)
  );
};

