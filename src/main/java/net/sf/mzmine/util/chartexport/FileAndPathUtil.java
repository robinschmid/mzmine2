package net.sf.mzmine.util.chartexport;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

public class FileAndPathUtil { 

	/**
	 * Returns the real file path as path/filename.fileformat
	 * @param file
	 * @param name
	 * @param format a format starting with a dot for example: .pdf ; or without a dot: pdf
	 * @return
	 */
	public static File getRealFilePath(File path, String name, String format) { 
		return new File(getFileAsFolder(path), getRealFileName(name, format));
	} 
	/**
	 * Returns the real file path as path/filename.fileformat
	 * @param file
	 * @param name
	 * @param format a format starting with a dot for example: .pdf ; or without a dot: pdf
	 * @return
	 * @throws Exception if there is no filname (selected path = folder)
	 */
	public static File getRealFilePath(File filepath, String format) throws Exception {  
		if(!isOnlyAFolder(filepath)) {
			return new File(getFileAsFolder(filepath), getRealFileName(getFileNameFromPath(filepath), format));
		}
		else {
			throw new Exception("No filename. selected path = folder");
		}
	} 
	/**
	 * Returns the real file name as filename.fileformat
	 * @param name
	 * @param format a format starting with a dot for example .pdf
	 * @return
	 */
	public static String getRealFileName(String name, String format) {  
		String result = eraseFormat(name);
		result = addFormat(result, format);
		return result;
	} 
	/**
	 * Returns the real file name as filename.fileformat
	 * @param name
	 * @param format a format starting with a dot for example .pdf
	 * @return
	 */
	public static String getRealFileName(File name, String format) {
		return getRealFileName(name.getAbsolutePath(), format);
	}
	
	/**
	 * erases the format. "image.png" will be returned as "image"
	 * this method is used by getRealFilePath and getRealFileName
	 * @param name
	 * @return
	 */
	public static String eraseFormat(String name) { 
		int lastDot = name.lastIndexOf(".");
		if(lastDot!=-1) 
			return name.substring(0, lastDot); 
		else return name;
	}
	
	/**
	 * Adds the format. "image" will be returned as "image.format"
	 * Maybe use erase format first.
	 * this method is used by getRealFilePath and getRealFileName
	 * @param name
	 * @param format
	 * @return
	 */
	public static String addFormat(String name, String format) { 
		if(format.startsWith(".")){
			return name+format;
		}
		else return name+"."+format;
	}
	
	/**
	 * Returns the file format without a dot (f.e. "pdf") or "" if there is no format
	 * @param file
	 * @return
	 */
	public static String getFormat(File file) { 
		return getFormat(file.getAbsolutePath());
	}   
	/**
	 * Returns the file format without a dot (f.e. "pdf") or "" if there is no format
	 * @param file
	 * @return
	 */
	public static String getFormat(String file) { 
		if(!isOnlyAFolder(file)) {
			return file.substring(file.lastIndexOf(".")+1);
		}
		else return "";
	}  
	
	/**
	 * Returns the file if it is already a folder. Or the parent folder if file is a data file
	 * @param file
	 * @return
	 */
	public static File getFileAsFolder(File file) { 
		if(!isOnlyAFolder(file)) {
			return file.getParentFile();
		}
		else return file;
	} 
	
	/**
	 * Returns the file name from a given file. If file is a folder it returns an empty String
	 * @param file
	 * @return
	 */
	public static String getFileNameFromPath(File file) { 
		if(!isOnlyAFolder(file)) {
			return file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("\\")+1);
		}
		else return "";
	} 
	/**
	 * Returns the file name from a given file. If file is a folder it returns an empty String
	 * @param file
	 * @return
	 */
	public static String getFileNameFromPath(String file) { 
		return getFileNameFromPath(new File(file));
	} 
	
	/**
	 * Checks if given File is a folder or a data file
	 */
	public static boolean isOnlyAFolder(File file) { 
		return isOnlyAFolder(file.getAbsolutePath());
	}
	/**
	 * Checks if given File is a folder or a data file
	 */
	public static boolean isOnlyAFolder(String file) { 
		String realPath = file;
		int lastDot = realPath.lastIndexOf(".");
		int lastPath = realPath.lastIndexOf("/");
		
		if(lastDot!=-1 && lastDot>lastPath) {
			return false; // file
		}
		else return true; // folder 
	}

    /**
     * creates a new directory
     * @param theDir
     * @return false only if directory was not created
     */
	public static boolean createDirectory(File theDir) {
    	// if the directory does not exist, create it
    	  if (!theDir.exists()) { 
    		boolean result = false; 
    	    try{
    	        theDir.mkdirs();
    	        result = true;
    	     } catch(SecurityException se){
    	        //handle it
    	     }        
    	     if(result) {    
    	       System.out.println("DIR created");  
    	     } 
    	     return result;
    	  }
    	  else return true;
    }
	
	
	/**
	 * sort an array of files
	 * these files must have an number at the end
	 * @param files
	 * @return
	 */
    public static File[] sortFilesByNumber(File[] files) {
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) { 
				try {
					int n1 = extractNumber(o1.getName());
	                int n2 = extractNumber(o2.getName());
	                return n1 - n2;
				} catch (Exception e) {
					System.err.println("NO NORMAL NUMBER FILE FORMAT - SORT LEXICO");
					return o1.compareTo(o2);
				}
            }

            private int extractNumber(String name) throws Exception {
                int i = 0;
                try { 
                    int e = name.lastIndexOf('.');
                    e = e==-1? name.length() : e;
                    int f = e-1;
                    for(; f>0; f--) {
                    	if(!isNumber(name.charAt(f))){
                    		f++;
                    		break;
                    	}
                    }
                    if(f<0) f=0;
                    String number = name.substring(f, e);
                    i = Integer.parseInt(number);
                } catch(Exception e) {
                    i = 0; // if filename does not match the format
                    throw e;       // then default to 0
                }
                return i;
            }
        }); 
        return files;
    }
    
    private static boolean isNumber(char c) {
    	return (c >= '0' && c <= '9');
    }
    
    /**
     * only all directories in the dir f will be returned
     * @param f
     * @return
     */
    public static File[] getSubDirectories(File f) {
    	return f.listFiles(new FilenameFilter() {
    		  @Override
    		  public boolean accept(File current, String name) {
    		    return new File(current, name).isDirectory();
    		  }
    		}); 
    }
    
    /**
     * returns the Path of Jar
     * @return
     */
    public static File getPathOfJar() {
    	/*
    	File f = new File(System.getProperty("java.class.path"));
    	File dir = f.getAbsoluteFile().getParentFile(); 
    	return dir; 
    	 */
    	try {
    	File jar = new File(FileAndPathUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
    	return jar.getParentFile();
    	}catch(Exception ex) {
    		return new File("");
    	}
    }
}
