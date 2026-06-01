package com.example.productservice.dto;

/** Arama autocomplete önerileri için yanıt nesnesi. */
public record ProductSuggestionResponse(Long id, String name, String category) {}

