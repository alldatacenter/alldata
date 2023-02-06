#!/bin/bash

set -u

abort() {
  printf "%s\n" "$@"
  exit 1
}

if [ -z "${BASH_VERSION:-}" ]; then
  abort "Bash is required to interpret this script."
fi

# First check OS.
OS="$(uname)"
if [[ "$OS" == "Linux" ]]; then
  ON_LINUX=1
elif [[ "$OS" == "Darwin" ]]; then
  ON_MACOS=1
else
  abort "The script is only supported on macOS and Linux."
fi

if [[ -z "${ON_LINUX-}" ]]; then
  PERMISSION_FORMAT="%A"
  CHOWN="/usr/sbin/chown"
  CHGRP="/usr/bin/chgrp"
  GROUP="admin"
else
  PERMISSION_FORMAT="%a"
  CHOWN="/bin/chown"
  CHGRP="/bin/chgrp"
  GROUP="$(id -gn)"
fi

# string formatters
if [[ -t 1 ]]; then
  tty_escape() { printf "\033[%sm" "$1"; }
else
  tty_escape() { :; }
fi
tty_mkbold() { tty_escape "1;$1"; }
tty_underline="$(tty_escape "4;39")"
tty_blue="$(tty_mkbold 34)"
tty_red="$(tty_mkbold 31)"
tty_bold="$(tty_mkbold 39)"
tty_reset="$(tty_escape 0)"

have_sudo_access() {
  local -a args
  if [[ -n "${SUDO_ASKPASS-}" ]]; then
    args=("-A")
  elif [[ -n "${NONINTERACTIVE-}" ]]; then
    args=("-n")
  fi

  if [[ -z "${HAVE_SUDO_ACCESS-}" ]]; then
    if [[ -n "${args[*]-}" ]]; then
      SUDO="/usr/bin/sudo ${args[*]}"
    else
      SUDO="/usr/bin/sudo"
    fi
    if [[ -n "${NONINTERACTIVE-}" ]]; then
      ${SUDO} -l mkdir &>/dev/null
    else
      ${SUDO} -v && ${SUDO} -l mkdir &>/dev/null
    fi
    HAVE_SUDO_ACCESS="$?"
  fi

  if [[ -n "${ON_MACOS-}" ]] && [[ "$HAVE_SUDO_ACCESS" -ne 0 ]]; then
    abort "Need sudo access on macOS (e.g. the user $USER needs to be an Administrator)!"
  fi

  return "$HAVE_SUDO_ACCESS"
}

shell_join() {
  local arg
  printf "%s" "$1"
  shift
  for arg in "$@"; do
    printf " "
    printf "%s" "${arg// /\ }"
  done
}

ohai() {
  printf "${tty_blue}==>${tty_bold} %s${tty_reset}\n" "$(shell_join "$@")"
}

execute() {
  if ! "$@"; then
    abort "$(printf "Failed during: %s" "$(shell_join "$@")")"
  fi
}

execute_sudo() {
  local -a args=("$@")
  if have_sudo_access; then
    if [[ -n "${SUDO_ASKPASS-}" ]]; then
      args=("-A" "${args[@]}")
    fi
    ohai "/usr/bin/sudo" "${args[@]}"
    execute "/usr/bin/sudo" "${args[@]}"
  else
    ohai "${args[@]}"
    execute "${args[@]}"
  fi
}

check_docker_version() {
  DOCKER_VERSION=$(docker -v | grep -Eo "[0-9]+\.[0-9]+\.[0-9]+")
  if [[ -n $DOCKER_VERSION ]]; then
    IFS="." read -r DOCKER_MAJOR_VERSION DOCKER_MINIOR_VERSION DOCKER_BUILD_VERSION <<< "$DOCKER_VERSION"
    if ((DOCKER_MAJOR_VERSION < 20 || DOCKER_MAJOR_VERSION == 20 && DOCKER_MINIOR_VERSION < 10)); then
      abort "Docker version 20.10.0 or later required"
    fi
  fi
}

# USER isn't always set so provide a fall back for the installer and subprocesses.
if [[ -z "${USER-}" ]]; then
  USER="$(chomp "$(id -un)")"
  export USER
fi

# Invalidate sudo timestamp before exiting (if it wasn't active before).
if ! /usr/bin/sudo -n -v 2>/dev/null; then
  trap '/usr/bin/sudo -k' EXIT
fi

######################################################script

if ! command -v docker >/dev/null; then
    abort "$(cat <<EOABORT
You must install Docker before installing Erda.
EOABORT
)"
fi

check_docker_version

if ! command -v docker-compose >/dev/null; then
    abort "$(cat <<EOABORT
You must install Docker-Compose before installing Erda.
EOABORT
)"
fi

INSTALL_LOCATION="$(pwd)"
ohai "Start setup Erda using ${INSTALL_LOCATION}/docker-compose.yml"

execute "docker-compose" "up" "-d" "mysql"
sleep 10
execute "docker-compose" "up" "--abort-on-container-exit" "--exit-code-from" "mysql-healthcheck" "mysql-healthcheck"
execute "docker-compose" "up" "--abort-on-container-exit" "--exit-code-from" "erda-migration" "erda-migration"

execute "docker-compose" "up" "sysctl-init"
execute "docker-compose" "up" "-d" "elasticsearch"

execute "docker-compose" "up" "-d" "cassandra"
execute "docker-compose" "up" "-d" "kafka"

execute "docker-compose" "up" "-d"

ohai "Setup local hosts"
(
  exists_in_host="$(grep -n erda.local /etc/hosts)"
  if [ -z "$exists_in_host" ]; then
    echo "127.0.0.1 erda.local one.erda.local gittar.erda.local collector.erda.local openapi.erda.local" | execute_sudo "tee" "-a" "/etc/hosts"
  fi
) || exit 1

ohai "Erda has been started successfully using ${INSTALL_LOCATION}/docker-compose.yml"

ohai "Next steps:"
echo "visit ${tty_underline}http://erda.local${tty_reset} to start your journey on Erda"
echo "visit ${tty_underline}https://docs.erda.cloud/1.3/manual/install/docker-install.html${tty_reset} for FAQs if you encounter problems installing Erda"
echo "visit ${tty_underline}https://docs.erda.cloud${tty_reset} for full introduction of Erda"
echo "goto ${INSTALL_LOCATION}/ dir to check and manage the docker-compose resources"
