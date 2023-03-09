/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package config

import (
	"context"
	"encoding/json"
	"errors"
	"flag"
	"strings"

	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	cliflag "k8s.io/component-base/cli/flag"
	"sigs.k8s.io/controller-runtime/pkg/client/config"

	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor"
	qosmanagerconfig "github.com/koordinator-sh/koordinator/pkg/koordlet/qosmanager/config"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resmanager"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

const (
	DefaultKoordletConfigMapNamespace = "koordinator-system"
	DefaultKoordletConfigMapName      = "koordlet-config"

	CMKeyQoSPluginExtraConfigs = "qos-plugin-extra-configs"
)

type Configuration struct {
	ConfigMapName      string
	ConfigMapNamesapce string
	KubeRestConf       *rest.Config
	StatesInformerConf *statesinformer.Config
	CollectorConf      *metricsadvisor.Config
	MetricCacheConf    *metriccache.Config
	ResManagerConf     *resmanager.Config
	QosManagerConf     *qosmanagerconfig.Config
	RuntimeHookConf    *runtimehooks.Config
	AuditConf          *audit.Config
	FeatureGates       map[string]bool
}

func NewConfiguration() *Configuration {
	return &Configuration{
		ConfigMapName:      DefaultKoordletConfigMapName,
		ConfigMapNamesapce: DefaultKoordletConfigMapNamespace,
		StatesInformerConf: statesinformer.NewDefaultConfig(),
		CollectorConf:      metricsadvisor.NewDefaultConfig(),
		MetricCacheConf:    metriccache.NewDefaultConfig(),
		ResManagerConf:     resmanager.NewDefaultConfig(),
		QosManagerConf:     qosmanagerconfig.NewDefaultConfig(),
		RuntimeHookConf:    runtimehooks.NewDefaultConfig(),
		AuditConf:          audit.NewDefaultConfig(),
	}
}

func (c *Configuration) InitFlags(fs *flag.FlagSet) {
	fs.StringVar(&c.ConfigMapName, "configmap-name", DefaultKoordletConfigMapName, "determines the name the koordlet configmap uses.")
	fs.StringVar(&c.ConfigMapNamesapce, "configmap-namespace", DefaultKoordletConfigMapNamespace, "determines the namespace of configmap uses.")
	system.Conf.InitFlags(fs)
	c.StatesInformerConf.InitFlags(fs)
	c.CollectorConf.InitFlags(fs)
	c.MetricCacheConf.InitFlags(fs)
	c.ResManagerConf.InitFlags(fs)
	c.RuntimeHookConf.InitFlags(fs)
	c.AuditConf.InitFlags(fs)
	resourceexecutor.Conf.InitFlags(fs)
	fs.Var(cliflag.NewMapStringBool(&c.FeatureGates), "feature-gates", "A set of key=value pairs that describe feature gates for alpha/experimental features. "+
		"Options are:\n"+strings.Join(features.DefaultKoordletFeatureGate.KnownFeatures(), "\n"))
}

func (c *Configuration) InitClient() error {
	cfg, err := config.GetConfig()
	if err != nil {
		return err
	}
	cfg.UserAgent = "koordlet"
	c.KubeRestConf = cfg
	return nil
}

func (c *Configuration) InitFromConfigMap() error {
	if c.KubeRestConf == nil {
		return errors.New("KubeRestConf is nil")
	}
	cli, err := kubernetes.NewForConfig(c.KubeRestConf)
	if err != nil {
		return err
	}
	cm, err := cli.CoreV1().ConfigMaps(c.ConfigMapNamesapce).Get(context.TODO(), c.ConfigMapName, metav1.GetOptions{})
	if err == nil {
		// Setup extra configs for QoS Manager.
		if qosPluginExtraConfigRaw, found := cm.Data[CMKeyQoSPluginExtraConfigs]; found {
			var extraConfigs map[string]string
			if err = json.Unmarshal([]byte(qosPluginExtraConfigRaw), &extraConfigs); err != nil {
				return err
			}
			c.QosManagerConf.PluginExtraConfigs = extraConfigs
		}
	} else if !k8serrors.IsNotFound(err) {
		return err
	}
	return nil
}
