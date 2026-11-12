package fr.univ.inscription.service;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/**
 * Mode démo "lent" (demo.slow-mode=true).
 *
 * Simule un traitement lent et limité, par exemple un service externe de validation
 * des dossiers qui ne peut traiter que N demandes à la fois (demo.slow-concurrency),
 * chacune prenant demo.slow-delay-ms millisecondes.
 *
 * Avec peu d'utilisateurs, chaque inscription prend juste un peu plus de temps.
 * Avec beaucoup d'utilisateurs, les demandes font la queue devant les N "guichets"
 * et les temps de réponse augmentent fortement : c'est la dégradation qu'on veut montrer.
 */
@Component
public class ModeDemoLent {

    private final boolean actif;
    private final long delaiMs;
    private final Semaphore guichets;

    public ModeDemoLent(@Value("${demo.slow-mode:false}") boolean actif,
                        @Value("${demo.slow-delay-ms:300}") long delaiMs,
                        @Value("${demo.slow-concurrency:10}") int nombreGuichets) {
        this.actif = actif;
        this.delaiMs = delaiMs;
        this.guichets = new Semaphore(nombreGuichets, true);
        if (actif) {
            LoggerFactory.getLogger(ModeDemoLent.class).warn(
                    "Mode démo LENT activé : {} ms par inscription, {} en parallèle au maximum",
                    delaiMs, nombreGuichets);
        }
    }

    public void simulerTraitementLent() {
        if (!actif) {
            return;
        }
        guichets.acquireUninterruptibly(); // attend qu'un "guichet" se libère
        try {
            Thread.sleep(delaiMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            guichets.release();
        }
    }
}
