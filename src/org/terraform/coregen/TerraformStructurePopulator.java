package org.terraform.coregen;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.terraform.biome.BiomeBank;
import org.terraform.data.SimpleChunkLocation;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TConfigOption;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.structure.StructurePopulator;
import org.terraform.structure.VillageHousePopulator;
import org.terraform.structure.caves.LargeCavePopulator;
import org.terraform.structure.dungeon.SmallDungeonPopulator;
import org.terraform.structure.mineshaft.MineshaftPopulator;
import org.terraform.structure.monument.MonumentPopulator;
import org.terraform.structure.shipwreck.ShipwreckPopulator;
import org.terraform.structure.stronghold.StrongholdPopulator;
import org.terraform.utils.GenUtils;

public class TerraformStructurePopulator extends BlockPopulator {
	
	public static final ArrayList<StructurePopulator> structurePops = new ArrayList<StructurePopulator>(){{
		add(new StrongholdPopulator());
		add(new VillageHousePopulator());
		add(new SmallDungeonPopulator());
		add(new MonumentPopulator());
		add(new ShipwreckPopulator());
		add(new MineshaftPopulator());
		add(new LargeCavePopulator());
	}};
	
	TerraformWorld tw;
	public TerraformStructurePopulator(TerraformWorld tw){
		this.tw = tw;
	}
	
	private ArrayList<SimpleChunkLocation> populating = new ArrayList<SimpleChunkLocation>();
	
	@Override
	public void populate(World world, Random random, Chunk chunk) {
		
		//Don't attempt generation pre-injection.
		if(!TerraformGeneratorPlugin.injectedWorlds.contains(world.getName())) 
			return;
		if(populating.contains(new SimpleChunkLocation(chunk))) {
			return;
		}else {
			populating.add(new SimpleChunkLocation(chunk));
		}
		
		
		PopulatorDataPostGen data = new PopulatorDataPostGen(chunk);
		
		//Use IChunkAccess to place blocks instead. Known to cause lighting problems.
		if(TConfigOption.DEVSTUFF_EXPERIMENTAL_STRUCTURE_PLACEMENT.getBoolean())
			data = new PopulatorDataRecursiveICA(chunk);
		
		//PopulatorDataAbstract data = TerraformGeneratorPlugin.injector.getICAData(chunk);
		//TerraformGeneratorPlugin.logger.debug("s-pop-1");
		ArrayList<BiomeBank> banks = GenUtils.getBiomesInChunk(tw, data.getChunkX(), data.getChunkZ());
		
		//TerraformGeneratorPlugin.logger.debug("s-pop-2");
		for(StructurePopulator spop:structurePops){
			
			//TerraformGeneratorPlugin.logger.debug("s-pop-3");
			if(spop.canSpawn(tw,data.getChunkX(),data.getChunkZ(),banks)){
				TerraformGeneratorPlugin.logger.info("Generating " + spop.getClass().getName() + " at chunk: " + data.getChunkX() + "," + data.getChunkZ());
				spop.populate(tw, data);
			}
		}
		populating.remove(new SimpleChunkLocation(chunk));
	}

}
