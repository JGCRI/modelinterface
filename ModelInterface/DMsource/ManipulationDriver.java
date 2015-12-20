/*
* LEGAL NOTICE
* This computer software was prepared by Battelle Memorial Institute,
* hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830
* with the Department of Energy (DOE). NEITHER THE GOVERNMENT NOR THE
* CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
* 
* Copyright 2012 Battelle Memorial Institute.  All Rights Reserved.
* Distributed as open-source under the terms of the Educational Community 
* License version 2.0 (ECL 2.0). http://www.opensource.org/licenses/ecl2.php
* 
* EXPORT CONTROL
* User agrees that the Software will not be shipped, transferred or
* exported into any country or used in any manner prohibited by the
* United States Export Administration Act or any other applicable
* export laws, restrictions or regulations (collectively the "Export Laws").
* Export of the Software may require some form of license or other
* authority from the U.S. Government, and failure to obtain such
* export control license may result in criminal liability under
* U.S. laws. In addition, if the Software is identified as export controlled
* items under the Export Laws, User represents and warrants that User
* is not a citizen, or otherwise located within, an embargoed nation
* (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
*     and that User is not otherwise prohibited
* under the Export Laws from receiving the Software.
* 
*/
/*!
 * \file ManipulationDriver.java
 * \ingroup DataManipulation
 * \brief Driver class for this program, runs based on supplied XML files.
 *
 *  Gets a list of commands, a list of data, and a definition of region hierarchy.
 * Based on these XML files creates a list of subRegions, builds a list of
 * superRegions from those. Then runs through the users commands and outputs the
 * results to the defined medium.
 *
 * \author Vincent Nibali
 * \date $Date: 2009-12-08 16:48:48 -0500 (Tue, 08 Dec 2009) $
 * \version $Revision: 3638 $
 */

package ModelInterface.DMsource;

/*
 * TreeMap(region name)->Region->TreeMap(field)->TreeMap(time)->Matrix(values)
 */

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;

import org.jdom.*;
import org.jdom.filter.ContentFilter;
import org.jdom.input.*;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.NetcdfFile;
import ucar.ma2.*;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;

import ModelInterface.PPsource.DataBuilder;


/**
 * Driver class for this program, runs based on supplied XML files. Gets a list of commands,
 * a list of data, and a definition of region hierarchy.
 * Based on these XML files creates a list of subRegions, builds a list of
 * superRegions from those. Then runs through the users commands and outputs the
 * results to the defined medium.
 * 
 * @author Vincent Nibali
 * @version 1.0
 */
public class ManipulationDriver
{
  public TreeMap regionList; //master list of all regions, regular and super
  public TreeMap variableList; //master list of all variables, reference or data
  public TreeMap dataAvgAdd; //a mapping of data names to wether they are averaged or added on aggregation
  public TreeMap dataRef; //a listing of the references for each data type for which one was supplied
  public TreeMap dataUnits; //a listing of the units which each variables values represent
  public HashSet writeFiles; //a list of files which have been written to so far by this run
  private String dSource; //the fielname sub regions and data are read from
  private String rSource; //the filename the region hierarchy is defined in
  private String cSource; //the filename which the users commands come from
  private Document dDocument; //document of data to create regions
  private Document rDocument; //document of region hierarchy
  private Document cDocument; //document of user commands
  private double resolution; //the resolution which data is stored at
  private int numAtomicRegions; //the number of lowest level regions (i dont think i even use this anymore)
  Logger log = Logger.getLogger("DataManipulation"); //log class to use for all logging output
  
//*****************************************************************************
//*****************Class Constructors******************************************
//*****************************************************************************
  /**
   * Default Constuctor. Uses 'out.xml' for data input, 'regionDef.xml' for region hierarchy,
   * and 'commands.xml' for the users desired commands.
   */
  public ManipulationDriver()
  {
    log.log(Level.WARNING, "Using ManipulationDriver's default constructor.");
    dSource = "out.xml";
    rSource = "regionDef.xml";
    cSource = "commands.xml";
    log.log(Level.CONFIG, "ManipuationDriver file names: out.xml, regionDef.xml, commands.xml");
    regionList = new TreeMap();
    variableList = new TreeMap();
    dataAvgAdd = new TreeMap();
    dataRef = new TreeMap();
    dataUnits = new TreeMap();
    writeFiles = new HashSet();
    log.log(Level.FINEST, "all data structures have been initialized");
  }
  /**
   * Standard constructor which takes all input files as paramaters.
   * @param d Source for data to be read in.
   * @param r Source for the hierarchy of regions to be read.
   * @param c Source of the users desired run commands.
   */
  public ManipulationDriver(String d, String r, String c)
  {
    log.log(Level.FINEST, "Entering ManipulationDriver standard constuctor.");
    dSource = d;
    rSource = r;
    cSource = c;
    log.log(Level.CONFIG, "ManipuationDriver file names: "+d+", "+r+", "+c);
    regionList = new TreeMap();
    variableList = new TreeMap();
    dataAvgAdd = new TreeMap();
    dataRef = new TreeMap();
    dataUnits = new TreeMap();
    writeFiles = new HashSet();
    log.log(Level.FINEST, "all TreeMap's have been initialized");
  }
  
//*****************************************************************************
//***************Begin Functions Proper****************************************
//*****************************************************************************
  /**
   * Wrapper function which runs all needed functions in the correct order for the user.
   */
  public void runAll()
  {
    /*
    try{
    System.out.println("Begin. waiting...");
    System.in.read();
    System.in.read();
    System.out.println("...going");
    */
    log.log(Level.FINE, "Calling makeStreams");
    makeStreams();
    log.log(Level.FINE, "Building lowest-level regions");
    //buildRegionData();
    log.log(Level.FINE, "Creating the region hierarchy");
    buildRegionHierarchy();
    log.log(Level.FINE, "Parsing user input");
    inputParser();
    /*
    } catch(IOException e){}
    */
  }
  /**
   * Reads in a list of regions from the supplied xml file. Regions are filled with data
   * as a {@link TreeMap} of variables, each with a TreeMap of times, then a matrix of double
   * values. Each region contains a data matrix called 'weight' which is the portion of
   * each cell which lies in that particular region (used for boarder cells).
   *
   */
  public void buildRegionData()
  {
    Element root = dDocument.getRootElement();
    List regionChildren = root.getChildren("region");
    Element currRegion;
    
    resolution = Double.parseDouble(root.getAttributeValue("res"));
	// Normalize resolution to units of one quarter of a degree
	resolution = (double)( (int)Math.round(resolution*60*4) )/(60.0*4.0);
    
    //getting the information for each field, such as reference and units
    currRegion = root.getChild("variableInfo");
    setFieldInfo(currRegion);
    
    numAtomicRegions = regionChildren.size();
    //MAIN XML READ LOOP FOR READING REGION DATA
    for(int i = 0; i < numAtomicRegions; i++)
    {
      addRegion((Element)regionChildren.get(i));
    }
    //END MAIN XML LOOP FOR REGION BUILD
    log.log(Level.FINER, "Done creating lowest-level regions");
  }
  /**
   * Reads definitions of super regions from an XML file and adds them to the region list.
   * Hierarchy of regions is defined in the file by what level the region resides on.
   * Levels are dependant on the dependencies of the region. Level 1 only contains
   * subRegions, level 2 can contain subRegions and level 1, and so on.
   *
   */
  public void buildRegionHierarchy()
  {
    Element root = rDocument.getRootElement();
    int numLevels = Integer.parseInt(root.getAttributeValue("numLevels"));
    List superChildren[] = new List[numLevels];
    for(int i = 0; i < numLevels; i++)
    {
      superChildren[i] = new ArrayList();
    }
    log.log(Level.FINEST, "sorting super regions into level lists");
    List holdChildren = root.getChildren("superRegion");
    for(int i = 0; i < holdChildren.size(); i++)
    { //this is annoying but i think it will make creating region defs easier for the user
      //what im doing is splitting children into lists based on their lvl
      Element hold = (Element)holdChildren.get(i);
      superChildren[(Integer.parseInt(hold.getAttributeValue("level"))-1)].add(hold);
    }
    log.log(Level.FINEST, "begin adding superregions loop");
    //BEGIN MAIN LOOP FOR READING SUPER REGIONS
    int internalRegionIDCount = 0;
    for(int i = 0; i < numLevels; i++)
    {
      for(int j = 0; j < superChildren[i].size(); j++)
      {
        internalRegionIDCount = addSuperRegion((Element)superChildren[i].get(j), internalRegionIDCount);
      }
    }
    //END MAIN LOOP FOR SUPER REGION CREATION
    log.log(Level.FINER, "Done creating super regions");
  }
  /**
   * Parses commands from the user's input and runs the appropriate manipulator
   * functions. Commands include defining variables (as scalars or regions), manipulating
   * the data in those variables, and printing variables to the screen (or some other output).
   *
   */
  public void inputParser()
  {
    List coms;
    Element currCom;
    Element root = cDocument.getRootElement();
    coms = root.getChildren();
    
    for(int i = 0; i < coms.size(); i++)
    {
      currCom = (Element)coms.get(i);
      runCommand(currCom);
    }
    // TODO: figure out a better way to know when to write these out.
    CSVFileManager.getInstance().writeAllCSVFiles();
    log.log(Level.FINER, "All user commands have been parsed");
  }
  
//*****************************************************************************
//******************Input Command Runners**************************************
//*****************************************************************************
  /**
   * Creates a new variable which holds only data. Builds according to the
   * contents of the passed XML node.
   * @param command XML command the variable will be based on.
   */
  private void newDataVariableCommand(Element command)  
  {
    log.log(Level.FINER, "begin function");
    Element currInfo;
    List dataChildren;
    String newName = command.getAttributeValue("name");
    Variable toAdd;
    int hY, hX;
    
    currInfo = command.getChild("dimension");
    int dim = Integer.parseInt(currInfo.getAttributeValue("value"));
    if(dim == 1)
    {
      currInfo = command.getChild("size");
      hX = Integer.parseInt(currInfo.getAttributeValue("x"));
      double[] toAddData = new double[hX];
      for(int i = 0; i < hX; i++)
      {
        toAddData[i] = Double.NaN;
      }
      
      dataChildren = command.getChildren("data");
      for(int i = 0; i < dataChildren.size(); i++)
      {
        currInfo = (Element)dataChildren.get(i);
        hX = Integer.parseInt(currInfo.getAttributeValue("x"));
        toAddData[hX] = Double.parseDouble(currInfo.getAttributeValue("value"));
      }
      
      toAdd = new DataVariable(newName, toAddData);
    } else if(dim == 2)
    {
      currInfo = command.getChild("size");
      hY = Integer.parseInt(currInfo.getAttributeValue("y"));
      hX = Integer.parseInt(currInfo.getAttributeValue("x"));
      double[][] toAddData = new double[hY][hX];
      for(int i = 0; i < hY; i++)
      {
        for(int k = 0; k < hX; k++)
        {
          toAddData[i][k] = Double.NaN;
        }
      }
      
      dataChildren = command.getChildren("data");
      for(int i = 0; i < dataChildren.size(); i++)
      {
        currInfo = (Element)dataChildren.get(i);
        hY = Integer.parseInt(currInfo.getAttributeValue("y"));
        hX = Integer.parseInt(currInfo.getAttributeValue("x"));
        toAddData[hY][hX] = Double.parseDouble(currInfo.getAttributeValue("value"));
      }
      
      toAdd = new DataVariable(newName, toAddData);
    } else //dim == 0, no size element
    {
      currInfo = command.getChild("data");
      double toAddData = Double.parseDouble(currInfo.getAttributeValue("value"));
      toAdd = new DataVariable(newName, toAddData);
    }
    
    log.log(Level.FINEST, "added "+newName+" to variable list");
    variableList.put(newName, toAdd);
  }
  /**
   * Creates a new variable which holds a regions worth of data. Builds
   * according to the contents of the passed XML node.
   * @param command XML command the variable will be based on.
   */
  private void newReferenceVariableCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    ReferenceVariable toAdd;
    Element currInfo;
    String newName = command.getAttributeValue("name");
    Region ref;
    String var, time, reg;
    boolean avg;

    currInfo = command.getChild("region");
    reg = currInfo.getAttributeValue("value");
    currInfo = command.getChild("field");
    var = currInfo.getAttributeValue("value");
    currInfo = command.getChild("time");
    time = currInfo.getAttributeValue("value");
    
    //!remove below line
    //System.out.println(command.toString());
    checkRegionFieldTime(reg, var, time);
    
    ref = (Region)regionList.get(reg);
    if(var.equals("RegionCellSize") && dataAvgAdd.get(var) == null) {
	    avg = false;
    } else {
	    avg = ((Boolean)dataAvgAdd.get(var)).booleanValue();
    }

    // creating the reference variable (fills with data also)
    toAdd = new ReferenceVariable(newName, ref, var, time, avg);

    // setting optional information
    if(dataRef.containsKey(var))
    {
      toAdd.reference = (String)dataRef.get(var);
    }
    if(dataUnits.containsKey(var))
    {
      toAdd.units = (String)dataUnits.get(var);
    }
    currInfo = command.getChild("comment");
    if(currInfo!=null)
    {
      toAdd.comment = currInfo.getAttributeValue("value");
    }

