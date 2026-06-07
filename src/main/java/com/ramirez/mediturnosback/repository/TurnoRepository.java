package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.EstadoTurno;
import com.ramirez.mediturnosback.model.Turno;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    @EntityGraph(attributePaths = {"adjuntos", "consulta"})
    List<Turno> findByPacienteIdOrderByFechaHoraInicioDesc(Long pacienteId);

    @EntityGraph(attributePaths = {"adjuntos", "consulta"})
    List<Turno> findByPacienteUsuario_IdOrderByFechaHoraInicioDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"adjuntos", "consulta"})
    List<Turno> findByPacienteUsuario_IdAndEstadoOrderByFechaHoraInicioDesc(Long usuarioId, EstadoTurno estado);

    @Query("select t from Turno t left join fetch t.adjuntos left join fetch t.consulta where t.profesionalInstitucion.id = :piId and t.fechaHoraInicio between :from and :to order by t.fechaHoraInicio asc")
    List<Turno> findByProfesionalInstitucionIdAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(@Param("piId") Long profesionalInstitucionId,
                                                                                                  @Param("from") LocalDateTime from,
                                                                                                  @Param("to") LocalDateTime to);

    @Query("select t from Turno t left join fetch t.adjuntos left join fetch t.consulta where t.profesional.id = :profesionalId and t.fechaHoraInicio between :from and :to order by t.fechaHoraInicio asc")
    List<Turno> findByProfesionalIdAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(@Param("profesionalId") Long profesionalId,
                                                                                       @Param("from") LocalDateTime from,
                                                                                       @Param("to") LocalDateTime to);

    @Query("select t from Turno t left join fetch t.adjuntos left join fetch t.consulta where t.profesional.usuario.id = :usuarioId and t.fechaHoraInicio between :from and :to order by t.fechaHoraInicio asc")
    List<Turno> findAgendaProfesional(@Param("usuarioId") Long usuarioId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("select t from Turno t left join fetch t.adjuntos left join fetch t.consulta join t.paciente p where p.dni = :dni and t.estado = com.ramirez.mediturnosback.model.EstadoTurno.ATENDIDO order by t.fechaHoraInicio desc")
    List<Turno> findHistoriaPorDni(@Param("dni") String dni);

    @Query("select t from Turno t left join fetch t.adjuntos left join fetch t.consulta join t.paciente p where p.dni = :dni and t.profesional.id = :profesionalId and t.estado = com.ramirez.mediturnosback.model.EstadoTurno.ATENDIDO order by t.fechaHoraInicio desc")
    List<Turno> findHistoriaPorDniAndProfesionalId(@Param("dni") String dni, @Param("profesionalId") Long profesionalId);

    @EntityGraph(attributePaths = {"adjuntos", "consulta"})
    Optional<Turno> findFirstByProfesionalUsuario_IdAndFechaHoraInicioAfterAndEstadoInOrderByFechaHoraInicioAsc(Long usuarioId, LocalDateTime now, List<EstadoTurno> estados);

    @Query("select t from Turno t left join fetch t.adjuntos left join fetch t.consulta where t.profesionalInstitucion.institucion.id = :institucionId and t.fechaHoraInicio between :from and :to order by t.fechaHoraInicio asc")
    List<Turno> findAgendaInstitucion(@Param("institucionId") Long institucionId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select t from Turno t left join fetch t.paciente p left join fetch p.usuario where t.fechaHoraInicio between :from and :to and (t.recordatorioTresHorasEnviado = false or t.recordatorioTresHorasEnviado is null) and t.estado in (com.ramirez.mediturnosback.model.EstadoTurno.CONFIRMADO, com.ramirez.mediturnosback.model.EstadoTurno.REPROGRAMADO)")
    List<Turno> findTurnosParaRecordatorioTresHoras(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
