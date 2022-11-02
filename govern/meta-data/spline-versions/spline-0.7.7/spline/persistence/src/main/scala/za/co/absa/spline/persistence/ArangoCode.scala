/*
 * Copyright 2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.persistence

/**
  * @see https://github.com/outr/scarango/blob/master/driver/src/main/scala/com/outr/arango/ArangoCode.scala
  */
object ArangoCode {
  private var codeMap = Map.empty[Int, ArangoCode]

  def apply(code: Int): ArangoCode = codeMap(code)

  // General errors
  val NoError = ArangoCode(0, "ERROR_NO_ERROR", "No error has occurred.")
  val Failed = ArangoCode(1, "ERROR_FAILED", "Will be raised when a general error occurred.")
  val SysError = ArangoCode(2, "ERROR_SYS_ERROR", "Will be raised when operating system error occurred.")
  val OutOfMemory = ArangoCode(3, "ERROR_OUT_OF_MEMORY", "Will be raised when there is a memory shortage.")
  val Internal = ArangoCode(4, "ERROR_INTERNAL", "Will be raised when an internal error occurred.")
  val IllegalNumber = ArangoCode(5, "ERROR_ILLEGAL_NUMBER", "Will be raised when an illegal representation of a number was given.")
  val NumericOverflow = ArangoCode(6, "ERROR_NUMERIC_OVERFLOW", "Will be raised when a numeric overflow occurred.")
  val IllegalOption = ArangoCode(7, "ERROR_ILLEGAL_OPTION", "Will be raised when an unknown option was supplied by the user.")
  val DeadPid = ArangoCode(8, "ERROR_DEAD_PID", "Will be raised when a PID without a living process was found.")
  val NotImplemented = ArangoCode(9, "ERROR_NOT_IMPLEMENTED", "Will be raised when hitting an unimplemented feature.")
  val BadParameter = ArangoCode(10, "ERROR_BAD_PARAMETER", "Will be raised when the parameter does not fulfill the requirements.")
  val Forbidden = ArangoCode(11, "ERROR_FORBIDDEN", "Will be raised when you are missing permission for the operation.")
  val OutOfMemoryMmap = ArangoCode(12, "ERROR_OUT_OF_MEMORY_MMAP", "Will be raised when there is a memory shortage.")
  val CorruptedCsv = ArangoCode(13, "ERROR_CORRUPTED_CSV", "Will be raised when encountering a corrupt csv line.")
  val FileNotFound = ArangoCode(14, "ERROR_FILE_NOT_FOUND", "Will be raised when a file is not found.")
  val CannotWriteFile = ArangoCode(15, "ERROR_CANNOT_WRITE_FILE", "Will be raised when a file cannot be written.")
  val CannotOverwriteFile = ArangoCode(16, "ERROR_CANNOT_OVERWRITE_FILE", "Will be raised when an attempt is made to overwrite an existing file.")
  val TypeError = ArangoCode(17, "ERROR_TYPE_ERROR", "Will be raised when a type error is unencountered.")
  val LockTimeout = ArangoCode(18, "ERROR_LOCK_TIMEOUT", "Will be raised when there's a timeout waiting for a lock.")
  val CannotCreateDirectory = ArangoCode(19, "ERROR_CANNOT_CREATE_DIRECTORY", "Will be raised when an attempt to create a directory fails.")
  val CannotCreateTempFile = ArangoCode(20, "ERROR_CANNOT_CREATE_TEMP_FILE", "Will be raised when an attempt to create a temporary file fails.")
  val RequestCanceled = ArangoCode(21, "ERROR_REQUEST_CANCELED", "Will be raised when a request is canceled by the user.")
  val Debug = ArangoCode(22, "ERROR_DEBUG", "Will be raised intentionally during debugging.")
  val IpAddressInvalid = ArangoCode(25, "ERROR_IP_ADDRESS_INVALID", "Will be raised when the structure of an IP address is invalid.")
  val FileExists = ArangoCode(27, "ERROR_FILE_EXISTS", "Will be raised when a file already exists.")
  val Locked = ArangoCode(28, "ERROR_LOCKED", "Will be raised when a resource or an operation is locked.")
  val Deadlock = ArangoCode(29, "ERROR_DEADLOCK", "Will be raised when a deadlock is detected when accessing collections.")
  val ShuttingDown = ArangoCode(30, "ERROR_SHUTTING_DOWN", "Will be raised when a call cannot succeed because a server shutdown is already in progress.")
  val OnlyEnterprise = ArangoCode(31, "ERROR_ONLY_ENTERPRISE", "Will be raised when an enterprise-feature is requested from the community edition.")
  val ResourceLimit = ArangoCode(32, "ERROR_RESOURCE_LIMIT", "Will be raised when the resources used by an operation exceed the configured maximum value.")

  // HTTP error status codes
  val HttpBadParameter = ArangoCode(400, "ERROR_HTTP_BAD_PARAMETER", "Will be raised when the HTTP request does not fulfill the requirements.")
  val HttpUnauthorized = ArangoCode(401, "ERROR_HTTP_UNAUTHORIZED", "Will be raised when authorization is required but the user is not authorized.")
  val HttpForbidden = ArangoCode(403, "ERROR_HTTP_FORBIDDEN", "Will be raised when the operation is forbidden.")
  val HttpNotFound = ArangoCode(404, "ERROR_HTTP_NOT_FOUND", "Will be raised when an URI is unknown.")
  val HttpMethodNotAllowed = ArangoCode(405, "ERROR_HTTP_METHOD_NOT_ALLOWED", "Will be raised when an unsupported HTTP method is used for an operation.")
  val HttpPreconditionFailed = ArangoCode(412, "ERROR_HTTP_PRECONDITION_FAILED", "Will be raised when a precondition for an HTTP request is not met.")
  val HttpServerError = ArangoCode(500, "ERROR_HTTP_SERVER_ERROR", "Will be raised when an internal server is encountered.")

  // HTTP processing errors
  val HttpCorruptedJson = ArangoCode(600, "ERROR_HTTP_CORRUPTED_JSON", "Will be raised when a string representation of a JSON object is corrupt.")
  val HttpSuperfluousSuffices = ArangoCode(601, "ERROR_HTTP_SUPERFLUOUS_SUFFICES", "Will be raised when the URL contains superfluous suffices.")

