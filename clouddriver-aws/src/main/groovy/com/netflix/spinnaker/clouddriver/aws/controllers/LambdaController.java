package com.netflix.spinnaker.clouddriver.aws.controllers;

import java.util.List;
import java.util.Map;
import com.netflix.spinnaker.clouddriver.aws.lambda.LambdaOperation;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.clouddriver.aws.cache.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.netflix.spinnaker.clouddriver.aws.cache.Keys.Namespace.LAMBDA_FUNCTIONS;

@RestController
@RequestMapping("/lambdas")
public class LambdaController {
  private final Cache cacheView;
  private LambdaOperation lambdaOperation;

  @Autowired
  public LambdaController(Cache cacheView, LambdaOperation lambdaOperation) {
    this.cacheView = cacheView;
    this.lambdaOperation = lambdaOperation;
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

  @RequestMapping(
    value = "/{region}/{account}",
    method = GET,
    produces = "application/json"
  )
  public List<String> listFunctions(@PathVariable String region,
                                    @PathVariable String account) {
    String functionSearch = Keys.getLambdaFunctionKey("*", region, account);
    Collection<CacheData> matches = cacheView.getAll(LAMBDA_FUNCTIONS.getNs(), cacheView.filterIdentifiers(LAMBDA_FUNCTIONS.getNs(), functionSearch));

    return matches
      .stream()
      .map(data -> (String) data.getAttributes().get("name"))
      .collect(Collectors.toList());
  }
}
