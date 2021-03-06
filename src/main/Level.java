package main;

import java.util.ArrayList;
import java.util.Iterator;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.TrueTypeFont;

import tiles.Tile;
import tiles.TileMap;
import tiles.Tileset;

public class Level
{
	final float TIME_STEP = 1.0f / 60.0f;
	final int POSITION_ITERATION = 10;
	final int VELOCITY_ITERATION = 10;

	float pixelsPerMeter;
	
	public String name;
	public TileMap tileMap;
	public Tileset tileset;
	
	public float tileSizeInPixels;
	public float tileSizeInMeters;
	
	public float levelScale = 1.0f;
	
	public int x;
	public int y;
	
	public int numRows;
	public int numColumns;
	
	public Tile[][] backgroundArray;
	public Tile[][] foregroundArray;
	
	public ArrayList<Tile> spawnTiles;
	public int currentSpawn = 0;
	
	public ArrayList<Bomb> bombs;
	
	private ArrayList<Explosion> explosions; //Do not modify this directly
	private ArrayList<Explosion> explosionsToBeAdded;
	
	public World world; //Box2d world for physics
	
	public Level(TileMap tileMap, Tileset tileset)
	{
		this.tileMap = tileMap;
		this.tileset = tileset;
		this.name = tileMap.name;
		
		//Create the physics world
		Vec2 gravity = new Vec2(0, 0);
		world = new World(gravity);
	}
	
	public void init() throws Exception
	{
		tileMap.init();
		tileset.init();
		
		tileSizeInPixels = tileset.tileSize;
		pixelsPerMeter = tileSizeInPixels;
		tileSizeInMeters = pixelsToMeters(tileSizeInPixels);
		
		numColumns = tileMap.numColumns;
		numRows = tileMap.numRows;
		backgroundArray = new Tile[numColumns][numRows];
		foregroundArray = new Tile[numColumns][numRows];
		
		initBoundaries();
		
		spawnTiles = new ArrayList<Tile>();
		
		//Convert background and foreground data into Tiles
		for(int r = 0; r < numRows; r++)
		{
			for(int c = 0; c < numColumns; c++)
			{
				float x = (c * tileSizeInMeters) + (tileSizeInMeters / 2);
				float y = (r * tileSizeInMeters) + (tileSizeInMeters / 2);
				
				int tileID = tileMap.background[c][r];
				
				if(tileset.getTileType(tileID) == null)
				{
					System.err.println("Tile id " + tileID + " not found in tileset!");
					tileID = -1;
				}
				
				Tile bgTile = new Tile(x, y, r, c, tileSizeInMeters, tileset.getTileType(tileID), this);
				
				backgroundArray[c][r] = bgTile;
				
				//If the tile is a spawn tile, add it to the list
				if(bgTile.tileType.spawn)
				{
					spawnTiles.add(bgTile);
				}
				
				tileID = tileMap.foreground[c][r];
				
				if(tileset.getTileType(tileID) == null)
				{
					System.err.println("Tile id " + tileID + " not found in tileset!");
					tileID = -1;
				}
				
				Tile fgTile = new Tile(x, y, r, c, tileSizeInMeters, tileset.getTileType(tileID), this);
				
				foregroundArray[c][r] = fgTile;
				
				if(fgTile.tileType.spawn)
				{
					spawnTiles.add(fgTile);
				}
			}
		}
		
		bombs = new ArrayList<Bomb>();
		explosions = new ArrayList<Explosion>();
		explosionsToBeAdded = new ArrayList<Explosion>();
	}
	
	private void initBoundaries()
	{
		float thickness = 1f;

		float worldWidth = tileSizeInMeters * numColumns;
		float worldHeight = tileSizeInMeters * numRows;
		
		float xMiddle = worldWidth / 2;
		float yMiddle = worldHeight / 2;
		
		LevelBoundary top = new LevelBoundary(xMiddle, - thickness / 2, worldWidth, thickness, this);
		LevelBoundary bottom = new LevelBoundary(xMiddle, worldHeight + thickness / 2, worldWidth, thickness, this);
		LevelBoundary left = new LevelBoundary(-thickness / 2, yMiddle, thickness, worldHeight, this);
		LevelBoundary right = new LevelBoundary(worldWidth + thickness / 2, yMiddle, thickness, worldHeight, this);
	}
	
