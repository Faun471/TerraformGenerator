package org.terraform.structure.small;

import java.util.Random;

import org.bukkit.Material;
import org.terraform.coregen.PopulatorDataAbstract;
import org.terraform.data.SimpleBlock;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TConfigOption;
import org.terraform.tree.FractalTreeBuilder;
import org.terraform.tree.FractalTreeType;
import org.terraform.utils.BlockUtils;
import org.terraform.utils.GenUtils;

public class GiantPumpkinPopulator{

	public void populate(TerraformWorld tw, Random random,
			PopulatorDataAbstract data) {
		if(!TConfigOption.STRUCTURES_SWAMPHUT_ENABLED.getBoolean()) return;
		int x = data.getChunkX()*16 + random.nextInt(16);
		int z = data.getChunkZ()*16 + random.nextInt(16);
		int height = GenUtils.getHighestGround(data, x, z);

		spawnGiantPumpkin(tw,random,data,x,height,z);
	}
	
	public void spawnGiantPumpkin(TerraformWorld tw, Random random,
			PopulatorDataAbstract data, int x, int y, int z){
		
		//Spawn the biggest pumpkin
		new FractalTreeBuilder(FractalTreeType.GIANT_PUMPKIN)
		.build(tw,data,x,y+1,z);
		
		//Spawn small pumpkins
		for(int i = 0 ; i < GenUtils.randInt(random,15,30); i++){
			int nx = x + GenUtils.getSign(random)*GenUtils.randInt(5,12);
			int nz = z + GenUtils.getSign(random)*GenUtils.randInt(5,12);
			int ny = GenUtils.getHighestGround(data,nx,nz);
			
			data.setType(nx, ny+1, nz, Material.PUMPKIN);
		}
		
		//Smol Bushes
		for(int i = 0 ; i < GenUtils.randInt(random,1,5); i++){
			int nx = x + GenUtils.getSign(random)*GenUtils.randInt(4,5);
			int nz = z + GenUtils.getSign(random)*GenUtils.randInt(4,5);
			int ny = GenUtils.getHighestGround(data,nx,nz);
			
			BlockUtils.setPersistentLeaves(data, nx, ny+1, nz);
		}
		
		//Spawn big bushes
		for(int i = 0 ; i < GenUtils.randInt(random,4,6); i++){
			int nx = x + GenUtils.getSign(random)*GenUtils.randInt(4,6);
			int nz = z + GenUtils.getSign(random)*GenUtils.randInt(4,6);
			int ny = GenUtils.getHighestGround(data,nx,nz);
			BlockUtils.replaceSphere(random.nextInt(9992), 
					3, 6, 3, new SimpleBlock(data,nx,ny,nz), false, Material.ACACIA_LEAVES);
			;
		}
	}

}
