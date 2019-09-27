/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
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

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudImage
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.ImageProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.IMAGES

@Component
class HuaweiCloudImageProvider implements ImageProvider {

  final Cache cacheView
  final ObjectMapper objectMapper

  @Autowired
  HuaweiCloudImageProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  List<HuaweiCloudImage> getAll(String account, String region) {

    Collection<String> filter = cacheView.filterIdentifiers(
      IMAGES.ns,
      Keys.getImageKey('*', account ?: '*', region ?: '*')
    )

    Collection<CacheData> data = cacheView.getAll(IMAGES.ns, filter)

    data ? data.collect(this.&fromCacheData) : []
  }

  HuaweiCloudImage fromCacheData(CacheData cacheData) {
    Map image = cacheData.attributes.image
    Map<String, String> parts = Keys.parse(cacheData.id)

    return new HuaweiCloudImage(
      id: parts.id,
      region: parts.region,
      name: image.name
    )
  }
}