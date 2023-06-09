/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.common;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-16
 */
abstract class JarFileManager {
    // private final AppDomainInfo domainInfo;
    // final File saveFile;
    //
    // private JarFileManager(Application application, Long currentTimestamp) {
    // super();
    // // this.domainInfo = domainInfo;
    // this.saveFile = new File(ConfigFileReader.getAppDomainDir(Config
    // .getLocalRepository(), application.getDptId(), application
    // .getAppId()), String.valueOf(currentTimestamp) + ".jar");
    // }
    //
    // public File getSaveFile() {
    // return this.saveFile;
    // }
    //
    // /**
    // * @param validateCode
    // * 校验码 ，防止文件被读之前已经被人篡改过了
    // * @return
    // * @throws FileNotFoundException
    // */
    // public InputStream readFile(String validateCode)
    // throws FileNotFoundException {
    // if (!saveFile.exists()) {
    // throw new IllegalStateException("file:"
    // + saveFile.getAbsolutePath() + " is not exist can not read");
    // }
    //
    // try {
    // if (!StringUtils.equalsIgnoreCase(md5file(this.saveFile),
    // validateCode)) {
    // throw new IllegalStateException("saveFile:"
    // + this.saveFile.getAbsolutePath() + " 已经被篡改过了");
    // }
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    //
    // return new FileInputStream(this.saveFile);
    // }
    //
    // public long getFileSize() {
    // if (!saveFile.exists()) {
    // throw new IllegalStateException("file:"
    // + saveFile.getAbsolutePath() + " is not exist can not read");
    // }
    //
    // return saveFile.length();
    // }
    //
    // /**
    // *保存文件
    // *
    // * @param reader
    // * @return validate code
    // */
    // public String save(InputStream reader) throws IOException {
    //
    // if (!saveFile.exists() && !saveFile.createNewFile()) {
    // throw new IllegalStateException("file:"
    // + saveFile.getAbsolutePath() + " can not be null");
    // }
    // // 将流保存到预定目录
    // return saveFile(reader, saveFile);
    // }
    //
    // /**
    // * @param saveFile
    // * @return
    // * @throws FileNotFoundException
    // * @throws IOException
    // */
    // private static String md5file(final File saveFile)
    // throws FileNotFoundException, IOException {
    // // 保存的文件的签名
    // return md5file(new FileInputStream(saveFile));
    // }
    //
    // private static String md5file(InputStream reader) throws IOException {
    //
    // try {
    // // 保存的文件的签名
    // return ConfigFileReader.md5file(IOUtils.toByteArray(reader));
    // } finally {
    // try {
    // reader.close();
    // } catch (Throwable e) {
    // }
    // }
    // }
    //
    // public static String saveFile(final InputStream reader, final File
    // saveFile)
    // throws FileNotFoundException, IOException {
    // OutputStream writer = null;
    // try {
    //
    // writer = new FileOutputStream(saveFile);
    // IOUtils.copy(reader, writer);
    // } finally {
    // try {
    // writer.close();
    // } catch (Throwable e) {
    // }
    //
    // try {
    // reader.close();
    // } catch (Throwable e) {
    // }
    // }
    //
    // return md5file(saveFile);
    // }
}
