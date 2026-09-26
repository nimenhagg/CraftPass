import 'package:flutter/material.dart';
import '../constants/app_colors.dart';
import '../models/inventory_data.dart';
import '../models/item_data.dart';
import 'item_slot_widget.dart';

class InventoryGrid extends StatelessWidget {
  final InventoryData data;

  const InventoryGrid({super.key, required this.data});

  @override
  Widget build(BuildContext context) {
    // Map items to slot positions
    final Map<int, ItemData> mainMap = {};
    for (var item in data.inventory) {
      mainMap[item.slot] = item;
    }

    final Map<int, ItemData> armorMap = {};
    for (var item in data.armor) {
      armorMap[item.slot] = item;
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Equipment Section (Armor & Offhand)
          const Text('装备与副手', style: TextStyle(color: AppColors.textMuted, fontSize: 13, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppColors.border),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                ItemSlotWidget(item: armorMap[103] ?? armorMap[39], placeholderLabel: '头盔'),
                ItemSlotWidget(item: armorMap[102] ?? armorMap[38], placeholderLabel: '胸甲'),
                ItemSlotWidget(item: armorMap[101] ?? armorMap[37], placeholderLabel: '护腿'),
                ItemSlotWidget(item: armorMap[100] ?? armorMap[36], placeholderLabel: '靴子'),
                Container(width: 1, height: 40, color: AppColors.border),
                ItemSlotWidget(item: data.offhand, placeholderLabel: '副手'),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Main Inventory (Slots 9-35: 3 rows of 9)
          const Text('背包物品 (27格)', style: TextStyle(color: AppColors.textMuted, fontSize: 13, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppColors.border),
            ),
            child: GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: 27,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 9,
                crossAxisSpacing: 6,
                mainAxisSpacing: 6,
              ),
              itemBuilder: (ctx, index) {
                final slot = index + 9; // slots 9 to 35
                return ItemSlotWidget(item: mainMap[slot]);
              },
            ),
          ),
          const SizedBox(height: 16),

          // Hotbar (Slots 0-8: 1 row of 9)
          const Text('快捷栏 (9格)', style: TextStyle(color: AppColors.textMuted, fontSize: 13, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppColors.primary.withOpacity(0.5), width: 1.5),
            ),
            child: GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: 9,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 9,
                crossAxisSpacing: 6,
                mainAxisSpacing: 6,
              ),
              itemBuilder: (ctx, index) {
                return ItemSlotWidget(item: mainMap[index]);
              },
            ),
          ),
        ],
      ),
    );
  }
}
