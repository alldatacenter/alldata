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

package manager

import (
	"context"
	"fmt"
	"io/ioutil"
	"time"

	"github.com/olivere/elastic"
	cfgpkg "github.com/recallsong/go-utils/config"
)

func (p *provider) setupIndexTemplate(client *elastic.Client) error {
	if len(p.C.IndexTemplateName) <= 0 || len(p.C.IndexTemplateFile) <= 0 {
		return nil
	}
	template, err := ioutil.ReadFile(p.C.IndexTemplateFile)
	if err != nil {
		return fmt.Errorf("fail to load index template: %s", err)
	}
	template = cfgpkg.EscapeEnv(template)
	body := string(template)
	p.L.Info("load index template: \n", body)
	for i := 0; i < 2; i++ {
		ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
		resp, err := client.IndexPutTemplate(p.C.IndexTemplateName).
			BodyString(body).Do(ctx)
		if err != nil {
			cancel()
			return fmt.Errorf("fail to set index template: %s", err)
		}
		cancel()
		if resp.Acknowledged {
			break
		}
	}
	p.L.Infof("Put index template (%s) success", p.C.IndexTemplateName)
	return nil
}
