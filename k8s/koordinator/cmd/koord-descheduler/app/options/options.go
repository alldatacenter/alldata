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

// Package options provides the descheduler flags
package options

import (
	"fmt"
	"os"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/uuid"
	apiserveroptions "k8s.io/apiserver/pkg/server/options"
	"k8s.io/client-go/dynamic"
	"k8s.io/client-go/dynamic/dynamicinformer"
	clientset "k8s.io/client-go/kubernetes"
	restclient "k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/client-go/tools/events"
	"k8s.io/client-go/tools/leaderelection"
	"k8s.io/client-go/tools/leaderelection/resourcelock"
	"k8s.io/client-go/tools/record"
	cliflag "k8s.io/component-base/cli/flag"
	componentbaseconfig "k8s.io/component-base/config"
	componentbaseoptions "k8s.io/component-base/config/options"
	"k8s.io/component-base/logs"
	"k8s.io/component-base/metrics"
	"k8s.io/klog/v2"
	ctrl "sigs.k8s.io/controller-runtime"

	deschedulerappconfig "github.com/koordinator-sh/koordinator/cmd/koord-descheduler/app/config"
	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	deschedulerscheme "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/scheme"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/v1alpha2"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/validation"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/informers"
	utilclient "github.com/koordinator-sh/koordinator/pkg/util/client"
	utilfeature "github.com/koordinator-sh/koordinator/pkg/util/feature"
)

func newDefaultComponentConfig() (*deschedulerconfig.DeschedulerConfiguration, error) {
	versionedCfg := v1alpha2.DeschedulerConfiguration{}
	deschedulerscheme.Scheme.Default(&versionedCfg)
	cfg := deschedulerconfig.DeschedulerConfiguration{}
	if err := deschedulerscheme.Scheme.Convert(&versionedCfg, &cfg, nil); err != nil {
		return nil, err
	}
	return &cfg, nil
}

// Options has all the params needed to run a Scheduler
type Options struct {
	ComponentConfig *deschedulerconfig.DeschedulerConfiguration

	LeaderElection          *componentbaseconfig.LeaderElectionConfiguration
	SecureServing           *apiserveroptions.SecureServingOptionsWithLoopback
	CombinedInsecureServing *CombinedInsecureServingOptions
	Metrics                 *metrics.Options
	Logs                    *logs.Options

	// ConfigFile is the location of the scheduler server's configuration file.
	ConfigFile string

	// WriteConfigTo is the path where the default configuration will be written.
	WriteConfigTo string

	// Flags hold the parsed CLI flags.
	Flags *cliflag.NamedFlagSets

	Client clientset.Interface
}

// NewOptions returns default scheduler app options.
func NewOptions() *Options {
	cfg, err := newDefaultComponentConfig()
	if err != nil {
		klog.Fatalf("Failed to newDefaultComponentConfig, err: %v", err)
	}
	o := &Options{
		ComponentConfig: cfg,
		SecureServing:   apiserveroptions.NewSecureServingOptions().WithLoopback(),
		CombinedInsecureServing: &CombinedInsecureServingOptions{
			Healthz: (&apiserveroptions.DeprecatedInsecureServingOptions{
				BindNetwork: "tcp",
			}).WithLoopback(),
			Metrics: (&apiserveroptions.DeprecatedInsecureServingOptions{
				BindNetwork: "tcp"}).WithLoopback(),
		},
		LeaderElection: &componentbaseconfig.LeaderElectionConfiguration{
			LeaderElect:       true,
			LeaseDuration:     metav1.Duration{Duration: 15 * time.Second},
			RenewDeadline:     metav1.Duration{Duration: 10 * time.Second},
			RetryPeriod:       metav1.Duration{Duration: 2 * time.Second},
			ResourceLock:      "leases",
			ResourceName:      "koord-descheduler",
			ResourceNamespace: "koordinator-system",
		},
		Metrics: metrics.NewOptions(),
		Logs:    logs.NewOptions(),
	}

	o.SecureServing.BindPort = deschedulerconfig.DefaultDeschedulerPort

	o.initFlags()

	return o
}

