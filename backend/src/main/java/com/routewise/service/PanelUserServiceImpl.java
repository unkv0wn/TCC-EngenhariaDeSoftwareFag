package com.routewise.service;

import com.routewise.auth.JwtService;
import com.routewise.dto.LoginRequestDto;
import com.routewise.dto.LoginResponseDto;
import com.routewise.dto.PanelUserDto;
import com.routewise.dto.PanelUserRequestDto;
import com.routewise.entity.PanelUser;
import com.routewise.exception.PanelAccessBlockedException;
import com.routewise.exception.PanelAuthException;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.PanelUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PanelUserServiceImpl implements IPanelUserService {

  /** Mesma frase para e-mail inexistente e senha errada — ver {@link PanelAuthException}. */
  private static final String BAD_CREDENTIALS = "E-mail ou senha incorretos.";

  private static final int MIN_PASSWORD_LENGTH = 6;

  private final PanelUserRepository repository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public PanelUserServiceImpl(PanelUserRepository repository, JwtService jwtService) {
    this.repository = repository;
    this.jwtService = jwtService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PanelUserDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  @Override
  public PanelUserDto create(PanelUserRequestDto request) {
    if (request.password() == null || request.password().isBlank()) {
      throw new IllegalArgumentException("Informe a senha do acesso.");
    }
    requireStrongEnough(request.password());

    String email = normalizeEmail(request.email());
    requireUniqueEmail(email, null);

    PanelUser entity = new PanelUser(
      UUID.randomUUID(),
      request.name().trim(),
      email,
      passwordEncoder.encode(request.password()),
      request.status()
    );
    return toDto(repository.save(entity));
  }

  @Override
  public PanelUserDto update(UUID id, PanelUserRequestDto request) {
    PanelUser entity = findOrThrow(id);

    String email = normalizeEmail(request.email());
    requireUniqueEmail(email, id);

    if (PanelUser.STATUS_BLOCKED.equals(request.status()) && entity.isActive()) {
      requireAnotherActiveAccess(id, "Este é o último acesso ativo ao painel. Bloqueá-lo deixaria o sistema sem ninguém para entrar.");
    }

    entity.setName(request.name().trim());
    entity.setEmail(email);
    entity.setStatus(request.status());

    // Senha em branco mantém a atual: corrigir um nome escrito errado não deveria custar
    // uma senha nova para a pessoa.
    if (request.password() != null && !request.password().isBlank()) {
      requireStrongEnough(request.password());
      entity.setPasswordHash(passwordEncoder.encode(request.password()));
    }

    return toDto(repository.save(entity));
  }

  @Override
  public void delete(UUID id, UUID requesterId) {
    PanelUser entity = findOrThrow(id);

    // Excluir o próprio acesso derruba quem está excluindo no meio da ação, e a sessão
    // continuaria válida até o token expirar — estado confuso sem ganho nenhum.
    if (id.equals(requesterId)) {
      throw new IllegalArgumentException("Você não pode excluir o próprio acesso.");
    }

    if (entity.isActive()) {
      requireAnotherActiveAccess(id, "Este é o último acesso ativo ao painel. Excluí-lo deixaria o sistema sem ninguém para entrar.");
    }

    repository.deleteById(id);
  }

  @Override
  public LoginResponseDto authenticate(LoginRequestDto request) {
    PanelUser user = repository.findByEmailIgnoreCase(normalizeEmail(request.email()))
      .orElseThrow(() -> new PanelAuthException(BAD_CREDENTIALS));

    // A senha é conferida antes do status, pelo mesmo motivo do app: quem não a tem não
    // deve descobrir que a conta existe — nem que ela existe e está bloqueada.
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new PanelAuthException(BAD_CREDENTIALS);
    }

    if (!user.isActive()) {
      throw new PanelAccessBlockedException("Este acesso está bloqueado. Fale com quem administra o painel.");
    }

    user.setLastLoginAt(Instant.now());
    repository.save(user);

    return new LoginResponseDto(
      jwtService.generatePanelToken(user.getEmail(), user.getId(), user.getName()),
      user.getId(),
      user.getName(),
      user.getEmail()
    );
  }

  /**
   * Impede que a última porta se feche por dentro.
   *
   * <p>Sem isto, bloquear ou excluir o acesso errado deixaria o painel sem ninguém capaz
   * de entrar — e o único conserto seria um UPDATE no banco, que é exatamente o tipo de
   * saída que não se tem às seis da tarde de uma sexta.
   */
  private void requireAnotherActiveAccess(UUID selfId, String message) {
    if (repository.countByStatusAndIdNot(PanelUser.STATUS_ACTIVE, selfId) == 0) {
      throw new IllegalArgumentException(message);
    }
  }

  private void requireUniqueEmail(String email, UUID selfId) {
    boolean taken = selfId == null
      ? repository.existsByEmailIgnoreCase(email)
      : repository.existsByEmailIgnoreCaseAndIdNot(email, selfId);
    if (taken) {
      throw new IllegalArgumentException("Já existe um acesso com este e-mail.");
    }
  }

  private void requireStrongEnough(String password) {
    if (password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalArgumentException("A senha deve ter ao menos " + MIN_PASSWORD_LENGTH + " caracteres.");
    }
  }

  private PanelUser findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Acesso não encontrado: " + id));
  }

  /**
   * E-mail é identificador, não texto livre: sem espaço nas pontas e em minúsculas,
   * senão "Teste@" e "teste@ " viram dois acessos para a mesma pessoa.
   */
  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }

  private PanelUserDto toDto(PanelUser entity) {
    return new PanelUserDto(
      entity.getId(),
      entity.getName(),
      entity.getEmail(),
      entity.getStatus(),
      entity.getLastLoginAt()
    );
  }
}
