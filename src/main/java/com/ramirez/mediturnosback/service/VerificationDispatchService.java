package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.TurnoResponse;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class VerificationDispatchService {

    public void enviarValidacionEmail(Usuario usuario, Paciente paciente, String verificationUrl) {
        System.out.println("========================================");
        System.out.println("SIMULACION ENVIO EMAIL DE VERIFICACION");
        System.out.println("Para: " + usuario.getEmail());
        System.out.println("Paciente: " + paciente.getNombre() + " " + paciente.getApellido());
        System.out.println("Link de verificacion: " + verificationUrl);
        System.out.println("========================================");
    }

    public void enviarValidacionWhatsapp(Paciente paciente, String verificationCode) {
        System.out.println("========================================");
        System.out.println("SIMULACION ENVIO WHATSAPP");
        System.out.println("Para: " + paciente.getTelefono());
        System.out.println("Paciente: " + paciente.getNombre() + " " + paciente.getApellido());
        System.out.println("Codigo sugerido: " + verificationCode);
        System.out.println("========================================");
    }

    public void enviarRecuperacionEmail(Usuario usuario, String resetToken) {
        System.out.println("========================================");
        System.out.println("SIMULACION ENVIO EMAIL DE RECUPERACION");
        System.out.println("Para: " + usuario.getEmail());
        System.out.println("Token de recuperación: " + resetToken);
        System.out.println("Usar en POST /api/auth/reset-password con body:");
        System.out.println("========================================");
    }

    public void enviarConfirmacionTurno(TurnoResponse turno) {
        System.out.println("========================================");
        System.out.println("SIMULACION ENVIO EMAIL DE CONFIRMACION DE TURNO");
        System.out.println("Paciente: " + turno.getPacienteApellido() + ", " + turno.getPacienteNombre());
        System.out.println("Profesional: " + turno.getProfesionalApellido() + ", " + turno.getProfesionalNombre());
        System.out.println("Especialidad: " + turno.getEspecialidad());
        System.out.println("Institucion: " + turno.getInstitucionNombre());
        System.out.println("Direccion: " + turno.getDireccionAtencion());
        System.out.println("Fecha y hora: " + turno.getFechaHora());
        System.out.println("========================================");
    }
}
