package cn.datax.common.qrcode;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * 二维码 工具类
 */
@Slf4j
public class QrCodeUtil {

    /**
     * 生成带图片的二维码(到destImagePath指向的File)
     *
     * @param content         二维码的内容
     * @param width           二维码的宽度(px)
     * @param height          二维码的高度(px)
     * @param embeddedImgPath 被镶嵌的图片的地址(null表示不带logo图片)
     * @param destImagePath   生成二维码图片的地址
     *
     * @return 生成的二维码文件path
     * @throws IOException     IOException
     * @throws WriterException WriterException
     */
    public static String QREncode(String content, int width, int height, String embeddedImgPath, String destImagePath) throws IOException, WriterException {
        File dest = getFile(destImagePath);
        // 图像类型
        String format = "jpg";
        Map<EncodeHintType, Object> hints = new HashMap<>(4);
        //设置UTF-8， 防止中文乱码
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        //设置二维码四周白色区域的大小
        hints.put(EncodeHintType.MARGIN, 1);
        //设置二维码的容错性
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //画二维码，记得调用multiFormatWriter.encode()时最后要带上hints参数，不然上面设置无效
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        //开始画二维码
        MatrixToImageWriter.writeToPath(bitMatrix, format, dest.toPath());
        if (null != embeddedImgPath){
            MatrixToImageConfig matrixToImageConfig = new MatrixToImageConfig(0xFF000001, 0xFFFFFFFF);
            BufferedImage bufferedImage = LogoMatrix(MatrixToImageWriter.toBufferedImage(bitMatrix, matrixToImageConfig), embeddedImgPath);
            //输出带logo图片
            ImageIO.write(bufferedImage, format, dest);
        }
        log.info("generate Qr code file {}", destImagePath);
        return destImagePath;
    }

