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

package dbclient

type DicePipelineSnippetClient struct {
	ID    uint64                     `json:"id" xorm:"pk autoincr"`
	Name  string                     `json:"name"`
	Host  string                     `json:"host"`
	Extra PipelineSnippetClientExtra `json:"extra" xorm:"json"`
}

func (ps *DicePipelineSnippetClient) TableName() string {
	return "dice_pipeline_snippet_clients"
}

type PipelineSnippetClientExtra struct {
	UrlPathPrefix string `json:"urlPathPrefix"`
}

func (client *Client) FindSnippetClientList() (clients []*DicePipelineSnippetClient, err error) {

	err = client.Find(&clients)
	if err != nil {
		return nil, err
	}

	return clients, err
}
