package com.netflix.spinnaker.clouddriver.aws.lambda;

public class LambdaExecutionException extends RuntimeException {
  public LambdaExecutionException(Throwable cause) {
    super(cause);
  }
}
