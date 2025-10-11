
package com.lcaohoanq.authserver.domain.token;

import com.lcaohoanq.authserver.components.JwtTokenUtils;
import com.lcaohoanq.authserver.feign.UserFeign;
import com.lcaohoanq.authserver.domain.auth.UserServiceClientImpl;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import com.lcaohoanq.commonlibrary.exceptions.ExpiredTokenException;
import com.lcaohoanq.commonlibrary.exceptions.TokenNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

  private static final int MAX_TOKENS = 3;
  private final UserServiceClientImpl userServiceClient;

  @Value("${jwt.expiration}")
  private int expiration; // save to an environment variable

  @Value("${jwt.expiration-refresh-token}")
  private int expirationRefreshToken;

  private final TokenRepository tokenRepository;
  private final JwtTokenUtils jwtTokenUtil;

  @Transactional
  @Override
  public Token refreshToken(String refreshToken, Long userid) throws Exception {
    Token existingToken =
        tokenRepository
            .findByRefreshToken(refreshToken)
            .orElseThrow(() -> new TokenNotFoundException("Refresh token does not exist"));

    if (existingToken.getRefreshExpirationDate().isBefore(LocalDateTime.now())) {
      tokenRepository.delete(existingToken);
      throw new ExpiredTokenException("Refresh token is expired");
    }
    String token = jwtTokenUtil.generateToken(userServiceClient.getUserById(userid));
    LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(expiration);
    existingToken.setExpirationDate(expirationDateTime);
    existingToken.setToken(token);
    existingToken.setRefreshToken(UUID.randomUUID().toString());
    existingToken.setRefreshExpirationDate(LocalDateTime.now().plusSeconds(expirationRefreshToken));
    return existingToken;
  }

  // do revoke token
  @Override
  public void deleteToken(String token, Long userid) {
    Token existingToken =
        tokenRepository
            .findByToken(token)
            .orElseThrow(() -> new TokenNotFoundException("Token does not exist"));

    if (existingToken.isRevoked()) {
      throw new TokenNotFoundException("Token has been revoked");
    }

    // check if token is attaching with user
    if (!Objects.equals(existingToken.getAccountId(), userid)) {
      throw new TokenNotFoundException("Token does not attach with current user");
    }
    existingToken.setRevoked(true);
    tokenRepository.save(existingToken);
  }

  @Override
  public Token findAccountByToken(String token) {
    return tokenRepository
        .findByToken(token)
        .orElseThrow(() -> new TokenNotFoundException("Token does not exist"));
  }

  @Transactional
  @Override
  public Token addToken(Long userId, String token, boolean isMobileDevice) {
    UserResponse existingUser = userServiceClient.getUserById(userId);
    List<Token> userTokens = tokenRepository.findByAccountId(existingUser.id());
    int tokenCount = userTokens.size();
    // Số lượng token vượt quá giới hạn, xóa một token cũ
    if (tokenCount >= MAX_TOKENS) {
      // kiểm tra xem trong danh sách userTokens có tồn tại ít nhất
      // một token không phải là thiết bị di động (non-mobile)
      boolean hasNonMobileToken = !userTokens.stream().allMatch(Token::isMobile);
      Token tokenToDelete;
      if (hasNonMobileToken) {
        tokenToDelete =
            userTokens.stream()
                .filter(userToken -> !userToken.isMobile())
                .findFirst()
                .orElse(userTokens.get(0));
      } else {
        // tất cả các token đều là thiết bị di động,
        // chúng ta sẽ xóa token đầu tiên trong danh sách
        tokenToDelete = userTokens.get(0);
      }
      tokenRepository.delete(tokenToDelete);
    }
    long expirationInSeconds = expiration;
    LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(expirationInSeconds);
    // Tạo mới một token cho người dùng
    Token newToken =
        Token.builder()
            .accountId(existingUser.id())
            .token(token)
            .revoked(false)
            .expired(false)
            .tokenType("Bearer")
            .expirationDate(expirationDateTime)
            .isMobile(isMobileDevice)
            .build();

    newToken.setRefreshToken(UUID.randomUUID().toString());
    newToken.setRefreshExpirationDate(LocalDateTime.now().plusSeconds(expirationRefreshToken));
    tokenRepository.save(newToken);
    return newToken;
  }
}
