package fr.univ.inscription.web;

import fr.univ.inscription.dto.Dtos.LoginRequest;
import fr.univ.inscription.dto.Dtos.LoginResponse;
import fr.univ.inscription.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/login  {"login":"etudiant001","motDePasse":"test123"} -> {"token":"...","etudiantId":1} */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest requete) {
        return authService.connecter(requete.login(), requete.motDePasse());
    }
}
