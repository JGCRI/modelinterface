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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Newer versions of the xml database don't like node level filtered queries and have
 * terrible performance.  The SingleQueries have been updated to avoid using that syntax
 * however copy/paste will still generate it as well as some queries users may still have
 * so we will try to detect such as case and use this query binding to wrap another query
 * binding and switch it around to get the expected performance and behavoir.
 * @author Pralit Patel
 */
public class NodeFilterDecoratorQueryBinding implements QueryBinding {
	private final QueryBinding wrappedBinding;
	public NodeFilterDecoratorQueryBinding(QueryBinding toWrap) {
		wrappedBinding = toWrap;
	}
	public String bindToQuery(Object[] scenarios, Object[] regions) {
		String ret = wrappedBinding.bindToQuery(scenarios, regions);
		Pattern p = Pattern.compile("^(.*(?:text|node)\\(\\))(\\[.*\\])$");
		Matcher m = p.matcher(ret);
		if(m.matches()) {
			String base = m.group(1);
			String filter = m.group(2);
			return getNodeLevelFilteredQuery(base, filter);
		} else {
			// this must be an error since the factory claimed it matched
			// but we can atleast just return the unmodified binded query
			return ret;
		}
	}

	/**
	 * Gets the appropriate query to get filtered node level values.  This could be
	 * as simple as concatinating the base query with the nodel level filter path 
	 * however there seems to be issues with the way this gets optimized in BDBXML 2.4.
	 * So will try wrapping this in a simple FLWR expression like:
	 * for $n in *baseQuery*
	 * return $n*nodeLevelFilter*
	 * TODO: This is the same as SingleQueryQueryBinding.getNodeLevelFilteredQuery and ideally
	 * 	 we would call it from there.
	 * @param baseQuery The base portion of the query
	 * @param nodeLevelFilter The generated query portion which will give filtered the results.
	 * @return A query which will get you the filtered filtered values in a reasonable way.
	 */
	private String getNodeLevelFilteredQuery(String baseQuery, String nodeLevelFilter) {
		StringBuilder query = new StringBuilder("for $n in "); 
		query.append(baseQuery).append(" return $n").append(nodeLevelFilter);
		return query.toString();
	}
}
