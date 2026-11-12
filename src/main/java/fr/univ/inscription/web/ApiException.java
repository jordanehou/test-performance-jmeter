package fr.univ.inscription.web;

import org.springframework.http.HttpStatus;

/**
 * Erreur "métier" associée à un code HTTP (401, 403, 404, 409...).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus statut;

    public ApiException(HttpStatus statut, String message) {
        super(message);
        this.statut = statut;
    }

    public HttpStatus getStatut() {
        return statut;
    }
}
