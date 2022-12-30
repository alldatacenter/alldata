/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.client.producer.qltystats;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TokenConstants;

public class BrokerStatsItemSet {

    private AtomicLong sendNum = new AtomicLong();
    private AtomicLong receiveNum = new AtomicLong();
    private AtomicLong recSucNum = new AtomicLong();
    private AtomicLong sendNumSnapshot = new AtomicLong();
    private AtomicLong recSucNumSnapshot = new AtomicLong();
    private long dltSendNum = 0;
    private long dltRecSucNum = 0;

    public BrokerStatsItemSet() {
        sendNum.set(0);
        receiveNum.set(0);
        recSucNum.set(0);
        sendNumSnapshot.set(0);
        recSucNumSnapshot.set(0);
        dltSendNum = 0;
        dltRecSucNum = 0;

    }

    public long getSendNum() {
        return sendNum.get();
    }

    public long getReceiveNum() {
        return receiveNum.get();
    }

    public long getRecSucNum() {
        return recSucNum.get();
    }

    public long incrementAndGetSendNum() {
        return sendNum.incrementAndGet();
    }

    public long incrementAndGetRecNum() {
        return receiveNum.incrementAndGet();
    }

    public long incrementAndGetRecSucNum() {
        return recSucNum.incrementAndGet();
    }

    public long getDltAndSnapshotSendNum() {
        long tmpSendNum = sendNum.get();
        long dltNum = tmpSendNum - sendNumSnapshot.get();
        sendNumSnapshot.set(tmpSendNum);
        if (dltNum < 0) {
            dltNum += Long.MAX_VALUE;
        }
        dltSendNum = dltNum;
        return dltNum;
    }

    public long getDltSendNum() {
        return dltSendNum;
    }

    public long getDltRecSucNum() {
        return dltRecSucNum;
    }

    public long getDltAndSnapshotRecSucNum() {
        long tmpRecSucNum = recSucNum.get();
        long dltNum = tmpRecSucNum - recSucNumSnapshot.get();
        recSucNumSnapshot.set(tmpRecSucNum);
        if (dltNum < 0) {
            dltNum += Long.MAX_VALUE;
        }
        dltRecSucNum = dltNum;
        return dltNum;
    }

    @Override
    public String toString() {

        return "sendNum:" + this.sendNum.longValue() + TokenConstants.ATTR_SEP + ",receiveNum:"
                + this.receiveNum.longValue() + TokenConstants.ATTR_SEP + ",recSucNum:"
                + this.recSucNum.longValue() + TokenConstants.ATTR_SEP + ",sendNumSnapshot:"
                + this.sendNumSnapshot.longValue() + TokenConstants.ATTR_SEP + ",recSucNumSnapshot:"
                + this.recSucNumSnapshot.longValue() + TokenConstants.ATTR_SEP + ",dltSendNum:"
                + this.dltSendNum + TokenConstants.ATTR_SEP + ",dltRecSucNum:" + this.dltSendNum;
    }
}
