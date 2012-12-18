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
	Integer vTableCount = 0;

	public QueryPlan getQueryPlan()
	{
		currentQP.put(currentEngine, growingRel);
		return currentQP;
	}
	
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
		currentEngine = minEngine;
		growingRel = (Relation) r.clone();
	}

	@Override
	public void visit(Union r) throws MVisitorException
	{
		float minCost = Float.MAX_VALUE;
		QueryPlan minQP = new QueryPlan();
		String minEngine = null;
		Union minTree = null;

		Map<Relation, Pair<Float, String>> costMap;
		costMap = new HashMap<Relation, Pair<Float, String>>();

		//On visite tout les enfants de la relation, on sauvegarde le résultat de la descente.
		for (Relation c : r.getRelations())
		{
			c.maccept(this);
			costMap.put(growingRel, new Pair(currentCost, currentEngine));
		}
		
		// Recherche d
		for (Map.Entry<Relation, Pair<Float, String>> e : costMap.entrySet())
		{
			QueryPlan curQP = new QueryPlan();
			String curEngine = e.getValue().getSecond();
			Union candidateTree = new Union();

			for (Map.Entry<Relation, Pair<Float, String>> f : costMap.entrySet())
			{
				// On parcours toute l'entryMap sauf e
				if (e != f)
				{
					/*
					 * Si la relation f est exécuté sur le même si que e,
					 * on l'ajoute directement à l'arbre
					 */
					if (f.getValue().getSecond().equals(curEngine))
					{
						candidateTree.addOperand(f.getKey());
					} /*
					 * Sinon, on crée un sous arbre avec comme racine une VariableTable,
					 * qui pour relation f et on l'ajout et QueryPlan courant.
					 * Dans l'arbre de l'Union, on remplace la branche f par la VariableTable
					 */ else
					{
						vTableCount++;
						VariableTable v = new VariableTable(
								vTableCount.toString(),
								f.getValue().getSecond());

						v.setRelation(f.getKey());
						curQP.put(f.getValue().getSecond(), v);
						candidateTree.addOperand(v);
					}
				}
			}
			Float curCost = QueryOptimizer.getCost(curQP);

			if (curCost < minCost)
			{
				minCost = curCost;
				minQP = curQP;
				minEngine = curEngine;
				minTree = candidateTree;
			}
		}
		
		currentCost = minCost;
		currentEngine = minEngine;
		currentQP.putAll(minQP);
		growingRel = minTree;
	}
