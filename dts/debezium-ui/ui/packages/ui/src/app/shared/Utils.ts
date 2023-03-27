import { ConnectorProperty, ConnectorType } from '@debezium/ui-models';
import _ from 'lodash';

export enum ConnectorState {
  UNASSIGNED = 'UNASSIGNED',
  RUNNING = 'RUNNING',
  PAUSED = 'PAUSED',
  FAILED = 'FAILED',
  DESTROYED = 'DESTROYED',
}

export enum ConnectorTypeId {
  POSTGRES = 'postgres',
  MYSQL = 'mysql',
  SQLSERVER = 'sqlserver',
  MONGO = 'mongodb',
  ORACLE = 'oracle'
}

export enum DatabaseFilter {
  NAME = 'database',
  LABEL = 'databaseName',
}

export enum SchemaFilter {
  NAME = 'schema',
  LABEL = 'schemaName',
}

export enum TableFilter {
  NAME = 'table',
  LABEL = 'tableName',
}

export enum CollectionFilter {
  NAME = 'collection',
  LABEL = 'collectionName',
}

export enum ColumnFilter {
  NAME = 'column',
  LABEL = 'columnName',
}

export enum FieldFilter {
  NAME = 'field',
  LABEL = 'fieldName',
}

export enum PropertyName {
  BIGINT_UNSIGNED_HANDLING_MODE = 'bigint.unsigned.handling.mode',
  BINARY_HANDLING_MODE = 'binary.handling.mode',
  BINLOG_BUFFER_SIZE = 'binlog.buffer.size',
  COLLECTION_INCLUDE_LIST = 'collection.include.list',
  COLLECTION_EXCLUDE_LIST = 'collection.exclude.list',
  COLUMN_INCLUDE_LIST = 'column.include.list',
  COLUMN_EXCLUDE_LIST = 'column.exclude.list',
  COLUMN_MASK_HASH_SALT = 'column.mask.hash',
  COLUMN_MASK = 'column.mask.with.(d+).chars',
  COLUMN_TRUNCATE = 'column.truncate.to.(d+).chars',
  CONNECT_BACKOFF_INITIAL_DELAY_MS = 'connect.backoff.initial.delay.ms',
  CONNECT_BACKOFF_MAX_DELAY_MS = 'connect.backoff.max.delay.ms',
  CONNECT_KEEP_ALIVE = 'connect.keep.alive',
  CONNECT_KEEP_ALIVE_INTERVAL_MS = 'connect.keep.alive.interval.ms',
  CONNECT_MAX_ATTEMPTS = 'connect.max.attempts',
  CONNECT_TIMEOUT_MS = 'connect.timeout.ms',
  CONNECTOR_NAME = 'connector.name',
  CONVERTERS = 'converters',
  DATABASE_DBNAME = 'database.dbname',
  DATABASE_INCLUDE_LIST = 'database.include.list',
  DATABASE_EXCLUDE_LIST = 'database.exclude.list',
  DATABASE_INITIAL_STATEMENTS = 'database.initial.statements',
  DATABASE_INSTANCE = 'database.instance',
  DATABASE_HISTORY = 'schema.history.internal',
  DATABASE_HISTORY_KAFKA_BOOTSTRAP_SERVERS = 'schema.history.internal.kafka.bootstrap.servers',
  DATABASE_HISTORY_KAFKA_RECOVERY_ATTEMPTS = 'schema.history.internal.kafka.recovery.attempts',
  DATABASE_HISTORY_KAFKA_RECOVERY_POLL_INTERVAL_MS = 'schema.history.internal.kafka.recovery.poll.interval.ms',
  DATABASE_HISTORY_KAFKA_TOPIC = 'schema.history.internal.kafka.topic',
  DATABASE_HOSTNAME = 'database.hostname',
  DATABASE_JDBC_DRIVER = 'database.jdbc.driver',
  DATABASE_SERVER_ID = 'database.server.id',
  DATABASE_SERVER_ID_OFFSET = 'database.server.id.offset',
  DATABASE_PORT = 'database.port',
  DATABASE_USER = 'database.user',
  DATABASE_PASSWORD = 'database.password',
  DATABASE_SSLMODE = 'database.sslmode',
  DATABASE_SSLCERT = 'database.sslcert',
  DATABASE_SSLPASSWORD = 'database.sslpassword',
  DATABASE_SSLROOTCERT = 'database.sslrootcert',
  DATABASE_SSLKEY = 'database.sslkey',
  DATABASE_SSLFACTORY = 'database.sslfactory',
  DATABASE_TCPKEEPALIVE = 'database.tcpKeepAlive',
  DECIMAL_HANDLING_MODE = 'decimal.handling.mode',
  ENABLE_TIME_ADJUSTER = 'enable.time.adjuster',
  EVENT_DESERIALIZATION_FAILURE_HANDLING_MODE = 'event.deserialization.failure.handling.mode',
  EVENT_PROCESSING_FAILURE_HANDLING_MODE = 'event.processing.failure.handling.mode',
  FIELD_EXCLUDE_LIST = 'field.exclude.list',
  FIELD_RENAMES = 'field.renames',
  GTID_SOURCE_FILTER_DML_EVENTS = 'gtid.source.filter.dml.events',
  GTID_SOURCE_INCLUDES = 'gtid.source.includes',
  GTID_SOURCE_EXCLUDES = 'gtid.source.excludes',
  HEARTBEAT_ACTION_QUERY = 'heartbeat.action.query',
  HEARTBEAT_INTERVAL_MS = 'heartbeat.interval.ms',
  HEARTBEAT_TOPICS_PREFIX = 'heartbeat.topics.prefix',
  HSTORE_HANDLING_MODE = 'hstore.handling.mode',
  INCLUDE_QUERY = 'include.query',
  INCLUDE_SCHEMA_CHANGES = 'include.schema.changes',
  INCLUDE_UNKNOWN_DATATYPES = 'include.unknown.datatypes',
  INCONSISTENT_SCHEMA_HANDLING_MODE = 'inconsistent.schema.handling.mode',
  INTERVAL_HANDLING_MODE = 'interval.handling.mode',
  LOB_ENABLED = 'lob.enabled',
  LOG_MINING_ARCHIVE_DESTINATION_NAME = 'log.mining.archive.destination.name',
  LOG_MINING_ARCHIVE_LOG_ONLY_MODE = 'log.mining.archive.log.only.mode',
  LOG_MINING_ARCHIVE_LOG_HOURS = 'log.mining.archive.log.hours',
  LOG_MINING_BATCH_SIZE_DEFAULT = 'log.mining.batch.size.default',
  LOG_MINING_BATCH_SIZE_MIN = 'log.mining.batch.size.min',
  LOG_MINING_BATCH_SIZE_MAX = 'log.mining.batch.size.max',
  LOG_MINING_SLEEP_TIME_DEFAULT_MS = 'log.mining.sleep.time.default.ms',
  LOG_MINING_SLEEP_TIME_MIN_MS = 'log.mining.sleep.time.min.ms',
  LOG_MINING_SLEEP_TIME_MAX_MS = 'log.mining.sleep.time.max.ms',
  LOG_MINING_SLEEP_TIME_INCREMENT_MS = 'log.mining.sleep.time.increment.ms',
  LOG_MINING_TRANSACTION_RETENTION_HOURS = 'log.mining.transaction.retention.hours',
  MAX_BATCH_SIZE = 'max.batch.size',
  MAX_QUEUE_SIZE = 'max.queue.size',
  MESSAGE_KEY_COLUMNS = 'message.key.columns',
  MONGODB_HOSTS = 'mongodb.hosts',
  MONGODB_MEMBERS_AUTO_DISCOVER = 'mongodb.members.auto.discover',
  MONGODB_POLL_INTERVAL_MS = 'mongodb.poll.interval.ms',
  MONGODB_USER = 'mongodb.user',
  MONGODB_PASSWORD = 'mongodb.password',
  MONGODB_NAME = 'mongodb.name',
  MONGODB_SSL_ENABLED = 'mongodb.ssl.enabled',
  MONGODB_SSL_INVALID_HOSTNAME_ALLOWED = 'mongodb.ssl.invalid.hostname.allowed',
  MONGODB_CONNECT_TIMEOUT_MS = 'mongodb.connect.timeout.ms',
  MONGODB_AUTHSOURCE = 'mongodb.authsource',
  MONGODB_SERVER_SELECTION_TIMEOUT_MS = 'mongodb.server.selection.timeout.ms',
  MONGODB_SOCKET_TIMEOUT_MS = 'mongodb.socket.timeout.ms',
  PLUGIN_NAME = 'plugin.name',
  POLL_INTERVAL_MS = 'poll.interval.ms',
  PROVIDE_TRANSACTION_METADATA = 'provide.transaction.metadata',
  PUBLICATION_NAME = 'publication.name',
  PUBLICATION_AUTOCREATE_MODE = 'publication.autocreate.mode',
  QUERY_FETCH_SIZE = 'query.fetch.size',
  RETRIABLE_RESTART_CONNECTOR_WAIT_MS = 'retriable.restart.connector.wait.ms',
  SANITIZE_FIELD_NAMES = 'sanitize.field.names',
  SCHEMA_INCLUDE_LIST = 'schema.include.list',
  SCHEMA_EXCLUDE_LIST = 'schema.exclude.list',
  SCHEMA_REFRESH_MODE = 'schema.refresh.mode',
  SKIPPED_OPERATIONS = 'skipped.operations',
  SLOT_NAME = 'slot.name',
  SLOT_DROP_ON_STOP = 'slot.drop.on.stop',
  SLOT_STREAM_PARAMS = 'slot.stream.params',
  SLOT_MAX_RETRIES = 'slot.max.retries',
  SLOT_RETRY_DELAY_MS = 'slot.retry.delay.ms',
  SNAPSHOT_MODE = 'snapshot.mode',
  SNAPSHOT_DELAY_MS = 'snapshot.delay.ms',
  SNAPSHOT_FETCH_SIZE = 'snapshot.fetch.size',
  SNAPSHOT_ISOLATION_MODE = 'snapshot.isolation.mode',
  SNAPSHOT_MAX_THREADS = 'snapshot.max.threads',
  SNAPSHOT_SELECT_STATEMENT_OVERRIDES = 'snapshot.select.statement.overrides',
  SNAPSHOT_LOCK_TIMEOUT_MS = 'snapshot.lock.timeout.ms',
  SNAPSHOT_LOCKING_MODE = 'snapshot.locking.mode',
  SNAPSHOT_NEW_TABLES = 'snapshot.new.tables',
  SNAPSHOT_CUSTOM_CLASS = 'snapshot.custom.class',
  SOURCE_STRUCT_VERSION = 'source.struct.version',
  STATUS_UPDATE_INTERVAL_MS = 'status.update.interval.ms',
  TABLE_IGNORE_BUILTIN = 'table.ignore.builtin',
  TABLE_INCLUDE_LIST = 'table.include.list',
  TABLE_EXCLUDE_LIST = 'table.exclude.list',
  TIME_PRECISION_MODE = 'time.precision.mode',
  TOASTED_VALUE_PLACEHOLDER = 'toasted.value.placeholder',
  TOMBSTONES_ON_DELETE = 'tombstones.on.delete',
  TOPIC_CREATION_PREFIX = 'topic.creation.',
  TOPIC_CREATION_GROUP_PREFIX = 'topic.creation.(.+).',
  TOPIC_CREATION_DEFAULT_REPLICATION_FACTOR = 'topic.creation.default.replication.factor',
  TOPIC_CREATION_DEFAULT_PARTITIONS = 'topic.creation.default.partitions',
  TOPIC_CREATION_DEFAULT_CLEANUP_POLICY = 'topic.creation.default.cleanup.policy',
  TOPIC_CREATION_DEFAULT_COMPRESSION_TYPE = 'topic.creation.default.compression.type',
  TOPIC_CREATION_GROUPS = 'topic.creation.groups',
  TOPIC_CREATION_GROUP_INCLUDE = 'topic.creation.(.+).include',
  TOPIC_CREATION_GROUP_EXCLUDE = 'topic.creation.(.+).exclude',
  TOPIC_CREATION_GROUP_REPLICATION_FACTOR = 'topic.creation.(.+).replication.factor',
  TOPIC_CREATION_GROUP_PARTITIONS = 'topic.creation.(.+).partitions',
  TOPIC_CREATION_GROUP_CLEANUP_POLICY = 'topic.creation.(.+).cleanup.policy',
  TOPIC_CREATION_GROUP_COMPRESSION_TYPE = 'topic.creation.(.+).compression.type',
  TOPIC_PREFIX = 'topic.prefix',
  XMIN_FETCH_INTERVAL_MS = 'xmin.fetch.interval.ms',
  DATA_SHAPE = 'data_shape',
  DATA_SHAPE_KEY = 'data_shape.key',
  DATA_SHAPE_VALUE = 'data_shape.value',
}

