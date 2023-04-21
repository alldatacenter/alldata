package org.dromara.cloudeon.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;

import java.io.File;

public class ImageUtil {

    /**
     * 读取图片转base64
     * @param imgFile
     * @return
     */
    public static String GetImageStr(String imgFile) {//将图片文件转化为字节数组字符串，并对其进行Base64编码处理
        // 读取图片文件
        File imageFile = new File(imgFile);
        // 将图片文件转换为 Base64 编码
        String base64 = Base64.encode(FileUtil.readBytes(imageFile));
        // 输出 Base64 编码
        return base64;
    }

    public static void main(String[] args) {
        System.out.println(GetImageStr("/Users/huzekang/openSource/e-mapreduce/cloudeon-stack/UDH-1.0.0/hdfs/icons/app.png"));;
    }

}
