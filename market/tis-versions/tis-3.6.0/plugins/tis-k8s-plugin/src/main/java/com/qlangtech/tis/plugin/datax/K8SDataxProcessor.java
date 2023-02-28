//package com.qlangtech.tis.plugin.datax;
//
//import com.qlangtech.tis.TIS;
//import com.qlangtech.tis.datax.IDataxReader;
//import com.qlangtech.tis.datax.IDataxWriter;
//import com.qlangtech.tis.datax.impl.DataxProcessor;
//import com.qlangtech.tis.extension.Descriptor;
//import com.qlangtech.tis.extension.TISExtension;
//import com.qlangtech.tis.plugin.PluginStore;
//import com.qlangtech.tis.plugin.annotation.FormField;
//import com.qlangtech.tis.plugin.annotation.FormFieldType;
//import com.qlangtech.tis.plugin.annotation.Validator;
//import com.qlangtech.tis.plugin.k8s.K8sImage;
//
///**
// * @author: baisui 百岁
// * @create: 2021-04-07 18:49
// **/
//public class K8SDataxProcessor extends DataxProcessor {
//    public static final String KEY_FIELD_NAME = "image";
//    private IDataxReader reader;
//    private IDataxWriter writer;
//
//    @FormField(ordinal = 0, type = FormFieldType.SELECTABLE, validate = {Validator.require})
//    public String image;
//    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String zkHost;
//
//    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
//    public int channel;
//
//    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
//    public int errLimit;
//
//    @FormField(ordinal = 4, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
//    public int errPercent;
//
//    @FormField(ordinal = 5, type = FormFieldType.TEXTAREA, validate = {Validator.require})
//    public String template;
//
//    @Override
//    public String identityValue() {
//        return K8SDataxProcessor.class.getSimpleName();
//    }
//
//    @Override
//    protected int getChannel() {
//        return this.channel;
//    }
//
//    @Override
//    protected int getErrorLimitCount() {
//        return this.errLimit;
//    }
//
//    @Override
//    protected int getErrorLimitPercentage() {
//        return this.errPercent;
//    }
//
//    public void setReader(IDataxReader reader) {
//        this.reader = reader;
//    }
//
//    public void setWriter(IDataxWriter writer) {
//        this.writer = writer;
//    }
//
//    @Override
//    public IDataxReader getReader() {
//        return reader;
//    }
//
//    @Override
//    public IDataxWriter getWriter() {
//        return writer;
//    }
//
//    @TISExtension()
//    public static class DescriptorImpl extends Descriptor<DataxProcessor> {
//
//        public DescriptorImpl() {
//            super();
//            this.registerSelectOptions(KEY_FIELD_NAME, () -> {
//                PluginStore<K8sImage> images = TIS.getPluginStore(K8sImage.class);
//                return images.getPlugins();
//            });
//        }
//
//        @Override
//        public String getDisplayName() {
//            return "datax-pipe";
//        }
//    }
//}
