### Licensed to the Apache Software Foundation (ASF) under one or more
### contributor license agreements.  See the NOTICE file distributed with
### this work for additional information regarding copyright ownership.
### The ASF licenses this file to You under the Apache License, Version 2.0
### (the "License"); you may not use this file except in compliance with
### the License.  You may obtain a copy of the License at
###
###     http://www.apache.org/licenses/LICENSE-2.0
###
### Unless required by applicable law or agreed to in writing, software
### distributed under the License is distributed on an "AS IS" BASIS,
### WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
### See the License for the specific language governing permissions and
### limitations under the License.

###
### A set of basic PowerShell routines that can be used to install and
### configure AMB components on a windows node.
###

### Valid properties in AMB_LAYOUT file
$VALID_LAYOUT_PROPERTIES = @("AMB_DATA_DIR", "SQL_SERVER_PASSWORD",
                             "SQL_SERVER_NAME", "SQL_SERVER_LOGIN", "SQL_SERVER_PORT","SQL_JDBC_PATH","HDP_VERSION"
                             )

### Mandatory properties in AMB_LAYOUT file
$MANDATORY_LAYOUT_PROPERTIES = @("AMB_DATA_DIR", "SQL_SERVER_PASSWORD",
                             "SQL_SERVER_NAME", "SQL_SERVER_LOGIN", "SQL_SERVER_PORT","SQL_JDBC_PATH")
$HDP_LAYOUT_PROPERTIES = @( "NAMENODE_HOST", "SECONDARY_NAMENODE_HOST", "JOBTRACKER_HOST",
                             "HIVE_SERVER_HOST", "OOZIE_SERVER_HOST", "WEBHCAT_HOST",
                             "SLAVE_HOSTS","ZOOKEEPER_HOSTS", "HBASE_MASTER",
                             "FLUME_HOSTS","CLIENT_HOSTS","NN_HA_JOURNALNODE_HOSTS","NN_HA_STANDBY_NAMENODE_HOST",
                             "RM_HA_STANDBY_RESOURCEMANAGER_HOST")


### Helper routine that converts a $null object to nothing. Otherwise, iterating over
### a $null object with foreach results in a loop with one $null element.
function empty-null($obj)
{
   if ($obj -ne $null) { $obj }
}

### Helper routine that updates the given fileName XML file with the given
### key/value configuration values. The XML file is expected to be in the
### Hadoop format. For example:
### <configuration>
###   <property>
###     <name.../><value.../>
###   </property>
### </configuration>
function UpdateXmlConfig(
    [string]
    [parameter( Position=0, Mandatory=$true )]
    $fileName, 
    [hashtable]
    [parameter( Position=1 )]
    $config = @{} )
{
    $xml = New-Object System.Xml.XmlDocument
    $xml.PreserveWhitespace = $true
    $xml.Load($fileName)

    foreach( $key in empty-null $config.Keys )
    {
        $value = $config[$key]
        $found = $False
        $xml.SelectNodes('/configuration/property') | ? { $_.name -eq $key } | % { $_.value = $value; $found = $True }
        if ( -not $found )
        {
            $newItem = $xml.CreateElement("property")
            $newItem.AppendChild($xml.CreateSignificantWhitespace("`r`n    ")) | Out-Null
            $newItem.AppendChild($xml.CreateElement("name")) | Out-Null
            $newItem.AppendChild($xml.CreateSignificantWhitespace("`r`n    ")) | Out-Null
            $newItem.AppendChild($xml.CreateElement("value")) | Out-Null
            $newItem.AppendChild($xml.CreateSignificantWhitespace("`r`n  ")) | Out-Null
            $newItem.name = $key
            $newItem.value = $value
            $xml["configuration"].AppendChild($xml.CreateSignificantWhitespace("`r`n  ")) | Out-Null
            $xml["configuration"].AppendChild($newItem) | Out-Null
            $xml["configuration"].AppendChild($xml.CreateSignificantWhitespace("`r`n")) | Out-Null
        }
    }
    
    $xml.Save($fileName)
    $xml.ReleasePath
}

### Helper routine to return the IPAddress given a hostname
function GetIPAddress($hostname)
{
    try
    {
        $remote_host = [System.Net.Dns]::GetHostAddresses($hostname) | ForEach-Object { if ($_.AddressFamily -eq "InterNetwork") { $_.IPAddressToString } }
		Test-Connection $remote_host -ErrorAction Stop
		
    }
    catch
    {
        throw "Error resolving IPAddress for host '$hostname'"
    }
}

### Helper routine to check if two hosts are the same
function IsSameHost(
    [string]
    [parameter( Position=0, Mandatory=$true )]
    $host1,
    [array]
    [parameter( Position=1, Mandatory=$false )]
    $host2ips = ((GetIPAddress $ENV:COMPUTERNAME) -as [array]))
{
    $host1ips = ((GetIPAddress $host1) -as [array])
    $heq = Compare-Object $host1ips $host2ips -ExcludeDifferent -IncludeEqual
    return ($heq -ne $null)
}