/*
 * L'utilisation de generics pour éviter la duplication du code est impossible.
 * En effet, la JVM ne permet pas d'instantier un type générique...
 */
	@Override
	public void visit(Intersection r) throws MVisitorException
	{
		float minCost = Float.MAX_VALUE;
		QueryPlan minQP = new QueryPlan();
		String minEngine = null;
		Intersection minTree = null;

		Map<Relation, Pair<Float, String>> costMap;
		costMap = new HashMap<Relation, Pair<Float, String>>();

		//On visite tout les enfants de la relation, on sauvegarde le résultat de la descente.
		for (Relation c : r.getRelations())
		{
			c.maccept(this);
			costMap.put(growingRel, new Pair(currentCost, currentEngine));
		}
		
		// Recherche d
		for (Map.Entry<Relation, Pair<Float, String>> e : costMap.entrySet())
		{
			QueryPlan curQP = new QueryPlan();
			String curEngine = e.getValue().getSecond();
			Intersection candidateTree = new Intersection();

			for (Map.Entry<Relation, Pair<Float, String>> f : costMap.entrySet())
			{
				// On parcours toute l'entryMap sauf e
				if (e != f)
				{
					/*
					 * Si la relation f est exécuté sur le même si que e,
					 * on l'ajoute directement à l'arbre
					 */
					if (f.getValue().getSecond().equals(curEngine))
					{
						candidateTree.addOperand(f.getKey());
					} /*
					 * Sinon, on crée un sous arbre avec comme racine une VariableTable,
					 * qui pour relation f et on l'ajout et QueryPlan courant.
					 * Dans l'arbre de l'Union, on remplace la branche f par la VariableTable
					 */ else
					{
						vTableCount++;
						VariableTable v = new VariableTable(
								vTableCount.toString(),
								f.getValue().getSecond());

						v.setRelation(f.getKey());
						curQP.put(f.getValue().getSecond(), v);
						candidateTree.addOperand(v);
					}
				}
			}
			Float curCost = QueryOptimizer.getCost(curQP);

			if (curCost < minCost)
			{
				minCost = curCost;
				minQP = curQP;
				minEngine = curEngine;
				minTree = candidateTree;
			}
		}
		
		currentCost = minCost;
		currentEngine = minEngine;
		currentQP.putAll(minQP);
		growingRel = minTree;
	}

	@Override
	public void visit(Join r) throws MVisitorException
	{

		float minCost = Float.MAX_VALUE;
		QueryPlan minQP = new QueryPlan();
		String minEngine = null;
		Intersection minTree = null;

		Map<Relation, Pair<Float, String>> costMap;
		costMap = new HashMap<Relation, Pair<Float, String>>();

		//On visite tout les enfants de la relation, on sauvegarde le résultat de la descente.
		r.getLeft().maccept(this);
		costMap.put(growingRel, new Pair(currentCost, currentEngine));
		r.getRight().maccept(this);
		costMap.put(growingRel, new Pair(currentCost, currentEngine));
		
		// Recherche d
		for (Map.Entry<Relation, Pair<Float, String>> e : costMap.entrySet())
		{
			QueryPlan curQP = new QueryPlan();
			String curEngine = e.getValue().getSecond();
			Intersection candidateTree = new Intersection();

			for (Map.Entry<Relation, Pair<Float, String>> f : costMap.entrySet())
			{
				// On parcours toute l'entryMap sauf e
				if (e != f)
				{
					/*
					 * Si la relation f est exécuté sur le même si que e,
					 * on l'ajoute directement à l'arbre
					 */
					if (f.getValue().getSecond().equals(curEngine))
					{
						candidateTree.addOperand(f.getKey());
					} /*
					 * Sinon, on crée un sous arbre avec comme racine une VariableTable,
					 * qui pour relation f et on l'ajout et QueryPlan courant.
					 * Dans l'arbre de l'Union, on remplace la branche f par la VariableTable
					 */ else
					{
						vTableCount++;
						VariableTable v = new VariableTable(
								vTableCount.toString(),
								f.getValue().getSecond());

						v.setRelation(f.getKey());
						curQP.put(f.getValue().getSecond(), v);
						candidateTree.addOperand(v);
					}
				}
			}
			Float curCost = QueryOptimizer.getCost(curQP);

			if (curCost < minCost)
			{
				minCost = curCost;
				minQP = curQP;
				minEngine = curEngine;
				minTree = candidateTree;
			}
		}
		
		currentCost = minCost;
		currentEngine = minEngine;
		currentQP.putAll(minQP);
		growingRel = minTree;
	}

	@Override
	public void visit(Product r) throws MVisitorException
	{
		float minCost = Float.MAX_VALUE;
		QueryPlan minQP = new QueryPlan();
		String minEngine = null;
		Product minTree = null;

		Map<Relation, Pair<Float, String>> costMap;
		costMap = new HashMap<Relation, Pair<Float, String>>();

		//On visite tout les enfants de la relation, on sauvegarde le résultat de la descente.
		for (Relation c : r.getRelations())
		{
			c.maccept(this);
			costMap.put(growingRel, new Pair(currentCost, currentEngine));
		}
		
		// Recherche d
		for (Map.Entry<Relation, Pair<Float, String>> e : costMap.entrySet())
		{
			QueryPlan curQP = new QueryPlan();
			String curEngine = e.getValue().getSecond();
			Product candidateTree = new Product();

			for (Map.Entry<Relation, Pair<Float, String>> f : costMap.entrySet())
			{
				// On parcours toute l'entryMap sauf e
				if (e != f)
				{
					/*
					 * Si la relation f est exécuté sur le même si que e,
					 * on l'ajoute directement à l'arbre
					 */
					if (f.getValue().getSecond().equals(curEngine))
					{
						candidateTree.addOperand(f.getKey());
					} /*
					 * Sinon, on crée un sous arbre avec comme racine une VariableTable,
					 * qui pour relation f et on l'ajout et QueryPlan courant.
					 * Dans l'arbre de l'Union, on remplace la branche f par la VariableTable
					 */ else
					{
						vTableCount++;
						VariableTable v = new VariableTable(
								vTableCount.toString(),
								f.getValue().getSecond());

						v.setRelation(f.getKey());
						curQP.put(f.getValue().getSecond(), v);
						candidateTree.addOperand(v);
					}
				}
			}
			Float curCost = QueryOptimizer.getCost(curQP);

			if (curCost < minCost)
			{
				minCost = curCost;
				minQP = curQP;
				minEngine = curEngine;
				minTree = candidateTree;
			}
		}
		
		currentCost = minCost;
		currentEngine = minEngine;
		currentQP.putAll(minQP);
		growingRel = minTree;
		
	}
}
