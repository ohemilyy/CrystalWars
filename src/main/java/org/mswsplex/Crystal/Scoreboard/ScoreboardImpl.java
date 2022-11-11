package org.mswsplex.Crystal.Scoreboard;

import io.github.nosequel.scoreboard.element.ScoreboardElement;
import io.github.nosequel.scoreboard.element.ScoreboardElementHandler;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.mswsplex.Crystal.Game.GameManager;
import org.mswsplex.Crystal.Utils.PlayerManager;
import org.mswsplex.Crystal.Utils.TimeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.mswsplex.Crystal.Game.GameManager;
import org.mswsplex.Crystal.Utils.MSG;
import org.mswsplex.Crystal.Utils.PlayerManager;
import org.mswsplex.Crystal.Utils.TimeManager;
import org.mswsplex.MSWS.Crystal.Main;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ScoreboardImpl implements ScoreboardElementHandler {
    PlayerManager pManager = new PlayerManager();
    TimeManager tManager = new TimeManager();
    public HashMap<World, List<String>> inGame = new HashMap<>();
    GameManager gManager = new GameManager();
    /**
     * Get the scoreboard element of a player
     *
     * @param player the player
     * @return the element
     */
    @Override
    public ScoreboardElement getElement(Player player, World world) {
        final ScoreboardElement element = new ScoreboardElement();

        element.setTitle(ChatColor.translateAlternateColorCodes('&', "&5&lOuter&f&lWorlds"));
//Lobby Scoreboard
        if(gManager.getStatus(world).equals("lobby")){
            element.add(ChatColor.translateAlternateColorCodes('&', "&f&m [                          ] &r "));
            element.add(ChatColor.translateAlternateColorCodes('&', "&5» &dServer&7: &f" + player.getWorld().getName()).replace("%kit%", pManager.getKit(player)));
//            element.add(ChatColor.translateAlternateColorCodes('&', "&5» &dBegins In&7: &f" + gManager.getStatus(player.getWorld()).startsWith("countdown") ? tManager.getRoundTimeMillis(
//                    Double.valueOf(gManager.getStatus(player.getWorld()).substring("countdown".length()))
//                            - System.currentTimeMillis() + 1000)
//                    : ""));
            element.add(ChatColor.translateAlternateColorCodes('&', "&5» &dPlayers&7: &f" + player.getWorld().getPlayers().size() + "/" + Main.plugin.data.getInt("Games." + player.getWorld().getName() + ".maxSize") + ""));

        }
        if(gManager.getStatus(player.getWorld()).startsWith("ingame")){
            element.add(ChatColor.translateAlternateColorCodes('&', "&f&m [                          ] &r "));

        }


            return element;
    }
}
