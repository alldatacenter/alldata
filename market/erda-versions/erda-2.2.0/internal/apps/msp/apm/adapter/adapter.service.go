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

package adapter

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-proto-go/msp/apm/adapter/pb"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/template"
)

var (
	endpoints = map[string]string{
		"Jaeger": "/api/jaeger/traces",
	}
)

type adapterService struct {
	p *provider
}

func (s *adapterService) GetInstrumentationLibrary(ctx context.Context, request *pb.GetInstrumentationLibraryRequest) (*pb.GetInstrumentationLibraryResponse, error) {
	result := &pb.GetInstrumentationLibraryResponse{
		Data: make([]*pb.InstrumentationLibrary, 0),
	}
	for _, v := range s.p.libraries {
		if !v.Enabled {
			continue
		}
		languages := make([]*pb.Language, 0)
		for _, l := range v.Languages {
			if !l.Enabled {
				continue
			}
			languages = append(languages, &pb.Language{
				Language: l.Name,
			})
		}
		library := &pb.InstrumentationLibrary{
			Strategy:  v.InstrumentationLibrary,
			Languages: languages,
		}
		result.Data = append(result.Data, library)
	}
	return result, nil
}

func (s *adapterService) GetInstrumentationLibraryDocs(ctx context.Context, request *pb.GetInstrumentationLibraryDocsRequest) (*pb.GetInstrumentationLibraryDocsResponse, error) {
	lang := apis.GetLang(ctx)
	if templates, ok := s.p.templates[request.Strategy]; ok {
		for _, t := range templates.Templates {
			if t.Language != request.Language {
				continue
			}
			renderMap := map[string]string{
				"erda_env_id": request.ScopeId,
				"endpoint":    s.p.Cfg.CollectorUrl + endpoints[request.Strategy],
			}
			result := template.Render(t.Template[lang], renderMap)
			return &pb.GetInstrumentationLibraryDocsResponse{
				Data: result,
			}, nil
		}
	}
	return nil, errors.NewInternalServerError(fmt.Errorf("not fount doc for strategy %s language %s", request.Strategy, request.Language))
}
