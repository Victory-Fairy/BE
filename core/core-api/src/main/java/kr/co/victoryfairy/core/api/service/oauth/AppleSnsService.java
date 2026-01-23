package kr.co.victoryfairy.core.api.service.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.dodn.springboot.core.enums.MemberEnum;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.model.AppleResponseWrapper;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.security.PrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service("APPLE")
@Slf4j
public class AppleSnsService implements OauthService {

    @Value("${auth.apple.cas.client_id}")
    private String appleClientId;

    @Value("${auth.apple.cas.client_secret}")
    private String appleClientSecret;

    @Value("${auth.apple.cas.team_id}")
    private String appleTeamId;

    @Value("${auth.apple.cas.key_id}")
    private String appleKeyId;

    @Value("${auth.apple.cas.callback_url}")
    private String appleCallbackUrl;

    @Value("${auth.apple.cas.secret_path}")
    private String appleSecretPath;

    private final static String APPLE_AUTH_URL = "https://appleid.apple.com";

    @Override
    public String initSnsAuthPath(String redirectUrl) {
        return UriComponentsBuilder.fromUriString("https://appleid.apple.com/auth/authorize")
            .queryParam("client_id", appleClientId)
            .queryParam("redirect_uri", StringUtils.hasText(redirectUrl) ? redirectUrl : appleCallbackUrl)
            .queryParam("response_type", "code id_token")
            .queryParam("scope", "name email")
            .queryParam("response_mode", "form_post")
            .build()
            .toUriString();
    }

    @Override
    public MemberDomain.MemberSns parseSnsInfo(MemberDomain.MemberLoginRequest request) {
        log.info("================appleCallback=============");
        log.info("code: {}", request.code());

        JsonObject json = null;
        var idToken = "";
        var url = "https://appleid.apple.com/auth/token";

        Map<String, String> param = new HashMap<>();
        param.put("grant_type", "authorization_code");
        param.put("code", request.code());
        param.put("redirect_uri", appleCallbackUrl);
        param.put("client_id", appleClientId);
        // jwt secret
        String client_secret = "";
        try {
            client_secret = this.getClientSecret();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(MessageEnum.Auth.FAIL_SNS);
        }

        param.put("client_secret", client_secret);

        log.warn("url:*******" + url + "*******");
        ObjectMapper mapper = new ObjectMapper();
        AppleResponseWrapper appleResponseWrapper = null;
        String response = HttpClientUtils.doPost(url, param);
        try {
            appleResponseWrapper = mapper.readValue(response, AppleResponseWrapper.class);
            log.info("appleResponseWrapper :  {}", appleResponseWrapper);
            idToken = appleResponseWrapper.getIdToken();
            json = this.parserIdentityToken(idToken);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(MessageEnum.Auth.FAIL_SNS);
        }

        return new MemberDomain.MemberSns(MemberEnum.SnsType.APPLE, getValue(json.get("sub")),
                getValue(json.get("email")));
    }

    public String getClientSecret() throws Exception {

        JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.setHeaderParam("kid", appleKeyId);
        jwtBuilder.setIssuer(appleTeamId);
        jwtBuilder.setIssuedAt(new Date());
        jwtBuilder.setExpiration(DateUtils.addMinutes(new Date(), 30));
        jwtBuilder.setAudience("https://appleid.apple.com");
        jwtBuilder.setSubject(appleClientId);

        PrivateKey privateKey = getPrivateKey();

        jwtBuilder.signWith(SignatureAlgorithm.ES256, privateKey);
        String secretCode = jwtBuilder.compact();

        return secretCode;
    }

    public PrivateKey getPrivateKey() throws Exception {
        // appleKeyId에 담겨있는 정보 가져오기
        Resource resources = ResourcePatternUtils.getResourcePatternResolver(new DefaultResourceLoader())
            .getResource("classpath:" + appleSecretPath);

        InputStream inputStream = resources.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String readLine = null;
        StringBuilder stringBuilder = new StringBuilder();
        while ((readLine = bufferedReader.readLine()) != null) {
            stringBuilder.append(readLine);
            stringBuilder.append("\n");
        }
        String keyPath = stringBuilder.toString();

        // privateKey 생성하기
        Reader reader = new StringReader(keyPath);
        PEMParser pemParser = new PEMParser(reader);
        JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter();
        PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) pemParser.readObject();
        PrivateKey privateKey = jcaPEMKeyConverter.getPrivateKey(privateKeyInfo);

        return privateKey;
    }

    public static JsonObject parserIdentityToken(String identityToken) {
        String[] arr = identityToken.split("\\.");
        Base64 base64 = new Base64();
        String decode = new String(base64.decodeBase64(arr[1]));
        String substring = decode.substring(0, decode.indexOf("}") + 1);
        JsonObject jsonObject = JsonParser.parseString(substring).getAsJsonObject();
        // JsonObject jsonObject = Json.parseObject(substring);
        return jsonObject;
    }

    private String getValue(Object obj) {
        if (obj != null) {
            return obj.toString().replaceAll("\"", "");
        }
        else {
            return null;
        }
    }

}
