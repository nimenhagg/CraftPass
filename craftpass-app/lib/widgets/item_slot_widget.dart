import 'package:flutter/material.dart';
import '../constants/app_colors.dart';
import '../models/item_data.dart';
import 'item_detail_dialog.dart';

class ItemSlotWidget extends StatelessWidget {
  final ItemData? item;
  final String? placeholderLabel;

  const ItemSlotWidget({
    super.key,
    this.item,
    this.placeholderLabel,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        if (item != null) {
          showDialog(
            context: context,
            builder: (ctx) => ItemDetailDialog(item: item!),
          );
        }
      },
      child: Container(
        width: 44,
        height: 44,
        decoration: BoxDecoration(
          color: AppColors.slotBackground,
          borderRadius: BorderRadius.circular(4),
          border: Border.all(
            color: item != null && item!.enchants.isNotEmpty
                ? AppColors.purple.withOpacity(0.6)
                : AppColors.border,
            width: 1.5,
          ),
        ),
        child: item == null
            ? Center(
                child: Text(
                  placeholderLabel ?? '',
                  style: TextStyle(
                    color: AppColors.textMuted.withOpacity(0.4),
                    fontSize: 10,
                  ),
                ),
              )
            : Stack(
                children: [
                  // Item Name Abbreviation / Icon placeholder
                  Center(
                    child: Padding(
                      padding: const EdgeInsets.all(2.0),
                      child: Text(
                        _getItemShortCode(item!.id),
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: item!.enchants.isNotEmpty ? AppColors.purple : AppColors.textMain,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),

                  // Durability indicator bar at bottom
                  if (item!.isDamaged)
                    Positioned(
                      left: 2,
                      right: 2,
                      bottom: 2,
                      child: Container(
                        height: 3,
                        decoration: BoxDecoration(
                          color: Colors.black50,
                          borderRadius: BorderRadius.circular(1),
                        ),
                        child: FractionallySizedBox(
                          alignment: Alignment.centerLeft,
                          widthFactor: item!.durabilityPercent,
                          child: Container(
                            decoration: BoxDecoration(
                              color: item!.durabilityPercent > 0.5
                                  ? AppColors.primary
                                  : (item!.durabilityPercent > 0.2 ? AppColors.gold : AppColors.redstone),
                              borderRadius: BorderRadius.circular(1),
                            ),
                          ),
                        ),
                      ),
                    ),

                  // Count indicator in bottom right
                  if (item!.count > 1)
                    Positioned(
                      right: 2,
                      bottom: 1,
                      child: Text(
                        '${item!.count}',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 11,
                          fontWeight: FontWeight.w900,
                          shadows: [
                            Shadow(color: Colors.black, blurRadius: 2, offset: Offset(1, 1)),
                          ],
                        ),
                      ),
                    ),
                ],
              ),
      ),
    );
  }

  String _getItemShortCode(String id) {
    final clean = id.replaceFirst('minecraft:', '');
    final parts = clean.split('_');
    if (parts.length == 1) {
      return parts[0].substring(0, parts[0].length > 4 ? 4 : parts[0].length).toUpperCase();
    }
    return parts.map((p) => p.isNotEmpty ? p[0].toUpperCase() : '').take(3).join();
  }
}
