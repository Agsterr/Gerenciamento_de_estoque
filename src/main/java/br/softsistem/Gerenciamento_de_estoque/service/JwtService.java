package br.softsistem.Gerenciamento_de_estoque.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Serviço para criação e validação de tokens JWT, agora com userId, orgId e roles.
 */
@Service
public class JwtService {

    // Chave secreta para assinatura (em produção, use local seguro!)
    private final String SECRET_KEY = "tFbZV3jdy2ktlZG7tr03eyO6jYd5g2uZ39do8Hgchws=";

    /**
     * Extrai o username (subject) do token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrai o orgId (claim "org_id") do token.
     */
    public Long extractOrgId(String token) {
        return extractClaim(token, claims -> {
            Object raw = claims.get("org_id");
            if (raw instanceof Integer) {
                return ((Integer) raw).longValue();
            } else if (raw instanceof Long) {
                return (Long) raw;
            }
            return null;
        });
    }

    /**
     * Extrai o userId (claim "user_id") do token.
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object raw = claims.get("user_id");
            if (raw instanceof Integer) {
                return ((Integer) raw).longValue();
            } else if (raw instanceof Long) {
                return (Long) raw;
            } else if (raw instanceof String) {
                try {
                    return Long.valueOf((String) raw);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
    }

    /**
     * Extrai as roles (claim "roles") do token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    /**
     * Extrai qualquer claim usando um resolver.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Decodifica e retorna todos os claims do JWT.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Gera um token JWT incluindo userId e orgId (sem roles).
     */
    public String generateToken(UserDetails userDetails, Long userId, Long orgId) {
        return generateToken(userDetails, userId, orgId, List.of());
    }

    /**
     * Gera um token JWT incluindo userId, orgId e roles.
     */
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

    /**
     * Monta efetivamente o JWT.
     */
    private String createToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

    /**
     * Valida o token comparando username e data de expiração.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Verifica se o token expirou.
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
