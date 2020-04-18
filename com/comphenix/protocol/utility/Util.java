package com.comphenix.protocol.utility;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.List;

public class Util
{
    public static List<Player> getOnlinePlayers() {
        return (List<Player>)Bukkit.getOnlinePlayers();
    }
    
    @SafeVarargs
    public static <E> List<E> asList(final E... elements) {
        final List<E> list = new ArrayList<E>(elements.length);
        for (final E element : elements) {
            list.add(element);
        }
        return list;
    }
    
    public static boolean isUsingSpigot() {
        return Bukkit.getServer().getVersion().contains("Spigot") || Bukkit.getServer().getVersion().contains("Paper");
    }
}
