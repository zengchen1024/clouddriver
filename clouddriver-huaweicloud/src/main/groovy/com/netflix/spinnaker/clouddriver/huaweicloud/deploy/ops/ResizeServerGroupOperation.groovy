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

import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroupUpdate
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.ResizeServerGroupDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation

class ResizeServerGroupOperation implements AtomicOperation<Void> {

  private final String BASE_PHASE = "RESIZE_SERVER_GROUP"
  ResizeServerGroupDescription description

  ResizeServerGroupOperation(ResizeServerGroupDescription description) {
    this.description = description
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
    TaskAware.task.updateStatus BASE_PHASE, "Resizing server group=${description.serverGroupName} in region=${description.region}..."

    def cloudClient = description.credentials.cloudClient

    def group = cloudClient.getScalingGroup(description.region, description.serverGroupId)
    if (group.minInstanceNumber == description.capacity.min
      && group.maxInstanceNumber == description.capacity.max
      && group.desireInstanceNumber == description.capacity.desired) {
      return
    }

    ASAutoScalingGroupUpdate params = ASAutoScalingGroupUpdate.builder()
      .desireInstanceNumber(description.capacity.desired)
      .minInstanceNumber(description.capacity.min)
      .maxInstanceNumber(description.capacity.max)
      .build()

    cloudClient.updateScalingGroup(
      description.region, description.serverGroupId, params
    )

    TaskAware.task.updateStatus BASE_PHASE, "Waiting for resizing server group=${description.serverGroupName} to be done."

    Boolean result = AsyncWait.asyncWait(-1, {
      try {
        group = cloudClient.getScalingGroup(description.region, description.serverGroupId)
        if (!group) {
          return AsyncWait.AsyncWaitStatus.UNKNOWN
        }

        return group.currentInstanceNumber == group.desireInstanceNumber ?
          AsyncWait.AsyncWaitStatus.SUCCESS : AsyncWait.AsyncWaitStatus.PENDING

      } catch (Exception) {
        return AsyncWait.AsyncWaitStatus.UNKNOWN
      }
    })

    TaskAware.task.updateStatus BASE_PHASE, "Finished resizing server group=${description.serverGroupName}, ${result ? "add succeed" : "but failed"}."
    return
  }
}
