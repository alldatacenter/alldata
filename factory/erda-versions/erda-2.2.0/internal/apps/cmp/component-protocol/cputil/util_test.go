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

package cputil

import (
	"testing"

	"github.com/rancher/wrangler/pkg/data"

	"github.com/erda-project/erda/apistructs"
)

func TestParseWorkloadStatus(t *testing.T) {
	fields := make([]string, 8, 8)
	fields[2], fields[3] = "1", "1"
	deployment := data.Object{
		"kind": "Deployment",
		"metadata": map[string]interface{}{
			"fields": fields,
		},
		"status": map[string]interface{}{
			"replicas": "1",
		},
	}
	status, color, breathing, err := ParseWorkloadStatus(deployment)
	if err != nil {
		t.Error(err)
	}
	if status != "Abnormal" || color != "error" {
		t.Errorf("test failed, deployment status is unexpected")
	}
	if breathing {
		t.Errorf("test failed, deployment breathing is unexpected")
	}
	fields[2], fields[3] = "0", "1"
	deployment = data.Object{
		"kind": "Deployment",
		"metadata": map[string]interface{}{
			"fields": fields,
		},
		"status": map[string]interface{}{
			"replicas": "1",
		},
	}
	status, color, breathing, err = ParseWorkloadStatus(deployment)
	if err != nil {
		t.Error(err)
	}
	if status != "Abnormal" || color != "error" {
		t.Errorf("test failed, deployment status is unexpected")
	}
	if breathing {
		t.Errorf("test failed, deployment breathing is unexpected")
	}

	fields = make([]string, 11, 11)
	fields[1], fields[3], fields[4] = "1", "1", "1"
	daemonset := data.Object{
		"kind": "DaemonSet",
		"metadata": map[string]interface{}{
			"fields": fields,
		},
	}
	status, color, breathing, err = ParseWorkloadStatus(daemonset)
	if err != nil {
		t.Error(err)
	}
	if status != "Active" || color != "success" {
		t.Errorf("test failed, daemonset status is unexpected")
	}
	if !breathing {
		t.Errorf("test failed, daemonset breathing is unexpected")
	}
	fields[1], fields[3], fields[4] = "2", "1", "2"
	daemonset = data.Object{
		"kind": "DaemonSet",
		"metadata": map[string]interface{}{
			"fields": fields,
		},
	}
	status, color, breathing, err = ParseWorkloadStatus(daemonset)
	if err != nil {
		t.Error(err)
	}
	if status != "Abnormal" || color != "error" {
		t.Errorf("test failed, daemonset status is unexpected")
	}
	if breathing {
		t.Errorf("test failed, daemonset breathing is unexpected")
	}

	statefulset := data.Object{
		"kind": "StatefulSet",
		"status": map[string]interface{}{
			"replicas":        "1",
			"readyReplicas":   "1",
			"updatedReplicas": "1",
		},
	}
	status, color, breathing, err = ParseWorkloadStatus(statefulset)
	if err != nil {
		t.Error(err)
	}
	if status != "Active" || color != "success" {
		t.Errorf("test failed, statefulset status is unexpected")
	}
	if !breathing {
		t.Errorf("test failed, statefulset breathing is unexpected")
	}
	statefulset = data.Object{
		"kind": "StatefulSet",
		"status": map[string]interface{}{
			"replicas":        "2",
			"readyReplicas":   "1",
			"updatedReplicas": "2",
		},
	}
	status, color, breathing, err = ParseWorkloadStatus(statefulset)
	if err != nil {
		t.Error(err)
	}
	if status != "Abnormal" || color != "error" {
		t.Errorf("test failed, statefulset status is unexpected")
	}
	if breathing {
		t.Errorf("test failed, statefulset breathing is unexpected")
	}

	fields = make([]string, 7, 7)
	job := data.Object{
		"kind": "Job",
		"metadata": map[string]interface{}{
			"fields": fields,
		},
		"status": map[string]interface{}{
			"active": 1,
			"field":  0,
		},
	}
	status, color, breathing, err = ParseWorkloadStatus(job)
	if err != nil {
		t.Error(err)
	}
	if status != "Active" || color != "success" {
		t.Errorf("test failed, job status is unexpected")
	}
	if !breathing {
		t.Errorf("test failed, job breathing is unexpected")
	}
	job = data.Object{
		"kind": "Job",
		"metadata": map[string]interface{}{
			"fields": fields,
		},
		"status": map[string]interface{}{
			"active": 0,
			"failed": 1,
		},
	}
	status, color, breathing, err = ParseWorkloadStatus(job)
	if err != nil {
		t.Error(err)
	}
	if status != "Failed" || color != "error" {
		t.Errorf("test failed, job status is unexpected")
	}
	if breathing {
		t.Errorf("test failed, job breathing is unexpected")
	}
	job = data.Object{
		"kind": "Job",
		"metadata": map[string]interface{}{
			"fields": fields,
		},
		"status": map[string]interface{}{
			"succeeded": 1,
		},
	}
	status, color, breathing, err = ParseWorkloadStatus(job)
	if err != nil {
		t.Error(err)
	}
	if status != "Succeeded" || color != "success" {
		t.Errorf("test failed, job status is unexpected")
	}
	if breathing {
		t.Errorf("test failed, job breathing is unexpected")
	}

	fields = make([]string, 7, 7)
	cronjob := data.Object{
		"kind": "CronJob",
		"metadata": map[string]interface{}{
			"fields": fields,
		},
	}
	status, color, breathing, err = ParseWorkloadStatus(cronjob)
	if err != nil {
		t.Error(err)
	}
	if status != "Active" || color != "success" {
		t.Errorf("test failed, cronjob status is unexpected")
	}
	if !breathing {
		t.Errorf("test failed, cronjob breathing is unexpected")
	}
}

