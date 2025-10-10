
package com.lcaohoanq.authserver.components;

import com.lcaohoanq.authserver.domain.token.Token;
import com.lcaohoanq.authserver.domain.token.TokenRepository;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import com.lcaohoanq.commonlibrary.exceptions.ExpiredTokenException;
import com.lcaohoanq.commonlibrary.exceptions.InvalidParamException;
import com.lcaohoanq.commonlibrary.exceptions.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenUtils {

  private final TokenRepository tokenRepository;

  @Value("${jwt.expiration}")
  private int expiration; // save to an environment variable

  @Value("${jwt.expiration-refresh-token}")
  private int expirationRefreshToken;

  @Value("${jwt.secretKey}")
  private String secretKey;

  public String generateToken(UserResponse user) {
    // properties => claims
    Map<String, Object> claims = new HashMap<>();
    claims.put("email", user.email());
    claims.put("userId", user.id());
    claims.put("username", user.username());
    claims.put("role", user.role());
    try {
        // how to extract claims from this ?
        return Jwts.builder()
            .setClaims(claims) // how to extract claims from this ?
            .setSubject(user.username())
            .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000L))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
    } catch (Exception e) {
      // you can "inject" Logger, instead System.out.println
      throw new InvalidParamException("Cannot create jwt token, error: " + e.getMessage());
      // return null;
    }
  }

  public String generateRefreshToken(UserResponse user) {
    // properties => claims for refresh token
    Map<String, Object> claims = new HashMap<>();
    claims.put("email", user.email());
    claims.put("userId", user.id());
    claims.put("username", user.username());
    claims.put("role", user.role());
    claims.put("tokenType", "refresh");
    try {
        return Jwts.builder()
            .setClaims(claims) 
            .setSubject(user.username())
            .setExpiration(new Date(System.currentTimeMillis() + expirationRefreshToken * 1000L))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
    } catch (Exception e) {
      throw new com.lcaohoanq.commonlibrary.exceptions.InvalidParamException("Cannot create refresh token, error: " + e.getMessage());
    }
  }

  public Long extractUserId(String token) {
    return extractClaim(token, claims -> claims.get("userId", Long.class));
  }

  public String extractUsername(String token) {
    return extractClaim(token, claims -> claims.get("username", String.class));
  }

  public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
  }

  private Key getSignInKey() {
    byte[] bytes = Decoders.BASE64.decode(secretKey);
    // Keys.hmacShaKeyFor(Decoders.BASE64.decode("TaqlmGv1iEDMRiFp/pHuID1+T84IABfuA0xXh4GhiUI="));
    return Keys.hmacShaKeyFor(bytes);
  }

  private String generateSecretKey() {
    SecureRandom random = new SecureRandom();
    byte[] keyBytes = new byte[32]; // 256-bit key
    random.nextBytes(keyBytes);
      return Encoders.BASE64.encode(keyBytes);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSignInKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = this.extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  // check expiration
  public boolean isTokenExpired(String token) {
    Date expirationDate = this.extractClaim(token, Claims::getExpiration);
    return expirationDate.before(new Date());
  }

  public String extractEmail(String token) {
    return extractClaim(token, claims -> claims.get("email", String.class));
  }

  public String extractSubject(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public String extractBearerToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      throw new JwtAuthenticationException("Authorization header is missing or invalid");
    }
    return header.substring(7); // Remove "Bearer " prefix
  }

  public boolean validateToken(String token) {
    try {
      // Check expiration
      if (isTokenExpired(token)) {
        throw new ExpiredTokenException("Token has expired");
      }

      // Validate token structure
      extractAllClaims(token);
      return true;
    } catch (ExpiredTokenException e) {
      throw new JwtAuthenticationException("JWT token has expired");
    } catch (MalformedJwtException e) {
      throw new JwtAuthenticationException("Invalid JWT token format");
    } catch (UnsupportedJwtException e) {
      throw new JwtAuthenticationException("Unsupported JWT token");
    } catch (IllegalArgumentException e) {
      throw new JwtAuthenticationException("JWT claims string is empty");
    }
  }

  public boolean validateTokenWithUser(String token, Long userId) {
    try {
      Token existingToken =
          tokenRepository
              .findByToken(token)
              .orElse(null);

      // Check token existence and revocation (optional - for token blacklisting)
      if (existingToken != null && existingToken.isRevoked()) {
        throw new JwtAuthenticationException("Token is invalid or has been revoked");
      }

      Long tokenUserId = extractUserId(token);
      
      // Check token matches user
      if (!userId.equals(tokenUserId)) {
        throw new JwtAuthenticationException("Token does not match user");
      }

      return validateToken(token);
    } catch (Exception e) {
      throw new JwtAuthenticationException("Token validation failed: " + e.getMessage());
    }
  }
}
