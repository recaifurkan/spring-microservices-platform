package com.example.frontend.model;

/** Kayıt sayfası view-model'i. */
public record RegisterPageModel(
        String  error,
        Boolean success
) implements PageModel {
    @Override public int cartCount() { return 0; }
}

