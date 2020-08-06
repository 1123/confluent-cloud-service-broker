package io.confluent.examples.pcf.servicebroker.accounts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * This class maintains a list of service accounts with associated Api Keys.
 * Each service account is used only once, never reused.
 * Api keys and secrets are only held in memory, never written to persistent storage.
 * Initially, the list is empty. New Api-Keys can be added via REST.
 */

@Service
@Slf4j
public class ServiceAccountAndApiKeyService {

    List<ServiceAccountAndApiKey> store = new ArrayList<>();

    public ServiceAccountAndApiKey get() {
        if (store.isEmpty()) throw new RuntimeException("No api keys configured");
        ServiceAccountAndApiKey first = store.remove(0);
        log.info("Retrieving ServiceAccount with Key {} and account number {}", first.getApiKey(), first.getServiceAccount());
        return first;
    }

    public void addAll(List<ServiceAccountAndApiKey> serviceAccountAndApiKey) {
        store.addAll(serviceAccountAndApiKey);
    }

    public List<ServiceAccountAndApiKey> getAll() {
        return store;
    }
}

