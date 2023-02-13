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

import corev1 "k8s.io/api/core/v1"

type FlinkKind string

const (
	FlinkJob     FlinkKind = "FlinkJob"
	FlinkSession FlinkKind = "FlinkSession"
)

type BigdataResource struct {
	CPU     string `json:"cpu"`
	Memory  string `json:"memory"`
	Replica int32  `json:"replica"`
}

type BigdataMetadata struct {
	Name      string `json:"name"`
	Namespace string `json:"namespace"`
}

type BigdataSpec struct {
	Image      string            `json:"image,omitempty"`
	Resource   string            `json:"resource,omitempty"`
	Class      string            `json:"class,omitempty"`
	Args       []string          `json:"args,omitempty"`
	Envs       []corev1.EnvVar   `json:"envs,omitempty"`
	Properties map[string]string `json:"properties,omitempty"`
	FlinkConf  *FlinkConf        `json:"flinkConf,omitempty"`
	SparkConf  *SparkConf        `json:"sparkConf,omitempty"`
}

type BigdataConf struct {
	BigdataMetadata `json:"metadata"`
	Spec            BigdataSpec `json:"spec"`
}

type FlinkConf struct {
	Kind                FlinkKind       `json:"kind"`
	Parallelism         int32           `json:"parallelism"`
	JobManagerResource  BigdataResource `json:"jobManagerResource"`
	TaskManagerResource BigdataResource `json:"taskManagerResource"`
	LogConfig           string          `json:"logConfig"`
}

type SparkConf struct {
	Type             string          `json:"type"` // support Java, Python, Scala and R
	Kind             string          `json:"kind"` // support Client and Cluster
	PythonVersion    *string         `json:"pythonVersion,omitempty"`
	DriverResource   BigdataResource `json:"driverResource"`
	ExecutorResource BigdataResource `json:"executorResource"`
	Deps             Dependencies    `json:"deps"`
}

type Dependencies struct {
	PyFiles []string `json:"pyFiles"`
}
