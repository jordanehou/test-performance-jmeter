package fr.univ.inscription.dto;

import java.time.LocalDateTime;

/**
 * Objets échangés en JSON avec les clients (et donc avec JMeter).
 * Les "record" Java sont de simples conteneurs de données immuables.
 */
public final class Dtos {

    private Dtos() {
    }

    /** Corps de POST /api/auth/login : {"login":"etudiant001","motDePasse":"test123"} */
    public record LoginRequest(String login, String motDePasse) {}

    /** Réponse du login : le jeton à renvoyer dans l'en-tête "Authorization: Bearer ..." */
    public record LoginResponse(String token, Long etudiantId) {}

    /** Élément de GET /api/filieres */
    public record FiliereDto(Long id, String nom, int capacite, int placesDisponibles) {}

    /** Corps de POST /api/inscriptions : {"filiereId":1} */
    public record InscriptionRequest(Long filiereId) {}

    /** Réponse de POST /api/inscriptions et de GET /api/inscriptions/{id} */
    public record InscriptionDto(Long id, String etudiant, Long filiereId, String filiere,
                                 LocalDateTime dateInscription) {}

    /** Message d'erreur renvoyé en JSON */
    public record ErreurDto(int statut, String erreur) {}
}
