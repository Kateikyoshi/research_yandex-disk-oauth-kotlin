package jp.warau.bakari.ya;

import io.netty.handler.logging.LogLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Service;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
//@EnableWebFluxSecurity
public class OAuth2ClientSecurityConfig {

    @Bean
    public ReactiveOAuth2AuthorizedClientService r2dbcReactiveClientRegistrationRepository(
            DatabaseClient databaseClient,
            ReactiveClientRegistrationRepository clientRegistrationRepository
    ) {
        return new R2dbcReactiveOAuth2AuthorizedClientService(databaseClient, clientRegistrationRepository);
    }

    /**
     *
     * We really need this bean.
     * AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager is crucial when you want to be off
     * the ServerExchange loop. Putting it simply, if you want to inject automatic authorization in your WebClient
     * and call it not from the endpoint (@GetMapping, @Controller...) but from some @Scheduled loop which has nothing to
     * do with an HTTP request to your endpoint from a client/user, then you need this one.
     */
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService
    ) {

        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .clientCredentials()
                        .build();

        //Critical when you need OAuth2Login Authorization outside ServerExchange (endpoints)
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService
                );
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Service
    @RequiredArgsConstructor
    public class CustomOAuth2UserService extends DefaultOAuth2UserService {

        //private final UserService userService;

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            return oAuth2User;
            //String clientRegId = userRequest.getClientRegistration().getRegistrationId();
            //AuthProvider provider = AuthProvider.fingByName(clientRegId);
            //return userService.saveAndMap(oAuth2User, provider);
        }
    }

    /**
     * Demonstrates what how SecurityWebFilterChain @Bean looks like if you
     * enable @EnableWebFluxSecurity to disable spring boot automatic configuration.
     * Pay attention, to use oauth2Client you need oauth2Login if authorization_code grant is used.
     */
    //@Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.authorizeExchange(ex -> ex.anyExchange().permitAll())
                .oauth2Client(Customizer.withDefaults())
                .oauth2Login(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Yandex file download has some redirects (HTTP 302). We need to follow them.
     */
    @Bean("redirectingConnector")
    public ClientHttpConnector redirectingClientHttpConnector() {
        HttpClient httpClient = HttpClient.create()
                .wiretap(this.getClass().getCanonicalName(), LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
        return new ReactorClientHttpConnector(httpClient.followRedirect(true));
    }
}