package apppackage

import (
	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
	"strings"
)

func initRemoteSyncCommand() {
	remoteSyncCommand.Flags().StringP("app-id", "a", "", "which app id to build")
	remoteSyncCommand.Flags().StringArrayP("tags", "t", []string{}, "tags for app package created")
	remoteSyncCommand.Flags().StringP("unit-id", "u", "", "target unit id")
	remoteSyncCommand.MarkFlagRequired("app-id")
	remoteSyncCommand.MarkFlagRequired("tags")
	remoteSyncCommand.MarkFlagRequired("unit-id")
	viper.BindPFlag("app-package.remote-sync.app-id", remoteSyncCommand.Flags().Lookup("app-id"))
	viper.BindPFlag("app-package.remote-sync.tags", remoteSyncCommand.Flags().Lookup("tags"))
	viper.BindPFlag("app-package.remote-sync.unit-id", remoteSyncCommand.Flags().Lookup("unit-id"))
}

var remoteSyncCommand = &cobra.Command{
	Use:   "remote-sync",
	Short: "sync app package to target unit",
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

		appId := viper.GetString("app-package.remote-sync.app-id")
		tags := viper.GetStringSlice("app-package.remote-sync.tags")
		unitId := viper.GetString("app-package.remote-sync.unit-id")
		appPackageId, err := server.GetAppPackageId(appId, map[string]string{
			"tagList": strings.Join(tags, ","),
		})
		if err != nil {
			log.Error().Err(err).Msgf("cannot get latest app package by tag %v", tags)
			lib.Exit()
		}
		response, err := server.RemoteSync(unitId, appId, appPackageId)
		if response != nil {
			log.Info().Msgf("app package has synced to target unit %s", unitId)
			lib.SwPrint(response.MustMarshal())
		} else {
			log.Error().Err(err).Msgf("remote sync app package failed")
			lib.Exit()
		}
	},
}
