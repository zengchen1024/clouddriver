package com.netflix.spinnaker.clouddriver.aws.deploy.description;

class InvokeLambdaDescription extends AbstractAmazonCredentialsDescription {
  String region
  String function
  Object payload
}
