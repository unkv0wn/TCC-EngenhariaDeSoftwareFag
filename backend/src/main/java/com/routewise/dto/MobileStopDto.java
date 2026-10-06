package com.routewise.dto;

/**
 * Uma parada já resolvida para o app: número, rótulo, coordenada e se foi entregue.
 *
 * <p>O casamento entre o waypoint otimizado e o rótulo do cliente é feito no servidor de
 * propósito. O front do gestor faz isso em {@code lib/routeResult.ts}; deixar o celular
 * refazer criaria uma terceira implementação da mesma regra, em uma terceira linguagem,
 * livre para divergir das outras duas — e o número da parada é justamente o que o
 * motorista usa para se achar no mapa e na conversa com o gestor.
 *
 * @param number      posição na ordem otimizada (1..n); o depósito não entra
 * @param ordersCount quantos pedidos saem nesta parada
 * @param delivered   já existe evento de entrega apontando para ela
 */
public record MobileStopDto(
  int number,
  String label,
  double lat,
  double lng,
  int ordersCount,
  boolean delivered
) {}
