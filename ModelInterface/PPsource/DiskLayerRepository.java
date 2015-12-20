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
package ModelInterface.PPsource;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.awt.geom.*;

public class DiskLayerRepository implements DataRepository
{
  /*
   * implements as a treemap of vars -> treemap of times -> location on disk
   */
  TreeMap<String, TreeMap<Double, String>> root;
  double[][] currLayer;
  String currName;
  int xSize;
  int ySize;
  boolean dirty;
  
//*********************************************************
//*****************Class Constructors**********************
//********************************************************* 
  
  public DiskLayerRepository()
  {
    root = new TreeMap<String, TreeMap<Double, String>>();
    xSize = 360;
    ySize = 180;
    currName = "";
    dirty = false;
  }
  public DiskLayerRepository(int x, int y)
  {
    root = new TreeMap<String, TreeMap<Double, String>>();
    xSize = x;
    ySize = y;
    currName = "";
    dirty = false;
  }
  
//*********************************************************
//*************Begin Functions Proper**********************
//*********************************************************
  
  public void changeLayer(String varName, double time)
  {
    if(!currName.equals((varName+time)))
    {
      if(dirty)
      {
        writeLayer(currName, currLayer);
      }
      
      double[][] thisLayer = createLayer(varName, time);
      
      currLayer = thisLayer;
      currName = (varName+time);
      dirty = false; //just changed layer after reading from disk (or writing to disk)
    }
  }
  public double[][] createLayer(String varName, double time)
  {
    if(!root.containsKey(varName))
    { //create variable
      root.put(varName, new TreeMap<Double, String>());
    }
    
    TreeMap<Double, String> inVar = root.get(varName);
    if(!inVar.containsKey(time))
    { //create this field matrix
      double[][] newb = new double[xSize][ySize];
      //filling this new layer with NaN's
      for(int i = 0; i < xSize; i ++)
      {
        for(int k = 0; k < ySize; k++)
        {
          newb[i][k] = Double.NaN;
        }
      }
      
      //storing new matrix identifier
      inVar.put(time, varName+time);
      writeLayer(varName+time, newb);
      
      return newb;
    }
    
    return readLayer(varName+time);
  }
  public double[][] getLayer(String varName, double time)
  {
    changeLayer(varName, time);
    
    return currLayer;
  }
  
  public void setValue(int X, int Y, double value)
  {
    currLayer[X][Y] = value;
    dirty = true;
  }
  public void setValue(String varName, double time, int X, int Y, double value)
  {
    changeLayer(varName, time);
    
    setValue(X, Y, value);
  }

  public void addValue(int X, int Y, double value)
  {
    try
    {
      if(Double.isNaN(currLayer[X][Y]))
      {
        currLayer[X][Y] = value;
      } else
      {
        currLayer[X][Y] += value;
      }
      dirty = true;
    } catch(ArrayIndexOutOfBoundsException e)
    {
      System.out.println("SEVERE: ("+X+","+Y+") out of bounds - PROGRAM TERMINATING");
      System.exit(1);
    }
    
    
    
  }
  public void addValue(String varName, double time, int X, int Y, double value)
  {
    changeLayer(varName, time);
    
    addValue(X, Y, value);
  }  
  
  public double getValue(int X, int Y)
  {
    return currLayer[X][Y];
  }
  public double getValue(String varName, double time, int X, int Y)
  {
    changeLayer(varName, time);
    
    return getValue(X, Y);
  }

  public int mergeHoldTo(String holdName, String varName)
  {
    //this will take whatever information is in the hold layer and merge it
    //with the designated variable
    //**this function can do multiple times at once, each time SHOULD align
    //**with a time in the variable (time == time)
    Map.Entry<Double, String> holdEntry;
    Iterator<Map.Entry<Double, String>> iH;
    Double thisTime;
    TreeMap<Double, String> hold;
    TreeMap<Double, String> overwrite;
    double[][] holdTime;
    double[][] overwriteTime;
    
    
    //make sure hold exists and the var to overwrite exists
    if((!root.containsKey(holdName))||(!root.containsKey(varName)))
    {
      return -1;
    }
    hold = root.get(holdName);
    overwrite = root.get(varName);
    
    //iterate through each time in hold
    iH = hold.entrySet().iterator();
    while(iH.hasNext())
    {
      holdEntry = iH.next();
      thisTime = holdEntry.getKey();
      if(!overwrite.containsKey(thisTime))
      {
        //this time didnt exist in what we are overwriting, cant merge them
        return -1;
      }
      holdTime = readLayer(holdEntry.getValue());
      overwriteTime = readLayer(overwrite.get(thisTime));
      
      //actually putting them together
      overwriteTime = overMerge(overwriteTime, holdTime);
      //replaciung the old info with this new info
      writeLayer(holdName+thisTime, overwriteTime);
    }
    
    //we have completely put hold's data in the passed var
    //remove hold
    removeLayer(holdName);
    
    return 1; //success
  }
  
  public TreeMap<String, TreeMap<Double, Double>> getAllLayers(int X, int Y)
  {
    /*
     * this function will not return NaN values, they will be completly unreported
     * this is because a NaN represents something that was never changed
     * aka it was not in the data.
     */
    TreeMap<String, TreeMap<Double, Double>> toReturn = new TreeMap<String, TreeMap<Double, Double>>();
    TreeMap<Double, Double> holdVar;
    
    Map.Entry<String, TreeMap<Double, String>> varEntry;
    Map.Entry<Double, String> timeEntry;
    Iterator<Map.Entry<Double, String>> iT;
    Iterator<Map.Entry<String, TreeMap<Double, String>>> iV = root.entrySet().iterator();
    Double value;
    String varName;
    Double timeName;
    
    while(iV.hasNext())
    {
      varEntry = iV.next();
      varName = varEntry.getKey();
      iT = varEntry.getValue().entrySet().iterator();
      while(iT.hasNext())
      {
        timeEntry = iT.next();
        timeName = timeEntry.getKey();
        changeLayer(varName, timeName);
        value = currLayer[X][Y];
        
        if(!Double.isNaN(value))
        {
          if(!toReturn.containsKey(varName))
          {
            toReturn.put(varName, new TreeMap<Double, Double>());
          }
          holdVar = toReturn.get(varName);
          
          holdVar.put(timeName, value);
        }
      }
    }
    
    return toReturn;
  }
  
