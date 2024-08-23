package jp.warau.bakari.ya;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.warau.bakari.BakariException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Component
public class FileLoader {

    /**
     * Yes, we cheat the system knowing the principal which is an email.
     */
    @Value("${spring.security.oauth2.client.registration.yandex.principal}")
    private String userEmail;

    @Value("${file.where-to}")
    private String whereToSaveFile;

    private final String yandexDiskBaseUrl = "https://downloader.disk.yandex.ru/disk";

    private final String yandexDiskMetadataBaseUrl = "https://cloud-api.yandex.net/";

    private final String yandexDiskMetadataUrl = "v1/disk/resources/download";

    private final Logger logger = LoggerFactory.getLogger(FileLoader.class);

    private final WebClient client;

    private final WebClient noUrlEncodingClient;

    private final ObjectMapper mapper;

    private final String authType = AuthType.OAUTH.getType();

    private final String authorizationHeader = "Authorization";

    private final ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager;

    private OAuth2AuthorizedClient currentAuthorizedClient;

    public FileLoader(WebClient.Builder builder,
                      ObjectMapper mapper,
                      ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager,
                      @Qualifier("redirectingConnector") ClientHttpConnector clientHttpConnector
    ) {
        this.mapper = mapper;
        this.reactiveOAuth2AuthorizedClientManager = reactiveOAuth2AuthorizedClientManager;

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(reactiveOAuth2AuthorizedClientManager);

        this.client = builder.baseUrl(yandexDiskMetadataBaseUrl)
                .clientConnector(clientHttpConnector)
                .filter(oauth2Client)
                .filter((clientRequest, exchangeFunction) -> {
                    ClientRequest fixedRequest = yandexHeaderFixer(clientRequest);

                    return exchangeFunction.exchange(fixedRequest);
                })
                .build();

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(yandexDiskBaseUrl); //Here comes your base url
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        noUrlEncodingClient = client.mutate().uriBuilderFactory(factory).build();
    }

    private ClientRequest yandexHeaderFixer(ClientRequest clientRequest) {
        logger.info("Let's fix trash Yandex header");
        HttpHeaders headers = clientRequest.headers();

        List<String> authHeader = headers.get(authorizationHeader);
        if (authHeader == null || authHeader.isEmpty()) {
            logger.info("Auth header wasn't set, is spring doing anything...?");
            return clientRequest;
        }
        String fixedHeader = authHeader.getFirst().replace("Bearer", authType);

        ArrayList<String> newAuthHeaders = new ArrayList<>();
        newAuthHeaders.add(fixedHeader);

        return ClientRequest.from(clientRequest)
                .headers(hh -> hh.replace(authorizationHeader, newAuthHeaders))
                .build();
    }

    private String extractHref(String json) throws BakariException {
        try {
            JsonNode node = mapper.readTree(json);
            JsonNode hrefNode = node.get("href");
            if (hrefNode == null) {
                throw new BakariException("href wasn't here");
            }
            return hrefNode.textValue();
        } catch (JsonProcessingException | BakariException e) {
            throw new BakariException(e.getMessage());
        }
    }

    /**
     * This is a demo method if we didn't use ServerOAuth2AuthorizedClientExchangeFilterFunction.
     * Say, we authorized by clicking 'yandex' on a '/login' page with oauth2Login help.
     * Then what? How do we utilize that? How do we get the token? By that moment client repo
     * has authorized client with a token here, inside our service.
     * How do we get that token? Well, you can check out @RegisteredOAuth2AuthorizedClient.
     * But we can't use it here, we are off the ServerExchange loop.
     * Well, all we can do is create OAuth2AuthorizeRequest with clientRegistrationId and
     * principal. clientRegistrationId is easy, we have it in configs. But what about principal?
     * Well, principal is an "email" of a user who registered a client at https://oauth.yandex.ru/.
     * We aren't supposed to know that, max we get is client id and client secret for our flow.
     * So we cheat and kinda provide it via config. Thus, we can fetch the token and use it.
     *
     * @see ServerOAuth2AuthorizedClientExchangeFilterFunction
     * @see RegisteredOAuth2AuthorizedClient
     * @see OAuth2AuthorizeRequest
     */
    private void prefetchToken() throws BakariException {

        logger.info("prefetchToken start");

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("yandex")
                .principal(userEmail)
                //.attribute(ServerWebExchange.class.getName(), exchange)
                .build();

        logger.info("prefetchToken authorizing");
        OAuth2AuthorizedClient auth = reactiveOAuth2AuthorizedClientManager.authorize(authorizeRequest).block();
        if (auth == null) {
            throw new BakariException("No token for us, omg");
        }
        String token = auth.getAccessToken().getTokenValue();
        logger.info("prefetchToken token: {}", token);
    }

