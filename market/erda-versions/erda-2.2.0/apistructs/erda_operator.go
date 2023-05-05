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

package apistructs

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const (
	ClusterPhaseNone     ClusterPhase = ""
	ClusterPhaseInitJobs ClusterPhase = "InitJobs"
	ClusterPhaseCreating ClusterPhase = "Creating"
	ClusterPhaseUpdating ClusterPhase = "Updating"
	ClusterPhaseRunning  ClusterPhase = "Running"
	ClusterPhaseFailed   ClusterPhase = "Failed"
	ClusterPhasePending  ClusterPhase = "Pending"
)

type ClusterPhase string
type ComponentStatus string
type ClusterSize string

type DiceClusterList struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Items             []DiceCluster `json:"items"`
}

type DiceCluster struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              ClusterSpec   `json:"spec"`
	Status            ClusterStatus `json:"status"`
}

type ClusterSpec struct {
	ResetStatus          bool        `json:"resetStatus"`
	AddonConfigMap       string      `json:"addonConfigMap"`
	ClusterinfoConfigMap string      `json:"clusterinfoConfigMap"`
	PlatformDomain       string      `json:"platformDomain"`
	CookieDomain         string      `json:"cookieDomain"`
	Size                 ClusterSize `json:"size"`
	DiceCluster          string      `json:"diceCluster"`
	// collector, openapi
	MainPlatform map[string]string `json:"mainPlatform"`
	// key: dice-service-name(e.g. ui), value: domain
	// customDomain:
	//   ui: dice.terminus.io,*.terminus.io
	CustomDomain map[string]string `json:"customDomain"`
	// deployment affinity labels for specific dice-service
	// key: dice-service-name(e.g. gittar), value: label
	// e.g.
	// gittar: dice/gittar
	CustomAffinity map[string]string `json:"customAffinity"`

	InitJobs diceyml.Object `json:"initJobs"`

	Dice           diceyml.Object `json:"dice"`
	AddonPlatform  diceyml.Object `json:"addonPlatform"`
	Gittar         diceyml.Object `json:"gittar"`
	Pandora        diceyml.Object `json:"pandora"`
	DiceUI         diceyml.Object `json:"diceUI"`
	UC             diceyml.Object `json:"uc"`
	SpotAnalyzer   diceyml.Object `json:"spotAnalyzer"`
	SpotCollector  diceyml.Object `json:"spotCollector"`
	SpotDashboard  diceyml.Object `json:"spotDashboard"`
	SpotFilebeat   diceyml.Object `json:"spotFilebeat"`
	SpotStatus     diceyml.Object `json:"spotStatus"`
	SpotTelegraf   diceyml.Object `json:"spotTelegraf"`
	Tmc            diceyml.Object `json:"tmc"`
	Hepa           diceyml.Object `json:"hepa"`
	SpotMonitor    diceyml.Object `json:"spotMonitor"`
	Fdp            diceyml.Object `json:"fdp"`
	MeshController diceyml.Object `json:"meshController"`
}
type ClusterStatus struct {
	Phase      ClusterPhase               `json:"phase"`
	Conditions []ErdaCondition            `json:"conditions"`
	Components map[string]ComponentStatus `json:"components"`
}

type ErdaCondition struct {
	Reason         string `json:"reason"`
	TransitionTime string `json:"transitionTime"`
}