func TestParseWorkloadID(t *testing.T) {
	id := "apps.deployments_default_test"
	kind, namespace, name, err := ParseWorkloadID(id)
	if err != nil {
		t.Error(err)
	}
	if kind != apistructs.K8SDeployment || namespace != "default" || name != "test" {
		t.Error("test failed, kind, namespace or name is unexpected")
	}
}

func TestGetWorkloadAgeAndImage(t *testing.T) {
	field := make([]string, 8, 8)
	field[4] = "1m"
	field[6] = "test"
	deployment := data.Object{
		"kind": "Deployment",
		"metadata": map[string]interface{}{
			"fields": field,
		},
	}
	age, image, err := GetWorkloadAgeAndImage(deployment)
	if err != nil {
		t.Error(err)
	}
	if age != "1m" || image != "test" {
		t.Error("test failed, deployment age or image is unexpected")
	}

	field = make([]string, 11, 11)
	field[7] = "1m"
	field[9] = "test"
	daemonset := data.Object{
		"kind": "DaemonSet",
		"metadata": map[string]interface{}{
			"fields": field,
		},
	}
	age, image, err = GetWorkloadAgeAndImage(daemonset)
	if err != nil {
		t.Error(err)
	}
	if age != "1m" || image != "test" {
		t.Error("test failed, daemonset age or image is unexpected")
	}

	field = make([]string, 5, 5)
	field[2] = "1m"
	field[4] = "test"
	statefulset := data.Object{
		"kind": "StatefulSet",
		"metadata": map[string]interface{}{
			"fields": field,
		},
	}
	age, image, err = GetWorkloadAgeAndImage(statefulset)
	if err != nil {
		t.Error(err)
	}
	if age != "1m" || image != "test" {
		t.Error("test failed, statefulset age or image is unexpected")
	}

	field = make([]string, 7, 7)
	field[3] = "1m"
	field[5] = "test"
	job := data.Object{
		"kind": "Job",
		"metadata": map[string]interface{}{
			"fields": field,
		},
	}
	age, image, err = GetWorkloadAgeAndImage(job)
	if err != nil {
		t.Error(err)
	}
	if age != "1m" || image != "test" {
		t.Error("test failed, job age or image is unexpected")
	}

	field = make([]string, 9, 9)
	field[5] = "1m"
	field[7] = "test"
	cronjob := data.Object{
		"kind": "CronJob",
		"metadata": map[string]interface{}{
			"fields": field,
		},
	}
	age, image, err = GetWorkloadAgeAndImage(cronjob)
	if err != nil {
		t.Error(err)
	}
	if age != "1m" || image != "test" {
		t.Error("test failed, cronjob age or image is unexpected")
	}
}
