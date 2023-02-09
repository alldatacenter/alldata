/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.test.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class PropertiesTools {
    private Properties testProperties = new Properties();
    
    private static PropertiesTools tools = null;
    
    public String getProperties(String key) {
        return this.testProperties.getProperty(key);
    }
    
    public String getProperties(String key, String defaultValue) {
        return this.testProperties.getProperty(key, defaultValue);
    }
    
    public static PropertiesTools getInstance(final File file) throws FileNotFoundException, IOException, IllegalArgumentException {
        if(null == tools) {
            tools = new PropertiesTools();
        }
        
        if (!file.exists()) {
            throw new FileNotFoundException("File doesn't exist:  "
                                            + file.getAbsolutePath());
        }

        FileInputStream stream = new FileInputStream(file);
        try {

            tools.testProperties.load(stream);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
        
        return tools;
    }
}