	public void render(Graphics g)
	{
		render(g, 0, 0);
	}
	
	public void render(Graphics g, int x, int y)
	{
		render(g, x, y, (int)getWidth(), (int)getHeight());
	}

	public void render(Graphics g, int x, int y, int drawWidth, int drawHeight)
	{
		//Scale the game so the level takes up the whole allotted area
		levelScale = Math.min(drawWidth / getWidth(), drawHeight / getHeight());
		
		//Center the level vertically in the allocated area
		if(drawHeight > getScaledHeight())
		{
			y += (drawHeight - getScaledHeight()) / 2;
		}
		
		//Center the level horizontally in the allocated area
		if(drawWidth > getScaledWidth())
		{
			x += (drawWidth - getScaledWidth()) / 2;
		}
		
		this.x = x;
		this.y = y;
		
		//Move the map to the desired coordinates
		g.translate(x, y);
		g.scale(levelScale, levelScale);
		
		for(int r = 0; r < numRows; r++)
		{
			for(int c = 0; c < numColumns; c++)
			{
				//Draw background
				if(backgroundArray[c][r].tileType.visible)
				{
					backgroundArray[c][r].render(g);
				}
				
				//Draw foreground
				if(foregroundArray[c][r].tileType.visible)
				{
					foregroundArray[c][r].render(g);
				}
			}	
		}
		
		for(Bomb b : bombs)
		{
			b.render();
		}
		
		for(Explosion e : explosions)
		{
			e.render();
		}
		
		g.resetTransform();
	}
	
	public void renderPlayers(Graphics g, ArrayList<Player> players)
	{
		g.translate(x, y);
		g.scale(levelScale, levelScale);
		
		TrueTypeFont playerNameFont = ResourceManager.getFont(ResourceManager.PLAYER_NAME_FONT);
		
		for(Player p : players)
		{
			if(p.isActive())
			{
				p.render(g);
				
				float nameLength = playerNameFont.getWidth(p.name);
				
				playerNameFont.drawString(p.getPixelsX() - nameLength / 2, p.getPixelsY(), p.name);
			}
		}
		
		g.resetTransform();
	}
	
	public void update(GameContainer gc)
	{
		world.step(TIME_STEP, VELOCITY_ITERATION, POSITION_ITERATION);
		
		//Update bombs
		
		Iterator<Bomb> bombIter = bombs.iterator();
		while (bombIter.hasNext()) 
		{
		    Bomb b = bombIter.next();

			if(b.isActive())
			{
				b.update();
			}
			else
			{
				bombIter.remove();
			}
		}
		
		//Add all pending explosions to the level
		for(Explosion e : explosionsToBeAdded)
		{
			explosions.add(e);
		}
		explosionsToBeAdded = new ArrayList<Explosion>();
		
		//Update all active explosions. Remove all inactive explosions
		Iterator<Explosion> explosionIter = explosions.iterator();
		while (explosionIter.hasNext()) 
		{
		    Explosion e = explosionIter.next();

			if(e.active)
			{
				e.update();
			}
			else
			{
				explosionIter.remove();
			}
		}
	}
	
	public void setForegroundTile(int row, int column, Tile newTile)
	{
		world.destroyBody(foregroundArray[column][row].body);
		foregroundArray[column][row] = newTile;
	}
	
	public float pixelsToMeters(float pixels)
	{
		return pixels / pixelsPerMeter;
	}
	
	public float metersToPixels(float meters)
	{
		return meters * pixelsPerMeter;
	}
	
	public float getWidth()
	{
		return numColumns * tileSizeInPixels;
	}
	
	public float getHeight()
	{
		return numRows * tileSizeInPixels;
	}
	
	public float getScaledWidth()
	{
		return getWidth() * levelScale;
	}
	
	public float getScaledHeight()
	{
		return getHeight() * levelScale;
	}
	
	public Vec2 getSpawnPoint()
	{
		if(spawnTiles.size() > 0)
		{
			Tile spawnTile = spawnTiles.get(currentSpawn);
			currentSpawn++;
			if(currentSpawn == spawnTiles.size())
			{
				currentSpawn = 0;
			}
			return new Vec2(spawnTile.body.getPosition().x, spawnTile.body.getPosition().y);
		}
		return new Vec2(1.0f, 1.0f);
	}
	
	public void addExplosion(Explosion e)
	{
		explosionsToBeAdded.add(e);
	}
}
