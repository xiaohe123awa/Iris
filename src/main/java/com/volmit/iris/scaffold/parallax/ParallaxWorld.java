package com.volmit.iris.scaffold.parallax;

import java.io.File;
import java.io.IOException;

import com.volmit.iris.util.*;
import org.bukkit.block.data.BlockData;

import com.volmit.iris.scaffold.hunk.Hunk;

public class ParallaxWorld implements ParallaxAccess
{
	private final KMap<Long, ParallaxRegion> loadedRegions;
	private final KList<Long> save;
	private final File folder;
	private final int height;
	private final ChronoLatch cleanup;

	public ParallaxWorld(int height, File folder)
	{
		this.height = height;
		this.folder = folder;
		save = new KList<>();
		loadedRegions = new KMap<>();
		cleanup = new ChronoLatch(5000);
		folder.mkdirs();
	}

	public int getRegionCount()
	{
		return loadedRegions.size();
	}

	public int getChunkCount()
	{
		int m = 0;

		synchronized (loadedRegions)
		{
			for(ParallaxRegion i : loadedRegions.values())
			{
				m+= i.getChunkCount();
			}
		}

		return m;
	}

	public synchronized void close()
	{
		for(ParallaxRegion i : loadedRegions.v())
		{
			unload(i.getX(), i.getZ());
		}

		save.clear();
		loadedRegions.clear();
	}

	public synchronized void save(ParallaxRegion region)
	{
		try
		{
			region.save();
		}

		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public boolean isLoaded(int x, int z)
	{
		return loadedRegions.containsKey(key(x, z));
	}

	public synchronized void save(int x, int z)
	{
		if(isLoaded(x, z))
		{
			save(getR(x, z));
		}
	}

	public synchronized void unload(int x, int z)
	{
		long key = key(x, z);

		if(isLoaded(x, z))
		{
			if(save.contains(key))
			{
				save(x, z);
				save.remove(key);
			}

			loadedRegions.remove(key).unload();
		}
	}

	public synchronized ParallaxRegion load(int x, int z)
	{
		if(isLoaded(x, z))
		{
			return loadedRegions.get(key(x, z));
		}

		ParallaxRegion v = new ParallaxRegion(height, folder, x, z);
		loadedRegions.put(key(x, z), v);

		if(cleanup.flip())
		{
			cleanup();
		}

		return v;
	}

	public ParallaxRegion getR(int x, int z)
	{
		long key = key(x, z);

		ParallaxRegion region = loadedRegions.get(key);

		if(region == null)
		{
			region = load(x, z);
		}

		return region;
	}

	public ParallaxRegion getRW(int x, int z)
	{
		save.addIfMissing(key(x, z));
		return getR(x, z);
	}

	private long key(int x, int z)
	{
		return (((long) x) << 32) | (((long) z) & 0xffffffffL);
	}

	@Override
	public Hunk<BlockData> getBlocksR(int x, int z)
	{
		return getR(x >> 5, z >> 5).getBlockSlice().getR(x & 31, z & 31);
	}

	@Override
	public synchronized Hunk<BlockData> getBlocksRW(int x, int z)
	{
		return getRW(x >> 5, z >> 5).getBlockSlice().getRW(x & 31, z & 31);
	}

	@Override
	public Hunk<String> getObjectsR(int x, int z)
	{
		return getR(x >> 5, z >> 5).getObjectSlice().getR(x & 31, z & 31);
	}

	@Override
	public synchronized Hunk<String> getObjectsRW(int x, int z)
	{
		return getRW(x >> 5, z >> 5).getObjectSlice().getRW(x & 31, z & 31);
	}

	@Override
	public Hunk<Boolean> getUpdatesR(int x, int z)
	{
		return getR(x >> 5, z >> 5).getUpdateSlice().getR(x & 31, z & 31);
	}

	@Override
	public synchronized Hunk<Boolean> getUpdatesRW(int x, int z)
	{
		return getRW(x >> 5, z >> 5).getUpdateSlice().getRW(x & 31, z & 31);
	}

	@Override
	public ParallaxChunkMeta getMetaR(int x, int z)
	{
		return getR(x >> 5, z >> 5).getMetaR(x & 31, z & 31);
	}

	@Override
	public ParallaxChunkMeta getMetaRW(int x, int z)
	{
		return getRW(x >> 5, z >> 5).getMetaRW(x & 31, z & 31);
	}

	public void cleanup()
	{
		cleanup(10000, 5000);
	}

	@Override
	public void cleanup(long r, long c) {
		J.a(() -> {
			synchronized (loadedRegions)
			{
				for(ParallaxRegion i : loadedRegions.v())
				{
					if(i.hasBeenIdleLongerThan(r))
					{
						unload(i.getX(), i.getZ());
					}

					else
					{
						i.cleanup(c);
					}
				}
			}
		});
	}

	@Override
	public void saveAll() {
		J.a(this::saveAllNOW);
	}

	@Override
	public synchronized void saveAllNOW() {
		synchronized (loadedRegions)
		{
			for(ParallaxRegion i : loadedRegions.v())
			{
				synchronized (save)
				{
					if(save.contains(key(i.getX(), i.getZ())))
					{
						save(i.getX(), i.getZ());
					}
				}
			}
		}
	}
}