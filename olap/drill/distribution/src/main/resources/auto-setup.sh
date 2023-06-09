#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This file is invoked by drill-config.sh during a Drillbit startup and provides
# default checks and autoconfiguration.
# Distributions should not put anything in this file. Checks can be
# specified in ${DRILL_HOME}/conf/distrib-setup.sh
# Users should not put anything in this file. Additional checks can be defined
# and put in ${DRILL_CONF_DIR}/drill-setup.sh instead.
# To FAIL any check, return with a non-zero return code
# e.g.
# if [ $status == "FAILED" ]; return 1; fi

###==========================================================================
# FEATURES
# 1. Provides checks and auto-configuration for memory settings
###==========================================================================

# Convert Java memory value to MB
function valueInMB() {
  if [ -z "$1" ]; then echo ""; return; fi
  local inputTxt=`echo $1| tr '[A-Z]' '[a-z]'`
  local inputValue=`echo ${inputTxt:0:${#inputTxt}-1}`;
  # Extracting Numeric Value
  if [[ "$inputTxt" == *g ]]; then
    let valueInMB=$inputValue*1024
  elif [[ "$DbitMaxProcMem" == *k ]]; then
    let valueInMB=$inputValue/1024
  elif [[ "$inputTxt" == *m ]]; then
    let valueInMB=$inputValue
  elif [[ "$inputTxt" == *% ]]; then
    #TotalRAM_inMB*percentage [Works on Linux]
    let valueInMB=$inputValue*$totalRAM_inMB/100;
  else
    echo error;
    return 1;
  fi
  echo "$valueInMB"
  return
}

# Convert Java memory value to GB
function valueInGB() {
  if [ -z "$1" ]; then echo ""; return; fi
  local inputTxt=`echo $1| tr '[A-Z]' '[a-z]'`
  local inputValue=`echo ${inputTxt:0:${#inputTxt}-1}`;
  # Extracting Numeric Value
  if [[ "$inputTxt" == *g ]]; then
    let valueInGB=$inputValue
  elif [[ "$DbitMaxProcMem" == *k ]]; then
    let valueInGB=$inputValue/1024/1024
  elif [[ "$inputTxt" == *m ]]; then
    let valueInGB=$inputValue/1024
  elif [[ "$inputTxt" == *% ]]; then
    #TotalRAM_inMB*percentage [Works on Linux]
    let valueInGB=$inputValue*`cat /proc/meminfo | grep MemTotal | tr ' ' '\n'| grep '[0-9]'`/1024/1024/100;
  else
    echo error;
    return 1;
  fi
  echo "$valueInGB"
  return
}

# Estimates code cache based on total heap and direct
function estCodeCacheInMB() {
  local totalHeapAndDirect=$1
  if [ $totalHeapAndDirect -le 4096 ]; then echo 512;
  elif [ $totalHeapAndDirect -le 10240 ]; then echo 768;
  else echo 1024;
  fi
}

#Print Current Allocation
function printCurrAllocation()
{
  if [ -n "$DRILLBIT_MAX_PROC_MEM" ]; then echo -e "    DRILLBIT_MAX_PROC_MEM=$DRILLBIT_MAX_PROC_MEM" 1>&2; fi
  if [ -n "$DRILL_HEAP" ]; then echo -e "    DRILL_HEAP=$DRILL_HEAP" 1>&2; fi
  if [ -n "$DRILL_MAX_DIRECT_MEMORY" ]; then echo -e "    DRILL_MAX_DIRECT_MEMORY=$DRILL_MAX_DIRECT_MEMORY" 1>&2; fi
  if [ -n "$DRILLBIT_CODE_CACHE_SIZE" ]; then
    echo -e "    DRILLBIT_CODE_CACHE_SIZE=$DRILLBIT_CODE_CACHE_SIZE " 1>&2
    echo -e "    *NOTE: It is recommended not to specify DRILLBIT_CODE_CACHE_SIZE as this will be auto-computed based on the HeapSize and would not exceed 1GB" 1>&2
  fi
}

#============================================================================
# Check and auto-configuration for memory settings
#----------------------------------------------------------------------------
#Default (Track status of this check: "" => Continue checking ; "PASSED" => no more check required)
AutoMemConfigStatus=""

#Computing existing system information
# Tested on Linux (CentOS/RHEL/Ubuntu); Cygwin (Win10Pro-64bit)
if [[ "$OSTYPE" == *linux* ]] || [[ "$OSTYPE" == cygwin* ]]; then
  let totalRAM_inMB=`cat /proc/meminfo | grep MemTotal | tr ' ' '\n'| grep '[0-9]'`/1024
  let freeRAM_inMB=`cat /proc/meminfo | grep MemFree | tr ' ' '\n'| grep '[0-9]'`/1024