    variableList.put(newName, toAdd);
    log.log(Level.FINEST, "added "+newName+" to variable list");

  }
  /**
   * Creates a new variable which is a region filled with the
   * read in scalar value. Builds
   * according to the contents of the passed XML node.
   * @param command XML command the variable will be based on.
   */
  private void newScalarReferenceVariableCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    ReferenceVariable toAdd;
    Element currInfo;
    String newName = command.getAttributeValue("name");
    Region ref;
    String time, reg;
    double scalarValue;
    // TODO: see if we want to allow false
    boolean avg = true;

    // the user may want to use an attr name other than value which would allow
    // them some flexability to have many scalers in a single xml file
    currInfo = command.getChild("scalar-attribute");
    final String scalarAttrName = currInfo == null ? "value" : currInfo.getAttributeValue("name");

    currInfo = command.getChild("region");
    reg = currInfo.getAttributeValue("value");
    currInfo = command.getChild("time");
    time = currInfo.getAttributeValue("value");
    
    // get the region and make sure it exists
    ref = (Region)regionList.get(reg);
    if(ref == null) {
	    log.log(Level.SEVERE, "Region "+reg+" was not found, variable "+newName+" was not created.");
	    return;
    }

    // creating the reference variable (does not fill data)
    toAdd = new ReferenceVariable(newName, ref, "", time, avg);

    // fill the data with the scalars
    // if there is a scalar element then set that into every region
    // contained in reg 
    currInfo = command.getChild("defaultValue");
    if(currInfo != null) {
	    scalarValue = Double.parseDouble(currInfo.getAttributeValue("value"));
	    ComponentManipulator.setValue(toAdd.data, scalarValue);
    }

    // now go through every sub region and set the it's scalar
    ArrayList<Element> subRegionElements = new ArrayList<Element>(command.getChildren("subRegion")); 

    // the user may also read these from a separate xml file
    currInfo = command.getChild("file");
    if(currInfo != null) {
	    try {
		    SAXBuilder builder = new SAXBuilder();
		    Document rDocument = builder.build(currInfo.getAttributeValue("name"));
		    subRegionElements.addAll(rDocument.getRootElement().getChildren("subRegion"));
	    } catch(Exception e) {
		    e.printStackTrace();
		    log.log(Level.WARNING, "Could not read scalers from file "+e);
	    }
    }

    for(int i = 0; i < subRegionElements.size(); i++) {
	    currInfo = subRegionElements.get(i);
	    String currSubRegion = currInfo.getAttributeValue("name");
	    scalarValue = Double.parseDouble(currInfo.getAttributeValue(scalarAttrName));
	    ref = (Region)regionList.get(currSubRegion);
	    // check to make sure the region exists
	    if(ref == null) {
		    log.log(Level.WARNING, "Region "+currSubRegion+" was not found, scalar "+scalarValue
				    +" was not set.");
	    } else {
		    Wrapper[] data = ref.extractRegion(toAdd);
		    // check to see if the region was acutally a sub region to reg
		    // if so set the value 
		    if(data.length != 0) {
			    ComponentManipulator.setValue(data, scalarValue);
		    } else {
			    log.log(Level.WARNING, "subRegion "+currSubRegion+" was not found in "+reg+", scalar "+scalarValue
					    +" was not set.");
		    }
	    }
    }

    variableList.put(newName, toAdd);
    log.log(Level.FINEST, "added "+newName+" to variable list");

  }
  /**
   * Creates a new variable which holds a group of other variables. Child
   * variables can be of any type. Defined uses are for holding all time
   * occurances of a field in a region, or holding all first level sub- regions
   * of a variable.
   * 
   * @param command
   *          XML command the variable will be filled based on.
   */
  private void newGroupVariableCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    GroupVariable toAdd;
    String newName = command.getAttributeValue("name");
    Element currInfo;
    
    toAdd = new GroupVariable(newName);
    
    currInfo = command.getChild("fill");
    String fillType = currInfo.getAttributeValue("value");
    
    if(fillType.equals("time"))
    { //filled by each time entry for a certain region and field
      toAdd.isRegion = false;
      toAdd.isTime = true;
      
      fillGroupByTime(toAdd, command.getChild("members"));
    } else if(fillType.equals("subregions"))
    { //some sortof subregions will fill group
      toAdd.isRegion = true;
      toAdd.isTime = false;
      
      currInfo = command.getChild("members");
      if(currInfo.getAttributeValue("variable") != null)
      { //given a variable, extract all first level subregions from it
        fillGroupByExtraction(toAdd, currInfo);
      } else
      { //generate all first level subregion variables
        fillGroupByChildren(toAdd, currInfo);
      }
    } else
    { //filled by explicitly listed variables
      toAdd.isRegion = false;
      toAdd.isTime = false;
      
      fillGroupByExplicit(toAdd, command.getChild("members"));
    }
    
    if(toAdd != null)
    {
      currInfo = command.getChild("comment");
      if(currInfo != null)
      {
       toAdd.comment = currInfo.getAttributeValue("value"); 
      }
      variableList.put(newName, toAdd);
      log.log(Level.FINEST, "added "+newName+" to variable list");
    } else
    { //returned a null pointer when we tried to fill because of a problem getting data
      log.log(Level.WARNING, "Command Failed: creating group variable -> "+newName);
    }
  }
  /**
   * Creates a new variable as an aggregation of a list of variables.
   * Can be used only for reference variables as aggregation is undefined
   * for data variables.
   * @param command XML command listing variable to aggregate.
   */
  private void aggregateVariablesCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    ReferenceVariable VDest = null;
    Element currInfo;
    List args;
    ReferenceVariable[] varData;
    int DWcount = 0; //number of data wrappers (sub regions) in all variables being added to this one
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    args = command.getChildren("argument");
    if(args.size() == 1)
    { //we were given a group variable!!!! yayyayay!!!
      currInfo = (Element)args.get(0);
      if(!variableList.containsKey(currInfo.getAttributeValue("name")))
      {
        log.log(Level.WARNING, "Command Failed: group variable argument did not exist.");
        return;
      }
      GroupVariable grp = (GroupVariable)variableList.get(currInfo.getAttributeValue("name"));
      varData = new ReferenceVariable[grp.data.size()];
      int i = 0;
      Map.Entry me;
      Iterator it = grp.data.entrySet().iterator();
      
      while(it.hasNext())
      {
        me = (Map.Entry)it.next();
        
        varData[i] = (ReferenceVariable)me.getValue();
        if(!varData[i].isReference())
        {
          log.log(Level.WARNING, "Command Failed: child variable was not of type Reference.");
          return;
        }
        DWcount += varData[i].data.length;
        i++;
      }
    } else
    { //just a regular old list of variable to aggregate
      varData = new ReferenceVariable[args.size()];
      for(int i = 0; i < args.size(); i++)
      {
        currInfo = (Element)args.get(i);
        if(!variableList.containsKey(currInfo.getAttributeValue("name")))
        {
          log.log(Level.WARNING, "Command Failed: seed variable "+currInfo.getAttributeValue("name")+" did not exist.");
          return;
        }
        varData[i] = (ReferenceVariable)variableList.get(currInfo.getAttributeValue("name"));
        if(!varData[i].isReference())
        {
          log.log(Level.WARNING, "Command Failed: seed variable "+currInfo.getAttributeValue("name")+" was not of type Reference.");
          return;
        }
        DWcount += varData[i].data.length;
      }
    }

    //we are adding region stuff, need to store answer in region stuff
    VDest = new ReferenceVariable(VDname, varData[0]);
    VDest.data = new Wrapper[DWcount];
    VDest.weight = new Wrapper[DWcount];
    VDest.landFract = new Wrapper[DWcount];
    VDest.region = null;
    
    DWcount = 0;
    for(int i = 0; i < varData.length; i++)
    {//updating aggregated regions bounds
      if(varData[i].y < VDest.y)
      {
        VDest.h += (VDest.y-varData[i].y);
        VDest.y = varData[i].y;
      }
      if((varData[i].y+varData[i].h)>(VDest.y+VDest.h))
      {
        VDest.h = ((varData[i].y+varData[i].h)-VDest.y);
      }
      if(varData[i].x<VDest.x)
      {
        VDest.w += (VDest.x-varData[i].x);
        VDest.x = varData[i].x;
      }
      if((varData[i].x+varData[i].w)>(VDest.x+VDest.w))
      {
        VDest.w = ((varData[i].x+varData[i].w)-VDest.x);
      }
      //adding member variables subregions to this variables subregion list
      for(int k = 0; k < varData[i].data.length; k++)
      {
        VDest.data[DWcount] = varData[i].data[k];
        VDest.weight[DWcount] = varData[i].weight[k];
	VDest.landFract[DWcount] = varData[i].landFract[k];
        DWcount++;
      }
    }
    
    /* ---weights are stored in wrappers now..---
    //filling this new aggregated variables weight with NaN's
    VDest.weight = new double[(int)(VDest.h/VDest.res)][(int)(VDest.w/VDest.res)];
    for(int iY = 0; iY < VDest.weight.length; iY++)
    {
      for(int iX = 0; iX < VDest.weight[iY].length; iX++)
      {
        VDest.weight[iY][iX] = Double.NaN;
      }
    }
    //filling weight with actual weights for locatiosn which exist in it
    double[][] holdW;
    int offsetY, offsetX;
    for(int i = 0; i < varData.length; i++)
    {
      holdW = varData[i].weight;
      offsetY = (int)(((VDest.y+VDest.h)-(varData[i].y+varData[i].h))/VDest.res);
      offsetX = (int)((varData[i].x-VDest.x)/VDest.res);
      for(int iY = 0; iY < holdW.length; iY++)
      {
        for(int iX = 0; iX < holdW[iY].length; iX++)
        {
          if(Double.isNaN(VDest.weight[(offsetY+iY)][(offsetX+iX)]))
          {
            VDest.weight[(offsetY+iY)][(offsetX+iX)] = (holdW[iY][iX]);
          }
        }
      }
    }
    */
    
    variableList.put(VDname, VDest);
  }

  /**
   * Adds the corresponding positions in the two passed variables. Requires that the
   * variables are of the same shape.
   * @param command XML node defining the operation.
   */
  private void addCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VD, V1, V2;
    List infoList;
    Element currInfo;
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    infoList = command.getChildren("argument");
    currInfo = (Element)infoList.get(0);
    V1 = getVariable(currInfo.getAttributeValue("name"));
    currInfo = (Element)infoList.get(1);
    V2 = getVariable(currInfo.getAttributeValue("name"));
    
    if(V1.sameShape(V2))
    {
	    if(V1.isReference() && (((ReferenceVariable)V1).avg ^ ((ReferenceVariable)V2).avg)) {
		    log.log(Level.WARNING, "Adding an averaged variable with one that has not");
	    }
      VD = V1.getShape(VDname);
      variableList.put(VDname, VD);
      
      VD.setData(ComponentManipulator.addVar(V1.getData(), V2.getData()));
    } else
    {
      log.log(Level.WARNING, "Command Failed: variables of different shapes.");
    }
  }
  /**
   * Subtracts the corresponding positions in the two passed variables. Requires that the
   * variables are of the same shape.
   * @param command XML node defining the operation.
   */
  private void subCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VD, V1, V2;
    List infoList;
    Element currInfo;
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    infoList = command.getChildren("argument");
    currInfo = (Element)infoList.get(0);
    V1 = getVariable(currInfo.getAttributeValue("name"));
    currInfo = (Element)infoList.get(1);
    V2 = getVariable(currInfo.getAttributeValue("name"));
    
    if(V1.sameShape(V2))
    {
	    if(V1.isReference() && (((ReferenceVariable)V1).avg ^ ((ReferenceVariable)V2).avg)) {
		    log.log(Level.WARNING, "Subtracting an averaged variable with one that has not");
	    }
      VD = V1.getShape(VDname);
      variableList.put(VDname, VD);
      
      VD.setData(ComponentManipulator.subtractVar(V1.getData(), V2.getData()));
    } else
    {
      log.log(Level.WARNING, "Command Failed: variables of different shapes.");
    }
    
  }
  /**
   * Adds a scalar to every position in the sent variable. There is no subtract
   * scalar command as negatives can be sent to this one.
   * @param command XML node defining the operation.
   */
  private void addScalarCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    double change;
    Element currInfo;
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("scalar");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      change = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      change = holdChange.getData()[0].data[0][0];
    }
    
    VDest = VSource.getShape(VDname);
    variableList.put(VDname, VDest);
    
    VDest.setData(ComponentManipulator.addVar(VSource.getData(), change));
  }
  /**
   * Multiplys the corresponding positions in the two passed variables. Requires
   * that the variables are of the same shape.
   * @param command XML node defining the operation.
   */
  private void multiplyCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VD, V1, V2;
    List infoList;
    Element currInfo;
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    infoList = command.getChildren("argument");
    currInfo = (Element)infoList.get(0);
    V1 = getVariable(currInfo.getAttributeValue("name"));
    currInfo = (Element)infoList.get(1);
    V2 = getVariable(currInfo.getAttributeValue("name"));

    if(V1.sameShape(V2))
    {
      VD = V1.getShape(VDname);
      variableList.put(VDname, VD);
      
      VD.setData(ComponentManipulator.multiplyVar(V1.getData(), V2.getData()));
	    if(V1.isReference() && (!((ReferenceVariable)V1).avg && !((ReferenceVariable)V2).avg)) {
		    log.log(Level.WARNING, "Warning double weighting "+VDname);
	    }
	    if(V1.isReference() && (!((ReferenceVariable)V1).avg || !((ReferenceVariable)V2).avg)) {
		    ((ReferenceVariable)VD).avg = false;
	    }
    } else
    {
      log.log(Level.WARNING, "Command Failed: variables of different shapes.");
    }
  }
  /**
   * Change the average attribute of a reference variable
   * @param command XML node defining the operation.
   */
  private void setAverageProperty(Element command)
  {
	  log.log(Level.FINER, "begin function");
	  Variable Vname;
	  List infoList;
	  Element currInfo;
	  currInfo = command.getChild( "target" );
	  Vname = getVariable( currInfo.getAttributeValue("name") );

	  currInfo = command.getChild( "average" );
	  Boolean avg = (Boolean.valueOf(currInfo.getAttributeValue("value"))).booleanValue();

	  if( Vname.isReference() && !Vname.isGroup() ) { 
		  ((ReferenceVariable)Vname).avg = avg; 
	  } else if( Vname.isReference() && Vname.isGroup() ) { 
		  Map.Entry me; 
		  Variable Vcurr; 
		  Iterator it = ((GroupVariable)Vname).data.entrySet().iterator(); 
		  while(it.hasNext()) 
		  { 
			  me = (Map.Entry)it.next(); 
			  Vcurr = (Variable)me.getValue(); 
			  ((ReferenceVariable)Vcurr).avg = avg; 
		  } 
	  } else { 
		  log.log(Level.WARNING, Vname.name+" is not a reference variable. "); 
	  }
  }

  /**
   * Divides the corresponding positions in the two passed variables. Requires
   * that the variables are of the same shape.
   * @param command XML node defining the operation.
   */
  private void divideCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VD, V1, V2;
    List infoList;
    Element currInfo;
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    infoList = command.getChildren("argument");
    currInfo = (Element)infoList.get(0);
    V1 = getVariable(currInfo.getAttributeValue("name"));
    currInfo = (Element)infoList.get(1);
    V2 = getVariable(currInfo.getAttributeValue("name"));
    
    if(V1.sameShape(V2))
    {
	    //TODO: put in checks for double weighting
      VD = V1.getShape(VDname);
      variableList.put(VDname, VD);
      
      VD.setData(ComponentManipulator.divideVar(V1.getData(), V2.getData()));
    } else
    {
      log.log(Level.WARNING, "Command Failed: variables of different shapes.");
    }
  }
  /**
   * Multiplys a scalar to every position in the sent variable.
   * @param command XML node defining the operation.
   */
  private void multiplyScalarCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    double change;
    Element currInfo;
    currInfo = command.getChild("target");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      VDest = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("scalar");
      if(currInfo.getAttribute("value") != null)
      { //this is just a number, so go ahead and read it
        change = Double.parseDouble(currInfo.getAttributeValue("value"));
      } else
      { //this is a scalar variable, get the value out of it
        Variable holdChange = getVariable(currInfo.getAttributeValue("name"));
        change = holdChange.getData()[0].data[0][0];
      }
    }
    else
    {
      String VDname = currInfo.getAttributeValue("name");
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("scalar");
      if(currInfo.getAttribute("value") != null)
      { //this is just a number, so go ahead and read it
        change = Double.parseDouble(currInfo.getAttributeValue("value"));
      } else
      { //this is a scalar variable, get the value out of it
        Variable holdChange = getVariable(currInfo.getAttributeValue("name"));
        change = holdChange.getData()[0].data[0][0];
      }
      VDest = VSource.getShape(VDname);
      variableList.put(VDname, VDest);
    }
    
    VDest.setData(ComponentManipulator.multiplyVar(VSource.getData(), change));
  }
  /**
   * Divides every position in the sent variable by the scalar.
   * @param command XML node defining the operation.
   */
  private void divideScalarCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    double change;
    Element currInfo;
    currInfo = command.getChild("target");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      VDest = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("scalar");
      if(currInfo.getAttribute("value") != null)
      { //this is just a number, so go ahead and read it
        change = Double.parseDouble(currInfo.getAttributeValue("value"));
      } else
      { //this is a scalar variable, get the value out of it
        Variable holdChange = getVariable(currInfo.getAttributeValue("name"));
        change = holdChange.getData()[0].data[0][0];
      }
    }
    else
    {
      String VDname = currInfo.getAttributeValue("name");
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("scalar");
      if(currInfo.getAttribute("value") != null)
      { //this is just a number, so go ahead and read it
        change = Double.parseDouble(currInfo.getAttributeValue("value"));
      } else
      { //this is a scalar variable, get the value out of it
        Variable holdChange = getVariable(currInfo.getAttributeValue("name"));
        change = holdChange.getData()[0].data[0][0];
      }
      VDest = VSource.getShape(VDname);
      variableList.put(VDname, VDest);
    }
    if(change == 0)
    {
      log.log(Level.SEVERE, "Attempting to divide by a scalar of 0!");
    }
    VDest.setData(ComponentManipulator.divideVar(VSource.getData(), change));
  }
  /**
   * Allows only values greater than either a scalar or corresponding
   * positions in another variable to remain. The rest are set to 0.
   * @param command XML node defining the operation.
   */
  private void parseGreaterThanCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Variable VMask = null;
    Element currInfo;
    String snapVal;
    double limit = 0;
    boolean reg = false;
    boolean snap = false;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("limit");
    if(currInfo!=null)
    { //this command gives a limit
      limit = Double.parseDouble(currInfo.getAttributeValue("value"));
      snapVal = currInfo.getAttributeValue("snaptolimit");
      if((snapVal != null)&&(snapVal.equals("true")))
      {
        snap = true;
      }
    } else
    { // this command gives a corresponding region
      reg = true;
      currInfo = command.getChild("mask");
      VMask = getVariable(currInfo.getAttributeValue("name"));
    }
    
    if(reg)
    {
      if(VSource.sameShape(VMask))
      {
        VDest = VSource.getShape(VDname);
        variableList.put(VDname, VDest);
        
        VDest.setData(ComponentManipulator.greaterThanRegion(VSource.getData(), VMask.getData()));
      } else
      {
        log.log(Level.WARNING, "Command Failed: variables of different shapes.");
      }
    } else
    {
      VDest = VSource.getShape(VDname);
      variableList.put(VDname, VDest);
      
      VDest.setData(ComponentManipulator.greaterThan(VSource.getData(), limit, snap));
    }
  }
  /**
   * Allows only values less than either a scalar or corresponding
   * positions in another variable to remain. The rest are set to 0.
   * @param command XML node defining the operation.
   */
  private void parseLessThanCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Variable VMask = null;
    Element currInfo;
    String snapVal;
    double limit = 0;
    boolean reg = false;
    boolean snap = false;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("limit");
    if(currInfo!=null)
    { //this command gives a limit
      limit = Double.parseDouble(currInfo.getAttributeValue("value"));
      snapVal = currInfo.getAttributeValue("snaptolimit");
      if((snapVal != null)&&(snapVal.equals("true")))
      {
        snap = true;
      }
    } else
    { // this command gives a corresponding region
      reg = true;
      currInfo = command.getChild("mask");
      VMask = getVariable(currInfo.getAttributeValue("name"));
    }

    if(reg)
    {
      if(VSource.sameShape(VMask))
      {
        VDest = VSource.getShape(VDname);
        variableList.put(VDname, VDest);
        
        VDest.setData(ComponentManipulator.lessThanRegion(VSource.getData(), VMask.getData()));
      } else
      {
        log.log(Level.WARNING, "Command Failed: variables of different shapes.");
      }
    } else
    {
      VDest = VSource.getShape(VDname);
      variableList.put(VDname, VDest);
      
      VDest.setData(ComponentManipulator.lessThan(VSource.getData(), limit, snap));
    }
  }
