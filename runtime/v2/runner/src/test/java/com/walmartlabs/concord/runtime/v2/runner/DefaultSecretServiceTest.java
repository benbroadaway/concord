package com.walmartlabs.concord.runtime.v2.runner;

import com.walmartlabs.concord.client2.ApiClient;
import com.walmartlabs.concord.client2.SecretClient;
import com.walmartlabs.concord.common.secret.BinaryDataSecret;
import com.walmartlabs.concord.runtime.common.cfg.RunnerConfiguration;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import com.walmartlabs.concord.runtime.v2.sdk.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSecretServiceTest {

    @Mock
    ApiClient apiClient;

    @Mock
    FileService fileService;

    @Mock
    InstanceId instanceId;

    @Mock
    SecretClient secretClient;

    @Mock
    BinaryDataSecret secret;

    @Test
    void testCache() throws Exception {
        var runnerConfiguration = RunnerConfiguration.builder()
                .enableSecretCache(true)
                .build();

        DefaultSecretService secretService = new DefaultSecretService(runnerConfiguration, apiClient, fileService, instanceId) {
            @Override
            protected SecretClient getSecretClient() {
                return secretClient;
            }
        };

        doAnswer(invocation -> secret)
                .when(secretClient).getData(any(), any(), any(), any());

        when(secret.getData()).thenReturn("hello".getBytes(StandardCharsets.UTF_8));

        secretService.exportAsString("my-org", "my-secret", null);
        secretService.exportAsString("my-org", "my-secret", null);
        secretService.exportAsString("my-org", "my-secret", null);
        verify(secretClient, times(1)).getData(any(), any(), any(), any());
    }

    @Test
    void testNoCache() throws Exception {
        var runnerConfiguration = RunnerConfiguration.builder()
                .enableSecretCache(false)
                .build();

        DefaultSecretService secretService = new DefaultSecretService(runnerConfiguration, apiClient, fileService, instanceId) {
            @Override
            protected SecretClient getSecretClient() {
                return secretClient;
            }
        };

        doAnswer(invocation -> secret)
                .when(secretClient).getData(any(), any(), any(), any());

        when(secret.getData()).thenReturn("hello".getBytes(StandardCharsets.UTF_8));

        secretService.exportAsString("my-org", "my-secret", null);
        secretService.exportAsString("my-org", "my-secret", null);
        secretService.exportAsString("my-org", "my-secret", null);
        verify(secretClient, times(3)).getData(any(), any(), any(), any());
    }
}
