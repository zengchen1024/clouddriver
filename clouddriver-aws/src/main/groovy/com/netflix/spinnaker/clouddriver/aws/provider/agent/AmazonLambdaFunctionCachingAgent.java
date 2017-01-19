/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.clouddriver.aws.provider.agent;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.netflix.spinnaker.cats.agent.AccountAware;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.aws.cache.Keys;
import com.netflix.spinnaker.clouddriver.aws.provider.AwsInfrastructureProvider;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials;
import com.netflix.spinnaker.clouddriver.cache.CustomScheduledAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;
import static com.netflix.spinnaker.clouddriver.aws.cache.Keys.Namespace.LAMBDA_FUNCTIONS;

public class AmazonLambdaFunctionCachingAgent implements CachingAgent, CustomScheduledAgent, AccountAware {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public static final long DEFAULT_POLL_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(2);
  public static final long DEFAULT_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(4);

  final AmazonClientProvider amazonClientProvider;
  final NetflixAmazonCredentials account;
  final String region;

  final long pollIntervalMillis;
  final long timeoutMillis;

  static final Set<AgentDataType> types = Collections.unmodifiableSet(new HashSet<AgentDataType>() {
    {
      add(AUTHORITATIVE.forType(LAMBDA_FUNCTIONS.getNs()));
    }
  });

  public AmazonLambdaFunctionCachingAgent(AmazonClientProvider amazonClientProvider,
                                          NetflixAmazonCredentials account,
                                          String region) {
    this(amazonClientProvider, account, region, DEFAULT_POLL_INTERVAL_MILLIS, DEFAULT_TIMEOUT_MILLIS);
  }

  public AmazonLambdaFunctionCachingAgent(AmazonClientProvider amazonClientProvider,
                                          NetflixAmazonCredentials account,
                                          String region,
                                          long pollIntervalMillis,
                                          long timeoutMillis) {
    this.amazonClientProvider = amazonClientProvider;
    this.account = account;
    this.region = region;
    this.pollIntervalMillis = pollIntervalMillis;
    this.timeoutMillis = timeoutMillis;
  }

  @Override
  public String getAgentType() {
    return account.getName() + "/" + region + "/" + AmazonLambdaFunctionCachingAgent.class.getSimpleName();
  }

  @Override
  public String getProviderName() {
    return AwsInfrastructureProvider.PROVIDER_NAME;
  }

  @Override
  public long getPollIntervalMillis() {
    return pollIntervalMillis;
  }

  @Override
  public long getTimeoutMillis() {
    return timeoutMillis;
  }

  @Override
  public String getAccountName() {
    return account.getName();
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    log.info("Describing items in " + getAgentType());
    AWSLambda lambda = amazonClientProvider.getAmazonLambda(account, region);
    List<CacheData> data = new ArrayList<>();
    List<FunctionConfiguration> allFunctions = new ArrayList<>();

    String next = null;
    do {
      ListFunctionsResult result = lambda.listFunctions(new ListFunctionsRequest().withMarker(next));
      List<FunctionConfiguration> functions = result.getFunctions();
      if (functions != null) {
        functions.forEach(allFunctions::add);
      }

      next = result.getNextMarker();

    } while (next != null);


    data.addAll(allFunctions
      .stream()
      .map(f -> {
        String key = Keys.getLambdaFunctionKey(f.getFunctionName(), region, account.getName());

        Map<String, Object> dat = new HashMap<>();
        dat.put("account", account.getName());
        dat.put("region", region);
        dat.put("name", f.getFunctionName());

        return new DefaultCacheData(
          key,
          dat,
          new HashMap<>()
        );
      }).collect(Collectors.toSet()));

    log.info(String.format("Caching %d items in %s", data.size(), getAgentType()));

    Map<String, Collection<CacheData>> result = new HashMap<>();
    result.put(LAMBDA_FUNCTIONS.getNs(), data);

    return new DefaultCacheResult(result);
  }
}
