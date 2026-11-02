package gmbh.tantive.jsonplaceholder_crud.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CassandraUserDetailsService implements UserDetailsService {

    private final AuthAccountStore accountStore;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AuthAccount account = accountStore.findByEmail(AuthEmail.normalize(email))
                .filter(candidate -> candidate.isEnabled())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .authorities(account.getRole())
                .disabled(!account.isEnabled())
                .build();
    }
}