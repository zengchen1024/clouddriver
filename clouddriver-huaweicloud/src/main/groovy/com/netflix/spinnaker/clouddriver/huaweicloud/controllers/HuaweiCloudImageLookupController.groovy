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
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudImage
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.ImageProvider
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
@RequestMapping("/huaweicloud/images")
class HuaweiCloudImageLookupController {

  final ImageProvider imageProvider

  @Autowired
  HuaweiCloudImageLookupController(ImageProvider imageProvider) {
    this.imageProvider = imageProvider
  }

  @RequestMapping(value = '/find', method = RequestMethod.GET)
  List<HuaweiCloudImage> find(@RequestParam(required = false) String account,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String region) {
    List<HuaweiCloudImage> result = this.imageProvider.getAll(account, region)
    if (!result) {
      return Collections.emptySet()
    }

    result.stream()
      .filter(getQueryFilter(q))
      .sorted(Comparator.comparing( { image -> image.name } ))
      .collect(Collectors.toList())
  }

  private Predicate<HuaweiCloudImage> getQueryFilter(String q) {
    Predicate<HuaweiCloudImage> queryFilter = { i -> true }

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
