#!/bin/bash
{
    set -e
    SUDO=''
    if [ "$(id -u)" != "0" ]; then
      SUDO='sudo'
      echo "This script requires superuser permissions."
      echo "You will be prompted for password by sudo."
      # clear any previous sudo permission
      sudo -k
    fi

    $SUDO bash <<SCRIPT
  set -e

  echoerr() { echo "\$@" 1>&2; }

  if [[ ! ":\$PATH:" == *":/usr/local/bin:"* ]]; then
    echoerr '$PATH does not contain /usr/local/bin, which need by this installer.'
    exit 1
  fi

  if ! [ \$(command -v jq) ]; then
    echoerr "This installer needs 'jq' command"
    exit 1
  elif ! [ \$(command -v curl) ]; then
    echoerr "This installer needs 'wget' command"
    exit 1
  fi

  if [ "\$(uname)" == "Darwin" ]; then
    OS=mac
  elif [ "\$(expr substr \$(uname -s) 1 5)" == "Linux" ]; then
    OS=linux
  else
    echoerr "This installer is only supported on Linux and MacOS"
    exit 1
  fi

  if [[ $# -gt 0 ]]; then
    if [[ "$1" == "alpha"  ]]; then
      Ver="master"
    else
      Ver="$1"
    fi
  else
    Ver=\$(curl -sS https://api.github.com/repos/erda-project/erda/releases/latest | jq .tag_name | sed 's/\"//g')
  fi

  Version=\$(curl -sS "https://raw.githubusercontent.com/erda-project/erda/\$Ver/VERSION")
  if [[ "\$Ver" == "master" ]]; then
    Version="\${Version}-alpha"
  fi

  URL=https://erda-release.oss-cn-hangzhou.aliyuncs.com/cli/\$OS/erda-\$Version
  echo "Installing CLI from \$URL"
  curl -o /usr/local/bin/erda-cli "\$URL"
  chmod +x /usr/local/bin/erda-cli

SCRIPT
  # test the installed CLI
  LOCATION=$(command -v erda-cli)
  echo "erda-cli installed to $LOCATION"
  erda-cli version
}