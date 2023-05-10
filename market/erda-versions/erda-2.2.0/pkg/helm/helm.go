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

package helm

import (
	"context"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/gofrs/flock"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v2"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/cli"
	clivalues "helm.sh/helm/v3/pkg/cli/values"
	"helm.sh/helm/v3/pkg/getter"
	"helm.sh/helm/v3/pkg/release"
	"helm.sh/helm/v3/pkg/repo"
	"helm.sh/helm/v3/pkg/storage/driver"
	"helm.sh/helm/v3/pkg/strvals"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/pkg/filehelper"
)

const (
	EnvHelmNamespace         = "HELM_NAMESPACE"
	EnvHelmDriver            = "HELM_DRIVER"
	EnvHelmDebug             = "HELM_DEBUG"
	EnvLocalChartPath        = "LOCAL_CHART_PATH"
	EnvLocalChartDiscoverDir = "LOCAL_CHART_DISCOVER_DIR"
	DefaultChartSuffix       = ".tgz"
)

type Helm interface {
	AddOrUpdateRepo(repoEntry *repo.Entry) error
	GetReleaseHistory(releaseName string) ([]*release.Release, error)
	InstallRelease(releaseName, chartName, version string, values ...string) error
	UninstallRelease(releaseName string) error
	UpgradeRelease(releaseName, localRepoName, targetVersion string) error
}

type Client struct {
	setting   *cli.EnvSettings
	ac        *action.Configuration
	getter    *RESTClientGetterImpl
	driver    string
	namespace string
	// specified local chart path store in local
	localChartPath string
	// specified local char directory, will discover chart by specified chartName and chartVersion
	// e.g. chartName(non-repoName)-version.tgz, localChartDiscoverDir have a higher priority
	localChartDiscoverDir string
}

type Option func(client *Client)

// New new Helm Interface
func New(options ...Option) (Helm, error) {
	h := Client{
		setting: cli.New(),
		driver:  os.Getenv(EnvHelmDriver),
	}

	h.loadEnvironment()

	for _, op := range options {
		op(&h)
	}

	var ac action.Configuration
	g := h.setting.RESTClientGetter()

	if h.getter != nil {
		g = h.getter
	}

	err := ac.Init(g, h.setting.Namespace(), h.driver, debug)
	if err != nil {
		return nil, err
	}

	h.ac = &ac

	return &h, nil
}

// WithLocalChartDiscoverDir with local chart discover dir
func WithLocalChartDiscoverDir(path string) Option {
	return func(client *Client) {
		client.localChartDiscoverDir = path
	}
}

// WithRESTClientGetter with custom rest client getter, use rest.Config to visit Kubernetes
func WithRESTClientGetter(getter *RESTClientGetterImpl) Option {
	return func(client *Client) {
		client.getter = getter
	}
}

// loadEnvironment load helm environment
func (c *Client) loadEnvironment() {
	if os.Getenv(EnvHelmDebug) == "true" || os.Getenv("DEBUG") == "true" {
		logrus.SetLevel(logrus.DebugLevel)
	}

	if ns := os.Getenv(EnvHelmNamespace); ns != "" {
		c.namespace = ns
	} else {
		c.namespace = metav1.NamespaceDefault
	}

	if path := os.Getenv(EnvLocalChartPath); path != "" {
		c.localChartPath = path
	}

	if dir := os.Getenv(EnvLocalChartDiscoverDir); dir != "" {
		c.localChartDiscoverDir = dir
	}
}

// GetReleaseHistory check release installed or not
func (c *Client) GetReleaseHistory(releaseName string) ([]*release.Release, error) {
	logrus.Infof("[%s] get release on target cluster", releaseName)

	// use HELM_NAMESPACE find release
	hc := action.NewHistory(c.ac)

	releases, err := hc.Run(releaseName)
	if err != nil {
		if err == driver.ErrReleaseNotFound {
			return releases, nil
		}
		logrus.Errorf("[%s] history client run error: %v", releaseName, err)
		return nil, err
	}

	return releases, nil
}

// InstallRelease install release
func (c *Client) InstallRelease(releaseName, chartName, version string, values ...string) error {
	logrus.Infof("install release, name: %s, version: %s, chartName: %s", releaseName, version, chartName)
	logrus.Infof("helm repository cache path: %s", c.setting.RepositoryCache)

	if res, err := c.GetReleaseHistory(releaseName); err != nil {
		return err
	} else {
		if len(res) != 0 {
			return fmt.Errorf("[%s] release already exist on target cluster, version: %s",
				releaseName, res[len(res)-1].Chart.Metadata.Version)
		}
	}

	ic := action.NewInstall(c.ac)

	ic.ReleaseName = releaseName
	ic.Version = version
	ic.Namespace = c.namespace

	chartReq, err := c.getChart(chartName, version, &ic.ChartPathOptions)
	if err != nil {
		return fmt.Errorf("[%s] get chart error: %v", releaseName, err)
	}

	var vals map[string]interface{}

	// if values setting, merge values to vals
	if len(values) != 0 {
		cvOptions := &clivalues.Options{}
		vals, err = cvOptions.MergeValues(getter.All(c.setting))
		if err != nil {
			return err
		}

		if err = strvals.ParseInto(values[0], vals); err != nil {
			return err
		}

	}

	if _, err = ic.Run(chartReq, vals); err != nil {
		return fmt.Errorf("[%s] install error: %v", releaseName, err)
	}

	logrus.Infof("[%s] release install success", releaseName)

	return nil
}