### Helper routine to normalize the directory path
function NormalizePath($path)
{
    [IO.Path]::GetFullPath($path.ToLower() + [IO.Path]::DirectorySeparatorChar)
}

### Helper routine to read a Java style properties file and return a hashtable
function Read-ClusterLayout($filepath,$VALID_LAYOUT_PROPERTIES,$MANDATORY_LAYOUT_PROPERTIES)
{
    $properties = @{}
    $propfile = Get-Content $filepath
    foreach ($line in $propfile)
    {
        $line=$line.Trim()
        if (($line) -and (-not $line.StartsWith("#")))
        {
            $prop = @($line.split("=", 2))
            $propkey = $prop[0].Trim()
            if ($VALID_LAYOUT_PROPERTIES -contains $propkey)
            {
                $propval = $prop[1].Trim()
                if ($propval)
                {
                    $properties.Add($propkey, $propval)
                }
                else
                {
                    throw "Property '$propkey' value cannot be left blank"
                }
            }
        }
    }
 	if ($MANDATORY_LAYOUT_PROPERTIES)
	{
	    ### Check the required properties
	    foreach ($prop in $MANDATORY_LAYOUT_PROPERTIES)
	    {
	        if ( -not ($properties.Contains($prop)) )
	        {
	            throw "Required property '$prop' not found in $filepath"
	        }
	    }
	}
    return $properties
}

### Helper routine to read a Java style properties file and import the properties into process' environment
function Export-ClusterLayoutIntoEnv($filepath,$type)
{
    ### Read the cluster layout file
	if ($type -ieq "amb")
	{
    	$properties = Read-ClusterLayout $filepath $VALID_LAYOUT_PROPERTIES $MANDATORY_LAYOUT_PROPERTIES
		### Abort install if AMB_DATA_DIR is invalid or is in the amb install root
		$ambDataDirs = [String]$properties["AMB_DATA_DIR"]
	    foreach ($folder in $ambDataDirs.Split(","))
	    {
	        $folder = $folder.Trim()
			
	        Check-Drive $folder "AMB_DATA_DIR"
			
	        if (-not $folder)
	        {
	            throw "Invalid AMB_DATA_DIR: $ambDataDirs"
	        }

	        if ($folder.IndexOf(" ") -ne -1)
	        {
	            throw "AMB_DATA_DIR ($folder) path cannot have spaces"
	        }

	        if (-not (Test-Path -IsValid $folder))
	        {
	            throw "AMB_DATA_DIR ($folder) is invalid"
	        }
		}
	}
	elseif ($type -ieq "hdp")
	{
		$properties = Read-ClusterLayout $filepath $HDP_LAYOUT_PROPERTIES
	}
   
   

    Write-Log "Following properties will be exported into the environment"
    Write-Log ("{0,-30}{1,-60}" -f "Property","Value")
    Write-Log ("{0,-30}{1,-60}" -f "--------","-----")
    foreach ($key in $properties.Keys)
    {
        $value = [String]$properties[$key]
        Write-Log ("{0,-30}{1,-60}" -f "$key","$value")
        [Environment]::SetEnvironmentVariable($key, $properties[$key], [EnvironmentVariableTarget]::Process)
    }

}

### Helper routine to emulate which
function Which($command)
{
    (Get-Command $command | Select-Object -first 1).Path
}

### Function to append a sub-path to a list of paths
function Get-AppendedPath(
    [String]
    [Parameter( Position=0, Mandatory=$true )]
    $pathList,
    [String]
    [Parameter( Position=1, Mandatory=$true )]
    $subPath,
    [String]
    [Parameter( Position=2, Mandatory=$false )]
    $delimiter = ",")
{
    $newPath = @()
    foreach ($path in $pathList.Split($delimiter))
    {
        $path = $path.Trim()
        if ($path -ne $null)
        {
            $apath = Join-Path $path $subPath
            $newPath = $newPath + $apath
        }
    }
    return ($newPath -Join $delimiter)
}

