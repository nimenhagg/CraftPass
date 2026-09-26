class ProtocolConstants {
  static const int magic = 0x4350; // "CP"

  // Packet Opcodes
  static const int opHandshakeReq = 0x0001;
  static const int opHandshakeResp = 0x0002;

  static const int opAuthReq = 0x0003;
  static const int opAuthResp = 0x0004;

  static const int opGetInventoryReq = 0x0005;
  static const int opGetInventoryResp = 0x0006;

  static const int opChangePasswordReq = 0x0007;
  static const int opChangePasswordResp = 0x0008;

  static const int opMigrateAccountReq = 0x0009;
  static const int opMigrateAccountResp = 0x000A;

  static const int opGetGeyserLinkReq = 0x000B;
  static const int opGetGeyserLinkResp = 0x000C;
  static const int opLinkGeyserReq = 0x000D;
  static const int opLinkGeyserResp = 0x000E;
  static const int opUnlinkGeyserReq = 0x000F;
  static const int opUnlinkGeyserResp = 0x0010;

  static const int opHeartbeat = 0x00FE;
  static const int opError = 0x00FF;
}
