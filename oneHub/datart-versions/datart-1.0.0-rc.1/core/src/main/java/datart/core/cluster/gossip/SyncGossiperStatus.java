/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.core.cluster.gossip;


import javax.management.Notification;
import javax.management.NotificationListener;
import java.util.Random;

public class SyncGossiperStatus implements NotificationListener {

    private final Random random = new Random();

    @Override
    public void handleNotification(Notification notification, Object handback) {

        GossiperManager manager = (GossiperManager) handback;

        synchronized (manager.getRemoteGossipers()) {

            if (manager.getShutdown().get()) {
                return;
            }

            if (manager.getRemoteGossipers() == null || manager.getRemoteGossipers().size() == 0) {
                manager.resetTimer();
                return;
            }
            Gossiper gossiper = randomGossiper(manager);
            
        }

    }


    private Gossiper randomGossiper(GossiperManager manager) {
        if (manager.getRemoteGossipers() == null || manager.getRemoteGossipers().size() == 0) {
            return null;
        }
        int randomIndex = random.nextInt(manager.getRemoteGossipers().size());
        return manager.getRemoteGossipers().get(randomIndex);
    }

}