export enum PropertyCategory {
  CONNECTOR_NAME = 'CONNECTOR_NAME',
  BASIC = 'CONNECTION',
  ADVANCED_GENERAL = 'CONNECTION_ADVANCED',
  ADVANCED_REPLICATION = 'CONNECTION_ADVANCED_REPLICATION',
  ADVANCED_PUBLICATION = 'CONNECTION_ADVANCED_PUBLICATION',
  ADVANCED_SSL = 'CONNECTION_ADVANCED_SSL',
  FILTERS = 'FILTERS',
  DATA_OPTIONS_GENERAL = 'CONNECTOR',
  DATA_OPTIONS_SNAPSHOT = 'CONNECTOR_SNAPSHOT',
  DATA_OPTIONS_ADVANCED = 'CONNECTOR_ADVANCED',
  RUNTIME_OPTIONS_ENGINE = 'ADVANCED',
  RUNTIME_OPTIONS_HEARTBEAT = 'ADVANCED_HEARTBEAT',
}

/**
 * Max retries for re-fetching the api call in case of error
 */
const MAX_RETRIES: number = 2;

/**
 * Get a description of the ConnectorType, based on the id
 * @param connType the connector type
 */
export function getConnectorTypeDescription(connType: ConnectorType): string {
  if (connType.id === ConnectorTypeId.MYSQL) {
    return 'MySQL database';
  } else if (connType.id === ConnectorTypeId.POSTGRES) {
    return 'PostgreSQL database';
  } else if (connType.id === ConnectorTypeId.SQLSERVER) {
    return 'SQLServer database';
  } else if (connType.id === ConnectorTypeId.MONGO) {
    return 'MongoDB database';
  } else if (connType.id === ConnectorTypeId.ORACLE) {
    return 'Oracle database';
  }
  return 'Unknown type';
}