  // Internal ArangoDB storage errors
  // For errors that occur because of a programming error.
  val ArangoIllegalState = ArangoCode(1000, "ERROR_ARANGO_ILLEGAL_STATE", "Internal error that will be raised when the datafile is not in the required state.")
  val ArangoDatafileSealed = ArangoCode(1002, "ERROR_ARANGO_DATAFILE_SEALED", "Internal error that will be raised when trying to write to a datafile.")
  val ArangoReadOnly = ArangoCode(1004, "ERROR_ARANGO_READ_ONLY", "Internal error that will be raised when trying to write to a read-only datafile or collection.")
  val ArangoDuplicateIdentifier = ArangoCode(1005, "ERROR_ARANGO_DUPLICATE_IDENTIFIER", "Internal error that will be raised when a identifier duplicate is detected.")
  val ArangoDatafileUnreadable = ArangoCode(1006, "ERROR_ARANGO_DATAFILE_UNREADABLE", "Internal error that will be raised when a datafile is unreadable.")
  val ArangoDatafileEmpty = ArangoCode(1007, "ERROR_ARANGO_DATAFILE_EMPTY", "Internal error that will be raised when a datafile is empty.")
  val ArangoRecovery = ArangoCode(1008, "ERROR_ARANGO_RECOVERY", "Will be raised when an error occurred during WAL log file recovery.")
  val ArangoDatafileStatisticsNotFound = ArangoCode(1009, "ERROR_ARANGO_DATAFILE_STATISTICS_NOT_FOUND", "Will be raised when a required datafile statistics object was not found.")

  // External ArangoDB storage errors
  // For errors that occur because of an outside event.
  val ArangoCorruptedDatafile = ArangoCode(1100, "ERROR_ARANGO_CORRUPTED_DATAFILE", "Will be raised when a corruption is detected in a datafile.")
  val ArangoIllegalParameterFile = ArangoCode(1101, "ERROR_ARANGO_ILLEGAL_PARAMETER_FILE", "Will be raised if a parameter file is corrupted or cannot be read.")
  val ArangoCorruptedCollection = ArangoCode(1102, "ERROR_ARANGO_CORRUPTED_COLLECTION", "Will be raised when a collection contains one or more corrupted data files.")
  val ArangoMmapFailed = ArangoCode(1103, "ERROR_ARANGO_MMAP_FAILED", "Will be raised when the system call mmap failed.")
  val ArangoFilesystemFull = ArangoCode(1104, "ERROR_ARANGO_FILESYSTEM_FULL", "Will be raised when the filesystem is full.")
  val ArangoNoJournal = ArangoCode(1105, "ERROR_ARANGO_NO_JOURNAL", "Will be raised when a journal cannot be created.")
  val ArangoDatafileAlreadyExists = ArangoCode(1106, "ERROR_ARANGO_DATAFILE_ALREADY_EXISTS", "Will be raised when the datafile cannot be created or renamed because a file of the same name already exists.")
  val ArangoDatadirLocked = ArangoCode(1107, "ERROR_ARANGO_DATADIR_LOCKED", "Will be raised when the database directory is locked by a different process.")
  val ArangoCollectionDirectoryAlreadyExists = ArangoCode(1108, "ERROR_ARANGO_COLLECTION_DIRECTORY_ALREADY_EXISTS", "Will be raised when the collection cannot be created because a directory of the same name already exists.")
  val ArangoMsyncFailed = ArangoCode(1109, "ERROR_ARANGO_MSYNC_FAILED", "Will be raised when the system call msync failed.")
  val ArangoDatadirUnlockable = ArangoCode(1110, "ERROR_ARANGO_DATADIR_UNLOCKABLE", "Will be raised when the server cannot lock the database directory on startup.")
  val ArangoSyncTimeout = ArangoCode(1111, "ERROR_ARANGO_SYNC_TIMEOUT", "Will be raised when the server waited too long for a datafile to be synced to disk.")

