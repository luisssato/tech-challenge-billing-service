package com.fiap.techchallenge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Value("${dynamodb.endpoint:}")
    private String endpoint;

    @Value("${spring.cloud.aws.region.static:${AWS_REGION:us-east-1}}")
    private String region;

    @Value("${spring.cloud.aws.credentials.access-key:${AWS_ACCESS_KEY_ID:}}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:${AWS_SECRET_ACCESS_KEY:}}")
    private String secretKey;

    @Value("${spring.cloud.aws.credentials.session-token:${AWS_SESSION_TOKEN:}}")
    private String sessionToken;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            if (sessionToken != null && !sessionToken.isBlank()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsSessionCredentials.create(accessKey, secretKey, sessionToken)
                ));
            } else {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ));
            }
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
