package com.example.crud.config;

import com.example.crud.repositories.IUsersRepository;
import com.example.crud.services.impl.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

//    private final DataSource dataSource;
//    @Autowired
//    public WebSecurityConfig(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }
//
//    @Autowired
//    public void configAuthentication(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
//        authenticationManagerBuilder.jdbcAuthentication()
//                .dataSource(dataSource)
//                .passwordEncoder(new BCryptPasswordEncoder())
//                .usersByUsernameQuery("select username, password, enabled from users where username=?")
//                .authoritiesByUsernameQuery("select username, role from users where username=?");
//    }


//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
//    }
//    @Bean
//    protected InMemoryUserDetailsManager userDetailsService(){
//        UserDetails userDetails = User
//                .withUsername("luka")
//                .password("{noop}password")
//                .roles("USER")
//                .build();
//        UserDetails userDetails2 = User
//                .withUsername("admin")
//                .password("{noop}password")
//                .roles("ADMIN")
//                .build();
//        return new InMemoryUserDetailsManager(userDetails,userDetails2);
//    }
    private final IUsersRepository usersRepository;
    private final DataSource dataSource;

    public WebSecurityConfig(IUsersRepository usersRepository, DataSource dataSource) {
        this.usersRepository = usersRepository;
        this.dataSource = dataSource;
    }

    @Bean
    public UserDetailsService userDetailsService(){
        return new UserDetailsServiceImpl(usersRepository);
    }
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean
    public DaoAuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());

        return authenticationProvider;
    }
    @Bean
    RememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
        TokenBasedRememberMeServices.RememberMeTokenAlgorithm encodingAlgorithm = TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256;
        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices("remember-me", userDetailsService, encodingAlgorithm);
        rememberMe.setMatchingAlgorithm(TokenBasedRememberMeServices.RememberMeTokenAlgorithm.MD5);
        return rememberMe;
    }
    @Bean
    protected SecurityFilterChain configure(HttpSecurity http,AuthenticationManagerBuilder authenticationManagerBuilder
            ,RememberMeServices rememberMeServices)throws Exception{
        authenticationManagerBuilder.authenticationProvider(authenticationProvider());
        http
                .authorizeHttpRequests(form->form
                        .requestMatchers(new AntPathRequestMatcher("/page/*"),
                                new AntPathRequestMatcher("/product/exportExcel"),
                                new AntPathRequestMatcher("/product/exportPDF"),
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/addToCart/add/*"),
                                new AntPathRequestMatcher("/product/exportCSV")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/edit/*")).hasAnyRole("ADMIN","EDITOR")
                        .requestMatchers(new AntPathRequestMatcher("/delete/*")).hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll())
                .logout(form -> form
                    .logoutUrl("/logout")
                        .deleteCookies("remember-me")
                    .permitAll())
                .rememberMe((remember) -> remember
                        .rememberMeServices(rememberMeServices)
                        .tokenValiditySeconds(3*24*60*60)
                        .tokenRepository(persistentTokenRepository())
                        .rememberMeParameter("remember-me"))
                .exceptionHandling(e->e
                        .accessDeniedPage("/403"));
        return http.build();
    }
    @Bean
    public PersistentTokenRepository persistentTokenRepository(){
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return null;
    }
}
