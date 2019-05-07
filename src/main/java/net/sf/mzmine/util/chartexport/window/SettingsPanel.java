package net.sf.mzmine.util.chartexport.window;
 
import net.sf.mzmine.util.chartexport.Settings;

public interface SettingsPanel {

	/*
	 * Save all settings to SettingsObject
	 */
	public void setAllSettings();
	/*
	 * Apply all settings to all panels
	 */
	public void setAllSettingsOnPanel();
	
	/*
	 * Returns the used settings object
	 */
	public Settings getSettings();
}
