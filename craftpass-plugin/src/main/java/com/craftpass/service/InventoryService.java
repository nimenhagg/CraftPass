package com.craftpass.service;

import com.craftpass.util.NbtParser;
import com.craftpass.util.PlayerDataLocator;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * Reads and serializes inventory and ender chest data for online and offline players.
 */
public class InventoryService {
    private final Gson gson = new Gson();

    public static class ItemDto {
        public int slot;
        public String id = "";
        public int count;
        public String displayName = "";
        public List<String> lore = new ArrayList<>();
        public Map<String, Integer> enchants = new LinkedHashMap<>();
        public int damage;
        public int maxDamage;
    }

    public static class InventoryDto {
        public boolean online;
        public double health = 20.0;
        public double maxHealth = 20.0;
        public int foodLevel = 20;
        public int expLevel = 0;
        public List<ItemDto> inventory = new ArrayList<>();
        public List<ItemDto> armor = new ArrayList<>();
        public ItemDto offhand = null;
        public List<ItemDto> enderchest = new ArrayList<>();
    }

    public InventoryDto getPlayerInventory(String username, UUID uuid) {
        Player player = Bukkit.getPlayerExact(username);
        if (player != null && player.isOnline()) {
            return readOnlineInventory(player);
        } else {
            return readOfflineInventory(uuid);
        }
    }

    private InventoryDto readOnlineInventory(Player player) {
        InventoryDto dto = new InventoryDto();
        dto.online = true;
        dto.health = player.getHealth();
        dto.maxHealth = player.getMaxHealth();
        dto.foodLevel = player.getFoodLevel();
        dto.expLevel = player.getLevel();

        ItemStack[] mainContents = player.getInventory().getStorageContents();
        for (int i = 0; i < mainContents.length; i++) {
            ItemStack is = mainContents[i];
            if (is != null && is.getType() != Material.AIR) {
                dto.inventory.add(itemStackToDto(i, is));
            }
        }

        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (int i = 0; i < armorContents.length; i++) {
            ItemStack is = armorContents[i];
            if (is != null && is.getType() != Material.AIR) {
                dto.armor.add(itemStackToDto(i, is));
            }
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            dto.offhand = itemStackToDto(40, offhand);
        }

        ItemStack[] enderContents = player.getEnderChest().getContents();
        for (int i = 0; i < enderContents.length; i++) {
            ItemStack is = enderContents[i];
            if (is != null && is.getType() != Material.AIR) {
                dto.enderchest.add(itemStackToDto(i, is));
            }
        }

        return dto;
    }

    private ItemDto itemStackToDto(int slot, ItemStack is) {
        ItemDto dto = new ItemDto();
        dto.slot = slot;
        dto.id = is.getType().getKey().toString();
        dto.count = is.getAmount();

        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                dto.displayName = meta.getDisplayName();
            } else {
                dto.displayName = formatMaterialName(is.getType().name());
            }

            if (meta.hasLore() && meta.getLore() != null) {
                dto.lore = meta.getLore();
            }

            meta.getEnchants().forEach((ench, lvl) -> {
                dto.enchants.put(ench.getKey().getKey(), lvl);
            });

            if (meta instanceof Damageable) {
                Damageable dmg = (Damageable) meta;
                dto.damage = dmg.getDamage();
                dto.maxDamage = is.getType().getMaxDurability();
            }
        } else {
            dto.displayName = formatMaterialName(is.getType().name());
        }
        return dto;
    }

    private InventoryDto readOfflineInventory(UUID uuid) {
        InventoryDto dto = new InventoryDto();
        dto.online = false;

        File datFile = PlayerDataLocator.findPlayerDataFile(uuid);
        if (datFile == null || !datFile.exists()) {
            return dto;
        }

        try {
            NbtParser.CompoundTag root = NbtParser.readGzipFile(datFile);
            dto.health = root.getInt("Health", 20);
            dto.foodLevel = root.getInt("foodLevel", 20);
            dto.expLevel = root.getInt("XpLevel", 0);

            // Read Inventory List
            List<Object> invList = root.getList("Inventory");
            for (Object obj : invList) {
                if (obj instanceof NbtParser.CompoundTag) {
                    NbtParser.CompoundTag itemTag = (NbtParser.CompoundTag) obj;
                    ItemDto itemDto = parseNbtItem(itemTag);
                    if (itemDto != null) {
                        if (itemDto.slot >= 0 && itemDto.slot < 36) {
                            dto.inventory.add(itemDto);
                        } else if (itemDto.slot >= 100 && itemDto.slot <= 103) {
                            dto.armor.add(itemDto);
                        } else if (itemDto.slot == -106 || itemDto.slot == 150) {
                            dto.offhand = itemDto;
                        }
                    }
                }
            }

            // Read EnderItems List
            List<Object> enderList = root.getList("EnderItems");
            for (Object obj : enderList) {
                if (obj instanceof NbtParser.CompoundTag) {
                    NbtParser.CompoundTag itemTag = (NbtParser.CompoundTag) obj;
                    ItemDto itemDto = parseNbtItem(itemTag);
                    if (itemDto != null) {
                        dto.enderchest.add(itemDto);
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CraftPass] Error reading offline NBT for " + uuid + ": " + e.getMessage());
        }

        return dto;
    }

    private ItemDto parseNbtItem(NbtParser.CompoundTag itemTag) {
        String id = itemTag.getString("id", "");
        if (id.isEmpty() || id.equalsIgnoreCase("minecraft:air")) {
            return null;
        }

        ItemDto dto = new ItemDto();
        dto.id = id;
        dto.slot = itemTag.getByte("Slot", (byte) 0);
        dto.count = itemTag.getInt("count", itemTag.getByte("Count", (byte) 1));

        NbtParser.CompoundTag tag = itemTag.getCompound("tag");
        if (tag != null) {
            NbtParser.CompoundTag display = tag.getCompound("display");
            if (display != null) {
                dto.displayName = display.getString("Name", "");
                List<Object> loreList = display.getList("Lore");
                for (Object l : loreList) {
                    if (l instanceof String) dto.lore.add((String) l);
                }
            }

            List<Object> enchList = tag.getList("Enchantments");
            if (enchList.isEmpty()) {
                enchList = tag.getList("ench");
            }
            for (Object e : enchList) {
                if (e instanceof NbtParser.CompoundTag) {
                    NbtParser.CompoundTag enchTag = (NbtParser.CompoundTag) e;
                    String enchId = enchTag.getString("id", "");
                    int lvl = enchTag.getInt("lvl", 1);
                    dto.enchants.put(enchId.replace("minecraft:", ""), lvl);
                }
            }

            dto.damage = tag.getInt("Damage", 0);
        }

        if (dto.displayName.isEmpty()) {
            dto.displayName = formatMaterialName(id.replace("minecraft:", ""));
        }

        return dto;
    }

    private String formatMaterialName(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
