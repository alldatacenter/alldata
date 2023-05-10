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
package issueFilter

import (
	"encoding/base64"
	"encoding/json"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/pkg/strutil"
)

type FilterSetData struct {
	Values FrontendConditions `json:"values,omitempty"`
	Label  string             `json:"label,omitempty"`
}

func (f *IssueFilter) initFilterBms() error {
	pageKey := f.issueFilterBmSvc.GenPageKey(f.InParams.FrontendFixedIteration, f.InParams.FrontendFixedIssueType)
	mp, err := f.issueFilterBmSvc.ListMyBms(f.sdk.Identity.UserID, strutil.String(f.InParams.ProjectID))
	if err != nil {
		return err
	}
	f.Bms = mp.GetByPageKey(pageKey)
	return nil
}

func (f *IssueFilter) FilterSet() ([]filter.SetItem, error) {
	options := []filter.SetItem{
		{
			ID:       "all",
			Label:    f.sdk.I18n("openAll"),
			IsPreset: true,
		},
	}
	if f.State.WithStateCondition {
		options = append(options, filter.SetItem{
			ID:       "defaultState",
			Label:    f.sdk.I18n("unfinishedIssue"),
			IsPreset: true,
			Values:   map[string]interface{}{PropConditionKeyStates: f.State.DefaultStateValues},
		})
	}
	for _, i := range f.Bms {
		value, err := FilterSetValueRetriever(i.FilterEntity)
		if err != nil {
			return nil, err
		}
		options = append(options, filter.SetItem{
			ID:     i.ID,
			Label:  i.Name,
			Values: *value,
		})
	}
	return options, nil
}

func FilterSetValueRetriever(filterEntity string) (*cptype.ExtraMap, error) {
	b, err := base64.StdEncoding.DecodeString(filterEntity)
	if err != nil {
		return nil, err
	}
	var value cptype.ExtraMap
	if err := json.Unmarshal(b, &value); err != nil {
		return nil, err
	}
	return &value, nil
}

func (f *IssueFilter) CreateFilterSetEntity(input interface{}) (string, error) {
	fb, err := json.Marshal(input)
	if err != nil {
		return "", err
	}
	base64Str := base64.StdEncoding.EncodeToString(fb)
	return base64Str, nil
}

func (f *IssueFilter) generateUrlQueryParams() (string, error) {
	fb, err := json.Marshal(f.State.FrontendConditionValues)
	if err != nil {
		return "", err
	}
	base64Str := base64.StdEncoding.EncodeToString(fb)
	return base64Str, nil
}
