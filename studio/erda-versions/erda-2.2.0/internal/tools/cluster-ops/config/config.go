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
	Debug         bool   `env:"DEBUG" default:"false" desc:"enable debug logging"`
	RepoURL       string `env:"HELM_REPO_URL" desc:"helm repo url"`
	RepoUsername  string `env:"HELM_REPO_USERNAME" desc:"helm repo url"`
	RepoPassword  string `env:"HELM_REPO_PASSWORD" desc:"helm repo url"`
	Reinstall     bool   `env:"REINSTALL" default:"false" desc:"reinstall erda comp"`
	Version       string `env:"ERDA_CHART_VERSION" desc:"erda chart version"`
	SetValues     string `env:"ERDA_CHART_VALUES" desc:"provide erda values"`
	InstallMode   string `env:"INSTALL_MODE" default:"local" desc:"install mode, remote or local"`
	TargetCluster string `env:"TARGET_CLUSTER" desc:"special when CREDENTIAL_FROM=CLUSTER_MANAGER"`
	NodeLabels    string `env:"NODE_LABELS" desc:"node labels after install erda"`
	// HELM_NAMESPACE: helm deploy namespace
	// HELM_REPO_URL: helm repo address
	// HELM_REPOSITORY_CONFIG: helm repository store path
	// HELM_REPOSITORY_CACHE: helm charts cache path
}
