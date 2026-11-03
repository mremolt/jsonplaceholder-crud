package gmbh.tantive.jsonplaceholder_crud.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    static final String AUDIENCE = "jsonplaceholder-api";

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtTokenService(JwtEncoder jwtEncoder, @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
    }

    public AccessToken issue(AuthAccount account) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(account.getAccountId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .audience(List.of(AUDIENCE))
                .claim("email", account.getEmail())
                .claim("roles", List.of(account.getRole()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, accessTokenTtl.toSeconds());
    }
}