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
 * \file RegionMask.java
 * \ingroup Preprocess
 * \brief Definition of a region as bounding rectangle and bitmask.
 *
 *  This class defines a region. A region consists of its x and y coordinates (in
 * latitude and longitude) its height and width, the resolution of data it contains
 * and a bit mask to define for each resolution sized block wether or not it is
 * contained in the region.
 *
 * \author Vincent Nibali
 * \date $Date: 2009-06-17 12:06:36 -0400 (Wed, 17 Jun 2009) $
 * \version $Revision: 3581 $
 */

/* DEVELOPER COMMENTS
 * contain a bounding box, resolution, bitmask for a region
 * this will be used to create an actual region object, to determine which data it should contain
 */
package ModelInterface.PPsource;

import java.awt.geom.*;
import java.util.logging.*; 
import java.util.Set;
import java.util.HashSet;

/**
 * Definition of a region as bounding rectangle and bitmask. 
 * A region consists of its x and y coordinates (in latitude and longitude)
 * its height and width, the resolution of data it contains
 * and a bit mask to define for each resolution sized block wether or not it is
 * contained in the region.
 * 
 * @author Vincent Nibali
 * @version 1.0
 */
public class RegionMask extends Rectangle2D.Double
{
  public String name; //the name of the region this is a mask for
  public byte  bMask[][]; //a bitmask which defines whether or not each point is in the region
  public double resolution; //how much space each bit represents
  Logger log = Logger.getLogger("Preprocess"); //log class to use for all logging output 

  /**
   * A set of variable names which should not be included in this region
   */
  private Set<String> excludeVarNames;

