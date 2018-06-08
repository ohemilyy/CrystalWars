package org.mswsplex.Crystal.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mswsplex.Crystal.Utils.MSG;
import org.mswsplex.Crystal.Utils.NBT;
import org.mswsplex.Crystal.Utils.PlayerManager;
import org.mswsplex.Crystal.Utils.TimeManager;
import org.mswsplex.MSWS.Crystal.Main;

public class GameManager {
	FileConfiguration data = Main.plugin.data;
	PlayerManager pManager = new PlayerManager();
	NBT NBT = new NBT();

	public void setInfo(World world, String id, Object data) { // Store info of the game
		if (!pManager.isSaveable(data)) {
			int currentLine = Thread.currentThread().getStackTrace()[2].getLineNumber();

			String fromClass = new Exception().getStackTrace()[1].getClassName();
			if (fromClass.contains("."))
				fromClass = fromClass.split("\\.")[fromClass.split("\\.").length - 1];
			MSG.log("WARNING!!! SAVING ODD DATA FROM " + fromClass + ":" + currentLine);
		}
		this.data.set("Games." + world.getName() + "." + id, data);
	}

	public Object getInfo(World world, String id) {
		return data.get("Games." + world.getName() + "." + id);
	}

	public String getStatus(World world) {
		if (getInfo(world, "status") == null)
			return "disabled";
		return (String) getInfo(world, "status");
	}

	public double getCrystalHP(World world, String team) {
		return Main.plugin.data.getDouble("Games." + world.getName() + ".teams." + team + ".crystalHealth");
	}

	public List<Player> alivePlayers(World world) {
		ConfigurationSection teams = data.getConfigurationSection("Games." + world.getName() + ".teams");
		if (teams == null)
			return null;
		List<Player> players = new ArrayList<Player>();
		for (String res : getTeams(world)) {
			ConfigurationSection team = teams.getConfigurationSection(res + ".members");
			if (team == null)
				continue;
			for (String name : team.getKeys(false)) {
				if (team.getBoolean(name + ".alive"))
					players.add(Bukkit.getPlayer(name));
			}
		}
		return players;
	}

	/**
	 * Automatically assigns teams to players that don't have a team
	 * 
	 * @param world
	 */
	public void assignTeams(World world) {
		if (getTeams(world) == null)
			return;
		if (!data.contains("Games." + world.getName()))
			return;
		for (Player player : world.getPlayers()) {
			if (pManager.getTeam(player) != null)
				continue;
			if (getLeastTeam(player.getWorld()) == null)
				continue;
			String team = getLeastTeam(player.getWorld());
			Main.plugin.data.set(
					"Games." + world.getName() + ".teams." + team + ".members." + player.getName() + ".alive", true);
			player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		}
		updateNames(world);
	}

	/**
	 * Update player display names and tablistnames compatible with if the player is
	 * alive or not
	 * 
	 * @param world
	 */
	public void updateNames(World world) {
		for (Player player : world.getPlayers()) {
			String team = pManager.getTeam(player);
			if (team == null)
				continue;
			String dName = getColor(world, team) + player.getName();
			if (!pManager.isAlive(player)) {
				player.setDisplayName(
						MSG.color("&7" + Main.plugin.config.getString("Status.Dead") + " " + dName + "&f"));
			} else {
				player.setDisplayName(MSG.color(dName + "&f"));

			}
			player.setPlayerListName(MSG.color(getColor(player.getWorld(), team) + player.getName() + "&r"));
		}
	}

	/**
	 * @param world
	 * @return List of all teams in the world
	 */
	public List<String> getTeams(World world) {
		List<String> teams = new ArrayList<String>();
		ConfigurationSection teamList = data.getConfigurationSection("Games." + world.getName() + ".teams");
		if (teamList == null)
			return null;
		for (String res : teamList.getKeys(false))
			teams.add(res);
		return teams;
	}

	/**
	 * @param world
	 * @param team
	 *            to get alive players in
	 * @return List of all players in the team
	 */
	public List<Player> getAlivePlayersInTeam(World world, String team) {
		List<Player> players = getTeamMembers(world, team);
		List<Player> result = new ArrayList<Player>();
		for (Player alive : alivePlayers(world)) {
			if (players.contains(alive)) {
				result.add(alive);
			}
		}

		return result;
	}

