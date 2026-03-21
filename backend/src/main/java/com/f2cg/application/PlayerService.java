package com.f2cg.application;

import com.f2cg.api.dto.AuthResponse;
import com.f2cg.api.dto.RegisterResponse;
import com.f2cg.domain.player.Player;
import com.f2cg.infrastructure.JwtUtil;
import com.f2cg.infrastructure.r2dbc.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PlayerService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern HTML_CHARS = Pattern.compile("[<>\"'&]");

    private final PlayerRepository playerRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Random random = new Random();

    public PlayerService(PlayerRepository playerRepository, JwtUtil jwtUtil, EmailService emailService) {
        this.playerRepository = playerRepository;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public Mono<RegisterResponse> register(String email, String password) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format"));
        }
        return playerRepository.findByUsername(email)
                .flatMap(existing -> Mono.<RegisterResponse>error(
                        new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered")))
                .switchIfEmpty(Mono.defer(() -> {
                    String id = UUID.randomUUID().toString();
                    String hash = passwordEncoder.encode(password);
                    String code = String.format("%05d", random.nextInt(100000));
                    LocalDateTime expires = LocalDateTime.now().plusMinutes(15);
                    Player player = new Player(id, email, hash, false, code, expires, null, null);
                    return playerRepository.save(player)
                            .doOnNext(p -> emailService.sendVerificationCode(email, code))
                            .thenReturn(new RegisterResponse("VERIFICATION_SENT"));
                }));
    }

    public Mono<AuthResponse> verify(String email, String code) {
        return playerRepository.findByUsername(email)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")))
                .flatMap(player -> {
                    if (!code.equals(player.getActivationCode())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code"));
                    }
                    if (player.getActivationCodeExpires() == null ||
                            LocalDateTime.now().isAfter(player.getActivationCodeExpires())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code has expired"));
                    }
                    player.setActive(true);
                    player.setActivationCode(null);
                    player.setActivationCodeExpires(null);
                    return playerRepository.save(player)
                            .map(p -> new AuthResponse(p.getId(), jwtUtil.generate(p.getId()), p.getNickname(), p.getCountry()));
                });
    }

    public Mono<AuthResponse> login(String email, String password) {
        return playerRepository.findByUsername(email)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(player -> {
                    if (!passwordEncoder.matches(password, player.getPasswordHash())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
                    }
                    if (!player.isActive()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not activated"));
                    }
                    return Mono.just(new AuthResponse(player.getId(), jwtUtil.generate(player.getId()), player.getNickname(), player.getCountry()));
                });
    }

    public Mono<Void> updateProfile(String playerId, String nickname, String country) {
        if (nickname != null && !nickname.isBlank()) {
            String n = nickname.trim();
            if (n.length() > 30) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname too long (max 30)"));
            }
            if (HTML_CHARS.matcher(n).find()) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname contains invalid characters"));
            }
        }
        if (country != null && !country.isBlank()) {
            String c = country.trim();
            if (c.length() > 60) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Country too long (max 60)"));
            }
            if (HTML_CHARS.matcher(c).find()) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Country contains invalid characters"));
            }
        }
        return playerRepository.findById(playerId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found")))
                .flatMap(player -> {
                    player.setNickname(nickname != null && !nickname.isBlank() ? nickname.trim() : null);
                    player.setCountry(country != null && !country.isBlank() ? country.trim() : null);
                    return playerRepository.save(player);
                })
                .then();
    }
}