    /**
     * 生成带图片的二维码(到outputStream流)
     *
     * @param content         二维码的内容
     * @param width           二维码的宽度(px)
     * @param height          二维码的高度(px)
     * @param embeddedImgPath 被镶嵌的图片的地址(null表示不带logo图片)
     * @param outputStream   生成二维码图片的地址
     *
     * @return 生成的二维码文件path
     * @throws IOException     IOException
     * @throws WriterException WriterException
     */
    public static void QREncode(String content, int width, int height, String embeddedImgPath, OutputStream outputStream) throws IOException, WriterException {
        // 图像类型
        String format = "jpg";
        Map<EncodeHintType, Object> hints = new HashMap<>(4);
        //设置UTF-8， 防止中文乱码
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        //设置二维码四周白色区域的大小
        hints.put(EncodeHintType.MARGIN, 1);
        //设置二维码的容错性
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //画二维码，记得调用multiFormatWriter.encode()时最后要带上hints参数，不然上面设置无效
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        //开始画二维码
        MatrixToImageConfig matrixToImageConfig = new MatrixToImageConfig(0xFF000001, 0xFFFFFFFF);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix, matrixToImageConfig);
        if (null != embeddedImgPath){
            bufferedImage = LogoMatrix(bufferedImage, embeddedImgPath);
        }
        //输出带logo图片
        boolean result = ImageIO.write(bufferedImage, format, outputStream);
        log.info("generate Qr code file to OutputStream {}", result ? "success" : "fail");
    }

    /**
     * 识别二维码内容信息
     *
     * @param file 二维码图片文件
     *
     * @return 二维码内容
     * @throws NotFoundException NotFoundException
     * @throws IOException       IOException
     */
    public static String QRReader(File file) throws NotFoundException, IOException {
        BufferedImage bufferedImage;
        bufferedImage = ImageIO.read(file);
        if (bufferedImage == null) {
            return null;
        }
        String data = decodeQrCode(bufferedImage);
        bufferedImage.flush();
        log.info("Qr code from [{}] data is -> {}", file.getAbsolutePath(), data);
        return data;
    }

    /**
     * 识别二维码内容信息
     *
     * @param is 二维码图片文件流
     *
     * @return 二维码内容
     * @throws NotFoundException NotFoundException
     * @throws IOException       IOException
     */
    public static String QRReader(InputStream is) throws NotFoundException, IOException {
        BufferedImage bufferedImage;
        bufferedImage = ImageIO.read(is);
        if (bufferedImage == null) {
            return null;
        }
        String data = decodeQrCode(bufferedImage);
        bufferedImage.flush();
        log.info("Qr code from InputStream data is -> {}", data);
        return data;
    }

    // ---------------------------------------------------以下为辅助方法、辅助类-----------------------------------------

    /**
     * 获取文件(顺带创建文件夹，如果需要的话)
     *
     * @param filePath 文件path
     * @return 文件对象
     */
    private static File getFile(String filePath) {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            boolean result = file.getParentFile().mkdirs();
            log.info("create directory {} {}", file.getParent(), result);
        }
        return file;
    }

    /**
     * 二维码添加logo
     *
     * @param matrixImage 源二维码图片
     * @param embeddedImgPath logo图片地址
     * @return 返回带有logo的二维码图片
     */
    public static BufferedImage LogoMatrix(BufferedImage matrixImage, String embeddedImgPath) throws IOException{
        /**
         * 读取二维码图片，并构建绘图对象
         */
        Graphics2D g2 = matrixImage.createGraphics();
        int matrixWidth = matrixImage.getWidth();
        int matrixHeigh = matrixImage.getHeight();
        /**
         * 读取Logo图片
         */
        File logoFile = getFile(embeddedImgPath);
        BufferedImage logo = ImageIO.read(logoFile);
        // 开始绘制图片
        g2.drawImage(logo, matrixWidth/5*2, matrixHeigh/5*2, matrixWidth/5, matrixHeigh/5, null);
        BasicStroke stroke = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        // 设置笔画对象
        g2.setStroke(stroke);
        // 指定弧度的圆角矩形
        RoundRectangle2D.Float round = new RoundRectangle2D.Float(matrixWidth/5*2, matrixHeigh/5*2, matrixWidth/5, matrixHeigh/5, 20, 20);
        g2.setColor(Color.white);
        // 绘制圆弧矩形
        g2.draw(round);
        // 设置logo 有一道灰色边框
        BasicStroke stroke2 = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        // 设置笔画对象
        g2.setStroke(stroke2);
        RoundRectangle2D.Float round2 = new RoundRectangle2D.Float(matrixWidth/5*2+2, matrixHeigh/5*2+2, matrixWidth/5-4, matrixHeigh/5-4, 20, 20);
        g2.setColor(new Color(128, 128, 128));
        // 绘制圆弧矩形
        g2.draw(round2);
        g2.dispose();
        matrixImage.flush();
        return matrixImage;
    }

    /**
     * 识别二维码内容信息
     *二维码图片信息BufferedImage
     * @param image
     *
     * @return 二维码内容
     * @throws NotFoundException NotFoundException
     */
    private static String decodeQrCode(BufferedImage image) throws NotFoundException {
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        HashMap<DecodeHintType, Object> hints = new HashMap<>(4);
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        Result result = new MultiFormatReader().decode(bitmap, hints);
        return result.getText();
    }

    public static void main(String[] args) throws IOException, WriterException, NotFoundException {
        // 生成二维码
//        QrCodeUtil.QREncode("java开发", 500, 500, null, "F://qrcode2//二维码.jpg");
//        // 生成带图片的二维码
//        QrCodeUtil.QREncode("java开发", 500, 500, "F://qrcode2//1.jpg", "F://qrcode2//带图片的二维码.jpg");
//        // 识别二维码
//        QrCodeUtil.QRReader(new File("F://qrcode2//二维码.jpg"));
//        QrCodeUtil.decodeQrCode(new FileInputStream("F://qrcode//带文字带图片的二维码.jpg"));
    }
}