  // General ArangoDB storage errors
  // For errors that occur when fulfilling a user request.
  val ArangoConflict = ArangoCode(1200, "ERROR_ARANGO_CONFLICT", "Will be raised when updating or deleting a document and a conflict has been detected.")
  val ArangoDatadirInvalid = ArangoCode(1201, "ERROR_ARANGO_DATADIR_INVALID", "Will be raised when a non-existing database directory was specified when starting the database.")
  val ArangoDocumentNotFound = ArangoCode(1202, "ERROR_ARANGO_DOCUMENT_NOT_FOUND", "Will be raised when a document with a given identifier or handle is unknown.")
  val ArangoCollectionNotFound = ArangoCode(1203, "ERROR_ARANGO_COLLECTION_NOT_FOUND", "Will be raised when a collection with the given identifier or name is unknown.")
  val ArangoCollectionParameterMissing = ArangoCode(1204, "ERROR_ARANGO_COLLECTION_PARAMETER_MISSING", "Will be raised when the collection parameter is missing.")
  val ArangoDocumentHandleBad = ArangoCode(1205, "ERROR_ARANGO_DOCUMENT_HANDLE_BAD", "Will be raised when a document handle is corrupt.")
  val ArangoMaximalSizeTooSmall = ArangoCode(1206, "ERROR_ARANGO_MAXIMAL_SIZE_TOO_SMALL", "Will be raised when the maximal size of the journal is too small.")
  val ArangoDuplicateName = ArangoCode(1207, "ERROR_ARANGO_DUPLICATE_NAME", "Will be raised when a name duplicate is detected.")
  val ArangoIllegalName = ArangoCode(1208, "ERROR_ARANGO_ILLEGAL_NAME", "Will be raised when an illegal name is detected.")
  val ArangoNoIndex = ArangoCode(1209, "ERROR_ARANGO_NO_INDEX", "Will be raised when no suitable index for the query is known.")
  val ArangoUniqueConstraintViolated = ArangoCode(1210, "ERROR_ARANGO_UNIQUE_CONSTRAINT_VIOLATED", "Will be raised when there is a unique constraint violation.")
  val ArangoViewNotFound = ArangoCode(1211, "ERROR_ARANGO_VIEW_NOT_FOUND", "Will be raised when a view with the given identifier or name is unknown.")
  val ArangoIndexNotFound = ArangoCode(1212, "ERROR_ARANGO_INDEX_NOT_FOUND", "Will be raised when an index with a given identifier is unknown.")
  val ArangoCrossCollectionRequest = ArangoCode(1213, "ERROR_ARANGO_CROSS_COLLECTION_REQUEST", "Will be raised when a cross-collection is requested.")
  val ArangoIndexHandleBad = ArangoCode(1214, "ERROR_ARANGO_INDEX_HANDLE_BAD", "Will be raised when a index handle is corrupt.")
  val ArangoDocumentTooLarge = ArangoCode(1216, "ERROR_ARANGO_DOCUMENT_TOO_LARGE", "Will be raised when the document cannot fit into any datafile because of it is too large.")
  val ArangoCollectionNotUnloaded = ArangoCode(1217, "ERROR_ARANGO_COLLECTION_NOT_UNLOADED", "Will be raised when a collection should be unloaded, but has a different status.")
  val ArangoCollectionTypeInvalid = ArangoCode(1218, "ERROR_ARANGO_COLLECTION_TYPE_INVALID", "Will be raised when an invalid collection type is used in a request.")
  val ArangoValidationFailed = ArangoCode(1219, "ERROR_ARANGO_VALIDATION_FAILED", "Will be raised when the validation of an attribute of a structure failed.")
  val ArangoAttributeParserFailed = ArangoCode(1220, "ERROR_ARANGO_ATTRIBUTE_PARSER_FAILED", "Will be raised when parsing an attribute name definition failed.")
  val ArangoDocumentKeyBad = ArangoCode(1221, "ERROR_ARANGO_DOCUMENT_KEY_BAD", "Will be raised when a document key is corrupt.")
  val ArangoDocumentKeyUnexpected = ArangoCode(1222, "ERROR_ARANGO_DOCUMENT_KEY_UNEXPECTED", "Will be raised when a user-defined document key is supplied for collections with auto key generation.")
  val ArangoDatadirNotWritable = ArangoCode(1224, "ERROR_ARANGO_DATADIR_NOT_WRITABLE", "Will be raised when the server's database directory is not writable for the current user.")
  val ArangoOutOfKeys = ArangoCode(1225, "ERROR_ARANGO_OUT_OF_KEYS", "Will be raised when a key generator runs out of keys.")
  val ArangoDocumentKeyMissing = ArangoCode(1226, "ERROR_ARANGO_DOCUMENT_KEY_MISSING", "Will be raised when a document key is missing.")
  val ArangoDocumentTypeInvalid = ArangoCode(1227, "ERROR_ARANGO_DOCUMENT_TYPE_INVALID", "Will be raised when there is an attempt to create a document with an invalid type.")
  val ArangoDatabaseNotFound = ArangoCode(1228, "ERROR_ARANGO_DATABASE_NOT_FOUND", "Will be raised when a non-existing database is accessed.")
  val ArangoDatabaseNameInvalid = ArangoCode(1229, "ERROR_ARANGO_DATABASE_NAME_INVALID", "Will be raised when an invalid database name is used.")
  val ArangoUseSystemDatabase = ArangoCode(1230, "ERROR_ARANGO_USE_SYSTEM_DATABASE", "Will be raised when an operation is requested in a database other than the system database.")
  val ArangoEndpointNotFound = ArangoCode(1231, "ERROR_ARANGO_ENDPOINT_NOT_FOUND", "Will be raised when there is an attempt to delete a non-existing endpoint.")
  val ArangoInvalidKeyGenerator = ArangoCode(1232, "ERROR_ARANGO_INVALID_KEY_GENERATOR", "Will be raised when an invalid key generator description is used.")
  val ArangoInvalidEdgeAttribute = ArangoCode(1233, "ERROR_ARANGO_INVALID_EDGE_ATTRIBUTE", "will be raised when the _from or _to values of an edge are undefined or contain an invalid value.")
  val ArangoIndexDocumentAttributeMissing = ArangoCode(1234, "ERROR_ARANGO_INDEX_DOCUMENT_ATTRIBUTE_MISSING", "Will be raised when an attempt to insert a document into an index is caused by in the document not having one or more attributes which the index is built on.")
  val ArangoIndexCreationFailed = ArangoCode(1235, "ERROR_ARANGO_INDEX_CREATION_FAILED", "Will be raised when an attempt to create an index has failed.")
  val ArangoWriteThrottleTimeout = ArangoCode(1236, "ERROR_ARANGO_WRITE_THROTTLE_TIMEOUT", "Will be raised when the server is write-throttled and a write operation has waited too long for the server to process queued operations.")
  val ArangoCollectionTypeMismatch = ArangoCode(1237, "ERROR_ARANGO_COLLECTION_TYPE_MISMATCH", "Will be raised when a collection has a different type from what has been expected.")
  val ArangoCollectionNotLoaded = ArangoCode(1238, "ERROR_ARANGO_COLLECTION_NOT_LOADED", "Will be raised when a collection is accessed that is not yet loaded.")
  val ArangoDocumentRevBad = ArangoCode(1239, "ERROR_ARANGO_DOCUMENT_REV_BAD", "Will be raised when a document revision is corrupt or is missing where needed.")

  // Checked ArangoDB storage errors
  // For errors that occur but are anticipated.
  val ArangoDatafileFull = ArangoCode(1300, "ERROR_ARANGO_DATAFILE_FULL", "Will be raised when the datafile reaches its limit.")
  val ArangoEmptyDatadir = ArangoCode(1301, "ERROR_ARANGO_EMPTY_DATADIR", "Will be raised when encountering an empty server database directory.")

  // ArangoDB replication errors
  val ReplicationNoResponse = ArangoCode(1400, "ERROR_REPLICATION_NO_RESPONSE", "Will be raised when the replication applier does not receive any or an incomplete response from the master.")
  val ReplicationInvalidResponse = ArangoCode(1401, "ERROR_REPLICATION_INVALID_RESPONSE", "Will be raised when the replication applier receives an invalid response from the master.")
  val ReplicationMasterError = ArangoCode(1402, "ERROR_REPLICATION_MASTER_ERROR", "Will be raised when the replication applier receives a server error from the master.")
  val ReplicationMasterIncompatible = ArangoCode(1403, "ERROR_REPLICATION_MASTER_INCOMPATIBLE", "Will be raised when the replication applier connects to a master that has an incompatible version.")
  val ReplicationMasterChange = ArangoCode(1404, "ERROR_REPLICATION_MASTER_CHANGE", "Will be raised when the replication applier connects to a different master than before.")
  val ReplicationLoop = ArangoCode(1405, "ERROR_REPLICATION_LOOP", "Will be raised when the replication applier is asked to connect to itself for replication.")
  val ReplicationUnexpectedMarker = ArangoCode(1406, "ERROR_REPLICATION_UNEXPECTED_MARKER", "Will be raised when an unexpected marker is found in the replication log stream.")
  val ReplicationInvalidApplierState = ArangoCode(1407, "ERROR_REPLICATION_INVALID_APPLIER_STATE", "Will be raised when an invalid replication applier state file is found.")
  val ReplicationUnexpectedTransaction = ArangoCode(1408, "ERROR_REPLICATION_UNEXPECTED_TRANSACTION", "Will be raised when an unexpected transaction id is found.")
  val ReplicationInvalidApplierConfiguration = ArangoCode(1410, "ERROR_REPLICATION_INVALID_APPLIER_CONFIGURATION", "Will be raised when the configuration for the replication applier is invalid.")
  val ReplicationRunning = ArangoCode(1411, "ERROR_REPLICATION_RUNNING", "Will be raised when there is an attempt to perform an operation while the replication applier is running.")
  val ReplicationApplierStopped = ArangoCode(1412, "ERROR_REPLICATION_APPLIER_STOPPED", "Special error code used to indicate the replication applier was stopped by a user.")
  val ReplicationNoStartTick = ArangoCode(1413, "ERROR_REPLICATION_NO_START_TICK", "Will be raised when the replication applier is started without a known start tick value.")
  val ReplicationStartTickNotPresent = ArangoCode(1414, "ERROR_REPLICATION_START_TICK_NOT_PRESENT", "Will be raised when the replication applier fetches data using a start tick, but that start tick is not present on the logger server anymore.")

