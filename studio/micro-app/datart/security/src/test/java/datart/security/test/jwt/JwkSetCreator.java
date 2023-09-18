package datart.security.test.jwt;

import com.nimbusds.jose.jwk.Curve;
import datart.core.common.Application;
import datart.core.common.ReflectUtils;
import datart.security.util.JwkUtils;
import org.jose4j.jwk.*;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.keys.AesKey;
import org.json.JSONArray;
import org.json.JSONObject;

import java.security.PrivateKey;
import java.security.interfaces.ECPublicKey;

public class JwkSetCreator {

    public final static String PEM_PATH = Application.userDir()+"/src/test/resources/jwt/pem/";

    public final static String JWK_PATH = Application.userDir()+"/src/test/resources/jwt/jwk/";

    public static JSONObject createRsaJwkSet() {
        JSONObject jsonObject = new JSONObject();
        try {
            RsaJsonWebKey jwk = RsaJwkGenerator.generateJwk(2048);
            jwk.setUse(Use.SIGNATURE);
            jwk.setKeyId("datart_ec");
            jwk.setAlgorithm(AlgorithmIdentifiers.RSA_USING_SHA256);
            String privateFile = PEM_PATH + "rsa_private_key.pem";
            PrivateKey privateKey = (PrivateKey) JwkUtils.getPrivateKeyFromPem(privateFile);
            jwk.setPrivateKey(privateKey);

            JSONObject ele = new JSONObject(jwk.toParams(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(ele);
            jsonObject.put("keys", jsonArray);
        }catch (Exception e) {}
        System.out.println(jsonObject);
        return jsonObject;
    }

    public static JSONObject createEcJwkSet() {
        JSONObject jsonObject = new JSONObject();
        try {
            String privateFile = PEM_PATH + "ec_private_key.pem";
            ECPublicKey publicKey = (ECPublicKey) JwkUtils.getPublicKeyFromPem(privateFile);
            EllipticCurveJsonWebKey jwk = new EllipticCurveJsonWebKey(publicKey);
            ReflectUtils.setFiledValue(jwk, "curveName", Curve.SECP256K1.getName());
            jwk.setUse(Use.SIGNATURE);
            jwk.setKeyId("datart_ec");
            jwk.setAlgorithm(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);

            PrivateKey privateKey = (PrivateKey) JwkUtils.getPrivateKeyFromPem(privateFile);
            jwk.setPrivateKey(privateKey);

            JSONObject ele = new JSONObject(jwk.toParams(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(ele);
            jsonObject.put("keys", jsonArray);
        }catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(jsonObject);
        return jsonObject;
    }

    public static JSONObject createHMACJwkSet() {
        JSONObject jsonObject = new JSONObject();
        String tokenSecret = "d@a$t%a^r&a*t";
        try {
            AesKey aesKey = new AesKey(tokenSecret.getBytes());
            OctetSequenceJsonWebKey jwk = new OctetSequenceJsonWebKey(aesKey);
            jwk.setUse(Use.SIGNATURE);
            jwk.setKeyId("datart_oct");
            jwk.setAlgorithm(AlgorithmIdentifiers.HMAC_SHA256);

            JSONObject ele = new JSONObject(jwk.toParams(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(ele);
            jsonObject.put("keys", jsonArray);
        }catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(jsonObject);
        return jsonObject;
    }
}
