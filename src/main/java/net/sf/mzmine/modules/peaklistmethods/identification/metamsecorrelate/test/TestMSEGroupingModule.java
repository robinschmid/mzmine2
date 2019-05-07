package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.test;

import java.io.File;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.projectmethods.projectload.ProjectLoaderParameters;
import net.sf.mzmine.modules.projectmethods.projectload.ProjectOpeningTask;

public class TestMSEGroupingModule {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MZmineCore core = new MZmineCore();
		core.main(args);
		
		// sample project
		File file = new File("C:\\DATA\\Project\\trigger_test.mzmine");
		// open project
		ProjectLoaderParameters par = new ProjectLoaderParameters(); 
		par.getParameter(ProjectLoaderParameters.projectFile).setValue(file);
		ProjectOpeningTask newTask = new ProjectOpeningTask(par); 
		core.getTaskController().addTask(newTask); 
	}

}
