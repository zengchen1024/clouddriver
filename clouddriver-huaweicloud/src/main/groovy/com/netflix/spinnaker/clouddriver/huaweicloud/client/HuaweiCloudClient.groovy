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

import com.huawei.openstack4j.model.compute.ext.AvailabilityZone
import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.huawei.openstack4j.model.scaling.ScalingGroupInstance
import com.huawei.openstack4j.openstack.ecs.v1.domain.CloudServer
import com.huawei.openstack4j.openstack.ecs.v1.domain.Flavor
import com.huawei.openstack4j.openstack.ims.v2.domain.Image
import com.huawei.openstack4j.openstack.vpc.v1.domain.PublicIp
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroup
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
   * List images in a region
   * @param region
   * @return
   */
  List<Image> getImages(String region)

  /**
   * List elastic compute instances in a region
   * @param region
   * @return
   */
  List<CloudServer> getInstances(String region)

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
}
