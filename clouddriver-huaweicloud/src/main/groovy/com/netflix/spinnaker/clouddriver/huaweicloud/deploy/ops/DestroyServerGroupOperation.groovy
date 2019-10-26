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
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.ServerGroupDescription
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
    TaskAware.task.updateStatus BASE_PHASE, "Destroying server group=${description.serverGroupName} in region=${description.region}..."

    def cloudClient = description.credentials.cloudClient
    String serverGroupId = description.serverGroupId

    if (!serverGroupId) {
      List<? extends ScalingGroup> groups = cloudClient.getScalingGroups(description.region, description.serverGroupName)
      if (!(groups.asBoolean() && groups.size() == 1)) {
        throw new OperationException(BASE_PHASE, "there are zero or more than one server groups with name ${description.serverGroupName}")
      }

      serverGroupId = groups[0].groupId
    }

    ActionResponse result = cloudClient.deleteScalingGroup(description.region, serverGroupId)

    if (!result.isSuccess()) {
      // if there are instances attached to this server group, it will fail.
      throw new OperationException(result, BASE_PHASE)
    }

    TaskAware.task.updateStatus BASE_PHASE, "Finished destroying server group=${description.serverGroupName}."
    return
  }
}
