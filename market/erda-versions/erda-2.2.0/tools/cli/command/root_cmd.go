// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package command

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/url"
	"os"
	"os/exec"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/gogap/errors"
	"github.com/sirupsen/logrus"
	"github.com/spf13/cobra"

	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/terminal/color_str"
	"github.com/erda-project/erda/tools/cli/status"
	"github.com/erda-project/erda/tools/cli/utils"
)

var (
	host         string // erda host, format: http[s]://<domain> eg: https://erda.cloud
	Remote       string // git remote name for erda repo
	username     string
	password     string
	debugMode    bool
	Interactive  bool
	IsCompletion bool
)

// Cmds which not require login
var (
	loginWhiteList = []string{
		"completion",
		"ext retag",
		"migrate",
		"migrate lint",
		"migrate mkpy",
		"migrate mkpypkg",
		"migrate record",
		"pipeline",
		"version",
		"help",
		"help [command]",
	}
)

// rootCmd represents the base command when called without any subcommands
var RootCmd = &cobra.Command{
	Use:   "erda-cli",
	Short: "Erda commandline client",
	Long: `
    _/_/_/_/       _/_/_/        _/_/_/          _/_/    
   _/             _/    _/      _/    _/      _/    _/   
  _/_/_/         _/_/_/        _/    _/      _/_/_/_/    
 _/             _/    _/      _/    _/      _/    _/     
_/_/_/_/       _/    _/      _/_/_/        _/    _/      
`,
	SilenceUsage:      true,
	PersistentPreRunE: PrepareCtx,
}

func PrepareCtx(cmd *cobra.Command, args []string) error {
	logrus.SetOutput(os.Stdout)
	var err error
	defer func() {
		cmd.SilenceErrors = true
	}()
	defer func() {
		if !IsCompletion && err != nil {
			fmt.Println(err)
		}
	}()

	ctx.Debug = debugMode
	httpOption := []httpclient.OpOption{httpclient.WithCompleteRedirect()}
	if debugMode {
		logrus.SetLevel(logrus.DebugLevel)
		httpOption = append(httpOption, httpclient.WithDebug(os.Stdout))
	} else if Interactive {
		httpOption = append(httpOption, httpclient.WithLoadingPrint(""))
	}
	if strings.HasPrefix(host, "https") {
		httpOption = append(httpOption, httpclient.WithHTTPS())
	}
	ctx.HttpClient = httpclient.New(httpOption...)

	u, err := getFullUse(cmd)
	if err != nil {
		err = fmt.Errorf(color_str.Red("✗ ") + err.Error())
		return err
	}

	// For completion zsh etc.
	if strings.HasPrefix(u, "completion ") || strings.HasPrefix(u, "__complete") {
		return nil
	}
	for _, w := range loginWhiteList {
		if w == u {
			return nil
		}
	}

	if strings.HasPrefix(u, "clone") || strings.HasPrefix(u, "push") {
		u, err := url.Parse(args[0])
		if err != nil {
			return err
		}
		host = fmt.Sprintf("%s://%s", u.Scheme, u.Host)
	}

	// parse and use context according to host param or config file
	if err := parseCtx(); err != nil {
		err = fmt.Errorf(color_str.Red("✗ ") + err.Error())
		return err
	}

	sessionInfos, err := ensureSessionInfos()
	if err != nil {
		err = fmt.Errorf(color_str.Red("✗ ") + err.Error())
		return err
	}
	ctx.Sessions = sessionInfos

	return nil
}

func getFullUse(cmd *cobra.Command) (string, error) {
	if cmd.HasParent() {
		pUse, err := getFullUse(cmd.Parent())
		if err != nil {
			return "", err
		}

		return strings.TrimSpace(strings.Join([]string{
			strings.TrimSpace(pUse), strings.TrimSpace(cmd.Use),
		}, " ")), nil
	}

	return "", nil
}

