package fr.univ.inscription.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Filière (formation) avec un nombre de places limité.
 */
@Entity
public class Filiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private int capacite;

    private int placesDisponibles;

    protected Filiere() {
        // requis par JPA
    }

    public Filiere(String nom, int capacite) {
        this.nom = nom;
        this.capacite = capacite;
        this.placesDisponibles = capacite;
    }

    /** Retire une place. À appeler uniquement quand la filière est verrouillée. */
    public void prendreUnePlace() {
        if (placesDisponibles <= 0) {
            throw new IllegalStateException("Plus de place disponible");
        }
        placesDisponibles--;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public int getCapacite() { return capacite; }
    public int getPlacesDisponibles() { return placesDisponibles; }
}
