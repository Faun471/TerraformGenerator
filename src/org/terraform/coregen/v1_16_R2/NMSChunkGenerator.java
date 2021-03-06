package org.terraform.coregen.v1_16_R2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R2.generator.CraftChunkData;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.terraform.coregen.TerraformGenerator;
import org.terraform.coregen.TerraformPopulator;
import org.terraform.data.TerraformWorld;
import org.terraform.structure.farmhouse.FarmhousePopulator;
import org.terraform.structure.monument.MonumentPopulator;
import org.terraform.structure.stronghold.StrongholdPopulator;

import com.mojang.serialization.Codec;

import net.minecraft.server.v1_16_R2.BiomeBase;
import net.minecraft.server.v1_16_R2.BiomeManager;
import net.minecraft.server.v1_16_R2.BiomeSettingsGeneration;
import net.minecraft.server.v1_16_R2.BiomeSettingsMobs;
import net.minecraft.server.v1_16_R2.BiomeStorage;
import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R2.ChunkGenerator;
import net.minecraft.server.v1_16_R2.ChunkGeneratorAbstract;
import net.minecraft.server.v1_16_R2.ChunkSection;
import net.minecraft.server.v1_16_R2.DefinedStructureManager;
import net.minecraft.server.v1_16_R2.EnumCreatureType;
import net.minecraft.server.v1_16_R2.GeneratorAccess;
import net.minecraft.server.v1_16_R2.HeightMap.Type;
import net.minecraft.server.v1_16_R2.IBlockAccess;
import net.minecraft.server.v1_16_R2.IChunkAccess;
import net.minecraft.server.v1_16_R2.IRegistry;
import net.minecraft.server.v1_16_R2.IRegistryCustom;
import net.minecraft.server.v1_16_R2.ITileEntity;
import net.minecraft.server.v1_16_R2.ProtoChunk;
import net.minecraft.server.v1_16_R2.RegionLimitedWorldAccess;
import net.minecraft.server.v1_16_R2.SeededRandom;
import net.minecraft.server.v1_16_R2.StructureGenerator;
import net.minecraft.server.v1_16_R2.StructureManager;
import net.minecraft.server.v1_16_R2.StructureSettings;
import net.minecraft.server.v1_16_R2.TileEntity;
import net.minecraft.server.v1_16_R2.WorldChunkManager;
import net.minecraft.server.v1_16_R2.WorldGenCanyonOcean;
import net.minecraft.server.v1_16_R2.WorldGenCarverWrapper;
import net.minecraft.server.v1_16_R2.WorldGenCavesOcean;
import net.minecraft.server.v1_16_R2.WorldGenStage;
import net.minecraft.server.v1_16_R2.WorldServer;

public class NMSChunkGenerator extends ChunkGenerator {
	
	public NMSChunkGenerator(String worldname, int seed, 
			WorldChunkManager worldchunkmanager, 
			WorldChunkManager worldchunkmanager1, 
			StructureSettings structuresettings, long i) {
		super(worldchunkmanager,worldchunkmanager1,structuresettings,i);
        tw = TerraformWorld.get(worldname,seed);
        pop = new TerraformPopulator(tw);
        world = ((CraftWorld) Bukkit.getWorld(worldname)).getHandle();
	}
	
	public TerraformWorld getTerraformWorld() {
		return tw;
	}

	private WorldServer world;
	private TerraformPopulator pop;
	private TerraformWorld tw;

	@Override
    public void createBiomes(IRegistry<BiomeBase> iregistry, IChunkAccess ichunkaccess) {
        ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();

        ((ProtoChunk) ichunkaccess).a(new BiomeStorage(iregistry, chunkcoordintpair, this.c));
    }

	@Override
    public BlockPosition findNearestMapFeature(WorldServer worldserver, StructureGenerator<?> structuregenerator, BlockPosition blockposition, int i, boolean flag) {
        //StructureGenerator<?> structuregenerator = (StructureGenerator) WorldGenerator.ao.get(s.toLowerCase(Locale.ROOT));
		int pX = blockposition.getX();
		int pZ = blockposition.getZ();
		if(structuregenerator == StructureGenerator.STRONGHOLD){
//			double minDistanceSquared = Integer.MAX_VALUE;
//			int[] min = null;
//			for(int[] loc:StrongholdPopulator.strongholdPositions(tw)){
//				double distSqr = Math.pow(loc[0]-pX,2) + Math.pow(loc[1]-pZ,2);
//				if(distSqr < minDistanceSquared){
//					minDistanceSquared = distSqr;
//					min = loc;
//				}
//			}
			int[] coords = new StrongholdPopulator().getNearestFeature(tw, pX, pZ);
			return new BlockPosition(coords[0],20,coords[1]);
		}else if(structuregenerator == StructureGenerator.VILLAGE){
			int[] coords = new FarmhousePopulator().getNearestFeature(tw, pX, pZ);
			return new BlockPosition(coords[0],100,coords[1]);
		}else if(structuregenerator == StructureGenerator.MONUMENT){
			int[] coords = new MonumentPopulator().getNearestFeature(tw, pX, pZ);
			return new BlockPosition(coords[0],100,coords[1]);
		}
		
        return null;
    }
	
