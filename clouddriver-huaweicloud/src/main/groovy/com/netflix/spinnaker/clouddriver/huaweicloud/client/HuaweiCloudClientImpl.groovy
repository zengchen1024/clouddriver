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

package com.netflix.spinnaker.clouddriver.huaweicloud.client

import com.huawei.openstack4j.api.OSClient
import com.huawei.openstack4j.model.common.ActionResponse
import com.huawei.openstack4j.model.compute.ext.AvailabilityZone
import com.huawei.openstack4j.model.compute.RebootType
import com.huawei.openstack4j.model.network.ext.LbPoolV2
import com.huawei.openstack4j.model.network.ext.LoadBalancerV2
import com.huawei.openstack4j.model.network.ext.LoadBalancerV2StatusTree
import com.huawei.openstack4j.model.network.ext.MemberV2
import com.huawei.openstack4j.model.scaling.ScalingConfig
import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.huawei.openstack4j.model.scaling.ScalingGroupCreate
import com.huawei.openstack4j.model.scaling.ScalingGroupInstance
import com.huawei.openstack4j.openstack.ecs.v1.domain.CloudServer
import com.huawei.openstack4j.openstack.ecs.v1.domain.Flavor
import com.huawei.openstack4j.openstack.ecs.v1.domain.InterfaceAttachment
import com.huawei.openstack4j.openstack.ims.v2.domain.Image
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingGroupUpdate
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingResourceTag
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingResourceType
import com.huawei.openstack4j.openstack.scaling.domain.ASAutoScalingTagActionType
import com.huawei.openstack4j.openstack.vpc.v1.domain.PublicIp
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroup
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroupCreate
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroupRule
import com.huawei.openstack4j.openstack.vpc.v1.domain.Subnet
import com.huawei.openstack4j.openstack.vpc.v1.domain.Vpc

class HuaweiCloudClientImpl implements HuaweiCloudClient {
  final AuthorizedClientProvider provider

  HuaweiCloudClientImpl(AuthorizedClientProvider provider) {
    this.provider = provider
  }

  private OSClient getRegionClient(String region) {
    this.provider.authClient.useRegion(region)
  }

  @Override
  void rebootInstance(String region, String instanceId) {
    getRegionClient(region).ecs().servers().reboot([instanceId], RebootType.SOFT)
  }

  @Override
  List<Vpc> listVpcs(String region) {
    getRegionClient(region).vpc().vpcs().list()
  }

  @Override
  List<Subnet> listSubnets(String region) {
    getRegionClient(region).vpc().subnets().list()
  }

  @Override
  List<PublicIp> listElasticIps(String region) {
    getRegionClient(region).vpc().publicips().list()
  }

  @Override
  ActionResponse enableScalingGroup(String region, String groupId) {
    getRegionClient(region).autoScaling().groups().resume(groupId)
  }

  @Override
  ActionResponse disableScalingGroup(String region, String groupId) {
    getRegionClient(region).autoScaling().groups().pause(groupId)
  }

  @Override
  ActionResponse deleteScalingGroup(String region, String groupId) {
    getRegionClient(region).autoScaling().groups().delete(groupId)
  }

  @Override
  String createScalingGroup(String region, ScalingGroupCreate params) {
    getRegionClient(region).autoScaling().groups().create(params)
  }

  @Override
  ScalingGroup getScalingGroup(String region, String groupId) {
    getRegionClient(region).autoScaling().groups().get(groupId)
  }

  @Override
  List<? extends ScalingGroup> getScalingGroups(String region) {
    getRegionClient(region).autoScaling().groups().list()
  }

  @Override
  String updateScalingGroup(String region, String groupId, ASAutoScalingGroupUpdate params) {
    getRegionClient(region).autoScaling().groups().update(groupId, params)
  }

  @Override
  ActionResponse createScalingGroupTags(String region, String groupId, List<ASAutoScalingResourceTag> tags) {
    getRegionClient(region).autoScaling().tags().updateOrDelete(
      ASAutoScalingResourceType.scaling_group_tag, groupId,
      ASAutoScalingResourceTag.ASAutoScalingResourceTags.builder()
        .tags(tags)
        .action(ASAutoScalingTagActionType.create)
        .build()
    )
  }

  @Override
  List<ASAutoScalingResourceTag> getScalingGroupTags(String region, String groupId) {
    ASAutoScalingResourceTag.ASAutoScalingResourceTags result = null
    result = getRegionClient(region).autoScaling().tags().get(
      ASAutoScalingResourceType.scaling_group_tag, groupId)

    result ? result.tags : null
  }

