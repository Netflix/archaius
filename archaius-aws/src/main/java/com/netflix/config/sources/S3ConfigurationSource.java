
package com.netflix.config.sources;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * A polled configuration source backed by a file on Amazon S3.
 * When successfully retrieved, the file is decoded using
 * {@link java.util.Properties#load(InputStream)} - just like
 * {@link com.netflix.config.sources.URLConfigurationSource}.
 *
 * Poll requests throw exceptions in line with
 * {@link com.amazonaws.services.s3.AmazonS3#getObject(GetObjectRequest)} and
 * {@link java.util.Properties#load(InputStream)} for the obvious reasons
 * (file not found, bad credentials, no network connection, malformed file...)
 *
 * @author Michael Tandy
 */
public class S3ConfigurationSource implements PolledConfigurationSource {
    private final AmazonS3 client;
    private final String bucketName;
    private final String key;

    /**
     * Create the instance with the specified credentials, bucket and key.
     * Uses the default {@link com.amazonaws.services.s3.AmazonS3Client}.
     * @param credentialsProvider
     * @param bucketName The S3 bucket containing the configuration file.
     * @param key The key of the file within that bucket.
     */
    public S3ConfigurationSource(AWSCredentialsProvider credentialsProvider, String bucketName, String key) {
        this(new AmazonS3Client(credentialsProvider), bucketName, key);
    }

    /**
     * Create the instance with the provided {@link com.amazonaws.services.s3.AmazonS3},
     * bucket and key. Suitable for injecting a custom client for testing,
     * messing around with endpoints etc.
     * @param client to be used to retrieve the object.
     * @param bucketName The S3 bucket containing the configuration file.
     * @param key The key of the file within that bucket.
     */
    public S3ConfigurationSource(AmazonS3 client, String bucketName, String key) {
        this.client = client;
        this.bucketName = bucketName;
        this.key = key;
    }

    @Override
    public PollResult poll(boolean initial, Object checkPoint) throws IOException, AmazonServiceException {
        GetObjectRequest s3request = new GetObjectRequest(bucketName, key);
        InputStream is = null;
        try {

            S3Object result = client.getObject(s3request);
            is = result.getObjectContent();
            Map<String,Object> resultMap = inputStreamToMap(is);
            return PollResult.createFull(resultMap);

        } finally {
            if (is!=null) is.close();
        }
    }

    protected Map<String,Object> inputStreamToMap(InputStream is) throws IOException {
        // Copied from URLConfigurationSource so behaviour is consistent.
        URLConfigurationSource u;
        Map<String, Object> map = new HashMap<String, Object>();
        Properties props = new Properties();
        props.load(is);
        for (Entry<Object, Object> entry : props.entrySet()) {
            map.put((String) entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(map);
    }

}
