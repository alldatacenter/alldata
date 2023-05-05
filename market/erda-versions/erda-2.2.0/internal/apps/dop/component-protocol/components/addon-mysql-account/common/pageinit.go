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
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/pkg/strutil"
)

type PageDataAccount struct {
	ProjectID  uint64
	InstanceID string

	AccountID string // for delete & viewPassword

	ShowDeleteModal       bool
	ShowViewPasswordModal bool

	FilterValues FilterValues
}

type PageDataAttachment struct {
	ProjectID  uint64
	InstanceID string

	AttachmentID string // for config & edit

	ShowConfigPanel   bool
	ShowEditFormModal bool

	FilterValues FilterValues
}

type FilterValues map[string]interface{}

func (v FilterValues) StringSlice(key string) []string {
	if v == nil {
		return nil
	}
	switch opt := v[key].(type) {
	case []string:
		return opt
	case []interface{}:
		var strOpts []string
		for _, o := range opt {
			strOpts = append(strOpts, strutil.String(o))
		}
		return strOpts
	}
	return nil
}

func InitPageDataAccount(ctx context.Context) (*PageDataAccount, error) {
	sdk := cputil.SDK(ctx)
	if sdk == nil {
		return nil, fmt.Errorf("bad sdk")
	}
	inParams := sdk.InParams
	instanceID := inParams.String("instanceId")
	projectID := inParams.Uint64("projectId")
	if instanceID == "" || projectID == 0 {
		return nil, fmt.Errorf("bad inParams: missing or bad instanceId and projectId")
	}

	ft := GetFilterBase64(ctx)
	v, err := GetValues(ft)
	if err != nil {
		return nil, err
	}

	d := &PageDataAccount{
		ProjectID:    projectID,
		InstanceID:   instanceID,
		FilterValues: v,
	}
	state, ok := ctx.Value(cptype.GlobalInnerKeyStateTemp).(map[string]interface{})
	if !ok {
		return nil, fmt.Errorf("bad state")
	}
	state["pageDataAccount"] = d
	return d, nil
}

func LoadPageDataAccount(ctx context.Context) *PageDataAccount {
	if ctx == nil {
		return nil
	}
	v := ctx.Value(cptype.GlobalInnerKeyStateTemp)
	if m, ok := v.(map[string]interface{}); ok {
		r, _ := m["pageDataAccount"].(*PageDataAccount)
		return r
	}
	return nil
}

func InitPageDataAttachment(ctx context.Context) (*PageDataAttachment, error) {
	sdk := cputil.SDK(ctx)
	if sdk == nil {
		return nil, fmt.Errorf("bad sdk")
	}
	inParams := sdk.InParams
	instanceID := inParams.String("instanceId")
	projectID := inParams.Uint64("projectId")
	if instanceID == "" || projectID == 0 {
		return nil, fmt.Errorf("bad inParams: missing or bad instanceId and projectId")
	}

	ft := GetFilterBase64(ctx)
	v, err := GetValues(ft)
	if err != nil {
		return nil, err
	}

	d := &PageDataAttachment{
		ProjectID:    projectID,
		InstanceID:   instanceID,
		FilterValues: v,
	}
	state, ok := ctx.Value(cptype.GlobalInnerKeyStateTemp).(map[string]interface{})
	if !ok {
		return nil, fmt.Errorf("bad state")
	}
	state["pageDataAttachment"] = d
	return d, nil
}

func LoadPageDataAttachment(ctx context.Context) *PageDataAttachment {
	if ctx == nil {
		return nil
	}
	v := ctx.Value(cptype.GlobalInnerKeyStateTemp)
	if v == nil {
		return nil
	}
	if m, ok := v.(map[string]interface{}); ok {
		if r, ok := m["pageDataAttachment"]; ok {
			return r.(*PageDataAttachment)
		}
	}
	return nil
}

func GetFilterBase64(ctx context.Context) string {
	t := cputil.GetInParamByKey(ctx, "filter__urlQuery")
	if t == nil {
		return ""
	}
	return t.(string)
}

func GetValues(filterBase64 string) (map[string]interface{}, error) {
	if filterBase64 == "" {
		return nil, nil
	}
	var values map[string]interface{}
	b, err := base64.StdEncoding.DecodeString(filterBase64)
	if err != nil {
		return nil, err
	}
	if err := json.Unmarshal(b, &values); err != nil {
		return nil, err
	}
	return values, nil
}

func ToBase64(values interface{}) (string, error) {
	b, err := json.Marshal(values)
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(b), nil
}
