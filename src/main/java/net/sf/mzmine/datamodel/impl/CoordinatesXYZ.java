package net.sf.mzmine.datamodel.impl;

public class CoordinatesXYZ extends CoordinatesXY {

	private int z;
	public CoordinatesXYZ(int x, int y, int z) {
		super(x, y);
		this.z = z;
	}
	@Override
	public int getZ() {
		return z;
	}
	@Override
	public void setZ(int z) {
		this.z = z;
	}
}
