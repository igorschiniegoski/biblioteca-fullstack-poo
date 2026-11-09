package br.edu.biblioteca.dto;

import java.math.BigDecimal;

public record PagamentoMultaResponse(Long leitorId, BigDecimal valorPago) {
}
