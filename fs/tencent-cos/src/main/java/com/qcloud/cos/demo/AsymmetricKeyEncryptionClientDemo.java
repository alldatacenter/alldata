package com.qcloud.cos.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

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

// 使用客户端加密前的注意事项请参考接口文档
// 这里给出使用非对称秘钥RSA加密每次生成的随机对称秘钥
public class AsymmetricKeyEncryptionClientDemo {
    private static final String pubKeyPath = "pub.key";
    private static final String priKeyPath = "pri.key";
    private static final SecureRandom srand = new SecureRandom();

	static String bucketName = "mybucket-1251668577";
    static  String key = "testKMS/asy.txt";
	static File localFile = new File("len1m.txt");

	static COSClient cosClient = createCosClient();

	static COSClient createCosClient() {
		return createCosClient("ap-guangzhou");
	}

	static COSClient createCosClient(String region) {
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 为防止请求头部被篡改导致的数据无法解密，强烈建议只使用 https 协议发起请求
        clientConfig.setHttpProtocol(HttpProtocol.https);

        KeyPair asymKeyPair = null;

        try {
            // 加载保存在文件中的秘钥, 如果不存在，请先使用buildAndSaveAsymKeyPair生成秘钥
            // buildAndSaveAsymKeyPair();
            asymKeyPair = loadAsymKeyPair();
        } catch (Exception e) {
            throw new CosClientException(e);
        }

        // 初始化 KMS 加密材料
        EncryptionMaterials encryptionMaterials = new EncryptionMaterials(asymKeyPair);
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

    private static void buildAndSaveAsymKeyPair() throws IOException, NoSuchAlgorithmException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(1024, srand);
        KeyPair keyPair = keyGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        FileOutputStream fos = new FileOutputStream(pubKeyPath);
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();

        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        fos = new FileOutputStream(priKeyPath);
        fos.write(pkcs8EncodedKeySpec.getEncoded());
        fos.close();
    }

    private static KeyPair loadAsymKeyPair()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // load public
        File filePublicKey = new File(pubKeyPath);
        FileInputStream fis = new FileInputStream(filePublicKey);
        byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
        fis.read(encodedPublicKey);
        fis.close();

        // load private
        File filePrivateKey = new File(priKeyPath);
        fis = new FileInputStream(filePrivateKey);
        byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
        fis.read(encodedPrivateKey);
        fis.close();

        // build RSA KeyPair
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }
    
    static void getObjectDemo() {
        // 下载文件
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        File downloadFile = new File("downAsy.txt");
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
