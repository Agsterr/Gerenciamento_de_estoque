package br.softsistem.Gerenciamento_de_estoque.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Serviço para criação e validação de tokens JWT, agora com userId e orgId.
 */
@Service
public class JwtService {

    // Sua chave secreta para assinatura. Em produção, armazene isso de forma segura!
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
     * Extrai qualquer claim específico usando um resolver.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Retorna todos os claims do JWT (decode + validação de assinatura).
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Gera um token JWT incluindo username, userId e orgId.
     *
     * @param userDetails detalhes do usuário (username e authorities)
     * @param userId      ID numérico do usuário a ser armazenado no claim "user_id"
     * @param orgId       ID da organização a ser armazenado no claim "org_id"
     * @return token JWT assinado em HS256
     */
    public String generateToken(UserDetails userDetails, Long userId, Long orgId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        claims.put("org_id", orgId);
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Constrói efetivamente o JWT com claims extras + subject.
     */
    private String createToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .setClaims(extraClaims)            // contém user_id e org_id
                .setSubject(subject)               // username
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // 10 horas de validade, ajuste conforme necessidade
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

    /**
     * Verifica se o token é válido comparando o username e se não expirou.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checa se o token já passou da data de expiração.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrai a data de expiração do token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
