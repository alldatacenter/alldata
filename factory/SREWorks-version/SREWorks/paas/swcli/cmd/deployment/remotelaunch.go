package deployment

import (
	"fmt"
	jsonvalue "github.com/Andrew-M-C/go.jsonvalue"
	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
	"os"
	"strings"
)

func initRemoteLaunchCommand() {
	remoteLaunchCommand.Flags().String("unit-id", "", "target unit-id")
	remoteLaunchCommand.Flags().String("app-id", "", "app id")
	remoteLaunchCommand.Flags().StringArrayP("tags", "t", []string{}, "tags for app package")
	remoteLaunchCommand.Flags().String("package-version", "", "which package version to select")
	remoteLaunchCommand.Flags().String("path", "", "configuration file path")
	remoteLaunchCommand.Flags().String("arch", "", "arch")
	remoteLaunchCommand.Flags().String("cluster", "", "cluster")
	remoteLaunchCommand.Flags().BoolP("wait", "w", true, "wait until finished")
	remoteLaunchCommand.MarkFlagRequired("unit-id")
	remoteLaunchCommand.MarkFlagRequired("app-id")
	remoteLaunchCommand.MarkFlagRequired("path")
	remoteLaunchCommand.MarkFlagRequired("arch")
	remoteLaunchCommand.MarkFlagRequired("cluster")
	remoteLaunchCommand.MarkFlagRequired("wait")
	viper.BindPFlag("deployment.remote-launch.unit-id", remoteLaunchCommand.Flags().Lookup("unit-id"))
	viper.BindPFlag("deployment.remote-launch.app-id", remoteLaunchCommand.Flags().Lookup("app-id"))
	viper.BindPFlag("deployment.remote-launch.tags", remoteLaunchCommand.Flags().Lookup("tags"))
	viper.BindPFlag("deployment.remote-launch.package-version", remoteLaunchCommand.Flags().Lookup("package-version"))
	viper.BindPFlag("deployment.remote-launch.path", remoteLaunchCommand.Flags().Lookup("path"))
	viper.BindPFlag("deployment.remote-launch.arch", remoteLaunchCommand.Flags().Lookup("arch"))
	viper.BindPFlag("deployment.remote-launch.cluster", remoteLaunchCommand.Flags().Lookup("cluster"))
	viper.BindPFlag("deployment.remote-launch.wait", remoteLaunchCommand.Flags().Lookup("wait"))
}

var remoteLaunchCommand = &cobra.Command{
	Use:   "remote-launch",
	Short: "deploy the application package in the specified unit",
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

		unitId := viper.GetString("deployment.remote-launch.unit-id")
		appId := viper.GetString("deployment.remote-launch.app-id")
		tags := viper.GetStringSlice("deployment.remote-launch.tags")
		packageVersion := viper.GetString("deployment.remote-launch.package-version")
		path := viper.GetString("deployment.remote-launch.path")
		arch := viper.GetString("deployment.remote-launch.arch")
		cluster := viper.GetString("deployment.remote-launch.cluster")
		wait := viper.GetBool("deployment.remote-launch.wait")

		params := map[string]string{}
		if len(tags) > 0 {
			for _, tag := range tags {
				if strings.HasPrefix(tag, " ") || strings.HasSuffix(tag, " ") || strings.Count(tag, "=") != 1 {
					_, err := fmt.Fprintf(os.Stderr, "invalid tag %s. example: 'build=develop'\n", tag)
					cobra.CheckErr(err)
					lib.Exit()
				}
			}
			params["tagList"] = strings.Join(tags, ",")
		} else {
			params["packageVersion"] = packageVersion
		}
		response, err := server.RemoteLaunch(unitId, appId, params, path, arch, cluster, jsonvalue.NewArray(), wait)
		if response != nil {
			lib.SwPrint(response.MustMarshal())
			if err != nil {
				lib.Exit()
			}
		} else {
			log.Error().Err(err).Msgf("deploy the application package in the specified unit failed")
			lib.Exit()
		}
	},
}
