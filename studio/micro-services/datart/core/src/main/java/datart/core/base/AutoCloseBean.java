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
package datart.core.base;

import javax.management.timer.Timer;
import java.io.Closeable;
import java.io.IOException;
import java.util.Date;

/**
 * 提供一个可超时自动关闭的计时器，超时时间通过 timeoutMillis() 方法设置。
 * 通过refresh()方法可刷新超时时间
 * 超时后Timer自动调用 close方法
 */
public abstract class AutoCloseBean extends Timer implements Closeable {

    public AutoCloseBean() {
        start();
        initTimer();
    }

    private void initTimer() {
        addNotificationListener((notification, handback) -> {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, null, null);
        addNewNotification();
    }

    /**
     * @return 超时关闭的时间，单位：毫秒
     */
    public abstract int timeoutMillis();

    /**
     * 刷新关闭时间
     */
    public void refresh() {
        removeAllNotifications();
        addNewNotification();
    }

    private void addNewNotification() {
        addNotification("close", "time to close", this, new Date(System.currentTimeMillis() + timeoutMillis()));
    }


}
