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

// Package app implements a Server object for running the scheduler.
package app

import (
	"context"
	"fmt"
	"net/http"
	"os"
	goruntime "runtime"

	"github.com/spf13/cobra"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/fields"
	utilerrors "k8s.io/apimachinery/pkg/util/errors"
	genericapifilters "k8s.io/apiserver/pkg/endpoints/filters"
	apirequest "k8s.io/apiserver/pkg/endpoints/request"
	"k8s.io/apiserver/pkg/server"
	genericfilters "k8s.io/apiserver/pkg/server/filters"
	"k8s.io/apiserver/pkg/server/healthz"
	"k8s.io/apiserver/pkg/server/mux"
	"k8s.io/apiserver/pkg/server/routes"
	"k8s.io/client-go/tools/events"
	"k8s.io/client-go/tools/leaderelection"
	"k8s.io/client-go/tools/record"
	cliflag "k8s.io/component-base/cli/flag"
	"k8s.io/component-base/cli/globalflag"
	"k8s.io/component-base/configz"
	"k8s.io/component-base/logs"
	"k8s.io/component-base/metrics/legacyregistry"
	"k8s.io/component-base/term"
	"k8s.io/component-base/version"
	"k8s.io/component-base/version/verflag"
	"k8s.io/klog/v2"
	"k8s.io/klog/v2/klogr"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"

	deschedulerappconfig "github.com/koordinator-sh/koordinator/cmd/koord-descheduler/app/config"
	"github.com/koordinator-sh/koordinator/cmd/koord-descheduler/app/options"
	"github.com/koordinator-sh/koordinator/pkg/descheduler"
	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	deschedulercontrollers "github.com/koordinator-sh/koordinator/pkg/descheduler/controllers"
	deschedulercontrollersoptions "github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/options"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/fieldindex"
	frameworkruntime "github.com/koordinator-sh/koordinator/pkg/descheduler/framework/runtime"
)

// Option configures a framework.Registry.
type Option func(frameworkruntime.Registry) error

// NewDeschedulerCommand creates a *cobra.Command object with default parameters and registryOptions
func NewDeschedulerCommand(registryOptions ...Option) *cobra.Command {
	opts := options.NewOptions()

	cmd := &cobra.Command{
		Use: "koord-descheduler",
		RunE: func(cmd *cobra.Command, args []string) error {
			return runCommand(cmd, opts, registryOptions...)
		},
		Args: func(cmd *cobra.Command, args []string) error {
			for _, arg := range args {
				if len(arg) > 0 {
					return fmt.Errorf("%q does not take any arguments, got %q", cmd.CommandPath(), args)
				}
			}
			return nil
		},
	}

	nfs := opts.Flags
	verflag.AddFlags(nfs.FlagSet("global"))
	globalflag.AddGlobalFlags(nfs.FlagSet("global"), cmd.Name())
	fs := cmd.Flags()
	for _, f := range nfs.FlagSets {
		fs.AddFlagSet(f)
	}

	cols, _, _ := term.TerminalSize(cmd.OutOrStdout())
	cliflag.SetUsageAndHelpFunc(cmd, *nfs, cols)

	if err := cmd.MarkFlagFilename("config", "yaml", "yml", "json"); err != nil {
		klog.ErrorS(err, "Failed to mark flag filename")
	}

	return cmd
}

// runCommand runs the scheduler.
func runCommand(cmd *cobra.Command, opts *options.Options, registryOptions ...Option) error {
	// Activate logging as soon as possible, after that
	// show flags with the final logging configuration.
	if err := opts.Logs.Validate(); err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}
	cliflag.PrintFlags(cmd.Flags())

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	go func() {
		stopCh := server.SetupSignalHandler()
		<-stopCh
		cancel()
	}()

	cc, desched, err := Setup(ctx, opts, registryOptions...)
	if err != nil {
		return err
	}

	return Run(ctx, cc, desched)
}

