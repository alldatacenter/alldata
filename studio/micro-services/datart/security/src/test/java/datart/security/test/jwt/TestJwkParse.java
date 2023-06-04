package datart.security.test.jwt;

import datart.core.base.exception.Exceptions;
import datart.security.util.JwkUtils;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TestJwkParse {

    private final static String PEM_PATH = JwkSetCreator.PEM_PATH;

    private final static String JWK_PATH = JwkSetCreator.JWK_PATH;

    @Test
    public void testHMACJwk() {
        String tokenSecret = "d@a$t%a^r&a*t";
        String token = Jwts.builder().signWith(SignatureAlgorithm.HS256, tokenSecret.getBytes())
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .claim(Claims.SUBJECT, "demo").compact();

        Jwt jwt = JwkUtils.parseJwt(token, JWK_PATH + "oct.json");
        Claims claims = (Claims) jwt.getBody();
        assert "demo".equals(claims.getSubject());
    }

    @Test
    public void testRsaJwk() {
        String privateFile = PEM_PATH + "rsa_private_key.pem";
        String token = createTokenFromPem(SignatureAlgorithm.RS256, privateFile, "demo");

        Jwt jwt = JwkUtils.parseJwt(token, JWK_PATH + "rsa.json");
        Claims claims = (Claims) jwt.getBody();
        assert "demo".equals(claims.getSubject());
    }

    @Test
    public void testEcJwk() {
        String privateFile = PEM_PATH + "ec_private_key.pem";
        String token = createTokenFromPem(SignatureAlgorithm.ES256, privateFile, "demo");

        Jwt jwt = JwkUtils.parseJwt(token, JWK_PATH + "ec.json");
        Claims claims = (Claims) jwt.getBody();
        assert "demo".equals(claims.getSubject());
    }

    @Test
    public void testRsaPemJwt() {
        List<String> tokens = new ArrayList<>();
        String privateFile = PEM_PATH + "rsa_private_key.pem";
        tokens.add(createTokenFromPem(SignatureAlgorithm.RS256, privateFile, "demo"));
        String privateFile_pkcs1 = PEM_PATH + "rsa_private_key_pkcs1.pem";
        tokens.add(createTokenFromPem(SignatureAlgorithm.RS256, privateFile_pkcs1, "demo"));
        for (String token : tokens) {
            String keyFile = PEM_PATH + "rsa_public_key.pem";
            validTokenWithPem(keyFile, token, "demo");
            keyFile = PEM_PATH + "rsa_public_key_pkcs1.pem";
            validTokenWithPem(keyFile, token, "demo");
        }
    }

    @Test
    public void testEcPemJwt() {
        List<String> tokens = new ArrayList<>();
        String privateFile = PEM_PATH + "ec_private_key.pem";
        tokens.add(createTokenFromPem(SignatureAlgorithm.ES256, privateFile, "demo"));
        String privateFile_pkcs1 = PEM_PATH + "ec_private_key_pkcs1.pem";
        tokens.add(createTokenFromPem(SignatureAlgorithm.ES256, privateFile_pkcs1, "demo"));

        for (String token : tokens) {
            //String keyFile = PEM_PATH + "ec_public_key.pem";
            String keyFile = PEM_PATH + "ec_private_key.pem";
            validTokenWithPem(keyFile, token, "demo");
        }
    }

    private static String createTokenFromPem(SignatureAlgorithm algorithm, String pemFile, String subject) {
        Key privateKey = JwkUtils.getPrivateKeyFromPem(pemFile);
        JwtBuilder jwtBuilder = Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .signWith(algorithm, privateKey);
        return jwtBuilder.claim(Claims.SUBJECT, subject).compact();
    }

    private static void validTokenWithPem(String pemFile, String token, String subject) {
        try {
            Key publicKey = JwkUtils.getPublicKeyFromPem(pemFile);
            Claims claims = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token).getBody();
            if (!Objects.equals(claims.get(Claims.SUBJECT), subject)) {
                Exceptions.msg("jwt claim subject valid failed.");
            }
            return ;
        } catch (Exception e) {
            Exceptions.msg("jwt valid failed: "+e.getMessage());
            return ;
        }
    }


}
