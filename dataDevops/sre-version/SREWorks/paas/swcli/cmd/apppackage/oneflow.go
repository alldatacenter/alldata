package apppackage

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
)

func initOneflowCommand() {
	oneflowCommand.Flags().StringP("app-id", "a", "", "which app id to build")
	oneflowCommand.Flags().StringArrayP("tags", "t", []string{}, "tags for app package created")
	oneflowCommand.Flags().String("path", "", "configuration file path")
	oneflowCommand.Flags().String("arch", "", "arch")
	oneflowCommand.Flags().String("cluster", "", "cluster")
	oneflowCommand.Flags().String("namespace", "", "namespace")
	oneflowCommand.Flags().String("stage", "", "stage")
	oneflowCommand.Flags().String("app-instance-name", "", "app instance name")
	oneflowCommand.Flags().BoolP("disable-dir-check", "", false, "disable directory check")
	oneflowCommand.MarkFlagRequired("app-id")
	oneflowCommand.MarkFlagRequired("tags")
	oneflowCommand.MarkFlagRequired("path")
	oneflowCommand.MarkFlagRequired("arch")
	oneflowCommand.MarkFlagRequired("cluster")
	viper.BindPFlag("app-package.oneflow.app-id", oneflowCommand.Flags().Lookup("app-id"))
	viper.BindPFlag("app-package.oneflow.tags", oneflowCommand.Flags().Lookup("tags"))
	viper.BindPFlag("app-package.oneflow.path", oneflowCommand.Flags().Lookup("path"))
	viper.BindPFlag("app-package.oneflow.arch", oneflowCommand.Flags().Lookup("arch"))
	viper.BindPFlag("app-package.oneflow.cluster", oneflowCommand.Flags().Lookup("cluster"))
	viper.BindPFlag("app-package.oneflow.namespace", oneflowCommand.Flags().Lookup("namespace"))
	viper.BindPFlag("app-package.oneflow.stage", oneflowCommand.Flags().Lookup("stage"))
	viper.BindPFlag("app-package.oneflow.app-instance-name", oneflowCommand.Flags().Lookup("app-instance-name"))
	viper.BindPFlag("app-package.oneflow.disable-dir-check", oneflowCommand.Flags().Lookup("disable-dir-check"))
}

var oneflowCommand = &cobra.Command{
	Use:   "oneflow",
	Short: "build app package by configuration",
	Run: func(cmd *cobra.Command, args []string) {
		appId := viper.GetString("app-package.oneflow.app-id")
		tags := viper.GetStringSlice("app-package.oneflow.tags")
		launchPath := viper.GetString("app-package.oneflow.path")
		arch := viper.GetString("app-package.oneflow.arch")
		cluster := viper.GetString("app-package.oneflow.cluster")
		namespace := viper.GetString("app-package.oneflow.namespace")
		stage := viper.GetString("app-package.oneflow.stage")
		appInstanceName := viper.GetString("app-package.oneflow.app-instance-name")
		disableDirCheck := viper.GetBool("app-package.oneflow.disable-dir-check")

		// 检查当前的 appId 是否和当前目录保持一致
		currentDir, err := os.Getwd()
		cobra.CheckErr(err)
		currentBaseDir := filepath.Base(currentDir)
		if !disableDirCheck && appId != currentBaseDir {
			_, err = fmt.Fprintf(os.Stderr, "invalid app id, must equal to current directory name %s", currentBaseDir)
			cobra.CheckErr(err)
			lib.Exit()
		}

		// 检查 build.yaml 文件是否存在
		buildPath := filepath.Join(currentDir, "build.yaml")
		if _, err := os.Stat(buildPath); os.IsNotExist(err) {
			_, err = fmt.Fprintln(os.Stderr, "cannot find build.yaml in current directory")
			cobra.CheckErr(err)
			lib.Exit()
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
		response, err := server.Build(appId, buildPath, tags, "", true)
		if response != nil {
			lib.SwPrint(response.MustMarshal())
		} else {
			log.Error().Err(err).Msgf("build app package failed")
			lib.Exit()
		}

		// 获得 appPackageId
		appPackageId, err := response.GetInt64("appPackageId")
		cobra.CheckErr(err)

		// 发起部署流程
		response, err = server.Launch(appId, appPackageId, launchPath, arch, cluster, namespace, stage, appInstanceName, true)
		if response != nil {
			lib.SwPrint(response.MustMarshal())
			deployStatus, err := response.GetString("deployStatus")
			if err != nil {
				log.Error().Err(err).Msgf("cannot get deployStatus from launch response")
				lib.Exit()
			}
			if deployStatus != "SUCCESS" && deployStatus != "RUNNING" {
				lib.Exit()
			}
		} else {
			log.Error().Err(err).Msgf("launch deployment failed")
			lib.Exit()
		}
	},
}
