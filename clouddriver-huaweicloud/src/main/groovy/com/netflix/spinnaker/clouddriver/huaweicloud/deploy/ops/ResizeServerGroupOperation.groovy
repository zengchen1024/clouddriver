/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.huawei.openstack4j.model.common.ActionResponse
import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.huawei.openstack4j.model.scaling.ScalingGroupInstance
import com.huawei.openstack4j.model.scaling.ScalingGroup.ScalingGroupStatus
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroup
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroupUpdate
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.ResizeServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.servergroup.ServerGroupOperationUtils
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation

class ResizeServerGroupOperation implements AtomicOperation<Void> {

  private final String BASE_PHASE = "RESIZE_SERVER_GROUP"
  ResizeServerGroupDescription description

  ResizeServerGroupOperation(ResizeServerGroupDescription description) {
    this.description = description
  }

  ResizeServerGroupOperation(ResizeServerGroupDescription description, String basePhase) {
    this.description = description
    this.BASE_PHASE = basePhase
  }

  /*
   curl -X POST -H "Content-Type: application/json" -d '[{
     "resizeServerGroup": {
       "serverGroupName": "myapp-teststack-v000",
       "serverGroupId": "",
       "capacity": {
         "min": 1,
         "desired": 2,
         "max": 3
       },
       "region": "REGION1"
     }
   }]' localhost:7002/huaweicloud/ops
  */
  @Override
  Void operate(List priorOutputs) {
    String serverGroupName = description.serverGroupName
    String region = description.region

    TaskAware.task.updateStatus BASE_PHASE, "Resizing server group=${serverGroupName} in region=${region}..."

    def cloudClient = description.credentials.cloudClient

    ASAutoScalingGroup group = ServerGroupOperationUtils.findScalingGroup(
      serverGroupName, description.serverGroupId, cloudClient, region, BASE_PHASE)

    ResizeServerGroupDescription.Capacity newCapacity = description.capacity

    if (group.minInstanceNumber == newCapacity.min
      && group.maxInstanceNumber == newCapacity.max
      && group.desireInstanceNumber == newCapacity.desired
      && group.currentInstanceNumber == newCapacity.desired) {
      return
    }

    if (group.groupStatus == ScalingGroupStatus.PAUSED) {
      if (newCapacity.desired || newCapacity.min || newCapacity.max) {
        throw new OperationException(
          BASE_PHASE,
          "can't resize server group=${serverGroupName} when it is disabled and the expect capacity is not 0")
      }
    }

    resizeServerGroup(group, cloudClient, region)
  }

  private Void resizeServerGroup(ScalingGroup group, HuaweiCloudClient cloudClient, String region) {
    String groupId = group.groupId
    String serverGroupName = description.serverGroupName

    TaskAware.task.updateStatus BASE_PHASE, "starting to resize server group=${serverGroupName}."

    ResizeServerGroupDescription.Capacity newCapacity = description.capacity

    if (!(group.minInstanceNumber == newCapacity.min
      && group.maxInstanceNumber == newCapacity.max
      && group.desireInstanceNumber == newCapacity.desired)) {

      ASAutoScalingGroupUpdate params = ASAutoScalingGroupUpdate.builder()
        .desireInstanceNumber(newCapacity.desired)
        .minInstanceNumber(newCapacity.min)
        .maxInstanceNumber(newCapacity.max)
        .build()

      cloudClient.updateScalingGroup(region, groupId, params)
    }

    if (group.groupStatus == ScalingGroupStatus.PAUSED) {
      removeAllInstances(groupId, cloudClient, region)
    }

    TaskAware.task.updateStatus BASE_PHASE, "Waiting for resizing server group=${serverGroupName} to be done."

    Boolean result = AsyncWait.asyncWait(-1, {
      try {
        group = cloudClient.getScalingGroup(region, groupId)
        if (!group) {
          return AsyncWait.AsyncWaitStatus.UNKNOWN
        }

        return group.currentInstanceNumber == group.desireInstanceNumber ?
          AsyncWait.AsyncWaitStatus.SUCCESS : AsyncWait.AsyncWaitStatus.PENDING

      } catch (Exception) {
        return AsyncWait.AsyncWaitStatus.UNKNOWN
      }
    })

    TaskAware.task.updateStatus BASE_PHASE, "Finished resizing server group=${serverGroupName}, ${result ? "add succeed" : "but failed"}."
    return
  }

  private Void removeAllInstances(String groupId, HuaweiCloudClient cloudClient, String region) {
    List<? extends ScalingGroupInstance> instances = cloudClient.getScalingGroupInstances(region, groupId)

    List<String> instanceIds = instances?.collect { ScalingGroupInstance instance ->
      instance.instanceId
    }.findAll()

    Integer end = instanceIds.size()
    while (end > 0) {
      Integer start = end - 10
      if (start < 0) {
        start = 0
      }

      ActionResponse result = cloudClient.removeInstancesFromAS(region, groupId, instanceIds.subList(start, end))
      if (!result.isSuccess()) {
        throw new OperationException(result, BASE_PHASE)
      }

      end = start
    }

    return
  }
}