/**
 * Get the basic properties
 * @param propertyDefns the array of all ConnectorProperty objects
 */
export function getBasicPropertyDefinitions(
  propertyDefns: ConnectorProperty[],
  isMCS?: boolean
): ConnectorProperty[] {
  const connProperties: ConnectorProperty[] = [];
  for (const propDefn of propertyDefns) {
    if (propDefn?.category === PropertyCategory.BASIC) {
      connProperties.push(propDefn);
    }
  }
  // Add a property for the Connector name
  const connNameProperty = {
    category: PropertyCategory.CONNECTOR_NAME,
    description: 'A name for the connector which will be created',
    displayName: 'Connector name',
    name: PropertyName.CONNECTOR_NAME,
    isMandatory: true,
    type: 'STRING',
    gridWidthLg: 4,
    gridWidthSm: 12,
  } as ConnectorProperty;
  !isMCS && connProperties.push(connNameProperty);

  return connProperties;
}

export function formatPropertyDefinitions(
  propertyValues: ConnectorProperty[]
): ConnectorProperty[] {
  return propertyValues.map((value: ConnectorProperty) => {
    value.name = value.name.includes('data_shape')
      ? value.name
      : value.name.replace(/\./g, '&');
    return value;
  });
}

/**
 * Get the advanced properties
 * @param propertyDefns the array of all ConnectorProperty objects
 */
export function getAdvancedPropertyDefinitions(
  propertyDefns: ConnectorProperty[]
): ConnectorProperty[] {
  const connProperties: ConnectorProperty[] = [];
  for (const propDefn of propertyDefns) {
    if (
      propDefn?.category === PropertyCategory.ADVANCED_GENERAL ||
      propDefn?.category === PropertyCategory.ADVANCED_PUBLICATION ||
      propDefn?.category === PropertyCategory.ADVANCED_REPLICATION ||
      propDefn?.category === PropertyCategory.ADVANCED_SSL
    ) {
      connProperties.push(propDefn);
    }
  }
  return connProperties;
}

/**
 * Get the filter properties
 * @param propertyDefns the array of all ConnectorProperty objects
 */
export function getFilterPropertyDefinitions(
  propertyDefns: ConnectorProperty[]
): ConnectorProperty[] {
  const connProperties: ConnectorProperty[] = [];
  for (const propDefn of propertyDefns) {
    if (propDefn.category === PropertyCategory.FILTERS) {
      connProperties.push(propDefn);
    }
  }
  return connProperties;
}

/**
 * Get the data options properties
 * @param propertyDefns the array of all ConnectorProperty objects
 */
export function getDataOptionsPropertyDefinitions(
  propertyDefns: ConnectorProperty[]
): ConnectorProperty[] {
  const connProperties: ConnectorProperty[] = [];
  for (const propDefn of propertyDefns) {
    if (
      propDefn?.category === PropertyCategory.DATA_OPTIONS_GENERAL ||
      propDefn?.category === PropertyCategory.DATA_OPTIONS_ADVANCED ||
      propDefn?.category === PropertyCategory.DATA_OPTIONS_SNAPSHOT
    ) {
      connProperties.push(propDefn);
    }
  }
  return connProperties;
}

/**
 * Get the runtime options properties
 * @param propertyDefns the array of all ConnectorProperty objects
 */
export function getRuntimeOptionsPropertyDefinitions(
  propertyDefns: ConnectorProperty[]
): ConnectorProperty[] {
  const connProperties: ConnectorProperty[] = [];
  for (const propDefn of propertyDefns) {
    if (
      propDefn?.category === PropertyCategory.RUNTIME_OPTIONS_ENGINE ||
      propDefn?.category === PropertyCategory.RUNTIME_OPTIONS_HEARTBEAT
    ) {
      connProperties.push(propDefn);
    }
  }
  return connProperties;
}

/**
 * Determine if the supplied category is one of the Data Options
 * @param propertyCategory the category
 */
export function isDataOptions(propertyCategory: PropertyCategory): boolean {
  return (
    propertyCategory === PropertyCategory.DATA_OPTIONS_GENERAL ||
    propertyCategory === PropertyCategory.DATA_OPTIONS_ADVANCED ||
    propertyCategory === PropertyCategory.DATA_OPTIONS_SNAPSHOT
  );
}

/**
 * Determine if the supplied category is one of the Runtime Options
 * @param propertyCategory the category
 */
export function isRuntimeOptions(propertyCategory: PropertyCategory): boolean {
  return (
    propertyCategory === PropertyCategory.RUNTIME_OPTIONS_ENGINE ||
    propertyCategory === PropertyCategory.RUNTIME_OPTIONS_HEARTBEAT
  );
}

/**
 * Combine the supplied value maps, then minimize the property values.
 * @param propValues1 map 1 of property values
 * @param propValues2 map 2 of property values
 * @param propertyDefns the array of property definitions
 */
