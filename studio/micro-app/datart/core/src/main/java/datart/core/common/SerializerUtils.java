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

package datart.core.common;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SerializerUtils {

    public static <T extends Serializable> void serializeObjectToFile(T obj, boolean gzip, String path) throws IOException {
        FileUtils.mkdirParentIfNotExist(path);
        OutputStream outputStream;
        if (gzip) {
            outputStream = new GZIPOutputStream(new FileOutputStream(path));
        } else {
            outputStream = new FileOutputStream(path);
        }
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(obj);
        }
    }

    public static <T> T deserializeFile(String path, boolean gzipped) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream;
        if (gzipped) {
            inputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(path)));
        } else {
            inputStream = new ObjectInputStream(new FileInputStream(path));
        }
        return (T) inputStream.readObject();
    }

}
