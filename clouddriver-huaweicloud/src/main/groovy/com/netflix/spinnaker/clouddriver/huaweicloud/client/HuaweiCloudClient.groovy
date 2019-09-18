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

import com.huawei.openstack4j.model.common.ActionResponse
import com.huawei.openstack4j.model.compute.ext.AvailabilityZone
import com.huawei.openstack4j.model.network.ext.LbPoolV2
import com.huawei.openstack4j.model.network.ext.LoadBalancerV2
import com.huawei.openstack4j.model.network.ext.LoadBalancerV2StatusTree
import com.huawei.openstack4j.model.network.ext.MemberV2
import com.huawei.openstack4j.model.scaling.ScalingConfig
import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.huawei.openstack4j.model.scaling.ScalingGroupInstance
import com.huawei.openstack4j.openstack.ecs.v1.domain.CloudServer
import com.huawei.openstack4j.openstack.ecs.v1.domain.Flavor
import com.huawei.openstack4j.openstack.ims.v2.domain.Image
import com.huawei.openstack4j.openstack.ecs.v1.domain.InterfaceAttachment
import com.huawei.openstack4j.openstack.vpc.v1.domain.PublicIp
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroup
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroupCreate
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroupRule
import com.huawei.openstack4j.openstack.vpc.v1.domain.Subnet
import com.huawei.openstack4j.openstack.vpc.v1.domain.Vpc

interface HuaweiCloudClient {
  /**
   * Reboot an instance ... Default to SOFT reboot.
   * @param region
   * @param instanceId
   */
  void rebootInstance(String region, String instanceId)

  /**
   * List the available vpcs in a region.
   * @param region
   * @return
   */
  List<Vpc> listVpcs(String region)

  /**
   * List the available subnets in a region.
   * @param region
   * @return
   */
  List<Subnet> listSubnets(String region)

  /**
   * List elastic ips in a region.
   * @param region
   * @return
   */
  List<PublicIp> listElasticIps(String region)

  /**
   * List security groups in a region
   * @param region
   * @return
   */
  List<SecurityGroup> getSecurityGroups(String region)

  /**
   * List auto scaling groups in a region
   * @param region
   * @return
   */
  List<? extends ScalingGroup> getScalingGroups(String region)

  /**
   * List instances of auto scaling group in a region
   * @param region
   * @param groupId
   * @return
   */
  List<? extends ScalingGroupInstance> getScalingGroupInstances(String region, String groupId)

  /**
   * List auto scaling group config in a region
   * @param region
   * @return
   */
  List<? extends ScalingConfig> getScalingConfigs(String region)

  /**
   * Get an auto scaling group config in a region
   * @param region
   * @param configId
   * @return
   */
  ScalingConfig getScalingConfig(String region, String configId)

  /**
   * List images in a region
   * @param region
   * @return
   */
  List<Image> getImages(String region)

  /**
   * Get an image in a region
   * @param region
   * @param imageId
   * @return
   */
  Image getImage(String region, String imageId)

  /**
   * List elastic compute instances in a region
   * @param region
   * @return
   */
  List<CloudServer> getInstances(String region)

  /**
   * Get an elastic compute instance in a region
   * @param region
   * @param instanceId
   * @return
   */
  CloudServer getInstance(String region, String instanceId)

  /**
   * List nics of an elastic compute instance in a region
   * @param region
   * @param instanceId
   * @return
   */
  List<InterfaceAttachment> getInstanceNics(String region, String instanceId)

  /**
   * List instance types in a region
   * @param region
   * @param az
   * @return
   */
  List<Flavor> getInstanceTypes(String region, String az)

  /**
   * List availability zones in a region
   * @param region
   * @return
   */
  List<? extends AvailabilityZone> getZones(String region)

  /**
   * Create a security group in a region
   * @param region
   * @param name
   * @param vpcId
   * @return
   */
  SecurityGroup createSecurityGroup(String region, String name, String vpcId)

  /**
   * Get a security group in a region
   * @param region
   * @param groupId
   * @return
   */
  SecurityGroup getSecurityGroup(String region, String groupId)

  /**
   * Delete a security group in a region
   * @param region
   * @param groupId
   * @return
   */
  ActionResponse deleteSecurityGroup(String region, String groupId)

  /**
   * Delete a security group rule in a region
   * @param region
   * @param ruleId
   * @return
   */
  ActionResponse deleteSecurityGroupRule(String region, String ruleId)

  /**
   * Create a security group rule in a region
   * @param region
   * @param rule
   * @return
   */
  SecurityGroupRule createSecurityGroupRule(String region, SecurityGroupRule rule)

  /**
   * List load balancers in a region
   * @param region
   * @return
   */
  List<? extends LoadBalancerV2> getLoadBalancers(String region)

  /**
   * Get a load balancer in a region
   * @param region
   * @param lbid
   * @return
   */
  LoadBalancerV2 getLoadBalancer(String region, String lbid)

  /**
   * Get a load balancer status tree in a region
   * @param region
   * @param lbid
   * @return
   */
  LoadBalancerV2StatusTree getLoadBalancerStatusTree(String region, String lbid)

  /**
   * List load balancer pools in a region
   * @param region
   * @return
   */
  List<? extends LbPoolV2> getLoadBalancerPools(String region)

  /**
   * Get a load balancer pool in a region
   * @param region
   * @param poolId
   * @return
   */
  LbPoolV2 getLoadBalancerPool(String region, String poolId)

  /**
   * List members of a load balancer pool in a region
   * @param region
   * @param poolId
   * @return
   */
  List<? extends MemberV2> getLoadBalancerPoolMembers(String region, String poolId)
}
