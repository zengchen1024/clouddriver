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

import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroup
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingResourceTag
import com.huawei.openstack4j.openstack.scaling.domain.LBPool
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.CloneServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.DeployServerGroupDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import groovy.util.logging.Slf4j

@Slf4j
class CloneServerGroupOperation implements AtomicOperation<DeploymentResult> {
  private final String BASE_PHASE = "CLONE_SERVER_GROUP"

  CloneServerGroupDescription description

  CloneServerGroupOperation(CloneServerGroupDescription description) {
    this.description = description
  }

  /*
   curl -X POST -H "Content-Type: application/json" -d '[{
     "cloneServerGroup": {
       "serverGroupName": "drmaastestapp-drmaasteststack-v000",
       "serverGroupId": "",
       "region": "region",
     }
   }]' localhost:7002/huaweicloud/ops
  */
  @Override
  DeploymentResult operate(List priorOutputs) {
    TaskAware.task.updateStatus BASE_PHASE, "Cloning server group=${description.source.serverGroupName} in region=${description.region}..."

    if (!description.serverGroupParameters) {
      buildDeployServerGroupDescription()
    }

    DeploymentResult result = (new DeployServerGroupOperation(description, BASE_PHASE)).operate(priorOutputs)

    TaskAware.task.updateStatus BASE_PHASE, "Finished cloning server group=${description.source.serverGroupName}."

    result
  }

  private Void buildDeployServerGroupDescription() {
    def cloudClient = description.credentials.cloudClient
    def source = description.source

    List<? extends ScalingGroup> groups = cloudClient.getScalingGroups(description.region, source.serverGroupName)
    if (!(groups.asBoolean() && groups.size() == 1)) {
      throw new OperationException(BASE_PHASE, "there are zero or more than one server groups with name ${source.serverGroupName}")
    }

    ASAutoScalingGroup sourceGroup = groups[0] as ASAutoScalingGroup

    List<ASAutoScalingResourceTag> tags = cloudClient.getScalingGroupTags(description.region, sourceGroup.groupId)

    CloneServerGroupDescription.Capacity capacity = description.capacity
    if (!capacity) {
      capacity = new CloneServerGroupDescription.Capacity(0, 0, 0)
    }
    Boolean useSourceCapacity = description.useSourceCapacity

    description.serverGroupParameters = new DeployServerGroupDescription.ServerGroupParameters(
      maxSize: (useSourceCapacity ? sourceGroup.maxInstanceNumber : capacity.max) as Integer,
      minSize: (useSourceCapacity ? sourceGroup.minInstanceNumber : capacity.min) as Integer,
      desiredSize: (useSourceCapacity ? sourceGroup.desireInstanceNumber : capacity.desired) as Integer,

      serverGroupConfigId: sourceGroup.configId,

      zones: sourceGroup.availabilityZones,
      multiAZPriorityPolicy: sourceGroup.multiAZPriorityPolicy,

      subnets: sourceGroup.networks.collect { it.id }?.toSet(),
      vpcId: sourceGroup.vpcId,

      healthCheckWay: sourceGroup.healthPeriodicAuditMethod,
      healthCheckInterval: sourceGroup.healthPeriodicAuditTime,
      healthCheckGracePeriod: sourceGroup.healthPeriodicAuditGracePeriod,

      instanceRemovePolicy: sourceGroup.instanceTerminatePolicy,
      deleteEIP: sourceGroup.deletePublicip,

      loadBalancers: sourceGroup.lbPools?.collect { LBPool lbPool ->
        new DeployServerGroupDescription.ServerGroupParameters.LoadBalancerInfo(
          loadBalancerPoolId: lbPool.poolId,
          backendPort: lbPool.protocolPort,
          weight: lbPool.weight
        )
      },

      tags: tags ? tags.collectEntries {[(it.key): it.value]} : null,
    )
    return
  }
}
