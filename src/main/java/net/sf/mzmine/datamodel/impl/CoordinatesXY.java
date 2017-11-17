package net.sf.mzmine.datamodel.impl;

import net.sf.mzmine.datamodel.Coordinates;

public class CoordinatesXY extends Coordinates {

	private int x, y;

	public CoordinatesXY(int x, int y) {
		this.x = x; 
		this.y = y;
	}
	
	@Override
	public int getX() {
		return x;
	}
	@Override
	public int getY() {
		return y;
	}
	@Override
	public void setX(int x) {
		this.x = x;
	}
	@Override
	public void setY(int y) {
		this.y = y;
	}
	// implementation for xyz coordinates
	// layer 0
	@Override
	public int getZ() {
		return 0;
	}
	@Override
	public void setZ(int z) {
	}
}
