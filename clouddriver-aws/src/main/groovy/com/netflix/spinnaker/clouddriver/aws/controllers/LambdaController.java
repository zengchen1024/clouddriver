package com.netflix.spinnaker.clouddriver.aws.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.netflix.spinnaker.clouddriver.aws.lambda.LambdaOperation;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials;
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/lambdas")
public class LambdaController {

  @Autowired
  private AmazonClientProvider amazonClientProvider;

  @Autowired
  private AccountCredentialsProvider accountCredentialsProvider;

  @Autowired
  private LambdaOperation lambdaOperation;

  @RequestMapping(value = "/{region}/{account}", method = GET)
  public List<FunctionConfiguration> listFunctions(String account, String region) {
    NetflixAmazonCredentials credentials = (NetflixAmazonCredentials) accountCredentialsProvider.getCredentials(account);
    AWSLambda lambda = amazonClientProvider.getAmazonLambda(credentials, region);

    List<FunctionConfiguration> allFunctions = new ArrayList<>();

    String next = null;
    do {
      ListFunctionsResult result = lambda.listFunctions(new ListFunctionsRequest().withMarker(next));
      List<FunctionConfiguration> functions = result.getFunctions();

      if (functions != null) {
        for (FunctionConfiguration function : functions) {
          allFunctions.add(function);
        }
      }

      next = result.getNextMarker();

    } while (next != null);

    return allFunctions;
  }

  @RequestMapping(
    value = "/{region}/{account}/{function}",
    method = POST,
    produces = "application/json"
  )
  public DeferredResult<Map<String, Object>> invokeFunction(
    String account,
    String region,
    String function,
    Map<String, Object> payload) {
    DeferredResult<Map<String, Object>> deferredResult = new DeferredResult<>();

    lambdaOperation
      .execute(account, region, function, payload)
      // TODO: handle exception
      .whenCompleteAsync((result, ex) -> deferredResult.setResult(result));

    return deferredResult;
  }
}
