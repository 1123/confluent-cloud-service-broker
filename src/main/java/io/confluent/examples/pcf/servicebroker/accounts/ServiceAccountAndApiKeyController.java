package io.confluent.examples.pcf.servicebroker.accounts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController("/accounts")
@Slf4j
public class ServiceAccountAndApiKeyController {

    @Autowired ServiceAccountAndApiKeyService serviceAccountAndApiKeyService;

    @PostMapping
    public void add(@RequestBody List<ServiceAccountAndApiKey> serviceAccountAndApiKeys) {
        log.info("Received a list of {} service accounts", serviceAccountAndApiKeys.size());
        validate(serviceAccountAndApiKeys);
        serviceAccountAndApiKeyService.addAll(serviceAccountAndApiKeys);
        log.info("Stored service accounts for future use in memory");
    }

    private void validate(List<ServiceAccountAndApiKey> serviceAccountAndApiKeys) {
        serviceAccountAndApiKeys.forEach(
                serviceAccountAndApiKey -> {
                    if (empty(serviceAccountAndApiKey.getApiKey()))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "apiKey must be set.");
                    if (empty(serviceAccountAndApiKey.getApiSecret()))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "apiSecret must be set.");
                    if (empty(serviceAccountAndApiKey.getServiceAccount()))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceAccount must be set.");
                }
        );
        log.info("all service accounts are valid.");
    }

    private boolean empty(String input) {
        return input == null || input.equals("");
    }

    @GetMapping
    public List<ServiceAccountAndApiKey> getAll() {
        log.info("Listing service accounts with secrets removed");
        return serviceAccountAndApiKeyService.getAll().stream().map(account ->
                ServiceAccountAndApiKey.builder()
                        .apiKey(account.getApiKey())
                        .serviceAccount(account.getServiceAccount())
                        .apiSecret("hidden")
                        .build()).collect(Collectors.toList());
    }

}

