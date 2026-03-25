package com.payments.api.adapter.in.rest.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientManagementController {

    @GetMapping
    public ResponseEntity<List<Object>> listClients() {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<Object> getClient(@PathVariable String clientId) {
        return ResponseEntity.notFound().build();
    }
}
