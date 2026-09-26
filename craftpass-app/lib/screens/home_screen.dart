import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../constants/app_colors.dart';
import '../services/auth_service.dart';
import 'change_password_screen.dart';
import 'geyser_link_screen.dart';
import 'inventory_screen.dart';
import 'login_screen.dart';
import 'migration_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  final List<Widget> _pages = const [
    _DashboardTab(),
    InventoryScreen(),
    ChangePasswordScreen(),
    MigrationScreen(),
    GeyserLinkScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: _pages[_currentIndex],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) => setState(() => _currentIndex = index),
        backgroundColor: AppColors.surface,
        indicatorColor: AppColors.primary.withOpacity(0.2),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.dashboard_outlined, color: AppColors.textMuted),
            selectedIcon: Icon(Icons.dashboard, color: AppColors.primary),
            label: '概览',
          ),
          NavigationDestination(
            icon: Icon(Icons.backpack_outlined, color: AppColors.textMuted),
            selectedIcon: Icon(Icons.backpack, color: AppColors.primary),
            label: '背包',
          ),
          NavigationDestination(
            icon: Icon(Icons.lock_outline, color: AppColors.textMuted),
            selectedIcon: Icon(Icons.lock, color: AppColors.primary),
            label: '改密',
          ),
          NavigationDestination(
            icon: Icon(Icons.swap_horiz, color: AppColors.textMuted),
            selectedIcon: Icon(Icons.swap_horizontal_circle, color: AppColors.primary),
            label: '换名',
          ),
          NavigationDestination(
            icon: Icon(Icons.phonelink, color: AppColors.textMuted),
            selectedIcon: Icon(Icons.phonelink_ring, color: AppColors.primary),
            label: '互通',
          ),
        ],
      ),
    );
  }
}

class _DashboardTab extends StatelessWidget {
  const _DashboardTab();

  @override
  Widget build(BuildContext context) {
    final auth = Provider.of<AuthService>(context);
    final profile = auth.currentProfile;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        title: const Text('CraftPass', style: TextStyle(color: AppColors.textMain, fontSize: 17, fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout, color: AppColors.redstone),
            tooltip: '注销退出',
            onPressed: () {
              auth.logout();
              Navigator.of(context).pushReplacement(
                MaterialPageRoute(builder: (_) => const LoginScreen()),
              );
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Character Card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF1E293B), Color(0xFF0F172A)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.primary.withOpacity(0.5), width: 1.5),
              ),
              child: Row(
                children: [
                  Container(
                    width: 56,
                    height: 56,
                    decoration: BoxDecoration(
                      color: AppColors.primary.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: AppColors.primary, width: 2),
                    ),
                    child: const Icon(Icons.person, color: AppColors.primary, size: 36),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          profile?.username ?? '',
                          style: const TextStyle(
                            color: AppColors.textMain,
                            fontSize: 20,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'UUID: ${profile?.uuid ?? ""}',
                          style: const TextStyle(color: AppColors.textMuted, fontSize: 11, fontFamily: 'monospace'),
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            const Icon(Icons.link, color: AppColors.primary, size: 14),
                            const SizedBox(width: 4),
                            Text(
                              '${profile?.serverHost}:${profile?.serverPort} (TCP 直连)',
                              style: const TextStyle(color: AppColors.primary, fontSize: 11),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Quick Modules Grid
            const Text('功能服务面板', style: TextStyle(color: AppColors.textMuted, fontSize: 14, fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),

            _buildModuleTile(
              icon: Icons.backpack,
              color: AppColors.primary,
              title: '随身背包与末影箱',
              subtitle: '查看 36 格背包、4 件装备槽、副手及末影箱珍贵物品',
              onTap: () {
                final state = context.findAncestorStateOfType<_HomeScreenState>();
                state?.setState(() => state._currentIndex = 1);
              },
            ),
            const SizedBox(height: 12),

            _buildModuleTile(
              icon: Icons.lock_reset,
              color: AppColors.gold,
              title: '安全中心 · 密码修改',
              subtitle: '由 Server-Pepper 强加密保护，彻底免疫公开数据库字典爆破',
              onTap: () {
                final state = context.findAncestorStateOfType<_HomeScreenState>();
                state?.setState(() => state._currentIndex = 2);
              },
            ),
            const SizedBox(height: 12),

            _buildModuleTile(
              icon: Icons.move_up,
              color: AppColors.diamond,
              title: '自助改名数据平移',
              subtitle: '换新名字进服后，一键无损平移所有背包与成就进度',
              onTap: () {
                final state = context.findAncestorStateOfType<_HomeScreenState>();
                state?.setState(() => state._currentIndex = 3);
              },
            ),
            const SizedBox(height: 12),

            _buildModuleTile(
              icon: Icons.phonelink,
              color: AppColors.purple,
              title: 'Java ↔ 基岩版双端互通',
              subtitle: '自动绑定 Geyser 前缀账号，下线实时同步，互斥防刷',
              onTap: () {
                final state = context.findAncestorStateOfType<_HomeScreenState>();
                state?.setState(() => state._currentIndex = 4);
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildModuleTile({
    required IconData icon,
    required Color color,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.border),
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: color.withOpacity(0.15),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(icon, color: color, size: 24),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(color: AppColors.textMain, fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 3),
                  Text(subtitle, style: const TextStyle(color: AppColors.textMuted, fontSize: 12)),
                ],
              ),
            ),
            const Icon(Icons.arrow_forward_ios, color: AppColors.textMuted, size: 14),
          ],
        ),
      ),
    );
  }
}