export function combineAndMinimizePropertyValues(
  propValues1: Map<string, string>,
  propValues2: Map<string, string>,
  propertyDefns: ConnectorProperty[]
): Map<string, string> {
  const valueMap = new Map(
    (function* () {
      yield* propValues1;
      yield* propValues2;
    })()
  );
  const minimizedValues = minimizePropertyValues(
    valueMap,
    propertyDefns
  );
  return minimizedValues;
}

/**
 * Minimize property values.  This function eliminates property values from the supplied map
 * which are not required to be supplied to the backend for validation or connector creation.
 *   - include property values which are mandatory
 *   - include property values which have a default, and the value is different than the default
 *   - include property value if it is not empty - (if not mandatory and has no default)
 * @param propertyValues the map of property values
 * @param propertyDefns the array of property definitions
 */
export function minimizePropertyValues(
  propertyValues: Map<string, string>,
  propertyDefns: ConnectorProperty[]
): Map<string, string> {
  const minimizedValues: Map<string, string> = new Map<string, string>();

  // console.log("MinimizePropertyValues: " + JSON.stringify(mapToObject(propertyValues)));
  propertyValues.forEach((value: string, key: string) => {
    // Get the corresponding property definition
    const propDefn = propertyDefns.find(
      (prop) => prop?.name.replace(/&/g, '.') === key
    );
    if (propDefn) {
      // Include mandatory values
      if (propDefn.isMandatory) {
        minimizedValues.set(key, value);
        // Include non-mandatory if has default and value is different
      } else if (propDefn.defaultValue != null) {
        if (propDefn.defaultValue !== value) {
          minimizedValues.set(key, value);
        }
      } else if (propDefn.name.includes('(d+)_chars') && value !== '') {
        // The value value can have multiple entries (entries are separated by '@^')
        // Example colsString&&number @^ colsString&&number
        const entries = value.split('@^');
        for (const entry of entries) {
          if (entry && entry !== '') {
            const [a, b] = entry.split('&&');
            const updatedKey = key.replace('(d+)', b);
            if (a !== '' && b !== '') {
              minimizedValues.set(updatedKey, a.trim());
            }
          }
        }
      } else if (propDefn.name.startsWith('column_mask_hash') && value !== '') {
        // The value value can have multiple entries (entries are separated by '#$')
        // Example Cols&&Hash||Salt #$ Cols&&Hash||Salt
        const entries = value.split('#$');
        for (const entry of entries) {
          if (entry && entry !== '') {
            const [cols, trailing] = entry.split('&&');
            if (trailing) {
              const [hash, salt] = trailing.split('||');
              if (cols !== '' && hash !== '' && salt && salt !== '') {
                const updatedKey = 'column.mask.hash.([^.]+).with.salt.(.+)'
                  .replace('([^.]+)', hash)
                  .replace('(.+)', salt);
                minimizedValues.set(updatedKey, cols);
              }
            }
          }
        }
        // Include non-mandatory if no default, and not empty
      } else if (value !== '') {
        minimizedValues.set(key, value);
      }
    }
  });

  return minimizedValues;
}

/**
 * Alter the supplied connector properties for display purposes.
 * - Apply optional grid formatting values to some properties for better layouts.
 * - reset type if an alternate component is desired
 * @param propertyDefns the array of property definitions
 * @returns the array of altered property definitions
 */
