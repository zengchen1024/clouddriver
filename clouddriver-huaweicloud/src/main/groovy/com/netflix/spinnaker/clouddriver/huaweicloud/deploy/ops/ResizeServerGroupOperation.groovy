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

    // TODO check if need update

    ASAutoScalingGroupUpdate params = ASAutoScalingGroupUpdate.builder()
      .desireInstanceNumber(description.capacity.desired)
      .minInstanceNumber(description.capacity.min)
      .maxInstanceNumber(description.capacity.max)
      .build()

    description.credentials.cloudClient.updateScalingGroup(
      description.region, description.serverGroupId, params
    )

    TaskAware.task.updateStatus BASE_PHASE, "Finished resizing server group=${description.serverGroupName}."
    // TODO wait for the number of instance to match the desire size
    return
  }
}
