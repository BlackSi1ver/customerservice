package com.customerservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecretKey;

    private final long EXPIRATION_TIMEOUT_MS = 1000 * 60 * 60 * 2; // 2 hours

    private final ConcurrentHashMap<String, Integer> aliveTokensMap = new ConcurrentHashMap<>();

    private final Set<String> aliveTokensSet = aliveTokensMap.keySet(0);

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(jwtSecretKey).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        Set<String> roles = userDetails.getAuthorities()
                .stream()
                .map(c -> c.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
        claims.put("roles", roles);
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        final Date expirationDate = new Date(System.currentTimeMillis() + EXPIRATION_TIMEOUT_MS);

        final String jwtToken = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, jwtSecretKey).compact();

        aliveTokensSet.add(jwtToken);

        return jwtToken;
    }

    public Boolean validateToken(String token, UserDetails userDetails) {

        if (!aliveTokensSet.contains(token)) {
            return false;
        }

        if (isTokenExpired(token)) {
            aliveTokensSet.remove(token);
            return false;
        }

        final String username = extractUsername(token);

        return username.equals(userDetails.getUsername());
    }

    public void revokeToken(String jwt) {
        aliveTokensSet.remove(jwt);
    }
}
