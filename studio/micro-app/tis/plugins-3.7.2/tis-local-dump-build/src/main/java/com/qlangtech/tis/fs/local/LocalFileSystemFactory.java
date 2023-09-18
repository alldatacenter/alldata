//package com.qlangtech.tis.fs.local;
//
//import com.alibaba.citrus.turbine.Context;
//import com.qlangtech.tis.extension.Descriptor;
//import com.qlangtech.tis.extension.TISExtension;
//import com.qlangtech.tis.fs.ITISFileSystem;
//import com.qlangtech.tis.fs.ITISFileSystemFactory;
//import com.qlangtech.tis.offline.FileSystemFactory;
//import com.qlangtech.tis.plugin.annotation.FormField;
//import com.qlangtech.tis.plugin.annotation.Validator;
//import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
//
//import java.io.File;
//
///**
// * 支持本地文件读
// *
// * @author: baisui 百岁
// * @create: 2021-03-02 15:47
// **/
//public class LocalFileSystemFactory extends FileSystemFactory implements ITISFileSystemFactory {
//    private transient LocalFileSystem fileSystem;
//
//    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
//    public String name;
//
//    @FormField(ordinal = 1, validate = {Validator.require})
//    public String rootDir;
//
//    @Override
//    public String identityValue() {
//        return name;
//    }
//
//
//    @Override
//    public ITISFileSystem getFileSystem() {
//        if (fileSystem == null) {
//            fileSystem = new LocalFileSystem(this.rootDir);
//        }
//        return fileSystem;
//    }
//
//    @TISExtension()
//    public static class DefaultDescriptor extends Descriptor<FileSystemFactory> {
//        @Override
//        public String getDisplayName() {
//            return "localFile";
//        }
//
//        public boolean validateRootDir(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
//            File rootDir = new File(value);
//            if (!rootDir.exists()) {
//                msgHandler.addFieldError(context, fieldName, "path:" + rootDir.getAbsolutePath() + " is not exist");
//                return false;
//            }
//            return true;
//        }
//
//    }
//}
