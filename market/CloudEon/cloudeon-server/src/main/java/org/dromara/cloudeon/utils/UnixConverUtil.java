package org.dromara.cloudeon.utils;
import java.io.*;

public class UnixConverUtil {

    /**
     * 将目录里的所有文本文件转换成unix格式，兼容windows下生成的脚本无法在docker容器里运行
     * @param dir
     */
    public static void convertToUnix(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(file));
                        String line = null;
                        StringBuilder stringBuilder = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }
                        reader.close();

                        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                        writer.write(stringBuilder.toString().replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
                        writer.close();

                        file.setExecutable(true, false);
                        file.setReadable(true, false);
                        file.setWritable(true, false);

                        System.out.println("Converted file " + file.getName() + " to unix format.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (file.isDirectory()) {
                    convertToUnix(file);
                }
            }
        }
    }
}
