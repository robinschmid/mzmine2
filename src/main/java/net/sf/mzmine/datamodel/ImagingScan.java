package net.sf.mzmine.datamodel;

public interface ImagingScan {
	/**
	 * 
	 * @return the xyz coordinates. null if no coordinates were specified
	 */
	public Coordinates getCoordinates();

	public void setCoordinates(Coordinates coordinates);
}
