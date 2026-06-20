package br.softsistem.Gerenciamento_de_estoque.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Agora a chave é injetada pelo Spring
    private final String SECRET_KEY;

    public JwtService(@Value("${jwt.secret}") String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT_SECRET não configurado. Defina a variável de ambiente JWT_SECRET ou a propriedade jwt.secret.");
        }
        this.SECRET_KEY = secretKey;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractOrgId(String token) {
        return extractClaim(token, claims -> {
            Object raw = claims.get("org_id");
            if (raw instanceof Integer) return ((Integer) raw).longValue();
            if (raw instanceof Long) return (Long) raw;
            return null;
        });
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object raw = claims.get("user_id");
            if (raw instanceof Integer) return ((Integer) raw).longValue();
            if (raw instanceof Long) return (Long) raw;
            if (raw instanceof String) {
                try {
                    return Long.valueOf((String) raw);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        Key key = getSigningKey();
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateToken(UserDetails userDetails, Long userId, Long orgId) {
        return generateToken(userDetails, userId, orgId, List.of());
    }

    public String generateToken(UserDetails userDetails,
                                Long userId,
                                Long orgId,
                                List<String> roles) {
        return generateToken(userDetails, userId, orgId, roles, false);
    }

    public String generateToken(UserDetails userDetails,
                                Long userId,
                                Long orgId,
                                List<String> roles,
                                boolean bypassSubscription) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        claims.put("org_id", orgId);
        claims.put("roles", roles);
        claims.put("bypass_subscription", bypassSubscription);
        return createToken(claims, userDetails.getUsername());
    }

    public boolean extractBypassSubscription(String token) {
        return Boolean.TRUE.equals(extractClaim(token, claims -> {
            Object raw = claims.get("bypass_subscription");
            if (raw instanceof Boolean b) {
                return b;
            }
            return false;
        }));
    }

    private String createToken(Map<String, Object> extraClaims, String subject) {
        Key key = getSigningKey();
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }
}
