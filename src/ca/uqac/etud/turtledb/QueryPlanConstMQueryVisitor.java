/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.Engine;
import ca.uqac.dim.turtledb.MQueryVisitor;
import ca.uqac.dim.turtledb.Intersection;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.Product;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.QueryPlan;
import ca.uqac.dim.turtledb.QueryVisitor;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Selection;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.Union;
import ca.uqac.dim.turtledb.VariableTable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fx
 */
public class QueryPlanConstMQueryVisitor extends MQueryVisitor
{

	private float currentCost;
	private QueryPlan currentQP;
	private String currentEngine;
	private Relation growingRel;

	@Override
	public void visit(Projection r) throws MVisitorException
	{
		r.getRelation().maccept(this);
		growingRel = new Projection(r.getSchema(), growingRel);
	}

	@Override
	public void visit(Selection r) throws MVisitorException
	{
		r.getRelation().maccept(this);
		growingRel = new Selection(r.getCondition(), growingRel);
	}

	@Override
	public void visit(Table r) throws MVisitorException
	{
	}
// TODO : finir l'intégration de growingrel
	@Override
	public void visit(VariableTable r) throws MVisitorException
	{
		QueryPlan curqp;
		QueryPlan minqp = new QueryPlan();
		float curcost;
		float mincost = Float.MAX_VALUE;
		String minEngine = null;
		List<Engine> leg = BD.getTableLocations(r);

		for (Engine e : leg)
		{
			curqp = new QueryPlan();
			curqp.put(e.getName(), r);
			curcost = QueryOptimizer.getCost(curqp);

			if (curcost < mincost)
			{
				minEngine = e.getName();
				mincost = curcost;
				minqp = curqp;
			}
		}

		currentCost = mincost;
		currentQP = minqp;
		currentEngine = minEngine;
	}

	@Override
	public void visit(Union r) throws MVisitorException
	{
		float curcost = Float.MAX_VALUE;
		float mincost = Float.MAX_VALUE;
		QueryPlan curqp = new QueryPlan();
		QueryPlan minqp = new QueryPlan();
		String minEngine = null;
		Map<QueryPlan, Pair<Float, String>> costMap;
		costMap = new HashMap<QueryPlan, Pair<Float, String>>();

		for (Relation c : r.getRelations())
		{
			c.maccept(this);
			costMap.put(currentQP, new Pair(currentCost, currentEngine));
		}
		for (Map.Entry<QueryPlan, Pair<Float, String>> e : costMap.entrySet())
		{
			curqp.putAll(e.getKey());
		}

		QueryPlan uqp;
		for (String site : BD.sites.keySet())
		{
			uqp = new QueryPlan();
			uqp.putAll(curqp);
			uqp.put(site, r);
			curcost = QueryOptimizer.getCost(uqp);

			if (curcost < mincost)
			{
				minEngine = site;
				mincost = curcost;
				minqp = uqp;
			}
		}
		currentCost = mincost;
		currentQP = minqp;
		currentEngine = minEngine;

	}

	@Override
	public void visit(Intersection r) throws MVisitorException
	{
		float curcost = Float.MAX_VALUE;
		float mincost = Float.MAX_VALUE;
		QueryPlan curqp = new QueryPlan();
		QueryPlan minqp = new QueryPlan();
		String minEngine = null;
		Map<QueryPlan, Pair<Float, String>> costMap;
		costMap = new HashMap<QueryPlan, Pair<Float, String>>();

		for (Relation c : r.getRelations())
		{
			c.maccept(this);
			costMap.put(currentQP, new Pair(currentCost, currentEngine));
		}
		for (Map.Entry<QueryPlan, Pair<Float, String>> e : costMap.entrySet())
		{
			curqp.putAll(e.getKey());
		}

		QueryPlan uqp;
		for (String site : BD.sites.keySet())
		{
			uqp = new QueryPlan();
			uqp.putAll(curqp);
			uqp.put(site, r);
			curcost = QueryOptimizer.getCost(uqp);

			if (curcost < mincost)
			{
				minEngine = site;
				mincost = curcost;
				minqp = uqp;
			}
		}
		currentCost = mincost;
		currentQP = minqp;
		currentEngine = minEngine;
	}

	@Override
	public void visit(Join r) throws MVisitorException
	{
		float curcost = Float.MAX_VALUE;
		float mincost = Float.MAX_VALUE;
		QueryPlan curqp = new QueryPlan();
		QueryPlan minqp = new QueryPlan();
		String minEngine = null;
		Map<QueryPlan, Pair<Float, String>> costMap;
		costMap = new HashMap<QueryPlan, Pair<Float, String>>();

		r.getLeft().maccept(this);
			costMap.put(currentQP, new Pair(currentCost, currentEngine));
		r.getRight().maccept(this);
			costMap.put(currentQP, new Pair(currentCost, currentEngine));

		for (Map.Entry<QueryPlan, Pair<Float, String>> e : costMap.entrySet())
		{
			curqp.putAll(e.getKey());
		}

		QueryPlan uqp;
		for (String site : BD.sites.keySet())
		{
			uqp = new QueryPlan();
			uqp.putAll(curqp);
			uqp.put(site, r);
			curcost = QueryOptimizer.getCost(uqp);

			if (curcost < mincost)
			{
				minEngine = site;
				mincost = curcost;
				minqp = uqp;
			}
		}
		currentCost = mincost;
		currentQP = minqp;
		currentEngine = minEngine;
	}

	@Override
	public void visit(Product r) throws MVisitorException
	{
		float curcost = Float.MAX_VALUE;
		float mincost = Float.MAX_VALUE;
		QueryPlan curqp = new QueryPlan();
		QueryPlan minqp = new QueryPlan();
		String minEngine = null;
		Map<QueryPlan, Pair<Float, String>> costMap;
		costMap = new HashMap<QueryPlan, Pair<Float, String>>();

		for (Relation c : r.getRelations())
		{
			c.maccept(this);
			costMap.put(currentQP, new Pair(currentCost, currentEngine));
		}
		for (Map.Entry<QueryPlan, Pair<Float, String>> e : costMap.entrySet())
		{
			curqp.putAll(e.getKey());
		}

		QueryPlan uqp;
		for (String site : BD.sites.keySet())
		{
			uqp = new QueryPlan();
			uqp.putAll(curqp);
			uqp.put(site, r);
			curcost = QueryOptimizer.getCost(uqp);

			if (curcost < mincost)
			{
				minEngine = site;
				mincost = curcost;
				minqp = uqp;
			}
		}
		currentCost = mincost;
		currentQP = minqp;
		currentEngine = minEngine;
	}
}