  // ArangoDB cluster errors
  val ClusterNoAgency = ArangoCode(1450, "ERROR_CLUSTER_NO_AGENCY", "Will be raised when none of the agency servers can be connected to.")
  val ClusterNoCoordinatorHeader = ArangoCode(1451, "ERROR_CLUSTER_NO_COORDINATOR_HEADER", "Will be raised when a DB server in a cluster receives a HTTP request without a coordinator header.")
  val ClusterCouldNotLockPlan = ArangoCode(1452, "ERROR_CLUSTER_COULD_NOT_LOCK_PLAN", "Will be raised when a coordinator in a cluster cannot lock the Plan hierarchy in the agency.")
  val ClusterCollectionIdExists = ArangoCode(1453, "ERROR_CLUSTER_COLLECTION_ID_EXISTS", "Will be raised when a coordinator in a cluster tries to create a collection and the collection ID already exists.")
  val ClusterCouldNotCreateCollectionInPlan = ArangoCode(1454, "ERROR_CLUSTER_COULD_NOT_CREATE_COLLECTION_IN_PLAN", "Will be raised when a coordinator in a cluster cannot create an entry for a new collection in the Plan hierarchy in the agency.")
  val ClusterCouldNotReadCurrentVersion = ArangoCode(1455, "ERROR_CLUSTER_COULD_NOT_READ_CURRENT_VERSION", "Will be raised when a coordinator in a cluster cannot read the Version entry in the Current hierarchy in the agency.")
  val ClusterCouldNotCreateCollection = ArangoCode(1456, "ERROR_CLUSTER_COULD_NOT_CREATE_COLLECTION", "Will be raised when a coordinator in a cluster notices that some DBServers report problems when creating shards for a new collection.")
  val ClusterTimeout = ArangoCode(1457, "ERROR_CLUSTER_TIMEOUT", "Will be raised when a coordinator in a cluster runs into a timeout for some cluster wide operation.")
  val ClusterCouldNotRemoveCollectionInPlan = ArangoCode(1458, "ERROR_CLUSTER_COULD_NOT_REMOVE_COLLECTION_IN_PLAN", "Will be raised when a coordinator in a cluster cannot remove an entry for a collection in the Plan hierarchy in the agency.")
  val ClusterCouldNotRemoveCollectionInCurrent = ArangoCode(1459, "ERROR_CLUSTER_COULD_NOT_REMOVE_COLLECTION_IN_CURRENT", "Will be raised when a coordinator in a cluster cannot remove an entry for a collection in the Current hierarchy in the agency.")
  val ClusterCouldNotCreateDatabaseInPlan = ArangoCode(1460, "ERROR_CLUSTER_COULD_NOT_CREATE_DATABASE_IN_PLAN", "Will be raised when a coordinator in a cluster cannot create an entry for a new database in the Plan hierarchy in the agency.")
  val ClusterCouldNotCreateDatabase = ArangoCode(1461, "ERROR_CLUSTER_COULD_NOT_CREATE_DATABASE", "Will be raised when a coordinator in a cluster notices that some DBServers report problems when creating databases for a new cluster wide database.")
  val ClusterCouldNotRemoveDatabaseInPlan = ArangoCode(1462, "ERROR_CLUSTER_COULD_NOT_REMOVE_DATABASE_IN_PLAN", "Will be raised when a coordinator in a cluster cannot remove an entry for a database in the Plan hierarchy in the agency.")
  val ClusterCouldNotRemoveDatabaseInCurrent = ArangoCode(1463, "ERROR_CLUSTER_COULD_NOT_REMOVE_DATABASE_IN_CURRENT", "Will be raised when a coordinator in a cluster cannot remove an entry for a database in the Current hierarchy in the agency.")
  val ClusterShardGone = ArangoCode(1464, "ERROR_CLUSTER_SHARD_GONE", "Will be raised when a coordinator in a cluster cannot determine the shard that is responsible for a given document.")
  val ClusterConnectionLost = ArangoCode(1465, "ERROR_CLUSTER_CONNECTION_LOST", "Will be raised when a coordinator in a cluster loses an HTTP connection to a DBserver in the cluster whilst transferring data.")
  val ClusterMustNotSpecifyKey = ArangoCode(1466, "ERROR_CLUSTER_MUST_NOT_SPECIFY_KEY", "Will be raised when a coordinator in a cluster finds that the _key attribute was specified in a sharded collection the uses not only _key as sharding attribute.")
  val ClusterGotContradictingAnswers = ArangoCode(1467, "ERROR_CLUSTER_GOT_CONTRADICTING_ANSWERS", "Will be raised if a coordinator in a cluster gets conflicting results from different shards, which should never happen.")
  val ClusterNotAllShardingAttributesGiven = ArangoCode(1468, "ERROR_CLUSTER_NOT_ALL_SHARDING_ATTRIBUTES_GIVEN", "Will be raised if a coordinator tries to find out which shard is responsible for a partial document, but cannot do this because not all sharding attributes are specified.")
  val ClusterMustNotChangeShardingAttributes = ArangoCode(1469, "ERROR_CLUSTER_MUST_NOT_CHANGE_SHARDING_ATTRIBUTES", "Will be raised if there is an attempt to update the value of a shard attribute.")
  val ClusterUnsupported = ArangoCode(1470, "ERROR_CLUSTER_UNSUPPORTED", "Will be raised when there is an attempt to carry out an operation that is not supported in the context of a sharded collection.")
  val ClusterOnlyOnCoordinator = ArangoCode(1471, "ERROR_CLUSTER_ONLY_ON_COORDINATOR", "Will be raised if there is an attempt to run a coordinator-only operation on a different type of node.")
  val ClusterReadingPlanAgency = ArangoCode(1472, "ERROR_CLUSTER_READING_PLAN_AGENCY", "Will be raised if a coordinator or DBserver cannot read the Plan in the agency.")
  val ClusterCouldNotTruncateCollection = ArangoCode(1473, "ERROR_CLUSTER_COULD_NOT_TRUNCATE_COLLECTION", "Will be raised if a coordinator cannot truncate all shards of a cluster collection.")
  val ClusterAqlCommunication = ArangoCode(1474, "ERROR_CLUSTER_AQL_COMMUNICATION", "Will be raised if the internal communication of the cluster for AQL produces an error.")
  val ArangoDocumentNotFoundOrShardingAttributesChanged = ArangoCode(1475, "ERROR_ARANGO_DOCUMENT_NOT_FOUND_OR_SHARDING_ATTRIBUTES_CHANGED", "Will be raised when a document with a given identifier or handle is unknown, or if the sharding attributes have been changed in a REPLACE operation in the cluster.")
  val ClusterCouldNotDetermineId = ArangoCode(1476, "ERROR_CLUSTER_COULD_NOT_DETERMINE_ID", "Will be raised if a cluster server at startup could not determine its own ID from the local info provided.")
  val ClusterOnlyOnDbserver = ArangoCode(1477, "ERROR_CLUSTER_ONLY_ON_DBSERVER", "Will be raised if there is an attempt to run a DBserver-only operation on a different type of node.")
  val ClusterBackendUnavailable = ArangoCode(1478, "ERROR_CLUSTER_BACKEND_UNAVAILABLE", "Will be raised if a required db server can't be reached.")
  val ClusterUnknownCallbackEndpoint = ArangoCode(1479, "ERROR_CLUSTER_UNKNOWN_CALLBACK_ENDPOINT", "An endpoint couldn't be found")
  val ClusterAgencyStructureInvalid = ArangoCode(1480, "ERROR_CLUSTER_AGENCY_STRUCTURE_INVALID", "The structure in the agency is invalid")
  val ClusterAqlCollectionOutOfSync = ArangoCode(1481, "ERROR_CLUSTER_AQL_COLLECTION_OUT_OF_SYNC", "Will be raised if a collection needed during query execution is out of sync. This currently can only happen when using satellite collections")
  val ClusterCouldNotCreateIndexInPlan = ArangoCode(1482, "ERROR_CLUSTER_COULD_NOT_CREATE_INDEX_IN_PLAN", "Will be raised when a coordinator in a cluster cannot create an entry for a new index in the Plan hierarchy in the agency.")
  val ClusterCouldNotDropIndexInPlan = ArangoCode(1483, "ERROR_CLUSTER_COULD_NOT_DROP_INDEX_IN_PLAN", "Will be raised when a coordinator in a cluster cannot remove an index from the Plan hierarchy in the agency.")

