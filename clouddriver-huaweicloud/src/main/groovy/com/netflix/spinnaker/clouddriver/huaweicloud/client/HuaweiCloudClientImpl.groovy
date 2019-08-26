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
import com.huawei.openstack4j.model.compute.RebootType
import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.huawei.openstack4j.model.scaling.ScalingGroupInstance
import com.huawei.openstack4j.openstack.ims.v2.domain.Image
import com.huawei.openstack4j.openstack.vpc.v1.domain.PublicIp
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroup
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
    getRegionClient(region).compute().servers().reboot(instanceId, RebootType.SOFT)
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
  List<SecurityGroup> getSecurityGroups(String region) {
    getRegionClient(region).vpc().securityGroups().list()
  }

  @Override
  List<? extends ScalingGroup> getScalingGroups(String region) {
    getRegionClient(region).autoScaling().groups().list()
  }

  @Override
  List<? extends ScalingGroupInstance> getScalingGroupInstances(String region, String groupId) {
    getRegionClient(region).autoScaling().groupInstances().list(groupId)
  }

  @Override
  List<Image> getImages(String region) {
    getRegionClient(region).imsV2().images().list()
  }
}
