package fr.univ.inscription.service;

import fr.univ.inscription.dto.Dtos.LoginResponse;
import fr.univ.inscription.model.Etudiant;
import fr.univ.inscription.repository.EtudiantRepository;
import fr.univ.inscription.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentification volontairement simple (pas de Spring Security) :
 * à la connexion on génère un jeton aléatoire (UUID) gardé en mémoire.
 * Le client le renvoie ensuite dans l'en-tête "Authorization: Bearer <jeton>".
 */
@Service
public class AuthService {

    /** jeton -> identifiant de l'étudiant. ConcurrentHashMap car plusieurs requêtes arrivent en parallèle. */
    private final Map<String, Long> jetons = new ConcurrentHashMap<>();

    private final EtudiantRepository etudiantRepository;

    public AuthService(EtudiantRepository etudiantRepository) {
        this.etudiantRepository = etudiantRepository;
    }

    /** Vérifie login / mot de passe et renvoie un nouveau jeton. */
    public LoginResponse connecter(String login, String motDePasse) {
        Etudiant etudiant = etudiantRepository.findByLogin(login)
                .filter(e -> e.getMotDePasse().equals(motDePasse))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects"));
        String jeton = UUID.randomUUID().toString();
        jetons.put(jeton, etudiant.getId());
        return new LoginResponse(jeton, etudiant.getId());
    }

    /** Lit l'en-tête Authorization et renvoie l'identifiant de l'étudiant connecté. */
    public Long etudiantConnecte(String enteteAuthorization) {
        if (enteteAuthorization == null || !enteteAuthorization.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Jeton manquant");
        }
        Long etudiantId = jetons.get(enteteAuthorization.substring("Bearer ".length()));
        if (etudiantId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Jeton invalide");
        }
        return etudiantId;
    }
}
