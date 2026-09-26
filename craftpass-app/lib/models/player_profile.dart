class PlayerProfile {
  final String username;
  final String uuid;
  final String token;
  final int expiresAt;
  final String serverHost;
  final int serverPort;

  PlayerProfile({
    required this.username,
    required this.uuid,
    required this.token,
    required this.expiresAt,
    required this.serverHost,
    required this.serverPort,
  });

  bool get isExpired => DateTime.now().millisecondsSinceEpoch > expiresAt;
}
