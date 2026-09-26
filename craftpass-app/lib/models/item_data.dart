class ItemData {
  final int slot;
  final String id;
  final int count;
  final String displayName;
  final List<String> lore;
  final Map<String, int> enchants;
  final int damage;
  final int maxDamage;

  ItemData({
    required this.slot,
    required this.id,
    required this.count,
    required this.displayName,
    required this.lore,
    required this.enchants,
    required this.damage,
    required this.maxDamage,
  });

  factory ItemData.fromJson(Map<String, dynamic> json) {
    final rawEnchants = json['enchants'] as Map<String, dynamic>? ?? {};
    final Map<String, int> enchantsMap = {};
    rawEnchants.forEach((k, v) {
      enchantsMap[k] = (v as num).toInt();
    });

    final rawLore = json['lore'] as List<dynamic>? ?? [];
    final List<String> loreList = rawLore.map((e) => e.toString()).toList();

    return ItemData(
      slot: (json['slot'] as num?)?.toInt() ?? 0,
      id: json['id'] as String? ?? '',
      count: (json['count'] as num?)?.toInt() ?? 1,
      displayName: json['displayName'] as String? ?? '',
      lore: loreList,
      enchants: enchantsMap,
      damage: (json['damage'] as num?)?.toInt() ?? 0,
      maxDamage: (json['maxDamage'] as num?)?.toInt() ?? 0,
    );
  }

  bool get isDamaged => maxDamage > 0 && damage > 0;
  double get durabilityPercent => maxDamage > 0 ? (1.0 - (damage / maxDamage)).clamp(0.0, 1.0) : 1.0;
}