### Check validity of drive for $path
function Check-Drive($path, $message){
    if($path -cnotlike "*:*"){
        Write-Warning "Target path doesn't contains drive identifier, checking skipped"
        return
    }
    $pathvolume = $path.Split(":")[0] + ":"
    if (! (Test-Path ($pathvolume + '\')) ) {
        throw "Target volume for $message $pathvolume doesn't exist"
    }
}

### Gives full permissions on the folder to the given user
function GiveFullPermissions(
    [String]
    [Parameter( Position=0, Mandatory=$true )]
    $folder,
    [String]
    [Parameter( Position=1, Mandatory=$true )]
    $username,
    [bool]
    [Parameter( Position=2, Mandatory=$false )]
    $recursive = $false)
{
    Write-Log "Giving user/group `"$username`" full permissions to `"$folder`""
    $cmd = "icacls `"$folder`" /grant:r ${username}:(OI)(CI)F"
    if ($recursive) {
        $cmd += " /T"
    }
    Invoke-CmdChk $cmd
}

### Add service control permissions to authenticated users.
### Reference:
### http://stackoverflow.com/questions/4436558/start-stop-a-windows-service-from-a-non-administrator-user-account 
### http://msmvps.com/blogs/erikr/archive/2007/09/26/set-permissions-on-a-specific-service-windows.aspx

function Set-ServiceAcl ($service)
{
    $cmd = "sc sdshow $service"
    $sd = Invoke-Cmd $cmd

    Write-Log "Current SD: $sd"

    ## A;; --- allow
    ## RP ---- SERVICE_START
    ## WP ---- SERVICE_STOP
    ## CR ---- SERVICE_USER_DEFINED_CONTROL    
    ## ;;;AU - AUTHENTICATED_USERS

    $sd = [String]$sd
    $sd = $sd.Replace( "S:(", "(A;;RPWPCR;;;AU)S:(" )
    Write-Log "Modifying SD to: $sd"

    $cmd = "sc sdset $service $sd"
    Invoke-Cmd $cmd
}

### Creates and configures the service.
function CreateAndConfigureAmbariService(
    [String]
    [Parameter( Position=0, Mandatory=$true )]
    $service,
    [String]
    [Parameter( Position=1, Mandatory=$true )]
    $ambResourcesDir,
    [String]
    [Parameter( Position=2, Mandatory=$true )]
    $serviceBinDir
)
{
    if ( -not ( Get-Service "$service" -ErrorAction SilentlyContinue ) )
    {
        Write-Log "Creating service `"$service`" as $serviceBinDir\$service.exe"
        $xcopyServiceHost_cmd = "copy /Y `"$ambResourcesDir\serviceHost.exe`" `"$serviceBinDir\$service.exe`""
        Invoke-CmdChk $xcopyServiceHost_cmd

        #ServiceHost.exe will write to this log but does not create it
        #Creating the event log needs to be done from an elevated process, so we do it here
        if( -not ([Diagnostics.EventLog]::SourceExists( "$service" )))
        {
            [Diagnostics.EventLog]::CreateEventSource( "$service", "" )
        }

        Write-Log "Adding service $service"
        $s = New-Service -Name "$service" -BinaryPathName "$serviceBinDir\$service.exe" -DisplayName "Apache Ambari $service"
        if ( $s -eq $null )
        {
            throw "CreateAndConfigureAmbariService: Service `"$service`" creation failed"
        }

        $cmd="$ENV:WINDIR\system32\sc.exe failure $service reset= 30 actions= restart/5000"
        Invoke-CmdChk $cmd

        $cmd="$ENV:WINDIR\system32\sc.exe config $service start= demand"
        Invoke-CmdChk $cmd

        Set-ServiceAcl $service
    }
    else
    {
        Write-Log "Service `"$service`" already exists, Removing `"$service`""
        StopAndDeleteAmbariService $service
        CreateAndConfigureAmbariService $service $ambResourcesDir $serviceBinDir
    }
}

### Stops and deletes the Ambari service.
function StopAndDeleteAmbariService(
    [String]
    [Parameter( Position=0, Mandatory=$true )]
    $service
)
{
    Write-Log "Stopping $service"
    $s = Get-Service $service -ErrorAction SilentlyContinue

    if( $s -ne $null )
    {
        Stop-Service $service
        $cmd = "sc.exe delete $service"
        Invoke-Cmd $cmd
    }
}

function ReplaceAmbariServiceXML(
    [String]
    [Parameter( Position=0, Mandatory=$true )]
    $file,
    [String]
    [Parameter( Position=1, Mandatory=$true )]
    $find,
    [String]
    [Parameter( Position=2, Mandatory=$true )]
    $replace
)
{
    Get-Content $file | ForEach-Object { $_ -replace $find, $replace } | Set-Content ($file+".tmp")
    Remove-Item $file
    Rename-Item ($file+".tmp") $file
}


Export-ModuleMember -Function UpdateXmlConfig
Export-ModuleMember -Function GetIPAddress
Export-ModuleMember -Function IsSameHost
Export-ModuleMember -Function Export-ClusterLayoutIntoEnv
Export-ModuleMember -Function Which
Export-ModuleMember -Function Get-AppendedPath
Export-ModuleMember -Function GiveFullPermissions
Export-ModuleMember -Function CreateAndConfigureAmbariService
Export-ModuleMember -Function StopAndDeleteAmbariService
Export-ModuleMember -Function ReplaceAmbariServiceXML
