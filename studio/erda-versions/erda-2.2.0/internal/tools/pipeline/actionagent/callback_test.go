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

package actionagent

import (
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/metadata"
)

func TestHandleMetaFile(t *testing.T) {
	cb := Callback{}
	err := cb.HandleMetaFile([]byte(`{"metadata":[{"name":"commit","value":"7fbca4b1d73c2a68cf0817c31a0c246747e5735d"},{"name":"author","value":"linjun"},{"name":"author_date","value":"2019-08-07 09:57:52 +0800","type":"time"},{"name":"branch","value":"release/3.4"},{"name":"message","value":"update .npmrc\n","type":"message"}]}{"metadata":[{"name":"commit","value":"7fbca4b1d73c2a68cf0817c31a0c246747e5735d"},{"name":"author","value":"linjun"},{"name":"author_date","value":"2019-08-07 09:57:52 +0800","type":"time"},{"name":"branch","value":"release/3.4"},{"name":"message","value":"update .npmrc\n","type":"message"}]}`))
	assert.NoError(t, err)

	cb = Callback{}
	err = cb.HandleMetaFile([]byte(`
commit= 777
author =xxx
message = test metafile
name=pipeline
`))
	assert.NoError(t, err)
	assert.Equal(t, 4, len(cb.Metadata))
	assert.Equal(t, "commit", cb.Metadata[0].Name)
	assert.Equal(t, "777", cb.Metadata[0].Value)
	assert.Equal(t, "author", cb.Metadata[1].Name)
	assert.Equal(t, "xxx", cb.Metadata[1].Value)
	assert.Equal(t, "message", cb.Metadata[2].Name)
	assert.Equal(t, "test metafile", cb.Metadata[2].Value)
	assert.Equal(t, "name", cb.Metadata[3].Name)
	assert.Equal(t, "pipeline", cb.Metadata[3].Value)
}

func TestAppendMetaFile(t *testing.T) {
	var kvs = []struct {
		key   string
		value string
	}{
		{
			key: "name",
			value: `aaa
bbb`,
		},
		{
			key: "name1",
			value: `123456
2345667`,
		},
		{
			key:   "name3",
			value: `aaabbb`,
		},
	}

	var fields []*metadata.MetadataField
	for _, value := range kvs {
		fields = append(fields, &metadata.MetadataField{Name: value.key, Value: value.value})
	}

	cb := Callback{}
	cb.AppendMetadataFields(fields)
	for index := range kvs {
		assert.Equal(t, cb.Metadata[index].Name, kvs[index].key)
		assert.Equal(t, cb.Metadata[index].Value, kvs[index].value)
	}
}

func Test_canDoNormalCallback(t *testing.T) {
	tests := []struct {
		name           string
		openapiAddr    string
		pipelineAddr   string
		isEdgePipeline bool
		wantErr        bool
	}{
		{
			name:           "normal",
			openapiAddr:    "openapi:80",
			isEdgePipeline: false,
		},
		{
			name:           "normal empty openapi",
			openapiAddr:    "",
			isEdgePipeline: false,
			wantErr:        true,
		},
	}
	for _, tt := range tests {
		agent := &Agent{
			EasyUse: EasyUse{
				OpenAPIAddr:    tt.openapiAddr,
				PipelineAddr:   tt.pipelineAddr,
				IsEdgePipeline: tt.isEdgePipeline,
			},
		}
		t.Run(tt.name, func(t *testing.T) {
			if err := agent.canDoNormalCallback(); (err != nil) != tt.wantErr {
				t.Errorf("Agent.canDoNormalCallback() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_canDoEdgeCallback(t *testing.T) {
	tests := []struct {
		name           string
		pipelineAddr   string
		isEdgePipeline bool
		wantErr        bool
	}{
		{
			name:           "edge pipeline",
			pipelineAddr:   "pipeline:3081",
			isEdgePipeline: true,
			wantErr:        false,
		},
		{
			name:           "edge pipeline empty pipeline addr",
			pipelineAddr:   "",
			isEdgePipeline: true,
			wantErr:        true,
		},
	}
	for _, tt := range tests {
		agent := &Agent{
			EasyUse: EasyUse{
				PipelineAddr:   tt.pipelineAddr,
				IsEdgePipeline: tt.isEdgePipeline,
			},
		}
		t.Run(tt.name, func(t *testing.T) {
			if err := agent.canDoEdgeCallback(); (err != nil) != tt.wantErr {
				t.Errorf("Agent.canDoNormalCallback() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_SetCallbackReporter(t *testing.T) {
	agent := &Agent{
		EasyUse: EasyUse{},
	}
	os.Setenv(apistructs.EnvOpenapiTokenForActionBootstrap, "xxx")
	os.Setenv(EnvFileStreamTimeoutSec, "30")
	agent.SetCallbackReporter()
	reporter := agent.CallbackReporter.(*CenterCallbackReporter)
	assert.Equal(t, time.Second*time.Duration(30), reporter.FileStreamTimeoutSec)
}
