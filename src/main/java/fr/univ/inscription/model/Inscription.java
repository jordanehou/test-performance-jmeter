package fr.univ.inscription.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

/**
 * Inscription d'un étudiant dans une filière.
 * La contrainte "unique" sur etudiant_id garantit en base qu'un étudiant
 * n'a jamais deux inscriptions (pas de doublon), même en cas d'accès simultanés.
 */
@Entity
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "etudiant_id", unique = true)
    private Etudiant etudiant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "filiere_id")
    private Filiere filiere;

    private LocalDateTime dateInscription;

    protected Inscription() {
        // requis par JPA
    }

    public Inscription(Etudiant etudiant, Filiere filiere) {
        this.etudiant = etudiant;
        this.filiere = filiere;
        this.dateInscription = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Etudiant getEtudiant() { return etudiant; }
    public Filiere getFiliere() { return filiere; }
    public LocalDateTime getDateInscription() { return dateInscription; }
}
