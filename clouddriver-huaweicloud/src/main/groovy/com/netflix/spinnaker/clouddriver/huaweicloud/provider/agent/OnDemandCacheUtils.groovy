/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent.OnDemandResult
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.CacheResultBuilder
import java.util.concurrent.TimeUnit
import groovy.transform.Canonical

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.ON_DEMAND


class OnDemandCacheUtils {
  /**
   * Move cached data from onDemand to resource (Server Group, Load Balancer, Security Group) namespace.
   * @param objectMapper
   * @param cacheResultBuilder
   * @param serverGroupKey
   */
  static void moveOnDemandDataToNamespace(ObjectMapper objectMapper, CacheResultBuilder cacheResultBuilder, String key) {

    Map<String, List<MutableCacheData>> onDemandData = objectMapper.readValue(
      cacheResultBuilder.onDemand.toKeep[key].attributes.cacheResults as String,
      new TypeReference<Map<String, List<MutableCacheData>>>() {}
    )

    onDemandData.each { String namespace, List<MutableCacheData> cacheDatas ->
      if (namespace != ON_DEMAND.ns ) {
        cacheDatas.each { MutableCacheData cacheData ->
          cacheResultBuilder.namespace(namespace).keep(cacheData.id).with {
            it.attributes = cacheData.attributes
            it.relationships = cacheData.relationships // TODO merge relationships
          }
          cacheResultBuilder.onDemand.toKeep.remove(cacheData.id)
        }
      }
    }
  }

  /**
   * Helper method to inspect onDemand 'toKeep' cache to see if the cacheData should be used
   * instead of recreating it.
   * @param cacheResultBuilder
   * @param key
   * @return
   */
  static boolean shouldUseOnDemandData(CacheResultBuilder cacheResultBuilder, String key) {
    CacheData cacheData = cacheResultBuilder.onDemand.toKeep[key]
    cacheData ? cacheData.attributes.cacheTime >= cacheResultBuilder.startTime : false
  }

  /**
   * Add the cache record to ON_DEMAND namespace.
   * @param cacheResult
   * @param objectMapper
   * @param metricsSupport
   * @param providerCache
   * @param namespace
   * @param key
   * @return
   */
  static void saveOnDemandCache(CacheResult cacheResult, ObjectMapper objectMapper, OnDemandMetricsSupport metricsSupport, ProviderCache providerCache, String key) {
    metricsSupport.onDemandStore {
      CacheData cacheData = new DefaultCacheData(
        key,
        TimeUnit.MINUTES.toSeconds(10) as Integer, // ttl
        [
          cacheTime     : System.currentTimeMillis(),
          cacheResults  : objectMapper.writeValueAsString(cacheResult.cacheResults),
          processedCount: 0,
          processedTime : null
        ],
        [:]
      )

      providerCache.putCacheData(ON_DEMAND.ns, cacheData)
    }
  }

  /**
   * Method is a template for handling ON_DEMAND cache normal load data scenarios.  It will
   * check to see that ON_DEMAND data is evicted or keep based upon timestamp and process count.  At the end of processing
   * it will set processing time and increment count by 1.
   * @param providerCache
   * @param keys
   * @param cacheResultClosure
   * @return
   */
  static CacheResult buildLoadDataCache(ProviderCache providerCache, List<String> keys, Closure<CacheResult> cacheResultClosure) {
    CacheResultBuilder cacheResultBuilder = new CacheResultBuilder(startTime: System.currentTimeMillis())

    providerCache.getAll(ON_DEMAND.ns, keys).each { CacheData cacheData ->
      if (cacheData.attributes.cacheTime < cacheResultBuilder.startTime && cacheData.attributes.processedCount > 0) {
        cacheResultBuilder.onDemand.toEvict << cacheData.id
      } else {
        cacheResultBuilder.onDemand.toKeep[cacheData.id] = cacheData
      }
    }

    CacheResult result = cacheResultClosure.call(cacheResultBuilder)

    result.cacheResults[ON_DEMAND.ns].each {
      it.attributes.processedTime = System.currentTimeMillis()
      it.attributes.processedCount = (it.attributes.processedCount ?: 0) + 1
    }

    result
  }

  @Canonical
  static class MutableCacheData implements CacheData {
    String id
    int ttlSeconds = -1
    Map<String, Object> attributes = [:]
    Map<String, Collection<String>> relationships = [:].withDefault { [] as Set }
  }
}
