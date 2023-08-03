/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.parquet.io;

/**
 * Copy from hive-apache package, because include hive-apache will cause class conflict
 */
public final class ColumnIOUtil {
  private ColumnIOUtil() {

  }

  public static int columnDefinitionLevel(ColumnIO column) {
    return column.getDefinitionLevel();
  }

  public static int columnRepetitionLevel(ColumnIO column) {
    return column.getRepetitionLevel();
  }
}
