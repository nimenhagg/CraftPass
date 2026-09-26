import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../constants/app_colors.dart';
import '../models/inventory_data.dart';
import '../models/item_data.dart';
import '../services/auth_service.dart';
import '../widgets/inventory_grid.dart';
import '../widgets/item_slot_widget.dart';

class InventoryScreen extends StatefulWidget {
  const InventoryScreen({super.key});

  @override
  State<InventoryScreen> createState() => _InventoryScreenState();
}

class _InventoryScreenState extends State<InventoryScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  InventoryData? _inventoryData;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadInventory();
  }

  Future<void> _loadInventory() async {
    final auth = Provider.of<AuthService>(context, listen: false);
    if (!auth.isAuthenticated) return;

    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final inv = await auth.tcpService.getInventory(auth.currentProfile!.token);
      if (mounted) {
        setState(() {
          _inventoryData = inv;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString().replaceAll("Exception: ", "");
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        title: const Text('随身金库与背包', style: TextStyle(color: AppColors.textMain, fontSize: 17, fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: AppColors.textMain),
            tooltip: '刷新背包数据',
            onPressed: _loadInventory,
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppColors.primary,
          labelColor: AppColors.primary,
          unselectedLabelColor: AppColors.textMuted,
          tabs: const [
            Tab(text: '玩家背包 & 装备'),
            Tab(text: '末影箱 (27格)'),
          ],
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.error_outline, color: AppColors.redstone, size: 48),
                      const SizedBox(height: 12),
                      Text(_error!, style: const TextStyle(color: AppColors.redstone)),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _loadInventory,
                        style: ElevatedButton.styleFrom(backgroundColor: AppColors.primary),
                        child: const Text('重试'),
                      ),
                    ],
                  ),
                )
              : _inventoryData == null
                  ? const Center(child: Text('无背包数据', style: TextStyle(color: AppColors.textMuted)))
                  : Column(
                      children: [
                        // Status Bar (Online/Offline, HP, Food, Exp)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                          color: AppColors.card,
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceAround,
                            children: [
                              Row(
                                children: [
                                  Icon(
                                    Icons.circle,
                                    size: 10,
                                    color: _inventoryData!.online ? AppColors.primary : AppColors.textMuted,
                                  ),
                                  const SizedBox(width: 6),
                                  Text(
                                    _inventoryData!.online ? '游戏内在线' : '离线数据',
                                    style: TextStyle(
                                      color: _inventoryData!.online ? AppColors.primary : AppColors.textMuted,
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ],
                              ),
                              Row(
                                children: [
                                  const Icon(Icons.favorite, color: AppColors.redstone, size: 16),
                                  const SizedBox(width: 4),
                                  Text(
                                    '${_inventoryData!.health.toInt()}/${_inventoryData!.maxHealth.toInt()}',
                                    style: const TextStyle(color: AppColors.textMain, fontSize: 13, fontWeight: FontWeight.bold),
                                  ),
                                ],
                              ),
                              Row(
                                children: [
                                  const Icon(Icons.fastfood, color: AppColors.gold, size: 16),
                                  const SizedBox(width: 4),
                                  Text(
                                    '${_inventoryData!.foodLevel}/20',
                                    style: const TextStyle(color: AppColors.textMain, fontSize: 13, fontWeight: FontWeight.bold),
                                  ),
                                ],
                              ),
                              Row(
                                children: [
                                  const Icon(Icons.star, color: AppColors.primary, size: 16),
                                  const SizedBox(width: 4),
                                  Text(
                                    'Lv.${_inventoryData!.expLevel}',
                                    style: const TextStyle(color: AppColors.primary, fontSize: 13, fontWeight: FontWeight.bold),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),

                        // Tab Views
                        Expanded(
                          child: TabBarView(
                            controller: _tabController,
                            children: [
                              // Tab 1: Main Inventory
                              InventoryGrid(data: _inventoryData!),

                              // Tab 2: Ender Chest
                              _buildEnderChestGrid(_inventoryData!.enderchest),
                            ],
                          ),
                        ),
                      ],
                    ),
    );
  }

  Widget _buildEnderChestGrid(List<ItemData> enderList) {
    final Map<int, ItemData> enderMap = {};
    for (var it in enderList) {
      enderMap[it.slot] = it;
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('末影箱私密容器 (跨维度存储)', style: TextStyle(color: AppColors.textMuted, fontSize: 13, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppColors.purple.withOpacity(0.6), width: 1.5),
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
                return ItemSlotWidget(item: enderMap[index]);
              },
            ),
          ),
        ],
      ),
    );
  }
}
