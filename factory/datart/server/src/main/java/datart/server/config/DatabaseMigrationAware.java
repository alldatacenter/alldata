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

package datart.server.config;

import datart.core.migration.DatabaseMigration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DatabaseMigrationAware implements ApplicationContextAware, Ordered {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String migrationEnable = applicationContext.getEnvironment().getProperty("datart.migration.enable");
        if (migrationEnable == null || "false".equals(migrationEnable)) {
            return;
        }
        DatabaseMigration databaseMigration = applicationContext.getBean(DatabaseMigration.class);
        try {
            databaseMigration.migration();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
