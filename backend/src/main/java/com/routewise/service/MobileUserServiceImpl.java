package com.routewise.service;

import com.routewise.auth.JwtService;
import com.routewise.dto.MobileLoginRequestDto;
import com.routewise.dto.MobileLoginResponseDto;
import com.routewise.dto.MobileUserDto;
import com.routewise.dto.MobileUserRequestDto;
import com.routewise.entity.Driver;
import com.routewise.entity.MobileUser;
import com.routewise.exception.DeviceNotAuthorizedException;
import com.routewise.exception.MobileAuthException;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.DriverRepository;
import com.routewise.repository.MobileUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MobileUserServiceImpl implements IMobileUserService {

  /** Mesma frase para usuário inexistente e senha errada — ver {@link MobileAuthException}. */
  private static final String BAD_CREDENTIALS = "Usuário ou senha incorretos.";

  private static final int MIN_PASSWORD_LENGTH = 6;

  private final MobileUserRepository repository;
  private final DriverRepository driverRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public MobileUserServiceImpl(
    MobileUserRepository repository,
    DriverRepository driverRepository,
    JwtService jwtService
  ) {
    this.repository = repository;
    this.driverRepository = driverRepository;
    this.jwtService = jwtService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MobileUserDto> findAll() {
    // Nomes de uma vez: a lista é pequena, mas buscar o motorista de cada acesso dentro
    // do map faria uma consulta por linha sem necessidade.
    Map<UUID, String> driverNames = driverRepository.findAll().stream()
      .collect(Collectors.toMap(Driver::getId, Driver::getFullName));

    return repository.findAll().stream()
      .map(user -> toDto(user, driverNames.get(user.getDriverId())))
      .toList();
  }

  @Override
  public MobileUserDto create(MobileUserRequestDto request) {
    if (request.password() == null || request.password().isBlank()) {
      throw new IllegalArgumentException("Informe a senha do acesso.");
    }
    requireStrongEnough(request.password());

    String username = normalizeUsername(request.username());
    String deviceId = blankToNull(request.deviceId());

    requireDriverExists(request.driverId());
    requireUniqueUsername(username, null);
    requireDriverWithoutAccess(request.driverId(), null);
    requireUniqueDevice(deviceId, null);

    MobileUser entity = new MobileUser(
      UUID.randomUUID(),
      request.driverId(),
      username,
      passwordEncoder.encode(request.password()),
      deviceId,
      blankToNull(request.deviceLabel()),
      request.status()
    );
    return toDto(repository.save(entity), driverName(request.driverId()));
  }

  @Override
  public MobileUserDto update(UUID id, MobileUserRequestDto request) {
    MobileUser entity = findOrThrow(id);

    String username = normalizeUsername(request.username());
    String deviceId = blankToNull(request.deviceId());

    requireDriverExists(request.driverId());
    requireUniqueUsername(username, id);
    requireDriverWithoutAccess(request.driverId(), id);
    requireUniqueDevice(deviceId, id);

    entity.setDriverId(request.driverId());
    entity.setUsername(username);
    entity.setDeviceId(deviceId);
    entity.setDeviceLabel(blankToNull(request.deviceLabel()));
    entity.setStatus(request.status());

    // Senha em branco mantém a atual: corrigir o rótulo do aparelho não deveria custar
    // uma senha nova e um telefonema para o motorista.
    if (request.password() != null && !request.password().isBlank()) {
      requireStrongEnough(request.password());
      entity.setPasswordHash(passwordEncoder.encode(request.password()));
    }

    return toDto(repository.save(entity), driverName(request.driverId()));
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Acesso não encontrado: " + id);
    }
    repository.deleteById(id);
  }

  @Override
  public MobileLoginResponseDto authenticate(MobileLoginRequestDto request) {
    MobileUser user = repository.findByUsernameIgnoreCase(normalizeUsername(request.username()))
      .orElseThrow(() -> new MobileAuthException(BAD_CREDENTIALS));

    // A senha é conferida antes de qualquer coisa sobre aparelho ou status: quem não a
    // tem não deve descobrir que o usuário existe, nem qual celular está preso a ele.
    // Só depois de provar a senha é que as mensagens passam a ser específicas.
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new MobileAuthException(BAD_CREDENTIALS);
    }

    if (!user.isActive()) {
      throw new DeviceNotAuthorizedException("Este acesso está bloqueado. Fale com o gestor.");
    }
    if (!user.hasDevice()) {
      throw new DeviceNotAuthorizedException(
        "Este aparelho ainda não foi cadastrado. Envie ao gestor o identificador mostrado na tela de login."
      );
    }
    if (!user.matchesDevice(request.deviceId())) {
      throw new DeviceNotAuthorizedException(
        "Este celular não é o cadastrado para o seu usuário. Fale com o gestor para liberar o aparelho."
      );
    }

    user.setLastLoginAt(Instant.now());
    repository.save(user);

    return new MobileLoginResponseDto(
      jwtService.generateMobileToken(user.getUsername(), user.getDriverId(), user.getDeviceId()),
      user.getDriverId(),
      driverName(user.getDriverId()),
      user.getUsername()
    );
  }

  // ── Regras de unicidade ───────────────────────────────────────────────────
  //
  // Checadas aqui, e não só pelas constraints do banco, porque a violação crua volta
  // como 409 genérico ("já existe um registro com esses dados") e o gestor não saberia
  // qual dos três campos repetiu.

  private void requireUniqueUsername(String username, UUID selfId) {
    boolean taken = selfId == null
      ? repository.existsByUsernameIgnoreCase(username)
      : repository.existsByUsernameIgnoreCaseAndIdNot(username, selfId);
    if (taken) {
      throw new IllegalArgumentException("Já existe um acesso com este usuário.");
    }
  }

  private void requireDriverWithoutAccess(UUID driverId, UUID selfId) {
    boolean taken = selfId == null
      ? repository.existsByDriverId(driverId)
      : repository.existsByDriverIdAndIdNot(driverId, selfId);
    if (taken) {
      throw new IllegalArgumentException("Este motorista já tem um acesso ao app.");
    }
  }

  private void requireUniqueDevice(String deviceId, UUID selfId) {
    if (deviceId == null) return;
    boolean taken = selfId == null
      ? repository.existsByDeviceIdIgnoreCase(deviceId)
      : repository.existsByDeviceIdIgnoreCaseAndIdNot(deviceId, selfId);
    if (taken) {
      throw new IllegalArgumentException("Este aparelho já está cadastrado em outro acesso.");
    }
  }

  private void requireStrongEnough(String password) {
    if (password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalArgumentException("A senha deve ter ao menos " + MIN_PASSWORD_LENGTH + " caracteres.");
    }
  }

  private void requireDriverExists(UUID driverId) {
    if (!driverRepository.existsById(driverId)) {
      throw new ResourceNotFoundException("Motorista não encontrado: " + driverId);
    }
  }

  private String driverName(UUID driverId) {
    return driverRepository.findById(driverId).map(Driver::getFullName).orElse(null);
  }

  private MobileUser findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Acesso não encontrado: " + id));
  }

  /**
   * Usuário é identificador, não texto livre: sem espaço nas pontas e em minúsculas,
   * senão "Carlos" e "carlos " viram dois acessos que ninguém distingue na tela.
   */
  private String normalizeUsername(String username) {
    return username == null ? null : username.trim().toLowerCase();
  }

  /** Campo opcional vazio na tela chega como string vazia; no banco tem que ser nulo,
   *  senão a constraint de aparelho único trataria "sem aparelho" como um aparelho. */
  private String blankToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private MobileUserDto toDto(MobileUser entity, String driverName) {
    return new MobileUserDto(
      entity.getId(),
      entity.getDriverId(),
      driverName,
      entity.getUsername(),
      entity.getDeviceId(),
      entity.getDeviceLabel(),
      entity.getStatus(),
      entity.getLastLoginAt()
    );
  }
}