export function getFormattedProperties(
  propertyDefns: ConnectorProperty[],
  connectorTypeId: string
): ConnectorProperty[] {
  const formattedPropertyDefns: ConnectorProperty[] = [...propertyDefns];

  if (connectorTypeId === ConnectorTypeId.POSTGRES) {
    for (const propDefn of formattedPropertyDefns) {
      propDefn.gridWidthSm = 12;
      const propName = propDefn.name.replace(/&/g, '.'); // Ensure dotted version of name
      switch (propName) {
        case PropertyName.BINARY_HANDLING_MODE:
        case PropertyName.DECIMAL_HANDLING_MODE:
        case PropertyName.HSTORE_HANDLING_MODE:
        case PropertyName.INTERVAL_HANDLING_MODE:
        case PropertyName.TIME_PRECISION_MODE:
        case PropertyName.EVENT_PROCESSING_FAILURE_HANDLING_MODE:
        case PropertyName.PLUGIN_NAME:
        case PropertyName.PUBLICATION_AUTOCREATE_MODE:
        case PropertyName.SCHEMA_REFRESH_MODE:
          propDefn.gridWidthLg = 4;
          break;
        case PropertyName.DATA_SHAPE:
        case PropertyName.DATA_SHAPE_KEY:
        case PropertyName.DATA_SHAPE_VALUE:
          propDefn.gridWidthLg = 6;
          break;
        case PropertyName.SLOT_MAX_RETRIES:
          propDefn.gridWidthLg = 6;
          propDefn.type = 'NON-NEG-INT';
          break;
        case PropertyName.DATABASE_HOSTNAME:
          propDefn.gridWidthLg = 8;
          break;
        case PropertyName.SNAPSHOT_MODE:
          propDefn.gridWidthLg = 9;
          break;
        case PropertyName.DATABASE_TCPKEEPALIVE:
        case PropertyName.SLOT_DROP_ON_STOP:
        case PropertyName.TOMBSTONES_ON_DELETE:
        case PropertyName.PROVIDE_TRANSACTION_METADATA:
        case PropertyName.SANITIZE_FIELD_NAMES:
        case PropertyName.INCLUDE_UNKNOWN_DATATYPES:
          propDefn.gridWidthLg = 12;
          propDefn.type = 'BOOLEAN-SWITCH';
          break;
        case PropertyName.SNAPSHOT_DELAY_MS:
        case PropertyName.SNAPSHOT_LOCK_TIMEOUT_MS:
        case PropertyName.RETRIABLE_RESTART_CONNECTOR_WAIT_MS:
        case PropertyName.HEARTBEAT_INTERVAL_MS:
        case PropertyName.POLL_INTERVAL_MS:
          propDefn.gridWidthLg = 4;
          propDefn.type = 'DURATION';
          propDefn.displayName = propDefn.displayName
            .replace('(ms)', '')
            .replace('(milli-seconds)', '')
            .replace('(milliseconds)', '');
          break;
        case PropertyName.SLOT_RETRY_DELAY_MS:
        case PropertyName.STATUS_UPDATE_INTERVAL_MS:
        case PropertyName.XMIN_FETCH_INTERVAL_MS:
          propDefn.gridWidthLg = 6;
          propDefn.type = 'DURATION';
          propDefn.displayName = propDefn.displayName.replace('(ms)', '');
          break;
        case PropertyName.DATABASE_PORT:
        case PropertyName.SNAPSHOT_FETCH_SIZE:
          propDefn.gridWidthLg = 4;
          propDefn.type = 'NON-NEG-INT';
          break;
        case PropertyName.MAX_QUEUE_SIZE:
        case PropertyName.MAX_BATCH_SIZE:
          propDefn.gridWidthLg = 4;
          propDefn.type = 'POS-INT';
          break;
        case PropertyName.COLUMN_TRUNCATE:
        case PropertyName.COLUMN_MASK:
          propDefn.gridWidthLg = 12;
          propDefn.type = 'COL_MASK_OR_TRUNCATE';
          break;
        case PropertyName.COLUMN_MASK_HASH_SALT:
          propDefn.gridWidthLg = 12;
          propDefn.type = 'COL_MASK_HASH_SALT';
          break;
        default:
          propDefn.gridWidthLg = 12;
          break;
      }
    }
  } else if (connectorTypeId === ConnectorTypeId.MONGO) {
    for (const propDefn of formattedPropertyDefns) {
      propDefn.gridWidthSm = 12;
      const propName = propDefn.name.replace(/&/g, '.'); // Ensure dotted version of name
      switch (propName) {
        case PropertyName.MONGODB_MEMBERS_AUTO_DISCOVER:
        case PropertyName.TOMBSTONES_ON_DELETE:
        case PropertyName.PROVIDE_TRANSACTION_METADATA:
        case PropertyName.SANITIZE_FIELD_NAMES:
          propDefn.gridWidthLg = 12;
          propDefn.type = 'BOOLEAN-SWITCH';
          break;
        case PropertyName.EVENT_PROCESSING_FAILURE_HANDLING_MODE:
          propDefn.gridWidthLg = 4;
          break;
        case PropertyName.SNAPSHOT_MODE:
          propDefn.gridWidthLg = 9;
          break;
        case PropertyName.DATA_SHAPE:
        case PropertyName.DATA_SHAPE_KEY:
        case PropertyName.DATA_SHAPE_VALUE:
          propDefn.gridWidthLg = 6;
          break;
        case PropertyName.MONGODB_POLL_INTERVAL_MS:
        case PropertyName.MONGODB_CONNECT_TIMEOUT_MS:
        case PropertyName.CONNECT_BACKOFF_INITIAL_DELAY_MS:
        case PropertyName.CONNECT_BACKOFF_MAX_DELAY_MS:
        case PropertyName.MONGODB_SERVER_SELECTION_TIMEOUT_MS:
        case PropertyName.MONGODB_SOCKET_TIMEOUT_MS:
        case PropertyName.SNAPSHOT_DELAY_MS:
        case PropertyName.RETRIABLE_RESTART_CONNECTOR_WAIT_MS:
        case PropertyName.HEARTBEAT_INTERVAL_MS:
        case PropertyName.POLL_INTERVAL_MS:
          propDefn.gridWidthLg = 4;
          propDefn.type = 'DURATION';
          propDefn.displayName = propDefn.displayName
            .replace('(ms)', '')
            .replace('(milli-seconds)', '')
            .replace('(milliseconds)', '')
            .replace(' MS', '');
          break;
        case PropertyName.SNAPSHOT_FETCH_SIZE:
          propDefn.gridWidthLg = 4;
          propDefn.type = 'NON-NEG-INT';
          break;
        case PropertyName.QUERY_FETCH_SIZE:
          propDefn.gridWidthLg = 9;
          propDefn.type = 'NON-NEG-INT';
          break;
        case PropertyName.MAX_QUEUE_SIZE:
        case PropertyName.MAX_BATCH_SIZE:
          propDefn.gridWidthLg = 4;
          propDefn.type = 'POS-INT';
          break;
        case PropertyName.CONNECT_MAX_ATTEMPTS:
          propDefn.gridWidthLg = 9;
          propDefn.type = 'POS-INT';
          break;
        default:
          propDefn.gridWidthLg = 12;
          break;
      }
    }
  } else if (connectorTypeId === ConnectorTypeId.MYSQL) {
    for (const propDefn of formattedPropertyDefns) {
      if (propDefn) {
        propDefn.gridWidthSm = 12;
        const propName = propDefn.name.replace(/&/g, '.'); // Ensure dotted version of name
        switch (propName) {
          case PropertyName.EVENT_DESERIALIZATION_FAILURE_HANDLING_MODE:
          case PropertyName.DECIMAL_HANDLING_MODE:
          case PropertyName.TIME_PRECISION_MODE:
          case PropertyName.BIGINT_UNSIGNED_HANDLING_MODE:
          case PropertyName.EVENT_PROCESSING_FAILURE_HANDLING_MODE:
          case PropertyName.BINLOG_BUFFER_SIZE:
          case PropertyName.DATABASE_HISTORY_KAFKA_RECOVERY_ATTEMPTS:
          case PropertyName.SNAPSHOT_MODE:
          case PropertyName.SNAPSHOT_LOCKING_MODE:
          case PropertyName.SNAPSHOT_NEW_TABLES:
            propDefn.gridWidthLg = 4;
            break;
          case PropertyName.DATABASE_SERVER_ID_OFFSET:
          case PropertyName.GTID_SOURCE_INCLUDES:
          case PropertyName.GTID_SOURCE_EXCLUDES:
          case PropertyName.DATA_SHAPE:
          case PropertyName.DATA_SHAPE_KEY:
          case PropertyName.DATA_SHAPE_VALUE:
            propDefn.gridWidthLg = 6;
            break;
          case PropertyName.DATABASE_HOSTNAME:
          case PropertyName.INCONSISTENT_SCHEMA_HANDLING_MODE:
            propDefn.gridWidthLg = 8;
            break;
          case PropertyName.CONNECT_KEEP_ALIVE:
          case PropertyName.ENABLE_TIME_ADJUSTER:
          case PropertyName.GTID_SOURCE_FILTER_DML_EVENTS:
          case PropertyName.INCLUDE_SCHEMA_CHANGES:
          case PropertyName.TOMBSTONES_ON_DELETE:
          case PropertyName.INCLUDE_QUERY:
            propDefn.gridWidthLg = 12;
            propDefn.type = 'BOOLEAN-SWITCH';
            propDefn.displayName = propDefn.displayName.replace(
              '(true/false)',
              ''
            );
            break;
          case PropertyName.CONNECT_TIMEOUT_MS:
          case PropertyName.CONNECT_KEEP_ALIVE_INTERVAL_MS:
            propDefn.gridWidthLg = 6;
            propDefn.type = 'DURATION';
            propDefn.displayName = propDefn.displayName.replace('(ms)', '');
            break;
          case PropertyName.SNAPSHOT_DELAY_MS:
          case PropertyName.HEARTBEAT_INTERVAL_MS:
          case PropertyName.POLL_INTERVAL_MS:
          case PropertyName.DATABASE_HISTORY_KAFKA_RECOVERY_POLL_INTERVAL_MS:
            propDefn.gridWidthLg = 4;
            propDefn.type = 'DURATION';
            propDefn.displayName = propDefn.displayName
              .replace('(ms)', '')
              .replace('(milli-seconds)', '')
              .replace('(milliseconds)', '');
            break;
          case PropertyName.DATABASE_PORT:
          case PropertyName.SNAPSHOT_FETCH_SIZE:
            propDefn.gridWidthLg = 4;
            propDefn.type = 'NON-NEG-INT';
            break;
          case PropertyName.MAX_QUEUE_SIZE:
          case PropertyName.MAX_BATCH_SIZE:
            propDefn.gridWidthLg = 4;
            propDefn.type = 'POS-INT';
            break;
          case PropertyName.COLUMN_TRUNCATE:
          case PropertyName.COLUMN_MASK:
            propDefn.gridWidthLg = 12;
            propDefn.type = 'COL_MASK_OR_TRUNCATE';
            break;
          case PropertyName.COLUMN_MASK_HASH_SALT:
            propDefn.gridWidthLg = 12;
            propDefn.type = 'COL_MASK_HASH_SALT';
            break;
          default:
            propDefn.gridWidthLg = 12;
            break;
        }
      }
    }
  } else if (connectorTypeId === ConnectorTypeId.SQLSERVER) {
    for (const propDefn of formattedPropertyDefns) {
      propDefn.gridWidthSm = 12;
      const propName = propDefn.name.replace(/&/g, '.'); // Ensure dotted version of name
      switch (propName) {
        case PropertyName.BINARY_HANDLING_MODE:
        case PropertyName.DECIMAL_HANDLING_MODE:
        case PropertyName.TIME_PRECISION_MODE:
        case PropertyName.EVENT_PROCESSING_FAILURE_HANDLING_MODE:
        case PropertyName.SNAPSHOT_MODE:
        case PropertyName.SNAPSHOT_ISOLATION_MODE:
          propDefn.gridWidthLg = 4;
          break;
        case PropertyName.DATA_SHAPE:
        case PropertyName.DATA_SHAPE_KEY:
        case PropertyName.DATA_SHAPE_VALUE:
          propDefn.gridWidthLg = 6;
          break;
        case PropertyName.QUERY_FETCH_SIZE:
        case PropertyName.SNAPSHOT_MAX_THREADS:
        case PropertyName.MAX_QUEUE_SIZE:
        case PropertyName.MAX_BATCH_SIZE:
          propDefn.gridWidthLg = 4;
          propDefn.type = 'POS-INT';
          break;
        case PropertyName.SNAPSHOT_DELAY_MS:
        case PropertyName.SNAPSHOT_LOCK_TIMEOUT_MS:
        case PropertyName.HEARTBEAT_INTERVAL_MS:
        case PropertyName.POLL_INTERVAL_MS:
        case PropertyName.RETRIABLE_RESTART_CONNECTOR_WAIT_MS:
          propDefn.gridWidthLg = 4;
          propDefn.type = 'DURATION';
          propDefn.displayName = propDefn.displayName
            .replace('(ms)', '')
            .replace('(milli-seconds)', '')
            .replace('(milliseconds)', '');
          break;
        case PropertyName.DATABASE_PORT:
        case PropertyName.SNAPSHOT_FETCH_SIZE:
          propDefn.gridWidthLg = 4;
          propDefn.type = 'NON-NEG-INT';
          break;
        case PropertyName.DATABASE_DBNAME:
        case PropertyName.DATABASE_INSTANCE:
          propDefn.gridWidthLg = 6;
          break;
        case PropertyName.DATABASE_HISTORY_KAFKA_RECOVERY_POLL_INTERVAL_MS:
          propDefn.gridWidthLg = 6;
          propDefn.type = 'DURATION';
          propDefn.displayName = propDefn.displayName.replace('(ms)', '');
          break;
        case PropertyName.DATABASE_HISTORY_KAFKA_RECOVERY_ATTEMPTS:
          propDefn.gridWidthLg = 6;
          propDefn.type = 'POS-INT';
          break;
        case PropertyName.DATABASE_HOSTNAME:
          propDefn.gridWidthLg = 8;
          break;
        case PropertyName.TOMBSTONES_ON_DELETE:
        case PropertyName.PROVIDE_TRANSACTION_METADATA:
        case PropertyName.SANITIZE_FIELD_NAMES:
        case PropertyName.INCLUDE_SCHEMA_CHANGES:
          propDefn.gridWidthLg = 12;
          propDefn.type = 'BOOLEAN-SWITCH';
          break;
        case PropertyName.COLUMN_TRUNCATE:
        case PropertyName.COLUMN_MASK:
          propDefn.gridWidthLg = 12;
          propDefn.type = 'COL_MASK_OR_TRUNCATE';
          break;
        case PropertyName.COLUMN_MASK_HASH_SALT:
          propDefn.gridWidthLg = 12;
          propDefn.type = 'COL_MASK_HASH_SALT';
          break;
        default:
          propDefn.gridWidthLg = 12;
          break;
      }
    }
  } else if (connectorTypeId === ConnectorTypeId.ORACLE) {
    for (const propDefn of formattedPropertyDefns) {
      propDefn.gridWidthSm = 12;
      const propName = propDefn.name.replace(/_/g, ".");  // Ensure dotted version of name
      switch (propName) {
        case PropertyName.DECIMAL_HANDLING_MODE:
        case PropertyName.TIME_PRECISION_MODE:
        case PropertyName.EVENT_PROCESSING_FAILURE_HANDLING_MODE:
        case PropertyName.SNAPSHOT_MODE:
        case PropertyName.SOURCE_STRUCT_VERSION:
        case PropertyName.SNAPSHOT_LOCKING_MODE:
          propDefn.gridWidthLg = 4;
          break;
        case PropertyName.QUERY_FETCH_SIZE:
        case PropertyName.SNAPSHOT_MAX_THREADS:
        case PropertyName.MAX_QUEUE_SIZE:
        case PropertyName.MAX_BATCH_SIZE:
        case PropertyName.DATABASE_HISTORY_KAFKA_RECOVERY_ATTEMPTS:
        case PropertyName.LOG_MINING_BATCH_SIZE_DEFAULT:
        case PropertyName.LOG_MINING_BATCH_SIZE_MIN:
        case PropertyName.LOG_MINING_BATCH_SIZE_MAX:
          propDefn.gridWidthLg = 4;
          propDefn.type = "POS-INT";
          break;
        case PropertyName.SNAPSHOT_DELAY_MS:
        case PropertyName.SNAPSHOT_LOCK_TIMEOUT_MS:
        case PropertyName.HEARTBEAT_INTERVAL_MS:
        case PropertyName.POLL_INTERVAL_MS:
        case PropertyName.DATABASE_HISTORY_KAFKA_RECOVERY_POLL_INTERVAL_MS:
        case PropertyName.RETRIABLE_RESTART_CONNECTOR_WAIT_MS:
        case PropertyName.LOG_MINING_SLEEP_TIME_DEFAULT_MS:
        case PropertyName.LOG_MINING_SLEEP_TIME_MIN_MS:
        case PropertyName.LOG_MINING_SLEEP_TIME_MAX_MS:
        case PropertyName.LOG_MINING_SLEEP_TIME_INCREMENT_MS:
          propDefn.gridWidthLg = 4;
          propDefn.type = "DURATION";
          propDefn.displayName = propDefn.displayName.replace("(ms)", "").replace("(milli-seconds)","").replace("(milliseconds)","");
          break;
        case PropertyName.DATABASE_PORT:
        case PropertyName.SNAPSHOT_FETCH_SIZE:
          propDefn.gridWidthLg = 4;
          propDefn.type =  "NON-NEG-INT";
          break;
        case PropertyName.DATABASE_DBNAME:
          propDefn.gridWidthLg = 6;
          break;
        case PropertyName.DATABASE_HOSTNAME:
          propDefn.gridWidthLg = 8;
          break;
        case PropertyName.TOMBSTONES_ON_DELETE:
        case PropertyName.PROVIDE_TRANSACTION_METADATA:
        case PropertyName.SANITIZE_FIELD_NAMES:
        case PropertyName.INCLUDE_SCHEMA_CHANGES:
        case PropertyName.LOB_ENABLED:
        case PropertyName.LOG_MINING_ARCHIVE_LOG_ONLY_MODE:
          propDefn.gridWidthLg = 12;
          propDefn.type = "BOOLEAN-SWITCH";
          break;
        case PropertyName.COLUMN_TRUNCATE:
        case PropertyName.COLUMN_MASK:
          propDefn.gridWidthLg = 12;
          propDefn.type =  "COL_MASK_OR_TRUNCATE";
          break;
        case PropertyName.COLUMN_MASK_HASH_SALT:
          propDefn.gridWidthLg = 12;
          propDefn.type =  "COL_MASK_HASH_SALT";
          break;
        default:
          propDefn.gridWidthLg = 12;
          break;
      }
    }
  }
  return formattedPropertyDefns;
}