// Run executes the scheduler based on the given configuration. It only returns on error or when context is done.
func Run(ctx context.Context, cc *deschedulerappconfig.CompletedConfig, desched *descheduler.Descheduler) error {
	// To help debugging, immediately log version
	klog.InfoS("Starting Koordinator Descheduler", "version", version.Get())

	klog.InfoS("Golang settings", "GOGC", os.Getenv("GOGC"), "GOMAXPROCS", os.Getenv("GOMAXPROCS"), "GOTRACEBACK", os.Getenv("GOTRACEBACK"))

	// Configz registration.
	if cz, err := configz.New("componentconfig"); err == nil {
		cz.Set(cc.ComponentConfig)
	} else {
		return fmt.Errorf("unable to register configz: %s", err)
	}

	// Prepare the event broadcaster.
	cc.EventBroadcaster.StartRecordingToSink(ctx.Done())

	// Setup healthz checks.
	var checks []healthz.HealthChecker
	if cc.ComponentConfig.LeaderElection.LeaderElect {
		checks = append(checks, cc.LeaderElection.WatchDog)
	}

	// Start up the healthz server.
	if cc.InsecureServing != nil {
		handler := buildHandlerChain(newHealthzAndMetricsHandler(&cc.ComponentConfig, checks...))
		if err := cc.InsecureServing.Serve(handler, 0, ctx.Done()); err != nil {
			return fmt.Errorf("failed to start healthz server: %v", err)
		}
	}
	if cc.InsecureMetricsServing != nil {
		handler := buildHandlerChain(newHealthzAndMetricsHandler(&cc.ComponentConfig, checks...))
		if err := cc.InsecureMetricsServing.Serve(handler, 0, ctx.Done()); err != nil {
			return fmt.Errorf("failed to start metrics server: %v", err)
		}
	}

	// Start up the healthz server.
	if cc.SecureServing != nil {
		handler := buildHandlerChain(newHealthzAndMetricsHandler(&cc.ComponentConfig, checks...))
		// TODO: handle stoppedCh and listenerStoppedCh returned by c.SecureServing.Serve
		if _, err := cc.SecureServing.Serve(handler, 0, ctx.Done()); err != nil {
			// fail early for secure handlers, removing the old error loop from above
			return fmt.Errorf("failed to start secure server: %v", err)
		}
	}

	cc.Manager.Add(desched)

	// If leader election is enabled, runCommand via LeaderElector until done and exit.
	if cc.LeaderElection != nil {
		cc.LeaderElection.Callbacks = leaderelection.LeaderCallbacks{
			OnStartedLeading: func(ctx context.Context) {
				StartDescheduler(ctx, cc)
			},
			OnStoppedLeading: func() {
				select {
				case <-ctx.Done():
					// We were asked to terminate. Exit 0.
					klog.InfoS("Requested to terminate, exiting")
					os.Exit(0)
				default:
					// We lost the lock.
					klog.ErrorS(nil, "Leaderelection lost")
					klog.Flush()
				}
			},
		}
		leaderElector, err := leaderelection.NewLeaderElector(*cc.LeaderElection)
		if err != nil {
			return fmt.Errorf("couldn't create leader elector: %v", err)
		}

		leaderElector.Run(ctx)

		return fmt.Errorf("lost lease")
	}

	StartDescheduler(ctx, cc)
	return fmt.Errorf("finished without leader elect")
}

func StartDescheduler(ctx context.Context, cc *deschedulerappconfig.CompletedConfig) {
	// We don't need to start cc.InformerFactory because we adapt the InformerFactory to controller.Manager
	// if cc.InformerFactory != nil {
	// 	cc.InformerFactory.Start(ctx.Done())
	// }

	if cc.DynInformerFactory != nil {
		cc.DynInformerFactory.Start(ctx.Done())
		cc.DynInformerFactory.WaitForCacheSync(ctx.Done())
	}

	cc.Manager.Start(ctx)
}

// buildHandlerChain wraps the given handler with the standard filters.
func buildHandlerChain(handler http.Handler) http.Handler {
	requestInfoResolver := &apirequest.RequestInfoFactory{}
	handler = genericapifilters.WithRequestInfo(handler, requestInfoResolver)
	handler = genericapifilters.WithCacheControl(handler)
	handler = genericfilters.WithHTTPLogging(handler)
	handler = genericfilters.WithPanicRecovery(handler, requestInfoResolver)
	return handler
}

func installMetricHandler(pathRecorderMux *mux.PathRecorderMux) {
	configz.InstallHandler(pathRecorderMux)
	pathRecorderMux.Handle("/metrics", legacyregistry.HandlerWithReset())
}

