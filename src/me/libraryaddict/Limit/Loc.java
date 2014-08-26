package me.libraryaddict.Limit;

import org.bukkit.block.Block;

public class Loc {
	int	x, y, z;

	public Loc(Block block) {
		this(block.getX(), block.getY(), block.getZ());
	}

	public Loc(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Loc) {
			Loc loc = (Loc) obj;
			return loc.x == x && loc.y == y && loc.z == z;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}
}