package com.github.pagination.connector.internal.operations;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Optional;

public enum PaginationErrors implements ErrorTypeDefinition<PaginationErrors> {
    EMPTY_FIRST_PAGE,
    PROCESSING_ERROR,
    CHAIN_ERROR,
    INVALID_CONFIGURATION;

    @Override
    public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
        return Optional.empty();
    }
}