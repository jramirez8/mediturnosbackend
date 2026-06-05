package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.FeedbackTurno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackTurnoRepository extends JpaRepository<FeedbackTurno, Long> {
    Optional<FeedbackTurno> findByTurnoId(Long turnoId);
    List<FeedbackTurno> findTop50ByOrderByCreadoEnDesc();
}
