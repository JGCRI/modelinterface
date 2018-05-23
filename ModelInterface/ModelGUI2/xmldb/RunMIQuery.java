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
import java.io.OutputStream;
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
import org.basex.query.value.array.Array;
import org.basex.query.value.seq.StrSeq;
import org.basex.query.value.seq.Empty;
import org.basex.query.value.node.*;
import org.basex.query.value.map.Map;
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
    @Requires(Permission.READ)
    @Deterministic
    @ContextDependent
    public Value runMIQuery(ANode aMIQury, Value aScnNames, Value aRegionNames) throws QueryException {
        // We need to put any logging on STDERR so we can assume anything on STDOUT is results
        PrintStream stdout = System.out;
        PrintStream stderr = System.err;
        // If a user wants to suppress all output we must redirect STDERR as well
        if(Boolean.parseBoolean(System.getProperty("ModelInterface.SUPPRESS_OUTPUT", "false"))) {
            System.setErr(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    // ignore all
                }
            }));
        }
        System.setOut(System.err);
        XMLDB xmldb = null;
        try {
            String[] scenarioNames = null;
            String[] regions = null;
            if(aScnNames instanceof Empty) {
                scenarioNames = new String[0];
            } else if(aScnNames instanceof Str) {
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
            xmldb = new XMLDB(queryContext.context);
            Vector<ScenarioListItem> scenariosInDb = DbViewer.getScenarios(xmldb);
            Vector<ScenarioListItem> scenariosToRun = new Vector<ScenarioListItem>();
            if(scenarioNames.length == 0 && !scenariosInDb.isEmpty()) {
                scenariosToRun.add(scenariosInDb.lastElement());
            } else {
                for(String currScn : scenarioNames) {
                    String[] scnSplit = currScn.split(" (?=[^ ]+$)");
                    String scnName = scnSplit.length > 0 ? scnSplit[0] : null;
                    String scnDate = scnSplit.length > 1 ? scnSplit[1] : null;
                    ScenarioListItem found = ScenarioListItem.findClosestScenario(scenariosInDb, scnName, scnDate);
                    if(found != null) {
                        scenariosToRun.add(found);
                    }
                }
            }
            if(scenariosToRun.isEmpty()) {
                throw new Exception("Could not find scenarios to run.");
            }
            QueryProcessor queryProc = xmldb.createQuery(qg, scenariosToRun.toArray(), regions);
            return buildTable(queryProc, qg);
        } catch(Exception e) {
            e.printStackTrace();
            throw new QueryException(e);
        } finally {
            // note no need to close xmldb since we adopted the context
            // from the already running BaseX instance

            // reset output streams
            System.setOut(stdout);
            System.setErr(stderr);
        }
    }
    private Map buildTable(QueryProcessor queryProc, QueryGenerator qg) throws Exception {
        System.out.println("In Function: "+System.currentTimeMillis());
        // TODO: replicate these checks, currently just assuming they are all false
        boolean sumAll = false;
        boolean isTotal = false;
        boolean isGlobal = false;

        Map output = Map.EMPTY;
        ValueBuilder records = new ValueBuilder(queryContext);
        Iter res = queryProc.iter();
        ANode tempNode;
        final LinkedList<QueryRow>parentPath = new LinkedList<QueryRow>();
        LinkedList<Value> header = new LinkedList();
        Value[] row = null;
        boolean isFirstRow = true;
        while((tempNode = (ANode)res.next()) != null) {
            BXNode domNode = BXNode.get(tempNode);
            qg.defaultAddToDataTree(tempNode.parent(), parentPath.listIterator(0), isGlobal);

            if(!parentPath.peekLast().key.equals("Units")) {
                QueryRow unitsRow = new QueryRow(null);
                parentPath.offerLast(unitsRow);
                unitsRow.key = "Units";
            }
            String currUnits = XMLDB.getAttrMap(BXNode.get(tempNode.parent())).get("unit");
            parentPath.peekLast().value = currUnits == null ? "None Specified" : currUnits;

            if(isFirstRow) {
                for(Iterator<QueryRow> it = parentPath.descendingIterator(); it.hasNext(); ) {
                    QueryRow currRow = it.next();
                    if(!(currRow.key == null && currRow.value == null )) {
                        header.add(Str.get(currRow.key));
                    }
                }
                header.add(Str.get("value"));
                row = new Value[header.size()];
                isFirstRow = false;
            }

            boolean skip = false;
            int colIndex = 0;
            for(Iterator<QueryRow> it = parentPath.descendingIterator(); it.hasNext(); ) {
                QueryRow currRow = it.next();
                if(!(currRow.key == null && currRow.value == null )) {
                    row[colIndex++] = Str.get(currRow.value);
                    // skip this data since a rewrite list set the value
                    // to an empty string indicating the user wanted to
                    // delete it
                    skip = skip || currRow.value.equals("");
                }
            }
            if(!skip) {
                row[colIndex] = Str.get(domNode.getNodeValue());
                records.add(Array.from(row));
            }
        }
        // the xquery wants the header as an array under the "names" key
        output = output.put(Str.get("names"),
                Array.from(header.toArray(new Value[0])),
                queryContext.root.info);
        // and it wants the data as a sequence of arrays under the "records" key
        output = output.put(Str.get("records"),
                records.value(),
                queryContext.root.info);
        System.out.println("After Function: "+System.currentTimeMillis());
        return output;
    }
}
