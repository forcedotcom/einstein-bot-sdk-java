# Einstein Bots Runtime - Java SDK

Also refer to [Einstein Bot SDK public documentation](https://developer.salesforce.com/docs/service/einstein-bot-api/guide/sdk-overview.html)

Java SDK to interact with Einstein Bots Runtime. This SDK is a wrapper around the [Einstein Bots Runtime API](https://developer.salesforce.com/docs/service/einstein-bot-api/guide/get-started-overview.html) that provides a few added features out of the box:

* Auth support
* Session management


## Auth Support

Einstein Bots Runtime requires requests to be authenticated using OAuth. This SDK supports the [JWT Bearer OAuth Flow](https://help.salesforce.com/s/articleView?id=sf.remoteaccess_oauth_jwt_flow.htm&type=5). 

Additional auth mechanisms can be added by implementing the [AuthMechanism interface](src/main/java/com/salesforce/chatbot/sdk/auth/AuthMechanism.java).

## Session Management

A session ID is generated by Einstein Bots Runtime for every new session. This needs to be included in future requests sent to the service.

This SDK provides session management capabilities, caching Einstein Bots Runtime's session ID based on an external session ID.

An external session ID is the unique ID used to identify a session on the channel that is sending messages to Einstein Bots Runtime. Using Slack as an example,
the external session ID could be a combination of Slack app ID, Slack user ID and Slack channel ID (if present).

`Redis` is supported out of the box. There is an `InMemoryCache` included that is meant for testing purposes only. Additional caching methods can be used by implementing the [Cache interface](src/main/java/com/salesforce/chatbot/sdk/cache/Cache.java).

## Usage

### Add POM dependency

```xml
<dependency>
  <groupId>com.salesforce.einsteinbot</groupId>
  <artifactId>einstein-bot-sdk-java</artifactId>
  <version>${einstein-bot-sdk-java-version}</version>
</dependency>
```

### 1. Create Redis Cache
```java

    Cache redis = new RedisCache(); // uses default values for ttl (3 days) and redisUrl (redis://127.0.0.1:6379)
    
    // for custom ttl or redis url:
    Cache redis = new RedisCache(ttlSeconds, redisUrl);

```

### 2. Setup OAuth
```java
    private final String pvtKeyFile = "<your pvt key file>"; //filepath to pvt key file in der format
    private final String loginEndpoint = "<your login endpoint>"; //i.e. https://login.salesforce.com/
    private final String connectedAppId = "<your connected app id>"; //can be found in App Manager page
    private final String secret = "<your connected app secret>"; //can be found in App Manager page
    private final String userId = "<userId associated with certificate used for connected app>";
    
    JwtBearerOAuth oAuth = JwtBearerOAuth.with()
    .privateKeyFilePath(pvtKeyFile)
    .loginEndpoint(loginEndpoint)
    .connectedAppId(connectedAppId)
    .connectedAppSecret(secret)
    .userId(userId)
    .cache(cache) // 'cache' (Optional) created in Step 1. 
    // Provide cache if you want to cache oAuth tokens to avoid network requests
    .build(); 
```

You are also able to use a different constructor and pass in a `java.security.PrivateKey` instead of a private key file.

```java
 JwtBearerOAuth.with()
    .privateKey(privateKey)
    .loginEndpoint(loginEndpoint)
    ...
```
### 3. Setup Chatbot Client

Follow **step 3A** to use `BasicChatbotClient` if you want to track sessions yourself or 
follow **step 3B** to use `SessionManagedChatbotClient` for sessions to be managed by SDK.

#### 3A. Setup Basic Chatbot Client

```java
    ChatbotClient client = ChatbotClients.basic()
            .basePath(basePath) // Einstein Bots Runtime basepath. Can be found in the setup page
            .authMechanism(oAuth) // 'oAuth' created in Step 2
            .build();
```

#### 3B. Setup Session Managed Chatbot Client

```java
    SessionManagedChatbotClient client = ChatbotClients
        .sessionManaged()
        .basicClient(BasicChatbotClient.builder()
        .basePath(basePath) // Einstein Bots Runtime basepath. Can be found in the setup page
        .authMechanism(oAuth)  // 'oAuth' created in Step 2
        .build())
        .cache(cache)  // 'cache' created in Step 1
        .integrationName(integrationName) // Should match integrationName used when adding API Connection for connected app.
        .build();
```

### 4. Sending a Message

```Java

private void sendUsingBasicClient(){

    RequestConfig config = createRequestConfig();

    BotSendMessageRequest botSendInitMessageRequest = BotRequest
      .withMessage(buildTextMessage("Initial Message"))
      .build();

    //Start a new chat session
    BotResponse resp = client
      .startChatSession(config, externalSessionKey, botSendInitMessageRequest);

    System.out.println("Init Message Response :" + resp);

    String sessionId = resp.getResponseEnvelope().getSessionId();
    //Send a message to existing chat session
    BotSendMessageRequest botSendMessageRequest =  BotRequest
      .withMessage(buildTextMessage("Order Status"))
      .build();

    
    BotResponse textMsgResponse = client
      .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);  // 'client' created in Step 3A.

    System.out.println("Text Message Response :" + textMsgResponse);

    //End a chat session
    BotEndSessionRequest botEndSessionRequest = BotRequest
      .withEndSession(EndSessionReason.USER_REQUEST)
      .build();

    BotResponse endSessionResponse = client
      .endChatSession(config, new RuntimeSessionId(sessionId), botEndSessionRequest);

    System.out.println("End Session Response :" + endSessionResponse);
   
}

private void sendUsingSessionManagedClient() {

    RequestConfig config = createRequestConfig();
    
    BotSendMessageRequest botSendFirstMessageRequest = BotRequest
      .withMessage(buildTextMessage("Initial Message"))
      .build();

    //Just send a message, new session will be automatically created.
    BotResponse firstMsgResp = client
      .sendMessage(config, externalSessionKey, botSendFirstMessageRequest); // 'client' created in Step 3B.
    
    System.out.println("First Message Response: " + firstMsgResp);

    //Sending a another message with same externalSessionId will sendMessage to existing Session.
    BotSendMessageRequest botSendSecondMessageRequest =  BotRequest
      .withMessage(buildTextMessage("Order Status"))
      .build();

    BotResponse secondMsgResp = client.sendMessage(config, externalSessionKey, botSendSecondMessageRequest);

    System.out.println("Second Message Response: " + secondMsgResp);

    //End a chat session
    BotEndSessionRequest botEndSessionRequest = BotRequest
      .withEndSession(EndSessionReason.USER_REQUEST)
      .build();

    BotResponse endSessionResponse = client
      .endChatSession(config, externalSessionKey, botEndSessionRequest);

    System.out.println("End Session Response :" + convertObjectToJson(endSessionResponse));
  }

public static AnyRequestMessage buildTextMessage(String msg) {
    return new TextMessage()
      .text(msg)
      .type(TextMessage.TypeEnum.TEXT)
      .sequenceId(System.currentTimeMillis());
    }
    
public static RequestConfig createRequestConfig(){
    return RequestConfig.with()
      .botId(botId)
      .orgId(orgId)
      .forceConfigEndpoint(forceConfigEndPoint)
      .build()
    }    

```

### 5. Getting Health Status

```java
private void getHealthStatus() {
    System.out.println(client.getHealthStatus()); // 'client' created in Step 3A or 3B
  }
```

## Tips and Tricks

* If you use your own ObjectMapper instance in your application. Check how ObjectMapper is created in [UtilFunctions.getMapper()](https://github.com/forcedotcom/einstein-bot-sdk-java/blob/master/src/main/java/com/salesforce/einsteinbot/sdk/util/UtilFunctions.java#L114) to make sure to configure it correctly. 
Otherwise, you may run into issues with serializing/deserializing JSON.



### Full code examples

See [ChatbotClientExamples](src/test/java/com/salesforce/chatbot/sdk/examples/ChatbotClientExamples.java) for full code sample sending messages to Einstein Bots Runtime.

See [OAuthExamples](src/test/java/com/salesforce/chatbot/sdk/examples/OAuthExamples.java) for using OAuth.

## Developer Guide

### Code Style

The project uses the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
Format settings definition files for importing into IDEs are available for [Eclipse](https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml)
and [IntelliJ IDEA](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml).

### Publishing to Maven Central

#### Publish Release Versions
Go to [actions](https://github.com/forcedotcom/einstein-bot-sdk-java/actions/workflows/maven-release.yml) tab and select **Maven Release** action. Click *Run workflow* , provide good description and hit **Run workflow** button. 

#### Publish Snapshot Versions
Go to [actions](https://github.com/forcedotcom/einstein-bot-sdk-java/actions/workflows/maven-publish.yml) tab and select **Maven Publish Snapshot** action. Click *Run workflow* , provide good description and hit **Run workflow** button. 

#### Choosing Release Versions

The version of the artifact released is same as version defined in [pom.xml](https://github.com/forcedotcom/einstein-bot-sdk-java/blob/master/pom.xml#L14) with `-SNAPSHOT` removed. 
After the release pom.xml will be automatically updated with next patch snapshot version.

For eg. if `pom.xml` has version `2.0.1-SNAPSHOT` , then version `2.0.1` will be released and the pom.xml will be updated with version 2.0.2-SNAPSHOT

If you want to release a minor or major version, just update version in pom.xml, For eg, if you want to release `3.0.0` , then set version as  `3.0.0-SNAPSHOT` in pom.xml.

#### Version Numbering

The **minor** and **patch** versions will be incremented for minor features and bug fixes.

The **major** version will be incremented for 
* Major changes made to SDK OR 
* New Runtime API version support is added.

Here is the SDK version and corresponding Runtime API version supported by it.

| SDK Version | Supported Runtime API                     
|-------------| --------------------------------------
| 1.x.x       | /v4.0.0                        
| 2.0.x       | /v5.0.0
| 2.1.x       | /v5.1.0
| 2.2.x       | /v5.2.0
| 2.3.x       | /v5.3.0
| 3.0.0       | /v5.3.0

**Note**: The support for rich links has been removed from API V5.1.0, please upgrade from SDK V2.1.0 to V2.1.1 or later.

### Branching model to support development of multiple API versions

We will maintain the SDK code for each Runtime API version separately in individual branches. 
We will cut off release branch for each Runtime API version.

For eg, Currently, the master branch is used to develop for Runtime API Version 5.1.0. 
The [releases/api-4.x](https://github.com/forcedotcom/einstein-bot-sdk-java/tree/releases/api-4.x) branch is used to maintain support for API 4.0.0.

So to make change for SDK 1.x.x that supports API v4.0.0, it should be committed to `releases/api-4.x` branch. 
Also, choose `releases/api-4.x` when running Release action workflow.