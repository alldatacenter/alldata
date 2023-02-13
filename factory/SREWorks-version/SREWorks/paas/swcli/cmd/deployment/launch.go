package deployment

import (
	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
)

func initLaunchCommand() {
	launchCommand.Flags().String("app-id", "", "app id")
	launchCommand.Flags().Int64("app-package-id", 0, "app package id")
	launchCommand.Flags().String("path", "", "configuration file path")
	launchCommand.Flags().String("arch", "", "arch")
	launchCommand.Flags().String("cluster", "", "cluster")
	launchCommand.Flags().String("namespace", "", "namespace")
	launchCommand.Flags().String("stage", "", "stage")
	launchCommand.Flags().String("app-instance-name", "", "app instance name")
	launchCommand.Flags().BoolP("wait", "w", true, "wait until finished")
	launchCommand.MarkFlagRequired("app-id")
	launchCommand.MarkFlagRequired("app-package-id")
	launchCommand.MarkFlagRequired("path")
	launchCommand.MarkFlagRequired("arch")
	launchCommand.MarkFlagRequired("cluster")
	launchCommand.MarkFlagRequired("wait")
	viper.BindPFlag("deployment.launch.app-id", launchCommand.Flags().Lookup("app-id"))
	viper.BindPFlag("deployment.launch.app-package-id", launchCommand.Flags().Lookup("app-package-id"))
	viper.BindPFlag("deployment.launch.path", launchCommand.Flags().Lookup("path"))
	viper.BindPFlag("deployment.launch.arch", launchCommand.Flags().Lookup("arch"))
	viper.BindPFlag("deployment.launch.cluster", launchCommand.Flags().Lookup("cluster"))
	viper.BindPFlag("deployment.launch.namespace", launchCommand.Flags().Lookup("namespace"))
	viper.BindPFlag("deployment.launch.stage", launchCommand.Flags().Lookup("stage"))
	viper.BindPFlag("deployment.launch.app-instance-name", launchCommand.Flags().Lookup("app-instance-name"))
	viper.BindPFlag("deployment.launch.wait", launchCommand.Flags().Lookup("wait"))
}

var launchCommand = &cobra.Command{
	Use:   "launch",
	Short: "deploy the application package",
	Run: func(cmd *cobra.Command, args []string) {
		endpoint := viper.GetString("endpoint")
		clientId := viper.GetString("client-id")
		clientSecret := viper.GetString("client-secret")
		username := viper.GetString("username")
		password := viper.GetString("password")
		server := appmanagerv1.NewAppManagerServer(
			lib.NewClient(endpoint, clientId, clientSecret, username, password),
			endpoint,
		)

		appId := viper.GetString("deployment.launch.app-id")
		appPackageId := viper.GetInt64("deployment.launch.app-package-id")
		path := viper.GetString("deployment.launch.path")
		arch := viper.GetString("deployment.launch.arch")
		cluster := viper.GetString("deployment.launch.cluster")
		namespace := viper.GetString("deployment.launch.namespace")
		stage := viper.GetString("deployment.launch.stage")
		appInstanceName := viper.GetString("deployment.launch.app-instance-name")
		wait := viper.GetBool("deployment.launch.wait")
		response, err := server.Launch(appId, appPackageId, path, arch, cluster, namespace, stage, appInstanceName, wait)
		if response != nil {
			lib.SwPrint(response.MustMarshal())
			if wait {
				deployStatus, err := response.GetString("deployStatus")
				if err != nil {
					log.Error().Err(err).Msgf("cannot get deployStatus from launch response")
					lib.Exit()
				}
				if deployStatus != "SUCCESS" && deployStatus != "RUNNING" {
					lib.Exit()
				}
			}
		} else {
			log.Error().Err(err).Msgf("launch deployment failed")
			lib.Exit()
		}
	},
}
