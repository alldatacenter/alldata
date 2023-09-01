/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package constants

const (
	// ContainerCoordinatorRPCPort indicates rpc port used in coordinator containers.
	ContainerCoordinatorRPCPort int32 = 19997
	// ContainerCoordinatorHTTPPort indicates http port used in coordinator containers.
	ContainerCoordinatorHTTPPort int32 = 19996

	// ShuffleServerRPCPortEnv indicates environment name of rpc port used by shuffle servers.
	ShuffleServerRPCPortEnv = "SERVER_RPC_PORT"
	// ShuffleServerHTTPPortEnv indicates environment name of http port used by shuffle servers.
	ShuffleServerHTTPPortEnv = "SERVER_HTTP_PORT"
	// CoordinatorRPCPortEnv indicates environment name of rpc port used by coordinators.
	CoordinatorRPCPortEnv = "COORDINATOR_RPC_PORT"
	// CoordinatorHTTPPortEnv indicates environment name of http port used by coordinators.
	CoordinatorHTTPPortEnv = "COORDINATOR_HTTP_PORT"
	// RSSCoordinatorQuorumEnv indicates environment name of rss coordinator quorum used by shuffle servers.
	RSSCoordinatorQuorumEnv = "RSS_COORDINATOR_QUORUM"
	// XmxSizeEnv indicates environment name of xmx size used by coordinators or shuffle servers.
	XmxSizeEnv = "XMX_SIZE"
	// ServiceNameEnv indicates environment name of service name used by coordinators or shuffle servers.
	ServiceNameEnv = "SERVICE_NAME"
	// NodeNameEnv indicates environment name of physical node name used by coordinators or shuffle servers.
	NodeNameEnv = "NODE_NAME"
	// RssIPEnv indicates environment name of shuffle servers' ip addresses.
	RssIPEnv = "RSS_IP"

	// CoordinatorServiceName defines environment variable value of "SERVICE_NAME" used by coordinators.
	CoordinatorServiceName = "coordinator"
	// ShuffleServerServiceName defines environment variable value of "SERVICE_NAME" used by shuffle servers.
	ShuffleServerServiceName = "server"

	// ExcludeNodesFile indicates volume mounting name of exclude nodes file
	ExcludeNodesFile = "exclude-nodes-file"

	// UpdateStatusError means reason of updating status of rss error
	UpdateStatusError = "UpdateStatusError"

	// OwnerLabel is the label of configMap's owner.
	OwnerLabel = "uniffle.apache.org/owner-label"

	// ConfigurationVolumeName is the name of configMap volume records configuration of coordinators or shuffle servers.
	ConfigurationVolumeName = "configuration"
)

// PropertyKey defines property key in configuration of coordinators or shuffle servers.
type PropertyKey string

const (
	// RPCServerPort represent rss port property in configuration of coordinators or shuffle servers.
	RPCServerPort PropertyKey = "rss.rpc.server.port"
	// JettyHTTPPort represent http port property in configuration of coordinators or shuffle servers.
	JettyHTTPPort PropertyKey = "rss.jetty.http.port"

	// CoordinatorQuorum represent coordinator quorum property in configuration of shuffle servers.
	CoordinatorQuorum PropertyKey = "rss.coordinator.quorum"
	// StorageBasePath represent storage base path property in configuration of shuffle servers.
	StorageBasePath PropertyKey = "rss.storage.basePath"

	// CoordinatorExcludeNodesPath represent exclude nodes path property in configuration of coordinators.
	CoordinatorExcludeNodesPath PropertyKey = "rss.coordinator.exclude.nodes.file.path"
)