func ensureSessionInfos() (map[string]status.StatusInfo, error) {
	sessionInfos, err := status.GetSessionInfos()
	if err != nil && err != utils.NotExist {
		return nil, err
	}
	// file ~/.erda.d/sessions exist & and session for host also exist; otherwise need login fisrt
	currentSession, ok := sessionInfos[ctx.CurrentOpenApiHost]
	if ok {
		// check session if expired
		if currentSession.ExpiredAt != nil && time.Now().Before(*currentSession.ExpiredAt) {
			return sessionInfos, nil
		}
	}

	if Interactive {
		if username == "" {
			username = utils.InputNormal("Enter your erda username: ")
		}
		if password == "" {
			password = utils.InputPWD("Enter your erda password: ")
		}
	}

	if username != "" && password != "" {
		// fetch session & user info according to host, username & password
		if err = loginAndStoreSession(ctx.CurrentOpenApiHost, username, password); err != nil {
			return nil, err
		}

		// fetch sessions again
		sessionInfos, err = status.GetSessionInfos()
		if err != nil {
			return nil, err
		}
	} else {
		return nil, errors.Errorf("session expired at %s", currentSession.ExpiredAt.String())
	}

	return sessionInfos, nil
}

func parseCtx() error {
	if host == "" {
		_, config, err := GetProjectConfig()
		if err != nil && err != utils.NotExist {
			return err
		}
		if err == nil {
			host = config.Server
			ctx.CurrentOrg.ID = config.OrgID
			ctx.CurrentOrg.Name = config.Org
			ctx.CurrentProject.ProjectID = config.ProjectID
			ctx.CurrentProject.Project = config.Project
			for _, a := range config.Applications {
				a2 := ApplicationInfo{
					a.Application,
					a.ApplicationID,
					a.Mode,
					a.Desc,
					a.Sonarhost,
					a.Sonartoken,
					a.Sonarproject,
				}
				ctx.Applications = append(ctx.Applications, a2)
			}
		}

		if _, err := os.Stat(".git"); err == nil {
			// fetch host from git remote url
			info, err := utils.GetWorkspaceInfo(".", Remote)
			for _, a := range ctx.Applications {
				if a.Application == info.Application {
					ctx.CurrentApplication.Application = a.Application
					ctx.CurrentApplication.ApplicationID = a.ApplicationID
				}
			}

			if err != nil && err != utils.InvalidErdaRepo {
				return err
			}

			if err == nil {
				if host == "" {
					host = fmt.Sprintf("%s://%s", info.Scheme, info.Host)

					if username == "" || password == "" {
						gitCredentialStorage := fetchGitCredentialStorage()
						switch gitCredentialStorage {
						case "osxkeychain", "store":
							// fetch username & password from osxkeychain
							username, password, _ = fetchGitUserInfo(info.Host, gitCredentialStorage)
						}
					}
				} else {
					if !strings.Contains(host, info.Host) {
						fmt.Println(color_str.Yellow(
							fmt.Sprintf("current git repo remote %s: %s, different from config: %s",
								Remote, info.Scheme+"://"+info.Host, host)))
					}
				}
			}
		}

		if host == "" {
			if Interactive {
				// fetch host from stdin
				fmt.Print("Enter a erda host: ")
				fmt.Scanln(&host)
			} else {
				return errors.New("Not set a erda host")
			}
		}
	}

	slashIndex := strings.Index(host, "://")
	if slashIndex < 0 {
		return errors.Errorf("invalid host format, it should be http[s]://<domain>")
	}

	openAPIAddr := host
	if strings.HasSuffix(host, ".dev.terminus.io") {
		openAPIAddr = "https://openapi.dev.terminus.io"
	} else if strings.HasSuffix(host, ".daily.terminus.io") {
		openAPIAddr = "https://openapi.daily.terminus.io"
	} else if strings.HasSuffix(host, ".gts.terminus.io") {
		openAPIAddr = "https://openapi.gts.terminus.io"
	} else {
		orgIndex := strings.Index(host, "-org.")
		if orgIndex != -1 {
			openAPIAddr = host[:slashIndex+3] + host[orgIndex+5:]
		}

		oneIndex := strings.Index(openAPIAddr, "://one.")
		if oneIndex != -1 {
			openAPIAddr = openAPIAddr[:oneIndex+3] + openAPIAddr[oneIndex+7:]
		}

		hostHasOpenApi := strings.Index(openAPIAddr, "openapi.") != -1
		if strings.HasPrefix(host, "https") {
			if !hostHasOpenApi {
				openAPIAddr = "https://openapi." + openAPIAddr[slashIndex+3:]
			}
		} else {
			if !hostHasOpenApi {
				openAPIAddr = "http://openapi." + openAPIAddr[slashIndex+3:]
			}
		}
	}

	logrus.Debugf("openapi addr: %s", openAPIAddr)
	ctx.CurrentOpenApiHost = openAPIAddr

	return nil
}

