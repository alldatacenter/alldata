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
	// LeaderIDSuffix is the suffix of leader id used for components' leader election
	LeaderIDSuffix = "uniffle.apache.org"

	// RSSKind is the object kind of rss.
	RSSKind = "RemoteShuffleService"

	// PodNamespaceEnv is the name of environment variable indicates pod's namespace.
	PodNamespaceEnv = "POD_NAMESPACE"
	// DefaultNamespace is used when environment variable of PodNamespaceEnv is not set.
	DefaultNamespace = "kube-system"

	// AnnotationMetricsServerPort represents annotation of metrics servers' port.
	AnnotationMetricsServerPort = "uniffle.apache.org/metrics-server-port"
	// AnnotationShuffleServerPort represents annotation of port used to identify shuffle servers.
	AnnotationShuffleServerPort = "uniffle.apache.org/shuffle-server-port"
	// AnnotationRssName represents annotation of rss object name used by shuffle servers' pods.
	AnnotationRssName = "uniffle.apache.org/rss-name"
	// AnnotationRssUID represents annotation of rss object uid used by shuffle servers' pods.
	AnnotationRssUID = "uniffle.apache.org/rss-uid"

	// LabelCoordinator represents label of coordinators.
	LabelCoordinator = "uniffle.apache.org/coordinator"
	// LabelShuffleServer represents label of shuffle servers.
	LabelShuffleServer = "uniffle.apache.org/shuffle-server"

	// RSSFinalizerName represents finalizer name of rss objects.
	RSSFinalizerName = "WaitingShuffleServer"

	// RSSCoordinator represents the prefix or identifier of the coordinator.
	RSSCoordinator = "rss-coordinator"
	// RSSShuffleServer represents the prefix or identifier of the shuffle server.
	RSSShuffleServer = "rss-shuffle-server"

	// DefaultInitContainerImage represents default image of init container used to change owner of host paths.
	DefaultInitContainerImage = "busybox:latest"

	// ShuffleServerConfigKey represents shuffle server configuration key in configMap used by a rss object.
	ShuffleServerConfigKey = "server.conf"
	// CoordinatorConfigKey represents coordinator configuration key in configMap used by a rss object.
	CoordinatorConfigKey = "coordinator.conf"
	// Log4jPropertiesKey represents log4j properties key in configMap used by a rss object.
	Log4jPropertiesKey = "log4j.properties"
)