  // ArangoDB query errors
  val QueryKilled = ArangoCode(1500, "ERROR_QUERY_KILLED", "Will be raised when a running query is killed by an explicit admin command.")
  val QueryParse = ArangoCode(1501, "ERROR_QUERY_PARSE", "Will be raised when query is parsed and is found to be syntactically invalid.")
  val QueryEmpty = ArangoCode(1502, "ERROR_QUERY_EMPTY", "Will be raised when an empty query is specified.")
  val QueryScript = ArangoCode(1503, "ERROR_QUERY_SCRIPT", "Will be raised when a runtime error is caused by the query.")
  val QueryNumberOutOfRange = ArangoCode(1504, "ERROR_QUERY_NUMBER_OUT_OF_RANGE", "Will be raised when a number is outside the expected range.")
  val QueryVariableNameInvalid = ArangoCode(1510, "ERROR_QUERY_VARIABLE_NAME_INVALID", "Will be raised when an invalid variable name is used.")
  val QueryVariableRedeclared = ArangoCode(1511, "ERROR_QUERY_VARIABLE_REDECLARED", "Will be raised when a variable gets re-assigned in a query.")
  val QueryVariableNameUnknown = ArangoCode(1512, "ERROR_QUERY_VARIABLE_NAME_UNKNOWN", "Will be raised when an unknown variable is used or the variable is undefined the context it is used.")
  val QueryCollectionLockFailed = ArangoCode(1521, "ERROR_QUERY_COLLECTION_LOCK_FAILED", "Will be raised when a read lock on the collection cannot be acquired.")
  val QueryTooManyCollections = ArangoCode(1522, "ERROR_QUERY_TOO_MANY_COLLECTIONS", "Will be raised when the number of collections in a query is beyond the allowed value.")
  val QueryDocumentAttributeRedeclared = ArangoCode(1530, "ERROR_QUERY_DOCUMENT_ATTRIBUTE_REDECLARED", "Will be raised when a document attribute is re-assigned.")
  val QueryFunctionNameUnknown = ArangoCode(1540, "ERROR_QUERY_FUNCTION_NAME_UNKNOWN", "Will be raised when an undefined function is called.")
  val QueryFunctionArgumentNumberMismatch = ArangoCode(1541, "ERROR_QUERY_FUNCTION_ARGUMENT_NUMBER_MISMATCH", "Will be raised when the number of arguments used in a function call does not match the expected number of arguments for the function.")
  val QueryFunctionArgumentTypeMismatch = ArangoCode(1542, "ERROR_QUERY_FUNCTION_ARGUMENT_TYPE_MISMATCH", "Will be raised when the type of an argument used in a function call does not match the expected argument type.")
  val QueryInvalidRegex = ArangoCode(1543, "ERROR_QUERY_INVALID_REGEX", "Will be raised when an invalid regex argument value is used in a call to a function that expects a regex.")
  val QueryBindParametersInvalid = ArangoCode(1550, "ERROR_QUERY_BIND_PARAMETERS_INVALID", "Will be raised when the structure of bind parameters passed has an unexpected format.")
  val QueryBindParameterMissing = ArangoCode(1551, "ERROR_QUERY_BIND_PARAMETER_MISSING", "Will be raised when a bind parameter was declared in the query but the query is being executed with no value for that parameter.")
  val QueryBindParameterUndeclared = ArangoCode(1552, "ERROR_QUERY_BIND_PARAMETER_UNDECLARED", "Will be raised when a value gets specified for an undeclared bind parameter.")
  val QueryBindParameterType = ArangoCode(1553, "ERROR_QUERY_BIND_PARAMETER_TYPE", "Will be raised when a bind parameter has an invalid value or type.")
  val QueryInvalidLogicalValue = ArangoCode(1560, "ERROR_QUERY_INVALID_LOGICAL_VALUE", "Will be raised when a non-boolean value is used in a logical operation.")
  val QueryInvalidArithmeticValue = ArangoCode(1561, "ERROR_QUERY_INVALID_ARITHMETIC_VALUE", "Will be raised when a non-numeric value is used in an arithmetic operation.")
  val QueryDivisionByZero = ArangoCode(1562, "ERROR_QUERY_DIVISION_BY_ZERO", "Will be raised when there is an attempt to divide by zero.")
  val QueryArrayExpected = ArangoCode(1563, "ERROR_QUERY_ARRAY_EXPECTED", "Will be raised when a non-array operand is used for an operation that expects an array argument operand.")
  val QueryFailCalled = ArangoCode(1569, "ERROR_QUERY_FAIL_CALLED", "Will be raised when the function FAIL() is called from inside a query.")
  val QueryGeoIndexMissing = ArangoCode(1570, "ERROR_QUERY_GEO_INDEX_MISSING", "Will be raised when a geo restriction was specified but no suitable geo index is found to resolve it.")
  val QueryFulltextIndexMissing = ArangoCode(1571, "ERROR_QUERY_FULLTEXT_INDEX_MISSING", "Will be raised when a fulltext query is performed on a collection without a suitable fulltext index.")
  val QueryInvalidDateValue = ArangoCode(1572, "ERROR_QUERY_INVALID_DATE_VALUE", "Will be raised when a value cannot be converted to a date.")
  val QueryMultiModify = ArangoCode(1573, "ERROR_QUERY_MULTI_MODIFY", "Will be raised when an AQL query contains more than one data-modifying operation.")
  val QueryInvalidAggregateExpression = ArangoCode(1574, "ERROR_QUERY_INVALID_AGGREGATE_EXPRESSION", "Will be raised when an AQL query contains an invalid aggregate expression.")
  val QueryCompileTimeOptions = ArangoCode(1575, "ERROR_QUERY_COMPILE_TIME_OPTIONS", "Will be raised when an AQL data-modification query contains options that cannot be figured out at query compile time.")
  val QueryExceptionOptions = ArangoCode(1576, "ERROR_QUERY_EXCEPTION_OPTIONS", "Will be raised when an AQL data-modification query contains an invalid options specification.")
  val QueryCollectionUsedInExpression = ArangoCode(1577, "ERROR_QUERY_COLLECTION_USED_IN_EXPRESSION", "Will be raised when a collection is used as an operand in an AQL expression.")
  val QueryDisallowedDynamicCall = ArangoCode(1578, "ERROR_QUERY_DISALLOWED_DYNAMIC_CALL", "Will be raised when a dynamic function call is made to a function that cannot be called dynamically.")
  val QueryAccessAfterModification = ArangoCode(1579, "ERROR_QUERY_ACCESS_AFTER_MODIFICATION", "Will be raised when collection data are accessed after a data-modification operation.")

