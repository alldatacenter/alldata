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
function Split_Hosts ($var,$hosts)
{
	Write-Log "Started split of $var"
	if ($var -like "*,*")
	{
		$split_hosts = $var -split ","
		foreach ($server in $split_hosts)
		{
			$hosts.Value += $server
		}
	}
	else
	{ 
		$hosts.Value += $var
	}

}
function GetName($name,$param)
{
	if ($param -ieq "full")
	{
		$name = Get-Item $name
		return $name.FullName
	}
	elseif ($param -ieq "short")
	{
		$name = Get-Item $name
		return $name.Name
	}
}
function Main( $scriptDir )
{
    Write-Log "UNINSTALLATION of 2.0.0 STARTED"
	Write-Log "Reading Ambari and HDP layout"
	$destination= [Environment]::GetEnvironmentVariable("AMB_DATA_DIR","Machine")
	if (-not (Test-Path ENV:AMB_LAYOUT))
	{
		$ENV:AMB_LAYOUT = Join-Path $destination "ambariproperties.txt"
	}
	if (-not (Test-Path ENV:HDP_LAYOUT) -or ($ENV:HDP_LAYOUT -notlike "*:"))
	{
        $hdp = Join-Path $destination "\ambari-scom-server-*-conf"
        $hdp = Getname $hdp "full"
		$ENV:HDP_LAYOUT = Join-Path $hdp "conf\clusterproperties.txt"
	}
	Write-Log "Ambari layout = $ENV:AMB_LAYOUT" 
	Write-Log "Cluster layout = $ENV:HDP_LAYOUT" 
	Export-ClusterLayoutIntoEnv $ENV:AMB_LAYOUT "amb"
	Export-ClusterLayoutIntoEnv $ENV:HDP_LAYOUT "hdp"
	$jar = Join-Path $destination "\metrics-sink-*.jar"
    $ambari_metrics = Getname $jar "short"
    Write-Log "Metrics sink is $ambari_metrics" 
	$current = @()
	$SQL_SERVER_NAME = $env:SQL_SERVER_NAME
	$hosts= @($SQL_SERVER_NAME,$ENV:NAMENODE_HOST,$ENV:SECONDARY_NAMENODE_HOST,$ENV:RESOURCEMANAGER_HOST,$ENV:HIVE_SERVER_HOST,$ENV:OOZIE_SERVER_HOST,
	$ENV:WEBHCAT_HOST,$ENV:HBASE_MASTER,$ENV:NN_HA_STANDBY_NAMENODE_HOST,$ENV:RM_HA_STANDBY_RESOURCEMANAGER_HOST)
	Split_Hosts $ENV:SLAVE_HOSTS ([REF]$hosts)
	Split_Hosts $ENV:ZOOKEEPER_HOSTS ([REF]$hosts)
	Split_Hosts $ENV:FLUME_HOSTS ([REF]$hosts)
    Split_Hosts $ENV:CLIENT_HOSTS ([REF]$hosts)
    Split_Hosts $ENV:NN_HA_JOURNALNODE_HOSTS ([REF]$hosts)
	Write-Log "Hosts list:"
	Write-log $hosts
	Write-Log "Uninstalling from each host"
	$failed=@()
	$failed_file = join-path $env:TMP "ambari_failed.txt"
		if (Test-Path $failed_file)
		{
			Remove-Item $failed_file -Force
		}
	foreach ($server in $hosts)
	{
		if (($current -notcontains $server) -and ($server -ne $null))
		{
			Write-Log "Executing uninstallation on $server"
			$out = Invoke-Command -ComputerName $server -ScriptBlock {
				param( $server,$SQL_SERVER_NAME,$destination,$ambari_metrics)
				$log = Join-Path $env:tmp "ambari_uninstall.log"
				function Invoke-Cmd ($command)
				{
					Out-File -FilePath $log -InputObject "$command" -Append -Encoding "UTF8"
				    Write-Host $command
				    $out = cmd.exe /C "$command" 2>&1
 					$out | ForEach-Object { Write-Host "CMD" $_ }
				    return $out
				}
				$hdp_home = [Environment]::GetEnvironmentVariable("HADOOP_HOME","Machine")
				if ($hdp_home -ne $null)
				{
					Out-File -FilePath $log -InputObject "Starting Ambari removal" -Append -Encoding "UTF8"
					Out-File -FilePath $log -InputObject "Cleaning up Ambari folder" -Append -Encoding "UTF8"
					$out = Remove-Item $destination -Force -Recurse 2>&1
					if ($out -like "*Cannot remove*")
					{
						Write-Output "Failed"
					}
					else
					{
						Write-Output "Succeeded"
					}
					Out-File -FilePath $log -InputObject "Cleaning up metrics" -Append -Encoding "UTF8"
					$metrics = Join-Path $hdp_home "etc\hadoop\hadoop-metrics2.properties"
					if (-not (test-path $metrics))
					{
						$metrics = Join-Path $hdp_home "conf\hadoop-metrics2.properties"
					}
					$result=@()
					$file = Get-Content $metrics
					foreach ($string in $file)
					{
						if (($string -cnotlike "*sink.sql.class*") -and ($string -cnotlike "*.sink.sql.databaseUrl*"))
					 	{
							$result+=$string
						}
					}
					Set-Content -Path $metrics -Value $result
					Out-File -FilePath $log -InputObject "Cleaning up xml's" -Append -Encoding "UTF8"
					$names = @("namenode","secondarynamenode","datanode","historyserver","resourcemanager","nodemanager")
					foreach ($name in $names)
					{
						$xml_file= Join-Path $hdp_home "bin\$name.xml"
						Out-File -FilePath $log -InputObject "Cleaning up $xml_file" -Append -Encoding "UTF8"
                        Out-File -FilePath $log -InputObject "Removing ;$destination\$ambari_metrics;$destination\sqljdbc4.jar" -Append -Encoding "UTF8"
						(Get-Content $xml_file)|ForEach-Object {
						$_.Replace(";$destination\$ambari_metrics;$destination\sqljdbc4.jar","")
						}|Set-Content $xml_file
					}
					
				}
				if  (($server -ieq $SQL_SERVER_NAME) -and ($hdp_home -eq $null))
				{	
#					Out-File -FilePath $log -InputObject "Cleaning up HadoopMetrics database" -Append -Encoding "UTF8"
#					$cmd ="sqlcmd -s $SQL_SERVER_NAME -q 'drop database HadoopMetrics'"
#					invoke-cmd $cmd 
					if (Test-Path $destination)
					{
						$out = Remove-Item $destination -Force -Recurse 2>&1
						if ($out -like "*Cannot remove*")
						{
							Write-Output "Failed"
						}
						else
						{
							Write-Output "Succeeded"
						}
					}
					else
					{
						Write-Output "Succeeded"
					}
				}
			} -ArgumentList ($server,$SQL_SERVER_NAME,$destination,$ambari_metrics)
			if (($out -like "*failed*") -or ($out -eq $null))
			{
				$failed+=$server
				Write-Log "Uninstallation on $server does not finished. Please remove existing items manually"
			}
			else
			{
			Write-Log "Uninstallation on $server finished."
			}
			$current +=$server
		}
	}
	if ($failed.length -gt 0)
	{
		foreach ($server in $failed)
		{
			Out-File -FilePath $failed_file -InputObject $server -Append -Encoding "UTF8"
		}
		
	}
	Write-Log "Removing Ambari folder"
	Remove-Item $destination -force -Recurse
	Write-Log "Removing shortcut"
    $shortcuts = @("$env:USERPROFILE\Desktop\Stop Ambari SCOM Server.lnk","$env:USERPROFILE\Desktop\Start Ambari SCOM Server.lnk","$env:USERPROFILE\Desktop\Browse Ambari API.url","$env:USERPROFILE\Desktop\Browse Ambari Metrics.url")
    foreach ($shortcut in $shortcuts)
    {
    	Remove-Item $shortcut -Force
    }
    Write-Log "Removing service"
    StopAndDeleteAmbariService "ambariscom"
    $vars = @("HDP_LAYOUT","START_SERVICES","RECREATE_DB","AMB_DATA_DIR")
    foreach ($var in $vars)
    {
        [Environment]::SetEnvironmentVariable($var,$null,"Machine")
    }
    Write-Log "UNINSTALLATION COMPLETE "
}

try
{
    $scriptDir = Resolve-Path (Split-Path $MyInvocation.MyCommand.Path)
    $utilsModule = Import-Module -Name "$scriptDir\..\resources\Winpkg.Utils.psm1" -ArgumentList ("AMB") -PassThru
    $apiModule = Import-Module -Name "$scriptDir\InstallApi.psm1" -PassThru
    Main $scriptDir
}
finally
{
    if( $apiModule -ne $null )
    {
        Remove-Module $apiModule
    }
    if( $utilsModule -ne $null )
    {
        Remove-Module $utilsModule
    }
}