// ApplyLeaderElectionTo obtains the CLI args related with leaderelection, and override the values in `cfg`.
// Then the `cfg` object is injected into the `options` object.
func (o *Options) ApplyLeaderElectionTo(cfg *deschedulerconfig.DeschedulerConfiguration) {
	if o.Flags == nil {
		return
	}
	// Obtain CLI args related with leaderElection. Set them to `cfg` if specified in command line.
	leaderElection := o.Flags.FlagSet("leader election")
	if leaderElection.Changed("leader-elect") {
		cfg.LeaderElection.LeaderElect = o.LeaderElection.LeaderElect
	}
	if leaderElection.Changed("leader-elect-lease-duration") {
		cfg.LeaderElection.LeaseDuration = o.LeaderElection.LeaseDuration
	}
	if leaderElection.Changed("leader-elect-renew-deadline") {
		cfg.LeaderElection.RenewDeadline = o.LeaderElection.RenewDeadline
	}
	if leaderElection.Changed("leader-elect-retry-period") {
		cfg.LeaderElection.RetryPeriod = o.LeaderElection.RetryPeriod
	}
	if leaderElection.Changed("leader-elect-resource-lock") {
		cfg.LeaderElection.ResourceLock = o.LeaderElection.ResourceLock
	}
	if leaderElection.Changed("leader-elect-resource-name") {
		cfg.LeaderElection.ResourceName = o.LeaderElection.ResourceName
	}
	if leaderElection.Changed("leader-elect-resource-namespace") {
		cfg.LeaderElection.ResourceNamespace = o.LeaderElection.ResourceNamespace
	}

	o.ComponentConfig = cfg
}

// initFlags initializes flags by section name.
func (o *Options) initFlags() {
	if o.Flags != nil {
		return
	}

	nfs := cliflag.NamedFlagSets{}
	fs := nfs.FlagSet("misc")
	fs.StringVar(&o.ConfigFile, "config", o.ConfigFile, "The path to the configuration file.")
	fs.StringVar(&o.WriteConfigTo, "write-config-to", o.WriteConfigTo, "If set, write the configuration values to this file and exit.")

	o.SecureServing.AddFlags(nfs.FlagSet("secure serving"))
	o.CombinedInsecureServing.AddFlags(nfs.FlagSet("insecure serving"))
	componentbaseoptions.BindLeaderElectionFlags(o.LeaderElection, nfs.FlagSet("leader election"))
	utilfeature.DefaultMutableFeatureGate.AddFlag(nfs.FlagSet("feature gate"))
	o.Metrics.AddFlags(nfs.FlagSet("metrics"))
	o.Logs.AddFlags(nfs.FlagSet("logs"))

	o.Flags = &nfs
}

// ApplyTo applies the scheduler options to the given scheduler app configuration.
func (o *Options) ApplyTo(c *deschedulerappconfig.Config) error {
	if len(o.ConfigFile) == 0 {
		o.ApplyLeaderElectionTo(o.ComponentConfig)
		c.ComponentConfig = *o.ComponentConfig
	} else {
		cfg, err := loadConfigFromFile(o.ConfigFile)
		if err != nil {
			return err
		}
		// If the --config arg is specified, honor the leader election CLI args only.
		o.ApplyLeaderElectionTo(cfg)

		if err := validation.ValidateDeschedulerConfiguration(cfg); err != nil {
			return err
		}

		c.ComponentConfig = *cfg

		// use the loaded config file only, with the exception of --address and --port.
		if err := o.CombinedInsecureServing.ApplyToFromLoadedConfig(c, &c.ComponentConfig); err != nil {
			return err
		}
	}

	if err := o.SecureServing.ApplyTo(&c.SecureServing, &c.LoopbackClientConfig); err != nil {
		return err
	}
	o.Metrics.Apply()

	return nil
}

// Validate validates all the required options.
func (o *Options) Validate() []error {
	var errs []error

	if err := validation.ValidateDeschedulerConfiguration(o.ComponentConfig); err != nil {
		errs = append(errs, err.Errors()...)
	}
	errs = append(errs, o.SecureServing.Validate()...)
	errs = append(errs, o.CombinedInsecureServing.Validate()...)
	errs = append(errs, o.Metrics.Validate()...)

	return errs
}

