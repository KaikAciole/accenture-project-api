package br.com.accenture.assistant.infrastructure.security.ratelimit;

import br.com.accenture.assistant.infrastructure.config.RateLimitProperties;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BucketRegistryTest {

    private BucketRegistry registry;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties(
                new RateLimitProperties.Limits(5, 100),
                new RateLimitProperties.Limits(10, 500),
                new RateLimitProperties.Concurrency(20)
        );
        registry = new BucketRegistry(properties);
    }

    @Test
    void minuteBucketFor_shouldReturnSameInstanceForSameKey() {
        Bucket first = registry.minuteBucketFor("user:1", true);
        Bucket second = registry.minuteBucketFor("user:1", true);

        assertThat(first).isSameAs(second);
    }

    @Test
    void minuteBucketFor_shouldReturnDifferentInstancesForDifferentKeys() {
        Bucket bucketA = registry.minuteBucketFor("user:A", true);
        Bucket bucketB = registry.minuteBucketFor("user:B", true);

        assertThat(bucketA).isNotSameAs(bucketB);
    }

    @Test
    void minuteAndDayBuckets_shouldBeIndependentForSameKey() {
        Bucket minute = registry.minuteBucketFor("user:1", true);
        Bucket day = registry.dayBucketFor("user:1", true);

        assertThat(minute).isNotSameAs(day);
    }

    @Test
    void minuteBucketFor_shouldUseUserCapacityWhenUserKeyTrue() {
        Bucket bucket = registry.minuteBucketFor("user:1", true);

        assertThat(bucket.getAvailableTokens()).isEqualTo(5);
    }

    @Test
    void minuteBucketFor_shouldUseIpCapacityWhenUserKeyFalse() {
        Bucket bucket = registry.minuteBucketFor("ip:10.0.0.1", false);

        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
    }

    @Test
    void dayBucketFor_shouldUseUserCapacityWhenUserKeyTrue() {
        Bucket bucket = registry.dayBucketFor("user:1", true);

        assertThat(bucket.getAvailableTokens()).isEqualTo(100);
    }

    @Test
    void dayBucketFor_shouldUseIpCapacityWhenUserKeyFalse() {
        Bucket bucket = registry.dayBucketFor("ip:10.0.0.1", false);

        assertThat(bucket.getAvailableTokens()).isEqualTo(500);
    }

    @Test
    void minuteBucketFor_shouldReuseExistingBucketIgnoringUserKeyFlagOnSubsequentCalls() {
        Bucket created = registry.minuteBucketFor("shared-key", true);
        Bucket reused = registry.minuteBucketFor("shared-key", false);

        assertThat(reused).isSameAs(created);
        assertThat(reused.getAvailableTokens()).isEqualTo(5);
    }
}