/**
   * Allows only values less than or equal to either a scalar or corresponding
   * positions in another variable to remain. The rest are set to 0.
   * @param command XML node defining the operation.
   */
  private void parseLessThanOrEqualCommand(Element command)
  {
	  log.log(Level.FINER, "begin function");
	  Variable VDest;
	  Variable VSource;
	  Variable VMask = null;
	  Element currInfo;
	  String snapVal;
	  double limit = 0;
	  boolean reg = false;
	  boolean snap = false;

	  currInfo = command.getChild("target");
	  String VDname = currInfo.getAttributeValue("name");
	  currInfo = command.getChild("argument");
	  VSource = getVariable(currInfo.getAttributeValue("name"));
	  currInfo = command.getChild("limit");
	  if(currInfo!=null)
	  { //this command gives a limit
		  limit = Double.parseDouble(currInfo.getAttributeValue("value"));
		  snapVal = currInfo.getAttributeValue("snaptolimit");
		  if((snapVal != null)&&(snapVal.equals("true")))
		  {
			  snap = true;
		  }
	  } else
	  { // this command gives a corresponding region
		  reg = true;
		  currInfo = command.getChild("mask");
		  VMask = getVariable(currInfo.getAttributeValue("name"));
	  }

	  if(reg)
	  {
		  throw new UnsupportedOperationException();
		  /*
		  if(VSource.sameShape(VMask))
		  {
			  VDest = VSource.getShape(VDname);
			  variableList.put(VDname, VDest);

			  VDest.setData(ComponentManipulator.lessThanRegion(VSource.getData(), VMask.getData()));
		  } else
		  {
			  log.log(Level.WARNING, "Command Failed: variables of different shapes.");
		  }
		  */
	  } else
	  {
		  VDest = VSource.getShape(VDname);
		  variableList.put(VDname, VDest);

		  VDest.setData(ComponentManipulator.lessThanOrEqual(VSource.getData(), limit, snap));
	  }
  }
  /**
   * Randomly removes cells not already equal to NaN from the variable.
   * The fraction of current cells remaining after this operation is defined
   * by the goal element. Cells are removed by pure randomization, the goal
   * fraction is in no way assured.
   * @param command XML node defining the operation.
   */
  private void removeRandomCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    double fraction;
    Element currInfo;
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("goal");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      fraction = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      fraction = holdChange.getData()[0].data[0][0];
    }
    
    VDest = VSource.getShape(VDname);
    variableList.put(VDname, VDest);
    
    VDest.setData(ComponentManipulator.removeRandom(VSource.getData(), fraction));
  }
  /**
   * Randomly removes cells not already equal to NaN from the variable.
   * The fraction of current cells remaining after this operation is defined
   * by the goal element. Cells are removed by guided randomization, if to
   * many or to few cells are remaining the random value slowly shifts itself
   * in an attempt to approach the goal fraction as closely as possible.
   * @param command XML node defining the operation.
   */
  private void removeRandomGuidedCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    double fraction;
    Element currInfo;
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("goal");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      fraction = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      fraction = holdChange.getData()[0].data[0][0];
    }
    
    VDest = VSource.getShape(VDname);
    variableList.put(VDname, VDest);
    
    VDest.setData(ComponentManipulator.removeRandomGuided(VSource.getData(), fraction));
  }
  /**
   * Combines two variables' values using mask OR rules. Rather than NaN
   * values being thrown away outright if even one argument has a value at a
   * particular point that point will inherit a value. OR is performed
   * additively with a max of 1 (for full masking).
   * @param command XML node defining the operation.
   */
  private void maskCombineOrCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VD, V1, V2;
    List infoList;
    Element currInfo;
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    infoList = command.getChildren("argument");
    currInfo = (Element)infoList.get(0);
    V1 = getVariable(currInfo.getAttributeValue("name"));
    currInfo = (Element)infoList.get(1);
    V2 = getVariable(currInfo.getAttributeValue("name"));
    
    if(V1.sameShape(V2))
    {
      VD = V1.getShape(VDname);
      variableList.put(VDname, VD);
      
      VD.setData(ComponentManipulator.maskCombineOr(V1.getData(), V2.getData()));
    } else
    {
      log.log(Level.WARNING, "Command Failed: variables of different shapes.");
    }
  }
  /**
   * Combines two variables' values using mask AND rules. AND is performed
   * as conservatively as possible on a per cell basis. A cell will only
   * inheret a value if its two parent cells definately overlap (their fill
   * is more than 1 combined). This is done by adding cell values and then
   * subtracting 1 full cell worth.
   * @param command XML node defining the operation.
   */
  private void maskCombineAndCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VD, V1, V2;
    List infoList;
    Element currInfo;
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    infoList = command.getChildren("argument");
    currInfo = (Element)infoList.get(0);
    V1 = getVariable(currInfo.getAttributeValue("name"));
    currInfo = (Element)infoList.get(1);
    V2 = getVariable(currInfo.getAttributeValue("name"));
    
    if(V1.sameShape(V2))
    {
      VD = V1.getShape(VDname);
      variableList.put(VDname, VD);
      
      VD.setData(ComponentManipulator.maskCombineAnd(V1.getData(), V2.getData()));
    } else
    {
      log.log(Level.WARNING, "Command Failed: variables of different shapes.");
    }
  }
  /**
   * Allows only masked values to remain. If weight is selected, only
   * the portion of mask for each cell will remain of the initial value.
   * If weight is not selected, the full value will remain as long as there
   * is some portion masked. If limit is used portions must additionally be
   * greater than the given limit value.
   * @param command XML node defining the operation.
   */
  private void maskRemainCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VD, VSource, VMask;
    boolean toWeight;
    double limit;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("mask");
    VMask = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("weight");
    toWeight = (Boolean.valueOf(currInfo.getAttributeValue("value"))).booleanValue();
    
    currInfo = command.getChild("limit");
    if(currInfo != null)
    {
      if(currInfo.getAttribute("value")!=null)
      { //this is just a number, so go ahead and read it
        limit = Double.parseDouble(currInfo.getAttributeValue("value"));
      } else
      { //this is a scalar variable, get the value out of it
        Variable holdChange = (Variable)variableList
            .get(currInfo.getAttributeValue("name"));
        limit = holdChange.getData()[0].data[0][0];
      }
    } else
    {
      limit = 0;
    }

    if(!VMask.isReference()) {
	    double maskVal = VMask.getData()[0].getData()[0][0];
	    VD = VSource.getShape(VDname);
	    variableList.put(VDname, VD);
	    if(toWeight)
	    {
		    VD.setData(ComponentManipulator.maskRemainWeight(VSource.getData(), maskVal));
	    } else
	    {
		    VD.setData(ComponentManipulator.maskRemain(VSource.getData(), maskVal));
	    }
	    return;
    }
    
    if(VSource.sameShape(VMask))
    {
      VD = VSource.getShape(VDname);
      variableList.put(VDname, VD);
      if(toWeight)
      {
        VD.setData(ComponentManipulator.maskRemainWeight(VSource.getData(), VMask.getData(), limit));
	if(VSource.isReference() && (!((ReferenceVariable)VSource).avg && !((ReferenceVariable)VMask).avg)) {
		log.log(Level.WARNING, "Warning double weighting "+VDname);
	}
	if(VSource.isReference() && (!((ReferenceVariable)VSource).avg || !((ReferenceVariable)VMask).avg)) {
		((ReferenceVariable)VD).avg = false;
	}
      } else
      {
        VD.setData(ComponentManipulator.maskRemain(VSource.getData(), VMask.getData(), limit));
      }
    } else
    {
      log.log(Level.WARNING, "Command Failed: variables of different shapes.");
    }
  }
  /**
   * Allows only UNmasked values to remain. If weight is selected, only
   * the UMmasked portion of each cell will remain of the initial value.
   * If weight is not selected, the full value will remain as long as there
   * is some portion UNmasked. If limit is used portions must additionally be
   * less than the given limit value.
   * @param command XML node defining the operation.
   */
  private void maskRemoveCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VD, VSource, VMask;
    boolean toWeight;
    boolean userLimit = false;
    double limit = 0;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("mask");
    VMask = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("weight");
    toWeight = (Boolean.valueOf(currInfo.getAttributeValue("value"))).booleanValue();
    
    currInfo = command.getChild("limit");
    if(currInfo != null)
    {
      if(currInfo.getAttribute("value")!=null)
      { //this is just a number, so go ahead and read it
        limit = Double.parseDouble(currInfo.getAttributeValue("value"));
      } else
      { //this is a scalar variable, get the value out of it
        Variable holdChange = (Variable)variableList
            .get(currInfo.getAttributeValue("name"));
        limit = holdChange.getData()[0].data[0][0];
      }
      userLimit = true;
    }
    
    if(!VMask.isReference()) {
	    double maskVal = VMask.getData()[0].getData()[0][0];
	    VD = VSource.getShape(VDname);
	    variableList.put(VDname, VD);
	    if(toWeight)
	    {
		    VD.setData(ComponentManipulator.maskRemoveWeight(VSource.getData(), maskVal));
	    } else
	    {
		    VD.setData(ComponentManipulator.maskRemove(VSource.getData(), maskVal));
	    }
	    return;
    }

    if(VSource.sameShape(VMask))
    {
      VD = VSource.getShape(VDname);
      variableList.put(VDname, VD);
      if(toWeight)
      {
        if(!userLimit)
        {
          limit = 1;
        }
        VD.setData(ComponentManipulator.maskRemoveWeight(VSource.getData(), VMask.getData(), limit));
	// TODO: should I put checks for double weight or set avg here?
      } else
      {
        if(!userLimit)
        {
          limit = 0;
        }
        VD.setData(ComponentManipulator.maskRemove(VSource.getData(), VMask.getData(), limit));
      }
    } else
    {
      log.log(Level.WARNING, "Command Failed: variables of different shapes.");
    }
  }
  /**
   * Counts the occurance of elements which are greater in value than either
   * the passed scalar or the passed variable.
   * @param command XML node defining the operation.
   */
  private void countGreaterThanCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Variable VMask = null;
    Element currInfo;
    double limit = 0;
    boolean reg = false;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("limit");
    if(currInfo!=null)
    { //this command gives a limit
      limit = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { // this command gives a corresponding region
      reg = true;
      currInfo = command.getChild("mask");
      VMask = getVariable(currInfo.getAttributeValue("name"));
    }

    if(reg)
    {
      if(VSource.sameShape(VMask))
      {
        VDest = new DataVariable(VDname);
        variableList.put(VDname, VDest);
        
        VDest.setData(ComponentManipulator.countGreaterThanRegion(VSource.getData(), VMask.getData()));
      } else
      {
        log.log(Level.WARNING, "Command Failed: variables of different shapes.");
      }
    } else
    {
      VDest = new DataVariable(VDname);
      variableList.put(VDname, VDest);
      
      VDest.setData(ComponentManipulator.countGreaterThan(VSource.getData(), limit));
    }
  }
  /**
   * Counts the occurance of elements which are less in value than either
   * the passed scalar or the passed variable.
   * @param command XML node defining the operation.
   */
  private void countLessThanCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Variable VMask = null;
    Element currInfo;
    double limit = 0;
    boolean reg = false;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("limit");
    if(currInfo!=null)
    { //this command gives a limit
      limit = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { // this command gives a corresponding region
      reg = true;
      currInfo = command.getChild("mask");
      VMask = getVariable(currInfo.getAttributeValue("name"));
    }
    
    if(reg)
    {
      if(VSource.sameShape(VMask))
      {
        VDest = new DataVariable(VDname);
        variableList.put(VDname, VDest);
        
        VDest.setData(ComponentManipulator.countLessThanRegion(VSource.getData(), VMask.getData()));
      } else
      {
        log.log(Level.WARNING, "Command Failed: variables of different shapes.");
      }
    } else
    {
      VDest = new DataVariable(VDname);
      variableList.put(VDname, VDest);
      
      VDest.setData(ComponentManipulator.countLessThan(VSource.getData(), limit));
    }
  }
  /**
   * Counts the number of valid elements in the passed variable. Valid elements
   * are ones which are not defined as outside the mask of a region.
   * @param command XML node defining the operation.
   */
  private void countElementsCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Element currInfo;
    
    currInfo = command.getChild("target");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      VDest = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
    }
    else
    {
      String VDname = currInfo.getAttributeValue("name");
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
      //creating new datavariable to hold result
      VDest = new DataVariable();
      VDest.name = VDname;
      variableList.put(VDname, VDest);
    }

    VDest.setData(ComponentManipulator.countElements(VSource.getData()));
  }
  /**
   * Determines whether the data contained in this variable is added or averaged
   * on aggregation, then performs the correct operation with all valid
   * elements. Valid elements
   * are ones which are not defined as outside the mask of a region.
   * @param command XML node defining the operation.
   */
  private void aggregateValuesCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    ReferenceVariable VSource;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = (ReferenceVariable)variableList.get(currInfo.getAttributeValue("name"));
    //creating new datavariable to hold result
    VDest = new DataVariable();
    VDest.name = VDname;
    variableList.put(VDname, VDest);
    
    if((VSource).avg)
    { //dont need to weight values
      VDest.setData(ComponentManipulator.avgOverRegion(VSource.getData(), VSource.weight, VSource.x, VSource.y, VSource.h));
    } else
    {
      VDest.setData(ComponentManipulator.sumValues(VSource.getData()));
    }
  }
  /**
   * Sums the value of all valid elements in a variable. Valid elements
   * are ones which are not defined as outside the mask of a region.
   * @param command XML node defining the operation.
   */
  private void sumValuesCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    //creating new datavariable to hold result
    VDest = new DataVariable();
    VDest.name = VDname;
    variableList.put(VDname, VDest);
    if(VSource.isGroup())
    { //dealing with a group, average each member
      Map.Entry me;
      Variable Vcurr, Nvar;
      Iterator it = ((GroupVariable)VSource).data.entrySet().iterator();
      double[][] tempSum = new double[1][1];
      tempSum[0][0] = 0;
      
      while(it.hasNext())
      {
        me = (Map.Entry)it.next();
        Vcurr = (Variable)me.getValue();
	if((Vcurr.isReference())&&(((ReferenceVariable)Vcurr).avg))
	{ //dont need to weight values
		tempSum[0][0] += ((DataWrapper)(ComponentManipulator.sumValues(((ReferenceVariable)Vcurr).getData(),
					       ((ReferenceVariable)Vcurr).weight, ((ReferenceVariable)Vcurr).getLandFract(),
					       ((ReferenceVariable)Vcurr).x, ((ReferenceVariable)Vcurr).y, 
					       ((ReferenceVariable)Vcurr).h))[0]).data[0][0];
	} else
	{
		tempSum[0][0] += ((DataWrapper)(ComponentManipulator.sumValues(Vcurr.getData()))[0]).data[0][0];
	}
        
      }
      Wrapper[] tempWrapper = new Wrapper[1];
      tempWrapper[0] = new DataWrapper();
      tempWrapper[0].setData(tempSum);
      VDest.setData(tempWrapper);
      
    } else if((VSource.isReference())&&(((ReferenceVariable)VSource).avg))
    { //dont need to weight values
      VDest.setData(ComponentManipulator.sumValues(((ReferenceVariable)VSource).getData(), ((ReferenceVariable)VSource).weight, 
			      ((ReferenceVariable)VSource).getLandFract(), ((ReferenceVariable)VSource).x, ((ReferenceVariable)VSource).y, 
			      ((ReferenceVariable)VSource).h));
    } else
    {
      VDest.setData(ComponentManipulator.sumValues(VSource.getData()));
    }
  }
  /**
   * Sums the size of land represented by this variable. Assumes that the
   * given variable containes coverage values. Number returned is in km^2.
   * @param command XML node defining the operation.
   */
  private void sumAreaCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    //creating new datavariable to hold result
    VDest = new DataVariable();
    VDest.name = VDname;
    variableList.put(VDname, VDest);
    
    if((VSource.isReference())&&(((ReferenceVariable)VSource).avg))
    { //dont need to weight values
      VDest.setData(ComponentManipulator.sumArea(((ReferenceVariable)VSource).getData(), ((ReferenceVariable)VSource).weight, 
			      ((ReferenceVariable)VSource).getLandFract(), ((ReferenceVariable)VSource).x, ((ReferenceVariable)VSource).y, 
			      ((ReferenceVariable)VSource).h));
    } else
    {
      VDest.setData(ComponentManipulator.sumArea(VSource.getData()));
    }
  }

  /**
   * Sums the size of land represented by this variable with no regards 
   * to the value of the data in the variable.  Number returned is in km^2.
   * @param command XML node defining the operation.
   */
  private void sumRegionAreaCommand(Element command) {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    //creating new datavariable to hold result
    VDest = new DataVariable();
    VDest.name = VDname;
    variableList.put(VDname, VDest);
    
    /*
    if((VSource.isReference())&&(((ReferenceVariable)VSource).avg))
    { //dont need to weight values
      VDest.setData(ComponentManipulator.sumRegionArea(((ReferenceVariable)VSource).getData(), ((ReferenceVariable)VSource).weight, ((ReferenceVariable)VSource).x, ((ReferenceVariable)VSource).y, ((ReferenceVariable)VSource).h));
    } else
    {
    */
      VDest.setData(ComponentManipulator.sumRegionArea(VSource.getData()));
      /*
    }
    */
  }


   /**
   * Gets the largest single value in the passed variable.
   * @param command XML node defining the operation.
   */
  private void largestValueCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Element currInfo;
    
    currInfo = command.getChild("target");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      VDest = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
    }
    else
    {
      String VDname = currInfo.getAttributeValue("name");
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
      //creating new datavariable to hold result
      VDest = new DataVariable();
      VDest.name = VDname;
      variableList.put(VDname, VDest);
    }
    
    VDest.setData(ComponentManipulator.largestValue(VSource.getData()));
  }
  /**
   * Gets the smallest single value in the passed variable.
   * @param command XML node defining the operation.
   */
  private void smallestValueCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Element currInfo;
    
    currInfo = command.getChild("target");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      VDest = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
    }
    else
    {
      String VDname = currInfo.getAttributeValue("name");
      currInfo = command.getChild("argument");
      VSource = getVariable(currInfo.getAttributeValue("name"));
      //creating new datavariable to hold result
      VDest = new DataVariable();
      VDest.name = VDname;
      variableList.put(VDname, VDest);
    }
    
    VDest.setData(ComponentManipulator.smallestValue(VSource.getData()));
  }
  /**
   * Gets the average of all valid elements in a varaible. Valid elements
   * are ones which are not defined as outside the mask of a region. Values
   * which are only paritally in this region are weighted to account for
   * this fact, if that have not already been weighted.
   * @param command XML node defining the operation.
   */
  private void avgOverRegionCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    //creating new datavariable to hold result
    if(VSource.isGroup())
    { //dealing with a group, average each member
      VDest = new GroupVariable(VDname);
      variableList.put(VDname, VDest);
      Map.Entry me;
      Variable Vcurr, Nvar;
      Iterator it = ((GroupVariable)VSource).data.entrySet().iterator();
      
      while(it.hasNext())
      {
        me = (Map.Entry)it.next();
        Vcurr = (Variable)me.getValue();
        
        if((Vcurr.isReference())&&(((ReferenceVariable)Vcurr).avg))
        { //dont need to weight values
          Nvar = new DataVariable(Vcurr.name, ComponentManipulator.avgOverRegion(Vcurr.getData(), ((ReferenceVariable)Vcurr).weight, ((ReferenceVariable)Vcurr).x, ((ReferenceVariable)Vcurr).y, ((ReferenceVariable)Vcurr).h));
          ((GroupVariable)VDest).addData(Nvar);
        } else
        {
          Nvar = new DataVariable(Vcurr.name, ComponentManipulator.avgOverRegion(Vcurr.getData()));
          ((GroupVariable)VDest).addData(Nvar);
        }
      }
      
    } else
    { //just one
      VDest = new DataVariable(VDname);
      variableList.put(VDname, VDest);
      
      if((VSource.isReference())&&(((ReferenceVariable)VSource).avg))
      { //dont need to weight values
        VDest.setData(ComponentManipulator.avgOverRegion(VSource.getData(), ((ReferenceVariable)VSource).weight, ((ReferenceVariable)VSource).x, ((ReferenceVariable)VSource).y, ((ReferenceVariable)VSource).h));
      } else
      {
        VDest.setData(ComponentManipulator.avgOverRegion(VSource.getData()));
      }
    }
  }
  /**
   * Gets the average of all valid elements in a varaible with respect to
   * their proportion of area. Area changes based on latitude. Valid elements
   * are ones which are not defined as outside the mask of a region. Values
   * which are only paritally in this region are weighted to account for
   * this fact, if that have not already been weighted.
   * @param command XML node defining the operation.
   */
  private void avgOverRegionByAreaCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    //creating new datavariable to hold result
    if(VSource.isGroup())
    { //dealing with a group, average each member
      VDest = new GroupVariable(VDname);
      variableList.put(VDname, VDest);
      Map.Entry me;
      ReferenceVariable Vcurr;
      DataVariable Nvar;
      Iterator it = ((GroupVariable)VSource).data.entrySet().iterator();
      
      while(it.hasNext())
      {
        me = (Map.Entry)it.next();
        Vcurr = (ReferenceVariable)me.getValue();
        
        if((Vcurr).avg)
        { //dont need to weight values
          Nvar = new DataVariable(Vcurr.name, ComponentManipulator.avgOverRegionByArea(Vcurr.getData(), Vcurr.weight, Vcurr.x, Vcurr.y, Vcurr.w, Vcurr.h));
          ((GroupVariable)VDest).addData(Nvar);
        } else
        {
          Nvar = new DataVariable(Vcurr.name, ComponentManipulator.avgOverRegionByArea(VSource.getData(), Vcurr.x, Vcurr.y, Vcurr.w, Vcurr.h));
          ((GroupVariable)VDest).addData(Nvar);
        }
      }
      
    } else
    { //just one
      VDest = new DataVariable(VDname);
      variableList.put(VDname, VDest);
      
      if((VSource.isReference())&&(((ReferenceVariable)VSource).avg))
      { //dont need to weight values
        VDest.setData(ComponentManipulator.avgOverRegionByArea(VSource.getData(), ((ReferenceVariable)VSource).weight, ((ReferenceVariable)VSource).x, ((ReferenceVariable)VSource).y, ((ReferenceVariable)VSource).w, ((ReferenceVariable)VSource).h));
      } else
      {
        VDest.setData(ComponentManipulator.avgOverRegionByArea(VSource.getData(), ((ReferenceVariable)VSource).x, ((ReferenceVariable)VSource).y, ((ReferenceVariable)VSource).w, ((ReferenceVariable)VSource).h));
      }
    }
  }
  /**
   * Gets the average at each location of all the variable values which 
   * occur at that location in their respective variable. Valid elements
   * are ones which are not defined as outside the mask of a region.
   * @param command XML node defining the operation.
   */
  private void avgVariablesCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest = null;
    Variable VSource = null;
    Element currInfo;
    List args;
    Wrapper[][] sendData;
    
    currInfo = command.getChild("target");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      VDest = getVariable(currInfo.getAttributeValue("name"));
      args = command.getChildren("argument");
      sendData = new Wrapper[args.size()][];
      for(int i = 0; i < args.size(); i++)
      {
        currInfo = (Element)args.get(i);
        VSource = getVariable(currInfo.getAttributeValue("name"));
        sendData[i] = VSource.getData();
      }
    }
    else
    {
      String VDname = currInfo.getAttributeValue("name");
      args = command.getChildren("argument");
      sendData = new Wrapper[args.size()][];
      for(int i = 0; i < args.size(); i++)
      {
        currInfo = (Element)args.get(i);
        VSource = getVariable(currInfo.getAttributeValue("name"));
        sendData[i] = VSource.getData();
      }
      //creating new datavariable to hold result
      VDest = VSource.getShape(VDname);
      variableList.put(VDname, VDest);
    }
    
    VDest.setData(ComponentManipulator.avgVariables(sendData));
  }
  /**
   * Gets the average for a region of a list of overlapping variables.
   * Valid elements are ones which are not defined as outside the mask of
   * a region.
   * @param command XML node defining the operation.
   */
  private void avgVariablesOverRegionCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest = null;
    Variable VSource = null;
    Element currInfo;
    List args;
    Wrapper[] toSend;
    Wrapper[][] sendData;
    
    currInfo = command.getChild("target");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      VDest = getVariable(currInfo.getAttributeValue("name"));
      args = command.getChildren("argument");
      sendData = new Wrapper[args.size()][];
      for(int i = 0; i < args.size(); i++)
      {
        currInfo = (Element)args.get(i);
        VSource = getVariable(currInfo.getAttributeValue("name"));
        sendData[i] = VSource.getData();
      }
    }
    else
    {
      String VDname = currInfo.getAttributeValue("name");
      args = command.getChildren("argument");
      sendData = new Wrapper[args.size()][];
      for(int i = 0; i < args.size(); i++)
      {
        currInfo = (Element)args.get(i);
        VSource = getVariable(currInfo.getAttributeValue("name"));
        sendData[i] = VSource.getData();
      }
      //creating new datavariable to hold result
      VDest = new DataVariable(VDname);
      variableList.put(VDname, VDest);
    }
    
    if((VSource.isReference())&&(((ReferenceVariable)VSource).avg))
    { //dont need to weight values
      toSend = ComponentManipulator.avgVariables(sendData);
      VDest.setData(ComponentManipulator.avgOverRegion(toSend, ((ReferenceVariable)VSource).weight, ((ReferenceVariable)VSource).x, ((ReferenceVariable)VSource).y, ((ReferenceVariable)VSource).h));
    } else
    {
      toSend = ComponentManipulator.avgVariables(sendData);
      VDest.setData(ComponentManipulator.avgOverRegion(toSend));
    }
  }
  /**
   * Gets the average for a region of a list of overlapping variables
   * accounting for area. Area changes based on latitude.
   * Valid elements are ones which are not defined as outside the mask of
   * a region.
   * @param command XML node defining the operation.
   */
  private void avgVariablesOverRegionByAreaCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest = null;
    Variable VSource = null;
    Element currInfo;
    List args;
    Wrapper[] toSend;
    Wrapper[][] sendData;
    
    currInfo = command.getChild("target");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      VDest = getVariable(currInfo.getAttributeValue("name"));
      args = command.getChildren("argument");
      sendData = new Wrapper[args.size()][];
      for(int i = 0; i < args.size(); i++)
      {
        currInfo = (Element)args.get(i);
        VSource = getVariable(currInfo.getAttributeValue("name"));
        sendData[i] = VSource.getData();
      }
    }
    else
    {
      String VDname = currInfo.getAttributeValue("name");
      args = command.getChildren("argument");
      sendData = new Wrapper[args.size()][];
      for(int i = 0; i < args.size(); i++)
      {
        currInfo = (Element)args.get(i);
        VSource = getVariable(currInfo.getAttributeValue("name"));
        sendData[i] = VSource.getData();
      }
      //creating new datavariable to hold result
      VDest = new DataVariable(VDname);
      variableList.put(VDname, VDest);
    }
    
    if((VSource.isReference())&&(((ReferenceVariable)VSource).avg))
    { //need to weight values
      toSend = ComponentManipulator.avgVariables(sendData);
      VDest.setData(ComponentManipulator.avgOverRegionByArea(toSend,  ((ReferenceVariable)VSource).weight, ((ReferenceVariable)VSource).x, ((ReferenceVariable)VSource).y, ((ReferenceVariable)VSource).w, ((ReferenceVariable)VSource).h));
    } else
    {
      toSend = ComponentManipulator.avgVariables(sendData);
      VDest.setData(ComponentManipulator.avgOverRegionByArea(toSend, ((ReferenceVariable)VSource).x, ((ReferenceVariable)VSource).y, ((ReferenceVariable)VSource).w, ((ReferenceVariable)VSource).h));
    }
  }
  /**
   * Weights each valid value in the variable by another mask variable's
   * coresponding positions based one value limits and weight limits.
   * Valid elements are ones which are not defined as outside the mask of
   * a region.
   * @param command XML node defining the operation.
   */
  private void weightValuesCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest = null;
    Variable VSource = null;
    Variable VScale = null;
    Element currInfo;
    Element minMax;
    double minWeight, maxWeight;
    double minVal, maxVal;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("scale");
    VScale = getVariable(currInfo.getAttributeValue("name"));
    //this makes sure a scalar variable can be used anywhere a typed scalar can
    minMax = currInfo.getChild("minimum");
    if(minMax.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      minVal = Double.parseDouble(minMax.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = getVariable(minMax.getAttributeValue("name"));
      minVal = holdChange.getData()[0].data[0][0];
    }

    minMax = currInfo.getChild("maximum");
    if(minMax.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      maxVal = Double.parseDouble(minMax.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = getVariable(minMax.getAttributeValue("name"));
      maxVal = holdChange.getData()[0].data[0][0];
    }

    currInfo = command.getChild("minimumWeight");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      minWeight = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      minWeight = holdChange.getData()[0].data[0][0];
    }

    currInfo = command.getChild("maximumWeight");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      maxWeight = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      maxWeight = holdChange.getData()[0].data[0][0];
    }

    if(VSource.sameShape(VScale))
    {
      VDest = VSource.getShape(VDname);
      variableList.put(VDname, VDest);
      
      VDest.setData(ComponentManipulator.weightValues(VSource.getData(), VScale.getData(), minVal, maxVal, minWeight, maxWeight));
    } else
    {
      log.log(Level.WARNING, "Command Failed: variables of different shapes.");
    }
  }
  /**
   * Does a frequency analysis on square km of the passed variable.
   * Can either handle splitting one variable based on it's values,
   * or splitting one variable which contains coverage fractions based
   * on another variables values. Both usages account for the curvature
   * of the earth.
   * @param command XML node defining the operation.
   */
  private void frequencyAnalysisCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest, VSource, VSplit;
    int buckets;
    Element currInfo;

    //output code here
    
    currInfo = command.getChild("argument");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("buckets");
    if(currInfo.getAttribute("number")!= null)
    { //this is just a number, so go ahead and read it
      buckets = Integer.parseInt(currInfo.getAttributeValue("number"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = getVariable(currInfo.getAttributeValue("name"));
      buckets = (int)holdChange.getData()[0].data[0][0];
    }
    //creating new datavariable to hold result
    VDest = new DataVariable();
    VDest.name = "frequency analysis of "+VSource.name;
    //variableList.put(VDname, VDest);
    
    currInfo = command.getChild("split");
    if(currInfo != null)
    { //coverage source, splitting on other variable
      VSplit = getVariable(currInfo.getAttributeValue("name"));
      
      if(VSource.sameShape(VSplit))
      {
        VDest.setData(ComponentManipulator.freqAnalysis(VSource.getData(), VSplit.getData(), 
				((ReferenceVariable)VSource).weight, ((ReferenceVariable)VSource).getLandFract(),
				buckets, ((ReferenceVariable)VSource).avg));
      } else
      {
        log.log(Level.WARNING, "Command Failed: variables of different shapes.");
      }
    } else
    { //splitting on own value, not coverage
      VDest.setData(ComponentManipulator.freqAnalysis(VSource.getData(), ((ReferenceVariable)VSource).weight,
			      ((ReferenceVariable)VSource).getLandFract(), buckets, ((ReferenceVariable)VSource).avg));
    }
    
    //need to output cuz this is an abnormally shaped return
    currInfo = command.getChild("output");
    Writer writer;
    if(currInfo.getAttributeValue("csvfile") != null) {
	    writer = CSVFileManager.getInstance().getCSVOutputter(currInfo);
    } else {
	    // assume console
	    writer = new ConsoleWriter(Console.getConsole("Default Out"));
    }
    try
    {

      BufferedWriter out = new BufferedWriter(writer);
      VDest.printVerbose(out);
    } catch(IOException e)
    {
      log.log(Level.SEVERE, "IOException in -> freqAnalysisCommand on write");
    }
  }
  /**
   * This command is a simple math problem, given 5 parameters it computes
   * the idealized turbine production in GWh/yr. This uses some specific math
   * functions which can potentially fail given abnormal inputs.
   * @param command XML node defining the operation.
   */
  private void windPowerCalcCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    double height, diam, turb, avail, loss;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("height");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      height = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      height = holdChange.getData()[0].data[0][0];
    }
    
    currInfo = command.getChild("diameter");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      diam = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      diam = holdChange.getData()[0].data[0][0];
    }
    currInfo = command.getChild("turbine size");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      turb = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      turb = holdChange.getData()[0].data[0][0];
    }
    currInfo = command.getChild("availability");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      avail = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      avail = holdChange.getData()[0].data[0][0];
    }
    currInfo = command.getChild("annual loss");
    if(currInfo.getAttribute("value")!=null)
    { //this is just a number, so go ahead and read it
      loss = Double.parseDouble(currInfo.getAttributeValue("value"));
    } else
    { //this is a scalar variable, get the value out of it
      Variable holdChange = (Variable)variableList
          .get(currInfo.getAttributeValue("name"));
      loss = holdChange.getData()[0].data[0][0];
    }
    
    
    
    //creating new datavariable to hold result
    VDest = new DataVariable();
    VDest.name = VDname;
    variableList.put(VDname, VDest);

    VDest.setData(ComponentManipulator.windPower(height, diam, turb, avail, loss));

  }
  /**
   * Extracts a subregion of the specified reference variable. The subregion
   * is defined as a region which is the whole or part of the shape of the
   * passed reference variable. Extracted region can be on any level of the
   * sent region.
   * @param command XML node defining the operation.
   */
  private void extractSubRegionCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Region VShape;
    Element currInfo;
    currInfo = command.getChild("target");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      VDest = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("source");
      VSource = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("shape");
      VShape = (Region)regionList.get(currInfo.getAttributeValue("value"));
    }
    else
    {
      String VDname = currInfo.getAttributeValue("name");
      currInfo = command.getChild("source");
      VSource = getVariable(currInfo.getAttributeValue("name"));
      currInfo = command.getChild("shape");
      VShape = (Region)regionList.get(currInfo.getAttributeValue("value"));
      
      VDest = VSource.getShape(VDname);
      variableList.put(VDname, VDest);
    }
    
    VDest.setData(VShape.extractRegion((ReferenceVariable)VSource));
    ((ReferenceVariable)VDest).weight = VShape.getWorkingM("weight", "0");
    ((ReferenceVariable)VDest).landFract = VShape.getWorkingM(DataBuilder.LAND_FRACTION, "0.0");
  }
  /**
   * Give the user access to a contained Child varialbe in the passed
   * Group variable. Must know the contained name of the variable to be
   * extracted. For a time group name will be the string time respresentation
   * of the data. For subregion group name will be the name of the region
   * mapped by said child variable. For explicit groups child name is the
   * name of the variable used in group creation.
   * @param command XML node defining the operation.
   */
  private void getChildVariable(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable VDest;
    Variable VSource;
    Variable Vchild;
    String cName;
    Element currInfo;
    
    currInfo = command.getChild("target");
    String VDname = currInfo.getAttributeValue("name");
    currInfo = command.getChild("source");
    VSource = getVariable(currInfo.getAttributeValue("name"));
    currInfo = command.getChild("child");
    cName = currInfo.getAttributeValue("value");
    
    if(VSource.isGroup())
    {
      if(((GroupVariable)VSource).data.containsKey(cName))
      {
        Vchild = (Variable)((GroupVariable)VSource).data.get(cName);
        
        VDest = Vchild.getCopy(VDname);
        variableList.put(VDname, VDest);
      } else
      {
        log.log(Level.WARNING, "Source variable: "+VSource.name+" does not contain child variable: "+cName);
      }
    } else
    {
      log.log(Level.WARNING, "Source variable: "+VSource.name+" is not a Group variable.");
    }
  }
  /**
   * Prints only the actual data elements of the passed variable to the screen.
   * @param command XML node defining the operation.
   */
  private void printCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable toPrint;
    
    if(variableList.containsKey(command.getAttributeValue("variable")))
    {
      toPrint = getVariable(command.getAttributeValue("variable"));
      if(command.getAttributeValue("file") == null)
      {
        if(command.getAttributeValue("console") == null)
	{
		if(command.getAttributeValue("csvfile") == null)
		{ //print to default console
			try
			{
				//BufferedWriter out = new BufferedWriter(new PrintWriter(System.out));
				BufferedWriter out = new BufferedWriter(new ConsoleWriter(Console.getConsole("Default Out")));
				toPrint.printStandard(out);
			} catch(IOException e)
			{
				log.log(Level.SEVERE, "IOException in -> printCommand (default console)");
			}
		} else {
			// print to csv
			BufferedWriter out = new BufferedWriter(CSVFileManager.getInstance().getCSVOutputter(command));
			try {
				toPrint.printStandard(out);
			} catch(IOException ioe) {
				log.log(Level.SEVERE, "IOException in -> printCommand CSV file: "+ioe);
			}
			// TODO: shoud I close even though it won't do anything?
		}
        } else
        { //print to specified console
          try
          {
            //BufferedWriter out = new BufferedWriter(new PrintWriter(System.out));
            BufferedWriter out = new BufferedWriter(new ConsoleWriter(Console.getConsole(command.getAttributeValue("console"))));
            toPrint.printStandard(out);
          } catch(IOException e)
          {
            log.log(Level.SEVERE, "IOException in -> printCommand (console "+command.getAttributeValue("console")+")");
          }
        }
      } else
      { //print this to the specified file
        try
        {
          BufferedWriter out;
          if(writeFiles.contains(command.getAttributeValue("file")))
          {
            out = new BufferedWriter(new FileWriter(command
                .getAttributeValue("file"), true));
          } else
          {
            out = new BufferedWriter(new FileWriter(command
                .getAttributeValue("file"), false));
            writeFiles.add(command.getAttributeValue("file"));
          }

          toPrint.printStandard(out);
        } catch(IOException e)
        {
          log.log(Level.SEVERE, "IOException in -> printCommand (file)");
        }
      }
    } else
    { //this variable did not exist
      log.log(Level.WARNING, "Variable: "+command.getAttributeValue("variable")+" is undefined.");
    }
  }
  /**
   * Prints the passed variable to the screen along with additional qualifying
   * information such as name of variable, region, bounding rectangles.
   * @param command XML node defining the operation.
   */
  private void printVerboseCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable toPrint;
    
    if(variableList.containsKey(command.getAttributeValue("variable")))
    {
      toPrint = getVariable(command.getAttributeValue("variable"));
      if(command.getAttributeValue("file") == null)
      {
        if(command.getAttributeValue("console") == null)
        { //print to default console
          try
          {
            //BufferedWriter out = new BufferedWriter(new PrintWriter(System.out));
            BufferedWriter out = new BufferedWriter(new ConsoleWriter(Console.getConsole("Default Out")));
            toPrint.printVerbose(out);
          } catch(IOException e)
          {
            log.log(Level.SEVERE, "IOException in -> printCommand (default console)");
          }
        } else
        { //print to specified console
          try
          {
            //BufferedWriter out = new BufferedWriter(new PrintWriter(System.out));
            BufferedWriter out = new BufferedWriter(new ConsoleWriter(Console.getConsole(command.getAttributeValue("console"))));
            toPrint.printVerbose(out);
          } catch(IOException e)
          {
            log.log(Level.SEVERE, "IOException in -> printCommand (console "+command.getAttributeValue("console")+")");
          }
        }
      } else
      { //print this to the specified file
        try
        {
          BufferedWriter out;
          if(writeFiles.contains(command.getAttributeValue("file")))
          {
            out = new BufferedWriter(new FileWriter(command
                .getAttributeValue("file"), true));
          } else
          {
            out = new BufferedWriter(new FileWriter(command
                .getAttributeValue("file"), false));
            writeFiles.add(command.getAttributeValue("file"));
          }

          toPrint.printVerbose(out);
        } catch(IOException e)
        {
          log.log(Level.SEVERE, "IOException in -> printCommand (file)");
        }
      }
    } else
    { //this variable did not exist
      log.log(Level.WARNING, "Variable: "+command.getAttributeValue("variable")+" is undefined.");
    }
  }
  /**
   * Displays a graphical plotting of this variable. Each point is color coded
   * based on it's value. Points outside the variable are displayed as white.
   * @param command XML node defining the operation.
   */
  private void plotCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable toPrint;
    
    if(variableList.containsKey(command.getAttributeValue("variable")))
    {
      toPrint = getVariable(command.getAttributeValue("variable"));
      if(toPrint.isReference())
      {
        if(toPrint.isGroup())
        {
          double min, max;
          MatrixGrapher graph;
          GroupVariable grp = (GroupVariable)toPrint;
          ReferenceVariable currPrint;
          Map.Entry me;
          
          Iterator it = grp.data.entrySet().iterator();
          while(it.hasNext())
          {
            me = (Map.Entry)it.next();
            currPrint = (ReferenceVariable)me.getValue();
            graph = new MatrixGrapher();
            
            min = (ComponentManipulator.smallestValue(currPrint.getData()))[0].data[0][0];
            max = (ComponentManipulator.largestValue(currPrint.getData()))[0].data[0][0];
            graph.drawMatrix(toPrint.name, currPrint.buildMatrix(), min, max, currPrint.x, currPrint.y, currPrint.res);
          }
        } else
        {
          ReferenceVariable ref = (ReferenceVariable)toPrint;
          double min, max;
          MatrixGrapher graph = new MatrixGrapher();
          
          min = (ComponentManipulator.smallestValue(ref.getData()))[0].data[0][0];
          max = (ComponentManipulator.largestValue(ref.getData()))[0].data[0][0];
          graph.drawMatrix(toPrint.name, ref.buildMatrix(), min, max, ref.x, ref.y, ref.res);
        }
      } else
      { //variable is just data
        log.log(Level.WARNING, "Variable: "+command.getAttributeValue("variable")+" does not contain reference information.");
      }
    } else
    { //this variable did not exist
      log.log(Level.WARNING, "Variable: "+command.getAttributeValue("variable")+" is undefined.");
    }
  }
  /**
   * Creates a new 'txt' file containing this variable's data. This will allow
   * a user to perform manipulations on data, then save that data and reread
   * it back into the preprocessor later so they can start working at the point
   * they left off. The resulting data set can be quickly added to old working
   * data by using the seed command in the preprocessor.
   * @param command XML node defining the operation.
   */
  private void createDataSetCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable var;
    ReferenceVariable varR;
    String varName;
    String hold;
    Element currInfo, currTag;
    
    currInfo = command.getChild("source");
    if(variableList.containsKey(currInfo.getAttributeValue("name")))
    {
      varName = currInfo.getAttributeValue("name");
      var = getVariable(varName);
      if((var.isReference())&&(!var.isGroup()))
      {//this exists and is a reference variable, now make me a file!
//getting information and printin to file
        varR = (ReferenceVariable)var;
        try
        {
          currInfo = command.getChild("file");
          String outFile = currInfo.getAttributeValue("name");
          BufferedWriter out = new BufferedWriter( new java.io.FileWriter(outFile));
          currInfo = command.getChild("tag");
          if((Boolean.valueOf(currInfo.getAttributeValue("value"))).booleanValue())
          { //write tags to the beginning of this file
            //writing the name of this data
            currTag = currInfo.getChild("fieldName");
            if(currTag == null)
            {
              out.write(varName+""); //using variable name as the data name
              out.newLine();
            } else
            { //using the passed value as the field name
              out.write(currTag.getAttributeValue("value")+"");
              out.newLine();
            }
            
            //writing the time these values exist at
            currTag = currInfo.getChild("time"); //they have to give a time, no if statement
            out.write(currTag.getAttributeValue("value")+"");
            out.newLine();	
            
            //writing the resolution of this data
            out.write(varR.res+"");
            out.newLine();
            
            //writing whether or not this data is averaged on addition
            out.write(varR.avg+"");
            out.newLine();
            
            //writing that values are output in decimal form
            out.write("decimal");
            out.newLine();
            
            //writing this data's units if they exist or noUnit if they dont
            if(dataUnits.containsKey(varName))
            {
              hold = (String)dataUnits.get(varName);
              
            } else
            {
              hold = "noUnit"; //have to print somethinb because of static reading in preprocess
            }
            out.write(hold+"");
            out.newLine();
            
            //writing this data's refrence if it exists or no reference if it does not
            if(dataRef.containsKey(varName))
            {
              hold = (String)dataRef.get(varName);
              
            } else
            {
              hold = "no reference"; //always reads the same fields, as with units
            }
            out.write(hold+"");
            out.newLine();
            
          } //done writing tags
          //writing the matrix of values
          double[][] toPrint = varR.buildMatrix();
          for(double iY = 90; iY > (-90); iY-=varR.res)
          {
            if(((iY >= varR.y+varR.res))&&(iY <= (varR.y+varR.h)))
            { //this whole line is inside the y bounds
              for(double iX = -180; iX < (180); iX+=varR.res)
              {
                if(((iX >= varR.x))&&(iX <= (varR.x+varR.w-varR.res)))
                { //this point is inside x bounds of var, print this location
                  if(Double.isNaN(toPrint[(int)(((varR.y+varR.h)-iY)/varR.res)][(int)((iX-varR.x)/varR.res)]))
                  { //we dont have a value for this point, just print 0
                    out.write("0.0 ");
                  } else
                  { //do have a value, print it!
                    out.write(toPrint[(int)(((varR.y+varR.h)-iY)/varR.res)][(int)((iX-varR.x)/varR.res)]+" ");
                  }
                } else
                { //out of x bounds, print 0
                  out.write("0.0 ");
                }
              }
            } else
            { //this whole line is out of the y bounds, just print 0's
              for(double iX = -180; iX < (180); iX+=varR.res)
              {
                out.write("0.0 ");
              }
            }
            out.newLine();
          }//done writing matrix
          
          
          out.flush();
          out.close();
        } catch(IOException e)
        {
          log.log(Level.SEVERE, "IOException in -> createDataSetCommand");
        }
//done making the file
      } else
      {
        log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("name")+" is not a reference variable.");
      }
    } else
    {
      log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("name")+" is undefined.");
    }
  }
  /**
   * Creates a new netCDF file containing the specified variable's data.
   * NetCDF files can then be opened in Panopoly or read back into the
   * data manipulation tool at a later date.
   * @param command XML node defining the operation.
   */
  private void createNetCDFFileCommand(Element command)
  {
	  log.log(Level.FINER, "begin function");
	  Variable var;
	  ReferenceVariable varR;
	  String fileName;
	  double res;
	  boolean missing_value_test = false;
	  double[][] myData;
	  float degHold;
	  float missingValue = Float.NaN;
	  Element currInfo;
	  NetcdfFileWriteable ncfile;
	  // constant for converting from milliseconds to days
	  final long MILLS_TO_DAY = 1000L * 60 * 60 * 24;
	  GregorianCalendar relativeDate = null;
	  GregorianCalendar currDate = null;
	  currInfo = command.getChild("use-days-from");
	  boolean useRelativeDate = false;
	  if(currInfo != null) {
		  // TODO: possibly could do more detailed than just year
		  relativeDate = new GregorianCalendar(Integer.parseInt(currInfo.getAttributeValue("from-year")), Calendar.JANUARY, 1);
		  useRelativeDate = true;
	  }

	  List sourceVars = command.getChildren("source");
	  //currInfo = command.getChild("source");
	  if(sourceVars.size() == 0) {
		  log.log(Level.WARNING, "No source vars, did not create netcdf file");
		  return;
	  }
	  // TODO: assuming time and dimensions are the same for all source vars
	  // create a set of years that will go into the netcdf
	  currInfo = (Element)sourceVars.get(0);
	  var = getVariable(currInfo.getAttributeValue("name"));
	  if(var.isReference())
	  {//this exists and is a reference variable, now make me a file!
		  List<Map.Entry<String, ReferenceVariable>> timeSet;
		  if(!var.isGroup()) {
			  varR = (ReferenceVariable)var;
			  // create a one entry map so that we can get it's time right
			  Map<String, ReferenceVariable> tempMap = new HashMap<String, ReferenceVariable>(1);
			  // TODO: get the real time associated with this ref var,
			  // we may need to store the time str within the ref var
			  tempMap.put("1", varR);
			  timeSet = new ArrayList(tempMap.entrySet());
		  } else if(var.isGroup() && ((GroupVariable)var).isTime) {
			  GroupVariable varG = (GroupVariable)var;
			  timeSet = new ArrayList((Set<Map.Entry<String, ReferenceVariable>>)varG.data.entrySet());
			  // use the first in group as ref for all since they should
			  // all have the same shape
			  varR = timeSet.get(0).getValue();
		  } else {
			  // don't know about this one
			  log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("name")+" is not grouped by time.");
			  return;
		  }
		  res = varR.res;
		  currInfo = command.getChild("file");
		  fileName = currInfo.getAttributeValue("name"); //storing netcdf file name
		  if(!fileName.endsWith(".nc"))
		  {
			  fileName += ".nc";
		  }

		  //**************************DEFINING FILE*********************************
		  ncfile = NetcdfFileWriteable.createNew(fileName, true);
		  //define dimensions
		  Dimension latDim = ncfile.addDimension("lat", (int)Math.round(180/res));
		  Dimension lonDim = ncfile.addDimension("lon", (int)Math.round(360/res));
		  Dimension timeDim = ncfile.addDimension("time", timeSet.size());

		  //define Variables
		  ArrayList<Dimension> dim3 = new ArrayList<Dimension>(3);
		  dim3.add(timeDim);
		  dim3.add(latDim);
		  dim3.add(lonDim);

		  //float lat(lat) ;
		  ncfile.addVariable("lat", DataType.FLOAT, new Dimension[] {latDim});
		  ncfile.addVariableAttribute("lat", "units", "degrees_north");
		  ncfile.addVariableAttribute("lat", "long_name", "Latitude");
		  ncfile.addVariableAttribute("lat", "comment", "center of cell");

		  //float lon(lon) ;
		  ncfile.addVariable("lon", DataType.FLOAT, new Dimension[] {lonDim});
		  ncfile.addVariableAttribute("lon", "units", "degrees_east");
		  ncfile.addVariableAttribute("lon", "long_name", "Longitdue");
		  ncfile.addVariableAttribute("lon", "comment", "center of cell");

		  //float time(time) ;
		  if(!useRelativeDate) {
			  ncfile.addVariable("time", DataType.INT, new Dimension[] {timeDim});
			  ncfile.addVariableAttribute("time", "units", "year");
			  ncfile.addVariableAttribute("time", "long_name", "Time");
		  } else {
			  ncfile.addVariable("time", DataType.DOUBLE, new Dimension[] {timeDim});
			  SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			  ncfile.addVariableAttribute("time", "units", "days since "+dateFormatter.format(relativeDate.getTime()));
			  ncfile.addVariableAttribute("time", "long_name", "Time");
			  ncfile.addVariableAttribute("time", "calendar", "gregorian");
		  }

		  // set up the dimensions for each soruce variable
		  for(Iterator source = sourceVars.iterator(); source.hasNext(); ) {
			  currInfo = (Element)source.next();
			  var = getVariable(currInfo.getAttributeValue("name"));
			  if(!var.isGroup()) {
				  varR = (ReferenceVariable)var;
			  } else if(var.isGroup() && ((GroupVariable)var).isTime) {
				  GroupVariable varG = (GroupVariable)var;
				  timeSet = new ArrayList((Set<Map.Entry<String, ReferenceVariable>>)varG.data.entrySet());
				  // use the first in group as ref for all since they should
				  // all have the same shape
				  varR = timeSet.get(0).getValue();
			  } else {
				  // don't know about this one
				  log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("name")+" is not grouped by time.");
				  return;
			  }

			  //double data(time, lat, lon)
			  ncfile.addVariable(var.name, DataType.FLOAT, dim3);
			  ncfile.addVariableAttribute(var.name, "average", String.valueOf(varR.avg));

			  // Add additional metadata if present
			  if(varR.units != null) {
				  ncfile.addVariableAttribute(var.name, "units", varR.units);
			  } else {
				  if(  !(currInfo == null) && !(currInfo.getAttributeValue( "units" ) == null) )
				  {
					  ncfile.addVariableAttribute( var.name,"units", currInfo.getAttributeValue("units") );
				  }
			  }

			  if(varR.reference != null) {
				  ncfile.addVariableAttribute(var.name, "reference", varR.reference);
			  } else {
				  if(  !(currInfo == null) && !(currInfo.getAttributeValue( "reference" ) == null) )
				  {
					  ncfile.addVariableAttribute( var.name,"reference", currInfo.getAttributeValue("reference") );
				  }
			  }

			  if(  !(currInfo == null) && !(currInfo.getAttributeValue( "missing_value" ) == null) )
			  {
				  missingValue = Float.parseFloat( currInfo.getAttributeValue("missing_value") );
			  } else {
				  missingValue = Float.NaN; // default value
			  }
			  ncfile.addVariableAttribute(var.name, "_FillValue", missingValue);

			  // Overwrite missing value in netCDF file to test if actual value was written. 
			  // This is useful since Panoply (as of Feb 2009) display the missing_value as NaN no matter what.
			  if(  !(currInfo == null) && !(currInfo.getAttributeValue( "missing_value_test" ) == null) )
			  {
				  ncfile.addVariableAttribute( var.name,"_FillValue", currInfo.getAttributeValue("missing_value_test") );
			  }

			  if(  !(currInfo == null) && !(currInfo.getAttributeValue( "longName" ) == null) )
			  {
				  ncfile.addVariableAttribute( var.name,"long_name", currInfo.getAttributeValue("longName") );
			  }

			  if( !(currInfo == null) && !(currInfo.getAttributeValue( "title" ) == null) ) 
			  {
				  ncfile.addVariableAttribute( var.name,"title", currInfo.getAttributeValue("title") );
			  }

			  if(var.comment != null) {
				  ncfile.addGlobalAttribute("comments", var.comment);
			  }  else {
				  if(  !(currInfo == null) && !(currInfo.getAttributeValue( "comments" ) == null) )
				  {
					  ncfile.addVariableAttribute( var.name,"comments", currInfo.getAttributeValue("comments") );
				  }
			  }
		  }

		  //create the file
		  try {
			  ncfile.create();
		  }  catch (IOException e) {
			  log.log(Level.SEVERE, "NetCDF file '"+fileName+"' failed to create -> aborting");
			  System.exit(0);
		  }
		  log.log(Level.FINE, "Creating NetCDF file '"+fileName+"'");

		  ArrayFloat latArr = new ArrayFloat.D1(latDim.getLength());
		  ArrayFloat lonArr = new ArrayFloat.D1(lonDim.getLength());
		  ArrayInt timeArr = new ArrayInt.D1(timeDim.getLength());
		  // add the actual data for each source variable
		  for(Iterator source = sourceVars.iterator(); source.hasNext(); ) {
			  ArrayFloat dataArr = new ArrayFloat.D3(timeDim.getLength(), latDim.getLength(), lonDim.getLength());
			  currInfo = (Element)source.next();
			  var = getVariable(currInfo.getAttributeValue("name"));
			  if(!var.isGroup()) {
				  varR = (ReferenceVariable)var;
				  // create a one entry map so that we can get it's time right
				  Map<String, ReferenceVariable> tempMap = new HashMap<String, ReferenceVariable>(1);
				  // TODO: get the real time associated with this ref var,
				  // we may need to store the time str within the ref var
				  tempMap.put("1", varR);
				  timeSet = new ArrayList(tempMap.entrySet());
			  } else if(var.isGroup() && ((GroupVariable)var).isTime) {
				  GroupVariable varG = (GroupVariable)var;
				  timeSet = new ArrayList((Set<Map.Entry<String, ReferenceVariable>>)varG.data.entrySet());
				  // use the first in group as ref for all since they should
				  // all have the same shape
				  varR = timeSet.get(0).getValue();
			  } else {
				  // don't know about this one
				  log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("name")+" is not grouped by time.");
				  return;
			  }

			  //filling the array with the data
			  Index ima = dataArr.getIndex();
			  for(int t = 0; t < timeDim.getLength(); t++) {
				  myData = timeSet.get(t).getValue().buildWorldMatrix();
				  for(int i = 0; i < latDim.getLength(); i++)
				  {
					  for(int j = 0; j < lonDim.getLength(); j++)
					  {
						  // we want to flip the y so that (0,0) in the matrix
						  // is the lower left i.e. (-90, -180)
						  double currVal =  myData[(latDim.getLength() - i) - 1][j];
						  if ( Double.isNaN(currVal) ) {
							  dataArr.setFloat( ima.set(t,i,j), missingValue );
						  } else {
							  dataArr.setFloat( ima.set(t,i,j), (float)currVal );
						  }
					  }
				  }
				  myData = null;
				  // TODO: this is arbitrary and not sure if it is even necesary
				  // only garbage collect every 25th time
				  if(t % 25 == 0) {
					  System.gc();
				  }
			  }
			  try {
				  ncfile.write(var.name, dataArr);
				  ncfile.flush();
			  } catch (IOException e) {
				  log.log(Level.SEVERE, "NetCDF file failed to write data values -> "+e);
				  e.printStackTrace();
				  System.exit(0);
			  } catch (InvalidRangeException e) {
				  log.log(Level.SEVERE, "NetCDF file failed to write data values -> "+e);
				  e.printStackTrace();
				  System.exit(0);
			  }
		  }
		  //filling array with latitude degrees
		  Index iml = latArr.getIndex();
		  // want the index to be in the middle of the grid cell
		  // so we shift half a res down from the top
		  // also we are starting at the lower left so start
		  // degHold at -90 and move up
		  degHold = (float)(-90.0 + (res / 2));
		  for(int i = 0; i < latDim.getLength(); i++)
		  {
			  latArr.setFloat(iml.set(i), degHold);
			  degHold += res;
		  }
		  //filling array with longitude degrees
		  iml = lonArr.getIndex();
		  // want the index to be in the middle of the grid cell
		  // so we shift half a res over from the left
		  degHold = (float)(-180.0 + (res / 2));
		  for(int i = 0; i < lonDim.getLength(); i++)
		  {
			  lonArr.setFloat(iml.set(i), degHold);
			  degHold += res;
		  }
		  iml = timeArr.getIndex();
		  for(int t = 0; t < timeDim.getLength(); ++t) {
			  if(!useRelativeDate) {
				  timeArr.setInt(iml.set(t), (int)Double.parseDouble(timeSet.get(t).getKey()));
			  } else {
				  currDate = new GregorianCalendar((int)Double.parseDouble(timeSet.get(t).getKey()), Calendar.JANUARY, 1);
				  timeArr.setDouble(iml.set(t), ((currDate.getTimeInMillis() - relativeDate.getTimeInMillis()) / MILLS_TO_DAY));
			  }
		  }


		  // write data out to disk
		  try {
			  ncfile.write("lat", latArr);
			  ncfile.flush();
			  ncfile.write("lon", lonArr);
			  ncfile.flush();
			  ncfile.write("time", timeArr);
			  ncfile.flush();
		  } catch (IOException e) {
			  log.log(Level.SEVERE, "NetCDF file failed to write data values -> "+e);
			  e.printStackTrace();
			  System.exit(0);
		  } catch (InvalidRangeException e) {
			  log.log(Level.SEVERE, "NetCDF file failed to write data values -> "+e);
			  e.printStackTrace();
			  System.exit(0);
		  }

		  //all done
		  try {
			  ncfile.close();
		  } catch (IOException e) {
			  log.log(Level.WARNING, "NetCDF file failed to close -> continuing");
		  }



		  //***************************END FILE DEF*********************************
	  } else
	  {
		  log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("name")+" is not a reference variable.");
	  }

  }

  /**
   * Appends a variable to a NetCDF file. TODO: there is no error checking to make sure
   * the existing lat/lon/time dimensions make sense for this new variable.  Also it is
   * unclear how much of a performance hit this operation will take.
   * @param command XML node defining the operation.
   */
  private void appendNetCDFFileCommand(Element command)
  {
	  if(1==1) {
		  throw new UnsupportedOperationException("Need to upgrade netcdf library first");
	  }
    log.log(Level.FINER, "begin function");
    Variable var;
    ReferenceVariable varR;
    String fileName;
    double res;
    boolean missing_value_test = false;
    double[][] myData;
    float degHold;
    float missingValue = Float.NaN;
    Element currInfo;
    NetcdfFileWriteable ncfile = null;

    currInfo = command.getChild("source");
    var = getVariable(currInfo.getAttributeValue("name"));
    // need to figure out what type of var we have
    // TODO: currently only a single reference variable is supported
    if(var.isReference() && !var.isGroup()) {
	    varR = (ReferenceVariable)var;
    } else {
	    log.log(Level.WARNING, "Only reference vars are currently supported for append");
	    return;
    }
    res = varR.res;

    currInfo = command.getChild("file");
    fileName = currInfo.getAttributeValue("name"); //storing netcdf file name
    if(!fileName.endsWith(".nc"))
    {
	    fileName += ".nc";
    }

    File tmpFile = null;
    try {
	    //ncfile = NetcdfFileWriteable.openExisting(fileName, false);
	    File prevFile = new File(fileName);
	    tmpFile = new File(fileName+".tmp");
	    if (tmpFile.exists()) tmpFile.delete();
	    if (!prevFile.renameTo(tmpFile)) {
		    System.out.println("prevFile.exists "+prevFile.exists()+" canRead = "+ prevFile.canRead());
		    System.out.println("tmpFile.exists "+tmpFile.exists()+" canWrite "+ tmpFile.canWrite());
		    throw new RuntimeException("Cant rename "+prevFile.getAbsolutePath()+" to "+ tmpFile.getAbsolutePath());
	    }

	    NetcdfFile oldFile = NetcdfFile.open(tmpFile.getPath());
	    ucar.nc2.FileWriter fw = new ucar.nc2.FileWriter(fileName, true);
	    List<ucar.nc2.Variable> oldOnes = oldFile.getVariables();
	    fw.writeVariables(oldOnes);
	    ncfile = null; //TODO: when resupport this: fw.getNetcdf();

	    // TODO: the following would be ideal method for appending to netcdf which are available in NetCDF4
	    // however I had trouble with NetCDF4 seemingly not working correctly.  In particluar FileWriter.copyVarData
	    // didn't seem to work at all.  Also trying a basic test of copying a NetCDF file with 
	    // FileWriter.writeToFile(NetcdfFile fileIn, String fileOutName) did not even work.
	    /*
	       if(ncfile.setRedefineMode(true)) {
	       log.log(Level.FINER, "Will need to rewrite the entire file -- initial");
	       }
	       */

	    // find the existing dimensions so we can use them for the new var
	    // WARNING: we are expecting the dimensions names to be the data manipulator names
	    ArrayList<Dimension> dim4 = new ArrayList<Dimension>(4);
	    for(int dimIndex = 0; dimIndex < 4; ++dimIndex) {
		    String currDimName = null;
		    Dimension currDim;
		    switch(dimIndex) {
			    case 0:
				    currDimName = "level";
				    break;
			    case 1:
				    currDimName = "time";
				    break;
			    case 2:
				    currDimName = "lat";
				    break;
			    case 3:
				    currDimName = "lon";
				    break;
		    }
		    currDim = oldFile.findDimension(currDimName);
		    if(currDim == null) {
			    log.log(Level.WARNING, "Could not find dimension "+currDimName+" in NetCDF File "+fileName+" while appending");
			    return;
		    }
		    dim4.add(currDim);
	    }

	    // add the new var with the existing dimensions
	    // TODO: we need some error check to make sure the existing dimensions make sense
	    // TODO: add when resupported: ncfile.addVariable(var.name, DataType.FLOAT, "level time lat lon");
	    //ncfile.addVariable(var.name, DataType.FLOAT, dim4);
	    ncfile.addVariableAttribute(var.name, "average", String.valueOf(varR.avg));

	    // Add additional metadata if present
	    if(varR.units != null) {
		    ncfile.addVariableAttribute(var.name, "units", varR.units);
	    } else {
		    currInfo = command.getChild("units");
		    if(  !(currInfo == null) && !(currInfo.getAttributeValue( "value" ) == null) )
		    {
			    ncfile.addVariableAttribute( var.name,"units", currInfo.getAttributeValue("value") );
		    }
	    }

	    if(varR.reference != null) {
		    ncfile.addVariableAttribute(var.name, "reference", varR.reference);
	    } else {
		    currInfo = command.getChild("reference");
		    if(  !(currInfo == null) && !(currInfo.getAttributeValue( "value" ) == null) )
		    {
			    ncfile.addVariableAttribute( var.name,"reference", currInfo.getAttributeValue("value") );
		    }
	    }

	    currInfo = command.getChild("missing_value");
	    if(  !(currInfo == null) && !(currInfo.getAttributeValue( "value" ) == null) )
	    {
		    missingValue = Float.parseFloat( currInfo.getAttributeValue("value") );
	    } else {
		    missingValue = Float.NaN; // default value
	    }
	    ncfile.addVariableAttribute(var.name, "missing_value", missingValue);

	    // Overwrite missing value in netCDF file to test if actual value was written. 
	    // This is useful since Panoply (as of Feb 2009) display the missing_value as NaN no matter what.
	    currInfo = command.getChild("missing_value_test");
	    if(  !(currInfo == null) && !(currInfo.getAttributeValue( "value" ) == null) )
	    {
		    ncfile.addVariableAttribute( var.name,"missing_value", currInfo.getAttributeValue("value") );
	    }

	    currInfo = command.getChild("longName");
	    if(  !(currInfo == null) && !(currInfo.getAttributeValue( "value" ) == null) )
	    {
		    ncfile.addVariableAttribute( var.name,"long_name", currInfo.getAttributeValue("value") );
	    }

	    currInfo = command.getChild("title");
	    if( !(currInfo == null) && !(currInfo.getAttributeValue( "value" ) == null) ) 
	    {
		    ncfile.addVariableAttribute( var.name,"title", currInfo.getAttributeValue("value") );
	    }

	    if(var.comment != null) {
		    ncfile.addGlobalAttribute("comments", var.comment);
	    }  else {
		    currInfo = command.getChild("comments");
		    if(  !(currInfo == null) && !(currInfo.getAttributeValue( "value" ) == null) )
		    {
			    ncfile.addVariableAttribute( var.name,"comments", currInfo.getAttributeValue("value") );
		    }
	    }

	    /* part of the methodology what should work for NetCDF4 see the note above
	       if(ncfile.setRedefineMode(false)) {
	       log.log(Level.FINER, "Will need to rewrite the entire file -- close");
	       }
	       */

	    // 0 -- Level, 1 -- Time, 2 -- latitude, 3 -- longitude
	    ArrayFloat dataArr = new ArrayFloat.D4(dim4.get(0).getLength(), dim4.get(1).getLength(), dim4.get(2).getLength(), dim4.get(3).getLength());

	    //filling the array with the data
	    Index ima = dataArr.getIndex();
	    myData = varR.buildWorldMatrix();
	    // iterate over the lat index
	    for(int i = 0; i < dim4.get(2).getLength(); i++)
	    {
		    // iterate of the the lon index
		    for(int j = 0; j < dim4.get(3).getLength(); j++)
		    {
			    if ( Double.isNaN( myData[i][j] ) ) {
				    dataArr.setFloat( ima.set(0,0,i,j), missingValue );
			    } else {
				    dataArr.setFloat( ima.set(0,0,i,j), (float)myData[i][j] );
			    }
		    }
	    }
	    myData = null;


	    fw.finish();
	    ncfile = null;
	    ncfile = NetcdfFileWriteable.openExisting(fileName, true);
	    ncfile.write(var.name, dataArr);

    } catch(IOException ioe) {
	    log.log(Level.WARNING, "Error while appending to NetCDF file: "+ioe);
	    ioe.printStackTrace();
    } catch(InvalidRangeException ire) {
	    log.log(Level.WARNING, "Invalid range when adding "+var.name+": "+ire);
    } finally {
	    try {
		    if (tmpFile != null && tmpFile.exists()) tmpFile.delete();
		    if(ncfile != null) {
			    ncfile.close();
		    }
	    } catch(IOException ioe) {
		    // ignore
	    }
    }
  }

  /**
   * Adds a comment to a variable which will be outout to netCDF or printed with
   * that variable if the verbose option is selected.
   * @param command XML node defining the operation.
   */
  private void setComment(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable var;
    Element currInfo;
    
    currInfo = command.getChild("variable");
    if(variableList.containsKey(currInfo.getAttributeValue("value")))
    {
      var = getVariable(currInfo.getAttributeValue("value"));
      currInfo = command.getChild("text");
      var.comment = currInfo.getAttributeValue("value");
    } else
    {
      log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("value")+" is undefined.");
    }

  }
  /**
   * Adds a reference to a variable which will be outout to netCDF or printed with
   * printVerbose command.
   * @param command XML node defining the operation.
   */
  private void setReferenceCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable var;
    Element currInfo;
    
    currInfo = command.getChild("variable");
    if(variableList.containsKey(currInfo.getAttributeValue("value")))
    {
      var = getVariable(currInfo.getAttributeValue("value"));
      currInfo = command.getChild("text");
      if((var.isReference())&&(!var.isGroup()))
      {
        ((ReferenceVariable)var).reference = currInfo.getAttributeValue("value");
      } else
      {
        log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("value")+" is not a reference variable.");
      }
    } else
    {
      log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("value")+" is undefined.");
    }

  }
  /**
   * Adds a units descriptor to the variable which will be printed with the
   * printVerbose command.
   * @param command XML node defining the operation.
   */
  private void setUnitsCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    Variable var;
    Element currInfo;
    
    currInfo = command.getChild("variable");
    if(variableList.containsKey(currInfo.getAttributeValue("value")))
    {
      var = getVariable(currInfo.getAttributeValue("value"));
      currInfo = command.getChild("text");
      if((var.isReference())&&(!var.isGroup()))
      {
        ((ReferenceVariable)var).units = currInfo.getAttributeValue("value");
      } else
      {
        log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("value")+" is not a reference variable.");
      }
    } else
    {
      log.log(Level.WARNING, "Variable: "+currInfo.getAttributeValue("value")+" is undefined.");
    }

  }
  /**
   * Runs a set of commands on subregions which are extracted from the given variable.
   * Subregions are defined on a specific level according to the regionDef.
   * @param command XML node defining the operation.
   */
  private void forEachSubregionCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    
    Element currInfo, currReplace;
    Element addElem;
    Element originalCom, copyCom, extCom;
    List<String> regionNames = new ArrayList<String>();
    List<Element> replaceElements = new ArrayList<Element>();
    Iterator attIt;
    String currName;
    
    //list of regions
    regionNames.add("USA");
    regionNames.add("Canada");
    regionNames.add("OECD90 Europe");
    regionNames.add("Japan");
    regionNames.add("Aus and NZ");
    regionNames.add("FSU");
    regionNames.add("China/CPA");
    regionNames.add("Middle East");
    regionNames.add("Africa");
    regionNames.add("Latin America");
    regionNames.add("S and E Asia");
    regionNames.add("Eastern Europe");
    regionNames.add("Korea");
    regionNames.add("India");
    
    //original command
    currInfo = command.getChild("command");
    originalCom = (Element)(currInfo.getChildren().get(0)); //getting the first (and only child)
    // why copy?
    copyCom = (Element)originalCom.clone();
    List comms = currInfo.getChildren();
    
    //get all tags which need extraction
    for(int i = 0; i < comms.size(); ++i) {
	    copyCom = (Element)comms.get(i);
	    attIt = copyCom.getDescendants(new ContentFilter(ContentFilter.ELEMENT));
	    while(attIt.hasNext())
	    {
		    currInfo = (Element)attIt.next();
		    currName = currInfo.getAttributeValue("replace");
		    if((currName != null))
		    {
			    replaceElements.add(currInfo);
		    }
	    }
    }
    
    //for each region
    for(int r = 0; r < regionNames.size(); r++)
    {
      //set all name tags to extractions
      for(int e = 0; e < replaceElements.size(); e++)
      {
        currReplace = replaceElements.get(e);
        currName = currReplace.getAttributeValue("replace");
        
        //extract region from each tag
        extCom = new Element("extractSubRegion");
        addElem = new Element("target");
        addElem.setAttribute("name", currName+regionNames.get(r));
        extCom.addContent(addElem);
        addElem = new Element("source");
        addElem.setAttribute("name", currName);
        extCom.addContent(addElem);
        addElem = new Element("shape");
        addElem.setAttribute("value", regionNames.get(r));
        extCom.addContent(addElem);
        
        //run extract command
        runCommand(extCom);
        
        //add new name tag
        currReplace.setAttribute("name", currName+regionNames.get(r));
      }
      //run created command
      for(int i = 0; i < comms.size(); ++i) {
	      copyCom = (Element)comms.get(i);
	      runCommand(copyCom);
      }
      //removing all the extract variables
      for(int e = 0; e < replaceElements.size(); e++)
      {
        currReplace = replaceElements.get(e);
        currName = currReplace.getAttributeValue("name");
        
        //delete variable with this name
        variableList.remove(currName);
      }
    }
  }
  /**
   * Takes two lists of regions or productions and combines them into a
   * table of values. One list is defined as regions and one as the content
   * of those regions for the purpose of possibly averaging content, as
   * opposed to regularly just add land area in each combination.
   * @param command XML node defining the operation.
   */
  private void zoneCombineCommand(Element command)
  {
    log.log(Level.FINER, "begin function");
    BufferedWriter out = new BufferedWriter(new ConsoleWriter(Console.getConsole("Default Out")));
    List areas, contents;
    Element currA, currC;
    Element currInfo;
    Variable varArea, varContent, varMask;
    double sum, total = 0;
    boolean avg;
    
    currInfo = command.getChild("areas");
    areas = currInfo.getChildren();
    currInfo = command.getChild("content");
    contents = currInfo.getChildren();
    currInfo = command.getChild("average");
    avg = (Boolean.valueOf(currInfo.getAttributeValue("value"))).booleanValue();
    
    varMask = null;
    
    try
    {
      out.write("Combine Command Output");
      out.newLine();
      out.write("\t");
      out.flush();
      for(int c = 0; c < contents.size(); c++)
      {
        currC = (Element)contents.get(c);
        out.write(currC.getAttributeValue("name")+" ");
      }
      out.newLine();
      out.flush();
      
      for(int a = 0; a < areas.size(); a++)
      {
        currA = (Element)areas.get(a);
        if(!variableList.containsKey(currA.getAttributeValue("name")))
        {
          out.write(currA.getAttributeValue("name")+" did not exist");
          out.newLine();
          return;
        }
        varArea = getVariable(currA.getAttributeValue("name"));
        out.write(currA.getAttributeValue("name")+" ");
        
        if(a == 0)
        { //need these set at some point
          varMask = varArea.getShape("holdMask");
          total = ComponentManipulator.sumRegionArea(varArea.getData())[0].data[0][0];
        }
        
        for(int c = 0; c < contents.size(); c++)
        {
          currC = (Element)contents.get(c);
          if(!variableList.containsKey(currC.getAttributeValue("name")))
          {
            out.write(currC.getAttributeValue("name")+" did not exist");
            out.newLine();
            return;
          }
          varContent = getVariable(currC.getAttributeValue("name"));
          
          //maskRemain weighted
          varMask.setData(ComponentManipulator.maskRemainWeight(varContent.getData(), varArea.getData(), 0));
          
          //sumarea on remainder
          sum = ComponentManipulator.sumArea(varMask.getData())[0].data[0][0];
          
          if(avg)
          {
            sum /= total;
          }
          
          out.write(sum+" ");
        }
        out.newLine();
        out.flush();
      }
      out.newLine();
      out.close();
    } catch(IOException e)
    {
      log.log(Level.SEVERE, "IOException in -> zoneCombineCommand on write");
    }
  }

  /**
   * Runs a command on subregions which are extracted from the given variable.
   * Subregions are defined on a specific level according to the regionDef.
   * @param command XML node defining the operation.
   */
  private void forEachCommand(Element command)
  {
	  log.log(Level.FINER, "begin function");
	  final String currVarName = "$_";
	  final String replaceRegionStr = "$Current_Region_Name";
	  Variable VEachPriv; 
	  if(variableList.containsKey(currVarName)) {
		  VEachPriv = getVariable(currVarName);
	  } else {
		  VEachPriv = null;
	  }
	  Variable VSource;
	  Variable VCurr;
	  Element currInfo;

	  VSource = getVariable(command.getAttributeValue("name"));
	  currInfo = command.getChild("command");
	  List comms = currInfo.getChildren();
	  currInfo = null;
	  if(VSource.isGroup())
	  {
		  Map.Entry me;
		  Iterator it = ((GroupVariable)VSource).data.entrySet().iterator();

		  while(it.hasNext())
		  {
			  me = (Map.Entry)it.next();
			  VCurr = (Variable)me.getValue();
			  variableList.put(currVarName, VCurr);
			  for(int i = 0; i < comms.size(); ++i) {
				  // need to check if we have a extractSubRegion in which case we may need to 
				  // do a replace to tell it what the current region we are processing is
				  // this allows us to do joins between different variables in a forEach
				  if(((Element)comms.get(i)).getName().equals("extractSubRegion")) {
					  currInfo = ((Element)comms.get(i)).getChild("shape");
					  if(currInfo.getAttributeValue("value").equals(replaceRegionStr)) {
						  currInfo.setAttribute("value", VCurr.name);
					  } else {
						  currInfo = null;
					  }
				  }
				  runCommand((Element)comms.get(i));
				  // if we made a replace we should set it back so we can do it
				  // again for the next region
				  if(currInfo != null) {
					  currInfo.setAttribute("value", replaceRegionStr);
					  currInfo = null;
				  }
			  }
			  // set it back into the group in case it changed
			  me.setValue(variableList.get(currVarName));
		  }
	  } else 
	  {
		  variableList.put(currVarName, VSource);
		  for(int i = 0; i < comms.size(); ++i) {
			  runCommand((Element)comms.get(i));
		  }
	  }
	  if(VEachPriv != null) {
		  variableList.put(currVarName, VEachPriv);
	  } else {
		  variableList.remove(currVarName);
	  }
  }

