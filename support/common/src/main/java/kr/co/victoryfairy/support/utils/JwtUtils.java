package kr.co.victoryfairy.support.utils;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class JwtUtils {

    public static Claims parseToken(String token, String secretKey) {
        Claims claims = Jwts.claims();
        try {

            claims = Jwts.parserBuilder()
                .setSigningKey(DatatypeConverter.parseBase64Binary(secretKey))
                // .requireAudience(getIp(request))
                .build()
                .parseClaimsJws(token)
                .getBody();
            claims.put("isCertifiedToken", Boolean.TRUE);
            claims.put("isExpired", Boolean.FALSE);
        }
        catch (ExpiredJwtException ex) {
            log.debug("The token is expired", ex);
            claims.put("isCertifiedToken", Boolean.TRUE);
            claims.put("isExpired", Boolean.TRUE);
        }
        catch (JwtException ex) {
            log.error("JwtException occurs when parseToken", ex);
            claims.put("isCertifiedToken", Boolean.FALSE);
        }
        catch (IllegalArgumentException ex) {
            log.error("IllegalArgumentException occurs when parseToken", ex);
            claims.put("isCertifiedToken", Boolean.FALSE);
        }
        return claims;
    }

    public static String generateToken(Map<String, Object> claims, int expireMinutes, String secretKey) {
        String token = null;
        try {
            JwtBuilder jwtBuilder = Jwts.builder();

            if (claims != null) {
                jwtBuilder.setClaims(claims);
            }

            jwtBuilder.setId(getJwtId());
            // jwtBuilder.setAudience(getIp(request));

            if (expireMinutes > 0) {
                Date expireDate = DateUtils.addMinutes(new Date(), expireMinutes);
                jwtBuilder.setExpiration(expireDate);
            }

            byte[] secreKeyBytes = DatatypeConverter.parseBase64Binary(secretKey);
            jwtBuilder.signWith(new SecretKeySpec(secreKeyBytes, SignatureAlgorithm.HS256.getJcaName()),
                    SignatureAlgorithm.HS256);
            token = jwtBuilder.compact();
        }
        catch (JwtException ex) {
            log.error("JwtException occurs when generateToken", ex);
        }
        catch (IllegalArgumentException ex) {
            log.error("IllegalArgumentException occurs when generateToken", ex);
        }
        return token;
    }

    private static String getJwtId() {
        // create unique jwtId not do
        return UUID.randomUUID().toString();
    }

}
