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
package com.netflix.spinnaker.clouddriver.aws.provider.agent

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.FunctionConfiguration
import com.amazonaws.services.lambda.model.ListFunctionsResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.aws.cache.Keys
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials
import spock.lang.Specification
import spock.lang.Subject

class AmazonLambdaFunctionCachingAgentSpec extends Specification {

  static final String account = 'test'
  static final String region = 'us-east-1'

  AWSLambda lambda = Mock()
  AmazonClientProvider provider = Stub() {
    getAmazonLambda(_, _) >> lambda
  }

  NetflixAmazonCredentials creds = Stub() {
    getName() >> account
  }

  ProviderCache providerCache = Mock()

  @Subject
  def agent = new AmazonLambdaFunctionCachingAgent(provider, creds, region)

  def 'should add to cache'() {
    when:
    def result = agent.loadData(providerCache)
    def expected = Keys.getLambdaFunctionKey('wow', region, account)

    then:
    1 * lambda.listFunctions() >> new ListFunctionsResult(
      functions: [
        new FunctionConfiguration(functionName: 'wow')
      ]
    )
    with(result.cacheResults.get(Keys.Namespace.LAMBDA_FUNCTIONS.ns)) { List<CacheData> cd ->
      cd.size() == 1
      cd.find { it.id == expected }
    }
    0 * _
  }
}
