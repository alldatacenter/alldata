// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package config

type Config struct {
	Debug                  bool   `default:"false" desc:"enable debug logging"`
	CollectClusterInfo     bool   `default:"true" desc:"enable collect cluster info"`
	LeaderElection         bool   `default:"true" desc:"leader election"`
	LeaderElectionID       string `default:"cluster-agent.erda.cloud" desc:"leader election id"`
	LeasesResourceLockType string `default:"leases" desc:"leases resource lock type"`
	LeaseDuration          int    `desc:"lease duration"`
	RenewDeadline          int    `desc:"renew deadline"`
	RetryPeriod            int    `desc:"retry period"`
	ConRetryInterval       int    `desc:"agent connection retry interval"`
	ClusterManagerEndpoint string `desc:"cluster manager endpoint"`
	ClusterKey             string `desc:"cluster key"`
	ErdaNamespace          string `desc:"erda namespace"`
	ClusterAccessKey       string `desc:"cluster access key, if specified will doesn't start watcher"`
	K8SApiServerAddr       string `desc:"kube-apiserver address in cluster"`
}