  // AQL user function errors
  val QueryFunctionInvalidName = ArangoCode(1580, "ERROR_QUERY_FUNCTION_INVALID_NAME", "Will be raised when a user function with an invalid name is registered.")
  val QueryFunctionInvalidCode = ArangoCode(1581, "ERROR_QUERY_FUNCTION_INVALID_CODE", "Will be raised when a user function is registered with invalid code.")
  val QueryFunctionNotFound = ArangoCode(1582, "ERROR_QUERY_FUNCTION_NOT_FOUND", "Will be raised when a user function is accessed but not found.")
  val QueryFunctionRuntimeError = ArangoCode(1583, "ERROR_QUERY_FUNCTION_RUNTIME_ERROR", "Will be raised when a user function throws a runtime exception.")

  // AQL query registry errors
  val QueryBadJsonPlan = ArangoCode(1590, "ERROR_QUERY_BAD_JSON_PLAN", "Will be raised when an HTTP API for a query got an invalid JSON object.")
  val QueryNotFound = ArangoCode(1591, "ERROR_QUERY_NOT_FOUND", "Will be raised when an Id of a query is not found by the HTTP API.")
  val QueryInUse = ArangoCode(1592, "ERROR_QUERY_IN_USE", "Will be raised when an Id of a query is found by the HTTP API but the query is in use.")

  // ArangoDB cursor errors
  val CursorNotFound = ArangoCode(1600, "ERROR_CURSOR_NOT_FOUND", "Will be raised when a cursor is requested via its id but a cursor with that id cannot be found.")
  val CursorBusy = ArangoCode(1601, "ERROR_CURSOR_BUSY", "Will be raised when a cursor is requested via its id but a concurrent request is still using the cursor.")

  // ArangoDB transaction errors
  val TransactionInternal = ArangoCode(1650, "ERROR_TRANSACTION_INTERNAL", "Will be raised when a wrong usage of transactions is detected. this is an internal error and indicates a bug in ArangoDB.")
  val TransactionNested = ArangoCode(1651, "ERROR_TRANSACTION_NESTED", "Will be raised when transactions are nested.")
  val TransactionUnregisteredCollection = ArangoCode(1652, "ERROR_TRANSACTION_UNREGISTERED_COLLECTION", "Will be raised when a collection is used in the middle of a transaction but was not registered at transaction start.")
  val TransactionDisallowedOperation = ArangoCode(1653, "ERROR_TRANSACTION_DISALLOWED_OPERATION", "Will be raised when a disallowed operation is carried out in a transaction.")
  val TransactionAborted = ArangoCode(1654, "ERROR_TRANSACTION_ABORTED", "Will be raised when a transaction was aborted.")

  // User management errors
  val UserInvalidName = ArangoCode(1700, "ERROR_USER_INVALID_NAME", "Will be raised when an invalid user name is used.")
  val UserInvalidPassword = ArangoCode(1701, "ERROR_USER_INVALID_PASSWORD", "Will be raised when an invalid password is used.")
  val UserDuplicate = ArangoCode(1702, "ERROR_USER_DUPLICATE", "Will be raised when a user name already exists.")
  val UserNotFound = ArangoCode(1703, "ERROR_USER_NOT_FOUND", "Will be raised when a user name is updated that does not exist.")
  val UserChangePassword = ArangoCode(1704, "ERROR_USER_CHANGE_PASSWORD", "Will be raised when the user must change his password.")

