package com.proxio.backend.models.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {

        http
                .csrf(csrf -> csrf

                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/signup", "/css/**", "/images/**", "/uploads/**", "/error/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/weekly-offers/**", "/api/locations/**").authenticated()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("USER", "CUSTOMER", "VENDOR", "ADMIN")
                        .requestMatchers("/ui/**").hasRole("ADMIN")
                        .requestMatchers("/market/**", "/offers/**", "/orders/**").hasAnyRole("USER", "CUSTOMER", "VENDOR", "ADMIN")
                        .requestMatchers("/subscriptions/**").hasAnyRole("USER", "CUSTOMER", "ADMIN")
                        .requestMatchers("/vendors/*/subscribe", "/vendors/*/unsubscribe").hasAnyRole("USER", "CUSTOMER", "ADMIN")
                        .requestMatchers("/vendor/**").hasAnyRole("VENDOR", "ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())

                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean admin = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
                            boolean vendor = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_VENDOR"));
                            if (admin) {
                                response.sendRedirect("/ui/users");
                            } else if (vendor) {
                                response.sendRedirect("/vendor");
                            } else {
                                response.sendRedirect("/market");
                            }
                        })
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-key")
                        .permitAll()
                )
//
                .rememberMe(remember -> remember
                        .key("remember-key")
                        .tokenValiditySeconds(7 * 86400)
                );

        return http.build();
    }

}
