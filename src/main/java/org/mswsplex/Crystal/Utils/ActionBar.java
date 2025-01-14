package org.mswsplex.Crystal.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ActionBar {
	// Code provided by https://bukkit.org/threads/send-hotbar-messages.440664/

	/**
	 * These are the Class instances. Use these to get fields or methods for
	 * classes.
	 */
	private static Class<?> CRAFTPLAYERCLASS, PACKET_PLAYER_CHAT_CLASS, ICHATCOMP, CHATMESSAGE, PACKET_CLASS,
			CHAT_MESSAGE_TYPE_CLASS;

	private static Field PLAYERCONNECTION;
	private static Method GETHANDLE, SENDPACKET;

	/**
	 * These are the constructors for those classes. You need these to create new
	 * objects.
	 */
	private static Constructor<?> PACKET_PLAYER_CHAT_CONSTRUCTOR, CHATMESSAGE_CONSTRUCTOR;
	/**
	 * Used in 1.12+. Bytes are replaced with this enum
	 */
	private static Object CHAT_MESSAGE_TYPE_ENUM_OBJECT;

	/**
	 * This is the server version. This is how we know the server version.
	 */
	private static final String SERVER_VERSION;
	static {
		// This gets the server version.
		String name = Bukkit.getServer().getClass().getName();
		name = name.substring(name.indexOf("craftbukkit.") + "craftbukkit.".length());
		name = name.substring(0, name.indexOf("."));
		SERVER_VERSION = name;

		try {
			// This here sets the class fields.
			CRAFTPLAYERCLASS = Class.forName("org.bukkit.craftbukkit." + SERVER_VERSION + ".entity.CraftPlayer");
			PACKET_PLAYER_CHAT_CLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".PacketPlayOutChat");
			PACKET_CLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".Packet");
			ICHATCOMP = Class.forName("net.minecraft.server." + SERVER_VERSION + ".IChatBaseComponent");
			GETHANDLE = CRAFTPLAYERCLASS.getMethod("getHandle");
			PLAYERCONNECTION = GETHANDLE.getReturnType().getField("playerConnection");
			SENDPACKET = PLAYERCONNECTION.getType().getMethod("sendPacket", PACKET_CLASS);
			try {
				PACKET_PLAYER_CHAT_CONSTRUCTOR = PACKET_PLAYER_CHAT_CLASS.getConstructor(ICHATCOMP, byte.class);
			} catch (NoSuchMethodException e) {
				CHAT_MESSAGE_TYPE_CLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".ChatMessageType");
				CHAT_MESSAGE_TYPE_ENUM_OBJECT = CHAT_MESSAGE_TYPE_CLASS.getEnumConstants()[2];

				PACKET_PLAYER_CHAT_CONSTRUCTOR = PACKET_PLAYER_CHAT_CLASS.getConstructor(ICHATCOMP,
						CHAT_MESSAGE_TYPE_CLASS);
			}

			CHATMESSAGE = Class.forName("net.minecraft.server." + SERVER_VERSION + ".ChatMessage");

			CHATMESSAGE_CONSTRUCTOR = CHATMESSAGE.getConstructor(String.class, Object[].class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends the hotbar message 'message' to the player 'player'
	 *
	 * @param player
	 *            Player to send message to
	 * @param message
	 *            Message String (not translated to ChatColor)
	 */
	public static void sendHotBarMessage(Player player, String message) {
		try {
			// This creates the IChatComponentBase instance
			Object icb = CHATMESSAGE_CONSTRUCTOR.newInstance(message, new Object[0]);
			// This creates the packet
			Object packet;
			try {
				packet = PACKET_PLAYER_CHAT_CONSTRUCTOR.newInstance(icb, (byte) 2);
			} catch (Exception e) {
				packet = PACKET_PLAYER_CHAT_CONSTRUCTOR.newInstance(icb, CHAT_MESSAGE_TYPE_ENUM_OBJECT);
			}
			// This casts the player to a craftplayer
			Object craftplayerInst = CRAFTPLAYERCLASS.cast(player);
			// This invokes the method above.
			Object methodhHandle = GETHANDLE.invoke(craftplayerInst);
			// This gets the player's connection
			Object playerConnection = PLAYERCONNECTION.get(methodhHandle);
			// This sends the packet.
			SENDPACKET.invoke(playerConnection, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}