// Config return a scheduler config object
func (o *Options) Config() (*deschedulerappconfig.Config, error) {
	c := &deschedulerappconfig.Config{}
	if err := o.ApplyTo(c); err != nil {
		return nil, err
	}

	// Prepare kube config.
	kubeConfig, err := createKubeConfig(c.ComponentConfig.ClientConnection, "")
	if err != nil {
		return nil, err
	}

	// Prepare kube clients.
	client, eventClient, err := createClients(kubeConfig)
	if err != nil {
		return nil, err
	}

	c.EventBroadcaster = events.NewEventBroadcasterAdapter(eventClient)

	// Set up leader election if enabled.
	var leaderElectionConfig *leaderelection.LeaderElectionConfig
	if c.ComponentConfig.LeaderElection.LeaderElect {
		// Use the scheduler name in the first profile to record leader election.
		name := "koord-descheduler"
		if len(c.ComponentConfig.Profiles) != 0 {
			name = c.ComponentConfig.Profiles[0].Name
		}
		coreRecorder := c.EventBroadcaster.DeprecatedNewLegacyRecorder(name)
		leaderElectionConfig, err = makeLeaderElectionConfig(c.ComponentConfig.LeaderElection, kubeConfig, coreRecorder)
		if err != nil {
			return nil, err
		}
	}

	mgrKubeConfig := *kubeConfig
	mgrKubeConfig.ContentType = ""
	mgrKubeConfig.AcceptContentTypes = ""
	mgr, err := ctrl.NewManager(&mgrKubeConfig, ctrl.Options{
		Scheme:                 scheme,
		MetricsBindAddress:     "0",
		HealthProbeBindAddress: "0",
		LeaderElection:         false,
		SyncPeriod:             nil,
		NewClient:              utilclient.NewClient,
	})
	if err != nil {
		return nil, fmt.Errorf("unable to start manager, err: %w", err)
	}

	c.Manager = mgr
	c.Client = client
	c.KubeConfig = kubeConfig
	c.InformerFactory = informers.NewSharedInformerFactory(mgr, 0)
	dynClient := dynamic.NewForConfigOrDie(kubeConfig)
	c.DynInformerFactory = dynamicinformer.NewFilteredDynamicSharedInformerFactory(dynClient, 0, corev1.NamespaceAll, nil)
	c.LeaderElection = leaderElectionConfig

	return c, nil
}

// makeLeaderElectionConfig builds a leader election configuration. It will
// create a new resource lock associated with the configuration.
func makeLeaderElectionConfig(config componentbaseconfig.LeaderElectionConfiguration, kubeConfig *restclient.Config, recorder record.EventRecorder) (*leaderelection.LeaderElectionConfig, error) {
	hostname, err := os.Hostname()
	if err != nil {
		return nil, fmt.Errorf("unable to get hostname: %v", err)
	}
	// add a uniquifier so that two processes on the same host don't accidentally both become active
	id := hostname + "_" + string(uuid.NewUUID())

	rl, err := resourcelock.NewFromKubeconfig(config.ResourceLock,
		config.ResourceNamespace,
		config.ResourceName,
		resourcelock.ResourceLockConfig{
			Identity:      id,
			EventRecorder: recorder,
		},
		kubeConfig,
		config.RenewDeadline.Duration)
	if err != nil {
		return nil, fmt.Errorf("couldn't create resource lock: %v", err)
	}

	return &leaderelection.LeaderElectionConfig{
		Lock:            rl,
		LeaseDuration:   config.LeaseDuration.Duration,
		RenewDeadline:   config.RenewDeadline.Duration,
		RetryPeriod:     config.RetryPeriod.Duration,
		WatchDog:        leaderelection.NewLeaderHealthzAdaptor(time.Second * 20),
		Name:            "koord-descheduler",
		ReleaseOnCancel: true,
	}, nil
}

// createKubeConfig creates a kubeConfig from the given config and masterOverride.
func createKubeConfig(config componentbaseconfig.ClientConnectionConfiguration, masterOverride string) (*restclient.Config, error) {
	kubeConfig, err := clientcmd.BuildConfigFromFlags(masterOverride, config.Kubeconfig)
	if err != nil {
		return nil, err
	}

	kubeConfig.DisableCompression = true
	kubeConfig.AcceptContentTypes = config.AcceptContentTypes
	kubeConfig.ContentType = config.ContentType
	kubeConfig.QPS = config.QPS
	kubeConfig.Burst = int(config.Burst)

	return kubeConfig, nil
}

// createClients creates a kube client and an event client from the given kubeConfig
func createClients(kubeConfig *restclient.Config) (clientset.Interface, clientset.Interface, error) {
	client, err := clientset.NewForConfig(restclient.AddUserAgent(kubeConfig, "koord-descheduler"))
	if err != nil {
		return nil, nil, err
	}

	eventClient, err := clientset.NewForConfig(kubeConfig)
	if err != nil {
		return nil, nil, err
	}

	return client, eventClient, nil
}