elif [[ "$OSTYPE" == darwin* ]]; then
  # Mac OSX
  #Refer for math: https://apple.stackexchange.com/a/196925
  #Page Size
  let macOSPageSize=`vm_stat | grep 'page size' | grep -o -E '[0-9]+'`
  #MemoryUsage on MacOS
  let freePg=`vm_stat | grep free | awk '{ print $NF }' | sed 's/\.//'`
  let activePg=`vm_stat | grep -w 'active:' | awk '{ print $NF }' | sed 's/\.//'`
  let speculativePg=`vm_stat | grep speculative | awk '{ print $NF }' | sed 's/\.//'`
  let fileCachePg=`vm_stat | grep File-backed | awk '{ print $NF }' | sed 's/\.//'`
  let wiredMemPg=`vm_stat | grep 'wired down' | awk '{ print $NF }' | sed 's/\.//'`
  let compressedPg=`vm_stat | grep 'occupied by compressor' | awk '{ print $NF }' | sed 's/\.//'`
  #Total
  let totalRAM_inPages=$freePg+$activePg+$speculativePg+$fileCachePg+$wiredMemPg+$compressedPg
  let totalRAM_inMB=$totalRAM_inPages*$macOSPageSize/1048576
  let freeRAM_inMB=$freePg*$macOSPageSize/1048576
elif [[ "$OSTYPE" == "msys" ]]; then
  # Msys env on MinGW (TODO: Pending verification)
  let totalRAM_inMB=`cat /proc/meminfo | grep MemTotal | tr ' ' '\n'| grep '[0-9]'`/1024
  let freeRAM_inMB=`cat /proc/meminfo | grep MemFree | tr ' ' '\n'| grep '[0-9]'`/1024
else
  # Unknown OS
  echo `date +%Y-%m-%d" "%H:%M:%S`"  [WARN] Unknown OS ("$OSTYPE"). Will not attempt to auto-configure memory" 1>&2
  AutoMemConfigStatus="PASSED"
fi

#Read current values
DbitMaxProcMem=$(valueInMB $DRILLBIT_MAX_PROC_MEM)
DbitMaxDirectMem=$(valueInMB $DRILL_MAX_DIRECT_MEMORY)
DbitMaxHeapMem=$(valueInMB $DRILL_HEAP)
DbitMaxCodeCacheMem=$(valueInMB $DRILLBIT_CODE_CACHE_SIZE)

# Alert for %age usage
if [[ "$DRILLBIT_MAX_PROC_MEM" == *% ]] && [ -z "$AutoMemConfigStatus" ]; then
  echo `date +%Y-%m-%d" "%H:%M:%S`"  [WARN] "$DRILLBIT_MAX_PROC_MEM" of System Memory ("$(valueInGB $totalRAM_inMB'm')" GB) translates to "$(valueInGB $DbitMaxProcMem'm')" GB" 1>&2
fi

### Performing Auto-Configuration
if [ -z "$DbitMaxProcMem" ] && [ -z "$AutoMemConfigStatus" ]; then
  if [ -n "$DbitMaxDirectMem" ] && [ -n "$DbitMaxHeapMem" ]; then
    ## [SCENARIO 1]: TotalCap is NOT Defined, but Heap&Direct ARE Defined (i.e. no limit)
    let currTotal=$DbitMaxDirectMem+$DbitMaxHeapMem
    #Estimating CodeCache size of current total
    if [ -z "$DbitMaxCodeCacheMem" ]; then export DRILLBIT_CODE_CACHE_SIZE=$(estCodeCacheInMB $currTotal)'m'; fi
  fi
  # Default values will be loaded for unspecified memory parameters
  AutoMemConfigStatus="PASSED"
