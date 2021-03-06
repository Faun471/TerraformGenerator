package org.terraform.coregen.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.bukkit.block.data.BlockData;

public class SQLiteDB {
	/***
	 * Welcome to my failed frankenstein experiment that did not work
	 * even in the slightest. Be free to marvel at the rubberband code
	 * that I have graced upon this Earth.
	 */
	private static SQLiteDB i;
	public static SQLiteDB get(){
		if(i == null) i = new SQLiteDB();
		return i;
	}
	
	/**
	 * Create an entry in the BlocData table
	 * @param world
	 * @param chunkX
	 * @param chunkZ
	 * @param populated
	 */
	public void updateBlockData(Connection c, Statement stmt, String world, int chunkX, int chunkZ, int x, int y, int z, BlockData data){
		createTableIfNotExists(world);
		String dir = "plugins" 
    		   + File.separator 
    		   + "TerraformGenerator"
    		   + File.separator
    		   + world + ".db";
	    final String CHUNK = chunkX + "," + chunkZ;
	    final String COORDS = x + "," + y + "," + z;
	    final String DATA = data.toString();
	    try {
	      Class.forName("org.sqlite.JDBC");
	      c = DriverManager.getConnection("jdbc:sqlite:" + dir);
	      c.setAutoCommit(false);

	      stmt = c.createStatement();
	      String sql = "DELETE from BLOCKDATA where CHUNK='" + CHUNK + "' and"
	      		+ " COORDS='" + COORDS + "';";
	      stmt.executeUpdate(sql);
	     
	      sql = "INSERT INTO BLOCKDATA (CHUNK,COORDS,DATA) " +
	                   "VALUES ('" + CHUNK + "', '" 
	                   + COORDS + "', '"
	                   + DATA +"');"; 
	      stmt.executeUpdate(sql);

	      stmt.close();
	      c.commit();
	      c.close();
	    } catch ( Exception e ) {
	    	e.printStackTrace();
	    }		
	}
	
	/**
	 * Returns a boolean array. Index 0 is whether or not the chunk exists
	 * in the table. Index 1 is whether or not the chunk was populated yet.
	 * @param world
	 * @param chunkX
	 * @param chunkZ
	 * @return
	 */
	public boolean[] fetchFromChunks(String world, int chunkX, int chunkZ){
		createTableIfNotExists(world);
		Connection c = null;
	    Statement stmt = null;
		String dir = "plugins" 
		   + File.separator 
		   + "TerraformGenerator"
		   + File.separator
		   + world + ".db";
	    final String key = chunkX + "," + chunkZ;
	    boolean[] queryReply = new boolean[]{false,false};
	    try {
	      Class.forName("org.sqlite.JDBC");
	      c = DriverManager.getConnection("jdbc:sqlite:" + dir);
	      c.setAutoCommit(false);

	      stmt = c.createStatement();
	      ResultSet rs = stmt.executeQuery( "SELECT * FROM CHUNKS WHERE CHUNK='" + key +"';");
	      if( rs.next() ) {
	    	  queryReply =  new boolean[]{true, rs.getBoolean("POPULATED")};
	      }else{
	    	  queryReply =  new boolean[]{false,false};
	      }
	      rs.close();
	      stmt.close();
	      c.close();
	    } catch ( Exception e ) {
	    	e.printStackTrace();
	    	//Bukkit.getLogger().severe(e.getClass().getName() + "[" + e.getCause() +"]" + ":" + e.getMessage() );
		}
		
		return queryReply;
	}
	
	/**
	 * Create or update an entry in the Chunk table
	 * @param world
	 * @param chunkX
	 * @param chunkZ
	 * @param populated
	 */
	public void putChunk(String world, int chunkX, int chunkZ, boolean populated){
		createTableIfNotExists(world);
		String dir = "plugins" 
    		   + File.separator 
    		   + "TerraformGenerator"
    		   + File.separator
    		   + world + ".db";
		Connection c = null;
	    Statement stmt = null;
	    final String key = chunkX + "," + chunkZ;
	    try {
	      Class.forName("org.sqlite.JDBC");
	      c = DriverManager.getConnection("jdbc:sqlite:" + dir);
	      c.setAutoCommit(false);

	      stmt = c.createStatement();
	      String sql = "DELETE from CHUNKS where CHUNK='" + key + "';";
	      stmt.executeUpdate(sql);
	     
	      sql = "INSERT INTO CHUNKS (CHUNK,POPULATED) " +
	                   "VALUES ('" + key + "', '" 
	                   + populated + "');"; 
	      stmt.executeUpdate(sql);

	      stmt.close();
	      c.commit();
	      c.close();
	    } catch ( Exception e ) {
	    	e.printStackTrace();
	    }		
	}
	
	
	private static ArrayList<String> preparedWorlds = new ArrayList<String>();
    /** 
    * Ensures that the database and all relevant tables exist.
    */  
   public static void createTableIfNotExists(String world) {  
	   if(preparedWorlds.contains(world)) return;
       Connection conn = null;  
       String dir = "plugins" 
    		   + File.separator 
    		   + "TerraformGenerator"
    		   + File.separator
    		   + world + ".db";
       try {  
           // db parameters  
           String url = "jdbc:sqlite:" + dir;  
           // create a connection to the database & create the table
           
           //Create Chunks Table
           conn = DriverManager.getConnection(url); 
		   Statement stmt = conn.createStatement();
           String sql = "CREATE TABLE IF NOT EXISTS CHUNKS " +
                   "(CHUNK STRING PRIMARY KEY     NOT NULL," +
                   " POPULATED           BOOLEAN     NOT NULL); "; 
           stmt.executeUpdate(sql);
           stmt.close(); 

           //Create BlockData table
		   stmt = conn.createStatement();
           sql = "CREATE TABLE IF NOT EXISTS BLOCKDATA " +
                   "(CHUNK STRING NOT NULL,"
                   + "COORDS STRING NOT NULL," +
                   " DATA STRING NOT NULL,"
                   + "PRIMARY KEY (CHUNK,COORDS)); "; 
           stmt.executeUpdate(sql);
           stmt.close();  
           preparedWorlds.add(world);
       } catch (SQLException e) {  
           e.printStackTrace();
       } finally {
          closeConn(conn);
       } 
   }
   
   private static void closeConn(Connection conn){
	   try {
		   if (conn != null) {   
			   conn.close();
		   }
	   } catch (SQLException ex) {
		   ex.printStackTrace();
	   }  
   }
}