/**
 * Alter the supplied transform properties for display purposes.
 * - Apply optional grid formatting values to some properties for better layouts.
 * @param transformConfig the array of transform config
 * @returns the array of altered transform config
 */
export function getFormattedConfig(
  transformConfig: any[],
  transformTypeId: string
): any {
  const selectedTransformConfig: any = transformTypeId
    ? _.find([...transformConfig], ['transform', transformTypeId])?.properties
    : {};
  const formattedTransformConfig: any[] = [];
  // tslint:disable-next-line: forin
  for (const transform in selectedTransformConfig) {
    const transConfig = selectedTransformConfig[transform];
    transConfig.gridWidthSm = 12;
    transConfig.name = transConfig['x-name'];
    transConfig.displayName = transConfig.title;
    transConfig.allowedValues = transConfig?.enum;
    const propName = transConfig.name.replace(/\./g, '&'); // Ensure dotted version of name
    transConfig.name = propName;
    if (
      transformTypeId === 'io.debezium.transforms.Filter' ||
      transformTypeId === 'io.debezium.transforms.ContentBasedRouter'
    ) {
      switch (propName) {
        case 'condition':
          transConfig.gridWidthLg = 9;
          break;
        case 'language':
          transConfig.gridWidthLg = 3;
          break;
        case 'null_handling_mode':
          transConfig.gridWidthLg = 3;
          break;
        default:
          transConfig.gridWidthLg = 12;
          break;
      }
    } else if (
      transformTypeId === 'io.debezium.transforms.ExtractNewRecordState'
    ) {
      switch (propName) {
        case 'drop_tombstones':
          transConfig.type = 'BOOLEAN-SWITCH';
          break;
        case 'delete_handling_mode':
          transConfig.gridWidthLg = 4;
          break;
        case 'add_fields_prefix':
          transConfig.gridWidthLg = 3;
          break;
        case 'add_fields':
          transConfig.gridWidthLg = 9;
          break;
        case 'add_headers_prefix':
          transConfig.gridWidthLg = 3;
          break;
        case 'add_headers':
          transConfig.gridWidthLg = 9;
          break;
        default:
          transConfig.gridWidthLg = 12;
          break;
      }
    } else if (
      transformTypeId === 'io.debezium.transforms.ByLogicalTableRouter'
    ) {
      switch (propName) {
        case 'key_enforce​_uniqueness':
          transConfig.type = 'BOOLEAN-SWITCH';
          break;
      }
    }
    formattedTransformConfig.push(transConfig);
  }
  return formattedTransformConfig;
}