  //Rec2D already contains x, y, w, h so i wont store the min's and max's here...
  /**
   * Default Constructor. Creates an impossible rectangle with negative bounds.
   */
  public RegionMask()
  {
    name = "noName";
    resolution = 1;
    x = 0;
    y = 0;
    width = -1;
    height = -1;
    excludeVarNames = new HashSet<String>();
  }
  /**
   * Basic Constructor for name and resolution. The bounds of the rectangle are set to
   * impossible values to force later setting to correct values.
   * 
   * @param n The name of the region this is a mask for.
   * @param r The resolution of the bitMask this class holds.
   */
  public RegionMask(String n, double r)
  {
    name = n;
    resolution = r;
    x = 0;
    y = 0;
    width = -1;
    height = -1;
    excludeVarNames = new HashSet<String>();
  }
  /**
   * Gets the portion of the passed rectangle which overlaps this region.
   * Checks bounds of rectangle vs bounds of region, then checks agains the
   * bitmask of the region itself.
   * 
   * @param X X-value of the bottom left-most point in a rectangle.
   * @param Y Y-value of the bottom left-most point in a rectangle.
   * @param W Width of the rectangle.
   * @param H Height of the rectangle.
   * @return Portion of the passed rectangle which overlaps this region.
   */
  public double inRegion(double X, double Y, double W, double H)
  {
    //pretty self explanitory, checks if any part of the sent rectangle is in this specific region
    //know it intersects in some way, now find if it additionally lays over a
    //region of the mask which contains some 1's, return portion which is in region
    double toReturn = 0;
    Block inBlock = new Block(X, Y, H, W);
    int lowX, highX, lowY, highY; //the range of indicies to check in the mask
    double smallX, bigX, smallY, bigY; //used for proportion finding
    double thisX, thisY; //x and y coordinates for the current place in bit mask
    
// Get index of base point of this region
    Point2D.Double minThisRegion, minPassedPoint, maxThisRegion, maxPassedPoint;
    minThisRegion  = CoordConversions.point2index( new Point2D.Double(x, y), resolution, true);
    maxThisRegion  = CoordConversions.point2index( new Point2D.Double(x + width, y + height ), resolution, false);

	// Next block takes the coordinates of the passed in rectangle and converts them native resolution
	// indices aligned on working resolution cell boundaries
	
	// Working resolution indices
    minPassedPoint = CoordConversions.point2index( new Point2D.Double(X, Y), H, true);
    maxPassedPoint = CoordConversions.point2index( new Point2D.Double(X + W, Y + H ), H, false);

    Point2D.Double thisPointConverted;
	// Back to working resolution coordinates (since coordinates passed in are not necessarily at the boundary of a working resolution cell)
    thisPointConverted = CoordConversions.index2point(minPassedPoint, H );
    // Native resolution coordinates
    minPassedPoint = CoordConversions.point2index( thisPointConverted, resolution, true);

	// Back to working resolution coordinates (since coordinates passed in are not necessarily at the boundary of a working resolution cell)
    thisPointConverted = CoordConversions.index2point(maxPassedPoint, H );
    // Native resolution coordinates
    maxPassedPoint = CoordConversions.point2index( thisPointConverted, resolution, false);

	lowX  = (int)Math.round( minPassedPoint.x - minThisRegion.x );
	highX = (int)Math.round( maxPassedPoint.x - minThisRegion.x );

	// y dimension offset counts down from the top, but indicies count up from the bottom. 
	// So min Y here (which is bmask offset) is maxY passed in 
	lowY  = (int)Math.round( maxThisRegion.y - maxPassedPoint.y );
	highY = (int)Math.round( maxThisRegion.y - minPassedPoint.y );

    for(int i = (lowY); i< (highY); i++)
    {
      if((i<bMask.length)&&(i>=0)) //Check for out of bounds
      {
        for(int k = (lowX); k< (highX); k++)
        {
          if((k<bMask[i].length)&&(k>=0)) //Check for out of bounds
          {
     		Point2D.Double thisPoint;
     		// check that y is properly defined
            thisPoint = CoordConversions.index2point( new Point2D.Double(minThisRegion.x + k, maxThisRegion.y - i-1), resolution );
//			previous method
//         	thisX = (x+(k*resolution)); //counting down from the left
//          thisY = (y+height-((i+1)*resolution)); //counting down from the top
		    thisX = thisPoint.x; thisY = thisPoint.y;
            
           if(bMask[i][k]==1 && inBlock.intersects(thisX, thisY, resolution, resolution))
            { //this portion of checked area lies in block, add it to the blocks weight

              if(thisPoint.x > X)
                smallX = thisPoint.x;
              else
                smallX = X;
              if(thisPoint.y > Y)
                smallY = thisPoint.y;
              else
                smallY = Y;
              
              if((thisPoint.x+resolution) < (X+W))
                bigX = (thisPoint.x+resolution);
              else
                bigX = (X+W);
              if((thisPoint.y+resolution) < (Y+H))
                bigY = (thisPoint.y+resolution);
              else
                bigY = (Y+H);

              toReturn += (((bigX-smallX)*(bigY-smallY))/(W*H));
	      /*
	      double oldWay = (((bigX-smallX)*(bigY-smallY))/(W*H));
	      double newWay = inBlock.getOverlap(regionBlock);
	  System.out.println("("+X+", "+Y+") - "+H+"x"+W);
	  System.out.println("("+thisX+", "+thisY+") - "+resolution+"x"+resolution);
    System.out.println("old: "+oldWay);
    System.out.println("new: "+newWay);
	  try {
	  System.in.read();
	  } catch(java.io.IOException e) {}
	  */
            }
          }
        }
      }
    } //return the amount of overlap (DB of mask)

    // for debugging. log rounding errors if they matter. 
    if ( ( ( (highY - lowY ) != (int)Math.round( H/resolution ) ) ||
           ( (highX - lowX ) != (int)Math.round( W/resolution ) ) ) &&
         toReturn > 0 ) {
         
    	log.log(Level.FINE, " Rounding error encountered in region "+name+" with x and y bounds: ("
    					+lowX+","+highX+")  and y ("+lowY+","+highY+") and weight of: "+toReturn);
   	 }
 
    return toReturn;
  }
  /**
   * Creates an empty bitmask matrix using the regions height, width, and resolution.
   *
   */
  public void makeMatrix()
  {
    if((height != -1)&&(width != -1))
	    // try rounding instead of just truncating
      bMask = new byte[(int)Math.round(height/resolution)][(int)Math.round(width/resolution)];
  }
  /**
   * Sets the given coordinates to true in the bit mask.
   * 
   * @param findX X-coordinate (longitude) of the point to set true.
   * @param findY Y-coordinate (latitude) of the point to set true.
   * Note that bmask y coordinates counts down from top (as does all PP data)
   */
  public void setPointTrue(double findX, double findY)
  {

	  //Prev versions. Note Y is counting down from top of this object
      // int Xindx = (int)( (findX-x)/resolution + resolution/10 );
      // int Yindx = (int)( ( (y+height)-(findY+resolution) )/resolution + resolution/10  );

	Point2D.Double thisPointIndex, refPointIndex, maxThisRegion;
	refPointIndex = CoordConversions.point2index( new Point2D.Double( x,y ), resolution, true );
	thisPointIndex = CoordConversions.point2index( new Point2D.Double( findX,findY ), resolution, true );
    maxThisRegion  = CoordConversions.point2index( new Point2D.Double(x + width, y + height ), resolution, false);

	int Xindx = (int)Math.round( thisPointIndex.x - refPointIndex.x );
	// Y index counts up from bottom. This flips to count down from top. maxThisRegion.y  should be 1 larger than any point.
	int Yindx = (int)Math.round( maxThisRegion.y - thisPointIndex.y ) - 1; 
	    
      if( Yindx<=-1 || Yindx >=bMask.length ) {
	    log.log(Level.WARNING,"In "+name+" Yindx is out of bounds with "+Yindx+" possibly due to rounding errors, setting to 0");
	    Yindx = 0;
    }
    if( Xindx<=-1 || Xindx >=bMask[0].length ) {
	    log.log(Level.WARNING,"In "+name+" Xindx is out of bounds with "+Xindx+" possibly due to rounding errors, setting to 0");
	    Xindx = 0;
    }
    bMask[Yindx][Xindx] = 1;
  }
  /**
   * Prints to standard out the bitmask for this region as well as its bounds.
   * The bitmask is printed as a matrix of 1's and 0's.
   *
   */
  public void printMask()
  {
    System.out.println("Printing BitMask for: "+name);
    System.out.println("bounds: (x) "+x+" to "+(x+width)+" by (y) "+(y+height)+" to "+y);
    for(int i = 0; i < bMask.length; i++)
    {
      for(int k = 0; k < bMask[i].length; k++)
      {
        System.out.print(bMask[i][k]);
      }
      System.out.println();
    }
  }

  /**
   * Adds a variable to the exclusion list.  This is necessary when
   * we want to have variables which will be in a special global region.
   * @param varName The variable which will be excluded from this region.
   */
  public void addToExclusionList(String varName) {
	  excludeVarNames.add(varName);
  }

  /**
   * Determines if the given variable name should be included in this region mask.
   * @param varName The variable to check.
   * @return True if it should be excluded, false otherwise.
   */
  public boolean shouldExcludeVariable(String varName) {
	  return excludeVarNames.contains(varName);
  }
}

