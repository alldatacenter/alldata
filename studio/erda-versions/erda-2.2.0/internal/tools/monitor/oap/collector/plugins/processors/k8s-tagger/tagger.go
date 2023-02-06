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

package tagger

import (
	"regexp"
	"strings"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/k8s-tagger/metadata/pod"
)

var matcherPattern = regexp.MustCompile("%{([^%{}]*)}")

func (p *provider) getPodMetadata(tags map[string]string) pod.Value {
	fs := p.Cfg.Pod.AddMetadata.Finders
	res := pod.NewValue()
	for _, f := range fs {
		switch f.Indexer {
		case pod.IndexerPodName:
			index := generateIndexByMatcher(f.Matcher, tags)
			tmp, ok := p.podCache.GetByPodNameIndexer(index)
			if ok {
				res.Merge(tmp)
			}
		case pod.IndexerPodNameContainer:
			index := generateIndexByMatcher(f.Matcher, tags)
			tmp, ok := p.podCache.GetByPodNameContainerIndexer(index)
			if ok {
				res.Merge(tmp)
			}
		}
	}
	return res
}

// {namespace}/{pod}
func generateIndexByMatcher(matcher string, tags map[string]string) pod.Key {
	matches := matcherPattern.FindAllStringSubmatch(matcher, -1)
	for _, item := range matches {
		if len(item) != 2 {
			continue
		}
		if v, ok := tags[item[1]]; ok {
			matcher = strings.Replace(matcher, item[0], v, -1)
		}
	}
	return pod.Key(matcher)
}