/**
 * Alter the supplied topic creation properties for display purposes.  The property names are also shortened to remove common
 * prefixes ("topic.creation.", "topic.creation.(.+).")
 * - Apply optional grid formatting values to some properties for better layouts.
 * @param topicCreationConfig the array of topic create properties
 * @returns the array of altered topic creation properties
 */
export function getFormattedTopicCreationProperties(
  topicCreationProperties: any[]
): any {
  // Topic Group forms deal with properties that have prefixes stripped off
  topicCreationProperties.map(
    (prop) =>
      (prop.name = prop.name
        .replace(PropertyName.TOPIC_CREATION_GROUP_PREFIX, '')
        .replace(PropertyName.TOPIC_CREATION_PREFIX, ''))
  );

  const formattedTopicCreationProperties: ConnectorProperty[] = [
    ...topicCreationProperties.filter(
      (defn: any) =>
        defn.name !==
        PropertyName.TOPIC_CREATION_GROUPS.replace(
          PropertyName.TOPIC_CREATION_PREFIX,
          ''
        )
    ),
  ];

  for (const topicGroupProp of formattedTopicCreationProperties) {
    topicGroupProp.gridWidthSm = 12;
    const propName = topicGroupProp.name.replace(/&/g, '.'); // Ensure dotted version of name

    switch (propName) {
      case PropertyName.TOPIC_CREATION_DEFAULT_REPLICATION_FACTOR.replace(
        PropertyName.TOPIC_CREATION_PREFIX,
        ''
      ):
      case PropertyName.TOPIC_CREATION_DEFAULT_PARTITIONS.replace(
        PropertyName.TOPIC_CREATION_PREFIX,
        ''
      ):
        topicGroupProp.gridWidthLg = 6;
        break;
      case PropertyName.TOPIC_CREATION_DEFAULT_CLEANUP_POLICY.replace(
        PropertyName.TOPIC_CREATION_PREFIX,
        ''
      ):
      case PropertyName.TOPIC_CREATION_DEFAULT_COMPRESSION_TYPE.replace(
        PropertyName.TOPIC_CREATION_PREFIX,
        ''
      ):
        topicGroupProp.gridWidthLg = 6;
        break;
      case PropertyName.TOPIC_CREATION_GROUP_INCLUDE.replace(
        PropertyName.TOPIC_CREATION_GROUP_PREFIX,
        ''
      ):
      case PropertyName.TOPIC_CREATION_GROUP_EXCLUDE.replace(
        PropertyName.TOPIC_CREATION_GROUP_PREFIX,
        ''
      ):
        topicGroupProp.gridWidthLg = 12;
        break;
      default:
        topicGroupProp.gridWidthLg = 12;
        break;
    }
  }
  return formattedTopicCreationProperties;
}

