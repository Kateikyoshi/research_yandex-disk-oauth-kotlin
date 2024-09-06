package jp.warau.bakari.ya;

import org.slf4j.Logger;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeReactiveAuthenticationManager;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginReactiveAuthenticationManager;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static jp.warau.bakari.LoggerInitializer.initLogger;

@SuppressWarnings("unused")
@RestController
public class Endpoint {
    private final Logger logger = initLogger(this);

    /**
     * Sure, your redirect-uri looks exactly like this in config.
     * It is set like this in `https://oauth.yandex.ru/` too for your client
     * app called 'cloudChicken'. You thought you were being smart
     * by setting this one to catch a redirect and thus get the code?
     * But it doesn't work this way, apparently.
     * All you actually need to do is to go to `http://localhost:8082/login` when your service runs.
     * You will see your providers (google, yandex) clickable.
     * Click will call yandex and yandex will redirect back, but that call to yandex happens
     * through webClient meaning code exchange and event token exchange happen automatically.
     * When webClient got your token and visited user endpoint, you get redirected to 'index'.
     * We don't have index running here (to hell with thymeleaf?), so it will be a 404 for you.
     *
     * Here is the list of classes which are used in that hellish sequence:
     * @see OAuth2LoginReactiveAuthenticationManager
     * @see OAuth2AuthorizationCodeReactiveAuthenticationManager
     * @see WebClientReactiveAuthorizationCodeTokenResponseClient
     */
    @GetMapping("/login/oauth2/code/yandex")
    public Mono<Void> getUser(@RequestParam String code) {
        logger.info("We got redirected here! {}", code);

        return Mono.empty();
    }

    /**
     *
     * @param authorizedClient it is the easiest way to get an authorized client in a ServerExchange loop.
     *                         Another way is to inject manager or repository, create an authorization request
     *                         and use it to ask for an authorized client.
     */
    @GetMapping("/accessDisk")
    @ResponseBody //this is to treat it like plain/text and not try rendering some template
    public Mono<Void> accessDisk(@RegisteredOAuth2AuthorizedClient("yandex") OAuth2AuthorizedClient authorizedClient) {
        logger.info("accessDisk call");

        String token = authorizedClient.getAccessToken().getTokenValue();
        logger.info("token value: {}", token);

        return Mono.empty();
    }
}
