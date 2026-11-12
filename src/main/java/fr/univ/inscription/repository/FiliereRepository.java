package fr.univ.inscription.repository;

import fr.univ.inscription.model.Filiere;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FiliereRepository extends JpaRepository<Filiere, Long> {

    /**
     * Lit une filière en la VERROUILLANT (SELECT ... FOR UPDATE).
     * Tant que la transaction n'est pas terminée, les autres inscriptions
     * dans la même filière attendent : deux étudiants ne peuvent donc pas
     * prendre la dernière place en même temps.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Filiere f where f.id = :id")
    Optional<Filiere> findByIdPourMiseAJour(@Param("id") Long id);
}
