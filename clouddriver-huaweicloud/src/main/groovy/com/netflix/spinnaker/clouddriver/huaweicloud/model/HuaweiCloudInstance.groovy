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

package com.netflix.spinnaker.clouddriver.huaweicloud.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.huawei.openstack4j.openstack.networking.domain.ext.NeutronMemberV2
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroupInstance
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.model.health.HuaweiCloudASInstanceHealth
import com.netflix.spinnaker.clouddriver.huaweicloud.model.health.HuaweiCloudLoadBalanceHealth
import com.netflix.spinnaker.clouddriver.model.Health
import com.netflix.spinnaker.clouddriver.model.HealthState
import com.netflix.spinnaker.clouddriver.model.Instance
import groovy.transform.Canonical

@Canonical
class HuaweiCloudInstance {
  String account
  String region
  String zone
  Long launchTime

  ASAutoScalingGroupInstance asInstance
  List<NeutronMemberV2> lbInstances

  @JsonIgnore
  View getView() {
    new View()
  }

  class View implements Instance {
    static final long START_TIME_DRIFT = 180000

    final String providerType = HuaweiCloudProvider.ID
    final String cloudProvider = HuaweiCloudProvider.ID

    String account = HuaweiCloudInstance.this.account
    String region = HuaweiCloudInstance.this.region
    String zone = HuaweiCloudInstance.this.zone
    Long launchTime = HuaweiCloudInstance.this.launchTime

    String name = HuaweiCloudInstance.this.asInstance.instanceName
    String id = HuaweiCloudInstance.this.asInstance.instanceId

    @JsonIgnore
    List<? extends Health> allHealth

    View() {
      this.allHealth = buildAllHealth(
        HuaweiCloudInstance.this.asInstance,
        HuaweiCloudInstance.this.lbInstances)
    }

    @Override
    List<Map<String, Object>> getHealth() {
      ObjectMapper mapper = new ObjectMapper()

      allHealth.collect {
        mapper.convertValue(it, new TypeReference<Map<String, Object>>() {})
      }
    }

    @Override
    HealthState getHealthState() {
      someUpRemainingUnknown(allHealth) ? HealthState.Up :
        anyStarting(allHealth) ? HealthState.Starting :
          anyDown(allHealth) ? HealthState.Down :
            anyOutOfService(allHealth) ? HealthState.OutOfService :
              launchTime > System.currentTimeMillis() - START_TIME_DRIFT ? HealthState.Starting :
                HealthState.Unknown
    }

    private static List<? extends Health> buildAllHealth(ASAutoScalingGroupInstance asInstance,
                                                         List<NeutronMemberV2> lbInstances) {
      List<? extends Health> result = []

      if (asInstance) {
        result << new HuaweiCloudASInstanceHealth(asInstance)
      }

      if (lbInstances) {
        lbInstances.each {
          result << new HuaweiCloudLoadBalanceHealth(it)
        }
      }

      result
    }

    private static boolean someUpRemainingUnknown(List<? extends Health> healthList) {
      List<? extends Health> knownHealthList = healthList.findAll{ it.state != HealthState.Unknown }
      knownHealthList ? knownHealthList.every { it.state == HealthState.Up } : false
    }

    private static boolean anyStarting(List<? extends Health> healthList) {
      healthList.any { it.state == HealthState.Starting}
    }

    private static boolean anyDown(List<? extends Health> healthList) {
      healthList.any { it.state == HealthState.Down}
    }

    private static boolean anyOutOfService(List<? extends Health> healthList) {
      healthList.any { it.state == HealthState.OutOfService}
    }
  }
}
