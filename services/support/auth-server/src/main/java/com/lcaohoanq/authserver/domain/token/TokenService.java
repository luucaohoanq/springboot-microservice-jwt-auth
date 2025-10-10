
package com.lcaohoanq.authserver.domain.token;


public interface TokenService {

  Token addToken(Long userId, String token, boolean isMobileDevice);

  Token refreshToken(String refreshToken, Long userId) throws Exception;

  void deleteToken(String token, Long userid);

  Token findAccountByToken(String token);
}
