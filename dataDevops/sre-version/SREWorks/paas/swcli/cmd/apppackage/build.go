package apppackage

import (
	"fmt"
	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
	"os"
	"path/filepath"
	"strings"
)

func initBuildCommand() {
	buildCommand.Flags().StringP("app-id", "a", "", "which app id to build")
	buildCommand.Flags().StringArrayP("tags", "t", []string{}, "tags for app package created")
	buildCommand.Flags().BoolP("wait", "w", true, "wait until finished")
	buildCommand.Flags().BoolP("disable-dir-check", "", false, "disable directory check")
	buildCommand.Flags().StringP("path", "p", "", "build yaml path (default build.yaml)")
	buildCommand.Flags().StringP("branch", "b", "", "overwrite default build branch")
	buildCommand.MarkFlagRequired("app-id")
	buildCommand.MarkFlagRequired("tags")
	viper.BindPFlag("app-package.build.app-id", buildCommand.Flags().Lookup("app-id"))
	viper.BindPFlag("app-package.build.tags", buildCommand.Flags().Lookup("tags"))
	viper.BindPFlag("app-package.build.wait", buildCommand.Flags().Lookup("wait"))
	viper.BindPFlag("app-package.build.disable-dir-check", buildCommand.Flags().Lookup("disable-dir-check"))
	viper.BindPFlag("app-package.build.path", buildCommand.Flags().Lookup("path"))
	viper.BindPFlag("app-package.build.branch", buildCommand.Flags().Lookup("branch"))
}

var buildCommand = &cobra.Command{
	Use:   "build",
	Short: "build app package by configuration",
	Run: func(cmd *cobra.Command, args []string) {
		appId := viper.GetString("app-package.build.app-id")
		tags := viper.GetStringSlice("app-package.build.tags")
		wait := viper.GetBool("app-package.build.wait")
		disableDirCheck := viper.GetBool("app-package.build.disable-dir-check")
		buildPath := viper.GetString("app-package.build.path")
		branch := viper.GetString("app-package.build.branch")

		// 检查当前的 appId 是否和当前目录保持一致
		currentDir, err := os.Getwd()
		cobra.CheckErr(err)
		currentBaseDir := filepath.Base(currentDir)
		if !disableDirCheck && appId != currentBaseDir {
			_, err = fmt.Fprintf(os.Stderr, "invalid app id, must equal to current directory name %s", currentBaseDir)
			cobra.CheckErr(err)
			lib.Exit()
		}
		if len(buildPath) == 0 {
			buildPath = filepath.Join(currentDir, "build.yaml")
		}

		// 检查 build.yaml 文件是否存在
		if _, err := os.Stat(buildPath); os.IsNotExist(err) {
			_, err = fmt.Fprintf(os.Stderr, "cannot find %s\n", buildPath)
			cobra.CheckErr(err)
			lib.Exit()
		}

		// Tags 检查
		for _, tag := range tags {
			if strings.HasPrefix(tag, " ") || strings.HasSuffix(tag, " ") || strings.Count(tag, "=") == 0 {
				_, err = fmt.Fprintf(os.Stderr, "invalid tag %s. example: 'build=develop'\n", tag)
				cobra.CheckErr(err)
				lib.Exit()
			}
		}

		// 发起构建流程
		endpoint := viper.GetString("endpoint")
		clientId := viper.GetString("client-id")
		clientSecret := viper.GetString("client-secret")
		username := viper.GetString("username")
		password := viper.GetString("password")
		server := appmanagerv1.NewAppManagerServer(
			lib.NewClient(endpoint, clientId, clientSecret, username, password),
			endpoint,
		)
		response, err := server.Build(appId, buildPath, tags, branch, wait)
		if response != nil {
			lib.SwPrint(response.MustMarshal())
			if wait {
				if _, err := response.GetInt64("appPackageId"); err != nil {
					log.Error().Err(err).Msgf("build app package failed")
					lib.Exit()
				}
			}
		} else {
			log.Error().Err(err).Msgf("build app package failed")
			lib.Exit()
		}
	},
}
