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
package ModelInterface.ModelGUI2.queries;

import ModelInterface.ModelGUI2.xmldb.XMLDB;
import ModelInterface.common.DataPair;

import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.ListSelectionModel;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.EventListener;

public class EmissionsQueryBuilder extends QueryBuilder {
	public static Map<String, Boolean> ghgList;
	public static Map<String, Boolean> fuelList;
	protected Map sectorList;
	protected Map subsectorList;
	protected Map techList;
	public static String xmlName = "emissionsQueryBuilder";
	private static final String ghgQueryPortion = "*[@type = 'GHG']";
	public EmissionsQueryBuilder(QueryGenerator qgIn) {
		super(qgIn);
		sectorList = null;
		subsectorList = null;
		techList = null;
	}
	public EventListener getListSelectionListener(final JComponentAdapter list, final JButton nextButton, final JButton cancelButton) {
		queryFunctions.removeAllElements();
		queryFunctions.add("distinct-values");
		queryFilter = "/scenario/world/"+regionQueryPortion+"/";
		return (new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				int[] selectedInd = list.getSelectedRows();
				if(selectedInd.length == 0 && (qg.currSel != 0 && qg.currSel != 3)) {
					nextButton.setEnabled(false);
					cancelButton.setText(" Cancel "/*cancelTitle*/);
				} else if(qg.currSel == 1 || qg.currSel == 2) {
					nextButton.setEnabled(true);
				} else if(qg.currSel == 3) {
					nextButton.setEnabled(true);
					cancelButton.setText("Finished");
				} else if((qg.isSumable && (selectedInd[0] == 0 || selectedInd[0] == 1)) || selectedInd.length > 1
					|| ((String)list.getSelectedValues()[0]).startsWith("Group:")) {
					nextButton.setEnabled(false);
					cancelButton.setText("Finished");
				} else if(qg.currSel != 6){
					nextButton.setEnabled(true);
					cancelButton.setText(" Cancel "/*cancelTitle*/);
				} else {
					cancelButton.setText("Finished");
				}
			}
		});
	}
	public void doFinish(JComponentAdapter list) {
		++qg.currSel;
		updateSelected(list);
		--qg.currSel;
		createXPath();
		queryFunctions = null;
		queryFilter = null;
	}
	public JComponentAdapter doBack(JComponentAdapter list, JLabel label) {
		// doing this stuff after currSel has changed now..
		// have to sub 1
		if(qg.currSel == 3) {
			sectorList = null;
		} else if(qg.currSel == 4) {
			subsectorList = null;
		} else if(qg.currSel == 5) {
			techList = null;
		}
		return updateList(list, label);
	}
	public JComponentAdapter doNext(JComponentAdapter list, JLabel label) {
		// being moved to after currSel changed, adjust numbers
		updateSelected(list);
		if(qg.currSel == 3) {
			/*
			for(Iterator it = varList.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry me = (Map.Entry)it.next();
				if(((Boolean)me.getValue()).booleanValue()) {
					qg.var = (String)me.getKey();
					//System.out.println("var is "+var);
					qg.isSumable = qg.sumableList.contains(qg.var);
					/*
					if(isSumable) {
						System.out.println("it does contain it");
					} else {
						System.out.println("doesn't contain it");
					}
				}
			}
			*/
			qg.var = "emissions";
		}
		return updateList(list, label);
	}
	public boolean isAtEnd() {
		return qg.currSel == 7-1;
	}
	public JComponentAdapter updateList(JComponentAdapter list, JLabel label) {
		Map temp = null;
		switch(qg.currSel) {
			case 2: {
					list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					if(ghgList == null) {
						ghgList = createList(sectorQueryPortion+"/"+subsectorQueryPortion+"/"+technologyQueryPortion+"/"+
								ghgQueryPortion+"/@name", false);
						System.out.println("GETS HERE IN GHG LIST");
					}
					temp = ghgList;
					label.setText("Select Greenhouse Gas:");
					break;
			}
			case 3: {
					list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
					if(fuelList == null) {
						fuelList = createList(createListPath(6), false);
					}
					temp = fuelList;
					label.setText("Select Fuels: ");
					break;
			}
			case 4: {
					//list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
					if(sectorList == null) {
						sectorList = createList(sectorQueryPortion+"/@name", false);
						sectorList.putAll(createList(sectorQueryPortion+"/group/@name", true));
					}
					temp = sectorList;
					label.setText("Select Sector:");
					break;
			}
			case 5: {
					if(subsectorList == null) {
						subsectorList = createList(createListPath(4), false);
					}
					temp = subsectorList;
					label.setText("Select Subsector:");
					break;
			}
			case 6: {
					if(techList == null) {
						techList = createList(createListPath(5), false);
					}
					temp = techList;
					label.setText("Select Technology:");
					break;
			}
			default: System.out.println("Error currSel: "+qg.currSel);
		}
		Vector tempVector = new Vector();
		String[] currKeys = (String[])temp.keySet().toArray(new String[0]);
		((JList)list.getModel()).setListData(currKeys);
		// check the maps to see which ones are true and add it to the list of selected
		for (int i = 0; i < currKeys.length; ++i) {
			if (((Boolean)temp.get(currKeys[i])).booleanValue()) {
				tempVector.addElement(new Integer(i));
			}
		}
		int[] selected = new int[tempVector.size()];
		for (int i = 0; i < selected.length; i++) {
			selected[i] = ((Integer)tempVector.get(i)).intValue();
		}
		temp = null;
		tempVector = null;
		list.setSelectedRows(selected);
		return list;
	}
	public void updateSelected(JComponentAdapter list) {
		Object[] selectedKeys = list.getSelectedValues();
		Map selected = null;
		switch(qg.currSel -1) {
			case 1: {
					return;
			}
			case 2: {
					selected = ghgList;
					qg.isSumable = true;
					break;
			}
			case 3: {
					selected = fuelList;
					break;
			}
			case 4: {
					selected = sectorList;
					break;
			}
			case 5: {
					selected = subsectorList;
					break;
			}
			case 6: {
					selected = techList;
					break;
			}
			default: System.out.println("Error currSel: "+qg.currSel);
		}
		for(Iterator it = selected.entrySet().iterator(); it.hasNext(); ) {
			((Map.Entry)it.next()).setValue(new Boolean(false));
		}
		for(int i = 0; i < selectedKeys.length; ++i) {
			selected.put(selectedKeys[i], new Boolean(true));
		}
	}
	public String createListPath(int level) {
		Map tempMap;
		StringBuffer ret = new StringBuffer();
		boolean added = false;
		boolean gq = false;
		//Vector lV = null;
		System.out.println("level is "+level);
		if(level == -1) {
			gq = true;
			qg.sumAll = qg.group = false;
			level = 8;
		}
		for(int i = 0; i < level-3; ++i) {
			if(i == 0) {
				tempMap = sectorList;
				ret.append(sectorQueryPortion.substring(0, sectorQueryPortion.length()-1));
			} else if(i == 1){
				tempMap = subsectorList;
				ret.append(subsectorQueryPortion.substring(0, subsectorQueryPortion.length()-1));
			} else if(i == 2){
				tempMap = techList;
				ret.append(technologyQueryPortion.substring(0, technologyQueryPortion.length()-1));
				//++i;
			} else if(i == 3){
				tempMap = ghgList;
				//ret.append("GHG");
				ret.append(ghgQueryPortion.substring(0, ghgQueryPortion.length()-1));
			} else {
				//lV = new Vector();
				tempMap = fuelList;
				ret.append("emissions");
			}
			added = false;
			if(tempMap == null) {
				if(i <= 3) {
					ret.append("]/");
				} else {
					ret.append("/");
				}
				continue;
			}
			if(qg.isSumable && tempMap.get("Sum All") != null && ((Boolean)tempMap.get("Sum All")).booleanValue()) {
				qg.sumAll = true;
			}
			if(qg.isSumable && tempMap.get("Group All") != null && ((Boolean)tempMap.get("Group All")).booleanValue()) {
				qg.group = true;
			}
			//for(Iterator it = tempMap.entrySet().iterator(); it.hasNext() && !((Boolean)tempMap.get("Sum All")).booleanValue(); ) 
			for(Iterator it = tempMap.entrySet().iterator(); it.hasNext() && (i == 3 || i == 4 || !(qg.sumAll || qg.group)); ) {
				Map.Entry me = (Map.Entry)it.next();
				if(((Boolean)me.getValue()).booleanValue()) {
					if(!added) {
						if(i == 0 || i == 1 || i == 2 || i == 3) {
							ret.append(" and (");
						} else {
							ret.append("[( ");
						}
						added = true;
					} else {
						ret.append(" or ");
					}
					if(!gq && ((String)me.getKey()).startsWith("Group:")) {
						ret.append(expandGroupName(((String)me.getKey()).substring(7)));
						gq = true;
					} else if(i != 4) {
						ret.append("(@name='"+me.getKey()+"')");
					} else {
						ret.append("(@fuel-name='"+me.getKey()+"')");
						//lV.add(me.getKey());
					}
				}
			}
			if(added || i <= 3) {
				ret.append(" )]/");
			} else {
				ret.append("/");
			}
		}
		if(level == 4) {
			ret.append(subsectorQueryPortion+"/@name");
		} else if(level == 5) {
			ret.append(technologyQueryPortion+"/@name");
		} else if(level == 6) {
			ret.append(ghgQueryPortion+"/emissions/@fuel-name");
		} else if(level == 7) {
			ret.append(qg.var).append("/node()");
		} else {
			ret.append("node()");
			System.out.println("The xpath is: "+ret.toString());
		}
		if(gq) {
			qg.group = true;
			qg.sumAll = true;
		}
		return ret.toString();
	}
	private String expandGroupName(String gName) {
        /*
		String query;
		StringBuffer ret = new StringBuffer();
		if(qg.currSel == 3) {
			query = sectorQueryPortion+"/"+subsectorQueryPortion+"/"+technologyQueryPortion+"/"+ghgQueryPortion+"/emissions";
		} else if(qg.currSel == 4) {
			query = sectorQueryPortion;
		} else if(qg.currSel == 5) {
			query = sectorQueryPortion+"/"+subsectorQueryPortion;
		} else {
			query = sectorQueryPortion+"/"+subsectorQueryPortion+"/"+technologyQueryPortion;
		}
		// do I need to substr the ] out of query and add AND ??
		XmlResults res = XMLDB.getInstance().createQuery(queryFilter+query+
				"[child::group[@name='"+gName+"']]/@name", 
				queryFunctions, null, null);
		try {
			while(res.hasNext()) {
				ret.append("(@name='").append(res.next().asString()).append("') or ");
			}
		} catch(XmlException e) {
			e.printStackTrace();
		}
		ret.delete(ret.length()-4, ret.length());
		return ret.toString();
        */
        // TODO: reimplement?
        return null;
	}
	private void createXPath() {
		qg.xPath = createListPath(8);
		switch(qg.currSel) {
			case 3: qg.nodeLevel = new DataPair<String, String>("emissions", "fuel-name");
				break;
			case 4: qg.nodeLevel = new DataPair<String, String>("sector", "name");
				break;
			case 5: qg.nodeLevel = new DataPair<String, String>("subsector", "name");
				break;
			case 6: qg.nodeLevel = new DataPair<String, String>("technology", "name");
				break;
			default: System.out.println("Error currSel: "+qg.currSel);
		}
		qg.axis1Name = qg.nodeLevel.getKey();
		qg.yearLevel = new DataPair<String, String>("emissions", "year");
		qg.axis2Name = "Year";
	}
	private Map createList(String path, boolean isGroupNames) {
        /*
		System.out.println("createing "+path);
		LinkedHashMap ret = new LinkedHashMap();
		if(!isGroupNames && qg.isSumable) {
			ret.put("Sum All", new Boolean(false));
			ret.put("Group All", new Boolean(false));
		}
		XmlResults res = XMLDB.getInstance().createQuery(queryFilter+path, queryFunctions, null, null);
		try {
			while(res.hasNext()) {
				if(!isGroupNames) {
					ret.put(res.next().asString(), new Boolean(false));
				} else { 
					ret.put("Group: "+res.next().asString(), new Boolean(false));
				}
			}
		} catch(XmlException e) {
			e.printStackTrace();
		}
		res.delete();
		return ret;
        */
        // TODO: reimplement?
        return null;
	}
	public String getCompleteXPath(Object[] regions) {
		boolean added = false;
		StringBuffer ret = new StringBuffer();
        boolean isGlobal;
		if(((String)regions[0]).equals("Global")) {
			ret.append(regionQueryPortion+"/");
			//regionSel = new int[0]; 
			regions = new Object[0];
			isGlobal = true;
		} else {
			isGlobal = false;
		}
		for(int i = 0; i < regions.length; ++i) {
			if(!added) {
				ret.append(regionQueryPortion.substring(0, regionQueryPortion.length()-1) + " and (");
				added = true;
			} else {
				ret.append(" or ");
			}
			ret.append("(@name='").append(regions[i]).append("')");
		}
		if(!isGlobal) {
			ret.append(" )]/");
		}
		return ret.append(qg.getXPath()).toString();
	}
	public String getXMLName() {
		return xmlName;
	}
	public List<String> getDefaultCollpaseList() {
		List<String> defaultCollapse = new Vector<String>(5);
		defaultCollapse.add("sector");
		defaultCollapse.add("subsector");
		defaultCollapse.add("technology");
		defaultCollapse.add("emissions");
		defaultCollapse.add("GHG");
		return defaultCollapse;
	}
}
