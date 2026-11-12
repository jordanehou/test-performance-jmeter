package fr.univ.inscription.web;

import fr.univ.inscription.dto.Dtos.FiliereDto;
import fr.univ.inscription.service.InscriptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FiliereController {

    private final InscriptionService inscriptionService;

    public FiliereController(InscriptionService inscriptionService) {
        this.inscriptionService = inscriptionService;
    }

    /** GET /api/filieres : liste des filières avec leurs places disponibles (accès libre). */
    @GetMapping("/api/filieres")
    public List<FiliereDto> lister() {
        return inscriptionService.listerFilieres();
    }
}
