package br.softsistem.Gerenciamento_de_estoque.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8))
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
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        claims.put("org_id", orgId);
        claims.put("roles", roles);
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