// getChart get chart
func (c *Client) getChart(chartName, version string, chartPathOptions *action.ChartPathOptions) (*chart.Chart, error) {
	var (
		lc  *chart.Chart
		err error
	)

	if c.localChartPath != "" {
		lc, err = loader.LoadFile(c.localChartPath)
	} else if c.localChartDiscoverDir != "" {
		chartPart := strings.Split(chartName, "/")
		if len(chartPart) > 2 {
			return nil, fmt.Errorf("not support chartName format")
		}

		if len(chartPart) == 2 {
			chartName = chartPart[1]
		}

		lc, err = loader.LoadFile(fmt.Sprintf("%s/%s-%s%s", c.localChartDiscoverDir, chartName,
			version, DefaultChartSuffix))
	} else {
		option, err := chartPathOptions.LocateChart(chartName, c.setting)
		if err != nil {
			return nil, fmt.Errorf("located charts %s error: %v", chartName, err)
		}

		lc, err = loader.Load(option)
	}

	if err != nil {
		return nil, fmt.Errorf("load chart path options error: %v", err)
	}

	return lc, nil
}

// UninstallRelease uninstall release which deployed
func (c *Client) UninstallRelease(releaseName string) error {
	// use HELM_NAMESPACE find release
	uc := action.NewUninstall(c.ac)

	resp, err := uc.Run(releaseName)
	if resp != nil {
		logrus.Debugf("[%s] uninstall release %+v,response: %v", releaseName, resp.Release, resp.Info)
	}
	if err != nil {
		return fmt.Errorf("[%s] run uninstall client error: %v", releaseName, err)
	}

	logrus.Infof("[%s] uninstall release success", releaseName)

	return nil
}

// UpgradeRelease upgrade release version
func (c *Client) UpgradeRelease(releaseName, localRepoName, targetVersion string) error {
	// use HELM_NAMESPACE find release
	uc := action.NewUpgrade(c.ac)
	r, err := c.GetReleaseHistory(releaseName)
	if err != nil {
		return err
	}

	if len(r) == 0 {
		return fmt.Errorf("[%s] release doesn't install", releaseName)
	}

	if r[len(r)-1].Chart.Metadata.Version == targetVersion {
		return fmt.Errorf("[%s] version %s already installed", releaseName, r[len(r)-1].Chart.Metadata.Version)
	}

	uc.Version = targetVersion

	chartName := fmt.Sprintf("%s/%s", localRepoName, r[len(r)-1].Chart.Name())
	chartReq, err := c.getChart(chartName, targetVersion, &uc.ChartPathOptions)
	if err != nil {
		return fmt.Errorf("[%s] get chart error: %v", releaseName, err)
	}

	if _, err = uc.Run(releaseName, chartReq, nil); err != nil {
		return fmt.Errorf("[%s] release upgrade from version %s to %s error: %v", releaseName,
			r[len(r)-1].Chart.Metadata.Version, targetVersion, err)
	}

	logrus.Infof("[%s] release upgrade from version %s to %s success", releaseName,
		r[len(r)-1].Chart.Metadata.Version, targetVersion)

	return nil
}

// AddOrUpdateRepo Add or update repo from repo config
func (c *Client) AddOrUpdateRepo(repoEntry *repo.Entry) error {
	logrus.Infof("load repo info: %+v", repoEntry)

	rfPath := c.setting.RepositoryConfig

	if _, err := os.Stat(rfPath); err != nil {
		if !os.IsNotExist(err) {
			return err
		}
		if err = filehelper.CreateFile(rfPath, "", os.ModePerm); err != nil {
			return err
		}
	}

	// repo config lock
	rfLock := flock.New(strings.Replace(rfPath, filepath.Ext(rfPath), ".lock", 1))
	rfLockCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	locked, err := rfLock.TryLockContext(rfLockCtx, time.Second)
	if err != nil {
		return nil
	}

	if locked {
		defer func() {
			err = rfLock.Unlock()
			if err != nil {
				logrus.Errorf("unlock repo file error: %v", err)
			}
		}()
	}

	rfContent, err := ioutil.ReadFile(rfPath)
	if err != nil {
		return fmt.Errorf("repo file read error, path: %s, error: %v", rfPath, err)
	}

	rf := repo.File{}

	if err = yaml.Unmarshal(rfContent, &rf); err != nil {
		return err
	}

	logrus.Debugf("load repo file: %+v", rf)

	isNewRepo := true

	// if has repo already exists, tip and update repo.
	if rf.Has(repoEntry.Name) {
		logrus.Infof("[%s] repo already exists", repoEntry.Name)
		isNewRepo = false
	}

	cr, err := repo.NewChartRepository(repoEntry, getter.All(c.setting))
	if err != nil {
		return err
	}

	logrus.Infof("[%s] start download index file", repoEntry.Name)
	if _, err = cr.DownloadIndexFile(); err != nil {
		return fmt.Errorf("[%s] download index file error: %v", repoEntry.Name, err)
	}

	if !isNewRepo {
		logrus.Infof("[%s] repo update success, path: %s", repoEntry.Name, rfPath)
		return nil
	}

	// Update new repo to repo config file.
	rf.Update(repoEntry)
	if err := rf.WriteFile(c.setting.RepositoryConfig, 0644); err != nil {
		return fmt.Errorf("write repo file %s error: %v", rfPath, err)
	}

	logrus.Infof("change repo success, path: %s", rfPath)

	return nil
}

func debug(format string, v ...interface{}) {
	logrus.Debugf(format, v...)
}
