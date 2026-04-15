package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.EstadoTurno;
import com.ramirez.mediturnosback.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByPacienteIdOrderByFechaHoraInicioDesc(Long pacienteId);
    List<Turno> findByPacienteUsuario_IdOrderByFechaHoraInicioDesc(Long usuarioId);
    List<Turno> findByPacienteUsuario_IdAndEstadoOrderByFechaHoraInicioDesc(Long usuarioId, EstadoTurno estado);

    @Query("select t from Turno t where t.profesionalInstitucion.id = :piId and t.fechaHoraInicio between :from and :to order by t.fechaHoraInicio asc")
    List<Turno> findByProfesionalInstitucionIdAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(@Param("piId") Long profesionalInstitucionId,
                                                                                                  @Param("from") LocalDateTime from,
                                                                                                  @Param("to") LocalDateTime to);

    @Query("select t from Turno t where t.profesional.id = :profesionalId and t.fechaHoraInicio between :from and :to order by t.fechaHoraInicio asc")
    List<Turno> findByProfesionalIdAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(@Param("profesionalId") Long profesionalId,
                                                                                       @Param("from") LocalDateTime from,
                                                                                       @Param("to") LocalDateTime to);

    @Query("select t from Turno t where t.profesional.usuario.id = :usuarioId and t.fechaHoraInicio between :from and :to order by t.fechaHoraInicio asc")
    List<Turno> findAgendaProfesional(@Param("usuarioId") Long usuarioId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("select t from Turno t join t.paciente p where p.dni = :dni and t.estado = com.ramirez.mediturnosback.model.EstadoTurno.ATENDIDO order by t.fechaHoraInicio desc")
    List<Turno> findHistoriaPorDni(@Param("dni") String dni);

    Optional<Turno> findFirstByProfesionalUsuario_IdAndFechaHoraInicioAfterAndEstadoInOrderByFechaHoraInicioAsc(Long usuarioId, LocalDateTime now, List<EstadoTurno> estados);

    @Query("select t from Turno t where t.profesionalInstitucion.institucion.id = :institucionId and t.fechaHoraInicio between :from and :to order by t.fechaHoraInicio asc")
    List<Turno> findAgendaInstitucion(@Param("institucionId") Long institucionId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
