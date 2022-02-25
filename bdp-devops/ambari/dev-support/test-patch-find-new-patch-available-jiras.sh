#!/bin/bash
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

if [ "${TESTPATCHDEBUG}" == "true" ] ; then
  set -x
fi

BASEDIR=$(pwd)
TEMPDIR=${BASEDIR}/tmp

JIRAAVAILPATCHQUERY="https://issues.apache.org/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+in+%28AMBARI%29+AND+status+%3D+%22Patch+Available%22+ORDER+BY+updated+DESC&tempMax=1000"
TESTPATCHJOBURL="https://builds.apache.org/job/Ambari-trunk-test-patch"
# TESTPATCHJOBURL="http://10.103.219.57:8080/job/Ambari-trunk-test-patch"
TOKEN=""
SUBMIT="false"
DELETEHISTORYFILE="false"

RUNTESTSFILE=${BASEDIR}/TESTED_PATCHES.txt

printUsage() {
  echo "Usage: $0 <OPTIONS>"
  echo "          --submit --token=<AMBARI PRECOMMIT JOB TOKEN>"
  echo "          [--delete-history-file]"
  echo "          [--script-debug]"
  echo
}
###############################################################################
parseArgs() {
  for i in $*
  do
    case $i in
    --submit)
      SUBMIT="true"
      ;;
    --token=*)
      TOKEN=${i#*=}
      ;;
    --script-debug)
      DEBUG="-x"
      ;;
    --delete-history-file)
      DELETEHISTORYFILE="true"
      ;;
    *)
      echo "Invalid option"
      echo
      printUsage
      exit 1
      ;;
    esac
  done
  if [[ "$SUBMIT" == "true" && "${TOKEN}" == "" ]] ; then
    echo "Token has not been specified"
    echo
    printUsage
    exit 1
  fi
}
###############################################################################
findAndSubmitAvailablePatches() {

## Grab all the key (issue numbers) and largest attachment id for each item in the XML
curl --fail --location --retry 3 "${JIRAAVAILPATCHQUERY}" > ${TEMPDIR}/patch-availables.xml
if [ "$?" != "0" ] ; then
    echo "Could not retrieve available patches from JIRA"
    exit 1
fi
xpath -q -e "//item/key/text() | //item/attachments/attachment[not(../attachment/@id > @id)]/@id" ${TEMPDIR}/patch-availables.xml > ${TEMPDIR}/patch-attachments.element

### Replace newlines with nothing, then replace id=" with =, then replace " with newline
### to yield lines with pairs (issueNumber,largestAttachmentId). Example: AMBARI-123,456984
cat ${TEMPDIR}/patch-attachments.element | awk '{ if ( $1 ~ /^AMBARI\-/) {JIRA=$1 }; if ($1 ~ /id=/) { print JIRA","$1} }' | sed 's/id\="//' | sed 's/"//' > ${TEMPDIR}/patch-availables.pair

### Iterate through issue list and find the (issueNumber,largestAttachmentId) pairs that have
### not been tested (ie don't already exist in the patch_tested.txt file
touch ${RUNTESTSFILE}
cat ${TEMPDIR}/patch-availables.pair | while read PAIR ; do
  set +e
  COUNT=`grep -c "$PAIR" ${RUNTESTSFILE}`
  set -e
  if [ "$COUNT" -lt "1" ] ; then
    ### Parse $PAIR into project, issue number, and attachment id
    ISSUE=`echo $PAIR | sed -e "s/,.*$//"`
    echo "Found new patch for issue $ISSUE"
    if [ "$SUBMIT" == "true" ]; then
      ### Kick off job
      echo "Submitting job for issue $ISSUE"
      curl --fail --location --retry 3 "${TESTPATCHJOBURL}/buildWithParameters?token=${TOKEN}&JIRA_NUMBER=${ISSUE}" > /dev/null
      if [ "$?" != "0" ] ; then
        echo "Could not submit precommit job for $ISSUE"
        exit 1
      fi
    fi
    ### Mark this pair as tested by appending to file
    echo "$PAIR" >> ${RUNTESTSFILE}
  fi
done
}
###############################################################################

mkdir ${TEMPDIR} 2>&1 $STDOUT

parseArgs "$@"

if [ -n "${DEBUG}" ] ; then
  set -x
fi

if [ "${DELETEHISTORYFILE}" == "true" ] ; then
  rm ${RUNTESTSFILE}
fi

findAndSubmitAvailablePatches

exit 0
