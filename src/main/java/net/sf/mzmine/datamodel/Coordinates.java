package net.sf.mzmine.datamodel;

public abstract class Coordinates {
	public abstract int getX();
	public abstract int getY();
	public abstract void setX(int x);
	public abstract void setY(int y);
	public abstract int getZ();
	public abstract void setZ(int z);
	
	@Override
	public String toString() {
		return getX()+";"+getY()+";"+getZ()+";";
	}
}
