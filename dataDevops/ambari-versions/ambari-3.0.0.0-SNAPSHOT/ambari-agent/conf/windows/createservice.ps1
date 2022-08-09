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

param(
  [String]
  [Parameter(Mandatory=$true )]
  $username,
  [String]
  [Parameter(Mandatory=$true )]
  $password,
  [String]
  [Parameter(Mandatory=$true )]
  $servicename,
  [String]
  [Parameter(Mandatory=$true )]
  $hdpResourcesDir,
  [String]
  [Parameter(Mandatory=$true )]
  $servicecmdpath
  )

function Invoke-Cmd ($command)
{
  Write-Output $command
  $out = cmd.exe /C "$command" 2>&1
  Write-Output $out
  return $out
}

function Invoke-CmdChk ($command)
{
  Write-Output $command
  $out = cmd.exe /C "$command" 2>&1
  Write-Output $out
  if (-not ($LastExitCode  -eq 0))
  {
    throw "Command `"$out`" failed with exit code $LastExitCode "
  }
  return $out
}

### Stops and deletes the Hadoop service.
function StopAndDeleteHadoopService(
  [String]
  [Parameter( Position=0, Mandatory=$true )]
  $service
)
{
  Write-Output "Stopping $service"
  $s = Get-Service $service -ErrorAction SilentlyContinue

  if( $s -ne $null )
  {
    Stop-Service $service
    $cmd = "sc.exe delete $service"
    Invoke-Cmd $cmd
  }
}

# Convenience method for processing command-line credential objects
# Assumes $credentialsHash is a hash with one of the following being true:
#  - keys "username" and "password"/"passwordBase64" are set to strings
#  - key "credentialFilePath" is set to the path of a serialized PSCredential object
function Get-HadoopUserCredentials($credentialsHash)
{
  Write-Output "Using provided credentials for username $($credentialsHash["username"])" | Out-Null
  $username = $credentialsHash["username"]
  if($username -notlike "*\*")
  {
    $username = "$ENV:COMPUTERNAME\$username"
  }
  $securePassword = $credentialsHash["password"] | ConvertTo-SecureString -AsPlainText -Force
  $creds = New-Object System.Management.Automation.PSCredential $username, $securePassword
  return $creds
}

### Creates and configures the service.
function CreateAndConfigureHadoopService(
  [String]
  [Parameter( Position=0, Mandatory=$true )]
  $service,
  [String]
  [Parameter( Position=1, Mandatory=$true )]
  $hdpResourcesDir,
  [String]
  [Parameter( Position=2, Mandatory=$true )]
  $serviceBinDir,
  [String]
  [Parameter( Position=3, Mandatory=$true )]
  $servicecmdpath,
  [System.Management.Automation.PSCredential]
  [Parameter( Position=4, Mandatory=$true )]
  $serviceCredential
)
{
  if ( -not ( Get-Service "$service" -ErrorAction SilentlyContinue ) )
  {
    Write-Output "Creating service `"$service`" as $serviceBinDir\$service.exe"
    $xcopyServiceHost_cmd = "copy /Y `"$hdpResourcesDir\namenode.exe`" `"$serviceBinDir\$service.exe`""
    Invoke-CmdChk $xcopyServiceHost_cmd

    #HadoopServiceHost.exe will write to this log but does not create it
    #Creating the event log needs to be done from an elevated process, so we do it here
    if( -not ([Diagnostics.EventLog]::SourceExists( "$service" )))
    {
      [Diagnostics.EventLog]::CreateEventSource( "$service", "" )
    }
    Write-Output "Adding service $service"
    if ($serviceCredential.Password.get_Length() -ne 0)
    {
      $s = New-Service -Name "$service" -BinaryPathName "$serviceBinDir\$service.exe" -Credential $serviceCredential -DisplayName "Apache Hadoop $service"
      if ( $s -eq $null )
      {
        throw "CreateAndConfigureHadoopService: Service `"$service`" creation failed"
      }
    }
    else
    {
      # Separately handle case when password is not provided
      # this path is used for creating services that run under (AD) Managed Service Account
      # for them password is not provided and in that case service cannot be created using New-Service commandlet
      $serviceUserName = $serviceCredential.UserName
      $cred = $serviceCredential.UserName.Split("\")

      # Throw exception if domain is not specified
      if (($cred.Length -lt 2) -or ($cred[0] -eq "."))
      {
        throw "Environment is not AD or domain is not specified"
      }

      $cmd="$ENV:WINDIR\system32\sc.exe create `"$service`" binPath= `"$serviceBinDir\$service.exe`" obj= $serviceUserName DisplayName= `"Apache Hadoop $service`" "
      try
      {
        Invoke-CmdChk $cmd
      }
      catch
      {
        throw "CreateAndConfigureHadoopService: Service `"$service`" creation failed"
      }
    }

    $cmd="$ENV:WINDIR\system32\sc.exe failure $service reset= 30 actions= restart/5000"
    Invoke-CmdChk $cmd

    $cmd="$ENV:WINDIR\system32\sc.exe config $service start= demand"
    Invoke-CmdChk $cmd


    Write-Output "Creating service config ${serviceBinDir}\$service.xml"
    $cmd = "$servicecmdpath --service $service > `"$serviceBinDir\$service.xml`""
    Invoke-CmdChk $cmd
  }
  else
  {
    Write-Output "Service `"$service`" already exists, Removing `"$service`""
    StopAndDeleteHadoopService $service
    CreateAndConfigureHadoopService $service $hdpResourcesDir $serviceBinDir $servicecmdpath $serviceCredential
  }
}


try
{
  Write-Output "Creating credential object"
  ###
  ### Create the Credential object from the given username and password or the provided credentials file
  ###
  $serviceCredential = Get-HadoopUserCredentials -credentialsHash @{"username" = $username; "password" = $password}
  $username = $serviceCredential.UserName
  Write-Output "Username: $username"

  Write-Output "Creating service $service"
  ###
  ### Create Service
  ###
  CreateAndConfigureHadoopService $servicename $hdpResourcesDir $hdpResourcesDir $servicecmdpath $serviceCredential
  Write-Output "Done"
}
catch
{
  Write-Output "Failure"
  exit 1
}