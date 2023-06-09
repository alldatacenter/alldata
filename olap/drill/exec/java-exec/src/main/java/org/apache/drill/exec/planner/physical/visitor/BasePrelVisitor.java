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
package org.apache.drill.exec.planner.physical.visitor;

import org.apache.drill.exec.planner.physical.DirectScanPrel;
import org.apache.drill.exec.planner.physical.ExchangePrel;
import org.apache.drill.exec.planner.physical.JoinPrel;
import org.apache.drill.exec.planner.physical.LeafPrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.planner.physical.ScreenPrel;
import org.apache.drill.exec.planner.physical.WriterPrel;
import org.apache.drill.exec.planner.physical.UnnestPrel;
import org.apache.drill.exec.planner.physical.LateralJoinPrel;

public class BasePrelVisitor<RETURN, EXTRA, EXCEP extends Throwable> implements PrelVisitor<RETURN, EXTRA, EXCEP> {


  @Override
  public RETURN visitScan(ScanPrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }

  @Override
  public RETURN visitScan(DirectScanPrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }

  @Override
  public RETURN visitExchange(ExchangePrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }

  @Override
  public RETURN visitJoin(JoinPrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }

  @Override
  public RETURN visitProject(ProjectPrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }

  @Override
  public RETURN visitScreen(ScreenPrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }

  @Override
  public RETURN visitWriter(WriterPrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }

  @Override
  public RETURN visitPrel(Prel prel, EXTRA value) throws EXCEP {
    throw new UnsupportedOperationException(String.format("No visit method defined for prel %s in visitor %s.", prel, this.getClass().getName()));
  }

  @Override
  public RETURN visitUnnest(UnnestPrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }

  @Override
  public RETURN visitLateral(LateralJoinPrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }

  @Override
  public RETURN visitLeaf(LeafPrel prel, EXTRA value) throws EXCEP {
    return visitPrel(prel, value);
  }
}
