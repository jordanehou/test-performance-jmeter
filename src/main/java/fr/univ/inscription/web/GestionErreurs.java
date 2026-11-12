package fr.univ.inscription.web;

import fr.univ.inscription.dto.Dtos.ErreurDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Transforme les exceptions en réponses JSON avec le bon code HTTP.
 */
@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErreurDto> erreurMetier(ApiException e) {
        return ResponseEntity.status(e.getStatut())
                .body(new ErreurDto(e.getStatut().value(), e.getMessage()));
    }

    /**
     * Violation de la contrainte "unique" en base : un même étudiant a tenté
     * deux inscriptions exactement au même moment. La base a refusé la seconde.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErreurDto> doublon(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErreurDto(409, "Inscription déjà existante pour cet étudiant"));
    }
}
