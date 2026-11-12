package fr.univ.inscription.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Compte étudiant (etudiant001 à etudiant500).
 * Le mot de passe est stocké en clair : c'est volontaire, l'application est fictive
 * et sert uniquement aux tests de performance.
 */
@Entity
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(nullable = false)
    private String motDePasse;

    protected Etudiant() {
        // requis par JPA
    }

    public Etudiant(String login, String motDePasse) {
        this.login = login;
        this.motDePasse = motDePasse;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getMotDePasse() { return motDePasse; }
}
