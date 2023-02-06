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

package common

import (
	"encoding/json"
	"reflect"

	"github.com/pkg/errors"
)

const (
	ScenarioKey = "app-runtime"

	ProjectRuntimeFilter = "filter"

	DeleteOp = "delete"
	//DeployReleaseOp = "deploy-release"
	ReStartOp = "restart"

	FilterApp             = "app"
	FilterRuntimeStatus   = "runtimeStatus"
	FilterDeployStatus    = "deploymentStatus"
	FilterDeployOrderName = "deploymentOrderName"
	FilterDeployTime      = "deployTime"

	FilterInputCondition = "title"

	FrontedStatusSuccess    = "success"
	FrontedStatusDefault    = "default"
	FrontedStatusError      = "error"
	FrontedStatusWarning    = "warning"
	FrontedStatusProcessing = "processing"

	FrontedIconLoading   = "default_loading"
	FrontedIconBreathing = "default_breathing"
)

var (
	PtrRequiredErr     = errors.New("b must be a pointer")
	NothingToBeDoneErr = errors.New("nothing to be done")
)

type Operation struct {
	JumpOut bool                   `json:"jumpOut"`
	Target  string                 `json:"target"`
	Query   map[string]interface{} `json:"query"`
	Params  map[string]interface{} `json:"params"`
}

// Transfer transfer a to b with json, kind of b must be pointer
func Transfer(a, b interface{}) error {
	if reflect.ValueOf(b).Kind() != reflect.Ptr {
		return PtrRequiredErr
	}
	if a == nil {
		return NothingToBeDoneErr
	}
	aBytes, err := json.Marshal(a)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(aBytes, b); err != nil {
		return err
	}
	return nil
}

func getNextWithoutCase(s string) []int {
	next := make([]int, len(s)+1)
	next[0] = -1
	j := 0
	k := -1
	for j < len(s)-1 {
		if k == -1 || tolower(s[j]) == tolower(s[k]) {
			j++
			k++
			if tolower(s[j]) == tolower(s[k]) {
				next[j] = next[k]
			} else {
				next[j] = k
			}
		} else {
			k = next[k]
		}
	}
	return next
}
func tolower(c uint8) uint8 {
	if c >= 'a' && c <= 'z' {
		return c - 32
	}
	return c
}
func ExitsWithoutCase(s, sub string) bool {
	i := 0
	j := 0
	next := getNextWithoutCase(sub)
	for i < len(s) && j < len(sub) {
		if j == -1 || tolower(s[i]) == tolower(sub[j]) {
			i++
			j++
		} else {
			j = next[j]
		}
	}
	if j == len(sub) {
		return true
	}
	return false
}
