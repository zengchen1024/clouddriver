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
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.huawei.openstack4j.openstack.ims.v2.domain.Image
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingInstanceConfig
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroup
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.model.HealthState
import com.netflix.spinnaker.clouddriver.model.Instance
import com.netflix.spinnaker.clouddriver.model.ServerGroup
import com.netflix.spinnaker.clouddriver.model.ServerGroup.Capacity
import com.netflix.spinnaker.clouddriver.model.ServerGroup.ImageSummary
import com.netflix.spinnaker.clouddriver.model.ServerGroup.ImagesSummary
import com.netflix.spinnaker.clouddriver.model.ServerGroup.InstanceCounts
import groovy.transform.Canonical

@Canonical
class HuaweiCloudServerGroup {
  String account
  String region
  ASAutoScalingGroup scalingGroup
  ASAutoScalingInstanceConfig config
  Map<String, HuaweiCloudInstance> instances // id -> instance
  Map<String, String> loadBalancers          // id -> name
  Map<String, String> securityGroups         // id -> name
  Image image

  @JsonIgnore
  View getView() {
    new View()
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Canonical
  class View implements ServerGroup {
    final String type = HuaweiCloudProvider.ID
    final String cloudProvider = HuaweiCloudProvider.ID

    String account = HuaweiCloudServerGroup.this.account
    String region = HuaweiCloudServerGroup.this.region

    Set<String> loadBalancers = HuaweiCloudServerGroup.this.loadBalancers?.values()
    Set<String> securityGroups = HuaweiCloudServerGroup.this.securityGroups?.values()

    String name = HuaweiCloudServerGroup.this.scalingGroup.groupName
    String vpcId = HuaweiCloudServerGroup.this.scalingGroup.vpcId
    Set<String> subnets = HuaweiCloudServerGroup.this.scalingGroup.networks.collect { it.id }?.toSet()
    Set<String> zones = HuaweiCloudServerGroup.this.scalingGroup.availabilityZones

    Map<String, String> tags = null // it seems no tags

    Set<? extends Instance> Instances = HuaweiCloudServerGroup.this.instances.collect { it.value.view }?.toSet()

    @Override
    Boolean isDisabled() {
      HuaweiCloudServerGroup.this.scalingGroup.groupStatus != ScalingGroup.ScalingGroupStatus.INSERVICE
    }

    @Override
    Long getCreatedTime() {
      HuaweiCloudServerGroup.this.scalingGroup.createTime?.time
    }

    @Override
    Map<String, Object> getLaunchConfig() {
      def config = HuaweiCloudServerGroup.this.config

      Map<String, Object> result = [:]
      result["instanceType"] = config.flavorRef
      result["securityGroups"] = config.securityGroups.collect { it.id }?.toSet()
      result["imageId"] = config.imageRef

      result
    }

    @Override
    Capacity getCapacity() {
      def scalingGroup = HuaweiCloudServerGroup.this.scalingGroup

      new Capacity(
        min: scalingGroup.minInstanceNumber ? scalingGroup.minInstanceNumber as Integer : 0,
        max: scalingGroup.maxInstanceNumber ? scalingGroup.maxInstanceNumber as Integer : 0,
        desired: scalingGroup.desireInstanceNumber ? scalingGroup.desireInstanceNumber as Integer : 0
      )
    }

    @Override
    InstanceCounts getInstanceCounts() {
      Set<HealthState> allStates = this.getInstances().collect { it.getHealthState() }

      new InstanceCounts(
        total: allStates ? allStates.size() : 0,
        up: calInstancesNumByHealthState(allStates, HealthState.Up) ?: 0,
        down: calInstancesNumByHealthState(allStates, HealthState.Down) ?: 0,
        unknown: calInstancesNumByHealthState(allStates, HealthState.Unknown) ?: 0,
        starting: calInstancesNumByHealthState(allStates, HealthState.Starting) ?: 0,
        outOfService: calInstancesNumByHealthState(allStates, HealthState.OutOfService) ?: 0)
    }

    static Integer calInstancesNumByHealthState(Set<HealthState> allStates, HealthState healthState) {
      allStates.findAll { it == healthState }?.size()
    }

    @Override
    ImagesSummary getImagesSummary() {
      def image = HuaweiCloudServerGroup.this.image
      ObjectMapper mapper = new ObjectMapper()

      new ImagesSummaryImpl(
        summaries: [
          new ImageSummaryImpl(
            serverGroupName: this.name,
            imageName: image.name,
            imageId: image.id,
            image: mapper.convertValue(it, new TypeReference<Map<String, Object>>() {})
          )
        ]
      )
    }

    @Override
    ImageSummary getImageSummary() {
      imagesSummary?.summaries?.getAt(0)
    }

    static class ImageSummaryImpl implements ImageSummary {
      String serverGroupName
      String imageId
      String imageName
      Map<String, Object> image
      Map<String, Object> buildInfo
    }

    static class ImagesSummaryImpl implements ImagesSummary {
      List<ImageSummary> summaries
    }
  }
}
