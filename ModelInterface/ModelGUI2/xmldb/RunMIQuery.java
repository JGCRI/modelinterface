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
package ModelInterface.ModelGUI2.xmldb;

import java.io.PrintStream;
import java.util.Vector;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;

import org.w3c.dom.Node;

import org.basex.core.*;
import org.basex.query.*;
import org.basex.query.iter.Iter;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.StrSeq;
import org.basex.query.value.seq.Empty;
import org.basex.query.value.node.*;
import org.basex.api.dom.BXNode;
import org.basex.util.list.StringList;

import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.ModelGUI2.ScenarioListItem;

/**
 * A BaseX Java bound QueryModule to run a Model Interface query.
 * @author Pralit Patel.
 */ 
public class RunMIQuery extends QueryModule {
    @Requires(Permission.NONE)
    @Deterministic
    @ContextDependent
    public Value runMIQuery(ANode aMIQury, Value aScnNames, Value aRegionNames) throws QueryException {
        // We need to put any logging on STDERR so we can assume anything on STDOUT is results
        PrintStream stdout = System.out;
        System.setOut(System.err);
        try {
            String[] scenarioNames = null;
            String[] regions = null;
            if(aScnNames instanceof Str) {
                scenarioNames = new String[] { ((Str)aScnNames).toJava() };
            } else if(aScnNames instanceof StrSeq) {
                scenarioNames = ((StrSeq)aScnNames).toJava();
            } else {
                throw new Exception("Scenario argument of unexpected type: "+aScnNames.toString());
            }
            if(aRegionNames instanceof Empty) {
                regions = new String[0];
            } else if(aRegionNames instanceof Str) {
                regions = new String[] { ((Str)aRegionNames).toJava() };
            } else if(aRegionNames instanceof StrSeq) {
                regions = ((StrSeq)aRegionNames).toJava();
            } else {
                throw new Exception("Regions argument of unexpected type: "+aRegionNames.toString());
            }
            QueryGenerator qg = new QueryGenerator(aMIQury.toJava());
            XMLDB.openDatabase(queryContext.context);
            String currScen = scenarioNames[0];
            Vector<ScenarioListItem> scenarios = DbViewer.getScenarios();
            ScenarioListItem[] found = null;
            for(ListIterator<ScenarioListItem> scenarioIt = scenarios.listIterator(scenarios.size()); scenarioIt.hasPrevious() && found == null; ) {
                ScenarioListItem scenarioItem = scenarioIt.previous();
                if(currScen.equals(scenarioItem.getScnName())) {
                    found = new ScenarioListItem[] { scenarioItem };
                    // TODO: warn about duplicates?
                }
            }
            QueryProcessor queryProc = XMLDB.getInstance().createQuery(qg, found, regions);
            ValueBuilder vb = new ValueBuilder();
            FElem elem = new FElem("csv");
            vb.add(elem);
            buildTable(queryProc, qg, elem);
            return vb.value();
            //return res.value();
        } catch(Exception e) {
            e.printStackTrace();
            throw new QueryException(e);
        } finally {
            XMLDB.closeDatabase();
            // reset STDOUT
            System.setOut(stdout);
        }
    }
    private void buildTable(QueryProcessor queryProc, QueryGenerator qg, FElem outputDoc) throws Exception {
        System.out.println("In Function: "+System.currentTimeMillis());
        // TODO: replicate these checks, currently just assuming they are all false
        boolean sumAll = false;
        boolean isTotal = false;
        boolean isGlobal = false;

        Iter res = queryProc.iter();
        ANode tempNode;
        final LinkedList<QueryRow>parentPath = new LinkedList<QueryRow>();
        while((tempNode = (ANode)res.next()) != null) {
            BXNode domNode = BXNode.get(tempNode);
            qg.defaultAddToDataTree(tempNode.parent(), parentPath.listIterator(0), isGlobal);

            if(!parentPath.peekLast().key.equals("Units")) {
                QueryRow unitsRow = new QueryRow(null);
                parentPath.offerLast(unitsRow);
                unitsRow.key = "Units";
            }
            parentPath.peekLast().value = XMLDB.getAttrMap(BXNode.get(tempNode.parent())).get("unit");

            FElem row = new FElem("record");
            for(Iterator<QueryRow> it = parentPath.descendingIterator(); it.hasNext(); ) {
                QueryRow currRow = it.next();
                if(!(currRow.key == null && currRow.value == null )) {
                    FElem col = new FElem(currRow.key);
                    col.add(currRow.value);
                    row.add(col);
                }
            }
            FElem valueCol = new FElem("value");
            valueCol.add(domNode.getNodeValue());
            row.add(valueCol);
            outputDoc.add(row);
        }
        System.out.println("After Function: "+System.currentTimeMillis());
    }
}