func fetchGitCredentialStorage() string {
	c, err := exec.Command("git", "config", "credential.helper").Output()
	if err != nil {
		fmt.Printf("fetch git credential err: %v", err)
		return ""
	}

	return strings.TrimSuffix(string(c), "\n")
}

func fetchGitUserInfo(host, credentialStorage string) (string, string, error) {
	c1 := exec.Command("echo", fmt.Sprintf("host=%s", host))
	c2 := exec.Command("git", fmt.Sprintf("credential-%s", credentialStorage), "get")

	rs, err := utils.PipeCmds(c1, c2)
	if err != nil {
		return "", "", err
	}

	sl := strings.Split(strings.TrimSpace(rs), "\n")
	if len(sl) < 2 {
		return "", "", errors.New("Get user info from git failed")
	}

	var (
		username string
		password string
	)
	for _, v := range sl {
		if strings.HasPrefix(v, "username=") {
			username = strings.TrimSpace(strings.SplitN(v, "=", 2)[1])
		} else if strings.HasPrefix(v, "password=") {
			password = strings.TrimSpace(strings.SplitN(v, "=", 2)[1])
		}
	}

	return username, password, nil
}

func loginAndStoreSession(host, username, password string) error {
	form := make(url.Values)
	form.Set("username", username)
	form.Set("password", password)

	logrus.Debugf("current ctx: %+v", ctx)
	var body bytes.Buffer
	res, err := ctx.Post().Path("/login").FormBody(form).Do().Body(&body)
	if err != nil {
		return fmt.Errorf(utils.FormatErrMsg("login", "error: "+err.Error(), false))
	}
	if !res.IsOK() {
		return fmt.Errorf(utils.FormatErrMsg("login",
			"failed to login, status code: "+strconv.Itoa(res.StatusCode()), false))
	}
	var s status.StatusInfo
	d := json.NewDecoder(&body)
	if err := d.Decode(&s); err != nil {
		return fmt.Errorf(utils.FormatErrMsg(
			"login", "failed to  decode login response ("+err.Error()+")", false))
	}
	// 从 openapi 获取的 session 无过期时间，暂设 12 小时，小于 openapi 的 24 小时
	expiredAt := time.Now().Add(time.Hour * 12)
	s.ExpiredAt = &expiredAt

	if err := status.StoreSessionInfo(host, s); err != nil {
		return err
	}
	return nil
}

// Execute adds all child commands to the root command and sets flags appropriately.
// This is called by main.main(). It only needs to happen once to the rootCmd.
func Execute() {
	if Interactive {
		// hind cursor
		tput("civis")
		// unhind cursor
		defer tput("cnorm")
	}

	c := make(chan os.Signal, 1)
	signal.Notify(c, syscall.SIGINT, syscall.SIGQUIT, syscall.SIGKILL, syscall.SIGTERM)
	if Interactive {
		go func() {
			for range c {
				tput("cnorm")
				os.Exit(1)
			}
		}()
	}

	RootCmd.PersistentFlags().StringVar(&host, "host", "", "Erda host to visit (e.g. https://erda.cloud)")
	RootCmd.PersistentFlags().StringVarP(&Remote, "remote", "", "origin", "the remote for Erda repo")
	RootCmd.PersistentFlags().StringVarP(&username, "username", "u", "", "Erda username to authenticate")
	RootCmd.PersistentFlags().StringVarP(&password, "password", "p", "", "Erda password to authenticate")
	RootCmd.PersistentFlags().BoolVarP(&debugMode, "verbose", "V", false, "if true, enable verbose mode")
	RootCmd.PersistentFlags().BoolVarP(&Interactive, "interactive", "", true, "if true, interactive with user")

	RootCmd.Execute()
}

func tput(arg string) error {
	cmd := exec.Command("tput", arg)
	cmd.Stdout = os.Stdout
	return cmd.Run()
}
