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

package pod

type WatchSelector struct {
	Namespace     string `file:"namespace"`
	LabelSelector string `file:"label_selector"`
	FieldSelector string `file:"field_selector"`
}

type AddMetadata struct {
	LabelInclude      []string `file:"label_include"`
	AnnotationInclude []string `file:"annotation_include"`
	Finders           []Finder `file:"finders"`
}

type Filter struct {
	Key   string `file:"key"`
	Op    string `file:"op"`
	Value string `file:"value"`
}

type Finder struct {
	Indexer  string `file:"indexer" desc:"The type of index to index metadata"`
	Matcher  string `file:"matcher"`
	CnameKey string `file:"cname_key" desc:"The key of container name to find container metadata"`
}

type Config struct {
	WatchSelector WatchSelector `file:"watch_selector"`
	AddMetadata   AddMetadata   `file:"add_metadata"`
}
