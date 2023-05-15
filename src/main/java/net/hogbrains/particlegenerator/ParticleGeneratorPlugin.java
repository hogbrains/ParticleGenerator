package net.hogbrains.particlegenerator;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class ParticleGeneratorPlugin extends JavaPlugin {

    private Map<String, ParticleGenerator> generators = new HashMap<>();

    @Override
    public void onEnable() {

        // Save the default config file to the plugin data folder
        saveDefaultConfig();

        // Load the generators from the config
        loadGenerators();

        // Start the generator tasks
        for (ParticleGenerator generator : generators.values()) {
            generator.start();
        }

        // Register the /particlegen command
        getCommand("particlegen").setExecutor(this);
        getCommand("particlegen").setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        // Stop the generator tasks
        for (ParticleGenerator generator : generators.values()) {
            generator.stop();
        }
    }

    private void loadGenerators() {
        // Load the generators from the config
        FileConfiguration config = getConfig();
        Map<String, Object> generatorStrings = config.getConfigurationSection("generators").getValues(false);
        for (Map.Entry<String, Object> entry : generatorStrings.entrySet()) {
            String name = entry.getKey();
            String generatorString = (String) entry.getValue();

            String[] parts = generatorString.split(";");
            String worldName = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            Particle particle = Particle.valueOf(parts[4]);
            int count = Integer.parseInt(parts[5]);
            int interval = Integer.parseInt(parts[6]);
            double speed = Double.parseDouble(parts[7]);
            double offsetX = Double.parseDouble(parts[8]);
            double offsetY = Double.parseDouble(parts[9]);
            double offsetZ = Double.parseDouble(parts[10]);

            Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
            Vector offset = new Vector(offsetX, offsetY, offsetZ);
            ParticleGenerator generator = new ParticleGenerator(location, particle, count, interval, speed, offset);
            generators.put(name, generator);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length < 1) {
                // Send a message to the player if they didn't specify any arguments
                player.sendMessage(ChatColor.GRAY + "Running ParticleGenerator v1.0 by Hemazoid :D");
                return true;
            }

            String subCommand = args[0];
            if (subCommand.equalsIgnoreCase("create")) {
                if (args.length < 8) {
                    player.sendMessage(parseMessage("createUsage"));
                    return true;
                }

                String name = args[1];
                if (generators.containsKey(name)) {
                    // Send a message to the player if the generator name is already taken
                    player.sendMessage(parseMessage("generatorExists"));
                    return true;
                }

                Particle particle = null;
                try {
                    particle = Particle.valueOf(args[2]);
                } catch (IllegalArgumentException e) {
                    // Send a message to the player if they specified an invalid particle
                    player.sendMessage(parseMessage("invalidParticle"));
                    return true;
                }

                int count = 0;
                try {
                    count = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    // Send a message to the player if they specified an invalid count
                    player.sendMessage(parseMessage("invalidCount"));
                    return true;
                }

                int interval = 0;
                try {
                    interval = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    // Send a message to the player if they specified an invalid interval
                    player.sendMessage(ChatColor.RED + "Invalid interval specified!");
                    return true;
                }

                double speed = 0;
                try {
                    speed = Double.parseDouble(args[5]);
                } catch (NumberFormatException e) {
                    // Send a message to the player if they specified an invalid speed
                    player.sendMessage(parseMessage("invalidSpeed"));
                    return true;
                }

                double offsetX = 0;
                double offsetY = 0;
                double offsetZ = 0;
                try {
                    offsetX = Double.parseDouble(args[6]);
                    offsetY = Double.parseDouble(args[7]);
                    offsetZ = Double.parseDouble(args[8]);
                } catch (NumberFormatException e) {
                    // Send a message to the player if they specified an invalid offset
                    player.sendMessage(parseMessage("invalidOffset"));
                    return true;
                }

                Vector offset = new Vector(offsetX, offsetY, offsetZ);

                // Create a new particle generator at the player's location
                Location location = player.getLocation();
                ParticleGenerator generator = new ParticleGenerator(location, particle, count, interval, speed, offset);
                generators.put(name, generator);
                generator.start();

                // Save the generator to the config
                saveGenerator(name, generator);

                // Send a message to the player
                player.sendMessage(parseMessage("generatorCreated"));
            } else if (subCommand.equalsIgnoreCase("edit")) {
                if (args.length < 2) {
                    player.sendMessage(parseMessage("editUsage"));
                    return true;
                }

                String name = args[1];
                ParticleGenerator generator = generators.get(name);
                if (generator == null) {
                    // Send a message to the player if the generator doesn't exist
                    player.sendMessage(parseMessage("generatorDoesNotExist"));
                    return true;
                }

                boolean changed = false;
                if (args.length >= 3) {
                    Particle particle = null;
                    try {
                        particle = Particle.valueOf(args[2]);
                    } catch (IllegalArgumentException e) {
                        // Send a message to the player if they specified an invalid particle
                        player.sendMessage(parseMessage("invalidParticle"));
                        return true;
                    }
                    generator.setParticle(particle);
                    changed = true;
                }
                if (args.length >= 4) {
                    int count = 0;

                    try {
                        count = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        // Send a message to the player if they specified an invalid count
                        player.sendMessage(parseMessage("invalidCount"));
                        return true;
                    }
                    generator.setCount(count);
                    changed = true;
                }
                if (args.length >= 5) {
                    int interval = 0;
                    try {
                        interval = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) {
                        // Send a message to the player if they specified an invalid interval
                        player.sendMessage(parseMessage("invalidInterval"));
                        return true;
                    }
                    generator.setInterval(interval);
                    changed = true;
                }
                if (args.length >= 6) {
                    double speed = 0;
                    try {
                        speed = Double.parseDouble(args[5]);
                    } catch (NumberFormatException e) {
                        // Send a message to the player if they specified an invalid speed
                        player.sendMessage(parseMessage("invalidSpeed"));
                        return true;
                    }
                    generator.setSpeed(speed);
                    changed = true;
                }

                if (args.length >= 7) {
                    double offsetX = 0;
                    double offsetY = 0;
                    double offsetZ = 0;
                    try {
                        offsetX = Double.parseDouble(args[6]);
                        offsetY = Double.parseDouble(args[7]);
                        offsetZ = Double.parseDouble(args[8]);
                    } catch (NumberFormatException e) {
                        // Send a message to the player if they specified an invalid offset
                        player.sendMessage(parseMessage("invalidOffset"));
                        return true;
                    }
                    Vector offset = new Vector(offsetX, offsetY, offsetZ);
                    generator.setOffset(offset);
                    changed = true;
                }


                if (changed) {
                    generator.stop();
                    // Save the generator to the config
                    saveGenerator(name, generator);

                    generator.start();

                    // Send a message to the player
                    player.sendMessage(parseMessage("generatorEdited"));
                } else {
                    // Send a message to the player if they didn't specify any changes
                    player.sendMessage(parseMessage("noChanges"));
                }
            } else if (subCommand.equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    // Send a message to the player if they didn't specify enough arguments
                    player.sendMessage(parseMessage("deleteUsage"));
                    return true;
                }

                String name = args[1];
                ParticleGenerator generator = generators.remove(name);
                if (generator == null) {
                    // Send a message to the player if the generator doesn't exist
                    player.sendMessage(parseMessage("generatorDoesNotExist"));
                    return true;
                }

                // Stop the generator task
                generator.stop();

                // Remove the generator from the config
                FileConfiguration config = getConfig();
                config.set("generators." + name, null);
                saveConfig();

                // Send a message to the player
                player.sendMessage(parseMessage("generatorDeleted"));
            } else if (args[0].equalsIgnoreCase("list")) {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(parseMessage("invalidPage"));
                        return true;
                    }
                }
                int pageSize = 5;
                int totalPages = (int) Math.ceil((double) generators.size() / pageSize);
                if (page < 1 || page > totalPages) {
                    sender.sendMessage(parseMessage("invalidPage"));
                    return true;
                }
                sender.sendMessage(parseMessage("paginTitle", new String[]{"" + page, "" + totalPages}));
                int startIndex = (page - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, generators.size());
                List<Map.Entry<String, ParticleGenerator>> generatorList = new ArrayList<>(generators.entrySet());
                for (int i = startIndex; i < endIndex; i++) {
                    Map.Entry<String, ParticleGenerator> entry = generatorList.get(i);
                    String name = entry.getKey();
                    ParticleGenerator generator = entry.getValue();
                    Location location = generator.getLocation();
                    sender.sendMessage(parseMessage("paginItem", new String[]{name, String.valueOf(generator.getParticle()), location.getWorld().getName(), "" + location.getX(), "" + location.getY(), "" + location.getZ(), "" + generator.getCount(), "" + generator.getInterval(), "" + generator.getSpeed(), "" + generator.getOffset().getX(), "" + generator.getOffset().getY(), "" + generator.getOffset().getZ()}, false));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("move")) {
                if (args.length < 2) {
                    sender.sendMessage(parseMessage("moveUsage"));
                    return true;
                }
                String name = args[1];
                ParticleGenerator generator = generators.get(name);
                if (generator == null) {
                    sender.sendMessage(parseMessage("generatorDoesNotExist"));
                    return true;
                }
                Location location;
                if (args.length < 6) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(parseMessage("moveFromConsole"));
                        return true;
                    }
                    location = ((Player) sender).getLocation();
                } else {
                    String worldName = args[2];
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        sender.sendMessage(parseMessage("worldNotFound", new String[]{worldName}));
                        return true;
                    }
                    try {
                        double x = Double.parseDouble(args[3]);
                        double y = Double.parseDouble(args[4]);
                        double z = Double.parseDouble(args[5]);
                        location = new Location(world, x, y, z);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(parseMessage("invalidCoords"));
                        return true;
                    }
                }
                generator.setLocation(location);
                sender.sendMessage(parseMessage("generatorMoved", new String[]{"" + location.getX(), "" + location.getY(), "" + location.getZ(), location.getWorld().getName()}));
                saveGenerator(name, generator);
                return true;
            } else if (args[0].equalsIgnoreCase("set")) {
                if (args.length < 3) {
                    sender.sendMessage(parseMessage("setUsage"));
                    return true;
                }
                String name = args[1];
                ParticleGenerator generator = generators.get(name);
                if (generator == null) {
                    sender.sendMessage(parseMessage("generatorDoesNotExist"));
                    return true;
                }
                String property = args[2];
                if (property.equalsIgnoreCase("particle")) {
                    if (args.length < 4) {
                        sender.sendMessage(parseMessage("setPropertyUsage", new String[]{name, "particle"}));
                        return true;
                    }
                    try {
                        Particle particle = Particle.valueOf(args[3].toUpperCase());
                        generator.setParticle(particle);
                        sender.sendMessage(parseMessage("setPropertyFeedback", new String[]{name, "particle", "" + particle}));
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(parseMessage("invalidParticle"));
                        return true;
                    }
                } else if (property.equalsIgnoreCase("count")) {
                    if (args.length < 4) {
                        sender.sendMessage(parseMessage("setPropertyUsage", new String[]{name, "count"}));
                        return true;
                    }
                    try {
                        int count = Integer.parseInt(args[3]);
                        generator.setCount(count);
                        sender.sendMessage(parseMessage("setPropertyFeedback", new String[]{name, "count", "" + count}));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(parseMessage("invalidCount"));
                        return true;
                    }
                } else if (property.equalsIgnoreCase("interval")) {
                    if (args.length < 4) {
                        sender.sendMessage(parseMessage("setPropertyUsage", new String[]{name, "interval"}));;
                        return true;
                    }
                    try {
                        int interval = Integer.parseInt(args[3]);
                        generator.setInterval(interval);
                        sender.sendMessage(parseMessage("setPropertyFeedback", new String[]{name, "interval", "" + interval}));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(parseMessage("invalidInterval"));
                        return true;
                    }
                } else if (property.equalsIgnoreCase("speed")) {
                    if (args.length < 4) {
                        sender.sendMessage(parseMessage("setPropertyUsage", new String[]{name, "speed"}));
                        return true;
                    }
                    try {
                        double speed = Double.parseDouble(args[3]);
                        generator.setSpeed(speed);
                        sender.sendMessage(parseMessage("setPropertyFeedback", new String[]{name, "speed", "" + speed}));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(parseMessage("invalidSpeed"));
                        return true;
                    }
                } else if (property.equalsIgnoreCase("offset")) {
                    if (args.length < 4) {
                        sender.sendMessage(parseMessage("setOffsetUsage", new String[]{name}));
                        return true;
                    }
                    try {
                        double x = Double.parseDouble(args[3]);
                        double y = Double.parseDouble(args[4]);
                        double z = Double.parseDouble(args[5]);
                        Vector offset = new Vector(x, y, z);
                        generator.setOffset(offset);
                        sender.sendMessage(parseMessage("setOffsetFeedback", new String[]{name, "" + x, "" + y, "" + z}));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(parseMessage("invalidOffset"));
                        return true;
                    }
                } else {
                    sender.sendMessage(parseMessage("invalidProperty"));
                    return true;
                }
                generator.stop();
                saveGenerator(name, generator);
                generator.start();
                return true;
            } else {
                // Send a message to the player if they specified an invalid subcommand
                player.sendMessage(parseMessage("invalidCommand"));
            }
        } else {
            // Send a message to the console if the command was executed from the console
            getLogger().info(parseMessage("noConsole", false));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Tab complete subcommands
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "edit", "delete", "list", "set", "move"), new ArrayList<>());
        } else if (args.length == 2) {
            // Tab complete generator names
            if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("move")) {
                return StringUtil.copyPartialMatches(args[1], generators.keySet(), new ArrayList<>());
            }
        } else if (args.length == 3) {
            // Tab complete particle names
            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("edit")) {
                return StringUtil.copyPartialMatches(args[2], Arrays.stream(Particle.values()).map(Enum::name).collect(Collectors.toList()), new ArrayList<>());
            } else if (args[0].equalsIgnoreCase("set")) {
                // Tab complete properties
                return StringUtil.copyPartialMatches(args[2], Arrays.asList("particle", "count", "interval", "speed", "offset"), new ArrayList<>());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("particle")) {
            // Tab complete particle names
            return StringUtil.copyPartialMatches(args[3], Arrays.stream(Particle.values()).map(Enum::name).collect(Collectors.toList()), new ArrayList<>());
        }
        return Collections.emptyList();
    }




            private void saveGenerator(String name, ParticleGenerator generator) {
        // Save the generator to the config
        FileConfiguration config = getConfig();
        String generatorString = generator.getLocation().getWorld().getName() + ";" +
                generator.getLocation().getX() + ";" +
                generator.getLocation().getY() + ";" +
                generator.getLocation().getZ() + ";" +
                generator.getParticle() + ";" +
                generator.getCount() + ";" +
                generator.getInterval() + ";" +
                generator.getSpeed() + ";" +
                generator.getOffset().getX() + ";" +
                generator.getOffset().getY() + ";" +
                generator.getOffset().getZ();
        config.set("generators." + name, generatorString);
        saveConfig();
    }



    private class ParticleGenerator {
        private Location location;
        private Particle particle;
        private int count;
        private int interval;
        private BukkitTask task;
        private double speed;
        private Vector offset;




        public ParticleGenerator(Location location, Particle particle, int count, int interval, double speed, Vector offset) {
            this.location = location;
            this.particle = particle;
            this.count = count;
            this.interval = interval;
            this.speed = speed;
            this.offset = offset;
        }

        public void start() {
            if (task == null) {
                task = Bukkit.getScheduler().runTaskTimer(ParticleGeneratorPlugin.this, new Runnable() {
                    @Override
                    public void run() {
                        location.getWorld().spawnParticle(particle, location, count, offset.getX(), offset.getY(), offset.getZ(), speed, null, true);
                    }
                }, 0, interval);
            }
        }

        public void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public Particle getParticle() {
            return particle;
        }

        public void setParticle(Particle particle) {
            this.particle = particle;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getInterval() {
            return interval;
        }

        public void setInterval(int interval) {
            this.interval = interval;
        }

        public double getSpeed() {
            return speed;
        }

        public void setSpeed(double speed) {
            this.speed = speed;
        }

        public Vector getOffset() {
            return offset;
        }

        public void setOffset(Vector offset) {
            this.offset = offset;
        }
    }

    private String parseMessage(String messageName, String[] replacements, boolean prefix) {
        String message = getConfig().getString("messages." + messageName);
        if(message == null) {
            return "";
        }
        if (replacements != null) {
            for (int i = 0; i < replacements.length; i++) {
                String placeholder = "%" + i;
                if (i >= 10) {
                    placeholder = "%" + (char)('a' + (i - 10));
                }
                message = message.replace(placeholder, replacements[i]);
            }

        }
        return ChatColor.translateAlternateColorCodes('&', (prefix ? getConfig().getString("messages.prefix") : "") + message);
    }

    private String parseMessage(String messageName) {
        return parseMessage(messageName, null, true);
    }

    private String parseMessage(String messageName, boolean prefix) {
        return parseMessage(messageName, null, prefix);
    }

    private String parseMessage(String messageName, String[] replacements) {
        return parseMessage(messageName, replacements, true);
    }


}



