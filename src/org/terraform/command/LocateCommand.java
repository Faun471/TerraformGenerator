package org.terraform.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.drycell.command.DCArgument;
import org.drycell.command.DCCommand;
import org.drycell.command.InvalidArgumentException;
import org.drycell.main.DrycellPlugin;
import org.terraform.biome.BiomeBank;
import org.terraform.coregen.TerraformGenerator;
import org.terraform.coregen.TerraformStructurePopulator;
import org.terraform.data.MegaChunk;
import org.terraform.data.TerraformWorld;
import org.terraform.main.LangOpt;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.structure.SingleMegaChunkStructurePopulator;
import org.terraform.structure.StructurePopulator;
import org.terraform.utils.GenUtils;


public class LocateCommand extends DCCommand implements Listener{
	
	@EventHandler
	public void onLocateCommand(PlayerCommandPreprocessEvent event) {
		if(event.getPlayer().getWorld().getGenerator() instanceof TerraformGenerator) {
			if(event.getMessage().startsWith("/locate")) {
				event.getPlayer().sendMessage(LangOpt.COMMAND_LOCATE_NOVANILLA.parse());
				event.getPlayer().sendMessage("");
			}
		}
	}

	public LocateCommand(DrycellPlugin plugin, String... aliases) {
		super(plugin, aliases);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		this.parameters.add(new StructurePopulatorArgument("structureType",true));
	}

	@Override
	public String getDefaultDescription() {
		return "Locates nearest TerraformGenerator structures. Do /terra locate for all searchable structures.";
	}

	@Override
	public boolean canConsoleExec() {
		return true;
	}

	@Override
	public boolean hasPermission(CommandSender sender) {
		
		return sender.isOp() || sender.hasPermission("terraformgenerator.locate");
	}
	
	@Override
	public void execute(CommandSender sender, Stack<String> args)
			throws InvalidArgumentException {
		ArrayList<Object> params = this.parseArguments(sender, args);
		if(params.size() == 0) {
			sender.sendMessage(LangOpt.COMMAND_LOCATE_LIST_HEADER.parse());
			for(StructurePopulator spop:TerraformStructurePopulator.structurePops) {
				sender.sendMessage(LangOpt.COMMAND_LOCATE_LIST_ENTRY.parse("%entry%",spop.getClass().getSimpleName().replace("Populator", "")));
			}
			return;
		}
		if(!(sender instanceof Player)) {
			sender.sendMessage(LangOpt.fetchLang("permissions.console-cannot-exec"));
			return;
		}
		StructurePopulator spop = (StructurePopulator) params.get(0); //TODO: Get populator by name
		
		Player p = (Player) sender;	
//		if(!(p.getWorld().getGenerator() instanceof TerraformGenerator)) {
//			p.sendMessage(ChatColor.RED + "Can only be used in TerraformGenerator worlds!");
//			return;
//		}
		if(!spop.isEnabled()) {
			p.sendMessage(LangOpt.COMMAND_LOCATE_STRUCTURE_NOT_ENABLED.parse());
			return;
		}
		
		if(!(spop instanceof SingleMegaChunkStructurePopulator)) {
			int[] coords = spop.getNearestFeature(TerraformWorld.get(p.getWorld()), p.getLocation().getBlockX(),  p.getLocation().getBlockZ());
			syncSendMessage(p.getUniqueId(),LangOpt.COMMAND_LOCATE_LOCATE_COORDS.parse("%x%",coords[0]+"","%z%",coords[1]+""));
			return;
		}
		
		SingleMegaChunkStructurePopulator populator = (SingleMegaChunkStructurePopulator) spop;
		
		MegaChunk center = new MegaChunk(
				p.getLocation().getBlockX(),
				p.getLocation().getBlockY(),
				p.getLocation().getBlockZ());
		TerraformWorld tw = TerraformWorld.get(p.getWorld());
		p.sendMessage(LangOpt.COMMAND_LOCATE_SEARCHING.parse());
		UUID uuid = p.getUniqueId();
		
		final long startTime = System.currentTimeMillis();
		
        BukkitRunnable runnable = new BukkitRunnable() {
        	public void run() {
        		int blockX = -1;
        		int blockZ = -1;
        		int radius = 0;
        		boolean found = false;
        		
        		while(!found) {
        			for(MegaChunk mc:getSurroundingChunks(center,radius)) {
        				int[] coords = populator.getCoordsFromMegaChunk(tw, mc);
        				ArrayList<BiomeBank> banks = GenUtils.getBiomesInChunk(tw, coords[0]>>4, coords[1]>>4);
        				
        				if(populator.canSpawn(tw, coords[0]>>4, coords[1]>>4, banks)) {
        					found= true;
        					blockX = coords[0];
        					blockZ = coords[1];
        					break;
        				}
        			}
        			radius++;
        		}
        		long timeTaken = System.currentTimeMillis()-startTime;
        		
        		syncSendMessage(uuid,LangOpt.COMMAND_LOCATE_COMPLETED_TASK.parse("%time%",timeTaken+""));
        		
        		if(found)
        			syncSendMessage(uuid,LangOpt.COMMAND_LOCATE_LOCATE_COORDS.parse("%x%",blockX+"","%z%",blockZ+""));
        		else
        			syncSendMessage(uuid,ChatColor.RED + "Failed to find structure. Somehow.");

        	}
        };
        runnable.runTaskAsynchronously(plugin);
	}
	
	
	private Collection<MegaChunk> getSurroundingChunks(MegaChunk center, int radius){
		if(radius == 0) return new ArrayList<MegaChunk>() {{ add(center); }};
		//     xxxxx
		//xxx  x   x
		//xox  x o x
		//xxx  x   x
		//     xxxxx
		ArrayList<MegaChunk> candidates = new ArrayList<MegaChunk>();
		for(int rx = -radius; rx <= radius; rx++) {
			for(int rz = -radius; rz <= radius; rz++) {
				
				//Check that this is a border coord
				if(Math.abs(rx) == radius || Math.abs(rz) == radius) {
					candidates.add(center.getRelative(rx, rz));
				}
			}
		}
		
		return candidates;
	}
	
	private void syncSendMessage(UUID uuid, String message) {
		for(Player p:Bukkit.getOnlinePlayers()) {
			if(p.getUniqueId() == uuid) {
				p.sendMessage(message);
				break;
			}
		}
		TerraformGeneratorPlugin.logger.info("[Locate Command] "+message);
	}
	
	public static class StructurePopulatorArgument extends DCArgument<StructurePopulator>{

		public StructurePopulatorArgument(String name, boolean isOptional) {
			super(name, isOptional);
		}

		@Override
		public StructurePopulator parse(CommandSender arg0, String arg1) {
			
			for(StructurePopulator spop:TerraformStructurePopulator.structurePops) {
				if(spop.getClass().getSimpleName().equalsIgnoreCase(arg1)||
						spop.getClass().getSimpleName().equalsIgnoreCase(arg1+"populator"))
					return spop;
			}
			return null;
		}

		@Override
		public String validate(CommandSender arg0, String arg1) {
			if(this.parse(arg0,arg1) != null)
				return "";
			else
				return "Structure type does not exist";
		}
	}

}