//*****************************************************************************
//*******************Component Functions***************************************
//*****************************************************************************
  /**
   * Special reading function which gets all supplementary field information.
   * This includes whether a field is averaged or added on aggregation, the
   * reference for the field, and the units of the field.
   * @param elem XML element which contains additional data for all fields.
   */
  private void setFieldInfo(Element elem)
  {
    log.log(Level.FINER, "begin function");
    String dataName;
    boolean avg;
    String ref;
    String units;
    Element currElem;
    Element currInfo;
    List fields = elem.getChildren("variable");
    
    for(int i = 0; i < fields.size(); i++)
    {
      currElem = (Element)fields.get(i);
      dataName = currElem.getAttributeValue("name");
      
      currInfo = currElem.getChild("average");
      if(currInfo == null)
      {
        log.log(Level.SEVERE, "No average element found in "+dataName+" field element.");
      } else
      {
        avg = (Boolean.valueOf(currInfo.getAttributeValue("value"))).booleanValue();
        dataAvgAdd.put(dataName, new Boolean(avg));
      }
      
      
      currInfo = currElem.getChild("reference");
      if(currInfo != null)
      {
        ref = currInfo.getAttributeValue("value");
        dataRef.put(dataName, ref);
      }
      
      currInfo = currElem.getChild("units");
      if(currInfo != null)
      {
        units = currInfo.getAttributeValue("value");
        dataUnits.put(dataName, units);
      }
    }
  }
  
  /**
   * Creates a new region and fills it with data based on the passed XML element.
   * The created region is defined as a sub-region, a region on the lowest lvl.
   * This means that it contains only data and no other regions. Data in
   * the xml element is defined in a tree structure of variables->times->data.
   * @param currRegion XML element which contains a region worth of data.
   */
  private void addRegion(Element currRegion)
  {
    log.log(Level.FINER, "begin function");
    //creating a new region from the passed element and adding it to the master list
    
    int sizeX, sizeY, currX, currY;
    boolean avg;
    TreeMap toAddVar;
    double[][] toAddTime;
    String varName, timeName;
    List varList, timeList, dataList;
    Element currVar, currTime, currData;
    
    subRegion toAdd = new subRegion();
    toAdd.name = currRegion.getAttributeValue("name");
    toAdd.resolution = resolution;
    toAdd.x = Double.parseDouble(currRegion.getAttributeValue("x"));
    toAdd.y = Double.parseDouble(currRegion.getAttributeValue("y"));
    sizeX = Integer.parseInt(currRegion.getAttributeValue("sizeX"));
    sizeY = Integer.parseInt(currRegion.getAttributeValue("sizeY"));
    toAdd.width = (sizeX*resolution);
    toAdd.height = (sizeY*resolution);
    
  //special case to get the weight variable
    currVar = currRegion.getChild("weight");
    varName = "weight";
    toAddVar = new TreeMap();
    //begin getting times loop
    timeList = currVar.getChildren("time");
    for(int j = 0; j<timeList.size(); j++)
    {
      currTime = (Element)timeList.get(j);
      timeName = currTime.getAttributeValue("value");
      toAddTime = new double[sizeY][sizeX];
      for(int hy = 0; hy<sizeY; hy++)
      {
        for(int hx = 0; hx<sizeX; hx++)
        {
          toAddTime[hy][hx] = 0;
        }
      }
      //begin getting data loop
      dataList = currTime.getChildren("data");
      for(int k = 0; k<dataList.size(); k++)
      {
        currData = (Element)dataList.get(k);
        currX = Integer.parseInt(currData.getAttributeValue("x"));
        currY = Integer.parseInt(currData.getAttributeValue("y"));
        toAddTime[currY][currX] = stringToDouble(currData.getAttributeValue("value"));
      }
      //end getting data
      toAddVar.put(timeName, toAddTime);
    }
    //end getting time
    toAdd.data.put(varName, toAddVar);
  //end getting weight
    
    //begin getting variables loop
    varList = currRegion.getChildren("variable");
    for(int i = 0; i < varList.size(); i++)
    {
      currVar = (Element)varList.get(i);
      varName = currVar.getAttributeValue("value");
      avg = ((Boolean)dataAvgAdd.get(varName)).booleanValue();
      toAddVar = new TreeMap();
      //begin getting times loop
      timeList = currVar.getChildren("time");
      for(int j = 0; j<timeList.size(); j++)
      {
        currTime = (Element)timeList.get(j);
        timeName = currTime.getAttributeValue("value");
        toAddTime = new double[sizeY][sizeX];
        for(int hy = 0; hy<sizeY; hy++)
        {
          for(int hx = 0; hx<sizeX; hx++)
          {
            toAddTime[hy][hx] = Double.NaN;
          }
        }
        //begin getting data loop
        dataList = currTime.getChildren("data");
        for(int k = 0; k<dataList.size(); k++)
        {
          currData = (Element)dataList.get(k);
          currX = Integer.parseInt(currData.getAttributeValue("x"));
          currY = Integer.parseInt(currData.getAttributeValue("y"));
          toAddTime[currY][currX] = stringToDouble(currData.getAttributeValue("value"));
          if(!avg)
          { //this is an additive value and should be initially weighted (now)
            toAddTime[currY][currX] *= ((double[][])((TreeMap)toAdd.data.get("weight")).get("0"))[currY][currX];
          }
        }
        //end getting data
        toAddVar.put(timeName, toAddTime);
      }
      //end getting time
      //System.out.println("just added -> '"+varName+"'");
      toAdd.data.put(varName, toAddVar);
    }
    //end getting varialbes
    
    //adding region to master list
    regionList.put(toAdd.name, toAdd);
  }

  /**
   * Creates a new region based on a list of contained regions which have been
   * previously defined. The contained regions must all be of lower level,
   * where level is defined as one more than the greatest level of a contained
   * region, or as 0 for subregions. The created region will store data in the
   * form it gets it in, as a collection of matrices one for each contained 
   * sub-region.
   * @param currRegion XML element which contains a list of contained regions.
   * @param internalRegionIDCount A count to assign unique region IDs to all
   * 				  created regions.
   * @return The current region ID count
   */
  private int addSuperRegion(Element currRegion, int internalRegionIDCount)
  {
    log.log(Level.FINER, "begin function");
    List subRegions;
    Region sub;
    
    superRegion toAdd = new superRegion();
    
    toAdd.name = currRegion.getAttributeValue("name");
    toAdd.resolution = resolution;
    toAdd.level = Integer.parseInt(currRegion.getAttributeValue("level"));
    toAdd.regionID = ++internalRegionIDCount;
    
    //begin loop to add subregions to this superregion
    subRegions = currRegion.getChildren("region");
    for(int i = 0; i < subRegions.size(); i++)
    {
      if(regionList.containsKey(((Element)subRegions.get(i)).getAttributeValue("name")))
      {
        sub = (Region)regionList.get(((Element)subRegions.get(i)).getAttributeValue("name"));
        toAdd.data.add(sub);
        toAdd.numSub += sub.numSub;
	if(sub.regionID == -1) {
		sub.regionID = ++internalRegionIDCount;
	}
	// WARNING: this implies the last super region to contain this region will be called
	// it's parent
	sub.parentRegion = toAdd;
        //updating superregions bounds if necessary
        if(toAdd.height==-1)
        { //this is the first block being added to this region nothing to
          // test against yet
          toAdd.y = sub.y;
          toAdd.x = sub.x;
          toAdd.height = sub.height;
          toAdd.width = sub.width;
        } else
        { //test against old bounds, if outside them, change them
          if(sub.y < toAdd.y)
          {
            toAdd.height += (toAdd.y-sub.y);
            toAdd.y = sub.y;
          }
          if((sub.y+sub.height)>(toAdd.y+toAdd.height))
          {
            toAdd.height = ((sub.y+sub.height)-toAdd.y);
          }
          if(sub.x<toAdd.x)
          {
            toAdd.width += (toAdd.x-sub.x);
            toAdd.x = sub.x;
          }
          if((sub.x+sub.width)>(toAdd.x+toAdd.width))
          {
            toAdd.width = ((sub.x+sub.width)-toAdd.x);
          }
        }
      } else
      {
        log.log(Level.INFO, toAdd.name+" -> "+((Element)subRegions.get(i)).getAttributeValue("name")+" : tried to add but region WAS NOT FOUND");
      }
    }
    //end adding regions
    
    regionList.put(toAdd.name, toAdd);
    return internalRegionIDCount;
  }

  /**
   * Command which takes a reference variable and divides
   * it by its weights.  This would be used when a variable has
   * been double weighted.
   * @see ComponentManipulator.adjustWeights
   * @param command XML element describing which variable to divide by weights and
   * 	where to store it.
   */
  private void divByAreaFractCommand(Element command) {
	  log.log(Level.FINER, "begin function");
	  Variable VDest;
	  Variable VSource;
	  Element currInfo;

	  currInfo = command.getChild("target");
	  String VDname = currInfo.getAttributeValue("name");
	  currInfo = command.getChild("argument");
	  VSource = getVariable(currInfo.getAttributeValue("name"));
	   if((VSource.isReference())) {
		  VDest = VSource.getShape(VDname);
		  variableList.put(VDname, VDest);
		  VDest.setData(ComponentManipulator.adjustWeights(VSource.getData(), VSource.getWeight(), false));
		  // should I do anything with avg?
	  } else {
		  log.log(Level.WARNING, "Only averaged reference variables may be divided by area fraction.");
	  }
  }

  /**
   * Command which takes a reference variable and multiplies
   * it by its weights.  This could be used to essentailly
   * change a variable to averaged.
   * @see ComponentManipulator.adjustWeights
   * @param command XML element describing which variable to divide by weights and
   * 	where to store it.
   */
  private void multByAreaFractCommand(Element command) {
	  log.log(Level.FINER, "begin function");
	  Variable VDest;
	  Variable VSource;
	  Element currInfo;

	  currInfo = command.getChild("target");
	  String VDname = currInfo.getAttributeValue("name");
	  currInfo = command.getChild("argument");
	  VSource = getVariable(currInfo.getAttributeValue("name"));
	   if((VSource.isReference())) {
		  VDest = VSource.getShape(VDname);
		  variableList.put(VDname, VDest);
		  VDest.setData(ComponentManipulator.adjustWeights(VSource.getData(), VSource.getWeight(), true));
		  // should I do anything with avg?
	  } else {
		  log.log(Level.WARNING, "Only averaged reference variables may be multiplied by area fraction.");
	  }
  }

  /**
   * Debuging command used to pick out a single cell and follow its values
   * for all of the reference variables we know about. The command will
   * have a message with a value that is a useful message describing where
   * this message is in the DM code.  It also will have a where child which tells
   * which region name and it's (x, y) to pick out. An example would be: 
   * <p><printDebug>
   * 	<message value="Added up the PotentailForest" />
   * 	<where region="Maryland" x="5" y="2" />
   * </printDebug></p>
   * @param command The xml command with the children as stated above
   */
  private void printDebugCommand(Element command) {
	  //TODO: fix the XML in the comment above so it looks right if javadoc is ever run
	  Element regionElmt = command.getChild("where");
	  Iterator varsIt = variableList.entrySet().iterator();
	  Region VShape = (Region)regionList.get(regionElmt.getAttributeValue("region"));
	  int x = Integer.valueOf(regionElmt.getAttributeValue("x"));
	  int y = Integer.valueOf(regionElmt.getAttributeValue("y"));
	  System.out.println("================= "+command.getChild("message").getAttributeValue("value")+" in "
			  +VShape.name+" ("+x+", "+y+") ==============================");
	  System.out.println("Weight is "+VShape.getM()[y][x]);
	  while(varsIt.hasNext()) {
		  Map.Entry curr = (Map.Entry)varsIt.next();
		  if(((Variable)curr.getValue()).isReference()) {
			  String currVarName = (String)curr.getKey();
			  ReferenceVariable currVar = (ReferenceVariable)curr.getValue();
			  double [][] cData = VShape.extractRegion(currVar)[0].data;
			  System.out.println("Variable "+currVarName+" has value "+cData[y][x]+" and avg is "+currVar.avg);
		  }
	  }
	  System.out.println("=============================================================================");
  }

  /**
   * Downscales emissions from the region level to the courntry level(or really an smaller 
   * region level).  See EmissionsDownscaler.
   * @param command The xml command used to determine how and what to downscale.
   */
  private void downscaleEmissionsCommand(Element command) {
	  EmissionsDownscaler.doDownscaling(command, this);
  }

  /**
   * Adds a var to a group.  Currently it does not validate if it is a valid
   * year or region or if you are trying to mix years and regions.
   * @param command The xml command to determine which var to add, with what key,
   * 	and to what group variable
   */
  private void addToGroupCommand(Element command) {
	  log.log(Level.FINER, "begin function");
	  Variable VDest;
	  Variable VSource;
	  Element currInfo;

	  currInfo = command.getChild("target");
	  String VDname = currInfo.getAttributeValue("name");
	  VDest = getVariable(VDname);
	  currInfo = command.getChild("argument");
	  VSource = getVariable(currInfo.getAttributeValue("name"));
	  String key = currInfo.getAttributeValue("key");
	  if((VDest.isGroup())) {
		  ((GroupVariable)VDest).data.put(key, VSource);
	  } else {
		  log.log(Level.WARNING, "Can only add to a group variable");
	  }
  }

  /**
   * Remove a var from a group.  It will warn if it could not find the key.
   * @param command The xml command which specifies the key to remove from the group.
   */
  private void removeFromGroupCommand(Element command) {
	  log.log(Level.FINER, "begin function");
	  Variable VSource;
	  Element currInfo;

	  currInfo = command.getChild("argument");
	  VSource = getVariable(currInfo.getAttributeValue("name"));
	  currInfo = command.getChild("key");
	  String key = currInfo.getAttributeValue("value");
	  if((VSource.isGroup())) {
		  Object ret = ((GroupVariable)VSource).data.remove(key);
		  if(ret == null) {
			  log.log(Level.WARNING, "Did not find key: "+key+" in group ");
		  }
	  } else {
		  log.log(Level.WARNING, "Can only remove from a group variable");
	  }
  }

  /**
   * Realases references to a dm variable.  The idea here is that when we have long dm
   * scripts we accumulate variables which we don't intend on using anymore.  This command
   * allows users to flag them and hopefully release their memory. For the command element
   * we allow a list of variables which we will try to release.  We also allow a flag on 
   * the command which will tell this command to force garbage collection after releasing
   * reference.
   * @param command The xml command which specifies to dm vars to release.
   */
  private void releaseVarCommand(Element command) {
	  log.log(Level.FINER, "begin function");
	  Element currInfo;
	  boolean forceGC = false;
	  String valueTemp;

	  // check the force-gc flag on the command, by default we will not force this
	  if((valueTemp = command.getAttributeValue("force-gc")) != null) {
		  forceGC = Boolean.valueOf(valueTemp);
	  }

	  List<Element> removeVars = command.getChildren("variable");
	  for(Iterator<Element> it = removeVars.iterator(); it.hasNext(); ) {
		  currInfo = it.next();
		  if((valueTemp = currInfo.getAttributeValue("name")) != null) {
			  Object ret = variableList.remove(valueTemp);

			  // if we get null back that means that the variable didn't exist and
			  // we should warn the users since they may have mistyped something
			  if(ret == null) {
				  log.log(Level.WARNING, 
						  valueTemp+" was not released because it was not found in the variable list");
			  }
		  } else {
			  // warn the user that a name attribute is required
			  log.log(Level.WARNING, "Release var requires a name attribute on the variable element");
		  }
	  }

	  // force garbage collection if the user requested it
	  if(forceGC) {
		  log.log(Level.FINER, "Forcing garbage collection");
		  System.gc();
	  }
  }

  /**
   * Calculate the minimum distance(in km) from a reference variable such as land to another
   * reference variable such as offshore wind resource.  The result will be a reference variable
   * of the same shape as the distance to variable filled with the minimum distance to each cell.
   * Note that it is also useful to know which region was closest so a second output variable,
   * target region mask is also provided which is filled with the internal region ID of the closest
   * region.  This command provides the user to supply a parameter for the minimum value threshold
   * in the distance from variable to consider as well the region level to use when filling the
   * target region mask output variable.
   * @param command An XML command in the follow format:
   * <p>
   * <calcMinDistanceTo>
   *     <target name="[output with min distance]" />
   *     <targetRegionMask name="[output with the closest region's ID]" />
   *     <regionLevel value="[integer value represnting the region level for targetRegionMask]" />
   *     <distanceFrom name="[variable where the algorithm will search from]" />
   *     <distanceFromThreshold value="[optional param(default 0) minimum value to consider in distanceFrom" />
   *     <distanceTo name="[variable where the algorithm will calc a distance to]" />
   * </calcMinDistanceTo>
   * </p>
   */
  private void calcMinDistanceToCommand(Element command) {
	  log.log(Level.FINER, "begin function");
	  Element currInfo;
	  String valueTemp;
	  currInfo = command.getChild("regionLevel");
	  int regionLevel = Integer.parseInt(currInfo.getAttributeValue("value"));
	  currInfo = command.getChild("distanceFromThreshold");
	  double distanceFromThreshold = currInfo != null ? Double.parseDouble(currInfo.getAttributeValue("value")) : 0;
	  currInfo = command.getChild("distanceFrom");
	  Variable distanceFrom = getVariable(currInfo.getAttributeValue("name"));
	  currInfo = command.getChild("distanceTo");
	  Variable targetData = getVariable(currInfo.getAttributeValue("name"));

	  // both the target and target region mask take the same shape as distanceTo
	  currInfo = command.getChild("target");
	  Variable dataOut = targetData.getShape(currInfo.getAttributeValue("name"));
	  currInfo = command.getChild("targetRegionMask");
	  Variable regionMaskOut = targetData.getShape(currInfo.getAttributeValue("name"));

	  // copy all of the region info now, the data matricies will be created in the
	  // ComponentManipulator
	  Wrapper[] targetDataWrapper = targetData.getData();
	  Wrapper[] outWrapper = new Wrapper[targetDataWrapper.length];
	  Wrapper[] outRegionWrapper = new Wrapper[targetDataWrapper.length];
	  for(int rIndex = 0; rIndex < targetDataWrapper.length; ++rIndex) {
		  outWrapper[rIndex] = targetDataWrapper[rIndex].makeCopy();
		  outRegionWrapper[rIndex] = targetDataWrapper[rIndex].makeCopy();
	  }
	  dataOut.setData(outWrapper);
	  regionMaskOut.setData(outRegionWrapper);

	  // run the distance calculations
	  ComponentManipulator.distanceToData(distanceFrom.getData(), ((ReferenceVariable)distanceFrom).weight, targetDataWrapper, outWrapper, outRegionWrapper, regionLevel, distanceFromThreshold);

	  // place the results in the variable list so the user can use them
	  variableList.put(dataOut.name, dataOut);
	  variableList.put(regionMaskOut.name, regionMaskOut);
  }

  /**
   * Create a mask, where the data values of each contained cell is 1, from a given reference variable
   * and extending the mask to contain any cells within range of the given distance(in km).  Since we
   * are extending the range it is understandable that a user may want to use a differenct shape than
   * the data variable so a targetShape parameter is provided to determine the shape of the output
   * variable(note that values in targetShape are not considered at all only the shape).  An optional
   * parameter for the minimum value in the distance var to consider is also given which defaults to 0.
   * @param command An XML command in the follow format:
   * <p>
   * <maskDistanceFrom>
   *     <target name="[output mask]" />
   *     <targetShape name=”[the shape to use for target]” />
   *     <data name="[the data to create the mask from]" />
   *     <distance value="[a distance in km to extend the mask]" />
   *     <threshold value="[min value in data to consider, default 0]" />
   * </maskDistanceFrom>
   * </p>
   */
  private void maskDistanceFromCommand(Element command) {
	  log.log(Level.FINER, "begin function");
	  Element currInfo;
	  String valueTemp;
	  currInfo = command.getChild("distance");
	  double maxDistance = Double.parseDouble(currInfo.getAttributeValue("value"));
	  currInfo = command.getChild("threshold");
	  double minDataValue = currInfo != null ? Double.parseDouble(currInfo.getAttributeValue("value")) : 0;
	  currInfo = command.getChild("data");
	  ReferenceVariable distanceFrom = (ReferenceVariable)getVariable(currInfo.getAttributeValue("name"));
	  currInfo = command.getChild("targetShape");
	  Variable targetData = getVariable(currInfo.getAttributeValue("name"));

	  // copy the shape of targetShape for target
	  currInfo = command.getChild("target");
	  Variable dataOut = targetData.getShape(currInfo.getAttributeValue("name"));
	  Wrapper[] targetDataWrapper = targetData.getData();
	  Wrapper[] outWrapper = new Wrapper[targetDataWrapper.length];
	  for(int rIndex = 0; rIndex < targetDataWrapper.length; ++rIndex) {
		  outWrapper[rIndex] = targetDataWrapper[rIndex].makeCopy();
	  }
	  dataOut.setData(outWrapper);

	  // do the distance calculation to create the mask
	  ComponentManipulator.maskDistanceFrom(distanceFrom.getData(), targetDataWrapper, outWrapper, maxDistance, minDataValue);

	  // set the results into the variable list for use
	  variableList.put(dataOut.name, dataOut);
  }

  /**
   * Gets the internal region ID for a region by getting the region from a reference
   * variable and setting the ID into the target variable name as a DataVariable.  The
   * argument must be a reference variable so that we can get the associated region name
   * from it.  This command does not attempt to resolve levels of regions.  Note that
   * this method does not support looking up regions directly from a region name.
   * @param command The XML command which is assumed to be in the following format:
   * <p><getInternalID>
   * 		<target name="[target var name]" />
   * 		<argument name="[name of a reference variable to get the region from]" />
   * 	</getInternalID>
   * </p>
   */
  private void getInternalRegionIDCommand(Element command) {
	  log.log(Level.FINER, "begin function");
	  Element currInfo;
	  currInfo = command.getChild("target");
	  String targetVarName = currInfo.getAttributeValue("name");
	  if(targetVarName == null) {
		  log.log(Level.WARNING, "Could not find target attribute name");
		  return;
	  }
	  // we will attempt to get the region name from a reference variable, this
	  // approach allows us to use this within a forEachSubRegion command
	  currInfo = command.getChild("argument");
	  Variable regionVar= getVariable(currInfo.getAttributeValue("name"));
	  if(regionVar.isReference()) {
		  // get the region by looking it up by name in the regionList map
		  Region r = (Region)regionList.get(((ReferenceVariable)regionVar).region);
		  // we will just get the regionID directly instead of using the getInternalID
		  // method since we are not concerned about levels here
		  Variable regionIDVar = new DataVariable(targetVarName, r.regionID);
		  variableList.put(targetVarName, regionIDVar);
	  } else {
		  log.log(Level.WARNING, "Argument must be a reference variable to get the name of the region.");
	  }
  }

	  //*****************************************************************************
	  //*********************Helper Functions****************************************
	  //*****************************************************************************
	  /**
	   * Creates all of the documents and read and write streams which will
	   * be used by this class. Is automatically run by {@link runAll()}.
	   */
	  private void makeStreams()
	  {
		  log.log(Level.FINER, "begin parsing DMfiles.xml");
		  log.log(Level.FINER, "Parsing "+dSource);
		  //this function initializes all of the XML documents
		  //i will add the code for additional readers as i need them
		  try
		  {
			  SAXBuilder builder = new SAXBuilder();
			  //dDocument = builder.build(dSource);
			  dDocument = null;
			  try {
				  XMLReader saxReader = XMLReaderFactory.createXMLReader();
				  saxReader.setContentHandler(new DataContentHandler());
				  saxReader.parse(dSource);
			  } catch(SAXException e) {
				  e.printStackTrace();
			  }
			  log.log(Level.FINER, "data document parsed");
			  rDocument = builder.build(rSource);
			  log.log(Level.FINER, "region document parsed");
			  cDocument = builder.build(cSource);
			  log.log(Level.FINER, "command document parsed");
		  } catch(FileNotFoundException e)
		  {
			  log.log(Level.SEVERE, "FileNotFound! in -> makeStreams");
			  System.exit(0);
		  } catch(JDOMException e)
		  {
			  log.log(Level.SEVERE, "JDOM Exception! in -> makeStreams");
		  }
		  catch(IOException e)
		  {
			  log.log(Level.SEVERE, "IOException! in -> makeStreams");
		  }
	  }
	  /**
	   * Takes a string representation of a number in scientific form and returns
	   * a double containing that value. Assumes 'E' or 'e' will be used as the
	   * demarkation between mantissa and exponent. Following 'e' seperator will be
	   * a + or - sign denoting whether the exponent should be positive or
	   * negative.
	   * @param sc String to be parsed into a double.
	   * @return Double representation of supplied string.
	   */
	  private double stringToDouble(String sc)
	  {
		  //takes a string of the form #.###E+### and converts it to a double
		  double mantissa, exponent, expValue;
		  boolean expSignPos = false;
		  int E = sc.indexOf('E');
		  if(E == -1)
		  {
			  E = sc.indexOf('e');
		  }
		  if(E == -1)
		  { //this is a normal double value, use Double.parseString
			  return Double.parseDouble(sc);
		  } else
		  {
			  mantissa = Double.parseDouble(sc.substring(0, E));
			  char afterE = sc.charAt(E+1);
			  if(afterE == '+') {
				  expSignPos = true;
				  exponent = Double.parseDouble(sc.substring(E+2, sc.length()));
			  } else if(afterE == '-') {
				  exponent = Double.parseDouble(sc.substring(E+2, sc.length()));
			  } else {
				  // no sign and is positive
				  expSignPos = true;
				  exponent = Double.parseDouble(sc.substring(E+1, sc.length()));
			  }
			  if(expSignPos)
				  expValue = Math.pow(10, exponent);
			  else
				  expValue = Math.pow(10, (-1*exponent));

			  if(mantissa!=0)
			  {
				  return mantissa*expValue;
			  } else
			  {
				  return 0;
			  }
		  }
	  }

	  private void runCommand(Element currCom)
	  {
		  log.log(Level.FINER, "parsing "+currCom.getName()+" command");
		  if(currCom.getName().equals("variable"))
		  {
			  if(currCom.getAttributeValue("type").equals("data"))
			  {
				  log.log(Level.FINEST, "new data variable command");
				  newDataVariableCommand(currCom);
			  } else  if(currCom.getAttributeValue("type").equals("reference"))
			  {
				  log.log(Level.FINEST, "new reference variable command");
				  newReferenceVariableCommand(currCom);
			  } else  if(currCom.getAttributeValue("type").equals("group"))
			  {
				  log.log(Level.FINEST, "new group variable command");
				  newGroupVariableCommand(currCom);
			  } else  if(currCom.getAttributeValue("type").equals("scalar reference"))
			  {
				  log.log(Level.FINEST, "new scalar reference variable command");
				  newScalarReferenceVariableCommand(currCom);
			  } else
			  {
				  log.log(Level.WARNING, "Unknown variable type -> "+currCom.getAttributeValue("type"));
			  }
		  } else if(currCom.getName().equals("aggregateVariables"))
		  {
			  aggregateVariablesCommand(currCom);
		  } else if(currCom.getName().equals("add"))
		  {
			  addCommand(currCom);
		  } else if(currCom.getName().equals("subtract"))
		  {
			  subCommand(currCom);
		  } else if(currCom.getName().equals("addScalar"))
		  {
			  addScalarCommand(currCom);
		  } else if(currCom.getName().equals("multiply"))
		  {
			  multiplyCommand(currCom);
		  } else if(currCom.getName().equals("divide"))
		  {
			  divideCommand(currCom);
		  } else if(currCom.getName().equals("multiplyScalar"))
		  {
			  multiplyScalarCommand(currCom);
		  } else if(currCom.getName().equals("divideScalar"))
		  {
			  divideScalarCommand(currCom);
		  } else if(currCom.getName().equals("parseGreaterThan"))
		  {
			  parseGreaterThanCommand(currCom);
		  } else if(currCom.getName().equals("parseLessThan"))
		  {
			  parseLessThanCommand(currCom);
		  } else if(currCom.getName().equals("parseLessThanOrEqual"))
		  {
			  parseLessThanOrEqualCommand(currCom);
		  } else if(currCom.getName().equals("removeRandom"))
		  {
			  removeRandomCommand(currCom);
		  } else if(currCom.getName().equals("removeRandomGuided"))
		  {
			  removeRandomGuidedCommand(currCom);
		  } else if(currCom.getName().equals("maskCombineOr"))
		  {
			  maskCombineOrCommand(currCom);
		  } else if(currCom.getName().equals("maskCombineAnd"))
		  {
			  maskCombineAndCommand(currCom);
		  } else if(currCom.getName().equals("maskRemain"))
		  {
			  maskRemainCommand(currCom);
		  } else if(currCom.getName().equals("maskRemove"))
		  {
			  maskRemoveCommand(currCom);
		  } else if(currCom.getName().equals("countGreaterThan"))
		  {
			  countGreaterThanCommand(currCom);
		  } else if(currCom.getName().equals("countLessThan"))
		  {
			  countLessThanCommand(currCom);
		  } else if(currCom.getName().equals("countElements"))
		  {
			  countElementsCommand(currCom);
		  } else if(currCom.getName().equals("largestValue"))
		  {
			  largestValueCommand(currCom);
		  } else if(currCom.getName().equals("smallestValue"))
		  {
			  smallestValueCommand(currCom);
		  } else if(currCom.getName().equals("aggregateValues"))
		  {
			  aggregateValuesCommand(currCom);
		  } else if(currCom.getName().equals("sumValues"))
		  {
			  sumValuesCommand(currCom);
		  } else if(currCom.getName().equals("sumArea"))
		  {
			  sumAreaCommand(currCom);
		  } else if(currCom.getName().equals("sumRegionArea"))
		  {
			  sumRegionAreaCommand(currCom);
		  } else if(currCom.getName().equals("avgOverRegion"))
		  {
			  avgOverRegionCommand(currCom);
		  } else if(currCom.getName().equals("avgOverRegionByArea"))
		  {
			  avgOverRegionByAreaCommand(currCom);
		  } else if(currCom.getName().equals("avgVariables"))
		  {
			  avgVariablesCommand(currCom);
		  } else if(currCom.getName().equals("avgVariablesOverRegion"))
		  {
			  avgVariablesOverRegionCommand(currCom);
		  } else if(currCom.getName().equals("avgVariablesOverRegionByArea"))
		  {
			  avgVariablesOverRegionByAreaCommand(currCom);
		  } else if(currCom.getName().equals("weightValues"))
		  {
			  weightValuesCommand(currCom);
		  } else if(currCom.getName().equals("setAverage"))
		  {
			  setAverageProperty(currCom);
		  } else if(currCom.getName().equals("frequencyAnalysis"))
		  {
			  frequencyAnalysisCommand(currCom);
		  } else if(currCom.getName().equals("windPowerCalc"))
		  {
			  windPowerCalcCommand(currCom);
		  } else if(currCom.getName().equals("extractSubRegion"))
		  {
			  extractSubRegionCommand(currCom);
		  } else if(currCom.getName().equals("getChildVariable"))
		  {
			  getChildVariable(currCom);
		  } else if(currCom.getName().equals("print"))
		  {
			  printCommand(currCom);
		  } else if(currCom.getName().equals("printVerbose"))
		  {
			  printVerboseCommand(currCom);
		  } else if(currCom.getName().equals("plot"))
		  {
			  plotCommand(currCom);
		  } else if(currCom.getName().equals("createDataSet"))
		  {
			  createDataSetCommand(currCom);
		  } else if(currCom.getName().equals("createNetCDFFile"))
		  {
			  createNetCDFFileCommand(currCom);
		  } else if(currCom.getName().equals("appendNetCDFFile"))
		  {
			  appendNetCDFFileCommand(currCom);
		  } else if(currCom.getName().equals("comment"))
		  {
			  setComment(currCom);
		  } else if(currCom.getName().equals("setReference"))
		  {
			  setReferenceCommand(currCom);
		  } else if(currCom.getName().equals("setUnits"))
		  {
			  setUnitsCommand(currCom);
		  } else if(currCom.getName().equals("forEachSubregion"))
		  {
			  forEachSubregionCommand(currCom);
		  } else if(currCom.getName().equals("forEach"))
		  {
			  forEachCommand(currCom);
		  } else if(currCom.getName().equals("zoneCombine"))
		  {
			  zoneCombineCommand(currCom);
		  } else if(currCom.getName().equals("divByAreaFract"))
		  {
			  divByAreaFractCommand(currCom);
		  } else if(currCom.getName().equals("multByAreaFract"))
		  {
			  multByAreaFractCommand(currCom);
		  } else if(currCom.getName().equals("printDebug"))
		  {
			  printDebugCommand(currCom);
		  } else if(currCom.getName().equals("downscaleEmissions"))
		  {
			  downscaleEmissionsCommand(currCom);
		  } else if(currCom.getName().equals("addToGroup"))
		  {
			  addToGroupCommand(currCom);
		  } else if(currCom.getName().equals("removeFromGroup"))
		  {
			  removeFromGroupCommand(currCom);
		  } else if(currCom.getName().equals("releaseVar"))
		  {
			  releaseVarCommand(currCom);
		  } else if(currCom.getName().equals("calcMinDistanceTo"))
		  {
			  calcMinDistanceToCommand(currCom);
		  } else if(currCom.getName().equals("maskDistanceFrom"))
		  {
			  maskDistanceFromCommand(currCom);
		  } else if(currCom.getName().equals("getInternalRegionID"))
		  {
			  getInternalRegionIDCommand(currCom);
		  } else if(currCom.getName().equals("exit"))
		  {
			  System.exit(0);
		  } else
		  {
			  log.log(Level.WARNING, "Unknown user command -> "+currCom.getName());
		  }
	  }

	  private void fillGroupByExplicit(GroupVariable var, Element members)
	  {
		  log.log(Level.FINER, "begin function");
		  Element currMem;
		  String currName;
		  Variable currVar;
		  List mems = members.getChildren("variable");

		  for(int i = 0; i < mems.size(); i++)
		  {
			  currMem = (Element)mems.get(i);
			  currName = currMem.getAttributeValue("value");
			  if(variableList.containsKey(currName))
			  {
				  currVar = getVariable(currName);
				  var.addData(currVar.getCopy());
			  } else
			  { //this variable doesnt actualyl exist, kick to null
				  log.log(Level.WARNING, currName+" does not exist, cant add to group.");
				  var = null;
				  return;
			  }
		  }
	  }
	  private void fillGroupByTime(GroupVariable var, Element members)
	  {
		  log.log(Level.FINER, "begin function");
		  Element currInfo;
		  String reg, field;
		  Region R;
		  ArrayList<String> timeList;
		  Variable currVar;

		  currInfo = members.getChild("region");
		  reg = currInfo.getAttributeValue("value");
		  currInfo = members.getChild("field");
		  field = currInfo.getAttributeValue("value");

		  boolean avg = ((Boolean)dataAvgAdd.get(field)).booleanValue();

		  //get a list of times
		  if(regionList.containsKey(reg))
		  {
			  R = (Region)regionList.get(reg);
			  timeList = R.getTimeList(field);

			  for(int i = 0; i < timeList.size(); i ++)
			  { //add each time entry as a seperate variable
				  currVar = new ReferenceVariable(timeList.get(i), R, field, timeList.get(i), avg);
				  var.addData(currVar);
			  }
		  } else
		  { //cant very well add a region if it doesnt exist now can we
			  log.log(Level.WARNING, reg+" does not exist, cant extract time group.");
			  var = null;
		  }
	  }
	  private void fillGroupByExtraction(GroupVariable var, Element members)
	  {
		  log.log(Level.FINER, "begin function");
		  Variable VSource;
		  ReferenceVariable currVar;
		  String Vname, Rname;
		  Region RShape;
		  superRegion SR;
		  Region[] regList;

		  Vname = members.getAttributeValue("variable");

		  if(variableList.containsKey(Vname))
		  {
			  VSource = getVariable(Vname);
			  if(VSource.isReference())
			  {
				  Rname = ((ReferenceVariable)VSource).region;
				  RShape = (Region)regionList.get(Rname);
				  if(RShape.isSuper())
				  {
					  SR = (superRegion)RShape;
					  regList = (Region[])(SR).data.toArray(new Region[0]);

					  for(int i = 0; i < regList.length; i ++)
					  { //add each child region as a seperate variable
						  currVar = new ReferenceVariable(regList[i].name, regList[i]);
						  currVar.avg = ((ReferenceVariable)VSource).avg;
						  currVar.setData(regList[i].extractRegion((ReferenceVariable)VSource));

						  var.addData(currVar);
					  }
				  } else
				  {
					  var = null;
					  log.log(Level.WARNING, "Variable "+Vname+" is subRegion and has no child regions.");
					  return;
				  }
			  } else
			  {
				  var = null;
				  log.log(Level.WARNING, "Variable "+Vname+" is not a reference variable.");
				  return;
			  }
		  } else
		  {
			  var = null;
			  log.log(Level.WARNING, "Variable "+Vname+" does not exist!");
			  return;
		  }
	  }
	  private void fillGroupByChildren(GroupVariable var, Element members)
	  {
		  log.log(Level.FINER, "begin function");
		  Element currInfo;
		  String reg, field, time;
		  Region R;
		  Region[] regList;
		  Variable currVar;

		  currInfo = members.getChild("region");
		  reg = currInfo.getAttributeValue("value");
		  currInfo = members.getChild("field");
		  field = currInfo.getAttributeValue("value");
		  currInfo = members.getChild("time");
		  time = currInfo.getAttributeValue("value");

		  boolean avg = ((Boolean)dataAvgAdd.get(field)).booleanValue();

		  //get a list of times
		  if(regionList.containsKey(reg))
		  {
			  R = (Region)regionList.get(reg);
			  if(R.isSuper())
			  {
				  regList = (Region[])((superRegion)R).data.toArray(new Region[0]);
				  for(int i = 0; i < regList.length; i ++)
				  { //add each child region as a seperate variable
					  currVar = new ReferenceVariable(regList[i].name, regList[i], field, time, avg);
					  var.addData(currVar);
				  }
			  } else
			  { //this i sjust a sub region, no children to fill with, null and kick
				  var = null;
				  log.log(Level.WARNING, "Group variable "+var.name+" was seeded with a subRegion");
				  return;
			  }
		  } else
		  { //cant very well add a region if it doesnt exist now can we
			  var = null;
			  log.log(Level.WARNING, reg+" does not exist, cant extract children group.");
			  return;
		  }
	  }
	  /**
	   * Checks to make sure a variable referenced in some command currently exists.
	   * @param name the name of the Variable
	   * @return the Variable, if Variable does not exist will kill program
	   */
	  private Variable getVariable(String name)
	  {
		  Variable toReturn = (Variable)variableList.get(name);

		  if(toReturn == null)
		  {
			  log.log(Level.SEVERE, "referenced variable '"+name+"' does not exist -> terminating");
			  System.exit(0);
		  }

		  return toReturn;
	  }
	  /**
	   * Checks all input arguments for creation of a new Variable. Region, field, and time.
	   * If any of these do not exist will kill program.
	   * @param region The region to get data from.
	   * @param field The specific field of data.
	   * @param time The desired time within that field.
	   */
	  private void checkRegionFieldTime(String region, String field, String time)
	  {
		  Region holdRegion = (Region)regionList.get(region);

		  if(holdRegion == null)
		  {
			  log.log(Level.SEVERE, "referenced region '"+region+"' does not exist -> terminating");
			  System.exit(0);
		  }

		  // special case here where RegionCellSize is a special field which really
		  // does not exist.. if there was actually a field named RegionCellSize
		  // it will use the field instead of the special case
		  if(field.equals("RegionCellSize") && dataAvgAdd.get(field) == null) {
			  return;
		  }
		  if(dataAvgAdd.get(field) == null)
		  {
			  log.log(Level.SEVERE, "referenced field '"+field+"' does not exist -> terminating");
			  System.exit(0);
		  }

		  if(!holdRegion.getTimeList(field).contains(time))
		  {
			  log.log(Level.SEVERE, "referenced time '"+time+"' in field '"+field+"' does not exist\n" +
					  "valid times are: '"+holdRegion.getTimeList(field).toString()+"' -> terminating");

			  System.exit(0);
		  }
	  }

	  private class DataContentHandler extends DefaultHandler {
		  subRegion toAdd;
		  int sizeX, sizeY, currX, currY;
		  Map toAddVar;
		  double[][] toAddTime;
		  String varName, timeName;
		  boolean avg;
		  boolean isVarInfo = false;
		  public void startElement(String uri, String localName, String qName, Attributes attrs) {
			  if(localName.equals("input")) {
				  resolution = Double.parseDouble(attrs.getValue("res"));
				  // Normalize resolution to units of one quarter of a degree
				  resolution = (double)( (int)Math.round(resolution*60*4) )/(60.0*4.0);
			  } else if(localName.equals("variableInfo")) {
				  isVarInfo = true;
			  } else if(localName.equals("region")) {
				  // do I need to keep track of numAtomicRegions ?
				  toAdd = new subRegion();
				  toAdd.name = attrs.getValue("name");
				  toAdd.resolution = resolution;
				  toAdd.x = Double.parseDouble(attrs.getValue("x"));
				  toAdd.y = Double.parseDouble(attrs.getValue("y"));
				  sizeX = Integer.parseInt(attrs.getValue("sizeX"));
				  sizeY = Integer.parseInt(attrs.getValue("sizeY"));
				  toAdd.width = (sizeX*resolution);
				  toAdd.height = (sizeY*resolution);
			  } else if(localName.equals("weight")) {
				  varName = "weight";
				  toAddVar = new HashMap(); // does this have to be a treemap?
			  } else if(localName.equals("time")) {
				  timeName = attrs.getValue("value");
				  toAddTime = new double[sizeY][sizeX];
				  for(int hy = 0; hy<sizeY; hy++)
				  {
					  for(int hx = 0; hx<sizeX; hx++)
					  {
						  if(varName.equals("weight")) {
							  toAddTime[hy][hx] = 0;
						  } else { // variable
							  toAddTime[hy][hx] = Double.NaN;
						  }
					  }
				  }
			  } else if(localName.equals("data")) {
				  currX = Integer.parseInt(attrs.getValue("x"));
				  currY = Integer.parseInt(attrs.getValue("y"));
				  if (currX >= sizeX) { //sjs add trap for out of bounds error.
					  log.log(Level.WARNING, "Skipping X point "+currX+" greater than size "+sizeX+" for region: "+toAdd.name);

				  } else if (currY >= sizeY) { //sjs add trap for out of bounds error.
					  log.log(Level.WARNING, "Skipping Y point "+currY+" greater than size "+sizeY+" for region: "+toAdd.name);

				  } else if (currX < 0 ) { //sjs add trap for out of bounds error.
					  log.log(Level.WARNING, "Skipping X point "+currX+" less than zero for region: "+toAdd.name);

				  } else if (currY < 0) { //sjs add trap for out of bounds error.
					  log.log(Level.WARNING, "Skipping Y point "+currY+" less than zero for region: "+toAdd.name);

				  } else {
					  toAddTime[currY][currX] = stringToDouble(attrs.getValue("value"));
					  if(!varName.equals("weight") && !avg)
					  { //this is an additive value and should be initially weighted (now)
						  toAddTime[currY][currX] *= ((double[][])((Map)toAdd.data.get("weight")).get("0"))[currY][currX];
					  }
				  }
			  } else if(localName.equals("variable")) {
				  if(!isVarInfo) {
					  varName = attrs.getValue("value");
					  avg = ((Boolean)dataAvgAdd.get(varName)).booleanValue();
					  toAddVar = new HashMap(); // again has to be TreeMap?
				  } else {
					  varName = attrs.getValue("name");
				  }
			  } else if(localName.equals("average")) {
				  avg = (Boolean.valueOf(attrs.getValue("value"))).booleanValue();
				  dataAvgAdd.put(varName, new Boolean(avg));
			  } else if(localName.equals("reference")) {
				  dataRef.put(varName, attrs.getValue("value"));
			  } else if(localName.equals("units")) {
				  dataUnits.put(varName, attrs.getValue("value"));
			  } else {
				  log.log(Level.WARNING, "Didn't recognize element name: "+localName);
			  }
		  }
		  public void endElement(String uri, String localName, String qName) {
			  // make sure things get reset to null?
			  if(localName.equals("variableInfo")) {
				  isVarInfo = false;
			  } else if(localName.equals("region")) {
				  //adding region to master list
				  regionList.put(toAdd.name, toAdd);
			  } else if(localName.equals("weight")) {
				  //end getting weight
			  } else if(localName.equals("time")) {
				  //end getting time
				  toAddVar.put(timeName, toAddTime);
				  toAdd.data.put(varName, toAddVar);
			  } else if(localName.equals("data")) {
				  //end getting data
			  }
		  }
	  }

	  //*****************************************************************************
  }