  // Service management errors (legacy)
  // These have been superceded by the Foxx management errors in public APIs.
  val ServiceInvalidName = ArangoCode(1750, "ERROR_SERVICE_INVALID_NAME", "Will be raised when an invalid service name is specified.")
  val ServiceInvalidMount = ArangoCode(1751, "ERROR_SERVICE_INVALID_MOUNT", "Will be raised when an invalid mount is specified.")
  val ServiceDownloadFailed = ArangoCode(1752, "ERROR_SERVICE_DOWNLOAD_FAILED", "Will be raised when a service download from the central repository failed.")
  val ServiceUploadFailed = ArangoCode(1753, "ERROR_SERVICE_UPLOAD_FAILED", "Will be raised when a service upload from the client to the ArangoDB server failed.")

  // Task errors
  val TaskInvalidId = ArangoCode(1850, "ERROR_TASK_INVALID_ID", "Will be raised when a task is created with an invalid id.")
  val TaskDuplicateId = ArangoCode(1851, "ERROR_TASK_DUPLICATE_ID", "Will be raised when a task id is created with a duplicate id.")
  val TaskNotFound = ArangoCode(1852, "ERROR_TASK_NOT_FOUND", "Will be raised when a task with the specified id could not be found.")

  // Graph / traversal errors
  val GraphInvalidGraph = ArangoCode(1901, "ERROR_GRAPH_INVALID_GRAPH", "Will be raised when an invalid name is passed to the server.")
  val GraphCouldNotCreateGraph = ArangoCode(1902, "ERROR_GRAPH_COULD_NOT_CREATE_GRAPH", "Will be raised when an invalid name, vertices or edges is passed to the server.")
  val GraphInvalidVertex = ArangoCode(1903, "ERROR_GRAPH_INVALID_VERTEX", "Will be raised when an invalid vertex id is passed to the server.")
  val GraphCouldNotCreateVertex = ArangoCode(1904, "ERROR_GRAPH_COULD_NOT_CREATE_VERTEX", "Will be raised when the vertex could not be created.")
  val GraphCouldNotChangeVertex = ArangoCode(1905, "ERROR_GRAPH_COULD_NOT_CHANGE_VERTEX", "Will be raised when the vertex could not be changed.")
  val GraphInvalidEdge = ArangoCode(1906, "ERROR_GRAPH_INVALID_EDGE", "Will be raised when an invalid edge id is passed to the server.")
  val GraphCouldNotCreateEdge = ArangoCode(1907, "ERROR_GRAPH_COULD_NOT_CREATE_EDGE", "Will be raised when the edge could not be created.")
  val GraphCouldNotChangeEdge = ArangoCode(1908, "ERROR_GRAPH_COULD_NOT_CHANGE_EDGE", "Will be raised when the edge could not be changed.")
  val GraphTooManyIterations = ArangoCode(1909, "ERROR_GRAPH_TOO_MANY_ITERATIONS", "Will be raised when too many iterations are done in a graph traversal.")
  val GraphInvalidFilterResult = ArangoCode(1910, "ERROR_GRAPH_INVALID_FILTER_RESULT", "Will be raised when an invalid filter result is returned in a graph traversal.")
  val GraphCollectionMultiUse = ArangoCode(1920, "ERROR_GRAPH_COLLECTION_MULTI_USE", "an edge collection may only be used once in one edge definition of a graph.,")
  val GraphCollectionUseInMultiGraphs = ArangoCode(1921, "ERROR_GRAPH_COLLECTION_USE_IN_MULTI_GRAPHS", "is already used by another graph in a different edge definition.,")
  val GraphCreateMissingName = ArangoCode(1922, "ERROR_GRAPH_CREATE_MISSING_NAME", "a graph name is required to create a graph.,")
  val GraphCreateMalformedEdgeDefinition = ArangoCode(1923, "ERROR_GRAPH_CREATE_MALFORMED_EDGE_DEFINITION", "the edge definition is malformed. It has to be an array of objects.,")
  val GraphNotFound = ArangoCode(1924, "ERROR_GRAPH_NOT_FOUND", "a graph with this name could not be found.,")
  val GraphDuplicate = ArangoCode(1925, "ERROR_GRAPH_DUPLICATE", "a graph with this name already exists.,")
  val GraphVertexColDoesNotExist = ArangoCode(1926, "ERROR_GRAPH_VERTEX_COL_DOES_NOT_EXIST", "the specified vertex collection does not exist or is not part of the graph.,")
  val GraphWrongCollectionTypeVertex = ArangoCode(1927, "ERROR_GRAPH_WRONG_COLLECTION_TYPE_VERTEX", "the collection is not a vertex collection.,")
  val GraphNotInOrphanCollection = ArangoCode(1928, "ERROR_GRAPH_NOT_IN_ORPHAN_COLLECTION", "Vertex collection not in orphan collection of the graph.,")
  val GraphCollectionUsedInEdgeDef = ArangoCode(1929, "ERROR_GRAPH_COLLECTION_USED_IN_EDGE_DEF", "The collection is already used in an edge definition of the graph.,")
  val GraphEdgeCollectionNotUsed = ArangoCode(1930, "ERROR_GRAPH_EDGE_COLLECTION_NOT_USED", "The edge collection is not used in any edge definition of the graph.,")
  val GraphNotAnArangoCollection = ArangoCode(1931, "ERROR_GRAPH_NOT_AN_ARANGO_COLLECTION", "The collection is not an ArangoCollection.,")
  val GraphNoGraphCollection = ArangoCode(1932, "ERROR_GRAPH_NO_GRAPH_COLLECTION", "collection _graphs does not exist.,")
  val GraphInvalidExampleArrayObjectString = ArangoCode(1933, "ERROR_GRAPH_INVALID_EXAMPLE_ARRAY_OBJECT_STRING", "Invalid example type. Has to be String, Array or Object.,")
  val GraphInvalidExampleArrayObject = ArangoCode(1934, "ERROR_GRAPH_INVALID_EXAMPLE_ARRAY_OBJECT", "Invalid example type. Has to be Array or Object.,")
  val GraphInvalidNumberOfArguments = ArangoCode(1935, "ERROR_GRAPH_INVALID_NUMBER_OF_ARGUMENTS", "Invalid number of arguments. Expected: ,")
  val GraphInvalidParameter = ArangoCode(1936, "ERROR_GRAPH_INVALID_PARAMETER", "Invalid parameter type.,")
  val GraphInvalidId = ArangoCode(1937, "ERROR_GRAPH_INVALID_ID", "Invalid id,")
  val GraphCollectionUsedInOrphans = ArangoCode(1938, "ERROR_GRAPH_COLLECTION_USED_IN_ORPHANS", "The collection is already used in the orphans of the graph.,")
  val GraphEdgeColDoesNotExist = ArangoCode(1939, "ERROR_GRAPH_EDGE_COL_DOES_NOT_EXIST", "the specified edge collection does not exist or is not part of the graph.,")
  val GraphEmpty = ArangoCode(1940, "ERROR_GRAPH_EMPTY", "The requested graph has no edge collections.")

