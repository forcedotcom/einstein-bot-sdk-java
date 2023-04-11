package com.salesforce.einsteinbot.sdk.client.util;

import static com.salesforce.einsteinbot.sdk.util.WebClientUtil.createErrorResponseProcessor;
import static com.salesforce.einsteinbot.sdk.util.WebClientUtil.createFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.einsteinbot.sdk.api.BotApi;
import com.salesforce.einsteinbot.sdk.api.HealthApi;
import com.salesforce.einsteinbot.sdk.api.VersionsApi;
import com.salesforce.einsteinbot.sdk.exception.ChatbotResponseException;
import com.salesforce.einsteinbot.sdk.handler.ApiClient;
import com.salesforce.einsteinbot.sdk.util.LoggingJsonEncoder;
import com.salesforce.einsteinbot.sdk.util.ReleaseInfo;
import com.salesforce.einsteinbot.sdk.util.UtilFunctions;
import com.salesforce.einsteinbot.sdk.util.WebClientUtil;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class ClientFactory {

  public static class ClientWrapper {

    protected BotApi botApi;
    protected HealthApi healthApi;
    protected VersionsApi versionsApi;
    protected ApiClient apiClient;
    protected ReleaseInfo releaseInfo = ReleaseInfo.getInstance();

    protected ClientWrapper(String basePath, WebClient.Builder webClientBuilder) {
      this.apiClient = new ApiClient(createWebClient(webClientBuilder), UtilFunctions.getMapper(),
          UtilFunctions
              .createDefaultDateFormat());
      apiClient.setBasePath(basePath);
      apiClient.setUserAgent(releaseInfo.getAsUserAgent());
      botApi = new BotApi(apiClient);
      healthApi = new HealthApi(apiClient);
      versionsApi = new VersionsApi(apiClient);
    }

    public ApiClient getApiClient() {
      return this.apiClient;
    }

    public BotApi getBotApi() {
      return this.botApi;
    }

    public VersionsApi getVersionsApi() {
      return this.versionsApi;
    }

    public HealthApi getHealthApi() {
      return this.healthApi;
    }

    public void setBotApi(BotApi botApi) {
      this.botApi = botApi;
    }

    public void setHealthApi(HealthApi healthApi) {
      this.healthApi = healthApi;
    }

    public void setVersionsApi(VersionsApi versionsApi) {
      this.versionsApi = versionsApi;
    }

    private WebClient createWebClient(WebClient.Builder webClientBuilder) {

      return webClientBuilder
          .codecs(createCodecsConfiguration(UtilFunctions.getMapper()))
          .filter(createFilter(WebClientUtil::createLoggingRequestProcessor,
              clientResponse -> createErrorResponseProcessor(clientResponse,
                  this::mapErrorResponse)))
          .build();
    }

    private Consumer<ClientCodecConfigurer> createCodecsConfiguration(ObjectMapper mapper) {
      return clientDefaultCodecsConfigurer -> {
        clientDefaultCodecsConfigurer.defaultCodecs()
            .jackson2JsonEncoder(new LoggingJsonEncoder(mapper, MediaType.APPLICATION_JSON, false));
        clientDefaultCodecsConfigurer.defaultCodecs()
            .jackson2JsonDecoder(new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
      };
    }

    private Mono<ClientResponse> mapErrorResponse(ClientResponse clientResponse) {
      return clientResponse
          .body(WebClientUtil.errorBodyExtractor())
          .flatMap(errorDetails -> Mono
              .error(new ChatbotResponseException(clientResponse.statusCode(), errorDetails,
                  clientResponse.headers())));
    }
  }

  public static ClientWrapper createClient(String basePath, WebClient.Builder webClientBuilder) {
    return new ClientWrapper(basePath, webClientBuilder);
  }

}
