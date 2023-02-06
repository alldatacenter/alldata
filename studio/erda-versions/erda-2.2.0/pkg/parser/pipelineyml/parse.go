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

package pipelineyml

import (
	"bytes"
	"strings"

	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/strutil"
)

// parse parse pipeline.yml, compatible with history versions.
func (y *PipelineYml) parse(b []byte, errs ...string) (err error) {

	if len(errs) == 0 {
		errs = make([]string, 0)
	}

	defer func() {
		if err != nil {
			if len(errs) >= 2 {
				polishErrs := make([]string, 0)
				for _, s := range errs {
					if !strings.Contains(s, "cannot unmarshal") {
						polishErrs = append(polishErrs, s)
					}
				}
				if len(polishErrs) > 0 {
					errs = polishErrs
				}
			}
			if len(errs) > 0 {
				err = errors.Errorf(strutil.Join(errs, "\n", true))
			}
		}
	}()

	version, err := GetVersion(b)
	if err != nil {
		return errors.Errorf("failed to get version from yaml, err: %v", err)
	}

	switch version {

	// 1) 以 Spec 结构解析
	// 2) 尝试以 apistructs.PipelineYml 结构解析，该结构用户前端图形化展示
	case Version1dot1:

		// ParseSpec
		decoder := yaml.NewDecoder(bytes.NewBuffer(b))
		decoder.KnownFields(false)
		if err := decoder.Decode(&y.s); err == nil {
			return nil
		} else {
			errs = append(errs, errors.Errorf("parsed by 1.1 spec, err: %v", err).Error())
		}

		// Parse apistructs.PipelineYml
		if convertedPipelineYmlContent, err := ConvertGraphPipelineYmlContent(b); err == nil {
			// convertedPipelineYmlContent 会丢失 hint，由于 graph pipeline yaml 都是程序生成，hint 丢失可接受
			y.data = convertedPipelineYmlContent
			return y.parse(convertedPipelineYmlContent, errs...)
		} else {
			errs = append(errs, errors.Errorf("parsed by 1.1 spec(apistructs), err: %v", err).Error())
			return err
		}

	case Version1dot0, Version1:

		y.needUpgrade = true

		if err := y.parseV1(); err != nil {
			return errors.Errorf("failed to parse 1.0 pipelineyml, err: %v", err)
		}
		y.data = y.upgradedYmlContent
		return nil

	default:
		return errors.Errorf("invalid version: %s, currently support: 1.0, 1.1", version)
	}
}
