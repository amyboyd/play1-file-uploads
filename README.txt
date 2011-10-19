For use with the Play framework.

application.conf must set the properties "aws.accessKey", "aws.secretKey",
 * and "application.amazonS3Bucket" must also be set.
 * If amazonS3Bucket is "false", the local file system will be used instead of S3.
 * If not "false", the value will be the name of the S3 bucket.