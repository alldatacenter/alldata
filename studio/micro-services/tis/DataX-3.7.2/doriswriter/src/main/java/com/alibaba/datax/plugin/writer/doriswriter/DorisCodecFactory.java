package com.alibaba.datax.plugin.writer.doriswriter;

public class DorisCodecFactory {
    public DorisCodecFactory() {

    }

    public static DorisCodec createCodec(Keys writerOptions) {
//        if ( Keys.StreamLoadFormat.CSV.equals(writerOptions.getStreamLoadFormat())) {
//            Map<String, Object> props = writerOptions.getLoadProps();
//            return new DorisCsvCodec (null == props || !props.containsKey(Keys.LOAD_PROPS_COLUMN_SEPARATOR) ? null : String.valueOf(props.get(Keys.LOAD_PROPS_COLUMN_SEPARATOR)));
//        }
        //if ( Keys.StreamLoadFormat.JSON.equals(writerOptions.getStreamLoadFormat())) {
        return new DorisJsonCodec(writerOptions.getColumns());
        //}
        //throw new RuntimeException("Failed to create row serializer, unsupported `format` from stream load properties.");
    }
}
