package com.example.frontend.model;

import java.util.List;

/** Ana sayfa view-model'i. */
public record HomePageModel(
        List<?> featuredProducts,
        List<?> topRated,
        List<?> newest,
        int     cartCount
) implements PageModel {}

