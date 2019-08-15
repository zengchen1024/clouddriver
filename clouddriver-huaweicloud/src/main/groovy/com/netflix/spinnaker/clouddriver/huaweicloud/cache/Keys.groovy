/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
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

package com.netflix.spinnaker.clouddriver.huaweicloud.cache

import com.netflix.frigga.Names
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider

import static com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider.ID

class Keys {

  static enum Namespace {
    APPLICATIONS,
    CLUSTERS,
    NETWORKS,
    SUBNETS,
    ON_DEMAND

    final String ns

    private Namespace() {
      def parts = name().split('_')

      ns = parts.tail().inject(new StringBuilder(parts.head().toLowerCase())) {
        val, next -> val.append(next.charAt(0)).append(next.substring(1).toLowerCase()) }
    }

    String toString() {
      ns
    }
  }

  static Map<String, String> parse(String key) {
    def parts = key.split(':')
    if ((parts.length < 2) || (parts[0] != HuaweiCloudProvider.ID)) {
      return null
    }

    def result = [:]
    switch (parts[1]) {
      case Namespace.APPLICATIONS.ns:
        if (parts.length == 3) {
          result << [application: parts[2].toLowerCase()]
        }
        break
      case Namespace.CLUSTERS.ns:
        if (parts.length == 5) {
          def names = Names.parseName(parts[4])
          result << [
            application: parts[3].toLowerCase(),
            account: parts[2],
            name: parts[4],
            stack: names.stack,
            detail: names.detail]
        }
        break
      case Namespace.NETWORKS.ns:
        if (parts.length == 5) {
          result << [
            account: parts[2],
            region: parts[3],
            id: parts[4]]
        }
        break
      case Namespace.SUBNETS.ns:
        if (parts.length == 5) {
          result << [
            account: parts[2],
            region: parts[3],
            id: parts[4]]
        }
        break
      default:
        return null
    }

    if (result.isEmpty()) {
      return null
    }
    result << [provider: parts[0], type: parts[1]]
    result
  }

  static String getApplicationKey(String application) {
    "${ID}:${Namespace.APPLICATIONS}:${application.toLowerCase()}"
  }

  static String getNetworkKey(String networkId, String account, String region) {
    "${ID}:${Namespace.NETWORKS}:${account}:${region}:${networkId}"
  }

  static String getSubnetKey(String subnetId, String account, String region) {
    "${ID}:${Namespace.SUBNETS}:${account}:${region}:${subnetId}"
  }
}
