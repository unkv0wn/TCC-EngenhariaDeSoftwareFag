-- Preenche `delivered_at` dos pedidos entregues antes da V19.
--
-- A fonte é `order_status_history`, que é a única que existe para eles: o horário em que
-- o servidor registrou a entrega. **Não é o mesmo que o horário em que a entrega
-- aconteceu** — para um registro feito sem sinal, o histórico marca a sincronização.
--
-- Essa precisão nunca foi gravada para esses pedidos e não há como recuperá-la. Daqui
-- para frente o valor vem do `occurredAt` do evento do motorista (ver RouteOrderLifecycle),
-- que preserva o horário real. Ou seja: linhas antigas trazem "quando foi registrado",
-- linhas novas trazem "quando aconteceu", e isso é o melhor disponível para cada uma.
--
-- MAX() porque um pedido pode ter voltado a `faturado` ("retorno à empresa") e sido
-- entregue de novo; vale a última entrega.
UPDATE orders o
SET delivered_at = (
  SELECT MAX(h.changed_at)
  FROM order_status_history h
  WHERE h.order_id = o.id
    AND h.status = 'entregue'
)
WHERE o.status = 'entregue'
  AND o.delivered_at IS NULL;
