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

	cfgpkg "github.com/recallsong/go-utils/config"
)

func (p *provider) loadRolloverBody() error {
	if len(p.C.RolloverBodyFile) <= 0 {
		return nil
	}
	body, err := ioutil.ReadFile(p.C.RolloverBodyFile)
	if err != nil {
		return fmt.Errorf("fail to load index rollover file: %s", err)
	}
	body = cfgpkg.EscapeEnv(body)
	p.rolloverBody = string(body)
	p.L.Info("load rollover body: \n", p.rolloverBody)
	return nil
}

func (p *provider) doRolloverAlias() {
	indices := p.getIndicesAndWait()
	for addon := range indices {
		p.rolloverAlias(p.C.IndexPrefix + addon)
	}
	p.reload <- struct{}{}
}

func (p *provider) rolloverAlias(alias string) error {
	ctx, cancel := context.WithTimeout(context.Background(), p.C.RequestTimeout)
	defer cancel()
	resp, err := p.client.RolloverIndex(alias).BodyString(p.rolloverBody).Do(ctx)
	if err != nil {
		p.L.Errorf("fail to rollover alias %s : %s", alias, err)
		return err
	}
	p.L.Infof("rollover alias %s from %s to %s, %v", alias, resp.OldIndex, resp.NewIndex, resp.Acknowledged)
	return nil
}
