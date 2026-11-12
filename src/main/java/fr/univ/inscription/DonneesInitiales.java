package fr.univ.inscription;

import fr.univ.inscription.model.Etudiant;
import fr.univ.inscription.model.Filiere;
import fr.univ.inscription.repository.EtudiantRepository;
import fr.univ.inscription.repository.FiliereRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Remplit la base au démarrage :
 *  - 5 filières (600 places au total) ;
 *  - 500 comptes étudiants etudiant001 ... etudiant500, mot de passe "test123".
 */
@Component
public class DonneesInitiales implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DonneesInitiales.class);

    private final FiliereRepository filiereRepository;
    private final EtudiantRepository etudiantRepository;

    public DonneesInitiales(FiliereRepository filiereRepository, EtudiantRepository etudiantRepository) {
        this.filiereRepository = filiereRepository;
        this.etudiantRepository = etudiantRepository;
    }

    @Override
    public void run(String... args) {
        filiereRepository.saveAll(List.of(
                new Filiere("Informatique", 120),
                new Filiere("Mathématiques", 120),
                new Filiere("Physique", 120),
                new Filiere("Économie", 120),
                new Filiere("Biologie", 120)));

        List<Etudiant> etudiants = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            etudiants.add(new Etudiant(String.format("etudiant%03d", i), "test123"));
        }
        etudiantRepository.saveAll(etudiants);

        LOG.info("Données initiales : {} filières, {} étudiants",
                filiereRepository.count(), etudiantRepository.count());
    }
}