/**
 * Get a Filter configuration page content Obj, based on the
 * @param connectorType the connector type
 */
export function getFilterConfigurationPageContent(connectorType: string): any {
  let returnObj;
  if (connectorType.includes(ConnectorTypeId.MONGO)) {
    returnObj = {
      fieldArray: [
        {
          field: DatabaseFilter.NAME,
          valueSample: DatabaseFilter.LABEL,
          preview: true,
          excludeFilter: false,
        },
        {
          field: CollectionFilter.NAME,
          valueSample: `${DatabaseFilter.LABEL}.${CollectionFilter.LABEL}`,
          preview: true,
          excludeFilter: false,
        },
        {
          field: FieldFilter.NAME,
          valueSample: `${DatabaseFilter.LABEL}.${CollectionFilter.LABEL}.${FieldFilter.LABEL}`,
          preview: false,
          excludeFilter: true,
        },
      ],
    };
  } else if (connectorType.includes(ConnectorTypeId.MYSQL)) {
    returnObj = {
      fieldArray: [
        {
          field: DatabaseFilter.NAME,
          valueSample: DatabaseFilter.LABEL,
          preview: true,
          excludeFilter: false,
        },
        {
          field: TableFilter.NAME,
          valueSample: `${DatabaseFilter.LABEL}.${TableFilter.LABEL}`,
          preview: true,
          excludeFilter: false,
        },
        {
          field: ColumnFilter.NAME,
          valueSample: `${DatabaseFilter.LABEL}.${TableFilter.LABEL}.${ColumnFilter.LABEL}`,
          preview: false,
          excludeFilter: false,
        },
      ],
    };
  } else {
    returnObj = {
      fieldArray: [
        {
          field: SchemaFilter.NAME,
          valueSample: SchemaFilter.LABEL,
          preview: true,
          excludeFilter: false,
        },
        {
          field: TableFilter.NAME,
          valueSample: `${SchemaFilter.LABEL}.${TableFilter.LABEL}`,
          preview: true,
          excludeFilter: false,
        },
        {
          field: ColumnFilter.NAME,
          valueSample: `${SchemaFilter.LABEL}.${TableFilter.LABEL}.${ColumnFilter.LABEL}`,
          preview: false,
          excludeFilter: false,
        },
      ],
    };
  }
  return returnObj;
}

export function mapToObject(inputMap: Map<string, string>): {
  key: string;
  value: any;
} {
  const obj = {} as { key: string; value: any };
  inputMap.forEach((value, key) => {
    obj[key] = value;
  });
  return obj;
}

export function maskPropertyValues(inputObj: { key: string; value: any }) {
  for (const [key, value] of Object.entries(inputObj)) {
    if (key.includes('password') && !!inputObj[key]) {
      inputObj[key] = '*'.repeat(value.length);
    }
  }
  return inputObj;
}

/**
 * Wrapper function to call the underline api call repetitively upto MAX_RETRIES limit in case of error
 * @param api function fetching the api
 * @param serviceRef reference of service type on which to call the api function
 * @param postParam param for post API call of type Array containing element in same order as need to passed in funcion
 * @param retries no. of retries
 */
export function fetch_retry(
  api: any,
  serviceRef: any,
  postParam?: any,
  retries: number = 1
): Promise<any> {
  const apicall = api.bind(serviceRef);
  // For Get method
  if (postParam === undefined) {
    return apicall().catch((err: any) => {
      if (retries >= MAX_RETRIES) {
        throw err;
      }
      return fetch_retry(api, serviceRef, ++retries);
    });
  }
  // For Post method
  else {
    return apicall(...postParam).catch((err: any) => {
      if (retries >= MAX_RETRIES) {
        throw err;
      }
      return fetch_retry(api, serviceRef, postParam, ++retries);
    });
  }
}

export function checkForContradictingFilters(filters, connectorType): boolean {
  const filterFields = getFilterConfigurationPageContent(
    connectorType || ''
  ).fieldArray.flatMap((i) => i.field);
  const topFilter = filters.get(`${filterFields[0]}.exclude.list`);
  const middleFilter = filters.get(`${filterFields[1]}.exclude.list`);
  const middleIncludeFilter = filters.get(`${filterFields[1]}.include.list`);
  const bottomIncludeFilter = filters.get(`${filterFields[2]}.include.list`);
  if (topFilter) {
    if (middleIncludeFilter || bottomIncludeFilter) {
      return (
        middleIncludeFilter?.includes(topFilter) ||
        bottomIncludeFilter?.includes(topFilter)
      );
    }
  }
  if (middleFilter) {
    if (bottomIncludeFilter) {
      return bottomIncludeFilter.includes(middleFilter);
    }
  }
  return false;
}
