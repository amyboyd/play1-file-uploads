package com.alienmegacorp.fileuploads;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import java.io.File;
import play.Logger;
import play.Play;

class Helper {
    final static File DIR_ROOT = Play.getFile("public/uploads");

    final static String AWS_BUCKET_NAME = play.Play.configuration.getProperty("application.amazonS3Bucket", "false");

    final static AmazonS3Client S3_CLIENT;

    final static boolean USE_S3 = !AWS_BUCKET_NAME.equals("false");

    static {
        if (AWS_BUCKET_NAME == null || AWS_BUCKET_NAME.isEmpty()) {
            throw new RuntimeException("application.conf must have the property 'application.amazonS3Bucket'");
        }

        Logger.info("Using Amazon S3? " + (USE_S3 ? "yes, bucket is" + AWS_BUCKET_NAME : "no"));

        if (USE_S3) {
            AWSCredentials awsCredentials = new BasicAWSCredentials(
                    Play.configuration.getProperty("aws.accessKey"),
                    Play.configuration.getProperty("aws.secretKey"));
            S3_CLIENT = new AmazonS3Client(awsCredentials);
        } else {
            S3_CLIENT = null;
        }
    }

    static String getBaseURL() {
        final StringBuilder sb = new StringBuilder(100);

        final String protocol = ((play.mvc.Http.Request.current() != null && play.mvc.Http.Request.current().secure)
                ? "https://"
                : "http://");

        if (USE_S3) {
            sb.append(protocol);
            sb.append(AWS_BUCKET_NAME).append(".s3.amazonaws.com/");
        } else {
            String baseUrl = play.Play.configuration.getProperty("application.baseUrl");
            baseUrl = baseUrl.replace("http://", protocol);
            sb.append(baseUrl);
            sb.append("uploads/");
        }

        return sb.toString();
    }

    private Helper() {
    }
}
