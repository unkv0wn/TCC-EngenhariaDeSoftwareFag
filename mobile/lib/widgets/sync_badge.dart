import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Selo de eventos ainda não sincronizados.
///
/// Fica visível em todas as telas de propósito: o motorista precisa saber que o registro
/// existe mas ainda não subiu. Sem isso ele registra de novo achando que não pegou — e
/// é exatamente esse reenvio que o `clientEventId` protege do outro lado.
class SyncBadge extends StatelessWidget {
  const SyncBadge({super.key, required this.pendingCount});

  final int pendingCount;

  @override
  Widget build(BuildContext context) {
    if (pendingCount == 0) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppColors.warning500,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.schedule, size: 13, color: AppColors.warning800),
          const SizedBox(width: 6),
          Text(
            pendingCount == 1 ? '1 na fila' : '$pendingCount na fila',
            style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: AppColors.warning800),
          ),
        ],
      ),
    );
  }
}
