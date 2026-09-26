import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/player_profile.dart';
import 'tcp_client_service.dart';

class AuthService extends ChangeNotifier {
  final TcpClientService tcpService = TcpClientService();
  PlayerProfile? _currentProfile;
  bool _isLoading = false;
  String? _errorMessage;

  PlayerProfile? get currentProfile => _currentProfile;
  bool get isAuthenticated => _currentProfile != null && !_currentProfile!.isExpired;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  Future<bool> login({
    required String host,
    required int port,
    required String username,
    required String password,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      // 1. Connect TCP Socket
      await tcpService.connect(host, port);

      // 2. Handshake (get challenge nonce)
      final handshakeResp = await tcpService.handshake(username);
      if (handshakeResp['status'] != 'OK') {
        throw Exception(handshakeResp['message'] ?? 'Handshake rejected by server');
      }
      final nonce = handshakeResp['nonce'] as String;

      // 3. Authenticate
      final authResp = await tcpService.authenticate(username, password, nonce);
      if (authResp['status'] != 'SUCCESS') {
        throw Exception(authResp['message'] ?? 'Authentication failed');
      }

      final token = authResp['token'] as String;
      final uuid = authResp['uuid'] as String;
      final expiresAt = (authResp['expiresAt'] as num).toInt();

      _currentProfile = PlayerProfile(
        username: username,
        uuid: uuid,
        token: token,
        expiresAt: expiresAt,
        serverHost: host,
        serverPort: port,
      );

      // Save connection settings locally
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('last_host', host);
      await prefs.setInt('last_port', port);
      await prefs.setString('last_username', username);

      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceAll("Exception: ", "");
      _isLoading = false;
      tcpService.disconnect();
      notifyListeners();
      return false;
    }
  }

  void logout() {
    _currentProfile = null;
    tcpService.disconnect();
    notifyListeners();
  }
}
