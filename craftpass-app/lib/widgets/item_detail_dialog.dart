import 'package:flutter/material.dart';
import '../constants/app_colors.dart';
import '../models/item_data.dart';

class ItemDetailDialog extends StatelessWidget {
  final ItemData item;

  const ItemDetailDialog({super.key, required this.item});

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: AppColors.card,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.border, width: 1.5),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Item Title & Count
            Row(
              children: [
                Expanded(
                  child: Text(
                    item.displayName,
                    style: TextStyle(
                      color: item.enchants.isNotEmpty ? AppColors.purple : AppColors.gold,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.slotBackground,
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: AppColors.border),
                  ),
                  child: Text(
                    'x${item.count}',
                    style: const TextStyle(
                      color: AppColors.textMain,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),

            // Namespace ID
            Text(
              item.id,
              style: const TextStyle(
                color: AppColors.textMuted,
                fontSize: 12,
                fontFamily: 'monospace',
              ),
            ),
            const Divider(color: AppColors.border, height: 24),

            // Durability Bar (if damaged or damageable)
            if (item.maxDamage > 0) ...[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('耐久度', style: TextStyle(color: AppColors.textMuted, fontSize: 13)),
                  Text(
                    '${item.maxDamage - item.damage} / ${item.maxDamage}',
                    style: const TextStyle(color: AppColors.textMain, fontSize: 13, fontWeight: FontWeight.bold),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              ClipRRect(
                borderRadius: BorderRadius.circular(3),
                child: LinearProgressIndicator(
                  value: item.durabilityPercent,
                  backgroundColor: AppColors.slotBackground,
                  valueColor: AlwaysStoppedAnimation(
                    item.durabilityPercent > 0.5
                        ? AppColors.primary
                        : (item.durabilityPercent > 0.2 ? AppColors.gold : AppColors.redstone),
                  ),
                  minHeight: 6,
                ),
              ),
              const SizedBox(height: 16),
            ],

            // Enchantments List
            if (item.enchants.isNotEmpty) ...[
              const Text('附魔属性:', style: TextStyle(color: AppColors.diamond, fontWeight: FontWeight.bold, fontSize: 14)),
              const SizedBox(height: 6),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                children: item.enchants.entries.map((e) {
                  return Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppColors.purple.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: AppColors.purple.withOpacity(0.4)),
                    ),
                    child: Text(
                      '${_formatEnchantName(e.key)} ${_toRoman(e.value)}',
                      style: const TextStyle(color: Color(0xFFE9D5FF), fontSize: 12, fontWeight: FontWeight.bold),
                    ),
                  );
                }).toList(),
              ),
              const SizedBox(height: 16),
            ],

            // Lore / Description
            if (item.lore.isNotEmpty) ...[
              const Text('物品描述 (Lore):', style: TextStyle(color: AppColors.textMuted, fontSize: 13)),
              const SizedBox(height: 6),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.slotBackground,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: item.lore.map((line) => Text(
                    line,
                    style: const TextStyle(color: Color(0xFFD1D5DB), fontSize: 12, fontStyle: FontStyle.italic),
                  )).toList(),
                ),
              ),
              const SizedBox(height: 16),
            ],

            // Close Button
            Align(
              alignment: Alignment.centerRight,
              child: TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('关闭', style: TextStyle(color: AppColors.primary, fontWeight: FontWeight.bold)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatEnchantName(String name) {
    return name.split('_').map((w) => w.isNotEmpty ? w[0].toUpperCase() + w.substring(1) : '').join(' ');
  }

  String _toRoman(int number) {
    const romanMap = {1: 'I', 2: 'II', 3: 'III', 4: 'IV', 5: 'V'};
    return romanMap[number] ?? '$number';
  }
}
