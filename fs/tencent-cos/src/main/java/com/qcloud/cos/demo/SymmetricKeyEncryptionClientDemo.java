package com.qcloud.cos.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.COSEncryptionClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSStaticCredentialsProvider;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.internal.crypto.CryptoConfiguration;
import com.qcloud.cos.internal.crypto.CryptoMode;
import com.qcloud.cos.internal.crypto.CryptoStorageMode;
import com.qcloud.cos.internal.crypto.EncryptionMaterials;
import com.qcloud.cos.internal.crypto.StaticEncryptionMaterialsProvider;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;

//使用客户端加密前的注意事项请参考接口文档
//这里给出使用对称秘钥AES256加密每次生成的随机对称秘钥
public class SymmetricKeyEncryptionClientDemo {
    private static final String keyFilePath = "secret.key";

	static String bucketName = "mybucket-1251668577";
    static  String key = "testKMS/sym.txt";
	static File localFile = new File("len1m.txt");

	static COSClient cosClient = createCosClient();

	static COSClient createCosClient() {
		return createCosClient("ap-guangzhou");
	}

	static COSClient createCosClient(String region) {
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 为防止请求头部被篡改导致的数据无法解密，强烈建议只使用 https 协议发起请求
        clientConfig.setHttpProtocol(HttpProtocol.https);

        SecretKey symKey = null;

        try {
            // 加载保存在文件中的秘钥, 如果不存在，请先使用buildAndSaveAsymKeyPair生成秘钥
            //buildAndSaveSymmetricKey();
            symKey = loadSymmetricAESKey();
        } catch (Exception e) {
            throw new CosClientException(e);
        }

        // 初始化 KMS 加密材料
        EncryptionMaterials encryptionMaterials = new EncryptionMaterials(symKey);
        // 使用AES/GCM模式，并将加密信息存储在文件元信息中.
        CryptoConfiguration cryptoConf = new CryptoConfiguration(CryptoMode.AesCtrEncryption)
                .withStorageMode(CryptoStorageMode.ObjectMetadata);

        //// 如果 kms 服务的 region 与 cos 的 region 不一致，则在加密信息里指定 kms 服务的 region
        //cryptoConf.setKmsRegion(kmsRegion);

        //// 如果需要可以为 KMS 服务的 cmk 设置对应的描述信息。
        //encryptionMaterials.addDescription("kms-region", "guangzhou");

        // 生成加密客户端EncryptionClient, COSEncryptionClient是COSClient的子类, 所有COSClient支持的接口他都支持。
        // EncryptionClient覆盖了COSClient上传下载逻辑，操作内部会执行加密操作，其他操作执行逻辑和COSClient一致
        COSEncryptionClient cosEncryptionClient =
                new COSEncryptionClient(new COSStaticCredentialsProvider(cred),
                        new StaticEncryptionMaterialsProvider(encryptionMaterials), clientConfig,
                        cryptoConf);

		return cosEncryptionClient;
	}
    // 这里给出了一个产生和保存秘钥信息的示例, 推荐使用256位秘钥.
    public static void buildAndSaveSymmetricKey() throws IOException, NoSuchAlgorithmException {
        // Generate symmetric 256 bit AES key.
        KeyGenerator symKeyGenerator = KeyGenerator.getInstance("AES");
        // JDK默认不支持256位长度的AES秘钥, SDK内部默认使用AES256加密数据
        // 运行时会打印如下异常信息java.security.InvalidKeyException: Illegal key size or default parameters
        // 解决办法参考接口文档的FAQ
        symKeyGenerator.init(256);
        SecretKey symKey = symKeyGenerator.generateKey();

        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(symKey.getEncoded());
        FileOutputStream keyfos = new FileOutputStream(keyFilePath);
        keyfos.write(x509EncodedKeySpec.getEncoded());
        keyfos.close();
    }

    // 这里给出了一个加载秘钥的示例
    public static SecretKey loadSymmetricAESKey() throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException {
        // Read private key from file.
        File keyFile = new File(keyFilePath);
        FileInputStream keyfis = new FileInputStream(keyFile);
        byte[] encodedPrivateKey = new byte[(int) keyFile.length()];
        keyfis.read(encodedPrivateKey);
        keyfis.close();

        // Generate secret key.
        return new SecretKeySpec(encodedPrivateKey, "AES");
    }

    static void getObjectDemo() {
        // 下载文件
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        File downloadFile = new File("downSym.txt");
        ObjectMetadata objectMetadata = cosClient.getObject(getObjectRequest, downloadFile);
		System.out.println(objectMetadata.getRequestId());
    }

	static void putObjectDemo() {
        // 上传文件
        // 这里给出putObject的示例, 对于高级API上传，只用在生成TransferManager时传入COSEncryptionClient对象即可
		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
		PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
		System.out.println(putObjectResult.getRequestId());
	}

	static void deleteObjectDemo() {
        // 删除文件
		cosClient.deleteObject(bucketName, key);
	}

    public static void main(String[] args) throws Exception {
        putObjectDemo();
        getObjectDemo();
        // 关闭
        cosClient.shutdown();
    }
}
