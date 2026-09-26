import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../constants/app_colors.dart';
import '../services/auth_service.dart';

class MigrationScreen extends StatefulWidget {
  const MigrationScreen({super.key});

  @override
  State<MigrationScreen> createState() => _MigrationScreenState();
}

class _MigrationScreenState extends State<MigrationScreen> {
  final _targetUsernameController = TextEditingController();
  bool _isLoading = false;
  String? _message;
  bool _isSuccess = false;

  Future<void> _handleMigrate() async {
    final targetName = _targetUsernameController.text.trim();
    if (targetName.isEmpty) return;

    final auth = Provider.of<AuthService>(context, listen: false);
    if (!auth.isAuthenticated) return;

    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.card,
        title: const Text('确认账号数据平移', style: TextStyle(color: AppColors.textMain, fontWeight: FontWeight.bold)),
        content: Text(
          '确定要将当前账号 [${auth.currentProfile!.username}] 的所有数据（包含背包、末影箱、多世界背包、粘液科技进度等）原子平移至新用户名 [$targetName] 吗？\n\n系统将自动生成安全回滚快照。',
          style: const TextStyle(color: AppColors.textMuted, fontSize: 13),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('取消')),
          ElevatedButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.primary),
            child: const Text('确认迁移'),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    setState(() {
      _isLoading = true;
      _message = null;
    });

    try {
      final resp = await auth.tcpService.migrateAccount(auth.currentProfile!.token, targetName);
      final success = resp['status'] == 'SUCCESS';
      setState(() {
        _isSuccess = success;
        _message = resp['message'] ?? (success ? '数据迁移成功！' : '迁移失败');
        _isLoading = false;
      });
      if (success) {
        _targetUsernameController.clear();
      }
    } catch (e) {
      setState(() {
        _isSuccess = false;
        _message = e.toString().replaceAll("Exception: ", "");
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = Provider.of<AuthService>(context);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        title: const Text('自助换名数据平移', style: TextStyle(color: AppColors.textMain, fontSize: 17, fontWeight: FontWeight.bold)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Current User Banner
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                children: [
                  const Icon(Icons.account_circle, color: AppColors.primary, size: 40),
                  const SizedBox(width: 14),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('当前源账号', style: TextStyle(color: AppColors.textMuted, fontSize: 12)),
                      Text(
                        auth.currentProfile?.username ?? '',
                        style: const TextStyle(color: AppColors.textMain, fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Features Checklist
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.diamond.withOpacity(0.3)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('一键平移范围:', style: TextStyle(color: AppColors.diamond, fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 8),
                  _buildCheckItem('主世界/末影箱/血量/经验等级 (world/playerdata)'),
                  _buildCheckItem('成就进度与统计统计数据 (stats/advancements)'),
                  _buildCheckItem('Multiverse 多世界分组独立背包'),
                  _buildCheckItem('Slimefun 粘液科技解锁配方与背包数据'),
                  _buildCheckItem('LoginSecurity 登录认证信息与密码'),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Feedback Message
            if (_message != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: _isSuccess ? AppColors.primary.withOpacity(0.15) : AppColors.redstone.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: _isSuccess ? AppColors.primary : AppColors.redstone),
                ),
                child: Text(
                  _message!,
                  style: TextStyle(color: _isSuccess ? AppColors.primary : AppColors.redstone, fontSize: 13),
                ),
              ),
              const SizedBox(height: 16),
            ],

            // Target New Username Input
            TextField(
              controller: _targetUsernameController,
              style: const TextStyle(color: AppColors.textMain),
              decoration: InputDecoration(
                labelText: '目标新游戏名字 (New Username)',
                labelStyle: const TextStyle(color: AppColors.textMuted),
                prefixIcon: const Icon(Icons.badge, color: AppColors.textMuted),
                filled: true,
                fillColor: AppColors.surface,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
              ),
            ),
            const SizedBox(height: 20),

            // Migrate Button
            ElevatedButton(
              onPressed: _isLoading ? null : _handleMigrate,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.diamond,
                foregroundColor: Colors.black,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
              child: _isLoading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(color: Colors.black, strokeWidth: 2),
                    )
                  : const Text('开始数据平移', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCheckItem(String title) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2.0),
      child: Row(
        children: [
          const Icon(Icons.check_circle, color: AppColors.primary, size: 16),
          const SizedBox(width: 8),
          Expanded(child: Text(title, style: const TextStyle(color: AppColors.textMain, fontSize: 12))),
        ],
      ),
    );
  }
}
