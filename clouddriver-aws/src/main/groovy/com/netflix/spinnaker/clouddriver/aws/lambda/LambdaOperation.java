package com.netflix.spinnaker.clouddriver.aws.lambda;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static com.google.common.util.concurrent.Futures.lazyTransform;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Component
public class LambdaOperation {

  private final AmazonClientProvider amazonClientProvider;
  private final AWSCredentialsProvider awsCredentialsProvider;
  private final ObjectMapper amazonObjectMapper;

  @Autowired
  public LambdaOperation(AmazonClientProvider amazonClientProvider,
                         AWSCredentialsProvider awsCredentialsProvider,
                         ObjectMapper amazonObjectMapper) {
    this.amazonClientProvider = amazonClientProvider;
    this.awsCredentialsProvider = awsCredentialsProvider;
    this.amazonObjectMapper = amazonObjectMapper;
  }

  public CompletableFuture<Map<String, Object>> execute(String account,
                                                        String region,
                                                        String functionName,
                                                        Map<String, Object> input) {
    try {
      InvokeRequest request = new InvokeRequest();
      request.setFunctionName(functionName);
      request.setPayload(amazonObjectMapper.writeValueAsString(input));

      // TODO: fix this as it blocks a thread from common pool
      return supplyAsync(() -> {
        try {
          return invokeFunction(account, region, request).get();
        } catch (InterruptedException | ExecutionException e) {
          throw new LambdaExecutionException(e);
        }
      });
    } catch (IOException e) {
      throw new IllegalArgumentException(
        "Couldn't convert request payload to JSON",
        e
      );
    }
  }

  private Future<Map<String, Object>> invokeFunction(String account,
                                                     String region,
                                                     InvokeRequest request) {
    return lazyTransform(
      amazonClientProvider
        .getAmazonLambdaAsync(account, awsCredentialsProvider, region)
        .invokeAsync(request),
      this::parseResult
    );
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseResult(InvokeResult result) {
    try {
      return amazonObjectMapper.readValue(result.getPayload().array(), Map.class);
    } catch (IOException e) {
      throw new LambdaExecutionException(e);
    }
  }
}
