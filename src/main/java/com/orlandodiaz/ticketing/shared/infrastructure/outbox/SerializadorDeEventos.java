package com.orlandodiaz.ticketing.shared.infrastructure.outbox;

import com.orlandodiaz.ticketing.shared.domain.EventoDeDominio;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tools.jackson.databind.json.JsonMapper;

/**
 * Convierte eventos a JSON (para guardarlos en el outbox) y de vuelta a objetos (para entregarlos).
 *
 * Cada tipo de evento se REGISTRA con su nombre simple (ej. "OrdenConfirmada"). Ese nombre es lo que queda
 * guardado en la base: asi se puede mover una clase de paquete sin dejar huerfanos los mensajes pendientes.
 * Por eso esta clase no conoce los modulos: quien arma la aplicacion (configuracion) le dice que eventos existen.
 */
public class SerializadorDeEventos {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<String, Class<? extends EventoDeDominio>> tipos = new ConcurrentHashMap<>();

    public SerializadorDeEventos registrar(Class<? extends EventoDeDominio> tipo) {
        Class<? extends EventoDeDominio> previo = tipos.putIfAbsent(tipo.getSimpleName(), tipo);
        if (previo != null && previo != tipo) {
            throw new IllegalArgumentException("Ya hay un evento registrado con el nombre " + tipo.getSimpleName());
        }
        return this;
    }

    public String nombreDe(EventoDeDominio evento) {
        String nombre = evento.getClass().getSimpleName();
        if (tipos.get(nombre) != evento.getClass()) {
            throw new IllegalArgumentException("Evento sin registrar en el serializador: " + evento.getClass().getName());
        }
        return nombre;
    }

    public String aJson(EventoDeDominio evento) {
        nombreDe(evento); // falla pronto si nadie registro este tipo
        return mapper.writeValueAsString(evento);
    }

    public EventoDeDominio desdeJson(String tipo, String json) {
        Class<? extends EventoDeDominio> clase = tipos.get(tipo);
        if (clase == null) {
            throw new IllegalArgumentException("Tipo de evento desconocido: " + tipo);
        }
        return mapper.readValue(json, clase);
    }
}