    /**
     * ServerOAuth2AuthorizedClientExchangeFilterFunction won't find authorized client. Why?
     * If we just provided our clientRegistrationId to webclient [.attributes(clientRegistrationId("yandex"))]
     * it would use an anonymous client with an anonymous token spelling our doom since if you remember
     * we are authenticated with oauth2Login with a concrete user (authorization_f**king_grant!).
     * So that won't match and webClient will abort a call with a cry "yandex isn't authorized bla bla bla".
     * What we do is forcibly extract the authorization and pass it to webClient as if we are in ServerExchange flow.
     * All we need is user's email.
     *
     * @see ServerOAuth2AuthorizedClientExchangeFilterFunction
     * @see AnonymousAuthenticationToken
     * @see <a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/client/authorized-clients.html">How to plug authorization into WebClient</a>
     */
    private void extractAuthorizedClient() throws BakariException {

        logger.info("extractAuthorizedClient start");

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("yandex")
                .principal(userEmail)
                //.attribute(ServerWebExchange.class.getName(), exchange)
                .build();

        logger.info("extractAuthorizedClient authorizing");
        OAuth2AuthorizedClient auth = reactiveOAuth2AuthorizedClientManager.authorize(authorizeRequest).block();
        if (auth == null) {
            throw new BakariException("No token for us, omg");
        }
        currentAuthorizedClient = auth;
    }

    /**
     * Fetches and saves file from yandex disk.
     */
    public void fetchFileToDisk() throws BakariException {

        //prefetchToken();
        extractAuthorizedClient();

        String filePathOnDIsk = "/Bears.jpg";

        logger.info("step 1...");
        String url = prefetchFile(filePathOnDIsk);

        logger.info("step 2...");
        Flux<DataBuffer> flux = fetchFile(url);

        logger.info("step 3...");
        saveFile(flux);
    }

    /**
     * Finds out where the file is, gets the encoded URL pointing to it.
     */
    private String prefetchFile(String filePathOnDisk) throws BakariException {

        logger.info("prefetchFile starting");

        Mono<String> mono = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path(yandexDiskMetadataUrl)
                        .queryParam("path", filePathOnDisk)
                        .build()
                )
                //.header(authorizationHeader, authType + " " + currentToken)
                //.headers(h -> h.setBearerAuth(currentToken))
                //.accept(MediaType.APPLICATION_OCTET_STREAM)
                .attributes(oauth2AuthorizedClient(currentAuthorizedClient))
                .retrieve()
                .bodyToMono(String.class);
//                .onErrorResume(WebClientResponseException.class,
//                        ex -> ex.getStatusCode().value() == 404 ? Mono.empty() : Mono.error(ex)
//                );

        String json = mono.block();

        return extractHref(json);
    }

    /**
     * Defines how to fetch a file, but doesn't launch the process. URL is encoded already, so special treatment
     * is required. You need to disable URL encoding.
     */
    private Flux<DataBuffer> fetchFile(String url) {

        String nonBaseUri = url.replace(yandexDiskBaseUrl, "");
        logger.info("non base uri: {}", nonBaseUri);

        return noUrlEncodingClient.get()
                .uri(nonBaseUri)
                //.header(authorizationHeader, authType + " " + currentToken)
                //.attributes(clientRegistrationId("yandex"))
                .attributes(oauth2AuthorizedClient(currentAuthorizedClient))
                .accept(MediaType.IMAGE_JPEG, MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class);
    }

    /**
     * Launches the file load and then saves it.
     */
    private void saveFile(Flux<DataBuffer> flux) {

        Path path = Paths.get(whereToSaveFile);
        DataBufferUtils.write(flux, path).block();

        logger.info("File was saved");
    }
}