elif [ -z "$AutoMemConfigStatus" ]; then
  ## Scenario: Total IS Defined
  if [ -z "$DbitMaxCodeCacheMem" ]; then
    let DbitMaxCodeCacheMem=$(estCodeCacheInMB $DbitMaxProcMem)
    export DRILLBIT_CODE_CACHE_SIZE=$DbitMaxCodeCacheMem'm'
  fi
  if [ -n "$DbitMaxHeapMem" ] && [ -n "$DbitMaxDirectMem" ]; then
    ## [SCENARIO 2]: Heap & Direct ARE Defined
    let calcTotalInMB=$DbitMaxDirectMem+$DbitMaxHeapMem+$DbitMaxCodeCacheMem
    # Fail if exceeding process limit
    if [ $calcTotalInMB -gt $DbitMaxProcMem ]; then
      echo "[ERROR]    Unable to start Drillbit due to memory constraint violations" 1>&2
      echo "  Total Memory Requested : "$(valueInGB $calcTotalInMB'm')" GB" 1>&2
      echo "  Check the following settings to possibly modify (or increase the Max Memory Permitted):" 1>&2
      printCurrAllocation
      exit 127
    else
      #All numbers align
      let deltaInGB=($DbitMaxProcMem-$calcTotalInMB)/1024
      if [ $deltaInGB -gt 1 ]; then
        echo "[WARN] You have an allocation of "$deltaInGB" GB that is currently unused from a total of "$(valueInGB $DbitMaxProcMem'm')" GB. You can increase your existing memory configuration to use this extra memory" 1>&2
        printCurrAllocation
      fi
    fi
  elif [ -n "$DbitMaxHeapMem" ] && [ -z "$DbitMaxDirectMem" ]; then
    ## [SCENARIO 3]: Total and only Heap is defined
    echo "[WARN] Only DRILL_HEAP is defined. Auto-configuring for Direct memory" 1>&2
    let DbitMaxDirectMem=$DbitMaxProcMem-$DbitMaxHeapMem-$DbitMaxCodeCacheMem
  elif [ -z "$DbitMaxHeapMem" ] && [ -n "$DbitMaxDirectMem" ]; then
    ## [SCENARIO 4]: Total and only Direct is defined
    echo "[WARN] Only DRILL_MAX_DIRECT_MEMORY is defined. Auto-configuring for Heap" 1>&2
    let DbitMaxHeapMem=$DbitMaxProcMem-$DbitMaxDirectMem-$DbitMaxCodeCacheMem
  elif [ -z "$DbitMaxDirectMem" ] && [ -z "$DbitMaxHeapMem" ]; then
    ## [SCENARIO 5]: Only Total is defined
    echo "[WARN] Only DRILLBIT_MAX_PROC_MEM is defined. Auto-configuring for Heap & Direct memory" 1>&2
    ## Compute Direct & Heap
    let DbitMaxProcMemInGB=$(valueInGB $DbitMaxProcMem'm')
    let DbitMaxHeapMemInGB=`echo $DbitMaxProcMemInGB | awk '{heap=-13.2+6.12*log($1); if (heap<1) {heap=1}; printf "%0.0f\n", heap }'`
    let DbitMaxHeapMem=$(valueInMB $DbitMaxHeapMemInGB'g')
    let DbitMaxDirectMem=$DbitMaxProcMem-$DbitMaxHeapMem-$DbitMaxCodeCacheMem
  fi
  ## Export computed values
  export DRILL_HEAP=$(valueInGB $DbitMaxHeapMem'm')"G"
  export DRILL_MAX_DIRECT_MEMORY=$(valueInGB $DbitMaxDirectMem'm')"G"
  export DRILLBIT_CODE_CACHE_SIZE=$DbitMaxCodeCacheMem'm'
fi

### Broad check for System Level capacity
if [ -z "$AutoMemConfigStatus" ]; then
  # Rereading for recently exported env var
  DbitMaxDirectMem=$(valueInMB $DRILL_MAX_DIRECT_MEMORY)
  DbitMaxHeapMem=$(valueInMB $DRILL_HEAP)
  DbitMaxCodeCacheMem=$(valueInMB $DRILLBIT_CODE_CACHE_SIZE)
  echo "[INFO] Attempting to start up Drill with the following settings" 1>&2
  echo "  DRILL_HEAP="$DRILL_HEAP 1>&2
  echo "  DRILL_MAX_DIRECT_MEMORY="$DRILL_MAX_DIRECT_MEMORY 1>&2
  echo "  DRILLBIT_CODE_CACHE_SIZE="$DRILLBIT_CODE_CACHE_SIZE 1>&2
  let totalDBitMem_inMB=$DbitMaxDirectMem+$DbitMaxHeapMem+$DbitMaxCodeCacheMem 1>&2
  if [ $totalDBitMem_inMB -gt $totalRAM_inMB ]; then
    echo "[ERROR] Total Memory Allocation for Drillbit ("$(valueInGB $totalDBitMem_inMB'm')"GB) exceeds total system memory ("$(valueInGB $totalRAM_inMB'm')"GB)" 1>&2
    echo "[ERROR] Drillbit not will start up. Please check your allocations" 1>&2
    exit 127
  elif [ $totalDBitMem_inMB -gt $freeRAM_inMB ]; then
    echo "[WARN] Total Memory Allocation for Drillbit ("$(valueInGB $totalDBitMem_inMB'm')"GB) exceeds available free memory ("$(valueInGB $freeRAM_inMB'm')"GB)" 1>&2
    echo "[WARN] Drillbit will start up, but can potentially crash due to oversubscribing of system memory." 1>&2
  fi
fi

#Implicit that checks have passed
AutoMemConfigStatus="PASSED"
#----------------------------------------------------------------------------
# AT THIS POINT: Check and auto-configuration for memory settings [PASSED]
#============================================================================
