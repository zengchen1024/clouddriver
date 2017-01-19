package com.netflix.spinnaker.clouddriver.aws.lambda;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LambdaOperation {

  private final AmazonClientProvider amazonClientProvider;
  private final AWSCredentialsProvider awsCredentialsProvider;
  private final ObjectMapper mapper;

  @Autowired
  public LambdaOperation(AmazonClientProvider amazonClientProvider,
                         AWSCredentialsProvider awsCredentialsProvider,
                         ObjectMapper mapper) {
    this.amazonClientProvider = amazonClientProvider;
    this.awsCredentialsProvider = awsCredentialsProvider;
    this.mapper = mapper;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> execute(String account,
                                     String region,
                                     String functionName,
                                     Map<String, Object> input) {
    try {
      InvokeRequest request = new InvokeRequest();
      request.setFunctionName(functionName);
      request.setPayload(mapper.writeValueAsString(input));

      ByteBuffer result = amazonClientProvider
        .getAmazonLambda(account, awsCredentialsProvider, region)
        .invoke(request)
        .getPayload();
      return mapper.readValue(result.array(), Map.class);
    } catch (IOException e) {
      throw new LambdaExecutionException(e);
    }
  }
}
