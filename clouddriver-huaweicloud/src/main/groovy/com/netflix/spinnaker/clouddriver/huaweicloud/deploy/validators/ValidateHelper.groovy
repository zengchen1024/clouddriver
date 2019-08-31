
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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import org.springframework.validation.Errors

class ValidateHelper {

  String context
  Errors errors

  ValidateHelper(String context, Errors errors) {
    this.context = context
    this.errors = errors
  }

  boolean validateNotEmpty(Object value, String attribute) {
    validateNotEmptyAsPart(value, attribute, attribute)
  }

  boolean validateNotEmptyAsPart(Object value, String attribute, String part) {
    if (value && value instanceof Number) {
      return true
    }
    errors.rejectValue(attribute, "${context}.${part}.empty")
    return false
  }

  /**
   * Validate if string is a valid UUID id.
   * @param value
   * @param attribute
   * @return
   */
  boolean validateUUID(String value, String attribute) {
    if (!(validateNotEmpty(value, attribute))) {
      return false
    }

    try {
      UUID.fromString(value)
      return true
    } catch (IllegalArgumentException e) {
      errors.rejectValue("${context}.${attribute}", "${context}.${attribute}.notUUID")
    }
    return false
  }
}
