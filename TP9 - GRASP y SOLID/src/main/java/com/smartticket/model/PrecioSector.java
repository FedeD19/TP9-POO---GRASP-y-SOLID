package com.smartticket.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Precio de un sector para UN evento concreto. No es atributo de Sector. */
@Entity
@Table(name = "precios_sector", uniqueConstraints = {
    @UniqueConstraint(name = "uk_precio_evento_sector", columnNames = {"evento_id", "sector_id"})
})
public class PrecioSector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    public PrecioSector() {}

    public PrecioSector(Evento evento, Sector sector, BigDecimal precio) {
        this.evento = evento;
        this.sector = sector;
        this.precio = precio;
    }

    public Long getId() { return id; }
    public Evento getEvento() { return evento; }
    public Sector getSector() { return sector; }
    public BigDecimal getPrecio() { return precio; }
}
