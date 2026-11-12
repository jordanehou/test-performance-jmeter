package fr.univ.inscription.web;

import fr.univ.inscription.dto.Dtos.InscriptionDto;
import fr.univ.inscription.dto.Dtos.InscriptionRequest;
import fr.univ.inscription.service.AuthService;
import fr.univ.inscription.service.InscriptionService;
import fr.univ.inscription.service.ModeDemoLent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inscriptions")
public class InscriptionController {

    private final AuthService authService;
    private final InscriptionService inscriptionService;
    private final ModeDemoLent modeDemoLent;

    public InscriptionController(AuthService authService, InscriptionService inscriptionService,
                                 ModeDemoLent modeDemoLent) {
        this.authService = authService;
        this.inscriptionService = inscriptionService;
        this.modeDemoLent = modeDemoLent;
    }

    /** POST /api/inscriptions  {"filiereId":1}  (en-tête Authorization: Bearer <jeton> obligatoire) */
    @PostMapping
    public InscriptionDto inscrire(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestBody InscriptionRequest requete) {
        Long etudiantId = authService.etudiantConnecte(authorization);
        // Ne fait rien si demo.slow-mode=false. Appelé hors transaction : aucun verrou n'est bloqué pendant l'attente.
        modeDemoLent.simulerTraitementLent();
        return inscriptionService.inscrire(etudiantId, requete.filiereId());
    }

    /** GET /api/inscriptions/{id} : consulter son inscription (jeton obligatoire). */
    @GetMapping("/{id}")
    public InscriptionDto consulter(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long id) {
        Long etudiantId = authService.etudiantConnecte(authorization);
        return inscriptionService.consulter(id, etudiantId);
    }
}
