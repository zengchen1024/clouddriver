/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.netflix.spinnaker.clouddriver.huaweicloud.controllers

import com.netflix.spinnaker.cats.mem.InMemoryCache
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudServerGroupConfig
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.ServerGroupConfigProvider
import groovy.util.logging.Slf4j
import java.util.Comparator
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.stream.Collectors
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
@RequestMapping("/huaweicloud/serverGroupConfigs")
class HuaweiCloudServerGroupConfigLookupController {

  final ServerGroupConfigProvider serverGroupConfigProvider

  @Autowired
  HuaweiCloudServerGroupConfigLookupController(ServerGroupConfigProvider provider) {
    this.serverGroupConfigProvider = provider
  }

  @RequestMapping(value = '/find', method = RequestMethod.GET)
  List<HuaweiCloudServerGroupConfig> find(@RequestParam(required = false) String account,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String region) {
    List<HuaweiCloudServerGroupConfig> result = this.serverGroupConfigProvider.getAll(account, region)
    if (!result) {
      return Collections.emptySet()
    }

    result.stream()
      .filter(getQueryFilter(q))
      .sorted(Comparator.comparing( { config -> config.name } ))
      .collect(Collectors.toList())
  }

  private Predicate<HuaweiCloudServerGroupConfig> getQueryFilter(String q) {
    Predicate<HuaweiCloudServerGroupConfig> queryFilter = { i -> true }

    if (q != null && q.trim().length() > 0) {
      String glob = q.trim()
      if (!glob.contains("*")
          && !glob.contains("?")
          && !glob.contains("[")
          && !glob.contains("\\")) {
        glob = "*" + glob + "*"
      }
      Pattern pattern = new InMemoryCache.Glob(glob).toPattern()
      queryFilter = { i -> pattern.matcher(i.name).matches() }
    }

    queryFilter
  }
}