	/**
	 * @param world
	 * @param team
	 * @return Returns string of status based on Config values
	 */
	public String getTeamStatus(World world, String team) {
		if (data.getDouble("Games." + world.getName() + ".teams." + team + ".crystalHealth") > 0) {
			return Main.plugin.config.getString("Status.CrystalAlive");
		} else if (getAlivePlayersInTeam(world, team).size() > 0) {
			return Main.plugin.config.getString("Status.CrystalDead")
					.replace("%amo%", getAlivePlayersInTeam(world, team).size() + "")
					.replace("%s%", getAlivePlayersInTeam(world, team).size() == 1 ? "" : "s");
		} else {
			return Main.plugin.config.getString("Status.Dead");
		}
	}

	/**
	 * 
	 * @param world
	 * @param team
	 * @return List of team members in a team
	 */
	public List<Player> getTeamMembers(World world, String team) {
		List<Player> players = new ArrayList<Player>();
		for (Player target : world.getPlayers()) {
			if (pManager.getTeam(target) == null)
				continue;
			if (pManager.getTeam(target).equals(team))
				players.add(target);
		}
		return players;
	}

	/**
	 * 
	 * @param world
	 * @return List of teams with ALIVE players
	 */
	public List<String> getTeamsWithPlayers(World world) {
		List<String> teams = new ArrayList<String>();
		for (String team : getTeams(world)) {
			if (getAlivePlayersInTeam(world, team).size() > 0)
				teams.add(team);
		}
		return teams;
	}

	/**
	 * 
	 * @param world
	 * @return Returns what team has the least amount of players useful for
	 *         assigning teams
	 */
	public String getLeastTeam(World world) {
		if (getTeams(world) == null)
			return null;
		int size = 0;
		String team = "";
		for (String res : getTeams(world)) {
			if (getTeamMembers(world, res).size() <= size) {
				size = getTeamMembers(world, res).size();
				team = res;
			}
		}
		if (team.equals(""))
			return null;
		return team;
	}

	/**
	 * 
	 * @param world
	 * @param team
	 * @return Return what color the team has
	 */
	public String getColor(World world, String team) {
		return (data.getString("Games." + world.getName() + ".teams." + team + ".color"));
	}

	/**
	 * 
	 * @param world
	 * @return Player (if any) with most kills (null if none)
	 */
	public Player getMVP(World world) {
		double record = 0;
		Player winner = null;
		for (Player player : world.getPlayers()) {
			if (pManager.getDouble(player, "kills") > record) {
				record = pManager.getDouble(player, "kills");
				winner = player;
			}
		}
		return winner;
	}

	public String getBuilders(World world) {
		return Main.plugin.data.contains("Games." + world.getName() + ".builders")
				? Main.plugin.data.getString("Games." + world.getName() + ".builders")
				: "None";
	}

