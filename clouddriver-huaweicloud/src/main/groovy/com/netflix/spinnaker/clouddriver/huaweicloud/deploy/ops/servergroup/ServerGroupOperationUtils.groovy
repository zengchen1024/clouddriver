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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.servergroup

import com.huawei.openstack4j.model.network.ext.MemberV2
import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.huawei.openstack4j.model.scaling.ScalingGroupInstance
import com.huawei.openstack4j.openstack.ecs.v1.domain.FixedIp
import com.huawei.openstack4j.openstack.ecs.v1.domain.InterfaceAttachment
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroup
import com.huawei.openstack4j.openstack.scaling.domain.LBPool
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.OperationException

class ServerGroupOperationUtils {

  static ASAutoScalingGroup findScalingGroup(String name, String groupId, HuaweiCloudClient cloudClient, String region, String operation) {
      if (groupId) {
        ScalingGroup group = cloudClient.getScalingGroup(region, groupId)
        if (!group) {
          throw new OperationException(operation, "can't find server group with id ${groupId}")
        }

        return (group as ASAutoScalingGroup)
      }

      List<? extends ScalingGroup> groups = cloudClient.getScalingGroups(region, name)
      if (!groups) {
        throw new OperationException(operation, "can't find server group with name ${name}")
      }
      if (groups.size() != 1) {
        throw new OperationException(operation, "find  more than one server groups with name ${name}")
      }

      return (groups[0] as ASAutoScalingGroup)
  }

  static Map<String, Map<String, MemberV2>> findLBPoolMembers(ASAutoScalingGroup scalingGroup, HuaweiCloudClient cloudClient, String region) {

    scalingGroup.lbPools?.collectEntries { LBPool lbPool ->
       List<? extends MemberV2> members = cloudClient.getLoadBalancerPoolMembers(region, lbPool.poolId)

       [(lbPool.poolId): members?.collectEntries { MemberV2 member ->
        [(member.subnetId + "_" + member.address): member]
       }]
    }?.findAll { it.value }
  }

  static Map<String, Set<String>> findASInstancePorts(ASAutoScalingGroup scalingGroup, HuaweiCloudClient cloudClient, String region) {

    List<? extends ScalingGroupInstance> instances = cloudClient.getScalingGroupInstances(region, scalingGroup.groupId)

    instances?.collectEntries { ScalingGroupInstance instance ->
      // Maybe the instance is being removed from auto scaling, its id will be null
      if (!instance.instanceId) {
        return [(instance.instanceId): null]
      }

      List<InterfaceAttachment> interfaces = cloudClient.getInstanceNics(region, instance.instanceId)

      Set<String> ports = [] as Set
      interfaces?.each { InterfaceAttachment attachment ->
        attachment.fixedIps?.each { FixedIp fixedIp ->
          ports.add(fixedIp.subnetId + "_" + fixedIp.ipAddress)
        }
      }

      [(instance.instanceId): ports]
    }?.findAll { it.value }
  }

  static Map<String, List<String>> findASLBPoolMembers(ASAutoScalingGroup scalingGroup, HuaweiCloudClient cloudClient, String region, String operation) {

    Map<String, Map<String, MemberV2>> poolMembers = findLBPoolMembers(scalingGroup, cloudClient, region)
    if (!poolMembers) {
      return [:]
    }

    Map<String, Set<String>> instancePorts = findASInstancePorts(scalingGroup, cloudClient, region)
    if (!instancePorts) {
      return [:]
    }

    poolMembers.collectEntries {String poolId, Map<String, MemberV2> memberMaps ->

      List<String> memberIds = instancePorts.collect { String instanceId, Set<String> ports ->

        Collection<String> keys = ports.findAll { memberMaps?.containsKey(it) }
        if (!keys) {
          return ""
        }

        if (keys.size() != 1) {
          throw new OperationException(operation, "find more than one members in pool=${poolId} for instance=${instanceId}")
        }

        memberMaps.get(keys[0]).id
      }.findAll()

      [(poolId): memberIds]
    }.findAll { it.value }
  }
}
