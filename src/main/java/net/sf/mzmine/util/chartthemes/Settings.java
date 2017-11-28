package net.sf.mzmine.util.chartthemes;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jfree.ui.RectangleInsets;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sf.mzmine.util.chartexport.BinaryWriterReader;
import net.sf.mzmine.util.chartexport.FileAndPathUtil;
import net.sf.mzmine.util.chartexport.FileTypeFilter;

public abstract class Settings implements Serializable {  
	// do not change the version!
	private static final long serialVersionUID = 1L;

	// the super class
	protected static final String XMLATT_CLASS = "realClass";
	//
	//protected final String parameterElement = "parameter";
	//protected final String nameAttribute = "name";
	//
	protected final String description;
	protected final String path;
	protected final String fileEnding;


	public Settings(String description, String path, String fileEnding) {
		super();
		this.path = path;
		this.fileEnding = fileEnding; 
		this.description = description;
	}

	/**
	 * the super class to find settings in hashmaps
	 * @return
	 */
	public Class getSuperClass() {
		return this.getClass();
	}

	/**
	 * reset all parameters
	 */
	public abstract void resetAll();

	
	//##################################################################
	// xml write and read

	public File saveSettingsToFile(Component parentFrame)  throws Exception { 
		// Open new FC
		// create Path 
		File path = new File(FileAndPathUtil.getPathOfJar(), this.getPathSettingsFile());
		FileAndPathUtil.createDirectory(path);
		JFileChooser fc = new JFileChooser(path); 
		FileTypeFilter ffilter = new FileTypeFilter(this.getFileEnding(), "Save settings to");
		fc.addChoosableFileFilter(ffilter);  
		fc.setFileFilter(ffilter);
		// getting the file
		if (fc.showSaveDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();  
			// extention anbringen
			file = ffilter.addExtensionToFileName(file);
			//
			this.saveToXML(file);
			return file;
		}
		else {
			return null;
		}
	}
	
// was in settingsholder before
//	public Settings loadSettingsFromFile(File file, Settings cs) {
//		// Welches wurde geladen? 
//		if(cs instanceof SettingsHolder) {
//			// alle laden und setzen
//			SettingsHolder sett = (SettingsHolder)(cs.loadFromFile(settingsWriter, file));
//			// Alle settings aus geladenen holder kopieren
//			return this;
//		} 
//		else { 
//			try {
//				cs.loadFromXML(file);
//			} catch (IOException e) {
//				e.printStackTrace();
//				ImageEditorWindow.log("Cannot load settings", LOG.ERROR);
//			}
//			return cs;
//		}
//	}
	
	
	/**
	 * opens a JFileChooser and saves the setting sto the selected file
	 * @param parent
	 * @return
	 * @throws IOException
	 */
	public File saveToXML(Component parent) throws IOException {
		// Open new FC
		// create Path 
		File path = new File(FileAndPathUtil.getPathOfJar(), this.getPathSettingsFile());
		FileAndPathUtil.createDirectory(path);
		JFileChooser fc = new JFileChooser(path); 
		FileTypeFilter ffilter = new FileTypeFilter(this.getFileEnding(), "Save settings to");
		fc.addChoosableFileFilter(ffilter);  
		fc.setFileFilter(ffilter);
		// getting the file
		if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();  
			// extention anbringen
			file = ffilter.addExtensionToFileName(file);
			//
			this.saveToXML(file);
			return file;
		}
		else {
			return null;
		}
	}

	/**
	 * saves settings to a file
	 * @param file
	 * @throws IOException
	 */
	public void saveToXML(File file) throws IOException {
		FileAndPathUtil.createDirectory(file.getParentFile());
		saveToXML(new FileOutputStream(file));
	}

	/**
	 * saves settings to a file
	 * @param file
	 * @throws IOException
	 */
	public void saveToXML(OutputStream fos) throws IOException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			Document configuration = dBuilder.newDocument();
			Element configRoot = configuration.createElement("settings");
			configuration.appendChild(configRoot);

			// creates a new element for this settings class and appends Values
			appendSettingsToXML(configRoot, configuration);

			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer transformer = transfac.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");

			StreamResult result = new StreamResult(fos);
			DOMSource source = new DOMSource(configuration);
			transformer.transform(source, result); 
		} catch (Exception e) {
			throw new IOException(e);
		}
	}


	/**
	 * saves settings and calls abstract appendSettingsValuesToXML
	 * creates a new parent element for this settings class
	 * @param elParent
	 * @param doc
	 */
	public void appendSettingsToXML(Element elParent, Document doc) { 
		Element elSett = doc.createElement(this.getSuperClass().getName());
		// save real class as attribute
		if(!this.getClass().equals(this.getSuperClass()))
			elSett.setAttribute(XMLATT_CLASS, this.getClass().getName());
		elParent.appendChild(elSett);
		appendSettingsValuesToXML(elSett, doc);
	}

	/**
	 * append values of settings to xml
	 * gets called by appendSettingsToXML 
	 * @param elParent
	 * @param doc
	 */
	public abstract void appendSettingsValuesToXML(Element elParent, Document doc);

	//'''''''''''''''''''''''''''''''''
	// save specific values
	public static Element toXML(Element elParent, Document doc, String name, Object o) {
		if(o!=null) {
			//Element paramElement = doc.createElement(parameterElement);
			//paramElement.setAttribute(nameAttribute, name);
			Element paramElement = doc.createElement(name);
			elParent.appendChild(paramElement); 

			if(Color.class.isInstance(o) || Paint.class.isInstance(o)) {
				Color c = (Color) o;
				paramElement.setTextContent(String.valueOf(c.getRGB()));
				paramElement.setAttribute("alpha", String.valueOf(c.getAlpha()));
			}
			else if(File.class.isInstance(o)) {
				paramElement.setTextContent(((File)o).getAbsolutePath());
			} 
			else if(Point2D.class.isInstance(o)) {
				Point2D p = (Point2D)o;
				String s = String.valueOf(p.getX())+";"+String.valueOf(p.getY());
				paramElement.setTextContent(s);
			}
			else if(Font.class.isInstance(o)) {
				Font f = (Font)o;
				paramElement.setTextContent(f.getName());
				paramElement.setAttribute("style", ""+f.getStyle());
				paramElement.setAttribute("size", ""+f.getSize());
			}
			else if(RectangleInsets.class.isInstance(o)) {
				RectangleInsets r = (RectangleInsets)o;
				paramElement.setAttribute("top", ""+r.getTop());
				paramElement.setAttribute("left", ""+r.getLeft());
				paramElement.setAttribute("bottom", ""+r.getBottom());
				paramElement.setAttribute("right", ""+r.getRight());
			}
			else paramElement.setTextContent(String.valueOf(o));

			// return to give an option for more attributes
			return paramElement;
		}
		return null;
	}
	/**
	 * with attributes
	 * @param elParent
	 * @param doc
	 * @param name
	 * @param o
	 * @param attributes
	 * @param attValues
	 * @return
	 */
	public static Element toXML(Element elParent, Document doc, String name, Object o, String att, Object attValue) {
		if(o!=null) {
			//Element paramElement = doc.createElement(parameterElement);
			//paramElement.setAttribute(nameAttribute, name);
			Element paramElement = toXML(elParent, doc, name, o);

			if(paramElement==null) {
				paramElement = doc.createElement(name);
				elParent.appendChild(paramElement); 
				if(o!=null)
					paramElement.setTextContent(String.valueOf(o));
			}

			paramElement.setAttribute(att, String.valueOf(attValue));
			return paramElement;
		}
		return null;
	}
	/**
	 * with attributes
	 * @param elParent
	 * @param doc
	 * @param name
	 * @param o
	 * @param attributes
	 * @param attValues
	 * @return
	 */
	public static Element toXML(Element elParent, Document doc, String name, Object o, String[] attributes, Object[] attValues) {
		if(o!=null) {
			//Element paramElement = doc.createElement(parameterElement);
			//paramElement.setAttribute(nameAttribute, name);
			Element paramElement = toXML(elParent, doc, name, o);

			if(paramElement==null) {
				paramElement = doc.createElement(name);
				elParent.appendChild(paramElement); 
				if(o!=null)
					paramElement.setTextContent(String.valueOf(o));
			}

			for(int i=0; i<attributes.length; i++) {
				paramElement.setAttribute(attributes[i], String.valueOf(attValues[i]));
			}
			return paramElement;
		}
		return null;
	}
	public static void toXMLArray(Element elParent, Document doc, String name, Object[][] o) {
		if(o!=null) {
			//Element paramElement = doc.createElement(parameterElement);
			//paramElement.setAttribute(nameAttribute, name);
			Element paramElement = doc.createElement(name);
			elParent.appendChild(paramElement); 
			StringBuilder res = new StringBuilder();
			for(int i=0; i<o.length; i++) {
				for(int x=0; x<o[i].length; x++) {
					res.append(String.valueOf(o[i][x]));
					if(x<o[i].length-1)
						res.append(";");
				}
				if(i<o.length-1)
					res.append("\n");
			}

			paramElement.setTextContent(res.toString());
		}
	}

	public static void toXMLArray(Element elParent, Document doc, String name, Object[] o) {
		if(o!=null) {
			//Element paramElement = doc.createElement(parameterElement);
			//paramElement.setAttribute(nameAttribute, name);
			Element paramElement = doc.createElement(name);
			elParent.appendChild(paramElement); 
			StringBuilder res = new StringBuilder();
			for(int i=0; i<o.length; i++) {
				res.append(String.valueOf(o[i]));
				if(i<o.length-1)
					res.append(";");
			}

			paramElement.setTextContent(res.toString());
		}
	}
	public static void toXMLArray(Element elParent, Document doc, String name, float[] o) {
		if(o!=null) {
			//Element paramElement = doc.createElement(parameterElement);
			//paramElement.setAttribute(nameAttribute, name);
			Element paramElement = doc.createElement(name);
			elParent.appendChild(paramElement); 
			StringBuilder res = new StringBuilder();
			for(int i=0; i<o.length; i++) {
				res.append(String.valueOf(o[i]));
				if(i<o.length-1)
					res.append(";");
			}

			paramElement.setTextContent(res.toString());
		}
	}


	//'''''''''''''''''''''''''''''''''
	// load
	public Settings loadSettingsFromFile(Component parentFrame)  throws Exception {
		// TODO Auto-generated method stub 
		// Open new FC
		File path = new File(FileAndPathUtil.getPathOfJar(), this.getPathSettingsFile());
		FileAndPathUtil.createDirectory(path);
		JFileChooser fc = new JFileChooser(path); 
		FileFilter ffilter = new FileTypeFilter(this.getFileEnding(), "Load settings from");
		fc.addChoosableFileFilter(ffilter);  
		fc.setFileFilter(ffilter);
		// getting the file
		if (fc.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();   
			loadFromXML(file);
			return this;
		}
		return null;
	} 
	/**
	 * load xml settings from a file
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public void loadFromXML(File file) throws IOException { 
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document configuration = dBuilder.parse(file);

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();

			// load settings
			loadSettingsFromXML(configuration, xpath, "//settings");
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * load xml settings from a file
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public void loadFromXML(InputStream is) throws IOException { 
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document configuration = dBuilder.parse(is);

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();

			// load settings
			loadSettingsFromXML(configuration, xpath, "//settings");
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * loads settings from xml. calls loadValuesFromXML
	 * @param doc
	 * @param xpath
	 * @param path the String path to the parent (need to add /child)
	 * @throws XPathExpressionException
	 */
	public void loadSettingsFromXML(Document doc, XPath xpath, String parentpath) throws XPathExpressionException {
		// root= settings 
		XPathExpression expr = xpath.compile(parentpath+"/"+this.getSuperClass().getName());
		NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		if (nodes.getLength() == 1) {
			Element el = (Element) nodes.item(0);
			loadValuesFromXML(el, doc);
		}
	}
	/**
	 * load values
	 * @param el
	 * @param doc
	 */
	public abstract void loadValuesFromXML(Element el, Document doc);



	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static Double doubleFromXML(final Element el) {
		final String numString = el.getTextContent();
		if (numString.length() > 0) {
			return Double.parseDouble(numString);
		}
		return null;
	}

	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static Point2D point2DDoubleFromXML(final Element el) {
		final String numString = el.getTextContent();
		if (numString.length() > 0) {
			String[] split = numString.split(";");
			if(split.length==2) {
				return new Point2D.Double(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
			}
		}
		return null;
	}
	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static File fileFromXML(final Element el) {
		final String numString = el.getTextContent();
		if (numString.length() > 0) {
			return new File(numString);
		}
		return null;
	}
	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static Integer intFromXML(final Element el) {
		final String numString = el.getTextContent();
		if (numString.length() > 0) {
			return Integer.parseInt(numString);
		}
		return null;
	}
	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static Float floatFromXML(final Element el) {
		final String numString = el.getTextContent();
		if (numString.length() > 0) {
			return Float.parseFloat(numString);
		}
		return null;
	}
	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static float[] floatArrayFromXML(final Element el) {
		final String numString = el.getTextContent();
		if (numString.length() > 0) {
			String[] split = numString.split(";");
			float[] f = new float[split.length];
			for(int i=0; i<split.length; i++)
				f[i] = Float.valueOf(split[i]);
			return f;
		}
		return null;
	}
	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static Font fontFromXML(final Element el) {
		final String family = el.getTextContent();
		if (family.length() > 0) {
			int style = Integer.valueOf(el.getAttribute("style"));
			int size = Integer.valueOf(el.getAttribute("size"));
			return new Font(family, style, size);
		}
		return null;
	}

	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static Color colorFromXML(final Element el) {
		final String numString = el.getTextContent();
		if (numString.length() > 0) {
			int rgb =  Integer.parseInt(numString);
			int alpha = Integer.parseInt(el.getAttribute("alpha"));
			Color c = new Color(rgb);
			return new Color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
		}
		return null;
	}

	/**
	 * rectangle insets from xml (top, left, bottom, right)
	 * @param el
	 * @return null if no value was found
	 */
	public static RectangleInsets insetsFromXML(final Element el) {
		double t = Double.valueOf(el.getAttribute("top"));
		double l = Double.valueOf(el.getAttribute("left"));
		double b = Double.valueOf(el.getAttribute("bottom"));
		double r = Double.valueOf(el.getAttribute("right"));
		return new RectangleInsets(t, l, b, r);
	}
	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static Boolean booleanFromXML(final Element el) {
		final String numString = el.getTextContent();
		if (numString.length() > 0) {
			return Boolean.parseBoolean(numString);
		}
		return null;
	}


	/**
	 * 
	 * @param el
	 * @return null if no value was found
	 */
	public static Boolean[][] mapFromXML(final Element el) {
		final String numString = el.getTextContent();
		if (numString.length() > 0) {
			String[] lines = numString.split("\n");
			Boolean[][] map = new Boolean[lines.length][];
			for(int i=0; i<lines.length; i++) {
				String l = lines[i];
				String[] split = l.split(";");
				map[i] = new Boolean[split.length];
				for(int x=0; x<split.length; x++) {
					map[i][x] = Boolean.parseBoolean(split[x]);
				}
			}
			return map;
		}
		return null;
	}


	/**
	 * hashed map is the super class of the class in the xml node
	 * node name
	 * @param nextElement
	 * @return
	 */
	public static Class getHashedClassFromXML(Element nextElement) {
		try {
			// get super class name (hashed class name which was inserted to list) 
			Class hashedClass = Class.forName(nextElement.getNodeName());
			return hashedClass;
		} catch (Exception e) {
			System.err.println("No class for xml object "+nextElement.getNodeName());
		}
		return null;
	}
	/**
	 * real name of this class which was set as an attribute
	 * @return
	 */
	public static Class getRealClassFromXML(Element nextElement) {
		try {
			// get real class name (or super (hashed) class name which was inserted to list) 
			String realClassName = nextElement.getAttribute(XMLATT_CLASS);
			if(realClassName.length()==0)
				return getHashedClassFromXML(nextElement);
			else {
				Class hashedClass = Class.forName(realClassName);
				return hashedClass;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * checks if the node is a settingsnode of this class
	 * @param nextElement
	 * @param c
	 * @return
	 */
	public boolean isSettingsNode(Element nextElement, Class c) {
		Class hashedClass = getHashedClassFromXML(nextElement);
		return (hashedClass!=null && hashedClass.equals(c));
	}
	//'''''''''''''''''''''''''''''
	// load specific values

	//'''''''''''''''''''''''''''''''''
	// create 
	/**
	 * creates a new instance of a settings class
	 * @param className
	 * @return
	 */
	public static Settings createSettings(String className) {
		try {
			Class<?> clazz = Class.forName(className);
			return createSettings(clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * creates a new instance of a settings class
	 * @param className
	 * @return
	 */
	public static Settings createSettings(Class clazz) {
		try {
			Constructor<?> ctor = clazz.getConstructor();
			Object object = ctor.newInstance();
			return (Settings) object;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//##################################################################
	// binary write and read
	/**
	 * binary read
	 * @param writer
	 * @param file
	 */
	protected void saveToFile(BinaryWriterReader writer, File file) { 
		writer.save2file(this, file);
		writer.closeOut();
	} 

	/**
	 * binary write
	 * @param writer
	 * @param file
	 * @return
	 */
	protected Settings loadFromFile(BinaryWriterReader writer, File file) { 
		Settings set = (Settings) writer.readFromFile(file);
		writer.closeIn();
		return set;
	}


	public String getPathSettingsFile() {
		return path;
	} 

	public String getFileEnding() {
		return fileEnding;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * checks if a and b are not equal 
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean changed(Object a, Object b) {
		return ((a!=null ^ b!=null) || (a!=null && !a.equals(b)));
	}
	/**
	 * returns a copy by binary copy
	 * @return
	 * @throws Exception 
	 */
	public Settings copy() throws Exception { 
		return BinaryWriterReader.deepCopy(this);
	}

}
