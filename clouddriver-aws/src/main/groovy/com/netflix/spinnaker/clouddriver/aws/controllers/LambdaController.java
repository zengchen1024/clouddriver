package com.netflix.spinnaker.clouddriver.aws.controllers;

import java.util.Map;
import com.netflix.spinnaker.clouddriver.aws.lambda.LambdaOperation;
import com.netflix.spinnaker.cats.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

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
    @RequestParam("account") String account,
    @RequestParam("region") String region,
    @RequestParam("function") String function,
    @RequestBody Map<String, Object> payload) {
    DeferredResult<Map<String, Object>> deferredResult = new DeferredResult<>();

    lambdaOperation
      .execute(account, region, function, payload)
      // TODO: handle exception
      .whenCompleteAsync((result, ex) -> deferredResult.setResult(result));

    return deferredResult;
  }
}
