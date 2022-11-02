package apppackage

import (
	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
	"strings"
)

func initGetLatestCommand() {
	getLatestCommand.Flags().StringP("app-id", "a", "", "which app id to build")
	getLatestCommand.Flags().StringArrayP("tags", "t", []string{}, "tags for app package created")
	getLatestCommand.MarkFlagRequired("app-id")
	getLatestCommand.MarkFlagRequired("tags")
	viper.BindPFlag("app-package.get-latest.app-id", getLatestCommand.Flags().Lookup("app-id"))
	viper.BindPFlag("app-package.get-latest.tags", getLatestCommand.Flags().Lookup("tags"))
}

var getLatestCommand = &cobra.Command{
	Use: "get-latest",
	Short: "get latest app package",
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

		appId := viper.GetString("app-package.get-latest.app-id")
		tags := viper.GetStringSlice("app-package.get-latest.tags")
		params := map[string]string{
			"tagList": strings.Join(tags, ","),
		}
		appPackageId, err := server.GetAppPackageId(appId, params)
		if err != nil {
			log.Error().Err(err).Msgf("get app package id failed")
			lib.Exit()
		}
		response, err := server.GetAppPackage(appId, appPackageId)
		if response != nil {
			lib.SwPrint(response.MustMarshal())
		} else {
			log.Error().Err(err).Msgf("find latest app package failed")
			lib.Exit()
		}
	},
}