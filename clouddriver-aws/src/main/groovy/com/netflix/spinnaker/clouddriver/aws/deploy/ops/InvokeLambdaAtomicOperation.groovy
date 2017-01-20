package com.netflix.spinnaker.clouddriver.aws.deploy.ops

import com.amazonaws.services.lambda.model.InvokeRequest
import com.amazonaws.services.lambda.model.InvokeResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.clouddriver.aws.deploy.description.InvokeLambdaDescription
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import org.springframework.beans.factory.annotation.Autowired

class InvokeLambdaAtomicOperation implements AtomicOperation<Map<String, Object>> {

  private final InvokeLambdaDescription description

  @Autowired ObjectMapper amazonObjectMapper
  @Autowired AmazonClientProvider amazonClientProvider
  @Autowired AccountCredentialsProvider credentialsProvider

  InvokeLambdaAtomicOperation(InvokeLambdaDescription description) {
    this.description = description
  }

  @Override
  Map<String, Object> operate(List priorOutputs) {
    try {
      InvokeRequest request = new InvokeRequest()
      request.setFunctionName(description.function)
      request.setPayload(amazonObjectMapper.writeValueAsString(description.payload))
      return parseResult(
        amazonClientProvider.getAmazonLambda(
          credentialsProvider.getCredentials(description.account) as NetflixAmazonCredentials,
          description.region
        )
          .invoke(request)
      )
    } catch (IOException e) {
      throw new IllegalArgumentException(
        "Couldn't convert request payload to JSON",
        e
      )
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseResult(InvokeResult result) {
    try {
      return amazonObjectMapper.readValue(result.getPayload().array(), Map.class)
    } catch (IOException e) {
      throw new RuntimeException(e)
    }
  }
}
