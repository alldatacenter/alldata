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

package datart.core.migration;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

@Data
public class Migration implements Comparable<Migration> {

    private String id;

    private String version;

    private String fileName;

    private String executeUser;

    private Date executeDate;

    private boolean success;

    private Resource upgradeFile;

    private Resource rollbackFile;

    private final static String split = "__";

    private final static String fileSuffix = ".sql";

    public Migration() {

    }

    public Migration(Resource upgradeFile, Resource rollbackFile) {
        String fileName = upgradeFile.getFilename();
        String[] extractName = extractName(upgradeFile.getFilename());
        setId(extractName[0]);
        this.setVersion(extractName[1]);
        this.fileName = fileName;
        this.upgradeFile = upgradeFile;
        this.rollbackFile = rollbackFile;
    }

    @Override
    public int compareTo(Migration other) {
        int[] otherId = Arrays.stream(other.getId().split("\\.")).mapToInt(Integer::valueOf).toArray();
        int[] thisId = Arrays.stream(id.split("\\.")).mapToInt(Integer::valueOf).toArray();
        if (otherId.length != thisId.length) {
            return thisId.length - otherId.length;
        }
        for (int i = 0; i < thisId.length; i++) {
            if (thisId[i] != otherId[i]) {
                return thisId[i] - otherId[i];
            }
        }
        return 0;
    }

    private String[] extractName(String fileName) {
        String[] split = fileName.split(Migration.split);
        return new String[]{split[0].substring(1), split[1].replace(fileSuffix, "")};
    }

    public static boolean isMigrationFile(Resource resource) {
        if (resource == null) {
            return false;
        }
        String name = resource.getFilename();
        if (!StringUtils.endsWithIgnoreCase(name, fileSuffix)) {
            return false;
        }
        if (name.split(split).length != 2) {
            return false;
        }
        return name.startsWith(ScriptType.ROLLBACK.getPrefix()) || name.startsWith(ScriptType.UPGRADE.getPrefix());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Migration)) return false;
        Migration migration = (Migration) o;
        return id.equals(migration.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}