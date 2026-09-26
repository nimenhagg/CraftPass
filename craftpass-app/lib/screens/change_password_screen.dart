import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../constants/app_colors.dart';
import '../services/auth_service.dart';

class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _oldPassController = TextEditingController();
  final _newPassController = TextEditingController();
  final _confirmPassController = TextEditingController();

  bool _isLoading = false;
  String? _message;
  bool _isSuccess = false;

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    final auth = Provider.of<AuthService>(context, listen: false);
    if (!auth.isAuthenticated) return;

    setState(() {
      _isLoading = true;
      _message = null;
    });

    try {
      final resp = await auth.tcpService.changePassword(
        auth.currentProfile!.token,
        _oldPassController.text,
        _newPassController.text,
      );

      final success = resp['status'] == 'SUCCESS';
      setState(() {
        _isSuccess = success;
        _message = resp['message'] ?? (success ? '密码修改成功！' : '修改失败！');
        _isLoading = false;
      });

      if (success) {
        _oldPassController.clear();
        _newPassController.clear();
        _confirmPassController.clear();
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
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        title: const Text('安全中心 · 密码修改', style: TextStyle(color: AppColors.textMain, fontSize: 17, fontWeight: FontWeight.bold)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Security info banner
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.primary.withOpacity(0.4)),
                ),
                child: const Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.lock_person, color: AppColors.primary, size: 24),
                    SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('防数据库泄露防护机制', style: TextStyle(color: AppColors.primary, fontWeight: FontWeight.bold, fontSize: 14)),
                          SizedBox(height: 4),
                          Text(
                            '服务器已启用 256 位 Server-Pepper 加盐加密。即使服务器数据库被公开发布，离线 GPU 爆破也完全无法破解您的新密码。',
                            style: TextStyle(color: AppColors.textMuted, fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // Feedback message
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

              // Old Password
              TextFormField(
                controller: _oldPassController,
                obscureText: true,
                style: const TextStyle(color: AppColors.textMain),
                decoration: InputDecoration(
                  labelText: '当前原密码',
                  labelStyle: const TextStyle(color: AppColors.textMuted),
                  filled: true,
                  fillColor: AppColors.surface,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
                ),
                validator: (v) => v == null || v.isEmpty ? '请输入原密码' : null,
              ),
              const SizedBox(height: 16),

              // New Password
              TextFormField(
                controller: _newPassController,
                obscureText: true,
                style: const TextStyle(color: AppColors.textMain),
                decoration: InputDecoration(
                  labelText: '新密码 (6-32 位)',
                  labelStyle: const TextStyle(color: AppColors.textMuted),
                  filled: true,
                  fillColor: AppColors.surface,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return '请输入新密码';
                  if (v.length < 6 || v.length > 32) return '密码长度必须在 6 到 32 位之间';
                  return null;
                },
              ),
              const SizedBox(height: 16),

              // Confirm New Password
              TextFormField(
                controller: _confirmPassController,
                obscureText: true,
                style: const TextStyle(color: AppColors.textMain),
                decoration: InputDecoration(
                  labelText: '确认新密码',
                  labelStyle: const TextStyle(color: AppColors.textMuted),
                  filled: true,
                  fillColor: AppColors.surface,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
                ),
                validator: (v) {
                  if (v != _newPassController.text) return '两次输入的密码不一致';
                  return null;
                },
              ),
              const SizedBox(height: 24),

              // Submit Button
              ElevatedButton(
                onPressed: _isLoading ? null : _handleSubmit,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                child: _isLoading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                      )
                    : const Text('确认修改密码', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
