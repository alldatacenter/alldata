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
# limitations under the License */

bigDataDirRoot <- "/tmp/share"
myHadoopCluster <- RxHadoopMR(consoleOutput=TRUE)
rxSetComputeContext(myHadoopCluster)
source <- system.file("SampleData/AirlineDemoSmall.csv", package="RevoScaleR")
inputDir <- file.path(bigDataDirRoot,"AirlineDemoSmall")
rxHadoopMakeDir(inputDir)
rxHadoopCopyFromLocal(source, inputDir)
hdfsFS <- RxHdfsFileSystem()
colInfo <- list(DayOfWeek = list(type = "factor",
levels = c("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")))
airDS <- RxTextData(file = inputDir, missingValueString = "M", colInfo = colInfo, fileSystem = hdfsFS)
adsSummary <- rxSummary(~ArrDelay+CRSDepTime+DayOfWeek, data = airDS)
adsSummary