/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.server.rest.profile;

import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.exec.proto.UserBitShared.MajorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.MinorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile;

/**
 * Collection of comparators for comparing data in major fragment profiles, minor fragment profiles and
 * operator profiles.
 */
interface Comparators {

  final static Comparator<MajorFragmentProfile> majorId = new Comparator<MajorFragmentProfile>() {
    @Override
    public int compare(final MajorFragmentProfile o1, final MajorFragmentProfile o2) {
      return Long.compare(o1.getMajorFragmentId(), o2.getMajorFragmentId());
    }
  };

  final static Comparator<MinorFragmentProfile> minorId = new Comparator<MinorFragmentProfile>() {
    @Override
    public int compare(final MinorFragmentProfile o1, final MinorFragmentProfile o2) {
      return Long.compare(o1.getMinorFragmentId(), o2.getMinorFragmentId());
    }
  };

  final static Comparator<MinorFragmentProfile> startTime = new Comparator<MinorFragmentProfile>() {
    @Override
    public int compare(final MinorFragmentProfile o1, final MinorFragmentProfile o2) {
      return Long.compare(o1.getStartTime(), o2.getStartTime());
    }
  };

  final static Comparator<MinorFragmentProfile> lastUpdate = new Comparator<MinorFragmentProfile>() {
    @Override
    public int compare(final MinorFragmentProfile o1, final MinorFragmentProfile o2) {
      return Long.compare(o1.getLastUpdate(), o2.getLastUpdate());
    }
  };

  final static Comparator<MinorFragmentProfile> lastProgress = new Comparator<MinorFragmentProfile>() {
    @Override
    public int compare(final MinorFragmentProfile o1, final MinorFragmentProfile o2) {
      return Long.compare(o1.getLastProgress(), o2.getLastProgress());
    }
  };

  final static Comparator<MinorFragmentProfile> endTime = new Comparator<MinorFragmentProfile>() {
    @Override
    public int compare(final MinorFragmentProfile o1, final MinorFragmentProfile o2) {
      return Long.compare(o1.getEndTime(), o2.getEndTime());
    }
  };

  final static Comparator<MinorFragmentProfile> fragmentPeakMemory = new Comparator<MinorFragmentProfile>() {
    @Override
    public int compare(final MinorFragmentProfile o1, final MinorFragmentProfile o2) {
      return Long.compare(o1.getMaxMemoryUsed(), o2.getMaxMemoryUsed());
    }
  };

  final static Comparator<MinorFragmentProfile> runTime = new Comparator<MinorFragmentProfile>() {
    @Override
    public int compare(final MinorFragmentProfile o1, final MinorFragmentProfile o2) {
      return Long.compare(o1.getEndTime() - o1.getStartTime(), o2.getEndTime() - o2.getStartTime());
    }
  };

  final static Comparator<OperatorProfile> operatorId = new Comparator<OperatorProfile>() {
    @Override
    public int compare(final OperatorProfile o1, final OperatorProfile o2) {
      return Long.compare(o1.getOperatorId(), o2.getOperatorId());
    }
  };

  final static Comparator<Pair<OperatorProfile, Integer>> setupTime = new Comparator<Pair<OperatorProfile, Integer>>() {
    @Override
    public int compare(final Pair<OperatorProfile, Integer> o1, final Pair<OperatorProfile, Integer> o2) {
      return Long.compare(o1.getLeft().getSetupNanos(), o2.getLeft().getSetupNanos());
    }
  };

  final static Comparator<Pair<OperatorProfile, Integer>> processTime = new Comparator<Pair<OperatorProfile, Integer>>() {
    @Override
    public int compare(final Pair<OperatorProfile, Integer> o1, final Pair<OperatorProfile, Integer> o2) {
      return Long.compare(o1.getLeft().getProcessNanos(), o2.getLeft().getProcessNanos());
    }
  };

  final static Comparator<Pair<OperatorProfile, Integer>> waitTime = new Comparator<Pair<OperatorProfile, Integer>>() {
    @Override
    public int compare(final Pair<OperatorProfile, Integer> o1, final Pair<OperatorProfile, Integer> o2) {
      return Long.compare(o1.getLeft().getWaitNanos(), o2.getLeft().getWaitNanos());
    }
  };

  final static Comparator<Pair<OperatorProfile, Integer>> operatorPeakMemory = new Comparator<Pair<OperatorProfile, Integer>>() {
    @Override
    public int compare(final Pair<OperatorProfile, Integer> o1, final Pair<OperatorProfile, Integer> o2) {
      return Long.compare(o1.getLeft().getPeakLocalMemoryAllocated(), o2.getLeft().getPeakLocalMemoryAllocated());
    }
  };
}
