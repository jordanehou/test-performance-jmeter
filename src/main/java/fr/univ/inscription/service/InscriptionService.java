package fr.univ.inscription.service;

import fr.univ.inscription.dto.Dtos.FiliereDto;
import fr.univ.inscription.dto.Dtos.InscriptionDto;
import fr.univ.inscription.model.Etudiant;
import fr.univ.inscription.model.Filiere;
import fr.univ.inscription.model.Inscription;
import fr.univ.inscription.repository.EtudiantRepository;
import fr.univ.inscription.repository.FiliereRepository;
import fr.univ.inscription.repository.InscriptionRepository;
import fr.univ.inscription.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InscriptionService {

    private final FiliereRepository filiereRepository;
    private final InscriptionRepository inscriptionRepository;
    private final EtudiantRepository etudiantRepository;

    public InscriptionService(FiliereRepository filiereRepository,
                              InscriptionRepository inscriptionRepository,
                              EtudiantRepository etudiantRepository) {
        this.filiereRepository = filiereRepository;
        this.inscriptionRepository = inscriptionRepository;
        this.etudiantRepository = etudiantRepository;
    }

    @Transactional(readOnly = true)
    public List<FiliereDto> listerFilieres() {
        return filiereRepository.findAll().stream()
                .map(f -> new FiliereDto(f.getId(), f.getNom(), f.getCapacite(), f.getPlacesDisponibles()))
                .toList();
    }

    /**
     * Inscrit un étudiant dans une filière, en gérant la concurrence :
     *  1. on verrouille la filière (les autres inscriptions dans cette filière attendent) ;
     *  2. si l'étudiant est déjà inscrit dans CETTE filière, on renvoie l'inscription existante
     *     (requête rejouée = pas de doublon) ; s'il est inscrit ailleurs : erreur 409 ;
     *  3. s'il ne reste plus de place : erreur 409 ;
     *  4. sinon on retire une place et on enregistre l'inscription.
     * Le verrou est libéré à la fin de la transaction.
     */
    @Transactional
    public InscriptionDto inscrire(Long etudiantId, Long filiereId) {
        if (filiereId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "filiereId obligatoire");
        }
        Filiere filiere = filiereRepository.findByIdPourMiseAJour(filiereId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Filière inconnue"));

        Optional<Inscription> existante = inscriptionRepository.findByEtudiantId(etudiantId);
        if (existante.isPresent()) {
            if (existante.get().getFiliere().getId().equals(filiereId)) {
                return versDto(existante.get());
            }
            throw new ApiException(HttpStatus.CONFLICT, "Étudiant déjà inscrit dans une autre filière");
        }

        if (filiere.getPlacesDisponibles() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Plus de place disponible dans cette filière");
        }

        Etudiant etudiant = etudiantRepository.getReferenceById(etudiantId);
        filiere.prendreUnePlace();
        Inscription inscription = inscriptionRepository.saveAndFlush(new Inscription(etudiant, filiere));
        return versDto(inscription);
    }

    /** Un étudiant ne peut consulter que sa propre inscription. */
    @Transactional(readOnly = true)
    public InscriptionDto consulter(Long inscriptionId, Long etudiantId) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Inscription introuvable"));
        if (!inscription.getEtudiant().getId().equals(etudiantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette inscription n'est pas la vôtre");
        }
        return versDto(inscription);
    }

    private InscriptionDto versDto(Inscription i) {
        return new InscriptionDto(i.getId(), i.getEtudiant().getLogin(), i.getFiliere().getId(),
                i.getFiliere().getNom(), i.getDateInscription());
    }
}
