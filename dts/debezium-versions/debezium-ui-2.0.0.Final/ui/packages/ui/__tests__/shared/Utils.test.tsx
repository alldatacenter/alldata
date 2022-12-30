import {
  ConnectorTypeId,
  getAdvancedPropertyDefinitions,
  getBasicPropertyDefinitions,
  getConnectorTypeDescription,
  getDataOptionsPropertyDefinitions,
  getFilterConfigurationPageContent,
  getFilterPropertyDefinitions,
  getFormattedProperties,
  getRuntimeOptionsPropertyDefinitions,
  isDataOptions,
  isRuntimeOptions,
  PropertyCategory,
} from "../../src/app/shared/Utils";
import CONNECTOR_PROPERTIES from "../../assets/test/connector-properties";
import CONNECTOR_PROPERIES from "../../assets/test/connector-properties";

describe("Utils", () => {

  it("test isDataOptions", () => {
    expect(isDataOptions(PropertyCategory.DATA_OPTIONS_GENERAL)).toBe(true);
    expect(isDataOptions(PropertyCategory.DATA_OPTIONS_ADVANCED)).toBe(true);
    expect(isDataOptions(PropertyCategory.DATA_OPTIONS_SNAPSHOT)).toBe(true);
    expect(isDataOptions(PropertyCategory.RUNTIME_OPTIONS_ENGINE)).toBe(false);
  });

  it("test isRuntimeOptions", () => {
    expect(isRuntimeOptions(PropertyCategory.RUNTIME_OPTIONS_ENGINE)).toBe(true);
    expect(isRuntimeOptions(PropertyCategory.RUNTIME_OPTIONS_HEARTBEAT)).toBe(true);
    expect(isRuntimeOptions(PropertyCategory.DATA_OPTIONS_SNAPSHOT)).toBe(false);
  });

  it("test getConnectorTypeDescription", () => {
    const conn = {
      id: ConnectorTypeId.MONGO,
      className: "theClassName",
      displayName: "theDisplayName",
      version: "theVersion",
      enabled: true
    }
  
    expect(getConnectorTypeDescription(conn)).toBe("MongoDB database");

    conn.id = ConnectorTypeId.POSTGRES;
    expect(getConnectorTypeDescription(conn)).toBe("PostgreSQL database");

    conn.id = ConnectorTypeId.MYSQL;
    expect(getConnectorTypeDescription(conn)).toBe("MySQL database");

    conn.id = ConnectorTypeId.SQLSERVER;
    expect(getConnectorTypeDescription(conn)).toBe("SQLServer database");
  });

  it("test getBasicPropertyDefinitions", () => {
    expect(getBasicPropertyDefinitions(CONNECTOR_PROPERTIES)).toHaveLength(2);
  });

  it("test getAdvancedPropertyDefinitions", () => {
    expect(getAdvancedPropertyDefinitions(CONNECTOR_PROPERTIES)).toHaveLength(4);
  });

  it("test getFilterPropertyDefinitions", () => {
    expect(getFilterPropertyDefinitions(CONNECTOR_PROPERTIES)).toHaveLength(1);
  });

  it("test getDataOptionsPropertyDefinitions", () => {
    expect(getDataOptionsPropertyDefinitions(CONNECTOR_PROPERTIES)).toHaveLength(3);
  });

  it("test getRuntimeOptionsPropertyDefinitions", () => {
    expect(getRuntimeOptionsPropertyDefinitions(CONNECTOR_PROPERTIES)).toHaveLength(2);
  });

  it("test getFilterConfigurationPageContent", () => {
    expect(getFilterConfigurationPageContent(ConnectorTypeId.MONGO).fieldArray).toEqual(
      expect.arrayContaining([
        expect.objectContaining({field: "database"}),
        expect.objectContaining({field: "collection"}),
        expect.objectContaining({field: "field"})
      ])
    );
    expect(getFilterConfigurationPageContent(ConnectorTypeId.MYSQL).fieldArray).toEqual(
      expect.arrayContaining([
        expect.objectContaining({field: "database"}),
        expect.objectContaining({field: "table"}),
        expect.objectContaining({field: "column"})
      ])
    );
    expect(getFilterConfigurationPageContent(ConnectorTypeId.POSTGRES).fieldArray).toEqual(
      expect.arrayContaining([
        expect.objectContaining({field: "schema"}),
        expect.objectContaining({field: "table"}),
        expect.objectContaining({field: "column"})
      ])
    );
  });

  it("test getFormattedProperties", () => {
    // Test setting of formatting options on the properties
    expect(getFormattedProperties(CONNECTOR_PROPERIES, ConnectorTypeId.POSTGRES).length).toBe(11);
    expect(getFormattedProperties(CONNECTOR_PROPERIES, ConnectorTypeId.POSTGRES)).toEqual(
      expect.arrayContaining([
        expect.objectContaining({name: "topic.prefix", gridWidthLg: 12, gridWidthSm: 12}),
        expect.objectContaining({name: "heartbeat.interval.ms", gridWidthLg: 4, gridWidthSm: 12})
      ])
    );
    expect(getFormattedProperties(CONNECTOR_PROPERIES, ConnectorTypeId.MYSQL).length).toBe(11);
    expect(getFormattedProperties(CONNECTOR_PROPERIES, ConnectorTypeId.MYSQL)).toEqual(
      expect.arrayContaining([
        expect.objectContaining({name: "topic.prefix", gridWidthLg: 12, gridWidthSm: 12}),
        expect.objectContaining({name: "heartbeat.interval.ms", gridWidthLg: 4, gridWidthSm: 12})
      ])
    );
    expect(getFormattedProperties(CONNECTOR_PROPERIES, ConnectorTypeId.MONGO).length).toBe(11);
    expect(getFormattedProperties(CONNECTOR_PROPERIES, ConnectorTypeId.MONGO)).toEqual(
      expect.arrayContaining([
        expect.objectContaining({name: "topic.prefix", gridWidthLg: 12, gridWidthSm: 12}),
        expect.objectContaining({name: "heartbeat.interval.ms", gridWidthLg: 4, gridWidthSm: 12})
      ])
    );
  });

});