    @Override
    public void addDecorations(RegionLimitedWorldAccess rlwa, StructureManager structuremanager) {
		int chunkX = rlwa.a();
        int chunkZ = rlwa.b();
        PopulatorData popDat = new PopulatorData(rlwa,this,chunkX,chunkZ);
        pop.populate(tw, rlwa.getRandom(), popDat);

    }
    
//    private BiomeBase getBiome(BiomeManager biomemanager, BlockPosition bp) {
//    	return CraftBlock.biomeToBiomeBase(this.b,tw.getBiomeBank(bp.getX(), bp.getY(), bp.getZ()).getHandler().getBiome());
//    }

    @Override
    public void doCarving(long i, BiomeManager biomemanager, IChunkAccess ichunkaccess, WorldGenStage.Features worldgenstage_features) {
        BiomeManager biomemanager1 = biomemanager.a(this.b);
        SeededRandom seededrandom = new SeededRandom();
        ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();
        int j = chunkcoordintpair.x;
        int k = chunkcoordintpair.z;
        BiomeSettingsGeneration biomesettingsgeneration = this.b.getBiome(chunkcoordintpair.x << 2, 0, chunkcoordintpair.z << 2).e();
        BitSet bitset = ((ProtoChunk) ichunkaccess).b(worldgenstage_features);

        for (int l = j - 8; l <= j + 8; ++l) {
            for (int i1 = k - 8; i1 <= k + 8; ++i1) {
                List<Supplier<WorldGenCarverWrapper<?>>> list = biomesettingsgeneration.a(worldgenstage_features);
                ListIterator<Supplier<WorldGenCarverWrapper<?>>> listiterator = list.listIterator();

                while (listiterator.hasNext()) {
                    int j1 = listiterator.nextIndex();
                    WorldGenCarverWrapper<?> worldgencarverwrapper = (WorldGenCarverWrapper<?>) ((Supplier<?>) listiterator.next()).get();

	                  if(WorldGenCarverWrapper.a instanceof WorldGenCavesOcean){
	                	  //if(!TConfigOption.CAVES_ALLOW_FLOODED_CAVES.getBoolean())
	              		continue;
		              }
		              if(WorldGenCarverWrapper.a instanceof WorldGenCanyonOcean){
		              		//if(!TConfigOption.CAVES_ALLOW_FLOODED_RAVINES.getBoolean())
		              		continue;
		              }
                    
                    seededrandom.c(i + (long) j1, l, i1);
                    if (worldgencarverwrapper.a(seededrandom, l, i1)) {
                        worldgencarverwrapper.a(ichunkaccess, biomemanager1::a, seededrandom, this.getSeaLevel(), l, i1, j, k, bitset);
                    }
                }
            }
        }

    }
    
//    @Override
//    public void doCarving(long seed, BiomeManager biomemanager, IChunkAccess ichunkaccess, WorldGenStage.Features worldgenstage_features) {
//        SeededRandom seededrandom = new SeededRandom();
//        BiomeManager biomemanager1 = biomemanager.a(this.b);
//        
//        ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();
//        int chunkX = chunkcoordintpair.x;
//        int chunkZ = chunkcoordintpair.z;
//        BiomeSettingsGeneration biomesettingsgeneration = this.b.getBiome(chunkcoordintpair.x << 2, 0, chunkcoordintpair.z << 2).e();
//        BitSet bitset = ((ProtoChunk) ichunkaccess).b(worldgenstage_features);
//        for (int k = chunkX - 8; k <= chunkX + 8; ++k) {
//            for (int l = chunkZ - 8; l <= chunkZ + 8; ++l) {
//            	List<Supplier<WorldGenCarverWrapper<?>>> list = biomesettingsgeneration.a(worldgenstage_features);
//
//                ListIterator<Supplier<WorldGenCarverWrapper<?>>> listiterator = list.listIterator();
//
//                while (listiterator.hasNext()) {
//                    int i1 = listiterator.nextIndex();
//                    WorldGenCarverWrapper<?> worldgencarverwrapper = (WorldGenCarverWrapper<?>) listiterator.next();
//                    if(WorldGenCarverWrapper.a instanceof WorldGenCavesOcean){
//                    	//if(!TConfigOption.CAVES_ALLOW_FLOODED_CAVES.getBoolean())
//                    		continue;
//                    }
//                    if(WorldGenCarverWrapper.a instanceof WorldGenCanyonOcean){
//                    	//if(!TConfigOption.CAVES_ALLOW_FLOODED_RAVINES.getBoolean())
//                    		continue;
//                    }
//                    seededrandom.c(seed + (long) i1, k, l);
//                    if (worldgencarverwrapper.a(seededrandom, k, l)) {
//                        worldgencarverwrapper.a(ichunkaccess, biomemanager1::a, seededrandom, this.getSeaLevel(), k, l, chunkX, chunkZ, bitset);
//                    }
//                }
//            }
//        }
//
//    }
//    
    @Override
    public int getSeaLevel(){
    	return TerraformGenerator.seaLevel;
    }
    
