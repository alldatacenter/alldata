package apppackage

import (
	"fmt"

	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
)

func initImportCommand() {
	importCommand.Flags().StringP("app-id", "a", "", "which app id to build")
	importCommand.Flags().StringP("filepath", "f", "", "zip file")
	importCommand.Flags().BoolP("reset-version", "r", false, "reset version when import")
	importCommand.Flags().BoolP("print-only-app-package-id", "", false, "")
	importCommand.MarkFlagRequired("app-id")
	importCommand.MarkFlagRequired("filepath")
	viper.BindPFlag("app-package.import.app-id", importCommand.Flags().Lookup("app-id"))
	viper.BindPFlag("app-package.import.filepath", importCommand.Flags().Lookup("filepath"))
	viper.BindPFlag("app-package.import.reset-version", importCommand.Flags().Lookup("reset-version"))
	viper.BindPFlag("app-package.import.print-only-app-package-id",
		importCommand.Flags().Lookup("print-only-app-package-id"))
}

var importCommand = &cobra.Command{
	Use:   "import",
	Short: "import app package to appmanager server",
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

		appId := viper.GetString("app-package.import.app-id")
		filepath := viper.GetString("app-package.import.filepath")
		resetVersion := viper.GetBool("app-package.import.reset-version")
		printOnlyAppPackageId := viper.GetBool("app-package.import.print-only-app-package-id")
		response, err := server.ImportPackage(appId, filepath, resetVersion)
		if response != nil {
			log.Info().Msgf("app package has imported to appmanager")
			if printOnlyAppPackageId {
				appPackageId, err := response.GetInt64("id")
				if err != nil {
					log.Error().Err(err).Msgf("cannot find app package id in appmanager response")
					lib.Exit()
				} else {
					fmt.Println(appPackageId)
				}
			} else {
				lib.SwPrint(response.MustMarshal())
			}
		} else {
			log.Error().Err(err).Msgf("import app package failed")
			lib.Exit()
		}
	},
}
