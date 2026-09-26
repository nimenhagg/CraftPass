import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import '../constants/protocol_constants.dart';
import '../models/inventory_data.dart';

class TcpClientService {
  Socket? _socket;
  final List<int> _readBuffer = [];
  final Map<int, Completer<Map<String, dynamic>>> _pendingRequests = {};

  bool get isConnected => _socket != null;

  Future<void> connect(String host, int port) async {
    disconnect();
    _socket = await Socket.connect(host, port, timeout: const Duration(seconds: 10));
    _socket!.listen(
      _onData,
      onError: (err) => disconnect(),
      onDone: () => disconnect(),
      cancelOnError: true,
    );
  }

  void disconnect() {
    try {
      _socket?.destroy();
    } catch (_) {}
    _socket = null;
    _readBuffer.clear();
    for (var completer in _pendingRequests.values) {
      if (!completer.isCompleted) {
        completer.completeError(Exception("Socket disconnected"));
      }
    }
    _pendingRequests.clear();
  }

  void _onData(List<int> data) {
    _readBuffer.addAll(data);

    while (_readBuffer.length >= 8) {
      // Read 8-byte header: TotalLength (4), Magic (2), PacketType (2)
      final byteData = ByteData.sublistView(Uint8List.fromList(_readBuffer.sublist(0, 8)));
      final totalLen = byteData.getUint32(0, Endian.big);
      final magic = byteData.getUint16(4, Endian.big);
      final packetType = byteData.getUint16(6, Endian.big);

      if (magic != ProtocolConstants.magic) {
        disconnect();
        return;
      }

      if (_readBuffer.length < 4 + totalLen) {
        // Incomplete frame, wait for more data
        return;
      }

      final payloadLen = totalLen - 4;
      final payloadBytes = _readBuffer.sublist(8, 8 + payloadLen);
      _readBuffer.removeRange(0, 4 + totalLen);

      final jsonStr = utf8.decode(payloadBytes);
      final jsonMap = json.decode(jsonStr) as Map<String, dynamic>;

      final completer = _pendingRequests.remove(packetType);
      if (completer != null && !completer.isCompleted) {
        completer.complete(jsonMap);
      }
    }
  }

  Future<Map<String, dynamic>> sendRequest(int opCode, int expectedRespOpCode, Map<String, dynamic> payload) async {
    if (_socket == null) {
      throw Exception("Not connected to server");
    }

    final completer = Completer<Map<String, dynamic>>();
    _pendingRequests[expectedRespOpCode] = completer;

    final payloadBytes = utf8.encode(json.encode(payload));
    final totalLen = 2 + 2 + payloadBytes.length;

    final header = ByteData(8);
    header.setUint32(0, totalLen, Endian.big);
    header.setUint16(4, ProtocolConstants.magic, Endian.big);
    header.setUint16(6, opCode, Endian.big);

    _socket!.add(header.buffer.asUint8List());
    _socket!.add(payloadBytes);
    await _socket!.flush();

    return completer.future.timeout(const Duration(seconds: 15), onTimeout: () {
      _pendingRequests.remove(expectedRespOpCode);
      throw TimeoutException("Request timed out for opCode $opCode");
    });
  }

  // --- High-level Protocol Methods ---

  Future<Map<String, dynamic>> handshake(String username) async {
    return sendRequest(
      ProtocolConstants.opHandshakeReq,
      ProtocolConstants.opHandshakeResp,
      {"username": username},
    );
  }

  Future<Map<String, dynamic>> authenticate(String username, String password, String nonce) async {
    return sendRequest(
      ProtocolConstants.opAuthReq,
      ProtocolConstants.opAuthResp,
      {
        "username": username,
        "password": password,
        "nonce": nonce,
      },
    );
  }

  Future<InventoryData> getInventory(String token) async {
    final resp = await sendRequest(
      ProtocolConstants.opGetInventoryReq,
      ProtocolConstants.opGetInventoryResp,
      {"token": token},
    );
    if (resp['status'] != 'SUCCESS') {
      throw Exception(resp['message'] ?? 'Failed to load inventory');
    }
    return InventoryData.fromJson(resp['data'] as Map<String, dynamic>);
  }

  Future<Map<String, dynamic>> changePassword(String token, String oldPassword, String newPassword) async {
    return sendRequest(
      ProtocolConstants.opChangePasswordReq,
      ProtocolConstants.opChangePasswordResp,
      {
        "token": token,
        "oldPassword": oldPassword,
        "newPassword": newPassword,
      },
    );
  }

  Future<Map<String, dynamic>> migrateAccount(String token, String targetNewUsername) async {
    return sendRequest(
      ProtocolConstants.opMigrateAccountReq,
      ProtocolConstants.opMigrateAccountResp,
      {
        "token": token,
        "targetNewUsername": targetNewUsername,
      },
    );
  }

  Future<Map<String, dynamic>> getGeyserLink(String token) async {
    return sendRequest(
      ProtocolConstants.opGetGeyserLinkReq,
      ProtocolConstants.opGetGeyserLinkResp,
      {"token": token},
    );
  }

  Future<Map<String, dynamic>> linkGeyser(String token, String bedrockUsername, String bedrockPassword) async {
    return sendRequest(
      ProtocolConstants.opLinkGeyserReq,
      ProtocolConstants.opLinkGeyserResp,
      {
        "token": token,
        "bedrockUsername": bedrockUsername,
        "bedrockPassword": bedrockPassword,
      },
    );
  }

  Future<Map<String, dynamic>> unlinkGeyser(String token) async {
    return sendRequest(
      ProtocolConstants.opUnlinkGeyserReq,
      ProtocolConstants.opUnlinkGeyserResp,
      {"token": token},
    );
  }
}
