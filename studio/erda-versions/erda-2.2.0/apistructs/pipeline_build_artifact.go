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

type BuildArtifact struct {
	ID           int64  `json:"id"`
	Sha256       string `json:"sha256"`
	IdentityText string `json:"identityText"`
	Type         string `json:"type"`
	Content      string `json:"content"`
	ClusterName  string `json:"clusterName"`
	PipelineID   uint64 `json:"pipelineID"`
}

type BuildArtifactType string

const (
	BuildArtifactOfNfsLink     BuildArtifactType = "NFS_LINK "
	BuildArtifactOfFileContent BuildArtifactType = "FILE_CONTENT "
)

// register

type BuildArtifactRegisterRequest struct {
	SHA          string `json:"sha"`
	IdentityText string `json:"identity_text"`
	Type         string `json:"type"`
	Content      string `json:"content"`
	ClusterName  string `json:"cluster_name"`
	PipelineID   uint64 `json:"pipelineID"`
}

type BuildArtifactRegisterResponse struct {
	Header
	Data *BuildArtifact `json:"data"`
}

// delete

type BuildArtifactDeleteByImagesRequest struct {
	Images []string `json:"images"`
}

// query

type BuildArtifactQueryResponse struct {
	Header
	Data *BuildArtifact `json:"data"`
}
