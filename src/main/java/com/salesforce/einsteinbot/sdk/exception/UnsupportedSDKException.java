package com.salesforce.einsteinbot.sdk.exception;

public class UnsupportedSDKException extends RuntimeException {
  private static final String ERROR_MESSAGE_FORMAT = "SDK failed to start chat as the API version is not supported. Current API version in SDK is %s, latest supported API version is %s, please upgrade to the latest version. Further information about specific versions is available here: https://github.com/forcedotcom/einstein-bot-sdk-java#version-numbering";

  public UnsupportedSDKException(String currentVersion, String latestVersion) {
    super(String.format(ERROR_MESSAGE_FORMAT, currentVersion, latestVersion));
  }
}
