package com.netflix.spinnaker.clouddriver.aws.controllers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.clouddriver.aws.cache.Keys;
import com.netflix.spinnaker.clouddriver.aws.lambda.LambdaOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.netflix.spinnaker.clouddriver.aws.cache.Keys.Namespace.LAMBDA_FUNCTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
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
    value = "/{account}/{region}/{function}",
    method = POST,
    produces = "application/json"
  )
  public Map<String, Object> invokeFunction(
    @PathVariable String account,
    @PathVariable String region,
    @PathVariable String function,
    @RequestBody Map<String, Object> payload) {
//    DeferredResult<Map<String, Object>> deferredResult = new DeferredResult<>();
//
//    lambdaOperation
//      .execute(account, region, function, payload)
//      // TODO: handle exception
//      .whenCompleteAsync((result, ex) -> deferredResult.setResult(result));
//
//    return deferredResult;
    return lambdaOperation.execute(account, region, function, payload);
  }

  @ExceptionHandler(AWSLambdaException.class)
  public ResponseEntity<AWSLambdaException> handleCustomException(AWSLambdaException ex) {
    return new ResponseEntity<>(ex, HttpStatus.valueOf(ex.getStatusCode()));
  }
}