// newHealthzAndMetricsHandler creates a healthz server from the config, and will also
// embed the metrics handler.
func newHealthzAndMetricsHandler(config *deschedulerconfig.DeschedulerConfiguration, checks ...healthz.HealthChecker) http.Handler {
	pathRecorderMux := mux.NewPathRecorderMux("koord-descheduler")
	healthz.InstallHandler(pathRecorderMux, checks...)
	installMetricHandler(pathRecorderMux)
	if config.EnableProfiling {
		routes.Profiling{}.Install(pathRecorderMux)
		if config.EnableContentionProfiling {
			goruntime.SetBlockProfileRate(1)
		}
		routes.DebugFlags{}.Install(pathRecorderMux, "v", routes.StringFlagPutHandler(logs.GlogSetter))
	}
	return pathRecorderMux
}

// Setup creates a completed config and a scheduler based on the command args and options
func Setup(ctx context.Context, opts *options.Options, outOfTreeRegistryOptions ...Option) (*deschedulerappconfig.CompletedConfig, *descheduler.Descheduler, error) {
	if errs := opts.Validate(); len(errs) > 0 {
		return nil, nil, utilerrors.NewAggregate(errs)
	}

	c, err := opts.Config()
	if err != nil {
		return nil, nil, err
	}

	// Get the completed config
	cc := c.Complete()

	deschedulercontrollersoptions.Manager = cc.Manager
	ctrl.SetLogger(klogr.New())

	if err = fieldindex.RegisterFieldIndexes(cc.Manager.GetCache()); err != nil {
		return nil, nil, fmt.Errorf("failed to register field index, err: %w", err)
	}

	outOfTreeRegistry := make(frameworkruntime.Registry)
	for _, option := range outOfTreeRegistryOptions {
		if err := option(outOfTreeRegistry); err != nil {
			return nil, nil, err
		}
	}
	if err := outOfTreeRegistry.Merge(deschedulercontrollers.NewControllerRegistry()); err != nil {
		return nil, nil, err
	}

	completedProfiles := make([]deschedulerconfig.DeschedulerProfile, 0)

	recorderFactory := func(name string) events.EventRecorder {
		return record.NewEventRecorderAdapter(cc.Manager.GetEventRecorderFor(name))
	}

	desched, err := descheduler.New(
		cc.Client,
		cc.InformerFactory,
		cc.DynInformerFactory,
		recorderFactory,
		ctx.Done(),
		descheduler.WithComponentConfigVersion(cc.ComponentConfig.TypeMeta.APIVersion),
		descheduler.WithKubeConfig(cc.KubeConfig),
		descheduler.WithProfiles(cc.ComponentConfig.Profiles...),
		descheduler.WithFrameworkOutOfTreeRegistry(outOfTreeRegistry),
		descheduler.WithDryRun(cc.ComponentConfig.DryRun),
		descheduler.WithDeschedulingInterval(cc.ComponentConfig.DeschedulingInterval.Duration),
		descheduler.WithNodeSelector(cc.ComponentConfig.NodeSelector),
		descheduler.WithPodAssignedToNodeFn(podAssignedToNode(cc.Manager.GetClient())),
		descheduler.WithBuildFrameworkCapturer(func(profile deschedulerconfig.DeschedulerProfile) {
			completedProfiles = append(completedProfiles, profile)
		}),
	)
	if err != nil {
		return nil, nil, err
	}

	if err := options.LogOrWriteConfig(opts.WriteConfigTo, &cc.ComponentConfig, completedProfiles); err != nil {
		return nil, nil, err
	}

	return &cc, desched, nil
}

func podAssignedToNode(clt client.Client) descheduler.PodAssignedToNodeFn {
	return func(nodeName string) ([]*corev1.Pod, error) {
		podList := &corev1.PodList{}
		err := clt.List(context.TODO(), podList, &client.ListOptions{FieldSelector: fields.OneTermEqualSelector(fieldindex.IndexPodByNodeName, nodeName)})
		if err != nil {
			return nil, err
		}
		if len(podList.Items) == 0 {
			return nil, nil
		}
		pods := make([]*corev1.Pod, 0, len(podList.Items))
		for i := range podList.Items {
			pods = append(pods, &podList.Items[i])
		}
		return pods, nil
	}
}
