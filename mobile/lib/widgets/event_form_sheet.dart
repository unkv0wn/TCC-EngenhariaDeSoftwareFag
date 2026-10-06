import 'package:flutter/material.dart';

import '../models/route_models.dart';
import '../theme/app_theme.dart';

/// Resultado do formulário: o que o chamador manda pro controller.
class EventFormResult {
  const EventFormResult({required this.type, this.notes});

  final RouteEventType type;
  final String? notes;
}

/// Folha de registro de ocorrência.
///
/// Motivos pré-definidos existem porque digitar de pé, na chuva, com uma mão, é o
/// caminho mais curto pra ninguém registrar nada. O texto livre fica por cima disso,
/// pra quando o motivo não couber na lista.
class EventFormSheet extends StatefulWidget {
  const EventFormSheet({super.key, required this.stopLabel, required this.stopNumber});

  final String stopLabel;
  final int stopNumber;

  static Future<EventFormResult?> show(BuildContext context, {required String stopLabel, required int stopNumber}) {
    return showModalBottomSheet<EventFormResult>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => EventFormSheet(stopLabel: stopLabel, stopNumber: stopNumber),
    );
  }

  @override
  State<EventFormSheet> createState() => _EventFormSheetState();
}

class _EventFormSheetState extends State<EventFormSheet> {
  static const _reasons = [
    'Cliente ausente',
    'Recusa',
    'Endereço errado',
    'Avaria',
    'Acesso bloqueado',
  ];

  final _notesField = TextEditingController();
  String? _reason;

  @override
  void dispose() {
    _notesField.dispose();
    super.dispose();
  }

  void _submit() {
    // Ocorrência sem explicação não serve pra nada do outro lado: o gestor precisa
    // saber o que houve pra decidir se reagenda, cobra ou devolve.
    final notes = [_reason, _notesField.text.trim()].whereType<String>().where((s) => s.isNotEmpty).join(' — ');
    if (notes.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Escolha um motivo ou escreva o que aconteceu.')),
      );
      return;
    }
    Navigator.of(context).pop(EventFormResult(type: RouteEventType.ocorrencia, notes: notes));
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;

    return Padding(
      padding: EdgeInsets.fromLTRB(20, 10, 20, 22 + bottomInset),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(color: AppColors.gray300, borderRadius: BorderRadius.circular(999)),
              ),
            ),
            const SizedBox(height: 18),
            Row(
              children: [
                const Expanded(
                  child: Text(
                    'Registrar ocorrência',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: AppColors.gray900),
                  ),
                ),
                IconButton(
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close, size: 20),
                  tooltip: 'Fechar',
                ),
              ],
            ),
            Text(
              'Parada ${widget.stopNumber} — ${widget.stopLabel}',
              style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: AppColors.gray600),
            ),
            const SizedBox(height: 18),
            const Text(
              'O que aconteceu',
              style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: AppColors.gray700),
            ),
            const SizedBox(height: 9),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _reasons.map(_reasonChip).toList(),
            ),
            const SizedBox(height: 18),
            TextField(
              controller: _notesField,
              maxLines: 3,
              textCapitalization: TextCapitalization.sentences,
              decoration: const InputDecoration(
                labelText: 'Observação',
                hintText: 'Detalhe o que aconteceu',
                alignLabelWithHint: true,
              ),
            ),
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('Cancelar'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  flex: 2,
                  child: FilledButton(onPressed: _submit, child: const Text('Registrar')),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _reasonChip(String reason) {
    final selected = _reason == reason;
    return ChoiceChip(
      label: Text(reason),
      selected: selected,
      onSelected: (value) => setState(() => _reason = value ? reason : null),
      showCheckmark: false,
      labelStyle: TextStyle(
        fontSize: 13,
        fontWeight: FontWeight.w700,
        color: selected ? AppColors.danger700 : AppColors.gray700,
      ),
      backgroundColor: Colors.white,
      selectedColor: AppColors.danger50,
      side: BorderSide(color: selected ? AppColors.danger600 : AppColors.gray200, width: 1.5),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
    );
  }
}
