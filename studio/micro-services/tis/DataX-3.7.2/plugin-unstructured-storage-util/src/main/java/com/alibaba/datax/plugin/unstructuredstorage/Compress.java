package com.alibaba.datax.plugin.unstructuredstorage;

import com.alibaba.datax.plugin.unstructuredstorage.reader.Constant;
import com.alibaba.datax.plugin.unstructuredstorage.reader.ExpandLzopInputStream;
import com.alibaba.datax.plugin.unstructuredstorage.reader.ZipCycleInputStream;
import io.airlift.compress.snappy.SnappyFramedInputStream;
import io.airlift.compress.snappy.SnappyFramedOutputStream;
import org.anarres.lzo.LzoCompressor1x_1;
import org.anarres.lzo.LzoDecompressor1x_safe;
import org.anarres.lzo.LzoInputStream;
import org.anarres.lzo.LzoOutputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.function.Function;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-16 15:31
 **/
public enum Compress {
    noCompress("none", (input) -> {
        return input;
//        return new BufferedReader(new InputStreamReader(input,
//                encoding), Constant.DEFAULT_BUFFER_SIZE);
    }, ((output) -> {
        return output;
//        return new BufferedWriter(new OutputStreamWriter(output,
//                encode), Constant.DEFAULT_BUFFER_SIZE);
    })
    ),
    lzo_deflate("lzo_deflate", (input) -> {
        LzoInputStream lzoInputStream = new LzoInputStream(
                input, new LzoDecompressor1x_safe());
        return lzoInputStream;
//        return new BufferedReader(new InputStreamReader(
//                lzoInputStream, encoding));
    }, ((output) -> {
        LzoOutputStream lzoOutStream = new LzoOutputStream(output, new LzoCompressor1x_1());
        return lzoOutStream;
        // return new BufferedWriter(new OutputStreamWriter(lzoOutStream, encoding));
    })),
    lzo("lzo", (input) -> {
        try {
            LzoInputStream lzopInputStream = new ExpandLzopInputStream(
                    input);
            return lzopInputStream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        return new BufferedReader(new InputStreamReader(
//                lzopInputStream, encoding));
    }, null),
    gzip("gzip", (input) -> {

        try {
            CompressorInputStream compressorInputStream = new GzipCompressorInputStream(
                    input);
            return compressorInputStream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }, ((output) -> {
        try {
            return new GzipCompressorOutputStream(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    })),
    bzip2("bzip2", (input) -> {
        try {
            CompressorInputStream compressorInputStream = new BZip2CompressorInputStream(
                    input);
            return compressorInputStream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        return new BufferedReader(new InputStreamReader(
//                compressorInputStream, encoding), Constant.DEFAULT_BUFFER_SIZE);
    }, ((output) -> {
        try {
            BZip2CompressorOutputStream boutput = new BZip2CompressorOutputStream(output);
            return boutput;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    })),
    framingSnappy("framing-snappy", ((input) -> {
        try {
            InputStream snappyInputStream = new SnappyFramedInputStream(input);
            return snappyInputStream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }), ((output) -> {
        try {
            return new SnappyFramedOutputStream(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    })),
    zip("zip", (input) -> {
        ZipCycleInputStream zipCycleInputStream = new ZipCycleInputStream(
                input);
        return zipCycleInputStream;
    }, null);

    public static Compress parse(String token) {
        if (StringUtils.isEmpty(token)) {
            return noCompress;
        }
        for (Compress compress : Compress.values()) {
            if (compress.token.equalsIgnoreCase(token)) {
                return compress;
            }
        }
        throw new IllegalArgumentException("token is illegal:" + token);
    }

    public final String token;
    private final Function<InputStream, InputStream> unCompressDecorate;
    private final Function<OutputStream, OutputStream> compressCreator;

    private Compress(String token, Function<InputStream, InputStream> decorate, Function<OutputStream, OutputStream> compressCreator) {
        this.token = token;
        this.unCompressDecorate = decorate;
        this.compressCreator = compressCreator;
    }

    public boolean supportWriter() {
        return compressCreator != null;
    }

    public BufferedReader decorate(InputStream inputStream, String encoding) throws UnsupportedEncodingException, IOException {
        InputStream input = this.unCompressDecorate.apply(inputStream);
        return new BufferedReader(new InputStreamReader(input,
                encoding), Constant.DEFAULT_BUFFER_SIZE);
    }

    public BufferedWriter decorate(OutputStream outputStream, String encoding) throws UnsupportedEncodingException, IOException {
        OutputStream output = compressCreator.apply(outputStream);
        return new BufferedWriter(new OutputStreamWriter(output, encoding), Constant.DEFAULT_BUFFER_SIZE);
    }

}
