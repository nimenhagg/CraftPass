import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../constants/app_colors.dart';
import '../services/auth_service.dart';

class GeyserLinkScreen extends StatefulWidget {
  const GeyserLinkScreen({super.key});

  @override
  State<GeyserLinkScreen> createState() => _GeyserLinkScreenState();
}

class _GeyserLinkScreenState extends State<GeyserLinkScreen> {
  bool _isLoading = true;
  bool _isLinked = false;
  String _linkedAccount = '';
  String _prefix = '.';

  final _bedrockNameController = TextEditingController();
  final _bedrockPassController = TextEditingController();

  String? _message;
  bool _isSuccess = false;

  @override
  void initState() {
    super.initState();
    _fetchLinkStatus();
  }

  Future<void> _fetchLinkStatus() async {
    final auth = Provider.of<AuthService>(context, listen: false);
    if (!auth.isAuthenticated) return;

    setState(() {
      _isLoading = true;
      _message = null;
    });

    try {
      final resp = await auth.tcpService.getGeyserLink(auth.currentProfile!.token);
      if (mounted) {
        setState(() {
          _isLinked = resp['isLinked'] == true;
          _linkedAccount = resp['linkedAccount'] ?? '';
          _prefix = resp['bedrockPrefix'] ?? '.';
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _message = e.toString().replaceAll("Exception: ", "");
        });
      }
    }
  }

  Future<void> _handleLink() async {
    final bedrockName = _bedrockNameController.text.trim();
    final bedrockPass = _bedrockPassController.text;
    if (bedrockName.isEmpty || bedrockPass.isEmpty) return;

    final auth = Provider.of<AuthService>(context, listen: false);
    if (!auth.isAuthenticated) return;

    setState(() {
      _isLoading = true;
      _message = null;
    });

    try {
      final resp = await auth.tcpService.linkGeyser(auth.currentProfile!.token, bedrockName, bedrockPass);
      final ok = resp['status'] == 'SUCCESS';
      setState(() {
        _isSuccess = ok;
        _message = resp['message'] ?? (ok ? '绑定成功！' : '绑定失败');
      });
      if (ok) {
        _bedrockNameController.clear();
        _bedrockPassController.clear();
        await _fetchLinkStatus();
      } else {
        setState(() => _isLoading = false);
      }
    } catch (e) {
      setState(() {
        _isLoading = false;
        _isSuccess = false;
        _message = e.toString().replaceAll("Exception: ", "");
      });
    }
  }

  Future<void> _handleUnlink() async {
    final auth = Provider.of<AuthService>(context, listen: false);
    if (!auth.isAuthenticated) return;

    setState(() {
      _isLoading = true;
      _message = null;
    });

    try {
      final resp = await auth.tcpService.unlinkGeyser(auth.currentProfile!.token);
      final ok = resp['status'] == 'SUCCESS';
      setState(() {
        _isSuccess = ok;
        _message = resp['message'] ?? (ok ? '已解除绑定！' : '解绑失败');
      });
      await _fetchLinkStatus();
    } catch (e) {
      setState(() {
        _isLoading = false;
        _isSuccess = false;
        _message = e.toString().replaceAll("Exception: ", "");
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
        title: const Text('双端互通 (Geyser 基岩版)', style: TextStyle(color: AppColors.textMain, fontSize: 17, fontWeight: FontWeight.bold)),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Status Banner
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppColors.surface,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: _isLinked ? AppColors.primary : AppColors.border,
                        width: 1.5,
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(
                              _isLinked ? Icons.link : Icons.link_off,
                              color: _isLinked ? AppColors.primary : AppColors.textMuted,
                              size: 28,
                            ),
                            const SizedBox(width: 12),
                            Text(
                              _isLinked ? '已绑定互通基岩版账号' : '尚未绑定基岩版账号',
                              style: TextStyle(
                                color: _isLinked ? AppColors.primary : AppColors.textMuted,
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        if (_isLinked) ...[
                          const SizedBox(height: 12),
                          Text('Java 版角色: ${auth.currentProfile?.username}', style: const TextStyle(color: AppColors.textMain, fontSize: 14)),
                          const SizedBox(height: 4),
                          Text('基岩版角色: $_linkedAccount', style: const TextStyle(color: AppColors.diamond, fontSize: 14, fontWeight: FontWeight.bold)),
                        ],
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Info Card
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: AppColors.card,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('双端互通说明:', style: TextStyle(color: AppColors.gold, fontWeight: FontWeight.bold, fontSize: 13)),
                        const SizedBox(height: 6),
                        const Text('• 基岩版进服名字默认携带前缀 (如 .Steve)', style: TextStyle(color: AppColors.textMuted, fontSize: 12)),
                        const SizedBox(height: 4),
                        const Text('• 绑定后，任意一端下线时，系统将自动将最新的背包、末影箱与进度镜像至另一端', style: TextStyle(color: AppColors.textMuted, fontSize: 12)),
                        const SizedBox(height: 4),
                        const Text('• 互斥在线保护：防止两端同时在线造成数据冲突与刷物品', style: TextStyle(color: AppColors.textMuted, fontSize: 12)),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Message
                  if (_message != null) ...[
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: _isSuccess ? AppColors.primary.withOpacity(0.15) : AppColors.redstone.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: _isSuccess ? AppColors.primary : AppColors.redstone),
                      ),
                      child: Text(_message!, style: TextStyle(color: _isSuccess ? AppColors.primary : AppColors.redstone, fontSize: 13)),
                    ),
                    const SizedBox(height: 16),
                  ],

                  if (_isLinked) ...[
                    // Unlink Button
                    ElevatedButton(
                      onPressed: _handleUnlink,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.redstone,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                      child: const Text('解除双端绑定', style: TextStyle(fontWeight: FontWeight.bold)),
                    ),
                  ] else ...[
                    // Link Form
                    TextField(
                      controller: _bedrockNameController,
                      style: const TextStyle(color: AppColors.textMain),
                      decoration: InputDecoration(
                        labelText: '基岩版用户名 (需带前缀，例如 $_prefix${auth.currentProfile?.username ?? "Steve"})',
                        labelStyle: const TextStyle(color: AppColors.textMuted, fontSize: 13),
                        filled: true,
                        fillColor: AppColors.surface,
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
                      ),
                    ),
                    const SizedBox(height: 14),
                    TextField(
                      controller: _bedrockPassController,
                      obscureText: true,
                      style: const TextStyle(color: AppColors.textMain),
                      decoration: InputDecoration(
                        labelText: '基岩版账号密码 (用于身份所有权验证)',
                        labelStyle: const TextStyle(color: AppColors.textMuted, fontSize: 13),
                        filled: true,
                        fillColor: AppColors.surface,
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
                      ),
                    ),
                    const SizedBox(height: 20),
                    ElevatedButton(
                      onPressed: _handleLink,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                      child: const Text('验证并立即绑定', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                    ),
                  ],
                ],
              ),
            ),
    );
  }
}
