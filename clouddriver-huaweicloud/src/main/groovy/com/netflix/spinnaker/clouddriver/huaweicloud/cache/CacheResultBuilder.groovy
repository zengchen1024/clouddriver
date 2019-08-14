/*
 * 2019.8.14 modify NamespaceBuilder
 *
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.cache

import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.ON_DEMAND

@Slf4j
class CacheResultBuilder {

  Long startTime

  CacheMutation onDemand = new CacheMutation()

  Map<String, NamespaceCache> namespaceBuilders = [:].withDefault {
    ns -> new NamespaceCache(namespace: ns)
  }

  NamespaceCache namespace(String ns) {
    namespaceBuilders.get(ns)
  }

  DefaultCacheResult build() {
    Map<String, Collection<CacheData>> keep = [:]
    Map<String, Collection<String>> evict = [:]

    if (!onDemand.toKeep.empty) {
      keep[(ON_DEMAND.ns)] = onDemand.toKeep.values()
    }

    if (!onDemand.toEvict.empty) {
      evict[(ON_DEMAND.ns)] = onDemand.toEvict
    }

    namespaceBuilders.each { String namespace, NamespaceCache item ->
      if (!item.toKeep.empty) {
        keep[(namespace)] = item.keepValues()
      }

      if (!item.toEvict.empty) {
        evict[(namespace)] = item.toEvict
      }
    }

    new DefaultCacheResult(keep, evict)
  }

  class CacheMutation {
    Map<String, CacheData> toKeep = [:]
    List<String> toEvict = []
  }

  class NamespaceCache {
    String namespace

    Map<String, CacheDataBuilder> toKeep = [:].withDefault {
      id -> new CacheDataBuilder(id: id)
    }

    List<String> toEvict = []

    CacheDataBuilder keep(String key) {
      toKeep.get(key)
    }

    int keepSize() {
      toKeep.size()
    }

    def keepValues() {
      toKeep.collect { _, v -> v.build() } as Set
    }
  }

  class CacheDataBuilder {
    String id = ''
    int ttlSeconds = -1
    Map<String, Object> attributes = [:]
    Map<String, Collection<String>> relationships = [:].withDefault { [] as Set }

    public DefaultCacheData build() {
      new DefaultCacheData(id, ttlSeconds, attributes, relationships)
    }
  }
}
