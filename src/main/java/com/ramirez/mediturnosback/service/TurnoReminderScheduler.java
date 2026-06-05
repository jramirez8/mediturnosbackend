package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.TurnoResponse;
import com.ramirez.mediturnosback.model.Turno;
import com.ramirez.mediturnosback.repository.TurnoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TurnoReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(TurnoReminderScheduler.class);
    private final TurnoRepository turnoRepository;
    private final TurnoService turnoService;
    private final VerificationDispatchService verificationDispatchService;
    private final AuditService auditService;

    public TurnoReminderScheduler(TurnoRepository turnoRepository, TurnoService turnoService, VerificationDispatchService verificationDispatchService, AuditService auditService) {
        this.turnoRepository = turnoRepository;
        this.turnoService = turnoService;
        this.verificationDispatchService = verificationDispatchService;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${app.turnos.recordatorios.delay-ms:600000}", initialDelayString = "${app.turnos.recordatorios.initial-delay-ms:60000}")
    @Transactional
    public void enviarRecordatoriosTresHoras() {
        LocalDateTime from = LocalDateTime.now().plusHours(3).minusMinutes(10);
        LocalDateTime to = LocalDateTime.now().plusHours(3).plusMinutes(10);
        var turnos = turnoRepository.findTurnosParaRecordatorioTresHoras(from, to);
        for (Turno turno : turnos) {
            String email = turno.getPaciente() != null && turno.getPaciente().getUsuario() != null ? turno.getPaciente().getUsuario().getEmail() : null;
            TurnoResponse response = turnoService.obtenerPorId(turno.getId());
            boolean enviado = verificationDispatchService.enviarRecordatorioTresHoras(response, email);
            if (enviado) {
                turno.setRecordatorioTresHorasEnviado(true);
                turno.setRecordatorioTresHorasEnviadoEn(LocalDateTime.now());
                auditService.registrar("TURNO_RECORDATORIO_3H", "turnos", turno.getId(), "sistema", "Recordatorio 3 horas enviado");
            }
        }
        if (!turnos.isEmpty()) log.info("Recordatorios 3h procesados: {}", turnos.size());
    }
}
