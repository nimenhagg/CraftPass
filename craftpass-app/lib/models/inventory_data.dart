import 'item_data.dart';

class InventoryData {
  final bool online;
  final double health;
  final double maxHealth;
  final int foodLevel;
  final int expLevel;
  final List<ItemData> inventory;
  final List<ItemData> armor;
  final ItemData? offhand;
  final List<ItemData> enderchest;

  InventoryData({
    required this.online,
    required this.health,
    required this.maxHealth,
    required this.foodLevel,
    required this.expLevel,
    required this.inventory,
    required this.armor,
    this.offhand,
    required this.enderchest,
  });

  factory InventoryData.fromJson(Map<String, dynamic> json) {
    final invList = (json['inventory'] as List<dynamic>? ?? [])
        .map((e) => ItemData.fromJson(e as Map<String, dynamic>))
        .toList();

    final armorList = (json['armor'] as List<dynamic>? ?? [])
        .map((e) => ItemData.fromJson(e as Map<String, dynamic>))
        .toList();

    ItemData? offhandItem;
    if (json['offhand'] != null && json['offhand'] is Map<String, dynamic>) {
      offhandItem = ItemData.fromJson(json['offhand'] as Map<String, dynamic>);
    }

    final enderList = (json['enderchest'] as List<dynamic>? ?? [])
        .map((e) => ItemData.fromJson(e as Map<String, dynamic>))
        .toList();

    return InventoryData(
      online: json['online'] as bool? ?? false,
      health: (json['health'] as num?)?.toDouble() ?? 20.0,
      maxHealth: (json['maxHealth'] as num?)?.toDouble() ?? 20.0,
      foodLevel: (json['foodLevel'] as num?)?.toInt() ?? 20,
      expLevel: (json['expLevel'] as num?)?.toInt() ?? 0,
      inventory: invList,
      armor: armorList,
      offhand: offhandItem,
      enderchest: enderList,
    );
  }
}
