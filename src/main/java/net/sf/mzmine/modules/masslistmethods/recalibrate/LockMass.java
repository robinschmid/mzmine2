package net.sf.mzmine.modules.masslistmethods.recalibrate;

public class LockMass {

	private double mz = 0;
	private String name = "";
	private String sumformula;
	
	
	public LockMass(double mz, String name) {
		super();
		this.mz = mz;
		this.name = name;
	}


	public double getMz() {
		return mz;
	}
	public String getName() {
		return name;
	}
	public String getSumformula() {
		return sumformula;
	}
	public void setMz(double mz) {
		this.mz = mz;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setSumformula(String sumformula) {
		this.sumformula = sumformula;
	}
	
	@Override
	public String toString() {
		return name==null || name.length()==0? "mz="+mz : name+" (mz="+mz+")";
	}
}
