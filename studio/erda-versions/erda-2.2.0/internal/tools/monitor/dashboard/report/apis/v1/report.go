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

package reportapisv1

import (
	"context"
	"encoding/hex"
	"fmt"
	"net"
	"strings"

	uuid "github.com/satori/go.uuid"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v2"

	notifyGrouppb "github.com/erda-project/erda-proto-go/core/messenger/notifygroup/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda-proto-go/tools/monitor/dashboard/report/pb"
	dicestructs "github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/discover"
)

func editReportTaskFields(report *reportTask, update *reportTaskUpdate) *reportTask {
	if update.Name != nil {
		report.Name = *update.Name
	}
	if update.NotifyTarget != nil {
		report.NotifyTarget = pb2notify(update.NotifyTarget)
	}
	if update.DashboardId != nil {
		report.DashboardId = *update.DashboardId
	}
	return report
}

func (p *provider) getNotifyGroupRelByID(ctx context.Context, groupID string) *notifyGrouppb.NotifyGroup {
	if groupID == "" {
		return nil
	}
	notifyGroupsData, err := p.NotifyGroupService.BatchGetNotifyGroup(ctx, &notifyGrouppb.BatchGetNotifyGroupRequest{
		Ids: groupID,
	})
	if err != nil {
		p.Log.Errorf("request: query notify group error: %s", err)
		return nil
	}
	if len(notifyGroupsData.Data) > 0 {
		return notifyGroupsData.Data[0]
	}
	return nil
}

// stop and delete pipelineCron , ignored error
func (p *provider) stopAndDelPipelineCron(obj *reportTask) error {
	if obj.PipelineCronId != 0 {
		_, err := p.CronService.CronStop(context.Background(), &cronpb.CronStopRequest{
			CronID: obj.PipelineCronId,
		})
		_, _ = p.CronService.CronDelete(context.Background(), &cronpb.CronDeleteRequest{
			CronID: obj.PipelineCronId,
		})
		if err != nil {
			return err
		}
	}
	return nil
}

func (p *provider) createReportPipelineCron(obj *reportTask) error {
	pipeline, err := p.generatePipeline(obj)
	if err != nil {
		return err
	}
	createResp, err := p.bdl.CreatePipeline(&pipeline)
	if err != nil {
		return err
	}
	if createResp.CronID != nil {
		obj.PipelineCronId = *createResp.CronID
	}
	return nil
}

func (p *provider) generatePipeline(r *reportTask) (pipeline dicestructs.PipelineCreateRequestV2, err error) {
	pipeline.PipelineYml, err = p.generatePipelineYml(r)
	if err != nil {
		return pipeline, err
	}
	pipeline.PipelineSource = dicestructs.PipelineSourceDice
	pipeline.PipelineYmlName = hex.EncodeToString(uuid.NewV4().Bytes()) + ".yml"
	pipeline.ClusterName = p.Cfg.ClusterName
	pipeline.AutoRunAtOnce = r.RunAtOnce
	if r.Enable {
		pipeline.AutoStartCron = true
	} else {
		pipeline.AutoStartCron = false
	}

	return pipeline, nil
}

func (p *provider) generatePipelineYml(r *reportTask) (string, error) {
	pipelineYml := &dicestructs.PipelineYml{
		Version: p.Cfg.Pipeline.Version,
	}
	switch r.Type {
	case monthly:
		pipelineYml.Cron = p.Cfg.ReportCron.MonthlyCron
	case weekly:
		pipelineYml.Cron = p.Cfg.ReportCron.WeeklyCron
	case daily:
		pipelineYml.Cron = p.Cfg.ReportCron.DailyCron
	}
	org, err := p.bdl.GetOrg(r.ScopeID)
	if err != nil {
		return "", fmt.Errorf("failed to generate pipeline yaml, can not get OrgName by OrgID:%v,(%+v)", r.ScopeID, err)
	}

	maddr, err := p.createFQDN(discover.Monitor())
	if err != nil {
		return "", err
	}
	eaddr, err := p.createFQDN(discover.CoreServices())
	if err != nil {
		return "", err
	}
	pipelineYml.Stages = [][]*dicestructs.PipelineYmlAction{{{
		Type:    p.Cfg.Pipeline.ActionType,
		Version: p.Cfg.Pipeline.ActionVersion,
		Params: map[string]interface{}{
			"monitor_addr":       maddr,
			"core_services_addr": eaddr,
			"report_id":          r.ID,
			"org_name":           org.Name,
			"domain_addr":        fmt.Sprintf("%s://%s", p.Cfg.DiceProtocol, org.Domain),
		},
	}}}
	byteContent, err := yaml.Marshal(pipelineYml)
	if err != nil {
		return "", fmt.Errorf("failed to generate pipeline yaml, pipelineYml:%+v, (%+v)", pipelineYml, err)
	}

	logrus.Debugf("[PipelineYml]: %s", string(byteContent))
	return string(byteContent), nil
}

func (p *provider) createFQDN(addr string) (string, error) {
	host, port, err := net.SplitHostPort(addr)
	if err != nil {
		return "", err
	}
	var svc string
	idx := strings.Index(host, ".")
	if idx == -1 {
		svc = host
	} else {
		svc = host[:idx]
	}
	return net.JoinHostPort(svc+"."+p.Cfg.DiceNameSpace, port), nil
}

func notify2pb(obj *notify) *pb.Notify {
	return &pb.Notify{
		Type:        obj.Type,
		GroupId:     obj.GroupId,
		GroupType:   obj.GroupType,
		NotifyGroup: obj.NotifyGroup,
	}
}

func pb2notify(obj *pb.Notify) *notify {
	return (*notify)(obj)
}