	/**
	 * Teleports and starts the game, use startCountdown if you want a countdown
	 * 
	 * @param world
	 */
	public void startGame(World world) {
		setInfo(world, "status", "ingame" + System.currentTimeMillis());
		setInfo(world, "beginTimer",
				System.currentTimeMillis() + (Main.plugin.config.getDouble("BeginningTimer") * 1000));
		setInfo(world, "lastPling", (double) System.currentTimeMillis());
		saveWorld(world);
		List<String> tmp = new ArrayList<String>();
		Main.plugin.lang.getStringList("Game.StartMsg").forEach((res) -> MSG.tell(world,
				res.replace("%map%", world.getName()).replace("%builders%", getBuilders(world))));
		for (String team : getTeams(world)) {
			String rMap = "Games." + world.getName() + ".teams." + team + ".resources";
			setInfo(world, team + "purchases", null);
			data.set("Games." + world.getName() + ".teams." + team + ".enchants", null);
			data.set(rMap, null);
			data.set("Games." + world.getName() + ".teams." + team + ".crystalHealth",
					Main.plugin.config.getDouble("MaxCrystalHealth"));
			data.set(rMap + ".COAL.amo", 1);
			data.set(rMap + ".COAL.rate", 20 * 2.5);
			int pos = 0;
			for (Player target : getTeamMembers(world, team)) {
				pManager.spawnPlayer(target);
				pManager.setInfo(target, "kills", 0.0);
				pManager.setInfo(target, "purchases", null);
				data.set("Games." + target.getWorld().getName() + ".teams." + team + ".members." + target.getName()
						+ ".alive", true);
			}
			List<String> shops = new ArrayList<String>();

			EnderCrystal crystal = (EnderCrystal) world.spawnEntity(
					Main.plugin.getLocation("Games." + world.getName() + ".teams." + team + ".crystal"),
					EntityType.ENDER_CRYSTAL);
			ArmorStand name = (ArmorStand) world.spawnEntity(crystal.getLocation(), EntityType.ARMOR_STAND);
			ArmorStand hp = (ArmorStand) world.spawnEntity(crystal.getLocation().subtract(0, .2, 0),
					EntityType.ARMOR_STAND);
			name.setCustomName(MSG.color(getColor(world, team) + MSG.camelCase(team) + "'s Crystal "
					+ data.getDouble("Games." + world.getName() + ".teams." + team + ".crystalHealth") + "/"
					+ Main.plugin.config.getDouble("MaxCrystalHealth")));
			hp.setCustomName(MSG.progressBar(
					data.getDouble("Games." + world.getName() + ".teams." + team + ".crystalHealth"),
					Main.plugin.config.getDouble("MaxCrystalHealth"), Main.plugin.config.getInt("HealthBarLength")));
			name.setVisible(false);
			hp.setVisible(false);
			name.setCustomNameVisible(true);
			hp.setCustomNameVisible(true);
			hp.setGravity(false);
			name.setGravity(false);
			tmp.add(crystal.getUniqueId() + "");
			tmp.add(hp.getUniqueId() + "");
			tmp.add(name.getUniqueId() + "");
			shops.add(crystal.getUniqueId() + "");
			shops.add(hp.getUniqueId() + "");
			shops.add(name.getUniqueId() + "");
			setInfo(world, team + "crystal", shops);

			String[] names = { "Coal Shop", "End Shop", "Team Shop" };
			pos = 0;
			for (String res : new String[] { "coalShop", "endShop", "teamShop" }) {
				shops = new ArrayList<String>();
				Location loc = Main.plugin.getLocation("Games." + world.getName() + ".teams." + team + "." + res);
				// For some reason Location#add messes up with the Location
				Enderman shop = (Enderman) world.spawnEntity(loc, EntityType.ENDERMAN);
				ArmorStand stand = (ArmorStand) world.spawnEntity(loc.add(0, 1, 0), EntityType.ARMOR_STAND);
				NBT.setNBT("NoAI", shop, true);
				NBT.setNBT("Silent", shop, true);
				stand.setVisible(false);
				stand.setGravity(false);
				stand.setCustomName(MSG.color(getColor(world, team) + MSG.camelCase(team) + " " + names[pos]));
				stand.setCustomNameVisible(true);
				shop.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 999999, 255, true, false));
				tmp.add(shop.getUniqueId() + "");
				tmp.add(stand.getUniqueId() + "");
				shops.add(shop.getUniqueId() + "");
				shops.add(stand.getUniqueId() + "");
				setInfo(world, team + res, shops);
				pos++;
			}
		}
		setInfo(world, "removeEntities", tmp);
		updateNames(world);
	}

	/**
	 * Ends the game and broadcasts win message
	 * 
	 * @param world
	 */
	@SuppressWarnings("deprecation")
	public void winGame(World world) {
		String winner = "";
		winner = getTeamsWithPlayers(world).get(0);
		Player mvp = getMVP(world);
		for (String res : Main.plugin.lang.getStringList("Game.Over"))
			MSG.tell(world,
					res.replace("%team%", MSG.camelCase(winner)).replace("%winColor%", getColor(world, winner))
							.replace("%time%",
									new TimeManager().getTime(System.currentTimeMillis()
											- Double.valueOf(getStatus(world).substring("ingame".length())), 2))
							.replace("%mvp%",
									mvp == null ? "None"
											: MSG.color(getColor(world, pManager.getTeam(mvp)) + mvp.getName()))
							.replace("%kills%", mvp == null ? "0" : pManager.getKills(mvp) + "")
							.replace("%s%", pManager.getKills(mvp) == 1 ? "" : "s").replace("%map%", world.getName())
							.replace("%builders%", getBuilders(world)));
		for (Player player : world.getPlayers()) {
			player.sendTitle(MSG.color(getColor(world, winner) + "&l" + MSG.camelCase(winner)),
					MSG.color(getColor(world, winner) + "has won the game!"));
		}
		stopGame(world);
	}

	/**
	 * Ends the game, use winGame to broadcast win message
	 * 
	 * @param world
	 */
	@SuppressWarnings("unchecked")
	public void stopGame(World world) {
		for (Player target : world.getPlayers()) {
			target.teleport(Main.plugin.getLocation("Games." + world.getName() + ".lobby"));
			target.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
			target.getInventory().clear();
			target.getInventory().setArmorContents(null);
			for (Player player : world.getPlayers())
				player.showPlayer(target);
			target.setDisplayName(target.getName());
			target.setPlayerListName(target.getName());
			target.getInventory().addItem(pManager.parseItem(Main.plugin.gui, "gameSelectorItem", target));
		}
		setInfo(world, "status", "lobby");
		setInfo(world, "beginTimer", null);
		for (String team : getTeams(world))
			Main.plugin.data.set("Games." + world.getName() + ".teams." + team + ".members", null);
		if (getInfo(world, "removeEntities") != null)
			for (String uuid : ((List<String>) getInfo(world, "removeEntities"))) {
				Entity ent = getEntityByUUID(UUID.fromString(uuid), world);
				if (ent != null)
					ent.remove();
			}
		reloadWorld(world);
		setInfo(world, "removeEntities", null);
	}

	/**
	 * Used for chest protection
	 * 
	 * @param loc
	 * @return Returns what team's crystal is closest
	 */
	public String getClosetCrystal(Location loc) {
		World world = loc.getWorld();
		List<String> teams = getTeams(world);
		if (teams == null)
			return null;
		double dist = 0.0;
		String closest = "";
		for (String res : teams) {
			double tempDist = Main.plugin.getLocation("Games." + world.getName() + ".teams." + res + ".crystal")
					.distanceSquared(loc);
			if (tempDist < dist || dist == 0) {
				closest = res;
				dist = tempDist;
			}
		}
		return closest;
	}

	/**
	 * Saves the world
	 * 
	 * @param world
	 */
	public void saveWorld(World world) {
		world.save();
		world.setAutoSave(false);
	}

	/**
	 * Reloads the world, kicking players or teleporting them to endLocaion (if
	 * specified)
	 * 
	 * @param world
	 */
	public void reloadWorld(World world) {
		Location loc = Main.plugin.getLocation("Games." + world.getName() + ".endLocation");
		for (Player player : world.getPlayers()) {
			if (loc == null || loc.getWorld() == null)
				player.kickPlayer(MSG.getString("Game.Kicked", "Dead World"));
			player.teleport(loc);
		}
		Bukkit.unloadWorld(world, false);
		World tWorld = Bukkit.createWorld(WorldCreator.name(world.getName()));
		tWorld.setGameRuleValue("doMobSpawning", "false");
		tWorld.setGameRuleValue("doMobLoot", "false");
		for (Entity ent : tWorld.getEntities())
			ent.remove();
	}

	/**
	 * Forces a game to crash
	 * 
	 * @param world
	 * @param reason
	 */
	@SuppressWarnings("unchecked")
	public void crashGame(World world, String reason) {
		setInfo(world, "status", "lobby");
		setInfo(world, "beginTimer", null);
		if (getInfo(world, "removeEntities") != null)
			for (String uuid : ((List<String>) getInfo(world, "removeEntities"))) {
				Entity ent = getEntityByUUID(UUID.fromString(uuid), world);
				if (ent == null)
					continue;
				ent.remove();
			}
		setInfo(world, "removeEntities", null);
		int currentLine = Thread.currentThread().getStackTrace()[2].getLineNumber();
		String fromClass = new Exception().getStackTrace()[1].getClassName();
		if (fromClass.contains("."))
			fromClass = fromClass.split("\\.")[fromClass.split("\\.").length - 1];
		MSG.tell(world, MSG.getString("Game.Error", "Error: %reason% %class%:%line%").replace("%reason%", reason)
				.replace("%class%", fromClass).replace("%line%", currentLine + ""));
		MSG.log(MSG.getString("Game.Error", "Error: %class%:%line%").replace("%reason%", reason)
				.replace("%class%", fromClass).replace("%line%", currentLine + "") + " (World: " + world.getName()
				+ ")");
	}

	/**
	 * Returns what entity has the UUID, if world is null then will search all
	 * worlds
	 * 
	 * @param uuid
	 * @param world
	 * @return
	 */
	public Entity getEntityByUUID(UUID uuid, World world) {
		if (world == null) {
			Entity ent = null;
			for (World tWorld : Bukkit.getWorlds()) {
				ent = getEntityByUUID(uuid, tWorld);
				if (ent != null)
					return ent;
			}
			return null;
		}
		for (Entity ent : world.getEntities()) {
			if (ent.getUniqueId().equals(uuid))
				return ent;
		}
		return null;
	}

	public void startCountdown(World world, int startTime) {
		setInfo(world, "status", "countdown" + Math.max((System.currentTimeMillis() + (startTime * 1000)), 1000));
		setInfo(world, "lastPling", (double) System.currentTimeMillis());
	}
}
