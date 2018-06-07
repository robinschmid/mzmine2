package net.sf.mzmine.datamodel;

/**
 * zero based system!
 * the first pixel is at 0 0 0
 * @author r_schm33
 *
 */
public abstract class Coordinates {
	/**
	 * zero based. first pixel at 0
	 * @return
	 */
	public abstract int getX();
	/**
	 * zero based. first pixel at 0
	 * @return
	 */
	public abstract int getY();
	public abstract void setX(int x);
	public abstract void setY(int y);
	/**
	 * zero based. first pixel at 0
	 * @return
	 */
	public abstract int getZ();
	public abstract void setZ(int z);
	
	@Override
	public String toString() {
		return getX()+";"+getY()+";"+getZ()+";";
	}
}
