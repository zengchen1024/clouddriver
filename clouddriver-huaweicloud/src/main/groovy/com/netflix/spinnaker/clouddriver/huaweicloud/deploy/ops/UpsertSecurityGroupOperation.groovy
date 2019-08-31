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

import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroup
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroupRule
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.UpsertSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import groovy.util.logging.Slf4j

@Slf4j
class UpsertSecurityGroupOperation implements AtomicOperation<Void> {

  private final String BASE_PHASE = 'UPSERT_SECURITY_GROUP'
  UpsertSecurityGroupDescription description

  UpsertSecurityGroupOperation(UpsertSecurityGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {

    TaskAware.task.updateStatus BASE_PHASE, "Upserting security group=${description.name} in region=${description.region}..."

    if (description.id) {
      updateSecurityGroup()
    } else {
      createSecurityGroup()
    }

    TaskAware.task.updateStatus BASE_PHASE, "Finished upserting security group=${description.name}."
  }

  private Void createSecurityGroup() {

    TaskAware.task.updateStatus BASE_PHASE, "Creating a new security group with name=${description.name}"

    SecurityGroup securityGroup = description.credentials.cloudClient.createSecurityGroup(
      description.region, description.name, description.vpcId)

    description.rules.each { rule ->
      createSecurityGroupRule(securityGroup.id, rule)
    }
  }

  private Void updateSecurityGroup() {
    def task = TaskAware.task
    task.updateStatus BASE_PHASE, "Looking up existing security group with id=${description.id}"

    def cloudClient = description.credentials.cloudClient

    SecurityGroup securityGroup = cloudClient.getSecurityGroup(description.region, description.id)
    if (!securityGroup) {
      throw new OperationException(
        AtomicOperations.UPSERT_SECURITY_GROUP, 
        "Could not find securityGroup=$description.id in region=$description.region")
    }

    securityGroup.rules.each { rule ->
      task.updateStatus BASE_PHASE, "Deleting rule=${rule.id}"

      cloudClient.deleteSecurityGroupRule(description.region, rule.id)
    }

    description.rules.each { rule ->
      createSecurityGroupRule(description.id, rule)
    }
  }

  private Void createSecurityGroupRule(String securityGroupId, UpsertSecurityGroupDescription.Rule rule) {
    TaskAware.task.updateStatus BASE_PHASE, "Creating rule for ${rule.cidr} from port ${rule.fromPort} to port ${rule.toPort}"

    description.credentials.cloudClient.createSecurityGroupRule(
      description.region,
      SecurityGroupRule.builder()
        .securityGroupId(securityGroupId)
        .direction("ingress")
        .ethertype("IPv4")
        .protocol(rule.protocol)
        .portRangeMin(rule.portRangeMin)
        .portRangeMax(rule.portRangeMax)
        .remoteIpPrefix(rule.remoteIpPrefix)
        .remoteGroupId(rule.remoteSecurityGroupId)
        .build()
    )
  }
}