  // Session errors
  val SessionUnknown = ArangoCode(1950, "ERROR_SESSION_UNKNOWN", "Will be raised when an invalid/unknown session id is passed to the server.")
  val SessionExpired = ArangoCode(1951, "ERROR_SESSION_EXPIRED", "Will be raised when a session is expired.")

  // Simple Client errors
  val ClientUnknownError = ArangoCode(2000, "SIMPLE_CLIENT_UNKNOWN_ERROR", "This error should not happen.")
  val ClientCouldNotConnect = ArangoCode(2001, "SIMPLE_CLIENT_COULD_NOT_CONNECT", "Will be raised when the client could not connect to the server.")
  val ClientCouldNotWrite = ArangoCode(2002, "SIMPLE_CLIENT_COULD_NOT_WRITE", "Will be raised when the client could not write data.")
  val ClientCouldNotRead = ArangoCode(2003, "SIMPLE_CLIENT_COULD_NOT_READ", "Will be raised when the client could not read data.")

  // Communicator errors
  val CommunicatorRequestAborted = ArangoCode(2100, "COMMUNICATOR_REQUEST_ABORTED", "Request was aborted.")

  // Foxx management errors
  val MalformedManifestFile = ArangoCode(3000, "ERROR_MALFORMED_MANIFEST_FILE", "The service manifest file is not well-formed JSON.")
  val InvalidServiceManifest = ArangoCode(3001, "ERROR_INVALID_SERVICE_MANIFEST", "The service manifest contains invalid values.")
  val InvalidFoxxOptions = ArangoCode(3004, "ERROR_INVALID_FOXX_OPTIONS", "The service options contain invalid values.")
  val InvalidMountpoint = ArangoCode(3007, "ERROR_INVALID_MOUNTPOINT", "The service mountpath contains invalid characters.")
  val ServiceNotFound = ArangoCode(3009, "ERROR_SERVICE_NOT_FOUND", "No service found at the given mountpath.")
  val ServiceNeedsConfiguration = ArangoCode(3010, "ERROR_SERVICE_NEEDS_CONFIGURATION", "The service is missing configuration or dependencies.")
  val ServiceMountpointConflict = ArangoCode(3011, "ERROR_SERVICE_MOUNTPOINT_CONFLICT", "A service already exists at the given mountpath.")
  val ServiceManifestNotFound = ArangoCode(3012, "ERROR_SERVICE_MANIFEST_NOT_FOUND", "The service directory does not contain a manifest file.")
  val ServiceOptionsMalformed = ArangoCode(3013, "ERROR_SERVICE_OPTIONS_MALFORMED", "The service options are not well-formed JSON.")
  val ServiceSourceNotFound = ArangoCode(3014, "ERROR_SERVICE_SOURCE_NOT_FOUND", "The source path does not match a file or directory.")
  val ServiceSourceError = ArangoCode(3015, "ERROR_SERVICE_SOURCE_ERROR", "The source path could not be resolved.")
  val ServiceUnknownScript = ArangoCode(3016, "ERROR_SERVICE_UNKNOWN_SCRIPT", "The service does not have a script with this name.")

  // JavaScript module loader errors
  val ModuleNotFound = ArangoCode(3100, "ERROR_MODULE_NOT_FOUND", "The module path could not be resolved.")
  val ModuleFailure = ArangoCode(3103, "ERROR_MODULE_FAILURE", "Failed to invoke the module in its context.")

  // Enterprise errors
  val NoSmartCollection = ArangoCode(4000, "ERROR_NO_SMART_COLLECTION", "The requested collection needs to be smart, but it ain't")
  val NoSmartGraphAttribute = ArangoCode(4001, "ERROR_NO_SMART_GRAPH_ATTRIBUTE", "The given document does not have the smart graph attribute set.")
  val CannotDropSmartCollection = ArangoCode(4002, "ERROR_CANNOT_DROP_SMART_COLLECTION", "This smart collection cannot be dropped, it dictates sharding in the graph.")
  val KeyMustBePrefixedWithSmartGraphAttribute = ArangoCode(4003, "ERROR_KEY_MUST_BE_PREFIXED_WITH_SMART_GRAPH_ATTRIBUTE", "In a smart vertex collection _key must be prefixed with the value of the smart graph attribute.")
  val IllegalSmartGraphAttribute = ArangoCode(4004, "ERROR_ILLEGAL_SMART_GRAPH_ATTRIBUTE", "The given smartGraph attribute is illegal and connot be used for sharding. All system attributes are forbidden.")

  // Agency errors
  val AgencyInformMustBeObject = ArangoCode(20011, "ERROR_AGENCY_INFORM_MUST_BE_OBJECT", "The inform message in the agency must be an object.")
  val AgencyInformMustContainTerm = ArangoCode(20012, "ERROR_AGENCY_INFORM_MUST_CONTAIN_TERM", "The inform message in the agency must contain a uint parameter 'term'.")
  val AgencyInformMustContainId = ArangoCode(20013, "ERROR_AGENCY_INFORM_MUST_CONTAIN_ID", "The inform message in the agency must contain a string parameter 'id'.")
  val AgencyInformMustContainActive = ArangoCode(20014, "ERROR_AGENCY_INFORM_MUST_CONTAIN_ACTIVE", "The inform message in the agency must contain an array 'active'.")
  val AgencyInformMustContainPool = ArangoCode(20015, "ERROR_AGENCY_INFORM_MUST_CONTAIN_POOL", "The inform message in the agency must contain an object 'pool'.")
  val AgencyInquireClientIdMustBeString = ArangoCode(20020, "ERROR_AGENCY_INQUIRE_CLIENT_ID_MUST_BE_STRING", "Inquiry by clientId failed")

  // Dispatcher errors
  val DispatcherIsStopping = ArangoCode(21001, "ERROR_DISPATCHER_IS_STOPPING", "Will be returned if a shutdown is in progress.")
  val QueueUnknown = ArangoCode(21002, "ERROR_QUEUE_UNKNOWN", "Will be returned if a queue with this name does not exist.")
  val QueueFull = ArangoCode(21003, "ERROR_QUEUE_FULL", "Will be returned if a queue with this name is full.")
}

case class ArangoCode private(code: Int, title: String, message: String) {
  ArangoCode.codeMap += code -> this

  override def equals(obj: scala.Any): Boolean = obj match {
    case code: ArangoCode => code.code == this.code
    case _ => false
  }

  override def toString: String = s"code: $code, title: $title, message: $message"
}
