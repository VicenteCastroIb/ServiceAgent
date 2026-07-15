package com.tuapp.service;

/**
 * Error de negocio al sincronizar el catálogo con la tienda online (credenciales
 * inválidas, URL inalcanzable, etc.) - con mensaje pensado para mostrarse tal
 * cual en el panel, no un stacktrace (ver CatalogSyncService).
 */
public class CatalogSyncException extends RuntimeException {
    public CatalogSyncException(String message) {
        super(message);
    }
}
