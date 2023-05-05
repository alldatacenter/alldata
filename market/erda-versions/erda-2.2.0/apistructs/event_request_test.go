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
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestEventCreateRequestMarshal(t *testing.T) {
	eh := EventHeader{
		Event:     "eventname",
		Action:    "eventaction",
		OrgID:     "1",
		ProjectID: "2",
		TimeStamp: "3",
	}
	r := EventCreateRequest{
		EventHeader: eh,
		Sender:      "test",
		Content:     "testcontent",
	}
	m, err := json.Marshal(r)
	assert.Nil(t, err)
	var v interface{}
	assert.Nil(t, json.Unmarshal(m, &v))
	vm := v.(map[string]interface{})
	_, ok := vm["WEBHOOK"]
	assert.False(t, ok)

}

func TestConvertEventToPB(t *testing.T) {
	event := &EventCreateRequest{
		Sender: "pipeline",
		Content: PipelineInstanceEventData{
			PipelineID: 1,
			Status:     PipelineStatusSuccess.String(),
			Branch:     "develop",
		},
		EventHeader: EventHeader{
			Event: "pipeline",
		},
	}

	eventPB, err := event.ConvertToPB()
	assert.NoError(t, err)
	assert.Equal(t, "pipeline", eventPB.Sender)
}