  @Override
  List<? extends ScalingGroupInstance> getScalingGroupInstances(String region, String groupId) {
    getRegionClient(region).autoScaling().groupInstances().list(groupId)
  }

  @Override
  List<? extends ScalingConfig> getScalingConfigs(String region) {
    getRegionClient(region).autoScaling().configs().list()
  }

  @Override
  ScalingConfig getScalingConfig(String region, String configId) {
    getRegionClient(region).autoScaling().configs().get(configId)
  }

  @Override
  List<Image> getImages(String region) {
    getRegionClient(region).imsV2().images().list(
      [
        "__imagetype": "gold",
        "status": "active",
        "virtual_env_type": "FusionCompute"
      ]
    )
  }

  @Override
  Image getImage(String region, String imageId) {
    getRegionClient(region).imsV2().images().get(imageId)
  }

  @Override
  List<CloudServer> getInstances(String region) {
    getRegionClient(region).ecs().servers().list()
  }

  @Override
  CloudServer getInstance(String region, String instanceId) {
    getRegionClient(region).ecs().servers().get(instanceId)
  }

  @Override
  List<InterfaceAttachment> getInstanceNics(String region, String instanceId) {
    getRegionClient(region).ecs().servers().getInterfaces(instanceId)
  }

  @Override
  List<Flavor> getInstanceTypes(String region, String az) {
    getRegionClient(region).ecs().servers().getSpecifications(az)
  }

  @Override
  List<? extends AvailabilityZone> getZones(String region) {
    getRegionClient(region).compute().zones().list()
  }

  @Override
  List<SecurityGroup> getSecurityGroups(String region) {
    getRegionClient(region).vpc().securityGroups().list()
  }

  @Override
  SecurityGroup createSecurityGroup(String region, String name, String vpcId) {
    getRegionClient(region).vpc().securityGroups().create(
      SecurityGroupCreate.builder()
        .name(name).vpcId(vpcId).build()
    )
  }

  @Override
  SecurityGroup getSecurityGroup(String region, String groupId) {
    getRegionClient(region).vpc().securityGroups().get(groupId)
  }

  @Override
  ActionResponse deleteSecurityGroup(String region, String groupId) {
    getRegionClient(region).vpc().securityGroups().delete(groupId)
  }

  @Override
  ActionResponse deleteSecurityGroupRule(String region, String ruleId) {
    getRegionClient(region).vpc().securityGroups().deleteSecurityGroupRule(ruleId)
  }

  @Override
  SecurityGroupRule createSecurityGroupRule(String region, SecurityGroupRule rule) {
    getRegionClient(region).vpc().securityGroups().createSecurityGroupRule(rule)
  }

  @Override
  List<? extends LoadBalancerV2> getLoadBalancers(String region) {
    def result = []
    String marker = ""
    Map<String, String> page = ["limit": "20", "marker": ""]
    while(true) {
      page["marker"] = marker
      List<? extends LoadBalancerV2> v = getRegionClient(region).networking().lbaasV2().loadbalancer().list(page)
      if (!v) {
        break
      }
      result << v
      marker = v[-1].id
    }

    result.flatten()
  }

  @Override
  ActionResponse deleteLoadBalancer(String region, String lbid) {
    getRegionClient(region).networking().lbaasV2().loadbalancer().delete(lbid)
  }

  @Override
  LoadBalancerV2 getLoadBalancer(String region, String lbid) {
    getRegionClient(region).networking().lbaasV2().loadbalancer().get(lbid)
  }

  @Override
  LoadBalancerV2StatusTree getLoadBalancerStatusTree(String region, String lbid) {
    getRegionClient(region).networking().lbaasV2().loadbalancer().statusTree(lbid)
  }

  @Override
  List<? extends LbPoolV2> getLoadBalancerPools(String region) {
    getRegionClient(region).networking().lbaasV2().lbPool().list()
  }

  @Override
  LbPoolV2 getLoadBalancerPool(String region, String poolId) {
    getRegionClient(region).networking().lbaasV2().lbPool().get(poolId)
  }

  @Override
  List<? extends MemberV2> getLoadBalancerPoolMembers(String region, String poolId) {
    getRegionClient(region).networking().lbaasV2().lbPool().listMembers(poolId)
  }
}