    @Override
    public void createStructures(IRegistryCustom iregistrycustom, StructureManager structuremanager, IChunkAccess ichunkaccess, DefinedStructureManager definedstructuremanager, long i) {
    
    }

	@Override
	public int getSpawnHeight() {
		return getBaseHeight(0,0,null);
	}


	 @Override
	 public void buildNoise(GeneratorAccess generatoraccess, StructureManager structuremanager, IChunkAccess ichunkaccess) {
	        
	}
	
    @Override
    public void buildBase(RegionLimitedWorldAccess regionlimitedworldaccess, IChunkAccess ichunkaccess) {
		try {
	        int x = ichunkaccess.getPos().x;
	        int z = ichunkaccess.getPos().z;
	        TerraformGenerator generator = new TerraformGenerator();
	        Random random = tw.getRand(3);
	        random.setSeed((long) x * 341873128712L + (long) z * 132897987541L);
	
	        // Get default biome data for chunk
	        CustomBiomeGrid biomegrid = new CustomBiomeGrid(new BiomeStorage(world.r().b(IRegistry.ay), ichunkaccess.getPos(), this.getWorldChunkManager()));
	        
	        ChunkData data;
	        if (generator.isParallelCapable()) {
	            data = generator.generateChunkData(tw.getWorld(), random, x, z, biomegrid);
	        } else {
	            synchronized (this) {
	                data = generator.generateChunkData(tw.getWorld(), random, x, z, biomegrid);
	            }
	        }
	
	        CraftChunkData craftData = (CraftChunkData) data;
	        Method getRawChunkData = CraftChunkData.class.getDeclaredMethod("getRawChunkData");
	        getRawChunkData.setAccessible(true);
	        ChunkSection[] sections = (ChunkSection[]) getRawChunkData.invoke(craftData);
	
	        ChunkSection[] csect = ichunkaccess.getSections();
	        int scnt = Math.min(csect.length, sections.length);
	
	        // Loop through returned sections
	        for (int sec = 0; sec < scnt; sec++) {
	            if (sections[sec] == null) {
	                continue;
	            }
	            ChunkSection section = sections[sec];
	
	            csect[sec] = section;
	        }
	
	        // Set biome grid
	        ((ProtoChunk) ichunkaccess).a(biomegrid.biome);
	        
	        Method getTiles;
				getTiles = CraftChunkData.class.getDeclaredMethod("getTiles");
	        getTiles.setAccessible(true);
	        @SuppressWarnings("unchecked")
			Set<BlockPosition> tiles = (Set<BlockPosition>) getTiles.invoke(craftData);
	        if (tiles != null) {
	            for (BlockPosition pos : tiles) {
	                int tx = pos.getX();
	                int ty = pos.getY();
	                int tz = pos.getZ();
	                net.minecraft.server.v1_16_R2.Block block = craftData.getTypeId(tx, ty, tz).getBlock();
	
	                if (block.isTileEntity()) {
	                    TileEntity tile = ((ITileEntity) block).createTile(((CraftWorld) tw.getWorld()).getHandle());
	                    ichunkaccess.setTileEntity(new BlockPosition((x << 4) + tx, ty, (z << 4) + tz), tile);
	                }
	            }
	        }

		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
    }

//	@Override
//	public void buildNoise(GeneratorAccess generatoraccess, IChunkAccess ichunkaccess) {
//		BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();
//        TerraformGenerator gen = new TerraformGenerator();
//        CustomBiomeGrid biomegrid = new CustomBiomeGrid(new BiomeStorage(ichunkaccess.getPos(), this.getWorldChunkManager()));
//        
//        ChunkData cd = gen.generateChunkData(tw.getWorld(), tw.getRand(3), ichunkaccess.getPos().x, ichunkaccess.getPos().z, biomegrid);
//        
//        for(int x = 0; x < 16; x++){
//        	for(int z = 0; z < 16; z++){
//        		for(int y = 0; y < 255; y++){
//    				int rawX = ichunkaccess.getPos().x*16 + x;
//    				int rawZ = ichunkaccess.getPos().z*16 + z;
//                	ichunkaccess.getBiomeIndex().setBiome(x, y, z, CraftBlock.biomeToBiomeBase(biomegrid.getBiome(x,z)));
//                	generatoraccess.setTypeAndData(blockposition_mutableblockposition.d(x,y,z), CraftBlockData.newData(cd.getType(x,y,z), "").getState(), 0);
//                }
//            }
//        }
////		for (int x = 0; x < 16; ++x) {
////            for (int z = 0; z < 16; ++z) {
////				int rawX = ichunkaccess.getPos().x*16 + x;
////				int rawZ = ichunkaccess.getPos().z*16 + z;
////            	//double n = noise.GetNoise(rawX, rawZ)*7 + currentHeight;
////				for(int y = (int) n; y >= 0; y--){
////					ichunkaccess.getBiomeIndex().setBiome(x, y, z, Biomes.PLAINS);
////					//chunk.setBlock(x, y, z, Material.GRASS_BLOCK);
////					IBlockData iblockdata = Blocks.GRASS_BLOCK.getBlockData();//Material.GRASS_BLOCK.createBlockData();
////	                
////					if(y < n-3){
////	                	iblockdata = Blocks.STONE.getBlockData();
////	                }
////					ichunkaccess.setType(blockposition_mutableblockposition.d(x,y,z), iblockdata, false);
////	                
////				}
//////                heightmap.a(j, i, k, iblockdata);
//////                heightmap1.a(j, i, k, iblockdata);
////            }
////        }
//	
//	}
    
    @Override
    public List<BiomeSettingsMobs.c> getMobsFor(BiomeBase biomebase, StructureManager structuremanager, EnumCreatureType enumcreaturetype, BlockPosition blockposition) {
        if (structuremanager.a(blockposition, true, StructureGenerator.SWAMP_HUT).e()) {
            if (enumcreaturetype == EnumCreatureType.MONSTER) {
                return StructureGenerator.SWAMP_HUT.c();
            }

            if (enumcreaturetype == EnumCreatureType.CREATURE) {
                return StructureGenerator.SWAMP_HUT.j();
            }
        }

        if (enumcreaturetype == EnumCreatureType.MONSTER) {
            if (structuremanager.a(blockposition, false, StructureGenerator.PILLAGER_OUTPOST).e()) {
                return StructureGenerator.PILLAGER_OUTPOST.c();
            }

            if (structuremanager.a(blockposition, false, StructureGenerator.MONUMENT).e()) {
                return StructureGenerator.MONUMENT.c();
            }

            if (structuremanager.a(blockposition, true, StructureGenerator.FORTRESS).e()) {
                return StructureGenerator.FORTRESS.c();
            }
        }

        return super.getMobsFor(biomebase, structuremanager, enumcreaturetype, blockposition);
    }

	@Override
	public int getBaseHeight(int i, int j, Type heightmap_type) {
		return org.terraform.coregen.HeightMap.getHeight(tw,i,j);
	}
	
   private class CustomBiomeGrid implements BiomeGrid {

        private final BiomeStorage biome;

        public CustomBiomeGrid(BiomeStorage biome) {
            this.biome = biome;
        }

        @Override
        public Biome getBiome(int x, int z) {
            return getBiome(x, 0, z);
        }

        @Override
        public void setBiome(int x, int z, Biome bio) {
            for (int y = 0; y < tw.getWorld().getMaxHeight(); y++) {
                setBiome(x, y, z, bio);
            }
        }

        @Override
        public Biome getBiome(int x, int y, int z) {
            return CraftBlock.biomeBaseToBiome((IRegistry<BiomeBase>) biome.g, biome.getBiome(x >> 2, y >> 2, z >> 2));
        }

        @Override
        public void setBiome(int x, int y, int z, Biome bio) {
            biome.setBiome(x >> 2, y >> 2, z >> 2, CraftBlock.biomeToBiomeBase((IRegistry<BiomeBase>) biome.g, bio));
        }
    }

   @Override
	protected Codec<? extends ChunkGenerator> a() {
		return ChunkGeneratorAbstract.d;
	}

   @Override //getBaseColumn
   public IBlockAccess a(int i, int j) {
//       IBlockData[] aiblockdata = new IBlockData[this.o * 256];
//
	   //iterateNoiseColumn
//       this.a(i, j, aiblockdata, (Predicate) null);
//       return new BlockColumn(aiblockdata);
	   return null;
   }
}