  public Map<String, Map<String, Map<Point2D.Double, Double>>> getRegion(int X, int Y, double[][] weights, double xL, double yL, double res)
  {
    /*
     * X and Y are the top left corner
     */
    Map<String, Map<String, Map<Point2D.Double, Double>>> toReturn = new LinkedHashMap<String, Map<String, Map<Point2D.Double, Double>>>();
    Map<String, Map<Point2D.Double, Double>> holdVar;
    Map<Point2D.Double, Double> holdTime;
    Map.Entry<String, TreeMap<Double, String>> varEntry;
    Map.Entry<Double, String> timeEntry;
    Iterator<Map.Entry<Double, String>> iT;
    Iterator<Map.Entry<String, TreeMap<Double, String>>> iV = root.entrySet().iterator();
    String varName;
    Double timeName;
    double currXL, currYL;

    toReturn.put("time", new LinkedHashMap<String, Map<Point2D.Double, Double>>());
    holdVar = toReturn.get("time");
    holdVar.put("0", new TreeMap<Point2D.Double, Double>());
    holdTime = holdVar.get("0");
    currXL = xL;
    for(int x = X; x < (X+weights[0].length); x++)
    {
      currYL = yL;
      for(int y = Y; y > (Y-weights.length); y--)
      {
        if(weights[x][y] > 0)
        {
          //add weight
          holdTime.put(new Point2D.Double(currXL, currYL), weights[x][y]);
        }
        currYL -= res;
      }
      currXL += res;
    }
    
    while(iV.hasNext())
    {
      varEntry = iV.next();
      varName = varEntry.getKey();
      toReturn.put(varName, new LinkedHashMap<String, Map<Point2D.Double, Double>>());
      holdVar = toReturn.get(varName);
      iT = varEntry.getValue().entrySet().iterator();
      while(iT.hasNext())
      {
        timeEntry = iT.next();
        timeName = timeEntry.getKey();
        holdVar.put(timeName.toString(), new TreeMap<Point2D.Double, Double>());
        holdTime = holdVar.get(timeName.toString());
        
        changeLayer(varName, timeName);
        
        currXL = xL;
        for(int x = X; x < (X+weights[0].length); x++)
        {
          currYL = yL;
          for(int y = Y; y > (Y-weights.length); y--)
          {
            if(weights[x][y] > 0)
            {
              //get this point's value
              //add it to toReturn
              holdTime.put(new Point2D.Double(currXL, currYL), (currLayer[x][y]));
            }
            currYL -= res;
          }
          currXL += res;
        }
      }
    }
    
    return toReturn;
  }
  
//*********************************************************
//*************Begin Private Functions*********************
//*********************************************************
  
  private double[][] overMerge(double[][] oldData, double[][] newData)
  {
    if((oldData.length != newData.length)||(oldData[0].length != newData[0].length))
    {
      return null;
    }
    double[][] toReturn = oldData;
    
    for(int i = 0; i < oldData.length; i++)
    {
      for(int k = 0; k < oldData[0].length; k++)
      {
        if(!Double.isNaN(newData[i][k]))
        {
          //then we use the new value, otherwise just keep old value
          toReturn[i][k] = newData[i][k];
        }
      } 
    }
    
    //will now have a mesh of new and old values (new wherever they exist)
    return toReturn;
  }
  private double[][] readLayer(String Lname)
  {
    String fileName = Lname+".lay";
    double[][] toReturn = new double[xSize][ySize];
    
    InputStream fin;
    try
    {
      fin = new FileInputStream(fileName);
      DataInputStream in = new DataInputStream(fin);
      
      for(int i = 0; i < xSize; i ++)
      {
        for(int k = 0; k < ySize; k++)
        {
          toReturn[i][k] = in.readDouble();
        }
      }
    } catch(FileNotFoundException e){} 
      catch(IOException e){}
    
    return toReturn;
  }
  private void writeLayer(String Lname, double[][] lay)
  {
    String fileName = Lname+".lay";
    File myFile = new File(fileName);
    OutputStream fout;
    try
    {
      fout = new FileOutputStream(myFile);
      DataOutputStream out = new DataOutputStream(fout);
      
      for(int i = 0; i < xSize; i++)
      {
        for(int k = 0; k < ySize; k++)
        {
          out.writeDouble(lay[i][k]);
        }
      }
      
      myFile.deleteOnExit();
    } catch(FileNotFoundException e){} 
      catch(IOException e){}
  }
  
  private void removeLayer(String varName, double time)
  {
    TreeMap<Double, String> hold = root.get(varName);
    hold.remove(time);
    
    File toDelete = new File(varName+time+".lay");
    toDelete.delete();
  }
  private void removeLayer(String varName)
  {
    File toDelete;
    Double timeName;
    Map.Entry<Double, String> timeEntry;
    TreeMap<Double, String> hold = root.get(varName);
    Iterator<Map.Entry<Double, String>> iT = hold.entrySet().iterator();
    
    while(iT.hasNext())
    {
      timeEntry = iT.next();
      timeName = timeEntry.getKey();

      toDelete = new File(varName+timeName+".lay");
      toDelete.delete();
    }
    
    root.remove(varName);
  }
}
