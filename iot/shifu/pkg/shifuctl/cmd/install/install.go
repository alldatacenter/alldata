package install

import (
	"bufio"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"time"

	"github.com/briandowns/spinner"
	"github.com/spf13/cobra"
)

var InstallCmd = &cobra.Command{
	Use:   "install",
	Short: "install shifu and its dependencies",
	Run: func(cmd *cobra.Command, args []string) {
		install()
	},
}

// install installs shifu and its dependencies.
func install() {
	// start a spinner
	s := spinner.New(spinner.CharSets[35], 200*time.Millisecond)
	s.Prefix = "\033[1;33mChecking Shifu prerequisites...\033[0m"
	s.Suffix = "\n"
	s.Start()

	prompt := ""
	prompt = prompt + verifyDockerInstallation()
	prompt = prompt + verifyDockerHubConnection()
	prompt = prompt + verifyKubectlInstallation()
	prompt = prompt + verifyHelmInstallation()
	prompt = prompt + verifyKubernetesCluster()
	// If there's no error, then prompt is empty
	if prompt == "" {
		prompt = "\033[1;32mPrerequisite met, automatically installing Shifu!\033[0m\n"
	}
	fmt.Println(prompt)

	s.Stop()

	if err := installShifu(); err != nil {
		fmt.Println("\033[1;31mError installing Shifu\033[0m, Error: ", err)
		panic(err)
	}

	fmt.Println("\033[1;32mCongratulations! Shifu installed successfully!\033[0m")
}

// verifyDockerInstallation verifies if docker is properly installed.
func verifyDockerInstallation() string {
	s := ""

	// Run the "docker" command with the "ps" subcommand to get the list of containers
	_, err := exec.Command("docker", "ps").Output()
	if err != nil {
		s = s + "\033[1;31mError: Docker not installed properly for the current user. Error running: docker ps\033[0m\n"
		s = s + "\033[1;33mTo install Docker, run: curl -fsSL https://get.docker.com | sh\033[0m\n"
	}

	return s
}

// verifyDockerHubConnection verifies if the program is able to pull images from docker hub.
func verifyDockerHubConnection() string {
	s := ""

	// Run the "docker" command with the "pull" subcommand to pull the "hello-world" image
	_, err := exec.Command("docker", "pull", "hello-world").Output()
	if err != nil {
		s = s + "\033[1;31mError: Unable to establish a connection to Docker Hub. Error running: docker pull hello-world\033[0m\n"
		s = s + "\033[1;33mTo fix this issue, check your internet connection and run: docker login\033[0m\n"
	}

	return s
}

// verifyKubectlInstallation verifies if kubectl is properly installed.
func verifyKubectlInstallation() string {
	s := ""

	// Run the "kubectl" command with the "version" subcommand to get the version of kubectl
	_, err := exec.Command("kubectl", "version").Output()
	if err != nil {
		s = s + "\033[1;31mKubectl not installed properly for the current user. Error running: kubectl version\033[0m\n"
		s = s + "\033[1;33mTo install kubectl, follow the instructions at: https://kubernetes.io/docs/tasks/tools/install-kubectl/\033[0m\n"
	}

	return s
}

// verifyHelmInstallation verifies if helm is properly installed.
func verifyHelmInstallation() string {
	s := ""

	// Run the "helm" command with the "version" subcommand to get the version of helm
	_, err := exec.Command("helm", "version").Output()
	if err != nil {
		s = s + "\033[1;31mHelm not installed properly for the current user. Error running: helm version\033[0m\n"
		s = s + "\033[1;33mTo install helm, follow the instructions at: https://helm.sh/docs/intro/install/\033[0m\n"
	}

	return s
}

// verifyKubernetesCluster verifies if there's a kubernetes cluster running.
func verifyKubernetesCluster() string {
	s := ""

	// Run the "kubectl" command with the "get" subcommand to get the list of pods
	_, err := exec.Command("kubectl", "get", "pods").Output()
	if err != nil {
		s = s + "\033[1;31mError: Kubernetes cluster not running. Error running: kubectl get pods\033[0m\n"
		s = s + "\033[1;33mTo install Kubernetes, follow the instructions at: https://kubernetes.io/docs/setup/\033[0m\n"
	}

	return s
}

func installShifu() error {
	// start a spinner
	s := spinner.New(spinner.CharSets[35], 200*time.Millisecond)
	s.Start()

	//read version.txt
	versionFilePath := filepath.Join(os.Getenv("SHIFU_ROOT_DIR"), "version.txt")
	versionFile, err := os.Open(versionFilePath)
	if err != nil {
		return err
	}

	//read version.txt
	version := ""
	scanner := bufio.NewScanner(versionFile)
	for scanner.Scan() {
		version = scanner.Text()
	}

	// Run the "kubectl" command with the "apply" subcommand to install shifu
	_, err = exec.Command(
		"kubectl",
		"apply",
		"-f",
		"https://raw.githubusercontent.com/Edgenesis/shifu/"+version+"/pkg/k8s/crd/install/shifu_install.yml",
	).Output()
	if err != nil {
		return err
	}

	// Run the "kubectl" command with the "wait" subcommand to wait for shifu to be available
	_, err = exec.Command(
		"kubectl",
		"wait",
		"-n",
		"shifu-crd-system",
		"--for=condition=available",
		"--timeout=600s",
		"deployment/shifu-crd-controller-manager",
	).Output()

	s.Stop()

	return err
}
