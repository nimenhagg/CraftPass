# CraftPass Security Architecture: Public Database Defense

---

## 1. Threat Model: Publicly Exposed Database

In many Minecraft communities, full server backups or SQLite/MySQL databases (including `LoginSecurity.db`) may be publicly shared, downloaded, or mirrored for transparency.

### The Attack Vector:
When an attacker obtains `LoginSecurity.db`:
- In standard authentication systems, passwords are stored as `BCrypt(Password)`.
- Using modern multi-GPU rigs (e.g., NVIDIA RTX 4090 clusters) and toolsets like `hashcat` or `John the Ripper`, attackers can run offline dictionary attacks against the hashes at thousands of guesses per second, cracking weak or medium-complexity passwords.

---

## 2. CraftPass Countermeasure: Server-Side Pepper (HMAC-SHA256)

CraftPass neutralizes this threat by introducing a **Server-Side Pepper**:

$$\text{PepperedSecret} = \text{HMAC-SHA256}(\text{RawPassword}, \text{ServerPepper})$$
$$\text{StoredHash} = \text{BCrypt}(\text{PepperedSecret}, \text{Salt})$$

### Why Offline Cracking is Mathematically Impossible:
1. **The Pepper is Secret**: The `ServerPepper` is a 256-bit cryptographically secure random key stored **strictly in `config.yml` on the production server host**.
2. **Never in the Database**: The pepper is **never** written to `LoginSecurity.db` or any exported backup.
3. **No Key, No Guess**: Without the 256-bit pepper, an attacker cannot evaluate the HMAC-SHA256 step. Every single candidate password tested by an offline GPU attack produces an invalid candidate, rendering rainbow tables and dictionary attacks **completely ineffective**.

---

## 3. Seamless Auto-Upgrade for Legacy Accounts

When an existing server adopts CraftPass:
1. When a player logs in via CraftPass, the system first verifies against the peppered hash.
2. If it misses, it falls back to verify against the legacy un-peppered BCrypt hash.
3. Upon a successful legacy match, CraftPass **automatically upgrades the stored hash in `LoginSecurity.db` to the Peppered-BCrypt format in-place**.
4. Zero player disruption, zero database wipes, automatic progressive hardening.

---

## 4. Anti-Brute-Force & Rate Limiting

- **Lockout Mechanism**: After 5 consecutive failed authentication attempts from any IP address, CraftPass temporarily locks the IP for 10 minutes.
- **Single-Use Challenge Nonces**: Nonces are cryptographically unique and expire after 60 seconds.
- **Session Tokens**: Authenticated sessions are issued high-entropy 256-bit memory-only tokens with automatic expiration.
