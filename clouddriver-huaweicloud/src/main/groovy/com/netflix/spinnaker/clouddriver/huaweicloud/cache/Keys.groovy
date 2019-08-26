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
    ELASTIC_IPS,
    IMAGES,
    SECURITY_GROUPS,
    SERVER_GROUPS,
    INSTANCE_TYPES,
    INSTANCES,
    LOAD_BALANCERS,
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
      case Namespace.ELASTIC_IPS.ns:
        if (parts.length == 5) {
          result << [
            account: parts[2],
            region: parts[3],
            id: parts[4]]
        }
        break
      case Namespace.IMAGES.ns:
        if (parts.length == 5) {
          result << [
            account: parts[2],
            region: parts[3],
            id: parts[4]]
        }
        break
      case Namespace.SECURITY_GROUPS.ns:
        if (parts.length == 6) {
          def names = Names.parseName(parts[4])
          result << [
            account: parts[2],
            region: parts[3],
            application: names.app,
            name: parts[4],
            id: parts[5]]
        }
        break
      case Namespace.SERVER_GROUPS.ns:
        def names = Names.parseName(parts[4])
        if (parts.length == 5) {
          result << [
            account: parts[2],
            region: parts[3],
            application: names.app.toLowerCase(),
            cluster: names.cluster,
            name: parts[4]]
        }
        break
      case Namespace.INSTANCE_TYPES.ns:
        if (parts.length == 5) {
          result << [
            account: parts[2],
            region: parts[3],
            id: parts[4]]
        }
        break
      case Namespace.INSTANCES.ns:
        if (parts.length == 5) {
          result << [
            account: parts[2],
            region: parts[3],
            id: parts[4]]
        }
        break
      case Namespace.LOAD_BALANCERS.ns:
        if (parts.length == 6) {
          result << [
            account: parts[2],
            region: parts[3],
            id: parts[4],
            name: parts[5]]
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

  static String getElasticIPKey(String ipId, String account, String region) {
    "${ID}:${Namespace.ELASTIC_IPS}:${account}:${region}:${ipId}"
  }

  static String getImageKey(String imageId, String account, String region) {
    "${ID}:${Namespace.IMAGES}:${account}:${region}:${imageId}"
  }

  static String getSecurityGroupKey(String securityGroupName, String securityGroupId, String account, String region) {
    "${ID}:${Namespace.SECURITY_GROUPS}:${account}:${region}:${securityGroupName}:${securityGroupId}"
  }

  static String getServerGroupKey(String serverGroupName, String account, String region) {
    "${ID}:${Namespace.SERVER_GROUPS}:${account}:${region}:${serverGroupName}"
  }

  static String getInstanceTypeKey(String instanceType, String account, String region) {
    "${ID}:${Namespace.INSTANCE_TYPES}:${account}:${region}:${instanceType}"
  }

  static String getInstanceKey(String instanceId, String account, String region) {
    "${ID}:${Namespace.INSTANCES}:${account}:${region}:${instanceId}"
  }

  static String getLoadBalancerKey(String loadBalancerName, String loadBalancerId, String account, String region) {
    "${ID}:${Namespace.LOAD_BALANCERS}:${account}:${region}:${loadBalancerId}:${loadBalancerName}"
  }
}
