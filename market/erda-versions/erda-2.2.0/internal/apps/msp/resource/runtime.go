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

package resource

import (
	"context"
	"encoding/json"
	"strconv"
	"time"

	"github.com/olivere/elastic"

	apm "github.com/erda-project/erda/internal/tools/monitor/apm/common"
)

const ApplicationServiceNode = "application_service_node"

const (
	TagsTerminusKey   = "tags.terminus_key"
	TagsApplicationId = "tags.application_id"
	TagsRuntimeName   = "tags.runtime_name"
	TagsRuntimeId     = "tags.runtime_id"
)

const (
	EmptyIndex        = "spot-empty"
	TimeForSplitIndex = 24 * 60 * 60 * 1000
	IndexTTLDay       = 9
)

type RuntimeQuery struct {
	RuntimeId     string
	RuntimeName   string
	TerminusKey   string
	ApplicationId string
}

type RuntimeDTO struct {
	TerminusKey     string `json:"terminus_key"`
	Workspace       string `json:"Workspace"`
	ProjectId       string `json:"project_id"`
	ProjectName     string `json:"project_name"`
	ApplicationId   string `json:"application_id"`
	ApplicationName string `json:"application_name"`
	RuntimeId       string `json:"runtime_id"`
	RuntimeName     string `json:"runtime_name"`
}

func (s *resourceService) QueryRuntime(query RuntimeQuery) (*RuntimeDTO, error) {
	ctx := context.Background()
	boolQuery := elastic.NewBoolQuery()
	if len(query.RuntimeId) == 0 {
		boolQuery.Filter(elastic.NewTermQuery(TagsTerminusKey, query.TerminusKey)).
			Filter(elastic.NewTermQuery(TagsApplicationId, query.ApplicationId)).
			Filter(elastic.NewTermQuery(TagsRuntimeName, query.RuntimeName))
	} else {
		boolQuery.Filter(elastic.NewTermQuery(TagsRuntimeId, query.RuntimeId))
	}

	nowMs := time.Now().UnixNano() / 1e6
	indices := s.getIndices(ApplicationServiceNode, nowMs-1, nowMs)

	searchSource := elastic.NewSearchSource().Query(boolQuery).Size(1)
	resp, err := s.es.Search(indices...).SearchSource(searchSource).Do(ctx)
	if err != nil {
		return nil, err
	}

	if len(resp.Hits.Hits) == 0 {
		return nil, nil
	}

	source := resp.Hits.Hits[0].Source
	runtime, err := s.parseToRuntime(source)
	return runtime, err
}

func (s *resourceService) getIndices(indexKey string, startTimeMs int64, endTimeMs int64) []string {
	var indices []string
	if startTimeMs > endTimeMs {
		indices = append(indices, EmptyIndex)
		return indices
	}
	timestampMs := startTimeMs - startTimeMs%TimeForSplitIndex
	endTimeMs = endTimeMs - endTimeMs%TimeForSplitIndex

	for startTimestampMs, i := timestampMs, 0; i < IndexTTLDay && startTimestampMs <= endTimeMs; i++ {
		index := "spot-" + indexKey + "-*-" + strconv.FormatInt(startTimestampMs, 10)
		indices = append(indices, index)
		startTimestampMs += TimeForSplitIndex
	}

	if len(indices) <= 0 {
		indices = append(indices, EmptyIndex)
	}
	return indices
}

func (s *resourceService) parseToRuntime(hit *json.RawMessage) (*RuntimeDTO, error) {
	var runtimeInfo RuntimeDTO
	m := make(map[string]interface{})
	err := json.Unmarshal(*hit, &m)
	if err != nil {
		return nil, err
	}
	tags := m[apm.Tags]
	tagsJson, err := json.Marshal(tags)
	if err != nil {
		return nil, err
	}

	err = json.Unmarshal(tagsJson, &runtimeInfo)
	if err != nil {
		return nil, err
	}
	return &runtimeInfo, err
}
