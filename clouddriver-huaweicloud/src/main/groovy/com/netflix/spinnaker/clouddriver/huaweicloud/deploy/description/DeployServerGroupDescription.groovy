/*
 * Copyright 2016 Veritas Technologies LLC.
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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description

import com.netflix.spinnaker.clouddriver.deploy.DeployDescription
import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class DeployServerGroupDescription extends AbstractHuaweiCloudCredentialsDescription implements DeployDescription {
  String application
  String stack
  String freeFormDetails
  String region

  ServerGroupParameters serverGroupParameters

  @AutoClone
  @Canonical
  static class ServerGroupParameters {
    Integer maxSize
    Integer minSize
    Integer desiredSize

    String serverGroupConfigId

    List<String> zones
    String multiAZPriorityPolicy

    Set<String> subnets
    String vpcId

    String healthCheckWay
    Integer healthCheckInterval
    Integer healthCheckGracePeriod

    String instanceRemovePolicy
    Boolean deleteEIP

    List<LoadBalancerInfo> loadBalancers

    Map<String, String> tags

    @AutoClone
    @Canonical
    static class LoadBalancerInfo {
      String loadBalancerId
      String loadBalancerPoolId
      Integer backendPort
      Integer weight
    }
  }
}


