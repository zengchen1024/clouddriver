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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.huawei.openstack4j.openstack.common.IdResourceEntity
import com.huawei.openstack4j.model.scaling.ScalingGroup.HealthPeriodicAuditMethod
import com.huawei.openstack4j.model.scaling.ScalingGroup.InstanceTerminatePolicy
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroupCreate
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingResourceTag
import com.huawei.openstack4j.openstack.scaling.domain.LBPool
import com.huawei.openstack4j.openstack.scaling.domain.MultiAZPriorityPolicy
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.DeployServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.servergroup.ServerGroupNameResolver
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations

class DeployServerGroupOperation implements AtomicOperation<DeploymentResult> {

  private final String BASE_PHASE = "DEPLOY_SERVER_GROUP"

  DeployServerGroupDescription description

  DeployServerGroupOperation(DeployServerGroupDescription description) {
    this.description = description
  }

  DeployServerGroupOperation(DeployServerGroupDescription description, basePhase) {
    this.description = description
    this.BASE_PHASE = basePhase
  }

  /*
   curl -X POST -H "Content-Type: application/json" -d '[{
    "createServerGroup": {
      "stack": "teststack",
      "application": "myapp",
      "serverGroupParameters": {
        "scalingConfig": {
          "configId": "",
          "configName": "",
          "instanceType": "m1.medium",
          "image": "4e0d0b4b-8089-4703-af99-b6a0c90fbbc7",
          "securityGroups": ["e56fa7eb-550d-42d4-8d3f-f658fbacd496"],
        }
        "maxSize": 5,
        "minSize": 3,
        "desiredSize": 4,
        "subnetId": "77bb3aeb-c1e2-4ce5-8d8f-b8e9128af651",
        "floatingNetworkId: "99bb3aeb-c1e2-4ce5-8d8f-b8e9128af699",
        "loadBalancers": ["87077f97-83e7-4ea1-9ca9-40dc691846db"],
        "scaleup": {
          "cooldown": 60,
          "adjustment": 1,
          "period": 60,
          "threshold": 50
        },
        "scaledown": {
          "cooldown": 60,
          "adjustment": -1,
          "period": 600,
          "threshold": 15
        },
        "tags": {
          "foo": "bar",
          "bar": "foo"
        }
      },
      "userDataType": "URL",
      "userData": "http://foobar.com",
      "region": "REGION1",
      "disableRollback": false,
      "timeoutMins": 5,
      "account": "test"
    }
   }]' localhost:7002/huaweicloud/ops
  */
  @Override
  DeploymentResult operate(List priorOutputs) {
    def task = TaskAware.task

    task.updateStatus BASE_PHASE, "Initializing creation of server group..."

    task.updateStatus BASE_PHASE, "Building server group name"

    def nameResolver = new ServerGroupNameResolver(cloudClient, region, BASE_PHASE)

    def groupName = nameResolver.resolveNextServerGroupName(
      description.application, description.stack, description.freeFormDetails, false)

    task.updateStatus BASE_PHASE, "Creating server group=${groupName}"

    String groupId = createScalingGroup(groupName)

    task.updateStatus BASE_PHASE, "Creating tags for server group=${groupName}"

    createScalingGroupTags(groupId)

    cloudClient.enableScalingGroup(region, groupId)

    task.updateStatus BASE_PHASE, "Waiting for server group=${groupName} to be created"
    Boolean result = waitForCreatingScalingGroup(groupId)

    task.updateStatus BASE_PHASE, "Finished creating server group=${groupName}, ${result ? "add succeed" : "but failed"}"

    new DeploymentResult(
      serverGroupNames: ["${region}:${groupName}".toString()], // must set as region:groupName, otherwise orca will be stuck at WaitForUpInstancesTask
      serverGroupNameByRegion: [(region): groupName]
    )
  }

  /*
  private String createScalingConfig() {
    def scalingConfig = description.serverGroupParameters.scalingConfig

    if (scalingConfig.configId) {
      return scalingConfig.configId
    }

    def instanceConfig = new ASAutoScalingInstanceConfig(
      flavorRef: scalingConfig.instanceType,
      imageRef: scalingConfig.image,
      securityGroups: scalingConfig.securityGroups.collect { new ListItem(it) },
      // disk, etc
      // user data inject
    )

    ASAutoScalingConfigCreate config = ASAutoScalingConfigCreate.builder().
      .configName(scalingConfig.configName)
      .instanceConfig(instanceConfig)
      .build()

    return cloudClient.createScalingConfig(region, config)
  }*/

  private String createScalingGroup(String serverGroupName) {
    def input = description.serverGroupParameters

    ASAutoScalingGroupCreate params = ASAutoScalingGroupCreate.builder()
      .groupName(serverGroupName)
      .configId(input.serverGroupConfigId)
      .desireInstanceNumber(input.desiredSize)
      .minInstanceNumber(input.minSize)
      .maxInstanceNumber(input.maxSize)
      .lbaasListeners(input.loadBalancers?.collect {
        new LBPool(
          poolId: it.loadBalancerPoolId,
          protocolPort: it.backendPort,
          weight: it.weight
        )
      })
      .availabilityZones(input.zones)
      .networks(input.subnets.collect { new IdResourceEntity(it) })
      .vpcId(input.vpcId)
      .healthPeriodicAuditMethod(HealthPeriodicAuditMethod.valueOf(input.healthCheckWay))
      .healthPeriodicAuditTime(input.healthCheckInterval)
      .healthPeriodicAuditGracePeriod(input.healthCheckGracePeriod)
      .multiAZPriorityPolicy(MultiAZPriorityPolicy.valueOf(input.multiAZPriorityPolicy))
      .instanceTerminatePolicy(InstanceTerminatePolicy.valueOf(input.instanceRemovePolicy))
      .deletePublicip(input.deleteEIP)
      .build()

    return cloudClient.createScalingGroup(region, params)
  }

  private Boolean waitForCreatingScalingGroup(String groupId) {
    return AsyncWait.asyncWait(-1, {
      try {
        def group = cloudClient.getScalingGroup(region, groupId)
        if (!group) {
          return AsyncWait.AsyncWaitStatus.UNKNOWN
        }

        return group.currentInstanceNumber == group.desireInstanceNumber ?
          AsyncWait.AsyncWaitStatus.SUCCESS : AsyncWait.AsyncWaitStatus.PENDING

      } catch (Exception) {
        return AsyncWait.AsyncWaitStatus.UNKNOWN
      }
    })
  }

  private Void createScalingGroupTags(String groupId) {
    def input = description.serverGroupParameters
    if (!input.tags) {
      return
    }

    List<ASAutoScalingResourceTag> tags = input.tags.collect {
      ASAutoScalingResourceTag.builder()
        .key(it.key)
        .value(it.value)
        .build()
    }

    cloudClient.createScalingGroupTags(region, groupId, tags)
    return
  }

  /*
  private String getUserData(String serverGroupName) {
  }
  */

  private HuaweiCloudClient getCloudClient() {
    description.credentials.cloudClient
  }

  private String getRegion() {
    description.region
  }
}
