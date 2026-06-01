package com.example.authserver.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    @Test
    void givenMissingClientsWhenRunnerExecutesThenSeedsAllClients() throws Exception {
        RegisteredClientRepository repo = mock(RegisteredClientRepository.class);
        when(repo.findByClientId("client-app")).thenReturn(null);
        when(repo.findByClientId("frontend-client")).thenReturn(null);
        when(repo.findByClientId("service-account")).thenReturn(null);
        doAnswer(inv -> inv.getArgument(0)).when(repo).save(any());

        new DataInitializer().initOAuth2Clients(repo).run(null);

        verify(repo, times(3)).save(any());
    }

    @Test
    void givenExistingClientWhenRunnerExecutesThenSkipsDuplicateClient() throws Exception {
        RegisteredClientRepository repo = mock(RegisteredClientRepository.class);
        RegisteredClient existing = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("client-app")
                .clientSecret("secret")
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        when(repo.findByClientId("client-app")).thenReturn(existing);
        when(repo.findByClientId("frontend-client")).thenReturn(null);
        when(repo.findByClientId("service-account")).thenReturn(null);
        doAnswer(inv -> inv.getArgument(0)).when(repo).save(any());

        new DataInitializer().initOAuth2Clients(repo).run(null);

        verify(repo, times(2)).save(any());
    }
}

