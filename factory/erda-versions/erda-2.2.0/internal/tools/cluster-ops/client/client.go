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

package client

import (
	"context"
	"fmt"
	"strings"

	"github.com/sirupsen/logrus"
	"helm.sh/helm/v3/pkg/repo"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/cluster-ops/config"
	erdahelm "github.com/erda-project/erda/pkg/helm"
	kc "github.com/erda-project/erda/pkg/k8sclient/config"
)

const (
	defaultRepoName   = "stable"
	InstallModeRemote = "REMOTE"
	ErdaCharts        = "erda"
)

type Option func(client *Client)

type Client struct {
	config *config.Config
}

func New(opts ...Option) *Client {
	c := Client{}
	for _, op := range opts {
		op(&c)
	}

	return &c
}

func WithConfig(cfg *config.Config) Option {
	return func(c *Client) {
		c.config = cfg
	}
}

func (c *Client) Execute() error {
	logrus.Debugf("load config: %+v", c.config)

	opts, err := c.genHelmClientOptions()
	if err != nil {
		return fmt.Errorf("get helm client error: %v", err)
	}

	hc, err := erdahelm.New(opts...)
	if err != nil {
		return err
	}

	// TODO: support repo auth info.
	e := &repo.Entry{
		Name:     defaultRepoName,
		URL:      c.config.RepoURL,
		Username: c.config.RepoUsername,
		Password: c.config.RepoPassword,
	}

	if err = hc.AddOrUpdateRepo(e); err != nil {
		return err
	}

	if c.config.Reinstall {
		charts := c.getInitCharts()
		for _, chart := range charts {
			chart.Action = erdahelm.ActionUninstall
		}
		m := erdahelm.Manager{
			HelmClient:    hc,
			Charts:        charts,
			LocalRepoName: defaultRepoName,
		}

		if err = m.Execute(); err != nil {
			logrus.Errorf("execute uninstall error: %v", err)
			return err
		}
	}

	m := erdahelm.Manager{
		HelmClient:    hc,
		Charts:        c.getInitCharts(),
		LocalRepoName: defaultRepoName,
	}

	if err = m.Execute(); err != nil {
		logrus.Errorf("execute error: %v", err)
		return err
	}

	// Label node only local mode
	// TODO: support label remote with rest.config
	if strings.ToUpper(c.config.InstallMode) != InstallModeRemote {
		rc, err := rest.InClusterConfig()
		if err != nil {
			logrus.Errorf("get incluster rest config error: %v", err)
			return err
		}
		cs, err := kubernetes.NewForConfig(rc)
		if err != nil {
			logrus.Errorf("create clientSet error: %v", err)
			return err
		}
		nodes, err := cs.CoreV1().Nodes().List(context.Background(), metav1.ListOptions{})
		if err != nil {
			logrus.Errorf("get kubernetes nodes error: %v", err)
			return err
		}
		logrus.Infof("get nodes success, node count: %d", len(nodes.Items))

		labels := c.parseLabels()

		if len(labels) == 0 {
			return nil
		}

		logrus.Infof("start to label nodes, labels: %+v", labels)

		for _, node := range nodes.Items {
			for k, v := range labels {
				node.Labels[k] = v
			}
			if _, err = cs.CoreV1().Nodes().Update(context.Background(), &node, metav1.UpdateOptions{}); err != nil {
				logrus.Errorf("label node: %s, error: %v", node.Name, err)
				return err
			}
		}
	}

	return nil
}

// genHelmClientOptions create helm client options
func (c *Client) genHelmClientOptions() ([]erdahelm.Option, error) {
	opts := make([]erdahelm.Option, 0)

	switch strings.ToUpper(c.config.InstallMode) {
	case InstallModeRemote:
		b := bundle.New(bundle.WithClusterManager())
		cluster, err := b.GetCluster(c.config.TargetCluster)
		if err != nil {
			return nil, err
		}

		rc, err := kc.ParseManageConfig(c.config.TargetCluster, cluster.ManageConfig)
		if err != nil {
			return nil, err
		}

		opts = append(opts, erdahelm.WithRESTClientGetter(erdahelm.NewRESTClientGetterImpl(rc)))
	}

	return opts, nil
}

func (c *Client) getInitCharts() []*erdahelm.ChartSpec {
	return []*erdahelm.ChartSpec{
		{
			ReleaseName: ErdaCharts,
			ChartName:   ErdaCharts,
			Version:     c.config.Version,
			Action:      erdahelm.ActionInstall,
			Values:      c.config.SetValues,
		},
	}
}

func (c *Client) parseLabels() map[string]string {
	res := make(map[string]string, 0)
	if c.config.NodeLabels == "" {
		return res
	}

	labels := strings.Split(c.config.NodeLabels, ",")
	for _, label := range labels {
		keys := strings.Split(label, "=")
		switch len(keys) {
		case 1:
			res[keys[0]] = ""
		case 2:
			res[keys[0]] = keys[1]
		default:
			continue
		}
	}
	return res
}
