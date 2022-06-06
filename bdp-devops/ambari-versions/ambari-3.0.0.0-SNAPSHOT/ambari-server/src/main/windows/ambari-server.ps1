# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

# description: ambari-server service
# processname: ambari-server

$VERSION="1.3.0-SNAPSHOT"
$HASH="testhash"

switch ($($args[0])){
  "--version" {
    echo "$VERSION"
    exit 0
  }
  "--hash" {
    echo "$HASH"
    exit 0
  }
}

# Handle spaces in command line arguments properly
$quoted_args=@()

ForEach ($arg in $args)
{
  if($arg.Contains(' '))
  {
    $arg = """" + $arg + """"
  }
  $quoted_args = $quoted_args + @($arg)
}

$args = $quoted_args

$AMBARI_SERVER="ambari-server"
$AMBARI_SVC_NAME = "Ambari Server"
$current_directory = (Get-Item -Path ".\" -Verbose).FullName
#environment variables used in python, check if they exists, otherwise set them to $current_directory
#and pass to child python process
$Env:PYTHONPATH="$current_directory\sbin;$($Env:PYTHONPATH)"
$Env:PYTHON = "python.exe"

$AMBARI_LOG_DIR="\var\log\ambari-server"
$OUTFILE_STDOUT=Join-Path -path $AMBARI_LOG_DIR -childpath "ambari-server.stdout"
$OUTFILE_STDERR=Join-Path -path $AMBARI_LOG_DIR -childpath "ambari-server.stderr"
$LOGFILE=Join-Path -path $AMBARI_LOG_DIR -childpath "ambari-server.log"
$AMBARI_SERVER_PY_SCRIPT=Join-Path -path $PSScriptRoot -childpath "sbin\ambari-server.py"
if($AMBARI_SERVER_PY_SCRIPT.Contains(' '))
{
  $AMBARI_SERVER_PY_SCRIPT = """" + $AMBARI_SERVER_PY_SCRIPT + """"
}

$OK=1
$NOTOK=0


# Reading the environment file
#if [ -a /var/lib/ambari-server/ambari-env.sh ]; then
#  . /var/lib/ambari-server/ambari-env.sh
#fi


#echo $AMBARI_PASSPHRASE

$retcode=0

function _exit($code)
{
    $host.SetShouldExit($code)
    exit $code
}

function _detect_python()
{
    if(![boolean]$(Get-Command $Env:PYTHON -ErrorAction SilentlyContinue))
    {
        echo "ERROR: Can not find python.exe in PATH. Add python executable to PATH and try again."
        _exit(1)
    }
}
function _detect_local_sql()
{
  $services = Get-Service -Include @("MSSQL$*")
  if($services)
  {
    echo "Detected following local SQL Server instances:"
    foreach ($instance in $services) {
      echo $instance
    }
  } else {
    echo "WARNING: No local SQL Server instances detected. Make sure you have properly configured SQL Server"
  }
}

function _echo([switch]$off)
{
  if($off)
  {
    try
    {
      stop-transcript|out-null
    }
    catch [System.InvalidOperationException]
    {}
  }
  else
  {
    try
    {
      start-transcript|out-null
    }
    catch [System.InvalidOperationException]
    {}
  }
}

Function _pstart_brief($cmd_args)
{
  #start python with -u to make stdout and stderr unbuffered
  $arguments = @("-u",$AMBARI_SERVER_PY_SCRIPT) + $cmd_args

  $psi = New-Object System.Diagnostics.ProcessStartInfo

  $psi.RedirectStandardError = $True
  $psi.RedirectStandardOutput = $True

  $psi.UseShellExecute = $False

  $psi.FileName = $Env:PYTHON
  $psi.Arguments = $arguments
  #$psi.WindowStyle = WindowStyle.Hidden

  $process = [Diagnostics.Process]::Start($psi)

  $process.WaitForExit()

  Write-Output $process.StandardOutput.ReadToEnd()
}

Function _start($cmd_args)
{
  echo "Starting $AMBARI_SVC_NAME..."
  _echo -off

  _pstart_brief($cmd_args)

  $cnt = 0
  do
  {
    Start-Sleep -Milliseconds 250
    $svc = Get-Service -Name $AMBARI_SVC_NAME
    $cnt += 1
    if ($cnt -eq 120)
    {
      echo "$AMBARI_SVC_NAME still starting...".
      return
    }
  }
  until($svc.status -eq "Running")

  echo "$AMBARI_SVC_NAME is running"
}

Function _pstart($cmd_args)
{
  New-Item -ItemType Directory -Force -Path $AMBARI_LOG_DIR | Out-Null

  $arguments = @($AMBARI_SERVER_PY_SCRIPT) + $cmd_args

  $p = New-Object System.Diagnostics.Process
  $p.StartInfo.UseShellExecute = $false
  $p.StartInfo.FileName = $Env:PYTHON
  $p.StartInfo.Arguments = $arguments
  [void]$p.Start();

  echo "Verifying $AMBARI_SERVER process status..."
  if (!$p){
    echo "ERROR: $AMBARI_SERVER start failed"
    $host.SetShouldExit(-1)
    exit
  }
  echo "Server log at: $LOGFILE"

  $p.WaitForExit()
}

Function _pstart_ioredir($cmd_args)
{
  New-Item -ItemType Directory -Force -Path $AMBARI_LOG_DIR | Out-Null

  #start python with -u to make stdout and stderr unbuffered
  $arguments = @("-u",$AMBARI_SERVER_PY_SCRIPT) + $cmd_args
  $process = Start-Process -FilePath $Env:PYTHON -ArgumentList $arguments -WindowStyle Hidden -RedirectStandardError $OUTFILE_STDERR -RedirectStandardOutput $OUTFILE_STDOUT -PassThru
  echo "Verifying $AMBARI_SERVER process status..."
  if (!$process){
    echo "ERROR: $AMBARI_SERVER start failed"
    $host.SetShouldExit(-1)
    exit
  }
  echo "Server stdout at: $OUTFILE_STDOUT"
  echo "Server stderr at: $OUTFILE_STDERR"
  echo "Server log at: $LOGFILE"

  $process.WaitForExit()
}

Function _upgrade($cmd_args){
  _pstart($cmd_args)
}

Function _stop($cmd_args){
  echo "Stopping $AMBARI_SVC_NAME..."
  _pstart_brief($cmd_args)

  $cnt = 0
  do
  {
    Start-Sleep -Milliseconds 250
    $svc = Get-Service -Name $AMBARI_SVC_NAME
    $cnt += 1
    if ($cnt -eq 40)
    {
      echo "$AMBARI_SVC_NAME still stopping...".
      return
    }
  }
  until($svc.status -eq "Stopped")
  echo "$AMBARI_SVC_NAME is stopped"
}

Function _status($cmd_args){
  echo "Getting $AMBARI_SVC_NAME status..."
  _pstart_brief($cmd_args)
}

# check for python before any action
_detect_python
switch ($($args[0])){
  "start" {_start $args}
  "pstart"
  {
    echo "Starting Ambari Server"
    _pstart_ioredir $args
    echo "Ambari Server Start finished"
  }
  "stop"
  {
    echo "Stopping Ambari Server"
    _stop $args
    echo "Ambari Server Stop finished"
  }
  "reset"
  {
    echo "Reseting Ambari Server"
    _pstart $args
    echo "Ambari Server Reset finished"
  }
  "restart"
  {
    echo "Restarting Ambari Server"
    _stop @("stop")
    _start @("start")
    echo "Ambari Server Restart finished"
  }
  "upgrade"
  {
    echo "Upgrading Ambari Server"
    _upgrade $args
    echo "Ambari Server Upgrade finished"
  }
  "status"
  {
    echo "Checking Ambari Server status"
    _status $args
  }
#    "upgradestack" {_pstart $args}
  "setup"
  {
    echo "Installing Ambari Server"
    _detect_local_sql
    _pstart $args
    echo "Ambari Server Installation finished"
  }
  "setup-ldap"
  {
    _pstart $args
  }
  "setup-security"
  {
    echo "Setting up security for Ambari Server"
    _pstart $args
    echo "Ambari Server security setup finished"
  }
  "refresh-stack-hash"
  {
    echo "Refreshing stack hash"
    _pstart $args
    echo "Refreshing stack hash finished"
  }
  "setup-sso"
    {
      echo "Setting up SSO authentication for Ambari Server"
      _pstart $args
      echo "Ambari Server SSO authentication setup finished"
    }
  default
  {
    echo "Usage: ambari-server {start|stop|restart|setup|upgrade|status|upgradestack|setup-ldap|setup-security|setup-sso|refresh-stack-hash} [options]"
    echo "Use ambari-server <action> --help to get details on options available."
    echo "Or, simply invoke ambari-server.py --help to print the options."
    $retcode=1
  }
}

_exit($retcode)
