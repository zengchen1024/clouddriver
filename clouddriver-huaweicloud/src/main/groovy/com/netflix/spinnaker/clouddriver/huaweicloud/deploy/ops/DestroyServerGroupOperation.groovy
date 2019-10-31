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
import com.huawei.openstack4j.model.scaling.ScalingGroup.ScalingGroupStatus
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroup
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.ServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.ResizeServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.servergroup.ServerGroupOperationUtils
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import groovy.util.logging.Slf4j

@Slf4j
class DestroyServerGroupOperation implements AtomicOperation<Void> {
  private final String BASE_PHASE = "DESTROY_SERVER_GROUP"
  ServerGroupDescription description

  DestroyServerGroupOperation(ServerGroupDescription description) {
    this.description = description
  }

  /*
   curl -X POST -H "Content-Type: application/json" -d '[{
     "destroyServerGroup": {
       "serverGroupName": "drmaastestapp-drmaasteststack-v000",
       "serverGroupId": "",
       "region": "region",
     }
   }]' localhost:7002/huaweicloud/ops
  */
  @Override
  Void operate(List priorOutputs) {
    String serverGroupName = description.serverGroupName
    String region = description.region

    TaskAware.task.updateStatus BASE_PHASE, "Destroying server group=${serverGroupName} in region=${region}..."

    def cloudClient = description.credentials.cloudClient

    ASAutoScalingGroup group = ServerGroupOperationUtils.findScalingGroup(
      serverGroupName, description.serverGroupId, cloudClient, region, BASE_PHASE)

    // if the server group is disabled, then remove all instances, otherwise it will be failed to destroy sg.
    // TODO invoke force deleting auto scaling instead.
    if (group.groupStatus == ScalingGroupStatus.PAUSED) {
      ResizeServerGroupDescription resizeDescription = new ResizeServerGroupDescription(
        capacity: new ResizeServerGroupDescription.Capacity(0, 0, 0),
        region: region,
        serverGroupId: group.groupId,
        serverGroupName: serverGroupName,
        credentials: description.credentials,
      )

      (new ResizeServerGroupOperation(resizeDescription, BASE_PHASE)).operate(priorOutputs)
    }

    ActionResponse result = cloudClient.deleteScalingGroup(region, group.groupId)

    if (!result.isSuccess()) {
      // if there are instances attached to this server group, it will fail.
      throw new OperationException(result, BASE_PHASE)
    }

    TaskAware.task.updateStatus BASE_PHASE, "Finished destroying server group=${serverGroupName}."
    return
  }
}
