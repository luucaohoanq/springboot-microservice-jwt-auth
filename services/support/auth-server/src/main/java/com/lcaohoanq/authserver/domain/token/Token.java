
package com.lcaohoanq.authserver.domain.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "tokens")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({
  "id",
  "token",
  "refreshToken",
  "tokenType",
  "expirationDate",
  "refreshExpirationDate",
  "isMobile",
  "revoked",
  "expired",
  "accountId"
})
public class Token {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", unique = true, nullable = false)
  @JsonProperty("id")
  private Long id;

  private String token;
  private String refreshToken;
  private String tokenType;
  private LocalDateTime expirationDate;
  private LocalDateTime refreshExpirationDate;
  private boolean isMobile;
  private boolean revoked;
  private boolean expired;

  private Long accountId;
}
